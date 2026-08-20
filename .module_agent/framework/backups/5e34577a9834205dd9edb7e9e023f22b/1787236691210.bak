import { createServer, request } from 'http';
import { readFile, stat } from 'fs/promises';
import { extname, join, normalize, sep } from 'path';
import { fileURLToPath } from 'url';

const PORT = 3000;
const BACKEND_HOST = 'localhost';
const BACKEND_PORT = 8080;
const DIST_DIR = fileURLToPath(new URL('./dist', import.meta.url));

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.mjs': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.gif': 'image/gif',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.webp': 'image/webp',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.ttf': 'font/ttf',
  '.eot': 'application/vnd.ms-fontobject',
  '.otf': 'font/otf',
  '.txt': 'text/plain; charset=utf-8',
  '.webmanifest': 'application/manifest+json',
};

function getMimeType(filePath) {
  return MIME_TYPES[extname(filePath).toLowerCase()] || 'application/octet-stream';
}

function proxyApi(req, res) {
  const { pathname, search } = new URL(req.url, `http://${BACKEND_HOST}:${PORT}`);
  const proxyReq = request(
    {
      hostname: BACKEND_HOST,
      port: BACKEND_PORT,
      path: `${pathname}${search}`,
      method: req.method,
      headers: { ...req.headers, host: `${BACKEND_HOST}:${BACKEND_PORT}` },
    },
    (proxyRes) => {
      res.writeHead(proxyRes.statusCode, proxyRes.headers);
      proxyRes.pipe(res);
    }
  );
  proxyReq.on('error', (err) => {
    console.error(`[serve] /api proxy error: ${err.message}`);
    if (!res.headersSent) {
      res.writeHead(502, { 'Content-Type': 'text/plain; charset=utf-8' });
    }
    res.end('Bad Gateway: backend at localhost:8080 is unreachable');
  });
  req.pipe(proxyReq);
}

async function resolveFile(filePath) {
  const fileStat = await stat(filePath);
  if (fileStat.isDirectory()) {
    return join(filePath, 'index.html');
  }
  return filePath;
}

async function serveIndex(req, res) {
  try {
    const content = await readFile(join(DIST_DIR, 'index.html'));
    res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
    res.end(content);
  } catch (err) {
    console.error(`[serve] index.html missing: ${err.message}`);
    res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
    res.end('404 Not Found');
  }
}

async function serveStatic(req, res) {
  try {
    const { pathname } = new URL(req.url, `http://${BACKEND_HOST}:${PORT}`);
    const relative = decodeURIComponent(pathname).replace(/^[/\\]+/, '');
    const filePath = normalize(join(DIST_DIR, relative));

    if (filePath !== DIST_DIR && !filePath.startsWith(DIST_DIR + sep)) {
      res.writeHead(403, { 'Content-Type': 'text/plain; charset=utf-8' });
      res.end('403 Forbidden');
      return;
    }

    const target = await resolveFile(filePath);
    const content = await readFile(target);
    res.writeHead(200, { 'Content-Type': getMimeType(target) });
    res.end(content);
  } catch (err) {
    if (err && err.code === 'ENOENT') {
      await serveIndex(req, res);
    } else {
      console.error(`[serve] static error: ${err.message}`);
      res.writeHead(500, { 'Content-Type': 'text/plain; charset=utf-8' });
      res.end('500 Internal Server Error');
    }
  }
}

const server = createServer((req, res) => {
  try {
    const { pathname } = new URL(req.url, `http://${BACKEND_HOST}:${PORT}`);
    if (pathname.startsWith('/api')) {
      proxyApi(req, res);
    } else {
      serveStatic(req, res);
    }
  } catch (err) {
    console.error(`[serve] request error: ${err.message}`);
    res.writeHead(500, { 'Content-Type': 'text/plain; charset=utf-8' });
    res.end('500 Internal Server Error');
  }
});

// WebSocket 升级代理：将 /ws 连接转发到后端（支持全局 WebSocket 客户端）
server.on('upgrade', (req, socket, head) => {
  const { pathname } = new URL(req.url, `http://${BACKEND_HOST}:${PORT}`);
  if (!pathname.startsWith('/ws')) {
    socket.destroy();
    return;
  }
  const proxyReq = request({
    hostname: BACKEND_HOST,
    port: BACKEND_PORT,
    path: req.url,
    method: req.method,
    headers: { ...req.headers, host: `${BACKEND_HOST}:${BACKEND_PORT}` },
  });
  proxyReq.on('upgrade', (proxyRes, proxySocket, proxyHead) => {
    const lines = [
      'HTTP/1.1 101 Switching Protocols',
      `Upgrade: ${proxyRes.headers.upgrade || 'websocket'}`,
      `Connection: ${proxyRes.headers.connection || 'Upgrade'}`,
    ];
    const accept = proxyRes.headers['sec-websocket-accept'];
    if (accept) {
      lines.push(`Sec-WebSocket-Accept: ${accept}`);
    }
    socket.write(`${lines.join('\r\n')}\r\n\r\n`);
    if (proxyHead && proxyHead.length) {
      socket.write(proxyHead);
    }
    proxySocket.pipe(socket);
    socket.pipe(proxySocket);
  });
  proxyReq.on('error', (err) => {
    console.error(`[serve] /ws proxy error: ${err.message}`);
    socket.destroy();
  });
  proxyReq.end();
});

server.listen(PORT, () => {
  console.log(`[serve] static server running at http://localhost:${PORT}`);
  console.log(`[serve] /api proxied to http://${BACKEND_HOST}:${BACKEND_PORT}`);
  console.log(`[serve] /ws proxied to http://${BACKEND_HOST}:${BACKEND_PORT}`);
  console.log(`[serve] serving dist/ directory (SPA fallback to index.html)`);
});
