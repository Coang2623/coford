import { chromium } from 'playwright'
const BASE='http://localhost:5173'
const b=await chromium.launch({headless:true})
const ctx=await b.newContext({viewport:{width:1280,height:860}})
const p=await ctx.newPage()
await p.goto(BASE+'/orders')
await p.waitForSelector('#username',{timeout:20000})
await p.fill('#username','quanly'); await p.fill('#password','123456'); await p.click('#kc-login')
await p.waitForSelector('text=Đơn hàng',{timeout:20000}); await p.waitForTimeout(1500)
await p.getByRole('button',{name:'Dạng lưới'}).click(); await p.waitForTimeout(1000)
await p.screenshot({path:'.qa/orders-grid.png'})
await b.close(); console.log('DONE')
