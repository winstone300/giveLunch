import unittest

import benchmark_food_mcp_runner as runner


class FakeOpenAIClient:
    def __init__(self, responses):
        self.responses = list(responses)
        self.payloads = []

    def create_response(self, payload):
        self.payloads.append(payload)
        return self.responses.pop(0)


class FakeMcpClient:
    def __init__(self):
        self.calls = []

    def call_tool(self, name, arguments):
        self.calls.append((name, arguments))
        if name == "benchmark_start_run":
            return {"run_id": "run-1"}
        if name == "search_external_foods":
            return {"results": [{"name": arguments["name"]}]}
        if name == "benchmark_finish_run":
            return {"summary": arguments}
        raise AssertionError(f"Unexpected tool call: {name}")


class BenchmarkFoodMcpRunnerTest(unittest.TestCase):
    def test_runner_accumulates_exact_usage_across_responses(self):
        openai_client = FakeOpenAIClient([
            {
                "id": "resp-1",
                "usage": {"input_tokens": 100, "output_tokens": 20, "total_tokens": 120},
                "output": [
                    {
                        "type": "function_call",
                        "id": "call-1",
                        "call_id": "call-1",
                        "name": "search_external_foods",
                        "arguments": "{\"name\":\"비빔밥\",\"limit\":1}",
                    }
                ],
            },
            {
                "id": "resp-2",
                "usage": {"input_tokens": 40, "output_tokens": 10, "total_tokens": 50},
                "output": [
                    {
                        "type": "message",
                        "content": [{"type": "output_text", "text": "1건 저장 완료"}],
                    }
                ],
            },
        ])
        mcp_client = FakeMcpClient()

        benchmark_runner = runner.BenchmarkAgentRunner(openai_client, mcp_client)
        benchmark_runner.run(
            prompt="비빔밥을 저장해줘",
            scenario_name="demo",
            model="gpt-5.4",
            prompt_version="v1",
            food_names=["비빔밥"],
            limit=1,
        )

        finish_call = mcp_client.calls[-1]
        self.assertEqual(finish_call[0], "benchmark_finish_run")
        self.assertEqual(finish_call[1]["input_tokens"], 140)
        self.assertEqual(finish_call[1]["output_tokens"], 30)
        self.assertEqual(finish_call[1]["total_tokens"], 170)
        self.assertEqual(finish_call[1]["token_mode"], "exact")

    def test_runner_falls_back_to_estimated_tokens_when_usage_is_missing(self):
        openai_client = FakeOpenAIClient([
            {
                "id": "resp-1",
                "output": [
                    {
                        "type": "message",
                        "content": [{"type": "output_text", "text": "저장 완료"}],
                    }
                ],
            }
        ])
        mcp_client = FakeMcpClient()

        benchmark_runner = runner.BenchmarkAgentRunner(openai_client, mcp_client)
        benchmark_runner.run(
            prompt="비빔밥 저장",
            scenario_name="demo",
            model="gpt-5.4",
            prompt_version="v1",
            food_names=["비빔밥"],
        )

        finish_call = mcp_client.calls[-1]
        self.assertEqual(finish_call[0], "benchmark_finish_run")
        self.assertEqual(finish_call[1]["token_mode"], "estimated")
        self.assertEqual(finish_call[1]["input_text"], "비빔밥 저장")
        self.assertEqual(finish_call[1]["output_text"], "저장 완료")


if __name__ == "__main__":
    unittest.main()
