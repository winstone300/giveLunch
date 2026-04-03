import unittest

import food_mcp_server as food_server


class StubApiClient:
    def __init__(self):
        self.calls = []

    def post_json(self, path, payload):
        self.calls.append((path, payload))
        return {"ok": True, "path": path, "payload": payload}


class FoodMcpServerTest(unittest.TestCase):
    def test_tools_list_excludes_benchmark_tools(self):
        app = food_server.create_app(api_client=StubApiClient())
        response = app.handle_request({"jsonrpc": "2.0", "id": 1, "method": "tools/list"})

        tool_names = [tool["name"] for tool in response["result"]["tools"]]
        self.assertEqual(
            tool_names,
            ["search_external_foods", "save_foods", "import_foods_by_name", "bulk_import_foods_by_name"],
        )

    def test_food_tool_calls_backend_without_benchmark_tools(self):
        api_client = StubApiClient()
        app = food_server.create_app(api_client=api_client)

        response = app.handle_request({
            "jsonrpc": "2.0",
            "id": 2,
            "method": "tools/call",
            "params": {
                "name": "search_external_foods",
                "arguments": {"name": "비빔밥", "limit": 3},
            },
        })

        self.assertFalse(response["result"]["isError"])
        self.assertEqual(api_client.calls, [
            ("/api/agent/foods/search-external", {"name": "비빔밥", "limit": 3})
        ])
        self.assertEqual(response["result"]["structuredContent"]["path"], "/api/agent/foods/search-external")

    def test_bulk_import_tool_calls_backend(self):
        api_client = StubApiClient()
        app = food_server.create_app(api_client=api_client)

        response = app.handle_request({
            "jsonrpc": "2.0",
            "id": 3,
            "method": "tools/call",
            "params": {
                "name": "bulk_import_foods_by_name",
                "arguments": {"names": ["비빔밥", "김치찌개"], "limitPerName": 1},
            },
        })

        self.assertFalse(response["result"]["isError"])
        self.assertEqual(api_client.calls, [
            ("/api/agent/foods/bulk-import", {"names": ["비빔밥", "김치찌개"], "limitPerName": 1})
        ])
        self.assertEqual(response["result"]["structuredContent"]["path"], "/api/agent/foods/bulk-import")


if __name__ == "__main__":
    unittest.main()
