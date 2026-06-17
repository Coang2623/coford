import { useEffect, useRef, useState } from 'react'
import { Check, Wifi, WifiOff } from 'lucide-react'
import { api } from '../api'
import { formatTime } from '../lib/format'
import { Card, PageHeader, Loading, ErrorState, EmptyState, Toast } from '../components/ui'

export default function KitchenPage() {
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [connected, setConnected] = useState(false)
  const [now, setNow] = useState(Date.now())
  const [toast, setToast] = useState(null)
  const esRef = useRef(null)

  async function loadBoard() {
    setLoading(true)
    setError(null)
    try {
      setOrders(await api.kitchen.board())
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadBoard()

    // SSE: nhận event đơn real-time (đẩy từ Kafka qua backend)
    const es = new EventSource(api.kitchen.streamUrl)
    esRef.current = es
    es.onopen = () => setConnected(true)
    es.onerror = () => setConnected(false)
    es.onmessage = (e) => {
      try {
        const evt = JSON.parse(e.data)
        if (evt.type === 'CREATED' && evt.order) {
          setOrders((prev) =>
            prev.some((o) => o.id === evt.order.id)
              ? prev.map((o) => (o.id === evt.order.id ? evt.order : o)) // đơn được sửa -> cập nhật
              : [...prev, evt.order],
          )
        } else if (evt.type === 'PREPARED') {
          setOrders((prev) => prev.filter((o) => o.id !== evt.orderId))
        }
      } catch {
        // bỏ qua message không parse được
      }
    }
    return () => es.close()
  }, [])

  // cập nhật "x phút trước" mỗi 20s
  useEffect(() => {
    const t = setInterval(() => setNow(Date.now()), 20000)
    return () => clearInterval(t)
  }, [])

  async function markDone(id) {
    setOrders((prev) => prev.filter((o) => o.id !== id)) // bỏ ngay cho mượt; SSE cũng sẽ xác nhận
    try {
      await api.orders.prepare(id)
    } catch (e) {
      setToast({ type: 'error', message: e.message })
      loadBoard()
    }
  }

  const minutesAgo = (iso) => Math.max(0, Math.floor((now - new Date(iso).getTime()) / 60000))

  return (
    <div className="px-4 sm:px-6 lg:px-8 py-6 lg:py-8 flex flex-col gap-6 min-h-full">
      <PageHeader title="Màn hình bếp" subtitle="Đơn mới hiện ngay khi được tạo">
        <span className={`inline-flex items-center gap-2 px-3 h-9 rounded-full text-sm font-semibold ${connected ? 'bg-success-soft text-success' : 'bg-ink/[0.06] text-muted'}`}>
          {connected ? <Wifi className="w-4 h-4" /> : <WifiOff className="w-4 h-4" />}
          {connected ? 'Đang kết nối' : 'Mất kết nối'}
        </span>
      </PageHeader>

      {loading ? (
        <Card><Loading label="Đang tải đơn..." /></Card>
      ) : error ? (
        <Card><ErrorState message={error} onRetry={loadBoard} /></Card>
      ) : orders.length === 0 ? (
        <Card><EmptyState title="Chưa có đơn nào cần pha" hint="Đơn mới sẽ tự hiện ở đây." /></Card>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
          {orders.map((o) => {
            const mins = minutesAgo(o.createdAt)
            const urgent = mins >= 5
            return (
              <Card key={o.id} className={`flex flex-col overflow-hidden ${urgent ? 'ring-2 ring-accent' : ''}`}>
                <div className="flex items-center justify-between px-5 py-3.5 border-b border-line">
                  <div className="flex items-baseline gap-2">
                    <span className="text-lg font-bold text-ink">Bàn {o.tableNo}</span>
                    <span className="text-sm text-muted tnum">#{o.id}</span>
                  </div>
                  <span className={`text-sm font-semibold tnum ${urgent ? 'text-accent' : 'text-muted'}`}>
                    {mins === 0 ? 'vừa xong' : `${mins} phút`}
                  </span>
                </div>
                <ul className="px-5 py-3 flex flex-col gap-2 flex-1">
                  {o.items.map((it) => (
                    <li key={it.id} className="flex justify-between gap-3">
                      <span className="text-[15px] text-ink">
                        <span className="font-bold text-accent tnum">{it.quantity}×</span> {it.itemName}
                        {it.note && <span className="block text-xs text-muted">↳ {it.note}</span>}
                      </span>
                    </li>
                  ))}
                </ul>
                <button
                  onClick={() => markDone(o.id)}
                  className="m-3 h-11 rounded-[12px] bg-ink text-white font-semibold flex items-center justify-center gap-2 hover:bg-ink/90 active:scale-[0.99] transition cursor-pointer"
                >
                  <Check className="w-[18px] h-[18px]" /> Hoàn thành
                </button>
              </Card>
            )
          })}
        </div>
      )}

      <Toast toast={toast} onClose={() => setToast(null)} />
    </div>
  )
}
