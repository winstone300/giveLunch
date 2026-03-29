#!/usr/bin/env python3
import json
import math
import os
import re
import sys
import time
import urllib.error
import urllib.request
import uuid
from datetime import UTC, datetime
from pathlib import Path


PROTOCOL_VERSION = "2024-11-05"
SERVER_VERSION = "0.3.0"
SCRIPT_DIR = Path(__file__).resolve().parent
DEFAULT_BENCHMARK_DIR = SCRIPT_DIR / "benchmarks"
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
BENCHMARK_TOOLS = [
    {
        "name": "benchmark_start_run",
        "description": "Create a benchmark run folder and persist input.json for MCP agent measurements.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "run_id": {"type": "string"},
                "scenario_name": {"type": "string"},
                "model": {"type": "string"},
                "prompt_version": {"type": "string"},
                "food_names": {
                    "type": "array",
                    "items": {"type": "string"},
                },
                "limit": {"type": "integer", "minimum": 1},
                "cache_mode": {"type": "string"},
            },
            "required": ["scenario_name", "model", "prompt_version", "cache_mode"],
        },
    },
    {
        "name": "benchmark_finish_run",
        "description": "Persist result.json and summary.json for the active benchmark run.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "run_id": {"type": "string"},
                "total_elapsed_ms": {"type": "number", "minimum": 0},
                "input_tokens": {"type": "integer", "minimum": 0},
                "output_tokens": {"type": "integer", "minimum": 0},
                "total_tokens": {"type": "integer", "minimum": 0},
                "token_mode": {"type": "string"},
                "input_text": {"type": "string"},
                "output_text": {"type": "string"},
                "savedCount": {"type": "integer", "minimum": 0},
                "skippedCount": {"type": "integer", "minimum": 0},
                "failedCount": {"type": "integer", "minimum": 0},
                "result": {"type": "object"},
            },
            "required": ["total_elapsed_ms"],
        },
    },
]


def get_base_url():
    return os.environ.get("GIVELUNCH_AGENT_BASE_URL", "http://localhost:8080").rstrip("/")


def get_api_key():
    return os.environ.get("GIVELUNCH_AGENT_API_KEY", "giveLunch")


def get_benchmark_dir():
    return Path(os.environ.get("GIVELUNCH_AGENT_BENCHMARK_DIR", DEFAULT_BENCHMARK_DIR))


def utc_now():
    return datetime.now(UTC).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def write_json(path, payload):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def canonical_json(payload):
    return json.dumps(payload, ensure_ascii=False, sort_keys=True, separators=(",", ":"))


def normalize_value(value):
    if isinstance(value, str):
        return re.sub(r"\s+", " ", value).strip()
    if isinstance(value, list):
        return [normalize_value(item) for item in value]
    if isinstance(value, dict):
        return {key: normalize_value(value[key]) for key in sorted(value)}
    return value


def payload_size_bytes(payload):
    return len(canonical_json(payload).encode("utf-8"))


def to_non_negative_number(value, field_name):
    if value is None:
        return None
    try:
        number = float(value)
    except (TypeError, ValueError) as exc:
        raise RuntimeError(f"{field_name} must be a number") from exc
    if number < 0:
        raise RuntimeError(f"{field_name} must be >= 0")
    return number


def to_non_negative_int(value, field_name):
    number = to_non_negative_number(value, field_name)
    if number is None:
        return None
    if int(number) != number:
        raise RuntimeError(f"{field_name} must be an integer")
    return int(number)


def estimate_tokens_from_text(text):
    if text is None:
        return None
    normalized = str(text).strip()
    if not normalized:
        return 0
    return math.ceil(len(normalized.encode("utf-8")) / 4)


def safe_rate(numerator, denominator):
    if denominator in (None, 0):
        return None
    return round(numerator / denominator, 6)


