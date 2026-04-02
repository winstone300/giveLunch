# benchmark MCP 서버 및 skill 도입을 통한 음식 적재 성능 측정 자동화

`Python` `MCP` `Codex Skill` `Spring Boot`

## Background

---

- 에이전트가 음식 검색과 저장을 수행할 수 있게 되었지만 실행 성능을 체계적으로 측정하는 장치는 부족
- 일반 Food MCP 서버만으로는 실제 작업이 얼마나 걸렸는지, 어떤 도구 호출이 병목인지, 중복 호출이 있었는지, 토큰 사용량이 어느 정도인지 일관된 기준으로 남기기 어려웠음
- 단순 DB 저장 시간만이 아니라, 모델 추론, MCP 도구 호출, Spring API 처리, DB 반영까지 포함한 end-to-end 성능을 함께 측정할 수 있는 장치가 필요

## Approach

---

### Benchmark MCP 도입

- 역할을 아래처럼 나눔
    - `food_mcp_server.py`
        - 일반 운영용 Food MCP 서버
        - `search_external_foods`, `save_foods`, `import_foods_by_name`만 제공
    - `benchmark_food_mcp_server.py`
        - 성능 측정 전용 Benchmark MCP 서버
        - 음식 도구 3개와 `benchmark_start_run`, `benchmark_finish_run`를 함께 제공

### 공통 로직 모듈화

- `food_mcp_common.py`에 공통 기능을 모아 두 서버가 동일한 기반을 사용하도록 구성

### Benchmark 기록 기능 추가

- 기록 항목
    - tool 시작/종료 시각
    - tool elapsed time
    - request/response bytes
    - normalized arguments 기준 중복 호출 여부
- 산출물은 json 파일로 저장

### exact token usage 기록을 위한 benchmark runner 도입

- `benchmark_food_mcp_runner.py`를 추가해 OpenAI Responses API 응답의 `usage`를 누적
- exact usage를 얻을 수 있으면 얻은 토큰 값을 `benchmark_finish_run`에 전달
- exact usage를 얻을 수 없는 경우에는 텍스트 기반으로 토큰 추정
    - `math.ceil(len(normalized.encode("utf-8")) / 4)`
    - `token_mode = estimated`

### Codex skill 도입

- Codex가 benchmark를 수행할 때 `benchmark_start_run -> 음식 도구 호출 -> benchmark_finish_run` 순서를 항상 지키도록 유도해야 했음
- Codex가 benchmark 순서를 일관되게 따르도록 workspace skill `benchmark-food-mcp`를 추가
- skill이 담당하는 역할
    - benchmark 의도일 때 plain food MCP가 아니라 benchmark MCP 경로를 사용하도록 유도
    - 작업 시작 전 `benchmark_start_run` 호출
    - 작업 종료 후 `benchmark_finish_run` 호출
    - 결과 설명 시 `summary.json`, `tool_calls.json` 기준으로 핵심 지표를 보고하도록 안내
