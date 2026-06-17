import { chromium, devices } from 'playwright'
import { mkdirSync } from 'fs'

const BASE = 'http://localhost:5173'
const NEW = process.env.NEW_ORDER_ID || '3'
mkdirSync('.qa', { recursive: true })

const shots = [
  ['/', 'm-01-order'],
  ['/orders', 'm-02-orders'],
  [`/orders/${NEW}/pay`, 'm-03-payment'],
  ['/reports', 'm-04-report'],
  ['/menu', 'm-05-menu'],
]

const iPhone = devices['iPhone 13']
const browser = await chromium.launch({ headless: true })
const page = await browser.newPage({ ...iPhone })

for (const [route, name] of shots) {
  await page.goto(BASE + route, { waitUntil: 'networkidle' })
  await page.waitForTimeout(800)
  await page.screenshot({ path: `.qa/${name}.png`, fullPage: true })
  console.log('shot', name)
}

// Order với món trong giỏ (cuộn xuống thấy giỏ)
await page.goto(BASE + '/', { waitUntil: 'networkidle' })
await page.waitForTimeout(600)
const add = page.locator('button[aria-label^="Thêm"]')
await add.nth(0).click()
await add.nth(1).click()
await page.waitForTimeout(300)
await page.screenshot({ path: '.qa/m-01b-order-cart.png', fullPage: true })
console.log('shot m-01b-order-cart')

await browser.close()
console.log('DONE')
