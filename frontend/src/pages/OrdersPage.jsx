import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { Search, List, LayoutGrid } from 'lucide-react'
import { api } from '../api'
import { formatVnd, formatTime } from '../lib/format'
import { Card, PageHeader, Segmented, Loading, ErrorState, EmptyState, StatusText, Toast } from '../components/ui'

const FILTERS = [
  { label: 'Tất cả', value: null },
  { label: 'Chờ TT', value: 'NEW' },
  { label: 'Đã TT', value: 'PAID' },
  { label: 'Đã hủy', value: 'CANCELLED' },
]

export default function OrdersPage() {
  const [orders, setOrders] = useState([])
  const [status, setStatus] = useState(null)
  const [search, setSearch] = useState('')
  const [view, setView] = useState(() => localStorage.getItem('coford_orders_view') || 'list')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [toast, setToast] = useState(null)

  function changeView(v) {
    setView(v)
    localStorage.setItem('coford_orders_view', v)
  }

  async function load() {
    setLoading(true)
    setError(null)
    try {
      setOrders(await api.orders.list(status))
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }
  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [status])

  const rows = useMemo(() => {
    const q = search.trim().toLowerCase()
    if (!q) return orders
    return orders.filter((o) => o.tableNo.toLowerCase().includes(q) || String(o.id).includes(q.replace('#', '')))
  }, [orders, search])

  const itemCount = (o) => o.items.reduce((s, i) => s + i.quantity, 0)

  const detailBtn =
    'inline-flex items-center justify-center h-9 px-4 rounded-[10px] bg-ink/[0.05] text-ink text-sm font-semibold hover:bg-ink/[0.09] transition'

  function Actions({ o, full }) {
    if (o.status === 'NEW') {
      // Chưa thanh toán: xem chi tiết ở màn đơn, hoặc thanh toán luôn
      return (
        <div className={`flex items-center gap-2 ${full ? 'w-full' : 'justify-end'}`}>
          <Link to={`/orders/${o.id}`} className={`${detailBtn} ${full ? 'flex-1' : ''}`}>Chi tiết</Link>
          <Link to={`/orders/${o.id}/pay`} className={`inline-flex items-center justify-center h-9 px-4 rounded-[10px] bg-accent text-white text-sm font-semibold hover:brightness-105 active:scale-[0.98] transition ${full ? 'flex-1' : ''}`}>Thanh toán</Link>
        </div>
      )
    }
    if (o.status === 'PAID') {
      return <Link to={`/orders/${o.id}/pay`} className={`${detailBtn} ${full ? 'w-full' : ''}`}>Xem hóa đơn</Link>
    }
    // CANCELLED: vẫn xem được chi tiết
    return <Link to={`/orders/${o.id}`} className={`${detailBtn} ${full ? 'w-full' : ''}`}>Xem chi tiết</Link>
  }

  function OrderCard({ o }) {
    return (
      <Card className="p-4 flex flex-col border border-ink/15">
        <div className="flex items-start justify-between">
          <div>
            <span className="font-semibold text-[15px] text-ink tnum">#{o.id}</span>
            <span className="text-[15px] text-muted"> · Bàn {o.tableNo}</span>
          </div>
          <StatusText status={o.status} />
        </div>
        <div className="flex items-end justify-between mt-2">
          <span className="text-sm text-muted">{itemCount(o)} món · {formatTime(o.createdAt)}</span>
          <span className="text-lg font-bold text-ink tnum">{formatVnd(o.totalAmount)}</span>
        </div>
        <div className="mt-3"><Actions o={o} full /></div>
      </Card>
    )
  }

  return (
    <div className="px-4 sm:px-6 lg:px-8 py-6 lg:py-8 flex flex-col gap-6 min-h-full">
      <PageHeader title="Đơn hàng" subtitle="Theo dõi và thanh toán các đơn trong ca">
        <div className="flex items-center gap-3 w-full sm:w-auto">
          <div className="flex items-center gap-2 flex-1 sm:w-60 h-10 px-3.5 rounded-[12px] bg-ink/[0.05]">
            <Search className="w-4 h-4 text-faint" />
            <input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Tìm theo bàn, mã đơn..."
              className="flex-1 min-w-0 bg-transparent text-sm text-ink placeholder:text-faint focus:outline-none"
            />
          </div>
          {/* Chuyển đổi danh sách / lưới (chỉ desktop) */}
          <div className="hidden md:inline-flex p-1 rounded-[12px] bg-ink/[0.06] gap-1 shrink-0">
            {[['list', List], ['grid', LayoutGrid]].map(([v, Icon]) => (
              <button
                key={v}
                onClick={() => changeView(v)}
                title={v === 'list' ? 'Dạng danh sách' : 'Dạng lưới'}
                aria-label={v === 'list' ? 'Dạng danh sách' : 'Dạng lưới'}
                className={`w-9 h-9 rounded-[9px] flex items-center justify-center transition cursor-pointer ${
                  view === v ? 'bg-surface text-ink shadow-sm' : 'text-muted hover:text-ink'
                }`}
              >
                <Icon className="w-[18px] h-[18px]" />
              </button>
            ))}
          </div>
        </div>
      </PageHeader>

      <div className="overflow-x-auto -mx-1 px-1">
        <Segmented options={FILTERS} value={status} onChange={setStatus} />
      </div>

      {loading ? (
        <Card><Loading label="Đang tải đơn hàng..." /></Card>
      ) : error ? (
        <Card><ErrorState message={error} onRetry={load} /></Card>
      ) : rows.length === 0 ? (
        <Card><EmptyState title="Không có đơn nào" hint="Tạo đơn mới ở mục Tạo đơn." /></Card>
      ) : (
        <>
          {/* Mobile: luôn dạng thẻ xếp dọc */}
          <div className="md:hidden flex flex-col gap-3">
            {rows.map((o) => <OrderCard key={o.id} o={o} />)}
          </div>

          {/* Desktop: bảng hoặc lưới theo lựa chọn */}
          <div className="hidden md:block">
            {view === 'grid' ? (
              <div className="grid gap-3 grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 2xl:grid-cols-5">
                {rows.map((o) => <OrderCard key={o.id} o={o} />)}
              </div>
            ) : (
              <Card className="overflow-hidden">
                <div className="divide-y divide-line">
                  <div className="flex items-center px-5 py-3 text-[12px] font-semibold uppercase tracking-wide text-faint">
                    <div className="w-24">Mã đơn</div>
                    <div className="w-20">Bàn</div>
                    <div className="w-24">Số món</div>
                    <div className="w-40">Tổng tiền</div>
                    <div className="flex-1">Trạng thái</div>
                    <div className="w-20">Giờ</div>
                    <div className="w-48 text-right">Thao tác</div>
                  </div>
                  {rows.map((o) => (
                    <div key={o.id} className="flex items-center px-5 py-3.5 hover:bg-ink/[0.015] transition-colors">
                      <div className="w-24 font-semibold text-[15px] text-ink tnum">#{o.id}</div>
                      <div className="w-20 text-[15px] text-ink">{o.tableNo}</div>
                      <div className="w-24 text-[15px] text-muted">{itemCount(o)} món</div>
                      <div className="w-40 font-semibold text-[15px] text-ink tnum">{formatVnd(o.totalAmount)}</div>
                      <div className="flex-1"><StatusText status={o.status} /></div>
                      <div className="w-20 text-[15px] text-muted tnum">{formatTime(o.createdAt)}</div>
                      <div className="w-48 flex justify-end"><Actions o={o} /></div>
                    </div>
                  ))}
                </div>
              </Card>
            )}
          </div>
        </>
      )}

      <Toast toast={toast} onClose={() => setToast(null)} />
    </div>
  )
}
