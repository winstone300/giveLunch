#!/usr/bin/env python3
import argparse
import json
import os
import shlex
import subprocess
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

from benchmark_support import estimate_tokens_from_text
from food_mcp_common import FOOD_TOOLS

#OpenAI Responses API를 끼워서 benchmark를 실행

class UsageAccumulator:
    def __init__(self):
        self.input_tokens = 0
        self.output_tokens = 0
        self.total_tokens = 0
        self._has_usage = False

    #OpenAI 응답의 usage 정보를 누적
    def add(self, usage):
        if not isinstance(usage, dict):
            return

        input_tokens = usage.get("input_tokens")
        output_tokens = usage.get("output_tokens")
        total_tokens = usage.get("total_tokens")
        if input_tokens is None and output_tokens is None and total_tokens is None:
            return

        self._has_usage = True
        self.input_tokens += int(input_tokens or 0)
        self.output_tokens += int(output_tokens or 0)
        self.total_tokens += int(
            total_tokens if total_tokens is not None else (input_tokens or 0) + (output_tokens or 0)
        )

    @property
    def has_usage(self):
        return self._has_usage

    def as_finish_arguments(self):
        return {
            "input_tokens": self.input_tokens,
            "output_tokens": self.output_tokens,
            "total_tokens": self.total_tokens,
            "token_mode": "exact",
        }


def convert_tools_to_openai_functions(tools):
    return [
        {
            "type": "function",
            "name": tool["name"],
            "description": tool["description"],
            "parameters": tool["inputSchema"],
        }
        for tool in tools
    ]

#OpenAI 응답에서 최종 텍스트만 추출
def extract_output_text(response):
    if not isinstance(response, dict):
        return ""

    if isinstance(response.get("output_text"), str):
        return response["output_text"]

    parts = []
    for item in response.get("output", []):
        if not isinstance(item, dict):
            continue
        if item.get("type") in {"output_text", "text"} and isinstance(item.get("text"), str):
            parts.append(item["text"])
        for content in item.get("content", []):
            if isinstance(content, dict) and isinstance(content.get("text"), str):
                parts.append(content["text"])
    return "\n".join(part for part in parts if part).strip()


def extract_function_calls(response):
    calls = []
    for item in response.get("output", []):
        if not isinstance(item, dict):
            continue
        if item.get("type") not in {"function_call", "tool_call"}:
            continue

        raw_arguments = item.get("arguments") or "{}"
        arguments = json.loads(raw_arguments) if isinstance(raw_arguments, str) else raw_arguments
        calls.append({
            "name": item["name"],
            "arguments": arguments,
            "call_id": item.get("call_id") or item.get("id"),
        })
    return calls


def extract_result_payload(tool_results, fallback_text):
    for result in reversed(tool_results):
        if isinstance(result, dict) and any(key in result for key in ("savedCount", "skippedCount", "failedCount")):
            return result
    if fallback_text:
        return {"message": fallback_text}
    return None


class OpenAIResponsesClient:
    def __init__(self, api_key=None, base_url=None, timeout_seconds=60):
        self.api_key = api_key or os.environ.get("OPENAI_API_KEY")
        self.base_url = (base_url or os.environ.get("OPENAI_BASE_URL") or "https://api.openai.com/v1").rstrip("/")
        self.timeout_seconds = timeout_seconds

    #OpenAI Responses API 호출
    def create_response(self, payload):
        if not self.api_key:
            raise RuntimeError("OPENAI_API_KEY is not set")

        request = urllib.request.Request(
            self.base_url + "/responses",
            data=json.dumps(payload).encode("utf-8"),
            headers={
                "Authorization": f"Bearer {self.api_key}",
                "Content-Type": "application/json",
            },
            method="POST",
        )
        try:
            with urllib.request.urlopen(request, timeout=self.timeout_seconds) as response:
                return json.loads(response.read().decode("utf-8"))
        except urllib.error.HTTPError as exc:
            body = exc.read().decode("utf-8", errors="replace")
            raise RuntimeError(f"OpenAI Responses API error {exc.code}: {body}") from exc
        except urllib.error.URLError as exc:
            raise RuntimeError(f"OpenAI Responses API request failed: {exc.reason}") from exc


