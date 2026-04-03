#!/usr/bin/env python3
import json
import os
import time
import urllib.error
import urllib.request

from mcp_protocol import MCPServerApp


FOOD_TOOL_NAMES = {"search_external_foods", "save_foods", "import_foods_by_name"}
FOOD_TOOL_ENDPOINTS = {
    "search_external_foods": "/api/agent/foods/search-external",
    "save_foods": "/api/agent/foods/save",
    "import_foods_by_name": "/api/agent/foods/import",
}
FOOD_TOOLS = [
    {
        "name": "search_external_foods",
        "description": "Search external food data from giveLunch agent API.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "name": {"type": "string"},
                "limit": {"type": "integer", "minimum": 1},
            },
            "required": ["name"],
        },
    },
    {
        "name": "save_foods",
        "description": "Save FoodAndNutritionDto items into giveLunch through the agent API.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "items": {
                    "type": "array",
                    "items": {"type": "object"},
                    "minItems": 1,
                }
            },
            "required": ["items"],
        },
    },
    {
        "name": "import_foods_by_name",
        "description": "Search and save foods by names through the agent API.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "names": {
                    "type": "array",
                    "items": {"type": "string"},
                    "minItems": 1,
                },
                "limitPerName": {"type": "integer", "minimum": 1},
            },
            "required": ["names"],
        },
    },
]


def get_base_url():
    return os.environ.get("GIVELUNCH_AGENT_BASE_URL", "http://localhost:8080").rstrip("/")


def get_api_key():
    return os.environ.get("GIVELUNCH_AGENT_API_KEY", "giveLunch")


class FoodApiClient:
    def __init__(self, base_url=None, api_key=None):
        self.base_url = (base_url or get_base_url()).rstrip("/")
        self.api_key = api_key if api_key is not None else get_api_key()

    def post_json(self, path, payload):
        if not self.api_key:
            raise RuntimeError("GIVELUNCH_AGENT_API_KEY is not set")
        request = urllib.request.Request(
            self.base_url + path,
            data=json.dumps(payload).encode("utf-8"),
            headers={
                "Content-Type": "application/json",
                "Authorization": f"Bearer {self.api_key}",
            },
            method="POST",
        )
        try:
            with urllib.request.urlopen(request) as response:
                charset = response.headers.get_content_charset() or "utf-8"
                return json.loads(response.read().decode(charset))
        except urllib.error.HTTPError as exc:
            body = exc.read().decode("utf-8", errors="replace")
            raise RuntimeError(f"HTTP {exc.code}: {body}") from exc
        except urllib.error.URLError as exc:
            raise RuntimeError(f"Request failed: {exc.reason}") from exc


class FoodToolExecutor:
    def __init__(self, api_client=None, recorder=None):
        self.api_client = api_client or FoodApiClient()
        self.recorder = recorder

    def call_tool(self, name, arguments):
        endpoint = FOOD_TOOL_ENDPOINTS.get(name)
        if endpoint is None:
            raise RuntimeError(f"Unknown tool: {name}")

        started_at = None
        finished_at = None
        tool_elapsed_ms = None
        if self.recorder is not None:
            from benchmark_support import utc_now

            started_at = utc_now()

        start_perf = time.perf_counter()
        result = self.api_client.post_json(endpoint, arguments)
        tool_elapsed_ms = (time.perf_counter() - start_perf) * 1000

        if self.recorder is not None:
            from benchmark_support import utc_now

            finished_at = utc_now()
            self.recorder.record_tool_call(name, arguments, result, started_at, finished_at, tool_elapsed_ms)
        return result
