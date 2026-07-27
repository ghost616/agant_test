import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

const mockGet = vi.hoisted(() => vi.fn());

vi.mock('../session', () => ({
  getBrowserExtension: mockGet,
  getToolScript: vi.fn(),
}));

const createMockToolManager = () => {
  const handlers = new Map<string, Function>();
  return {
    has: (name: string) => handlers.has(name),
    add: ({ name, handler }: { name: string; handler: Function }) => { handlers.set(name, handler); },
    get: (name: string) => handlers.get(name),
    clear: () => handlers.clear(),
  };
};

const mockToolExecutorObj = { execute: vi.fn() };

let mockToolManagerObj: ReturnType<typeof createMockToolManager>;

import { toolManager, toolExecutor } from '../toolExecutor';

function setupWindow(): void {
  mockToolManagerObj = createMockToolManager();
  (globalThis as any).window = {
    ToolHostBridge: { registerTool: vi.fn(), passToolResult: vi.fn() },
    ToolManager: mockToolManagerObj,
    ToolExecutor: mockToolExecutorObj,
  };
}

describe('ToolManager 单例模式', () => {
  beforeEach(() => {
    setupWindow();
  });

  it('toolManager 应包含 ToolManager 实例的属性和方法', () => {
    expect(toolManager).toBeDefined();
    expect(toolManager.extensionLoaded).toBe(false);
    expect(typeof toolManager.loadExtension).toBe('function');
    expect(typeof toolManager.hasFunction).toBe('function');
    expect(typeof toolManager.registerFunction).toBe('function');
    expect(typeof toolManager.getFunction).toBe('function');
  });
});

describe('registerFunction / hasFunction / getFunction', () => {
  beforeEach(() => {
    setupWindow();
    toolManager.extensionLoaded = false;
    mockToolManagerObj.clear();
  });

  it('registerFunction 应注册函数到全局 ToolManager', () => {
    toolManager.registerFunction('testTool', 'return "hello"');
    expect(toolManager.hasFunction('testTool')).toBe(true);
  });

  it('hasFunction 应对未注册工具返回 false', () => {
    expect(toolManager.hasFunction('nonExistent')).toBe(false);
  });

  it('getFunction 应返回已注册的函数', () => {
    toolManager.registerFunction('testTool', 'return "hello"');
    const fn = toolManager.getFunction('testTool');
    expect(fn).toBeDefined();
    expect(typeof fn).toBe('function');
  });

  it('getFunction 应对未注册工具返回 undefined', () => {
    expect(toolManager.getFunction('nonExistent')).toBeUndefined();
  });

  it('registerFunction 注册的函数可执行并返回正确结果', () => {
    toolManager.registerFunction('add', 'return params.a + params.b');
    const fn = toolManager.getFunction('add')!;
    const result = fn({ a: 1, b: 2 });
    expect(result).toBe(3);
  });
});

describe('loadExtension', () => {
  beforeEach(() => {
    setupWindow();
    toolManager.extensionLoaded = false;
    mockToolManagerObj.clear();
    mockGet.mockReset();
  });

  afterEach(() => {
    mockGet.mockReset();
  });

  it('应调用 getBrowserExtension', async () => {
    mockGet.mockResolvedValueOnce('var x = 1;');
    await toolManager.loadExtension();
    expect(mockGet).toHaveBeenCalled();
  });

  it('extensionLoaded 为 true 时应直接返回不重复加载', async () => {
    toolManager.extensionLoaded = true;
    await toolManager.loadExtension();
    expect(mockGet).not.toHaveBeenCalled();
  });

  it('加载后 extensionLoaded 应设为 true', async () => {
    mockGet.mockResolvedValueOnce('var x = 1;');
    expect(toolManager.extensionLoaded).toBe(false);
    await toolManager.loadExtension();
    expect(toolManager.extensionLoaded).toBe(true);
  });
});

