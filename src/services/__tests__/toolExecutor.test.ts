import { describe, it, expect, vi, beforeEach } from 'vitest';

const mockGet = vi.hoisted(() => vi.fn());

vi.mock('../session', () => ({
  getBrowserExtension: mockGet,
  getToolScript: vi.fn(),
}));

import { toolManager, toolExecutor } from '../toolExecutor';

describe('ToolManager 单例模式', () => {
  it('toolManager 应包含 ToolManager 实例的属性和方法', () => {
    expect(toolManager).toBeDefined();
    expect(typeof toolManager.functions).toBe('object');
    expect(typeof toolManager.loadExtension).toBe('function');
    expect(typeof toolManager.hasFunction).toBe('function');
    expect(typeof toolManager.registerFunction).toBe('function');
    expect(typeof toolManager.getFunction).toBe('function');
  });
});

describe('registerFunction / hasFunction / getFunction', () => {
  beforeEach(() => {
    toolManager.functions.clear();
    toolManager.extensionLoaded = false;
  });

  it('registerFunction 应注册函数到 functions Map', () => {
    toolManager.registerFunction('testTool', '() => "hello"');
    expect(toolManager.hasFunction('testTool')).toBe(true);
  });

  it('hasFunction 应对未注册工具返回 false', () => {
    expect(toolManager.hasFunction('nonExistent')).toBe(false);
  });

  it('getFunction 应返回已注册的函数', () => {
    toolManager.registerFunction('testTool', '() => "hello"');
    const fn = toolManager.getFunction('testTool');
    expect(fn).toBeDefined();
    expect(typeof fn).toBe('function');
  });

  it('getFunction 应对未注册工具返回 undefined', () => {
    expect(toolManager.getFunction('nonExistent')).toBeUndefined();
  });

  it('registerFunction 注册的函数可执行并返回正确结果', () => {
    toolManager.registerFunction('add', '(args) => args.a + args.b');
    const fn = toolManager.getFunction('add')!;
    const result = fn({ a: 1, b: 2 });
    expect(result).toBe(3);
  });
});

describe('loadExtension', () => {
  beforeEach(() => {
    toolManager.functions.clear();
    toolManager.extensionLoaded = false;
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
    toolManager.functions.clear();
    toolManager.extensionLoaded = false;
  });

  it('应正确处理包含反斜杠的工具名称', () => {
    const nameWithBackslash = 'tool\\name';
    toolManager.registerFunction(nameWithBackslash, '() => "ok"');
    expect(toolManager.hasFunction(nameWithBackslash)).toBe(true);
    const fn = toolManager.getFunction(nameWithBackslash)!;
    expect(fn()).toBe('ok');
  });

  it('应正确处理包含单引号的工具名称', () => {
    const nameWithQuote = "it's a tool";
    toolManager.registerFunction(nameWithQuote, '() => "ok"');
    expect(toolManager.hasFunction(nameWithQuote)).toBe(true);
    const fn = toolManager.getFunction(nameWithQuote)!;
    expect(fn()).toBe('ok');
  });

  it('应正确处理同时包含反斜杠和单引号的工具名称', () => {
    const complexName = "tool\\'s name";
    toolManager.registerFunction(complexName, '() => "ok"');
    expect(toolManager.hasFunction(complexName)).toBe(true);
    const fn = toolManager.getFunction(complexName)!;
    expect(fn()).toBe('ok');
  });

  it('转义后注册的函数仍可接收参数并正确执行', () => {
    const name = "test'tool\\name";
    toolManager.registerFunction(name, '(args) => args.val');
    const fn = toolManager.getFunction(name)!;
    expect(fn({ val: 42 })).toBe(42);
  });
});

describe('ToolExecutor.execute', () => {
  beforeEach(() => {
    toolManager.functions.clear();
    toolManager.extensionLoaded = false;
  });

  it('应调用 ToolManager.getFunction 并执行函数', async () => {
    toolManager.registerFunction('echo', '(args) => args.msg');
    const spy = vi.spyOn(toolManager, 'getFunction');
    const result = await toolExecutor.execute('echo', '{"msg":"hello"}', 'sid', 'tid');
    expect(spy).toHaveBeenCalledWith('echo');
    expect(result).toBe('hello');
  });

  it('应在函数未注册时抛出错误', async () => {
    await expect(
      toolExecutor.execute('unknownTool', '{}', 'sid', 'tid'),
    ).rejects.toThrow('工具函数 unknownTool 未注册');
  });

  it('应返回字符串结果（当函数返回字符串时）', async () => {
    toolManager.registerFunction('strFn', '() => "result string"');
    const result = await toolExecutor.execute('strFn', '{}', 'sid', 'tid');
    expect(result).toBe('result string');
  });

  it('应将非字符串结果 JSON 序列化', async () => {
    toolManager.registerFunction('objFn', '() => ({ key: "value", num: 42 })');
    const result = await toolExecutor.execute('objFn', '{}', 'sid', 'tid');
    expect(result).toBe('{"key":"value","num":42}');
  });

  it('应将 context 传递给函数', async () => {
    toolManager.registerFunction('ctxFn', '(args, context) => context.sessionId + ":" + context.toolId');
    const result = await toolExecutor.execute('ctxFn', '{}', 'session-1', 'tool-1');
    expect(result).toBe('session-1:tool-1');
  });
});
