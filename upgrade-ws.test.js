// serve.js /ws upgrade 代理 —— 集成式验证测试（真实后端 8080 + 被测 serve.js 3000）
//
// 说明：尝试 mock http.request 的方式在本环境（Node 24 ESM-CJS named import 为快照）
// 不可行，故按测试说明允许的“对 serve.js 的 upgrade 处理做集成式验证”方案：
// 真实后端按客户端携带的 X-Test-Case 头路由返回不同 101 响应场景，端到端断言。
//
// 覆盖功能点：
//  1. 101 状态行 + Upgrade/Connection 行
//  2. sec-websocket-extensions（permessage-deflate）等扩展协商头原样透传、保留原始大小写
//  3. 多个 sec-websocket-* 头（accept/extensions/protocol）全部透传不遗漏
//  4. 非 sec-websocket-* 头（Content-Length）不透传
//  5. proxyHead（101 响应头部后紧随的后端数据）透传给客户端
//  6. 双向 pipe：客户端→后端与后端→客户端数据流通
//  7. 非 /ws 路径连接销毁
//  8. 后端连接错误（proxyReq error）时客户端连接销毁
import { test, describe } from 'node:test';
import assert from 'node:assert/strict';
import { createServer, request } from 'node:http';
import { createHash } from 'node:crypto';

const BACKEND_PORT = 8080;
const PORT = 3000;
const WS_GUID = '258EAFA5-E914-47DA-95CA-C5AB0DC85B11';
const SAMPLE_KEY = 'dGhlIHNhbXBsZSBub25jZQ==';
const SAMPLE_ACCEPT = 's3pPLMBiTxaQ9kYGzzhZRbK+xOo=';

// ---- 真实后端：按 X-Test-Case 头路由 ----
const backend = createServer();
backend.on('upgrade', (req, socket, head) => {
  const { pathname } = new URL(req.url, 'http://backend');
  if (pathname !== '/ws') { socket.destroy(); return; }
  const testCase = req.headers['x-test-case'] || 'extensions';
  const key = req.headers['sec-websocket-key'];
  const accept = createHash('sha1').update((key || SAMPLE_KEY) + WS_GUID).digest('base64');
  const base = [
    'HTTP/1.1 101 Switching Protocols',
    'Upgrade: websocket',
    'Connection: Upgrade',
  ];
  const send = (lines, extra) => {
    socket.write(lines.join('\r\n') + '\r\n\r\n');
    if (extra && extra.length) socket.write(extra);
  };
  switch (testCase) {
    case 'nosec': {
      send(base);
      socket.end();
      break;
    }
    case 'lowercase': {
      send([...base, 'sec-websocket-extensions: permessage-deflate; client_max_window_bits']);
      socket.end();
      break;
    }
    case 'echo': {
      send([...base,
        'Sec-WebSocket-Accept: ' + accept,
        'Sec-WebSocket-Extensions: permessage-deflate; server_max_window_bits=15',
      ]);
      socket.on('data', (d) => socket.write(Buffer.concat([Buffer.from('echo:'), d])));
      break;
    }
    case 'proxyhead': {
      // 101 响应头部后立即追加数据 -> 成为 serve.js 侧 proxyReq upgrade 事件的 head（proxyHead）
      send([...base,
        'Sec-WebSocket-Accept: ' + accept,
        'Sec-WebSocket-Extensions: permessage-deflate; server_max_window_bits=15',
      ], Buffer.from('residual-extension-data'));
      socket.end();
      break;
    }
    case 'close': {
      socket.destroy();
      break;
    }
    default: { // extensions：完整头集合（accept/extensions/protocol + 干扰项 Content-Length）
      send([...base,
        'Sec-WebSocket-Accept: ' + accept,
        'Sec-WebSocket-Extensions: permessage-deflate; server_max_window_bits=15',
        'Sec-WebSocket-Protocol: chat',
        'Content-Length: 0',
      ]);
      socket.end();
    }
  }
});
await new Promise((r) => backend.listen(BACKEND_PORT, 'localhost', r));

