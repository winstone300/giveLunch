import json
import tempfile
import unittest
from pathlib import Path

import benchmark_food_mcp_server as benchmark_server


class StubApiClient:
    def __init__(self):
        self.calls = []

    def post_json(self, path, payload):
        self.calls.append((path, payload))
        if path.endswith("/save"):
            return {"savedCount": 1, "skippedCount": 0, "failedCount": 0, "results": payload.get("items", [])}
        return {"results": [{"name": payload.get("name", "비빔밥")}]}


class BenchmarkFoodMcpServerTest(unittest.TestCase):
    def test_tools_list_includes_benchmark_tools(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            app = benchmark_server.create_app(api_client=StubApiClient(), benchmark_dir=Path(tmpdir))
            response = app.handle_request({"jsonrpc": "2.0", "id": 1, "method": "tools/list"})

        tool_names = [tool["name"] for tool in response["result"]["tools"]]
        self.assertEqual(
            tool_names,
            [
                "search_external_foods",
                "save_foods",
                "import_foods_by_name",
                "benchmark_start_run",
                "benchmark_finish_run",
            ],
        )

    def test_benchmark_server_records_tool_calls_and_summary(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            api_client = StubApiClient()
            app = benchmark_server.create_app(api_client=api_client, benchmark_dir=Path(tmpdir))

            start_response = app.handle_request({
                "jsonrpc": "2.0",
                "id": 1,
                "method": "tools/call",
                "params": {
                    "name": "benchmark_start_run",
                    "arguments": {
                        "run_id": "run-1",
                        "scenario_name": "demo",
                        "model": "gpt-5.4",
                        "prompt_version": "v1",
                        "cache_mode": "cold",
                    },
                },
            })
            self.assertFalse(start_response["result"]["isError"])

            app.handle_request({
                "jsonrpc": "2.0",
                "id": 2,
                "method": "tools/call",
                "params": {
                    "name": "search_external_foods",
                    "arguments": {"name": "비빔밥", "limit": 1},
                },
            })
            app.handle_request({
                "jsonrpc": "2.0",
                "id": 3,
                "method": "tools/call",
                "params": {
                    "name": "save_foods",
                    "arguments": {"items": [{"name": "비빔밥"}]},
                },
            })

            finish_response = app.handle_request({
                "jsonrpc": "2.0",
                "id": 4,
                "method": "tools/call",
                "params": {
                    "name": "benchmark_finish_run",
                    "arguments": {
                        "run_id": "run-1",
                        "total_elapsed_ms": 500,
                        "input_tokens": 11,
                        "output_tokens": 7,
                        "result": {"savedCount": 1, "skippedCount": 0, "failedCount": 0},
                    },
                },
            })

            self.assertFalse(finish_response["result"]["isError"])
            run_dir = Path(tmpdir) / "run-1"
            tool_calls = json.loads((run_dir / "tool_calls.json").read_text(encoding="utf-8"))
            summary = json.loads((run_dir / "summary.json").read_text(encoding="utf-8"))

            self.assertEqual(len(tool_calls), 2)
            self.assertEqual(summary["token_mode"], "exact")
            self.assertEqual(summary["total_tokens"], 18)
            self.assertEqual(summary["savedCount"], 1)


if __name__ == "__main__":
    unittest.main()
