// E2E：浏览器 WebSocket 协商一致性验证
// 场景：
//  - 后端 8080：真实 WS 后端，101 响应携带 permessage-deflate 扩展头，握手后主动推送一条 RSV1 压缩帧
//  - 被测 serve.js（3000）：修复版代理（透传全部 sec-websocket-* 头）
//  - 对照 legacy 代理（3001）：旧行为（仅 Upgrade/Connection/Sec-WebSocket-Accept，不透传扩展头）
// 断言：
//  - 经 serve.js：浏览器成功协商 permessage-deflate 并解压后端压缩帧 -> 收到 'server-push-compressed'（修复有效，无 1002）
//  - 经 legacy 代理：浏览器未协商压缩却收到 RSV1 压缩帧 -> 连接以协议错误关闭（复现修复前 1002 场景，证明测试敏感性）
import { createServer, request } from 'node:http';
import { createHash } from 'node:crypto';
import zlib from 'node:zlib';
import assert from 'node:assert/strict';

const WS_GUID = '258EAFA5-E914-47DA-95CA-C5AB0DC85B11';
const PUSH_MSG = 'server-push-compressed';

function wsAccept(key) {
  return createHash('sha1').update(key + WS_GUID).digest('base64');
}

// ---- 后端 8080：声明 permessage-deflate 并推送压缩帧 ----
const backend = createServer();
backend.on('upgrade', (req, socket) => {
  const { pathname } = new URL(req.url, 'http://backend');
  if (pathname !== '/ws') { socket.destroy(); return; }
  socket.write([
    'HTTP/1.1 101 Switching Protocols',
    'Upgrade: websocket',
    'Connection: Upgrade',
    'Sec-WebSocket-Accept: ' + wsAccept(req.headers['sec-websocket-key']),
    'Sec-WebSocket-Extensions: permessage-deflate; server_max_window_bits=15',
  ].join('\r\n') + '\r\n\r\n');
  // 推送压缩文本帧：FIN|RSV1|text=0xC1，payload=deflateRaw(msg, Z_SYNC_FLUSH)
  const compressed = zlib.deflateRawSync(Buffer.from(PUSH_MSG), { finishFlush: zlib.constants.Z_SYNC_FLUSH });
  const header = Buffer.from([0xC1, compressed.length]);
  socket.write(Buffer.concat([header, compressed]));
});
await new Promise((r) => backend.listen(8080, 'localhost', r));

// ---- 被测 serve.js（3000，修复版） ----
await import('./serve.js');

// ---- 对照 legacy 代理（3001，修改前行为：仅三行 + accept，不透传扩展头） ----
const legacyProxy = createServer();
legacyProxy.on('upgrade', (req, socket) => {
  const { pathname } = new URL(req.url, 'http://legacy');
  if (pathname !== '/ws') { socket.destroy(); return; }
  const proxyReq = request({
    hostname: 'localhost', port: 8080, path: req.url, method: req.method,
    headers: { ...req.headers, host: 'localhost:8080' },
  });
  proxyReq.on('upgrade', (proxyRes, proxySocket, proxyHead) => {
    socket.write([
      'HTTP/1.1 101 Switching Protocols',
      'Upgrade: websocket',
      'Connection: Upgrade',
      'Sec-WebSocket-Accept: ' + wsAccept(req.headers['sec-websocket-key']),
    ].join('\r\n') + '\r\n\r\n');
    if (proxyHead && proxyHead.length) socket.write(proxyHead);
    proxySocket.pipe(socket);
    socket.pipe(proxySocket);
  });
  proxyReq.on('error', () => socket.destroy());
  proxyReq.end();
});
await new Promise((r) => legacyProxy.listen(3001, 'localhost', r));

// ---- Playwright Chromium ----
const { chromium } = await import('@playwright/test');
const browser = await chromium.launch();
try {
  const page = await browser.newPage();
  await page.goto('http://localhost:3000/', { waitUntil: 'domcontentloaded', timeout: 15000 });

  const connectAndWait = (url) => page.evaluate((wsUrl) => new Promise((resolve) => {
    const ws = new WebSocket(wsUrl);
    const timer = setTimeout(() => resolve({ outcome: 'timeout' }), 6000);
    ws.onopen = () => {};
    ws.onmessage = (e) => { clearTimeout(timer); resolve({ outcome: 'message', data: e.data }); };
    ws.onerror = () => {};
    ws.onclose = (e) => { clearTimeout(timer); resolve({ outcome: 'closed', code: e.code, reason: e.reason }); };
  }), url);

  // 场景 1：经修复版 serve.js（3000）
  const fixed = await connectAndWait('ws://localhost:3000/ws');
  console.log('[E2E] 经 serve.js(修复版):', JSON.stringify(fixed));
  assert.equal(fixed.outcome, 'message', '浏览器应成功协商 permessage-deflate 并收到压缩帧解压后的消息（无 1002 关闭）');
  assert.equal(fixed.data, PUSH_MSG, '解压后的消息内容应与后端推送一致');

  // 场景 2：经 legacy 代理（3001，修改前行为）
  const legacy = await connectAndWait('ws://localhost:3001/ws');
  console.log('[E2E] 经 legacy 代理(修改前行为):', JSON.stringify(legacy));
  assert.equal(legacy.outcome, 'closed', '未透传扩展头时浏览器应与后端协商不一致而关闭连接');
  assert.ok(legacy.code === 1002 || legacy.code === 1006, 'legacy 场景应出现协议错误关闭（1002/1006），实际 code=' + legacy.code);

  console.log('E2E PASS：修复版协商一致（无 1002）；legacy 对照正确复现 1002 场景');
} finally {
  await browser.close();
  process.exit(0);
}
