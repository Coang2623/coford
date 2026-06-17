import { chromium } from 'playwright'
import { mkdirSync } from 'fs'

const BASE = 'http://localhost:5173'
const NEW = process.env.NEW_ORDER_ID || '3'
mkdirSync('.qa', { recursive: true })

const shots = [
  ['/', '01-order'],
  ['/orders', '02-orders'],
  [`/orders/${NEW}/pay`, '03-payment'],
  ['/orders/1/pay', '04-invoice'],
  ['/reports', '05-report'],
  ['/menu', '06-menu'],
]

const browser = await chromium.launch({ headless: true })
const page = await browser.newPage({ viewport: { width: 1280, height: 860 } })

for (const [route, name] of shots) {
  await page.goto(BASE + route, { waitUntil: 'networkidle' })
  await page.waitForTimeout(800)
  await page.screenshot({ path: `.qa/${name}.png` })
  console.log('shot', name)
}

// Order screen với vài món trong giỏ
await page.goto(BASE + '/', { waitUntil: 'networkidle' })
await page.waitForTimeout(600)
const add = page.locator('button[aria-label^="Thêm"]')
await add.nth(0).click()
await add.nth(1).click()
await add.nth(1).click()
await page.waitForTimeout(300)
await page.screenshot({ path: '.qa/01b-order-cart.png' })
console.log('shot 01b-order-cart')

// Modal thêm món
await page.goto(BASE + '/menu', { waitUntil: 'networkidle' })
await page.waitForTimeout(600)
await page.getByRole('button', { name: 'Thêm món' }).click()
await page.waitForTimeout(300)
await page.screenshot({ path: '.qa/06b-menu-modal.png' })
console.log('shot 06b-menu-modal')

await browser.close()
console.log('DONE')
