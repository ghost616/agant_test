HOOK 的注册（Java 接口类名、说明、生效阶段）、校验、生命周期触发、HOOK 管理器
## HOOK 执行契约

HookInvoker 接口定义了 HOOK 的执行契约，包含 getPhase()（获取生效阶段）和 execute(AgentExecutionContext, ChatChunk)（执行 HOOK，返回 void）两个方法。所有 HOOK 实现类需实现此接口。
ChatChunk 的 import 路径已从 com.ghost616.platform.dto.chat.ChatChunk 更正为 com.ghost616.platform.dto.model.ChatChunk。
SystemHook 接口继承 HookInvoker，新增 getIndex() 方法（default 返回 0），用于控制系统 HOOK 的执行顺序，数值越小越早执行。