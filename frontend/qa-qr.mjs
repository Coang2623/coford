import { chromium } from 'playwright'
const BASE = 'http://localhost:5173'
const OID = '10'
const b = await chromium.launch({ headless: true })

const p = await b.newPage({ viewport: { width: 1280, height: 900 } })
await p.goto(BASE + '/orders/' + OID + '/pay', { waitUntil: 'domcontentloaded' })
await p.waitForTimeout(1500)
await p.getByText('Chuyển khoản', { exact: true }).click()
await p.waitForTimeout(2800)
await p.screenshot({ path: '.qa/pay-qr.png' })

await p.goto(BASE + '/bank', { waitUntil: 'domcontentloaded' })
await p.waitForTimeout(2000)
await p.screenshot({ path: '.qa/bank-loggedin.png' })

const p2 = await b.newPage({ viewport: { width: 1280, height: 900 } })
await p2.route('**/api/bank/status', (r) => r.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ loggedIn: false, username: null }) }))
await p2.goto(BASE + '/bank', { waitUntil: 'domcontentloaded' })
await p2.waitForTimeout(1500)
await p2.screenshot({ path: '.qa/bank-login.png' })

await b.close()
console.log('DONE')
