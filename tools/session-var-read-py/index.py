import json


def execute(context, arguments):
    try:
        data = json.loads(arguments) if isinstance(arguments, str) else arguments
        key = str(data.get("key", "")).strip()
        if not key:
            return json.dumps({"error": "key 参数不能为空"})

        value = None
        if hasattr(context, "session_variables") and context.session_variables is not None:
            value = context.session_variables.get(key)

        return json.dumps({"key": key, "value": value, "exists": value is not None})
    except json.JSONDecodeError as e:
        return json.dumps({"error": "arguments JSON 解析失败: " + str(e)})
    except Exception as e:
        return json.dumps({"error": str(e)})
