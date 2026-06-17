import { chromium, devices } from 'playwright'
const BASE = 'http://localhost:5173'
const b = await chromium.launch({ headless: true })
const p = await b.newPage({ viewport: { width: 1280, height: 860 } })
await p.goto(BASE + '/kitchen', { waitUntil: 'domcontentloaded' })
await p.waitForTimeout(1800)
await p.screenshot({ path: '.qa/k-desktop.png' })
const m = await b.newPage({ ...devices['iPhone 13'] })
await m.goto(BASE + '/kitchen', { waitUntil: 'domcontentloaded' })
await m.waitForTimeout(1800)
await m.screenshot({ path: '.qa/k-mobile.png', fullPage: true })
await b.close()
console.log('DONE')
