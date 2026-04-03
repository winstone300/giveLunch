#!/usr/bin/env python3
from benchmark_support import BENCHMARK_TOOLS, BenchmarkRecorder, get_benchmark_dir
from food_mcp_common import FOOD_TOOLS, FoodToolExecutor
from mcp_protocol import MCPServerApp


SERVER_NAME = "givelunch-benchmark-foods"


class BenchmarkFoodToolHandler:
    def __init__(self, api_client=None, benchmark_dir=None):
        self.recorder = BenchmarkRecorder(benchmark_dir or get_benchmark_dir())
        self.food_executor = FoodToolExecutor(api_client=api_client, recorder=self.recorder)

    def call_tool(self, name, arguments):
        if name == "benchmark_start_run":
            return self.recorder.start_run(arguments)
        if name == "benchmark_finish_run":
            return self.recorder.finish_run(arguments)
        return self.food_executor.call_tool(name, arguments)


def create_app(api_client=None, benchmark_dir=None):
    handler = BenchmarkFoodToolHandler(api_client=api_client, benchmark_dir=benchmark_dir)
    app = MCPServerApp(SERVER_NAME, FOOD_TOOLS + BENCHMARK_TOOLS, handler.call_tool)
    app.tool_handler_instance = handler
    return app


def handle_request(message, api_client=None, benchmark_dir=None):
    return create_app(api_client=api_client, benchmark_dir=benchmark_dir).handle_request(message)


def main():
    create_app().serve()


if __name__ == "__main__":
    main()
