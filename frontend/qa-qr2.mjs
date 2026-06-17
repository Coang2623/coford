import { chromium } from 'playwright'
const BASE='http://localhost:5173'; const OID='10'
const b=await chromium.launch({headless:true})
const p=await b.newPage({viewport:{width:1280,height:950}})
await p.goto(BASE+'/orders/'+OID+'/pay',{waitUntil:'domcontentloaded'}); await p.waitForTimeout(1500)
await p.getByText('Chuyển khoản',{exact:true}).click()
// chờ ảnh QR tải xong
try { await p.waitForFunction(() => { const i=document.querySelector('img[alt*="VietQR"],img[alt*="QR"]'); return i && i.complete && i.naturalWidth>0 }, { timeout: 8000 }) } catch {}
await p.waitForTimeout(800)
await p.screenshot({path:'.qa/pay-qr2.png'})
await p.goto(BASE+'/bank',{waitUntil:'domcontentloaded'}); await p.waitForTimeout(2500)
await p.screenshot({path:'.qa/bank-config.png', fullPage:true})
await b.close(); console.log('DONE')
