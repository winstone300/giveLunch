# 문제 2

**prometheus에서 bucket이 생성 되지 않는 문제 해결**

---

**Background**

---

- prometheus를 이용해 p95, p99 측정하려 했으나 http_server_requests_seconds_bucket이 생성되지 않는 문제 발생
- sql 쿼리 병목 파악을 위해 BeanPostProcessor를 이용하여 QueryExecutionListener 등록한 상황
- 의존성 및 기본설정(application.yml)에 대한 시도는 이미 한 상황
- QueryExecutionListener는 별도의 “{projectName}.sql.query” timer사용

**Approach**

---

**원인 파악**

- 버킷 생성을 담당하는 micrometer 히스토그램 생성 메서드 디버깅
    
    → 히스토그램 설정이 false로 찍힘
    
    **Case1.** 확인한 meter가  타겟 meter가 아닌 다른 meter 일 수 있음
    
    **Case2.** application.yml의 설정이 제대로 적용되지 않음 
    
- ****상위 클래스(PropertiesMeterFilter)에서 meter 정보 확인이 필요
    
    → 디버깅 결과 상위 클래스 생성자는 호출이 되지만 설정을 적용하는 메서드(configure)가 호출이 안됨
    
    →  application.yml의 설정이 제대로 적용되지 않음
    
    → 메서드(configure) 호출 객체에 대한 확인이 필요
    

**문제 발견**

- MeterRegistryPostProcessor 확인
    
    →  PropertiesMeterFilter가 MeterRegistry에 제대로 등록되지 않음을 발견
    
    → BeanPostProcessor를 이용하여 QueryExecutionListener 등록할 때 MeterRegistry 의존성이 필요하여 MeterRegistry가 그 시점에 생성됨
    
    → 이 시점에서 MeterRegistryPostProcessor는 생성되기 전이라 application.yml 설정이 registry에 제대로 반영이 안됨
    

**Solve**

---

- BeanPostProcessor를 이용하여 QueryExecutionListener 등록시 MeterRegistry를 lazy 방식으로 등록하도록 수정

### 기존 eager 방식

```java
public static BeanPostProcessor dataSourceProxyBeanPostProcessor(
			ObservabilityProperties properties,
			MeterRegistry meterRegistry) 
```

### lazy 방식으로 수정

```java
public static BeanPostProcessor dataSourceProxyBeanPostProcessor(
			ObservabilityProperties properties,
			ObjectProvider<MeterRegistry> meterRegistryProvider) 
```
<img width="1076" height="1018" alt="image" src="https://github.com/user-attachments/assets/e1d37ae8-f0c5-4ac3-859e-1edf3ca056da" />