class BenchmarkRun:
    def __init__(self, base_dir, arguments):
        run_id = arguments.get("run_id") or self._create_run_id()
        self.run_id = str(run_id)
        self.run_dir = Path(base_dir) / self.run_id
        if self.run_dir.exists():
            raise RuntimeError(f"Benchmark run already exists: {self.run_id}")

        self.run_dir.mkdir(parents=True, exist_ok=False)
        self.tool_calls = []
        self._seen_call_keys = set()
        self.input_payload = {
            "run_id": self.run_id,
            "scenario_name": arguments.get("scenario_name"),
            "model": arguments.get("model"),
            "prompt_version": arguments.get("prompt_version"),
            "food_names": arguments.get("food_names") or [],
            "limit": arguments.get("limit"),
            "cache_mode": arguments.get("cache_mode"),
            "started_at": utc_now(),
        }
        write_json(self.run_dir / "input.json", self.input_payload)
        write_json(self.run_dir / "tool_calls.json", self.tool_calls)

    @staticmethod
    def _create_run_id():
        timestamp = datetime.now(UTC).strftime("%Y%m%dT%H%M%SZ")
        return f"run-{timestamp}-{uuid.uuid4().hex[:8]}"

    def record_tool_call(self, tool_name, arguments, result, started_at, finished_at, tool_elapsed_ms):
        normalized_arguments = normalize_value(arguments)
        duplicate_key = f"{tool_name}:{canonical_json(normalized_arguments)}"
        is_duplicate = duplicate_key in self._seen_call_keys
        self._seen_call_keys.add(duplicate_key)

        entry = {
            "tool_name": tool_name,
            "arguments": arguments,
            "normalized_arguments": normalized_arguments,
            "started_at": started_at,
            "finished_at": finished_at,
            "tool_elapsed_ms": round(tool_elapsed_ms, 3),
            "is_duplicate": is_duplicate,
            "request_bytes": payload_size_bytes(arguments),
            "response_bytes": payload_size_bytes(result),
        }
        self.tool_calls.append(entry)
        write_json(self.run_dir / "tool_calls.json", self.tool_calls)

    def finish(self, arguments):
        total_elapsed_ms = to_non_negative_number(arguments.get("total_elapsed_ms"), "total_elapsed_ms")
        if total_elapsed_ms is None:
            raise RuntimeError("total_elapsed_ms is required")

        result_document = self._build_result_document(arguments)
        write_json(self.run_dir / "result.json", result_document)

        token_metrics = self._build_token_metrics(arguments)
        tool_elapsed_ms_sum = round(sum(item["tool_elapsed_ms"] for item in self.tool_calls), 3)
        tool_call_count = len(self.tool_calls)
        duplicate_tool_calls = sum(1 for item in self.tool_calls if item["is_duplicate"])
        saved_count = result_document["savedCount"]

        summary = {
            "run_id": self.run_id,
            "scenario_name": self.input_payload.get("scenario_name"),
            "total_elapsed_ms": round(total_elapsed_ms, 3),
            "tool_call_count": tool_call_count,
            "duplicate_tool_calls": duplicate_tool_calls,
            "tool_elapsed_ms_sum": tool_elapsed_ms_sum,
            "agent_overhead_ms": round(max(total_elapsed_ms - tool_elapsed_ms_sum, 0), 3),
            "input_tokens": token_metrics["input_tokens"],
            "output_tokens": token_metrics["output_tokens"],
            "total_tokens": token_metrics["total_tokens"],
            "token_mode": token_metrics["token_mode"],
            "savedCount": saved_count,
            "tokens_per_success": safe_rate(token_metrics["total_tokens"], saved_count)
            if token_metrics["total_tokens"] is not None else None,
            "tool_calls_per_success": safe_rate(tool_call_count, saved_count),
            "duplicate_call_rate": safe_rate(duplicate_tool_calls, tool_call_count),
            "finished_at": utc_now(),
        }
        write_json(self.run_dir / "summary.json", summary)
        return {
            "run_id": self.run_id,
            "run_path": str(self.run_dir),
            "input_path": str(self.run_dir / "input.json"),
            "tool_calls_path": str(self.run_dir / "tool_calls.json"),
            "result_path": str(self.run_dir / "result.json"),
            "summary_path": str(self.run_dir / "summary.json"),
            "summary": summary,
        }

    def _build_result_document(self, arguments):
        result_payload = arguments.get("result")
        saved_count = arguments.get("savedCount")
        skipped_count = arguments.get("skippedCount")
        failed_count = arguments.get("failedCount")

        if isinstance(result_payload, dict):
            if saved_count is None:
                saved_count = result_payload.get("savedCount")
            if skipped_count is None:
                skipped_count = result_payload.get("skippedCount")
            if failed_count is None:
                failed_count = result_payload.get("failedCount")

        return {
            "run_id": self.run_id,
            "savedCount": to_non_negative_int(saved_count if saved_count is not None else 0, "savedCount"),
            "skippedCount": to_non_negative_int(skipped_count if skipped_count is not None else 0, "skippedCount"),
            "failedCount": to_non_negative_int(failed_count if failed_count is not None else 0, "failedCount"),
            "result": result_payload,
        }

    def _build_token_metrics(self, arguments):
        input_tokens = to_non_negative_int(arguments.get("input_tokens"), "input_tokens")
        output_tokens = to_non_negative_int(arguments.get("output_tokens"), "output_tokens")
        total_tokens = to_non_negative_int(arguments.get("total_tokens"), "total_tokens")
        token_mode = arguments.get("token_mode")

        if input_tokens is not None or output_tokens is not None or total_tokens is not None:
            if input_tokens is not None and output_tokens is not None and total_tokens is None:
                total_tokens = input_tokens + output_tokens
            return {
                "input_tokens": input_tokens,
                "output_tokens": output_tokens,
                "total_tokens": total_tokens,
                "token_mode": token_mode or "exact",
            }

        input_text = arguments.get("input_text")
        output_text = arguments.get("output_text")
        if input_text is not None or output_text is not None:
            input_tokens = estimate_tokens_from_text(input_text or "")
            output_tokens = estimate_tokens_from_text(output_text or "")
            return {
                "input_tokens": input_tokens,
                "output_tokens": output_tokens,
                "total_tokens": input_tokens + output_tokens,
                "token_mode": token_mode or "estimated",
            }

        return {
            "input_tokens": None,
            "output_tokens": None,
            "total_tokens": None,
            "token_mode": token_mode or "unavailable",
        }


