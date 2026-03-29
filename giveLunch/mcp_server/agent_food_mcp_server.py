#!/usr/bin/env python3
from benchmark_food_mcp_server import SERVER_NAME, create_app, handle_request, main
from food_mcp_common import (
    BENCHMARK_TOOLS,
    FOOD_TOOLS,
    BenchmarkRecorder,
    BenchmarkRun,
    FoodApiClient,
    FoodToolExecutor,
    MCPServerApp,
    canonical_json,
    error_response,
    estimate_tokens_from_text,
    get_api_key,
    get_base_url,
    get_benchmark_dir,
    normalize_value,
    payload_size_bytes,
    safe_rate,
    success_response,
    to_non_negative_int,
    to_non_negative_number,
    utc_now,
    write_json,
)


TOOLS = FOOD_TOOLS + BENCHMARK_TOOLS


if __name__ == "__main__":
    main()
