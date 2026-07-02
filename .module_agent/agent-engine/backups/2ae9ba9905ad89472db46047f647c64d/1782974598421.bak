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


class SkillInfo:
    def __init__(self, data):
        self.name = data.get("name", "")
        self.description = data.get("description")
        self.prompt = data.get("prompt")
        skill_tools_data = data.get("skillTools")
        self.skill_tools = [ToolInfo(st) for st in skill_tools_data] if skill_tools_data else []


class VariableProxy:
    def __init__(self, data):
        self._data = dict(data or {})
        self._added = {}
        self._removed = []

    def __getitem__(self, key):
        return self._data[key]

    def __setitem__(self, key, value):
        self._data[key] = value
        self._added[key] = value
        if key in self._removed:
            self._removed.remove(key)

    def __delitem__(self, key):
        if key in self._data:
            del self._data[key]
        self._removed.append(key)
        if key in self._added:
            del self._added[key]

    def __contains__(self, key):
        return key in self._data

    def get(self, key, default=None):
        return self._data.get(key, default)

    def keys(self):
        return self._data.keys()

    def items(self):
        return self._data.items()

    def values(self):
        return self._data.values()

    def __iter__(self):
        return iter(self._data)

    def __len__(self):
        return len(self._data)

    def __repr__(self):
        return repr(self._data)

    def get_changes(self):
        return {"added": dict(self._added), "removed": list(self._removed)}


class AgentExecutionContext:
    def __init__(self, data):
        self.session_id = data.get("sessionId")
        self.agent_id = data.get("agentId")
        self.system_prompt = data.get("systemPrompt")
        self.model_id = data.get("modelId")
        self.recent_message_count = data.get("recentMessageCount")
        history_data = data.get("history")
        self.history = [HistoryEntry(h) for h in history_data] if history_data else []
        tools_data = data.get("tools")
        self.tools = [ToolInfo(t) for t in tools_data] if tools_data else []
        skills_data = data.get("skills")
        self.skills = [SkillInfo(s) for s in skills_data] if skills_data else []
        self.session_variables = data.get("sessionVariables") or {}
        self.conversation_variables = data.get("conversationVariables") or {}


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

    session_snapshot = dict(execute_input.context.session_variables or {})
    conversation_snapshot = dict(execute_input.context.conversation_variables or {})

    session_proxy = VariableProxy(session_snapshot)
    conversation_proxy = VariableProxy(conversation_snapshot)

    execute_input.context.session_variables = session_proxy
    execute_input.context.conversation_variables = conversation_proxy

    try:
        import index

        if not callable(getattr(index, "execute", None)):
            raise RuntimeError("index module does not export execute function")

        raw_result = index.execute(execute_input.context, execute_input.arguments)

        session_changes = session_proxy.get_changes()
        conversation_changes = conversation_proxy.get_changes()

        output = {
            "result": raw_result if isinstance(raw_result, str) else json.dumps(raw_result),
            "sessionVariables": session_changes,
            "conversationVariables": conversation_changes,
        }

        sys.stdout.write(json.dumps(output))
    except Exception as e:
        print(json.dumps({"error": str(e)}), file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
