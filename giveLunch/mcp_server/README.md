# giveLunch Agent MCP Server

표준 입출력(`stdio`) 기반으로 동작하는 MCP 서버

## TOOL


- 외부 음식 데이터를 검색
- 검색한 음식 데이터를 giveLunch에 저장
- 음식 이름 목록으로 검색과 저장을 한 번에 수행

Python MCP 서버는 중계 계층, 실제 비즈니스 처리는 Spring 애플리케이션이 담당

## 환경 변수

서버 실행 전 아래 환경 변수를 설정

- `GIVELUNCH_AGENT_BASE_URL`
  - Spring 애플리케이션의 기본 주소
  - 기본값: `http://localhost:8080`
- `GIVELUNCH_AGENT_API_KEY`
  - 필수 값입
  - Spring 애플리케이션의 `app.agent-auth.api-key` 값과 반드시 일치

## 실행 방법

프로젝트 루트 또는 해당 스크립트가 보이는 위치에서 아래 명령으로 실행

```bash
python mcp_server/agent_food_mcp_server.py
```

## 제공 도구

### `search_external_foods`

외부 음식 데이터를 검색

- 입력 예시

```json
{ "name": "비빔밥", "limit": 5 }
```

- 주요 필드
  - `name`: 검색할 음식 이름
  - `limit`: 최대 검색 개수, 생략 가능

### `save_foods`

검색된 음식 데이터(`FoodAndNutritionDto` 형태)를 giveLunch에 저장

- 입력 예시

```json
{ "items": [FoodAndNutritionDto, "..."] }
```

- 주요 필드
  - `items`: 저장할 음식 데이터 배열

### `import_foods_by_name`

음식 이름 목록을 기준으로 외부 검색과 저장을 한 번에 수행

- 입력 예시

```json
{ "names": ["비빔밥", "우동"], "limitPerName": 3 }
```

- 주요 필드
  - `names`: 검색할 음식 이름 목록
  - `limitPerName`: 각 이름별 최대 검색 개수

## 내부 동작 방식

각 MCP 도구 호출은 아래 Spring API 엔드포인트로 전달

- `search_external_foods` -> `POST /api/agent/foods/search-external`
- `save_foods` -> `POST /api/agent/foods/save`
- `import_foods_by_name` -> `POST /api/agent/foods/import`

요청은 JSON으로 전송되며, 인증은 아래 헤더를 사용

```http
Authorization: Bearer <GIVELUNCH_AGENT_API_KEY>
```

## 참고 사항

- `GIVELUNCH_AGENT_API_KEY`가 설정되지 않으면 서버는 도구 호출 시 오류를 반환합니다.
- Spring 서버가 실행 중이 아니거나 주소가 잘못되면 HTTP 또는 연결 오류가 발생합니다.
- MCP 응답에는 텍스트 형태의 결과와 함께 구조화된 JSON 결과가 포함됩니다.
