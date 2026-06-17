import { useEffect, useMemo, useState } from 'react'
import { api } from '../api'
import { formatVnd } from '../lib/format'
import { Card, PageHeader, Segmented, Loading, ErrorState, EmptyState } from '../components/ui'

export default function ReportPage() {
  const [daily, setDaily] = useState([])
  const [top, setTop] = useState([])
  const [win, setWin] = useState(7)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const [d, t] = await Promise.all([api.reports.dailyRevenue(), api.reports.topItems(8)])
      setDaily(d)
      setTop(t)
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }
  useEffect(() => {
    load()
  }, [])

  const chart = useMemo(() => [...daily].reverse().slice(-win), [daily, win])
  const maxRev = Math.max(1, ...chart.map((d) => Number(d.revenue)))
  const today = daily[0]
  const revenueToday = Number(today?.revenue || 0)
  const ordersToday = Number(today?.orderCount || 0)
  const avg = ordersToday ? revenueToday / ordersToday : 0
  const itemsSold = top.reduce((s, t) => s + t.totalQuantity, 0)

  const fmtDay = (iso) => {
    const d = new Date(iso)
    return `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}`
  }

  const stats = [
    ['Doanh thu gần nhất', formatVnd(revenueToday)],
    ['Số đơn', String(ordersToday)],
    ['TB mỗi đơn', formatVnd(Math.round(avg))],
    ['Ly đã bán', String(itemsSold)],
  ]

  return (
    <div className="px-4 sm:px-6 lg:px-8 py-6 lg:py-8 flex flex-col gap-6 min-h-full">
      <PageHeader title="Báo cáo" subtitle="Tổng quan kết quả bán hàng">
        <Segmented options={[{ label: '7 ngày', value: 7 }, { label: '30 ngày', value: 30 }]} value={win} onChange={setWin} />
      </PageHeader>

      {loading ? (
        <Card><Loading /></Card>
      ) : error ? (
        <Card><ErrorState message={error} onRetry={load} /></Card>
      ) : (
        <>
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
            {stats.map(([label, value]) => (
              <Card key={label} className="px-5 py-5">
                <p className="text-[28px] font-bold tracking-[-0.02em] leading-none text-ink tnum">{value}</p>
                <p className="text-[13px] text-muted mt-2">{label}</p>
              </Card>
            ))}
          </div>

          <div className="flex flex-col lg:flex-row gap-4 items-stretch">
            <Card className="flex-1 min-w-0 px-5 sm:px-6 py-5">
              <h2 className="text-[13px] font-semibold uppercase tracking-wide text-faint mb-6">Doanh thu theo ngày</h2>
              {chart.length === 0 ? (
                <EmptyState title="Chưa có doanh thu" hint="Hoàn tất vài đơn để xem biểu đồ." />
              ) : (
                <div className="flex items-end justify-between gap-4 h-[260px]">
                  {chart.map((d, i) => {
                    const isLast = i === chart.length - 1
                    const h = Math.round((Number(d.revenue) / maxRev) * 200) + 8
                    return (
                      <div key={d.day} className="flex-1 flex flex-col items-center justify-end gap-2">
                        <span className="text-[11px] font-semibold text-muted tnum">{(Number(d.revenue) / 1_000_000).toFixed(1)}M</span>
                        <div className={`w-full max-w-[40px] rounded-[8px] ${isLast ? 'bg-accent' : 'bg-ink/[0.10]'}`} style={{ height: `${h}px` }} />
                        <span className="text-xs text-muted tnum">{fmtDay(d.day)}</span>
                      </div>
                    )
                  })}
                </div>
              )}
            </Card>

            <Card className="w-full lg:w-[380px] shrink-0 px-6 py-5">
              <h2 className="text-[13px] font-semibold uppercase tracking-wide text-faint mb-3">Món bán chạy</h2>
              {top.length === 0 ? (
                <EmptyState title="Chưa có dữ liệu" />
              ) : (
                <div className="divide-y divide-line">
                  {top.map((t, i) => (
                    <div key={t.itemName} className="flex items-center gap-4 py-3">
                      <span className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold tnum ${i < 3 ? 'bg-accent-soft text-accent' : 'bg-ink/[0.05] text-muted'}`}>{i + 1}</span>
                      <div className="flex-1 min-w-0">
                        <p className="text-[15px] font-medium text-ink truncate">{t.itemName}</p>
                        <p className="text-xs text-muted">{t.totalQuantity} ly</p>
                      </div>
                      <span className="text-[15px] font-semibold text-ink tnum">{formatVnd(t.revenue)}</span>
                    </div>
                  ))}
                </div>
              )}
            </Card>
          </div>
        </>
      )}
    </div>
  )
}
