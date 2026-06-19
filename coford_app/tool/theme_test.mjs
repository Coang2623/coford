import { chromium } from 'playwright';
const BASE = 'http://127.0.0.1:8090';
const OUT = new URL('./shots/', import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, '$1');
const W = 414, H = 896;
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

const browser = await chromium.launch({ channel: 'chrome' });
const ctx = await browser.newContext({
  viewport: { width: W, height: H },
  deviceScaleFactor: 2,
  hasTouch: true,
  isMobile: true
});
const page = await ctx.newPage();
page.on('pageerror', (e) => console.log('[pageerror]', e.message));
await page.goto(BASE, { waitUntil: 'load' });
await sleep(8000);

const shot = async (n) => { await page.screenshot({ path: `${OUT}${n}.png` }); console.log('shot', n); };
const click = async (x, y, w = 800) => { await page.mouse.click(x, y); await sleep(w); };
const tabX = (i) => Math.round(W * (i + 0.5) / 5);
const tabY = H - 26;

// 1. Sang tab Quản lý
await click(tabX(4), tabY, 1500);
await shot('theme_01_espresso_settings');

// Click Màu chủ đề (khoảng Y = 340)
await click(W / 2, 340, 1500);
await shot('theme_03_action_sheet');

// Chọn Matcha (Thanh mát) - Y khoảng 620
await click(W / 2, 620, 1500);

// Sang tab Bán hàng xem màu mới
await click(tabX(0), tabY, 1500);
await shot('theme_04_matcha_sales');

// Quay lại Quản lý để chọn Rose Berry
await click(tabX(4), tabY, 1500);
await click(W / 2, 340, 1500);
// Chọn Rose Berry (Ngọt ngào) - Y khoảng 720
await click(W / 2, 720, 1500);

// Sang tab Bán hàng xem màu mới
await click(tabX(0), tabY, 1500);
await shot('theme_05_rose_sales');

await browser.close();
console.log('ALL SHOTS DONE');
