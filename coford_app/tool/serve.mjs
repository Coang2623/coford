import http from 'http';
import { readFile } from 'fs/promises';
import { extname, join, normalize } from 'path';
const ROOT = join(process.cwd(), 'build', 'web');
const MIME = { '.html':'text/html', '.js':'text/javascript', '.mjs':'text/javascript',
  '.json':'application/json', '.wasm':'application/wasm', '.css':'text/css',
  '.png':'image/png', '.ttf':'font/ttf', '.otf':'font/otf', '.ico':'image/x-icon',
  '.svg':'image/svg+xml', '.map':'application/json' };
const server = http.createServer(async (req, res) => {
  try {
    let p = decodeURIComponent(req.url.split('?')[0]);
    if (p === '/' || p === '') p = '/index.html';
    const fp = normalize(join(ROOT, p));
    if (!fp.startsWith(ROOT)) { res.writeHead(403); return res.end(); }
    let data;
    try { data = await readFile(fp); }
    catch { data = await readFile(join(ROOT, 'index.html')); }
    res.writeHead(200, {
      'Content-Type': MIME[extname(fp)] || 'application/octet-stream',
      'Cross-Origin-Opener-Policy': 'same-origin',
      'Cross-Origin-Embedder-Policy': 'require-corp',
      'Cache-Control': 'no-store',
    });
    res.end(data);
  } catch (e) { res.writeHead(500); res.end(String(e)); }
});
server.listen(8090, '127.0.0.1', () => console.log('serving build/web at http://127.0.0.1:8090'));
