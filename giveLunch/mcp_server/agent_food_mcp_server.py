#!/usr/bin/env python3
import json
import os
import sys
import urllib.error
import urllib.request


BASE_URL = os.environ.get("GIVELUNCH_AGENT_BASE_URL", "http://localhost:8080").rstrip("/")
API_KEY = os.environ.get("GIVELUNCH_AGENT_API_KEY", "")
SERVER_NAME = "givelunch-agent-foods"
PROTOCOL_VERSION = "2024-11-05"

#도구 목록 정의
TOOLS = [
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
        "description": "Search and save foods by names through the giveLunch agent API.",
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

#백엔드 API에 POST요청
def post_json(path, payload):
    if not API_KEY:
        raise RuntimeError("GIVELUNCH_AGENT_API_KEY is not set")
    request = urllib.request.Request(
        BASE_URL + path,
        data=json.dumps(payload).encode("utf-8"),
        headers={
            "Content-Type": "application/json",
            "Authorization": f"Bearer {API_KEY}",
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

#도구와 해당 API 매핑
def call_tool(name, arguments):
    if name == "search_external_foods":
        return post_json("/api/agent/foods/search-external", arguments)
    if name == "save_foods":
        return post_json("/api/agent/foods/save", arguments)
    if name == "import_foods_by_name":
        return post_json("/api/agent/foods/import", arguments)
    raise RuntimeError(f"Unknown tool: {name}")


def success_response(message_id, result):
    return {
        "jsonrpc": "2.0",
        "id": message_id,
        "result": result,
    }


def error_response(message_id, code, message):
    return {
        "jsonrpc": "2.0",
        "id": message_id,
        "error": {
            "code": code,
            "message": message,
        },
    }


def handle_request(message):
    method = message.get("method")
    message_id = message.get("id")
    params = message.get("params", {})

    if method == "initialize":
        return success_response(message_id, {
            "protocolVersion": PROTOCOL_VERSION,
            "capabilities": {"tools": {}},
            "serverInfo": {"name": SERVER_NAME, "version": "0.1.0"},
        })

    if method == "notifications/initialized":
        return None

    if method == "tools/list":
        return success_response(message_id, {"tools": TOOLS})

    if method == "tools/call":
        name = params.get("name")
        arguments = params.get("arguments", {})
        try:
            result = call_tool(name, arguments)
            return success_response(message_id, {
                "content": [
                    {"type": "text", "text": json.dumps(result, ensure_ascii=False, indent=2)}
                ],
                "structuredContent": result,
                "isError": False,
            })
        except Exception as exc:
            return success_response(message_id, {
                "content": [{"type": "text", "text": str(exc)}],
                "isError": True,
            })

    return error_response(message_id, -32601, f"Method not found: {method}")


def main():
    for raw_line in sys.stdin:
        line = raw_line.strip()
        if not line:
            continue
        try:
            message = json.loads(line)
        except json.JSONDecodeError as exc:
            response = error_response(None, -32700, f"Invalid JSON: {exc}")
        else:
            response = handle_request(message)

        if response is not None:
            sys.stdout.write(json.dumps(response, ensure_ascii=False) + "\n")
            sys.stdout.flush()


if __name__ == "__main__":
    main()