// ---- 被测服务（监听 3000） ----
await import('./serve.js');

function upgradeRequest({ path = '/ws', extraHeaders = {} } = {}, timeoutMs = 3000) {
  return new Promise((resolve) => {
    const req = request({
      hostname: 'localhost',
      port: PORT,
      path,
      method: 'GET',
      headers: {
        Connection: 'Upgrade',
        Upgrade: 'websocket',
        'Sec-WebSocket-Version': '13',
        'Sec-WebSocket-Key': SAMPLE_KEY,
        ...extraHeaders,
      },
    });
    const timer = setTimeout(() => resolve({ timedOut: true, req }), timeoutMs);
    req.on('upgrade', (res, socket, head) => { clearTimeout(timer); resolve({ res, socket, head, req }); });
    req.on('error', (err) => { clearTimeout(timer); resolve({ error: err, req }); });
    req.on('close', () => { clearTimeout(timer); resolve({ closed: true, req }); });
    req.end();
  });
}

async function collectSocketData(socket, ms = 200) {
  const chunks = [];
  return new Promise((resolve) => {
    const done = () => resolve(Buffer.concat(chunks));
    const timer = setTimeout(done, ms);
    socket.on('data', (c) => { chunks.push(Buffer.from(c)); });
    socket.on('close', () => { clearTimeout(timer); done(); });
  });
}

function assertUpgradeOk(result, label) {
  assert.ok(result.res, label + '：预期收到 101 upgrade，实际: ' + JSON.stringify({ error: result.error && result.error.message, closed: result.closed, timedOut: result.timedOut }));
}

function rawHeaderValue(rawHeaders, name) {
  const idx = rawHeaders.indexOf(name);
  if (idx === -1) return undefined;
  return rawHeaders[idx + 1];
}

