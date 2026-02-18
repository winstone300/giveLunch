package main.givelunch.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import main.givelunch.properties.ObservabilityProperties;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ObservabilityProperties.class)
public class ObservabilityConfig {

    @Bean   // 서블릿 필터 빈으로 등록
    @ConditionalOnProperty(prefix = "app.observability", name = "enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<RequestCorrelationFilter> requestCorrelationFilter(ObservabilityProperties properties) {
        FilterRegistrationBean<RequestCorrelationFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new RequestCorrelationFilter(properties));   // runId와 시나리오 MDC에 저장하는 필터
        registrationBean.setOrder(Integer.MIN_VALUE + 100);     // 필터체인 실행 순서 설정
        return registrationBean;
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.observability.sql", name = "enabled", havingValue = "true", matchIfMissing = true)
    public BeanPostProcessor dataSourceProxyBeanPostProcessor(
            ObservabilityProperties properties,
            MeterRegistry meterRegistry) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                // 커넥션 풀 아니면 그냥 반환
                if (!(bean instanceof DataSource dataSource)) {
                    return bean;
                }

                // 이미 프록시로 감싸져 있거나 scopedTarget빈이면 그냥 반환
                if (bean instanceof ProxyDataSource || beanName.contains("scopedTarget")) {
                    return bean;
                }

                return ProxyDataSourceBuilder.create(dataSource)
                        .name(beanName)
                        // 쿼리 실행 전후 호출
                        .listener(new SlowQueryMetricsListener(properties.sql().slowQueryMs(), meterRegistry))
                        .build();
            }
        };
    }

    @Slf4j
    static class SlowQueryMetricsListener implements QueryExecutionListener {

        private static final int MAX_SQL_LOG_LENGTH = 400;
        private final long slowQueryMs;
        private final Timer timer;

        SlowQueryMetricsListener(long slowQueryMs, MeterRegistry meterRegistry) {
            this.slowQueryMs = slowQueryMs;
            this.timer = Timer.builder("givelunch.sql.query")
                    .description("Elapsed SQL query execution time from datasource-proxy")
                    .register(meterRegistry);
        }

        @Override
        public void beforeQuery(net.ttddyy.dsproxy.ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
        }

        @Override
        public void afterQuery(net.ttddyy.dsproxy.ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
            long elapsedMs = execInfo.getElapsedTime();     // sql 실행에 걸린 시간(프록시에 저장된 값 가져옴)
            timer.record(elapsedMs, TimeUnit.MILLISECONDS);
            if (elapsedMs < slowQueryMs) {
                return;
            }
            String sql = queryInfoList.stream()
                    .map(QueryInfo::getQuery)
                    .reduce((left, right) -> left + "; " + right)
                    .orElse("(empty)");
            log.warn("SLOW_SQL elapsedMs={} batch={} success={} query={}",
                    elapsedMs,
                    execInfo.isBatch(),
                    execInfo.isSuccess(),
                    truncate(sql));
        }

        // sql문 너무 길면 길이 제한
        private String truncate(String sql) {
            if (sql == null || sql.length() <= MAX_SQL_LOG_LENGTH) {
                return sql;
            }
            return sql.substring(0, MAX_SQL_LOG_LENGTH) + "...";
        }
    }
}
