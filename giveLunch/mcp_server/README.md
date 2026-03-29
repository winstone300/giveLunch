# giveLunch MCP Servers

표준 입출력(`stdio`) 기반으로 동작하는 MCP 서버 모음입니다.

- `food_mcp_server.py`
  - 일반 운영/수동 사용용 서버
  - 음식 검색/저장 도구만 노출
- `benchmark_food_mcp_server.py`
  - 벤치마크 실행 전용 프록시 서버
  - 동일한 음식 도구와 benchmark 기록 도구를 함께 노출
- `agent_food_mcp_server.py`
  - 기존 진입점 호환용 래퍼
  - 현재는 `benchmark_food_mcp_server.py`를 실행
- `benchmark_food_mcp_runner.py`
  - OpenAI `Responses API` 응답의 `usage`를 누적해 benchmark 종료 시 exact 토큰 값을 기록하는 러너

Python MCP 서버는 중계 계층이며, 실제 비즈니스 처리는 Spring 애플리케이션이 담당합니다.

## 환경 변수

- `GIVELUNCH_AGENT_BASE_URL`
  - Spring 애플리케이션 기본 주소
  - 기본값: `http://localhost:8080`
- `GIVELUNCH_AGENT_API_KEY`
  - Spring 애플리케이션의 `app.agent-auth.api-key` 와 동일해야 함
- `GIVELUNCH_AGENT_BENCHMARK_DIR`
  - benchmark 산출물 저장 경로
  - 기본값: `mcp_server/benchmarks`
- `OPENAI_API_KEY`
  - `benchmark_food_mcp_runner.py` 실행 시 필요
- `OPENAI_BASE_URL`
  - 필요 시 OpenAI 호환 엔드포인트로 변경 가능

## 실행 방법

일반 Food MCP:

```bash
python mcp_server/food_mcp_server.py
```

Benchmark Food MCP:

```bash
python mcp_server/benchmark_food_mcp_server.py
```

기존 호환 진입점:

```bash
python mcp_server/agent_food_mcp_server.py
```

## 제공 도구

### Food MCP

- `search_external_foods`
- `save_foods`
- `import_foods_by_name`

각 도구는 아래 Spring API 엔드포인트로 전달됩니다.

- `search_external_foods` -> `POST /api/agent/foods/search-external`
- `save_foods` -> `POST /api/agent/foods/save`
- `import_foods_by_name` -> `POST /api/agent/foods/import`

### Benchmark Food MCP

Food MCP의 3개 도구에 더해 아래 benchmark 도구를 제공합니다.

- `benchmark_start_run`
  - benchmark run 폴더를 만들고 `input.json` 저장
- `benchmark_finish_run`
  - `result.json`, `summary.json` 저장

`benchmark_start_run` 입력 예시:

```json
{
  "scenario_name": "50-food-batch",
  "model": "gpt-5.4",
  "prompt_version": "v1",
  "food_names": ["비빔밥", "김치찌개"],
  "limit": 1,
  "cache_mode": "cold"
}
```

`benchmark_finish_run` 입력 예시:

```json
{
  "total_elapsed_ms": 12500,
  "input_tokens": 2100,
  "output_tokens": 650,
  "token_mode": "exact",
  "result": {
    "savedCount": 2,
    "skippedCount": 0,
    "failedCount": 0,
    "results": []
  }
}
```

## Benchmark 기록 방식

Benchmark Food MCP는 활성 run이 있을 때만 아래 정보를 자동 기록합니다.

- tool 시작/종료 시각
- tool elapsed time
- 요청/응답 바이트 수
- 정규화 인자 기준 중복 호출 여부

산출물은 `benchmarks/<run_id>/input.json`, `tool_calls.json`, `result.json`, `summary.json` 형식으로 저장됩니다.

## Exact Token 집계

`benchmark_food_mcp_runner.py`는 OpenAI `Responses API` 호출의 `usage`를 누적해 `benchmark_finish_run`에 아래 필드를 전달합니다.

- `input_tokens`
- `output_tokens`
- `total_tokens`
- `token_mode = "exact"`

`usage`를 받을 수 없는 경우에는 아래 fallback을 사용합니다.

- `input_text`
- `output_text`
- `token_mode = "estimated"`

토큰 관련 정보가 전혀 없으면 `token_mode = "unavailable"`로 저장됩니다.