class BenchmarkRecorder:
    def __init__(self, base_dir, tracked_tool_names=None):
        self.base_dir = Path(base_dir)
        self.active_run = None
        self.tracked_tool_names = set(tracked_tool_names or FOOD_TOOL_NAMES)

    def start_run(self, arguments):
        if self.active_run is not None:
            raise RuntimeError(f"Benchmark run already active: {self.active_run.run_id}")
        self.active_run = BenchmarkRun(self.base_dir, arguments)
        return {
            "run_id": self.active_run.run_id,
            "run_path": str(self.active_run.run_dir),
            "input_path": str(self.active_run.run_dir / "input.json"),
            "tool_calls_path": str(self.active_run.run_dir / "tool_calls.json"),
        }

    def finish_run(self, arguments):
        if self.active_run is None:
            raise RuntimeError("No active benchmark run")
        requested_run_id = arguments.get("run_id")
        if requested_run_id is not None and str(requested_run_id) != self.active_run.run_id:
            raise RuntimeError(
                f"Active benchmark run is {self.active_run.run_id}, not {requested_run_id}"
            )
        finished = self.active_run.finish(arguments)
        self.active_run = None
        return finished

    def record_tool_call(self, tool_name, arguments, result, started_at, finished_at, tool_elapsed_ms):
        if self.active_run is None or tool_name not in self.tracked_tool_names:
            return
        self.active_run.record_tool_call(tool_name, arguments, result, started_at, finished_at, tool_elapsed_ms)


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

        started_at = utc_now()
        start_perf = time.perf_counter()
        result = self.api_client.post_json(endpoint, arguments)
        finished_at = utc_now()
        tool_elapsed_ms = (time.perf_counter() - start_perf) * 1000
        if self.recorder is not None:
            self.recorder.record_tool_call(name, arguments, result, started_at, finished_at, tool_elapsed_ms)
        return result


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


class MCPServerApp:
    def __init__(self, server_name, tools, tool_handler, version=SERVER_VERSION):
        self.server_name = server_name
        self.tools = tools
        self.tool_handler = tool_handler
        self.version = version

    def handle_request(self, message):
        method = message.get("method")
        message_id = message.get("id")
        params = message.get("params", {})

        if method == "initialize":
            return success_response(message_id, {
                "protocolVersion": PROTOCOL_VERSION,
                "capabilities": {"tools": {}},
                "serverInfo": {"name": self.server_name, "version": self.version},
            })

        if method == "notifications/initialized":
            return None

        if method == "tools/list":
            return success_response(message_id, {"tools": self.tools})

        if method == "tools/call":
            name = params.get("name")
            arguments = params.get("arguments", {})
            try:
                result = self.tool_handler(name, arguments)
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

    def serve(self, input_stream=None, output_stream=None):
        input_stream = input_stream or sys.stdin
        output_stream = output_stream or sys.stdout
        for raw_line in input_stream:
            line = raw_line.strip()
            if not line:
                continue
            try:
                message = json.loads(line)
            except json.JSONDecodeError as exc:
                response = error_response(None, -32700, f"Invalid JSON: {exc}")
            else:
                response = self.handle_request(message)

            if response is not None:
                output_stream.write(json.dumps(response, ensure_ascii=False) + "\n")
                output_stream.flush()
