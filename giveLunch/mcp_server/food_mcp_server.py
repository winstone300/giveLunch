#!/usr/bin/env python3
from food_mcp_common import FOOD_TOOLS, FoodToolExecutor
from mcp_protocol import MCPServerApp


SERVER_NAME = "givelunch-foods"


def create_app(api_client=None):
    executor = FoodToolExecutor(api_client=api_client)
    return MCPServerApp(SERVER_NAME, FOOD_TOOLS, executor.call_tool)


def handle_request(message, api_client=None):
    return create_app(api_client=api_client).handle_request(message)


def main():
    create_app().serve()


if __name__ == "__main__":
    main()
