import { chromium } from 'playwright';
const BASE = 'http://127.0.0.1:8090';
const OUT = new URL('./shots/', import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, '$1');
const W = 414, H = 896;
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

const browser = await chromium.launch({ channel: 'chrome' });
const ctx = await browser.newContext({ viewport: { width: W, height: H }, deviceScaleFactor: 2 });
const page = await ctx.newPage();
page.on('pageerror', (e) => console.log('[pageerror]', e.message));
await page.goto(BASE, { waitUntil: 'load' });
await sleep(8000);

const shot = async (n) => { await page.screenshot({ path: `${OUT}${n}.png` }); console.log('shot', n); };
const click = async (x, y, w = 700) => { await page.mouse.click(x, y); await sleep(w); };
const tabX = (i) => Math.round(W * (i + 0.5) / 5);
const tabY = H - 26;
const addX = W - 38;

// Tạo đơn
await click(addX, 231); await click(addX, 231); await click(addX, 315);
await click(W / 2, 790, 1100);          // mở giỏ
await click(W / 2, 860, 1600);          // gửi đơn

// Sang Đơn hàng -> Thanh toán
await click(tabX(1), tabY, 1200);
await click(318, 248, 1400);            // nút Thanh toán trên thẻ đơn
await shot('p1-payment');

// Chọn Chuyển khoản (tile phải) -> hiện QR
await click(334, 262, 1000);
await shot('p2-transfer');

// Xác nhận thanh toán
await click(W / 2, 855, 1600);
await shot('p3-invoice');

// Quay lại (nút "Xong") rồi sang Báo cáo
await click(W / 2, 540, 1200);          // nút Xong -> pop về danh sách đơn
await click(tabX(3), tabY, 1800);       // tab Báo cáo
await shot('p4-report');

await browser.close();
console.log('DONE');
