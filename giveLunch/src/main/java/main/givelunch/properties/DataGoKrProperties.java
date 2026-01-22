package main.givelunch.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "app.data-go-kr")
public record DataGoKrProperties(
        // 기본 주소
        String baseUrl,

        // 인증용 서비스 키
        String serviceKey,

        // 요청 api
        String getAPI,

        // 응답 포맷 타입(json 등)
        String type,

        // 조회할 페이지 수
        int pageSize,

        // 조회할 data 수(관리자용)
        int numOfRowsAdmin,

        // 조회할 data 수(사용자용)
        int numOfRowsUser
) {}