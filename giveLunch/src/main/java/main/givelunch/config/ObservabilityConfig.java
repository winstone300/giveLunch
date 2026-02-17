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

    @Bean
    @ConditionalOnProperty(prefix = "app.observability", name = "enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<RequestCorrelationFilter> requestCorrelationFilter(ObservabilityProperties properties) {
        FilterRegistrationBean<RequestCorrelationFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new RequestCorrelationFilter(properties));
        registrationBean.setOrder(Integer.MIN_VALUE + 100);
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
                if (!(bean instanceof DataSource dataSource)) {
                    return bean;
                }
                if (bean instanceof ProxyDataSource || beanName.contains("scopedTarget")) {
                    return bean;
                }

                return ProxyDataSourceBuilder.create(dataSource)
                        .name(beanName)
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
            long elapsedMs = execInfo.getElapsedTime();
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

        private String truncate(String sql) {
            if (sql == null || sql.length() <= MAX_SQL_LOG_LENGTH) {
                return sql;
            }
            return sql.substring(0, MAX_SQL_LOG_LENGTH) + "...";
        }
    }
}
