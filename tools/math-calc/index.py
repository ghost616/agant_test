import json
import math


def execute(context, arguments):
    try:
        data = json.loads(arguments) if isinstance(arguments, str) else arguments
        formula = str(data.get("formula", "")).strip()
        if not formula:
            return json.dumps({"error": "formula 参数不能为空"})

        namespace = {}
        for name in dir(math):
            if not name.startswith("_"):
                namespace[name] = getattr(math, name)

        result = eval(formula, {"__builtins__": {}}, namespace)
        return json.dumps({"formula": formula, "result": result})
    except json.JSONDecodeError as e:
        return json.dumps({"error": "arguments JSON 解析失败: " + str(e)})
    except Exception as e:
        return json.dumps({"error": str(e)})
