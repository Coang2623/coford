import { chromium } from 'playwright'
const G='http://localhost:3000'
const b=await chromium.launch({headless:true})
const p=await b.newPage({viewport:{width:1400,height:900}})
await p.goto(G+'/login',{waitUntil:'domcontentloaded'})
await p.waitForSelector('input[name="user"]',{timeout:15000})
await p.fill('input[name="user"]','admin'); await p.fill('input[name="password"]','admin')
await p.click('button[type="submit"]')
await p.waitForTimeout(3000)
await p.goto(G+'/d/coford-overview?from=now-15m&to=now',{waitUntil:'domcontentloaded'})
await p.waitForTimeout(5000)
await p.screenshot({path:'.qa/grafana.png'})
await b.close(); console.log('DONE')
