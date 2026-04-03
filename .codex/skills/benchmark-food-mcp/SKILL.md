---
name: benchmark-food-mcp
description: Measure end-to-end food search, save, and import work through the giveLunch benchmark MCP workflow. Use when Codex should call `benchmark_start_run`, execute food MCP tool calls, finish with `benchmark_finish_run`, and explain benchmark artifacts such as `summary.json` and `tool_calls.json`.
---

# Benchmark Food MCP

Use this skill when the user wants measured execution, not an ordinary food search, save, or import task.

Read `references/workflow.md` before starting a benchmarked task.
Read `references/result-files.md` when the user asks for metric interpretation or artifact analysis.

## Workflow

1. Use the benchmark MCP path, not the plain food MCP path.
2. Call `benchmark_start_run` before any measured food work.
3. Fill benchmark metadata with these defaults unless the user provides better values:
   - `prompt_version`: `v1`
   - `cache_mode`: `cold`
   - `scenario_name`: a short stable name such as `save-bibimbap-1`
4. Include `food_names` when the target food names are known.
5. Perform the actual work with these tools as needed:
   - `search_external_foods`
   - `save_foods`
   - `import_foods_by_name`
6. Treat the benchmark as one task-level run, not one run per tool call.
7. Always call `benchmark_finish_run` after the task, even if the result is partial or failed.

## Token Policy

- If the runtime exposes exact model usage, pass:
  - `input_tokens`
  - `output_tokens`
  - `total_tokens`
  - `token_mode: "exact"`
- If exact usage is not available, pass:
  - `input_text`
  - `output_text`
  - `token_mode: "estimated"`
- If neither usage nor text is available, finish with `token_mode: "unavailable"`.

## Failure Handling

- If `benchmark_start_run` fails, stop and report that measured execution could not begin.
- If a food tool fails after a run has started, still call `benchmark_finish_run` with:
  - the elapsed time so far
  - partial `result` when available
  - `failedCount` when known
- Never skip `benchmark_finish_run` after a successful start.

## Result Reporting

After the benchmark completes, report:

- whether the run completed successfully
- the run directory or artifact paths when available
- the key summary metrics:
  - `total_elapsed_ms`
  - `tool_call_count`
  - `tool_elapsed_ms_sum`
  - `agent_overhead_ms`
  - `input_tokens`
  - `output_tokens`
  - `total_tokens`
  - `token_mode`

Prefer quoting values from `summary.json` instead of paraphrasing from memory.