describe('registerFunction 转义正确性', () => {
  beforeEach(() => {
    setupWindow();
    toolManager.extensionLoaded = false;
    mockToolManagerObj.clear();
  });

  it('应正确处理包含反斜杠的工具名称', () => {
    const nameWithBackslash = 'tool\\name';
    toolManager.registerFunction(nameWithBackslash, 'return "ok"');
    expect(toolManager.hasFunction(nameWithBackslash)).toBe(true);
    const fn = toolManager.getFunction(nameWithBackslash)!;
    expect(fn()).toBe('ok');
  });

  it('应正确处理包含单引号的工具名称', () => {
    const nameWithQuote = "it's a tool";
    toolManager.registerFunction(nameWithQuote, 'return "ok"');
    expect(toolManager.hasFunction(nameWithQuote)).toBe(true);
    const fn = toolManager.getFunction(nameWithQuote)!;
    expect(fn()).toBe('ok');
  });

  it('应正确处理同时包含反斜杠和单引号的工具名称', () => {
    const complexName = "tool\\'s name";
    toolManager.registerFunction(complexName, 'return "ok"');
    expect(toolManager.hasFunction(complexName)).toBe(true);
    const fn = toolManager.getFunction(complexName)!;
    expect(fn()).toBe('ok');
  });

  it('转义后注册的函数仍可接收参数并正确执行', () => {
    const name = "test'tool\\name";
    toolManager.registerFunction(name, 'return params.val');
    const fn = toolManager.getFunction(name)!;
    expect(fn({ val: 42 })).toBe(42);
  });
});

describe('ToolExecutor.execute', () => {
  beforeEach(() => {
    setupWindow();
    toolManager.extensionLoaded = false;
    mockToolManagerObj.clear();
    mockToolExecutorObj.execute.mockReset();
  });

  it('应优先使用全局 ToolExecutor.execute', async () => {
    mockToolExecutorObj.execute.mockResolvedValueOnce('global result');
    const result = await toolExecutor.execute('echo', '{"msg":"hello"}', 'sid', 'tid', 'toolConfig-1');
    expect(mockToolExecutorObj.execute).toHaveBeenCalledWith('sid', 'tid', 'echo', { msg: 'hello' }, 'toolConfig-1');
    expect(result).toBe('global result');
  });

  it('全局 ToolExecutor 不存在时应回退到本地执行', async () => {
    (globalThis as any).window = {
      ToolHostBridge: { registerTool: vi.fn(), passToolResult: vi.fn() },
      ToolManager: mockToolManagerObj,
    };
    toolManager.registerFunction('echo', 'return params.msg');
    const result = await toolExecutor.execute('echo', '{"msg":"hello"}', 'sid', 'tid', 'toolConfig-1');
    expect(result).toBe('hello');
  });

  it('应在函数未注册时抛出错误', async () => {
    (globalThis as any).window = {
      ToolHostBridge: { registerTool: vi.fn(), passToolResult: vi.fn() },
      ToolManager: mockToolManagerObj,
    };
    await expect(
      toolExecutor.execute('unknownTool', '{}', 'sid', 'tid', 'toolConfig-1'),
    ).rejects.toThrow('工具函数 unknownTool 未注册');
  });

  it('应返回字符串结果（当函数返回字符串时）', async () => {
    (globalThis as any).window = {
      ToolHostBridge: { registerTool: vi.fn(), passToolResult: vi.fn() },
      ToolManager: mockToolManagerObj,
    };
    toolManager.registerFunction('strFn', 'return "result string"');
    const result = await toolExecutor.execute('strFn', '{}', 'sid', 'tid', 'toolConfig-1');
    expect(result).toBe('result string');
  });

  it('应将非字符串结果 JSON 序列化', async () => {
    (globalThis as any).window = {
      ToolHostBridge: { registerTool: vi.fn(), passToolResult: vi.fn() },
      ToolManager: mockToolManagerObj,
    };
    toolManager.registerFunction('objFn', 'return { key: "value", num: 42 }');
    const result = await toolExecutor.execute('objFn', '{}', 'sid', 'tid', 'toolConfig-1');
    expect(result).toBe('{"key":"value","num":42}');
  });

  it('应将 context 传递给函数', async () => {
    (globalThis as any).window = {
      ToolHostBridge: { registerTool: vi.fn(), passToolResult: vi.fn() },
      ToolManager: mockToolManagerObj,
    };
    toolManager.registerFunction('ctxFn', 'return context.sessionId + ":" + context.toolId');
    const result = await toolExecutor.execute('ctxFn', '{}', 'session-1', 'tool-1', 'toolConfig-1');
    expect(result).toBe('session-1:tool-1');
  });
});
