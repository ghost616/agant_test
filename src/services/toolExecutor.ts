import { getBrowserExtension, getToolScript } from './session';

interface JBToolHostBridge {
  passToolResult?: (sessionId: string, toolConfigId: string, result: string) => void;
}

interface JBToolManager {
  has?: (toolName: string) => boolean;
  add?: (entry: { name: string; handler: Function }) => void;
  get?: (toolName: string) => Function | undefined;
}

interface JBToolExecutor {
  execute?: (sessionId: string, toolConfigId: string, toolName: string, params: any) => Promise<string>;
}

class ToolManager {
  private static instance: ToolManager;
  public extensionLoaded = false;

  public jbToolHostBridge?: JBToolHostBridge;
  public jbToolManager?: JBToolManager;
  public jbToolExecutor?: JBToolExecutor;

  private constructor() {}

  static getInstance(): ToolManager {
    if (!ToolManager.instance) {
      ToolManager.instance = new ToolManager();
    }
    return ToolManager.instance;
  }

  async loadExtension(): Promise<void> {
    if (this.extensionLoaded) return;
    const js = await getBrowserExtension();
    const fn = new Function(js + '; return { ToolHostBridge, ToolManager, ToolExecutor };');
    let result: { ToolHostBridge: JBToolHostBridge; ToolManager: JBToolManager; ToolExecutor: JBToolExecutor };
    try {
      result = fn();
    } catch (e) {
      console.error(`[ToolManager.loadExtension] fn() 异常`, e);
      throw e;
    }
    this.jbToolHostBridge = result!.ToolHostBridge;
    this.jbToolManager = result!.ToolManager;
    this.jbToolExecutor = result!.ToolExecutor;
    this.extensionLoaded = true;
  }

  hasFunction(toolName: string): boolean {
    return this.jbToolManager?.has?.(toolName) ?? false;
  }

  registerFunction(toolName: string, toolScript: string): void {
    if (!toolName) return;
    const handler = new Function('params', 'context', toolScript) as Function;
    this.jbToolManager?.add?.({ name: toolName, handler });
  }

  getFunction(toolName: string): Function | undefined {
    return this.jbToolManager?.get?.(toolName);
  }
}

class ToolExecutor {
  private static instance: ToolExecutor;

  private constructor() {}

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
    toolConfigId: string,
  ): Promise<string> {
    const params = JSON.parse(args);
    const jbExecutor = this.getManager().jbToolExecutor;
    if (jbExecutor?.execute) {
      return jbExecutor.execute(sessionId, toolConfigId, toolName, params);
    }
    const fn = this.getManager().getFunction(toolName);
    if (!fn) throw new Error(`工具函数 ${toolName} 未注册`);
    const result = await fn(params, { sessionId, toolConfigId });
    return typeof result === 'string' ? result : JSON.stringify(result);
  }

  private getManager(): ToolManager {
    return ToolManager.getInstance();
  }
}

export const toolManager = ToolManager.getInstance();
export const toolExecutor = ToolExecutor.getInstance();
