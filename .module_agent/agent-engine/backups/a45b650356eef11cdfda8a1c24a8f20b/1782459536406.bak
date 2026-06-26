# _runner.py - Auto-generated bridge file for PythonToolInvoker

import json
import os
import sys


class ToolCall:
    def __init__(self, data):
        self.id = data.get("id")
        self.name = data.get("name")
        self.arguments = data.get("arguments")


class HistoryEntry:
    def __init__(self, data):
        self.role = data.get("role", "")
        self.content = data.get("content")
        self.reasoning = data.get("reasoning")
        self.tool_call_id = data.get("toolCallId")
        self.sequence_num = data.get("sequenceNum", 0)
        self.create_time = data.get("createTime")
        tool_calls_data = data.get("toolCalls")
        self.tool_calls = [ToolCall(tc) for tc in tool_calls_data] if tool_calls_data else []


class ToolInfo:
    def __init__(self, data):
        self.name = data.get("name", "")
        self.description = data.get("description")
        self.parameter_schema = data.get("parameterSchema")


class AgentExecutionContext:
    def __init__(self, data):
        self.session_id = data.get("sessionId")
        self.agent_id = data.get("agentId")
        self.system_prompt = data.get("systemPrompt")
        self.model_id = data.get("modelId")
        history_data = data.get("history")
        self.history = [HistoryEntry(h) for h in history_data] if history_data else []
        tools_data = data.get("tools")
        self.tools = [ToolInfo(t) for t in tools_data] if tools_data else []


class ExecuteInput:
    def __init__(self, data):
        self.context = AgentExecutionContext(data.get("context", {}))
        self.arguments = data.get("arguments", "")


def main():
    if len(sys.argv) < 2:
        print(json.dumps({"error": "Missing input file path argument"}), file=sys.stderr)
        sys.exit(1)

    input_path = sys.argv[1]
    try:
        with open(input_path, "r", encoding="utf-8") as f:
            json_str = f.read()
    except OSError as e:
        print(json.dumps({"error": "Failed to read input file: " + str(e)}), file=sys.stderr)
        sys.exit(1)

    try:
        raw_data = json.loads(json_str)
    except json.JSONDecodeError as e:
        print(json.dumps({"error": "JSON parse error: " + str(e)}), file=sys.stderr)
        sys.exit(1)

    execute_input = ExecuteInput(raw_data)

    try:
        import index

        if not callable(getattr(index, "execute", None)):
            raise RuntimeError("index module does not export execute function")

        result = index.execute(execute_input.context, execute_input.arguments)

        if isinstance(result, str):
            sys.stdout.write(result)
        else:
            sys.stdout.write(json.dumps(result))
    except Exception as e:
        print(json.dumps({"error": str(e)}), file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
