import { chromium } from 'playwright'
const BASE = 'http://localhost:5173'
const browser = await chromium.launch({ headless: true })
async function shot(user, file) {
  const ctx = await browser.newContext({ viewport: { width: 1280, height: 860 } })
  const p = await ctx.newPage()
  await p.goto(BASE + '/')
  await p.waitForSelector('#username', { timeout: 20000 })
  await p.fill('#username', user)
  await p.fill('#password', '123456')
  await p.click('#kc-login')
  await p.waitForSelector('text=Tạo đơn', { timeout: 20000 })
  await p.waitForTimeout(2500)
  await p.screenshot({ path: file })
  console.log('shot', user, '->', file)
  await ctx.close()
}
await shot('quanly', '.qa/auth-manager.png')
await shot('thungan', '.qa/auth-staff.png')
await browser.close()
console.log('DONE')
