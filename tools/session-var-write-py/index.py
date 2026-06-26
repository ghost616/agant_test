import json


def execute(context, arguments):
    try:
        data = json.loads(arguments) if isinstance(arguments, str) else arguments
        key = str(data.get("key", "")).strip()
        value = str(data.get("value", ""))
        if not key:
            return json.dumps({"error": "key 参数不能为空"})

        if hasattr(context, "session_variables") and context.session_variables is not None:
            context.session_variables[key] = value
        else:
            return json.dumps({"error": "session_variables 不可用"})

        return json.dumps({"status": "ok"})
    except json.JSONDecodeError as e:
        return json.dumps({"error": "arguments JSON 解析失败: " + str(e)})
    except Exception as e:
        return json.dumps({"error": str(e)})
