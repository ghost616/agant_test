import { getBrowserExtension, getToolScript } from './session';

class ToolManager {
  private static instance: ToolManager;
  public functions: Map<string, Function> = new Map();
  public extensionLoaded = false;

  private constructor() {}

  static getInstance(): ToolManager {
    if (!ToolManager.instance) {
      ToolManager.instance = new ToolManager();
    }
    return ToolManager.instance;
  }

  /** 加载 extension JS，仅用于浏览器 API polyfill（如定义 window 对象上的浏览器交互方法），无需访问 ToolManager 上下文 */
  async loadExtension(): Promise<void> {
    if (this.extensionLoaded) return;
    const js = await getBrowserExtension();
    const fn = new Function(js);
    fn();
    this.extensionLoaded = true;
  }

  hasFunction(toolName: string): boolean {
    return this.functions.has(toolName);
  }

  registerFunction(toolName: string, toolScript: string): void {
    if (!toolName) return;
    const escapedName = toolName.replace(/\\/g, '\\\\').replace(/'/g, "\\'");
    const wrappedFn = new Function('ToolManager', `
      var toolFn = ${toolScript};
      ToolManager.functions.set('${escapedName}', toolFn);
    `);
    wrappedFn(ToolManager.getInstance());
  }

  getFunction(toolName: string): Function | undefined {
    return this.functions.get(toolName);
  }
}

class ToolExecutor {
  private static instance: ToolExecutor;
  private toolManager: ToolManager;

  private constructor() {
    this.toolManager = ToolManager.getInstance();
  }

  static getInstance(): ToolExecutor {
    if (!ToolExecutor.instance) {
      ToolExecutor.instance = new ToolExecutor();
    }
    return ToolExecutor.instance;
  }

  async execute(
    toolName: string,
    args: string,
    sessionId: string,
    toolId: string,
  ): Promise<string> {
    const fn = this.toolManager.getFunction(toolName);
    if (!fn) {
      throw new Error(`工具函数 ${toolName} 未注册`);
    }
    const context = { sessionId, toolId };
    const parsedArgs = JSON.parse(args);
    const result = await fn(parsedArgs, context);
    return typeof result === 'string' ? result : JSON.stringify(result);
  }
}

export const toolManager = ToolManager.getInstance();
export const toolExecutor = ToolExecutor.getInstance();
