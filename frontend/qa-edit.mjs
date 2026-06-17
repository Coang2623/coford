import { chromium } from 'playwright'
const BASE='http://localhost:5173'; const NID='13'
const b=await chromium.launch({headless:true})
const ctx=await b.newContext({viewport:{width:1280,height:900}})
const p=await ctx.newPage()
await p.goto(BASE+'/orders/'+NID)
await p.waitForSelector('#username',{timeout:20000})
await p.fill('#username','thungan'); await p.fill('#password','123456'); await p.click('#kc-login')
await p.waitForSelector('text=Sửa món trong đơn',{timeout:20000}); await p.waitForTimeout(1500)
await p.screenshot({path:'.qa/order-edit.png'})
await b.close(); console.log('DONE')
