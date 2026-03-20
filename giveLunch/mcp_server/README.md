# giveLunch Agent MCP Server

`agent_food_mcp_server.py` is a standalone stdio MCP server that forwards tool calls to the Spring agent API.

## Environment

- `GIVELUNCH_AGENT_BASE_URL`
  - Default: `http://localhost:8080`
- `GIVELUNCH_AGENT_API_KEY`
  - Required. Must match `app.agent-auth.api-key` on the Spring app.

## Run

```bash
python mcp_server/agent_food_mcp_server.py
```

## Tools

- `search_external_foods`
  - Input: `{ "name": "비빔밥", "limit": 5 }`
- `save_foods`
  - Input: `{ "items": [FoodAndNutritionDto, ...] }`
- `import_foods_by_name`
  - Input: `{ "names": ["비빔밥", "우동"], "limitPerName": 3 }`

## Spring endpoints used

- `POST /api/agent/foods/search-external`
- `POST /api/agent/foods/save`
- `POST /api/agent/foods/import`
