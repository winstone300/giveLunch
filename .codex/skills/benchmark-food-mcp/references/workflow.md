# Benchmark Workflow

Use this workflow for measured food operations in this repository.

## Relevant project files

- Benchmark server: `D:\portPolio\giveLunch\giveLunch\mcp_server\benchmark_food_mcp_server.py`
- Normal food server: `D:\portPolio\giveLunch\giveLunch\mcp_server\food_mcp_server.py`
- Benchmark runner: `D:\portPolio\giveLunch\giveLunch\mcp_server\benchmark_food_mcp_runner.py`
- Benchmark docs: `D:\portPolio\giveLunch\giveLunch\mcp_server\README.md`

## Default artifact location

- `D:\portPolio\giveLunch\giveLunch\mcp_server\benchmarks\<run_id>\`

## Required benchmark order

1. Call `benchmark_start_run`.
2. Execute the measured food task with one or more of:
   - `search_external_foods`
   - `save_foods`
   - `import_foods_by_name`
3. Call `benchmark_finish_run`.

## Metadata defaults

- `prompt_version`: `v1`
- `cache_mode`: `cold`
- `scenario_name`: short stable identifier
- `food_names`: include targeted food names when available
- `limit`: include only when the task or scenario needs it

## Exact vs estimated tokens

- Prefer exact usage when the runtime exposes model token counts.
- Otherwise provide `input_text`, `output_text`, and `token_mode: "estimated"`.
- Do not claim exact token values without runtime usage data.