describe('serve.js /ws upgrade 代理（集成式验证）', () => {
  test('1. 101 状态行 + Upgrade/Connection 行正确；sec-websocket-* 全量透传保留原始大小写；Content-Length 不透传', async () => {
    const result = await upgradeRequest({ extraHeaders: { 'X-Test-Case': 'extensions' } });
    assertUpgradeOk(result, 'extensions 场景');
    assert.equal(result.res.statusCode, 101);
    assert.equal(result.res.headers.upgrade, 'websocket');
    assert.equal(result.res.headers.connection, 'Upgrade');
    // 扩展协商头原样透传（值完整）
    assert.equal(result.res.headers['sec-websocket-extensions'], 'permessage-deflate; server_max_window_bits=15');
    assert.equal(result.res.headers['sec-websocket-protocol'], 'chat');
    assert.equal(result.res.headers['sec-websocket-accept'], SAMPLE_ACCEPT);
    // rawHeaders 保留原始头名大小写
    assert.ok(result.res.rawHeaders.includes('Sec-WebSocket-Extensions'), 'rawHeaders 应保留 Sec-WebSocket-Extensions 原始大小写');
    assert.ok(result.res.rawHeaders.includes('Sec-WebSocket-Protocol'));
    assert.ok(result.res.rawHeaders.includes('Sec-WebSocket-Accept'));
    // 非 sec-websocket-* 头不透传
    assert.equal(result.res.headers['content-length'], undefined, 'Content-Length 不应出现在 101 响应中');
    assert.ok(!result.res.rawHeaders.includes('Content-Length'), 'rawHeaders 不应包含 Content-Length');
    result.socket.destroy();
  });

  test('2. 多个 sec-websocket-* 头全部透传不遗漏（accept/extensions/protocol 三类均存在）', async () => {
    const result = await upgradeRequest({ extraHeaders: { 'X-Test-Case': 'extensions' } });
    assertUpgradeOk(result, 'extensions 场景');
    assert.equal(rawHeaderValue(result.res.rawHeaders, 'Sec-WebSocket-Accept'), SAMPLE_ACCEPT, 'accept 头应透传');
    assert.equal(rawHeaderValue(result.res.rawHeaders, 'Sec-WebSocket-Extensions'), 'permessage-deflate; server_max_window_bits=15', 'extensions 头应透传');
    assert.equal(rawHeaderValue(result.res.rawHeaders, 'Sec-WebSocket-Protocol'), 'chat', 'protocol 头应透传');
    result.socket.destroy();
  });

  test('3. 后端未返回 sec-websocket-* 头时，101 响应仍正常（仅三行，无多余头）', async () => {
    const result = await upgradeRequest({ extraHeaders: { 'X-Test-Case': 'nosec' } });
    assertUpgradeOk(result, 'nosec 场景');
    assert.equal(result.res.statusCode, 101);
    assert.equal(result.res.headers['sec-websocket-extensions'], undefined);
    assert.equal(result.res.headers['sec-websocket-protocol'], undefined);
    assert.equal(result.res.headers['sec-websocket-accept'], undefined);
    assert.equal(result.res.headers['content-length'], undefined);
    result.socket.destroy();
  });

  test('4. 后端返回小写 sec-websocket-* 头名时原样透传（保留原始大小写）', async () => {
    const result = await upgradeRequest({ extraHeaders: { 'X-Test-Case': 'lowercase' } });
    assertUpgradeOk(result, 'lowercase 场景');
    assert.ok(result.res.rawHeaders.includes('sec-websocket-extensions'), '小写头名应原样透传');
    assert.equal(result.res.headers['sec-websocket-extensions'], 'permessage-deflate; client_max_window_bits');
    result.socket.destroy();
  });

  test('5. proxyHead（101 响应头部后紧随的后端数据）透传给客户端 socket', async () => {
    const result = await upgradeRequest({ extraHeaders: { 'X-Test-Case': 'proxyhead' } });
    assertUpgradeOk(result, 'proxyhead 场景');
    const headBuf = result.head && result.head.length ? Buffer.from(result.head) : Buffer.alloc(0);
    const dataBuf = await collectSocketData(result.socket, 200);
    const all = Buffer.concat([headBuf, dataBuf]);
    assert.ok(all.includes(Buffer.from('residual-extension-data')), '客户端应收到 proxyHead 字节，实际 head+data: ' + JSON.stringify(all.toString()));
    result.socket.destroy();
  });

  test('6. 双向 pipe 建立：客户端→后端→回显→客户端（数据双向流通）', async () => {
    const result = await upgradeRequest({ extraHeaders: { 'X-Test-Case': 'echo' } });
    assertUpgradeOk(result, 'echo 场景');
    const received = collectSocketData(result.socket, 500);
    result.socket.write('ping-through-pipe');
    const all = await received;
    assert.ok(all.toString().includes('echo:ping-through-pipe'), '客户端应收到后端回显（验证 socket.pipe(proxySocket) 与 proxySocket.pipe(socket) 双向透传），实际: ' + JSON.stringify(all.toString()));
    result.socket.destroy();
  });

  test('7. 非 /ws 路径：连接被直接销毁（无 upgrade 响应）', async () => {
    const p = upgradeRequest({ path: '/other' });
    await new Promise((r) => setTimeout(r, 300));
    const result = await p;
    assert.equal(result.res, undefined, '非 /ws 路径不应收到 upgrade 响应');
    assert.ok(result.error || result.closed, '非 /ws 路径连接应被销毁');
  });

  test('8. 后端连接错误（proxyReq error 分支）：客户端连接被销毁', async () => {
    // 后端收到 upgrade 后立即 destroy，serve.js 的 proxyReq 触发 error 并销毁客户端连接
    const p = upgradeRequest({ extraHeaders: { 'X-Test-Case': 'close' } });
    await new Promise((r) => setTimeout(r, 500));
    const result = await p;
    assert.equal(result.res, undefined, '后端错误时不应有 upgrade 响应');
    assert.ok(result.error || result.closed, '后端错误时客户端连接应被销毁');
  });
});
