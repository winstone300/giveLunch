# Benchmark Result Files

Use these files to explain benchmark results after a measured run.

## `summary.json`

Read this file first for the top-level result.

Important fields:

- `total_elapsed_ms`: end-to-end runtime for the measured task
- `tool_call_count`: number of food tool calls inside the run
- `tool_elapsed_ms_sum`: sum of measured tool times
- `agent_overhead_ms`: total runtime minus tool time sum
- `input_tokens`
- `output_tokens`
- `total_tokens`
- `token_mode`
- `savedCount`
- `duplicate_call_rate`

## `tool_calls.json`

Read this file when the user wants tool-level timing or duplicate-call analysis.

Important fields per entry:

- `tool_name`
- `started_at`
- `finished_at`
- `tool_elapsed_ms`
- `is_duplicate`
- `request_bytes`
- `response_bytes`
- `normalized_arguments`

## `result.json`

Read this file when the user wants the save/import result payload.

Important fields:

- `savedCount`
- `skippedCount`
- `failedCount`
- `result`

## `input.json`

Read this file when the user wants the benchmark setup or scenario metadata.

Important fields:

- `run_id`
- `scenario_name`
- `model`
- `prompt_version`
- `food_names`
- `limit`
- `cache_mode`