class BenchmarkMcpClient:
    def __init__(self, command):
        self.command = command
        self.process = None
        self._next_id = 1

    #MCP 서버 start(with 문 진입 시 자동 실행)
    def __enter__(self):
        self.start()
        return self

    def __exit__(self, exc_type, exc, tb):
        self.close()
        return False

    #MCP 서버 프로세스 실행 (subprocess)
    def start(self):
        if self.process is not None:
            return

        self.process = subprocess.Popen(
            self.command,
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            encoding="utf-8",
        )
        self.request("initialize", {})
        self.notify("notifications/initialized", {})

    #MCP 서버 프로세스 종료 처리
    def close(self):
        if self.process is None:
            return
        if self.process.stdin:
            self.process.stdin.close()
        if self.process.poll() is None:
            self.process.terminate()
            try:
                self.process.wait(timeout=5)
            except subprocess.TimeoutExpired:
                self.process.kill()
                self.process.wait(timeout=5)
        self.process = None

    def notify(self, method, params):
        self._write({"jsonrpc": "2.0", "method": method, "params": params})

    def request(self, method, params):
        message_id = self._next_id
        self._next_id += 1
        self._write({
            "jsonrpc": "2.0",
            "id": message_id,
            "method": method,
            "params": params,
        })
        return self._read_response(message_id)

    def call_tool(self, name, arguments):
        response = self.request("tools/call", {"name": name, "arguments": arguments})
        result = response["result"]
        if result.get("isError"):
            raise RuntimeError(result["content"][0]["text"])
        return result.get("structuredContent")

    def _write(self, message):
        if self.process is None or self.process.stdin is None:
            raise RuntimeError("Benchmark MCP process is not running")
        self.process.stdin.write(json.dumps(message, ensure_ascii=False) + "\n")
        self.process.stdin.flush()

    def _read_response(self, message_id):
        if self.process is None or self.process.stdout is None:
            raise RuntimeError("Benchmark MCP process is not running")

        while True:
            line = self.process.stdout.readline()
            if not line:
                stderr = self.process.stderr.read() if self.process.stderr else ""
                raise RuntimeError(f"Benchmark MCP process exited unexpectedly. {stderr}".strip())
            response = json.loads(line)
            if response.get("id") == message_id:
                return response


class BenchmarkAgentRunner:
    def __init__(self, openai_client, mcp_client):
        self.openai_client = openai_client
        self.mcp_client = mcp_client
        self.openai_tools = convert_tools_to_openai_functions(FOOD_TOOLS)

    def run(self, *, prompt, scenario_name, model, prompt_version, food_names, limit=None, cache_mode="cold"):
        started_at = time.perf_counter()
        benchmark_start = self.mcp_client.call_tool("benchmark_start_run", {
            "scenario_name": scenario_name,
            "model": model,
            "prompt_version": prompt_version,
            "food_names": food_names,
            "limit": limit,
            "cache_mode": cache_mode,
        })

        usage = UsageAccumulator()
        tool_results = []
        response = self.openai_client.create_response({
            "model": model,
            "input": prompt,
            "tools": self.openai_tools,
        })
        final_text = ""

        while True:
            usage.add(response.get("usage"))
            response_text = extract_output_text(response)
            if response_text:
                final_text = response_text

            function_calls = extract_function_calls(response)
            if not function_calls:
                break

            function_outputs = []
            for call in function_calls:
                tool_result = self.mcp_client.call_tool(call["name"], call["arguments"])
                tool_results.append(tool_result)
                function_outputs.append({
                    "type": "function_call_output",
                    "call_id": call["call_id"],
                    "output": json.dumps(tool_result, ensure_ascii=False),
                })

            response = self.openai_client.create_response({
                "model": model,
                "previous_response_id": response.get("id"),
                "input": function_outputs,
            })

        finish_arguments = {
            "run_id": benchmark_start["run_id"],
            "total_elapsed_ms": round((time.perf_counter() - started_at) * 1000, 3),
            "result": extract_result_payload(tool_results, final_text),
        }
        if usage.has_usage:
            finish_arguments.update(usage.as_finish_arguments())
        else:
            finish_arguments.update({
                "input_text": prompt,
                "output_text": final_text,
                "token_mode": "estimated" if prompt or final_text else "unavailable",
            })

        benchmark_finish = self.mcp_client.call_tool("benchmark_finish_run", finish_arguments)
        return {
            "benchmark": benchmark_finish,
            "final_response": response,
            "final_text": final_text,
            "estimated_total_tokens": None if usage.has_usage else estimate_tokens_from_text(prompt) + estimate_tokens_from_text(final_text),
        }


def parse_args(argv=None):
    parser = argparse.ArgumentParser(description="Run benchmark scenarios against the benchmark food MCP server.")
    parser.add_argument("--scenario-name", required=True)
    parser.add_argument("--model", required=True)
    parser.add_argument("--prompt-version", required=True)
    parser.add_argument("--prompt", required=True)
    parser.add_argument("--food-name", dest="food_names", action="append", default=[])
    parser.add_argument("--limit", type=int)
    parser.add_argument("--cache-mode", default="cold")
    parser.add_argument("--openai-api-key")
    parser.add_argument("--openai-base-url")
    parser.add_argument("--benchmark-server-command")
    return parser.parse_args(argv)


def default_benchmark_server_command():
    return [sys.executable, str(Path(__file__).resolve().with_name("benchmark_food_mcp_server.py"))]


def main(argv=None):
    args = parse_args(argv)
    command = shlex.split(args.benchmark_server_command, posix=False) if args.benchmark_server_command else default_benchmark_server_command()
    openai_client = OpenAIResponsesClient(
        api_key=args.openai_api_key,
        base_url=args.openai_base_url,
    )

    with BenchmarkMcpClient(command) as mcp_client:
        runner = BenchmarkAgentRunner(openai_client, mcp_client)
        result = runner.run(
            prompt=args.prompt,
            scenario_name=args.scenario_name,
            model=args.model,
            prompt_version=args.prompt_version,
            food_names=args.food_names,
            limit=args.limit,
            cache_mode=args.cache_mode,
        )

    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
