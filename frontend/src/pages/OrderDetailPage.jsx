import { useEffect, useMemo, useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { ArrowLeft, Plus, Minus, X, ShoppingBag } from 'lucide-react'
import { api } from '../api'
import { formatVnd, formatDateTime } from '../lib/format'
import { Button, Card, Segmented, Loading, ErrorState, StatusText, Toast } from '../components/ui'

export default function OrderDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [order, setOrder] = useState(null)
  const [cats, setCats] = useState([])
  const [items, setItems] = useState([])
  const [activeCat, setActiveCat] = useState(null)
  const [cart, setCart] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [saving, setSaving] = useState(false)
  const [toast, setToast] = useState(null)

  function toCart(o) {
    return o.items.map((it) => ({ id: it.menuItemId, name: it.itemName, price: it.unitPrice, qty: it.quantity, note: it.note || '' }))
  }

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const [o, c, i] = await Promise.all([api.orders.get(id), api.menu.categories(), api.menu.items()])
      setOrder(o)
      setCats(c)
      setItems(i)
      setCart(toCart(o))
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }
  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id])

  const editable = order?.status === 'NEW'
  const filtered = useMemo(() => (activeCat ? items.filter((i) => i.categoryId === activeCat) : items), [items, activeCat])
  const total = cart.reduce((s, c) => s + c.price * c.qty, 0)
  const sig = (list) => JSON.stringify(list.map((c) => [c.id, c.qty, c.note]))
  const dirty = order ? sig(cart) !== sig(toCart(order)) : false

  function addToCart(item) {
    setCart((prev) => {
      const f = prev.find((c) => c.id === item.id)
      if (f) return prev.map((c) => (c.id === item.id ? { ...c, qty: c.qty + 1 } : c))
      return [...prev, { id: item.id, name: item.name, price: item.price, qty: 1, note: '' }]
    })
  }
  const changeQty = (id2, d) => setCart((prev) => prev.flatMap((c) => (c.id === id2 ? (c.qty + d <= 0 ? [] : [{ ...c, qty: c.qty + d }]) : [c])))
  const removeLine = (id2) => setCart((prev) => prev.filter((c) => c.id !== id2))
  const setNote = (id2, note) => setCart((prev) => prev.map((c) => (c.id === id2 ? { ...c, note } : c)))

  async function save() {
    if (!cart.length) return
    setSaving(true)
    try {
      const res = await api.orders.updateItems(id, cart.map((c) => ({ menuItemId: c.id, quantity: c.qty, note: c.note || null })))
      setOrder(res)
      setCart(toCart(res))
      setToast({ type: 'success', message: 'Đã lưu thay đổi đơn' })
      return true
    } catch (e) {
      setToast({ type: 'error', message: e.message })
      return false
    } finally {
      setSaving(false)
    }
  }

  async function goPay() {
    if (dirty) {
      const ok = await save()
      if (!ok) return
    }
    navigate(`/orders/${id}/pay`)
  }

  async function cancel() {
    if (!confirm(`Hủy đơn #${id}?`)) return
    try {
      await api.orders.cancel(id)
      setToast({ type: 'success', message: `Đã hủy đơn #${id}` })
      load()
    } catch (e) {
      setToast({ type: 'error', message: e.message })
    }
  }

  const catOptions = [{ label: 'Tất cả', value: null }, ...cats.map((c) => ({ label: c.name, value: c.id }))]

  return (
    <div className="px-4 sm:px-6 lg:px-8 py-6 lg:py-8 flex flex-col gap-6 min-h-full">
      <header className="flex items-center gap-4">
        <button onClick={() => navigate('/orders')} className="w-10 h-10 rounded-full bg-ink/[0.05] flex items-center justify-center text-ink hover:bg-ink/[0.09] cursor-pointer" aria-label="Quay lại">
          <ArrowLeft className="w-5 h-5" />
        </button>
        <div className="flex-1">
          <h1 className="text-[30px] font-bold tracking-[-0.02em] leading-none text-ink">Đơn #{id}</h1>
          <p className="text-[15px] text-muted mt-2">{editable ? 'Sửa món trong đơn' : 'Chi tiết đơn hàng'}</p>
        </div>
        {order && <StatusText status={order.status} />}
      </header>

      {loading ? (
        <Card><Loading /></Card>
      ) : error ? (
        <Card><ErrorState message={error} onRetry={load} /></Card>
      ) : editable ? (
        // === Đơn chưa thanh toán: trình sửa món ===
        <div className="flex flex-col lg:flex-row gap-6 lg:gap-7 lg:items-start">
          <div className="flex-1 min-w-0 flex flex-col gap-5">
            <div className="overflow-x-auto -mx-1 px-1">
              <Segmented options={catOptions} value={activeCat} onChange={setActiveCat} />
            </div>
            <Card className="overflow-hidden divide-y divide-line">
              {filtered.map((item) => {
                const off = !item.available
                return (
                  <div key={item.id} className={`flex items-center justify-between gap-4 px-5 py-4 ${off ? 'opacity-55' : ''}`}>
                    <div className="min-w-0">
                      <p className="text-[15px] font-medium text-ink">{item.name}</p>
                      <p className={`text-[13px] mt-0.5 ${off ? 'text-danger font-medium' : 'text-muted'}`}>{off ? 'Tạm hết' : item.description}</p>
                    </div>
                    <div className="flex items-center gap-4 shrink-0">
                      <span className="text-[15px] font-semibold text-ink tnum">{formatVnd(item.price)}</span>
                      <button onClick={() => addToCart(item)} disabled={off} aria-label={`Thêm ${item.name}`}
                        className="w-9 h-9 rounded-full bg-accent-soft text-accent flex items-center justify-center hover:brightness-95 active:scale-95 transition disabled:opacity-40 cursor-pointer">
                        <Plus className="w-[18px] h-[18px]" strokeWidth={2.5} />
                      </button>
                    </div>
                  </div>
                )
              })}
            </Card>
          </div>

          <Card className="w-full lg:w-[348px] shrink-0 flex flex-col overflow-hidden lg:sticky lg:top-8">
            <div className="px-5 pt-5 pb-4">
              <p className="text-[13px] font-semibold text-muted">ĐƠN #{id} · BÀN {order.tableNo}</p>
              <p className="text-[22px] font-bold tracking-[-0.01em] text-ink mt-0.5">{cart.reduce((s, c) => s + c.qty, 0)} món</p>
            </div>
            <div className="flex-1 px-5 divide-y divide-line border-t border-line">
              {cart.length === 0 ? (
                <div className="flex flex-col items-center justify-center gap-2 py-12 text-center text-muted">
                  <ShoppingBag className="w-7 h-7 text-faint" strokeWidth={1.5} />
                  <p className="text-sm">Đơn trống. Thêm món để lưu.</p>
                </div>
              ) : (
                cart.map((c) => (
                  <div key={c.id} className="py-3.5">
                    <div className="flex items-baseline justify-between gap-2">
                      <span className="text-[15px] font-medium text-ink">{c.name}</span>
                      <span className="text-[15px] font-semibold text-ink tnum">{formatVnd(c.price * c.qty)}</span>
                    </div>
                    <div className="flex items-center justify-between mt-2">
                      <div className="flex items-center gap-2">
                        <button onClick={() => changeQty(c.id, -1)} className="w-7 h-7 rounded-full bg-ink/[0.06] text-ink flex items-center justify-center hover:bg-ink/[0.1] active:scale-95 transition cursor-pointer"><Minus className="w-4 h-4" /></button>
                        <span className="text-[15px] font-semibold text-ink w-5 text-center tnum">{c.qty}</span>
                        <button onClick={() => changeQty(c.id, 1)} className="w-7 h-7 rounded-full bg-ink/[0.06] text-ink flex items-center justify-center hover:bg-ink/[0.1] active:scale-95 transition cursor-pointer"><Plus className="w-4 h-4" /></button>
                      </div>
                      <button onClick={() => removeLine(c.id)} className="text-faint hover:text-danger cursor-pointer" aria-label="Xóa"><X className="w-4 h-4" /></button>
                    </div>
                    <input value={c.note} onChange={(e) => setNote(c.id, e.target.value)} placeholder="Ghi chú..." className="mt-2.5 w-full text-[13px] text-ink bg-bg rounded-[10px] px-3 py-2 placeholder:text-faint focus:outline-none focus:bg-ink/[0.04]" />
                  </div>
                ))
              )}
            </div>
            <div className="px-5 py-5 border-t border-line flex flex-col gap-3">
              <div className="flex items-baseline justify-between">
                <span className="text-sm text-muted">Tổng cộng</span>
                <span className="text-[24px] font-bold tracking-[-0.01em] text-ink tnum">{formatVnd(total)}</span>
              </div>
              <Button onClick={save} disabled={!dirty || !cart.length || saving} className="w-full">
                {saving ? 'Đang lưu...' : dirty ? 'Lưu thay đổi' : 'Đã lưu'}
              </Button>
              <div className="flex gap-3">
                <Button variant="ghost" onClick={cancel} className="flex-1">Hủy đơn</Button>
                <Button onClick={goPay} disabled={!cart.length} className="flex-1">Thanh toán</Button>
              </div>
            </div>
          </Card>
        </div>
      ) : (
        // === Đã thanh toán / đã hủy: chỉ xem ===
        <div className="w-full max-w-[460px] flex flex-col gap-4">
          <Card className="overflow-hidden">
            <div className="px-5 py-4 border-b border-line flex items-center justify-between">
              <div>
                <p className="text-[15px] font-bold text-ink">Bàn {order.tableNo}</p>
                <p className="text-xs text-muted tnum">{formatDateTime(order.createdAt)}</p>
              </div>
              <StatusText status={order.status} />
            </div>
            <ul className="px-5 py-3 divide-y divide-line">
              {order.items.map((it) => (
                <li key={it.id} className="py-3 flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="text-[15px] text-ink"><span className="font-bold text-accent tnum">{it.quantity}×</span> {it.itemName}</p>
                    {it.note && <p className="text-xs text-muted mt-0.5">↳ {it.note}</p>}
                  </div>
                  <span className="text-[15px] font-medium text-ink tnum whitespace-nowrap">{formatVnd(it.lineTotal)}</span>
                </li>
              ))}
            </ul>
            <div className="px-5 py-4 border-t border-line flex items-center justify-between">
              <span className="text-[15px] text-muted">Tổng cộng</span>
              <span className="text-[24px] font-bold tracking-[-0.01em] text-ink tnum">{formatVnd(order.totalAmount)}</span>
            </div>
          </Card>
          {order.status === 'PAID' && (
            <Link to={`/orders/${id}/pay`} className="inline-flex items-center justify-center h-11 rounded-[12px] bg-ink/[0.05] text-ink font-semibold hover:bg-ink/[0.09] transition">Xem hóa đơn</Link>
          )}
          {order.status === 'CANCELLED' && <p className="text-sm text-muted text-center">Đơn này đã bị hủy.</p>}
        </div>
      )}

      <Toast toast={toast} onClose={() => setToast(null)} />
    </div>
  )
}
