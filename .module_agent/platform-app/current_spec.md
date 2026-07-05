# platform-app 模块功能说明

> 待力牧首次执行后填充，记录模块公共方法与功能。
## 模块功能说明

platform-app 模块包含以下功能：
1. 智能体(Agent)上下文管理 - ContextDataProvider、MessageDataProvider 等实现
2. 模型调用器(Model Invoker) - 支持 OpenAI、Anthropic、Azure、Ollama、DeepSeek、Custom 等平台
3. Agent 上下文配置 (AgentContextConfiguration) - Spring Bean 装配
4. 所有 agent-base 依赖通过子包路径导入（如 com.ghost616.agentbase.dto.*、com.ghost616.agentbase.enums.*、com.ghost616.agentbase.service.* 等）
- 新增 MessageDataProvider、ContextDataProvider、ModelInvokerDataProvider、SystemTool 等 agent-base 接口的跨包 import 修复