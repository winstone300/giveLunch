import json
import tempfile
import unittest
from pathlib import Path

import agent_food_mcp_server as server


class BenchmarkRecorderTest(unittest.TestCase):
    def test_normalize_value_trims_and_sorts(self):
        payload = {
            "b": "  비빔밥   세트 ",
            "a": [" 김치찌개 ", {"y": "  1  2 ", "x": " 값 "}],
        }

        normalized = server.normalize_value(payload)

        self.assertEqual(
            normalized,
            {
                "a": ["김치찌개", {"x": "값", "y": "1 2"}],
                "b": "비빔밥 세트",
            },
        )

    def test_duplicate_tool_call_is_recorded(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            recorder = server.BenchmarkRecorder(Path(tmpdir))
            recorder.start_run({
                "scenario_name": "duplicate-check",
                "model": "gpt-5.4",
                "prompt_version": "v1",
                "cache_mode": "cold",
            })

            recorder.record_tool_call(
                "search_external_foods",
                {"name": " 비빔밥 ", "limit": 1},
                [{"name": "비빔밥"}],
                "2026-03-26T00:00:00Z",
                "2026-03-26T00:00:01Z",
                100.0,
            )
            recorder.record_tool_call(
                "search_external_foods",
                {"limit": 1, "name": "비빔밥"},
                [{"name": "비빔밥"}],
                "2026-03-26T00:00:02Z",
                "2026-03-26T00:00:03Z",
                120.0,
            )

            tool_calls = json.loads((Path(tmpdir) / recorder.active_run.run_id / "tool_calls.json").read_text(encoding="utf-8"))
            self.assertEqual(len(tool_calls), 2)
            self.assertFalse(tool_calls[0]["is_duplicate"])
            self.assertTrue(tool_calls[1]["is_duplicate"])

    def test_finish_run_writes_summary_files(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            recorder = server.BenchmarkRecorder(Path(tmpdir))
            start = recorder.start_run({
                "run_id": "run-fixed",
                "scenario_name": "batch-10",
                "model": "gpt-5.4",
                "prompt_version": "v2",
                "food_names": ["비빔밥", "김치찌개"],
                "limit": 1,
                "cache_mode": "warm",
            })

            self.assertEqual(start["run_id"], "run-fixed")

            recorder.record_tool_call(
                "search_external_foods",
                {"name": "비빔밥", "limit": 1},
                [{"name": "비빔밥"}],
                "2026-03-26T00:00:00Z",
                "2026-03-26T00:00:01Z",
                100.0,
            )
            recorder.record_tool_call(
                "save_foods",
                {"items": [{"name": "비빔밥"}]},
                {"savedCount": 1, "skippedCount": 0, "failedCount": 0},
                "2026-03-26T00:00:02Z",
                "2026-03-26T00:00:03Z",
                150.0,
            )

            finished = recorder.finish_run({
                "run_id": "run-fixed",
                "total_elapsed_ms": 1000,
                "input_text": "비빔밥을 저장해줘",
                "output_text": "1건 저장 완료",
                "result": {
                    "savedCount": 1,
                    "skippedCount": 0,
                    "failedCount": 0,
                    "results": [{"name": "비빔밥", "status": "SAVED"}],
                },
            })

            run_dir = Path(finished["run_path"])
            self.assertTrue((run_dir / "input.json").exists())
            self.assertTrue((run_dir / "tool_calls.json").exists())
            self.assertTrue((run_dir / "result.json").exists())
            self.assertTrue((run_dir / "summary.json").exists())

            summary = json.loads((run_dir / "summary.json").read_text(encoding="utf-8"))
            self.assertEqual(summary["tool_call_count"], 2)
            self.assertEqual(summary["savedCount"], 1)
            self.assertEqual(summary["tool_elapsed_ms_sum"], 250.0)
            self.assertEqual(summary["agent_overhead_ms"], 750.0)
            self.assertEqual(summary["tool_calls_per_success"], 2.0)
            self.assertEqual(summary["duplicate_call_rate"], 0.0)
            self.assertEqual(summary["token_mode"], "estimated")
            self.assertIsNotNone(summary["total_tokens"])

    def test_rates_are_null_when_saved_count_is_zero(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            recorder = server.BenchmarkRecorder(Path(tmpdir))
            recorder.start_run({
                "scenario_name": "empty-result",
                "model": "gpt-5.4",
                "prompt_version": "v1",
                "cache_mode": "cold",
            })

            finished = recorder.finish_run({
                "total_elapsed_ms": 300,
                "result": {"savedCount": 0, "skippedCount": 1, "failedCount": 0, "results": []},
            })

            summary = finished["summary"]
            self.assertIsNone(summary["tokens_per_success"])
            self.assertIsNone(summary["tool_calls_per_success"])


if __name__ == "__main__":
    unittest.main()
