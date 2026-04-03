#!/usr/bin/env python3
import json
import sys


PROTOCOL_VERSION = "2024-11-05"
SERVER_VERSION = "0.3.0"


def success_response(message_id, result):
    return {
        "jsonrpc": "2.0",
        "id": message_id,
        "result": result,
    }


def error_response(message_id, code, message):
    return {
        "jsonrpc": "2.0",
        "id": message_id,
        "error": {
            "code": code,
            "message": message,
        },
    }


class MCPServerApp:
    def __init__(self, server_name, tools, tool_handler, version=SERVER_VERSION):
        self.server_name = server_name
        self.tools = tools
        self.tool_handler = tool_handler
        self.version = version

    def handle_request(self, message):
        method = message.get("method")
        message_id = message.get("id")
        params = message.get("params", {})

        if method == "initialize":
            return success_response(message_id, {
                "protocolVersion": PROTOCOL_VERSION,
                "capabilities": {"tools": {}},
                "serverInfo": {"name": self.server_name, "version": self.version},
            })

        if method == "notifications/initialized":
            return None

        if method == "tools/list":
            return success_response(message_id, {"tools": self.tools})

        if method == "tools/call":
            name = params.get("name")
            arguments = params.get("arguments", {})
            try:
                result = self.tool_handler(name, arguments)
                return success_response(message_id, {
                    "content": [
                        {"type": "text", "text": json.dumps(result, ensure_ascii=False, indent=2)}
                    ],
                    "structuredContent": result,
                    "isError": False,
                })
            except Exception as exc:
                return success_response(message_id, {
                    "content": [{"type": "text", "text": str(exc)}],
                    "isError": True,
                })

        return error_response(message_id, -32601, f"Method not found: {method}")

    def serve(self, input_stream=None, output_stream=None):
        input_stream = input_stream or sys.stdin
        output_stream = output_stream or sys.stdout
        for raw_line in input_stream:
            line = raw_line.strip()
            if not line:
                continue
            try:
                message = json.loads(line)
            except json.JSONDecodeError as exc:
                response = error_response(None, -32700, f"Invalid JSON: {exc}")
            else:
                response = self.handle_request(message)

            if response is not None:
                output_stream.write(json.dumps(response, ensure_ascii=False) + "\n")
                output_stream.flush()
