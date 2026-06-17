import { useEffect, useMemo, useState } from 'react'
import { Plus, Minus, X, ShoppingBag } from 'lucide-react'
import { api } from '../api'
import { formatVnd } from '../lib/format'
import { Button, Card, PageHeader, Segmented, Loading, ErrorState, Toast } from '../components/ui'

const TABLES = ['B1', 'B2', 'B3', 'B4', 'B5', 'B6', 'Mang về']

export default function OrderPage() {
  const [cats, setCats] = useState([])
  const [items, setItems] = useState([])
  const [activeCat, setActiveCat] = useState(null)
  const [cart, setCart] = useState([])
  const [tableNo, setTableNo] = useState('B1')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)
  const [toast, setToast] = useState(null)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const [c, i] = await Promise.all([api.menu.categories(), api.menu.items()])
      setCats(c)
      setItems(i)
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }
  useEffect(() => {
    load()
  }, [])

  const filtered = useMemo(
    () => (activeCat ? items.filter((i) => i.categoryId === activeCat) : items),
    [items, activeCat],
  )
  const total = cart.reduce((s, c) => s + c.price * c.qty, 0)
  const count = cart.reduce((s, c) => s + c.qty, 0)

  function addToCart(item) {
    setCart((prev) => {
      const found = prev.find((c) => c.id === item.id)
      if (found) return prev.map((c) => (c.id === item.id ? { ...c, qty: c.qty + 1 } : c))
      return [...prev, { id: item.id, name: item.name, price: item.price, qty: 1, note: '' }]
    })
  }
  const changeQty = (id, d) =>
    setCart((prev) => prev.flatMap((c) => (c.id === id ? (c.qty + d <= 0 ? [] : [{ ...c, qty: c.qty + d }]) : [c])))
  const removeLine = (id) => setCart((prev) => prev.filter((c) => c.id !== id))
  const setNote = (id, note) => setCart((prev) => prev.map((c) => (c.id === id ? { ...c, note } : c)))

  async function submit() {
    if (!cart.length) return
    setSubmitting(true)
    try {
      const res = await api.orders.create(
        {
          tableNo,
          note: null,
          items: cart.map((c) => ({ menuItemId: c.id, quantity: c.qty, note: c.note || null })),
        },
        crypto.randomUUID(), // Idempotency-Key: chống tạo đơn trùng nếu gửi lại
      )
      setCart([])
      setToast({ type: 'success', message: `Đã tạo đơn #${res.id} cho ${tableNo}` })
    } catch (e) {
      setToast({ type: 'error', message: e.message })
    } finally {
      setSubmitting(false)
    }
  }

  const catOptions = [{ label: 'Tất cả', value: null }, ...cats.map((c) => ({ label: c.name, value: c.id }))]

  return (
    <div className="px-4 sm:px-6 lg:px-8 py-6 lg:py-8 flex flex-col gap-6 lg:gap-7 min-h-full">
      <PageHeader title="Tạo đơn" subtitle="Chọn món để thêm vào đơn của bàn">
        <label className="flex items-center gap-2.5 text-[15px]">
          <span className="text-muted">Bàn</span>
          <select
            value={tableNo}
            onChange={(e) => setTableNo(e.target.value)}
            className="h-10 px-3.5 rounded-[12px] bg-ink/[0.05] font-semibold text-ink focus:outline-none cursor-pointer"
          >
            {TABLES.map((t) => (
              <option key={t} value={t}>{t}</option>
            ))}
          </select>
        </label>
      </PageHeader>

      <div className="flex flex-col lg:flex-row gap-6 lg:gap-7 flex-1 items-stretch lg:items-start">
        <div className="flex-1 min-w-0 flex flex-col gap-5">
          {loading ? (
            <Card className="py-2"><Loading label="Đang tải menu..." /></Card>
          ) : error ? (
            <Card><ErrorState message={error} onRetry={load} /></Card>
          ) : (
            <>
              <div className="overflow-x-auto -mx-1 px-1">
                <Segmented options={catOptions} value={activeCat} onChange={setActiveCat} />
              </div>
              <Card className="overflow-hidden divide-y divide-line">
                {filtered.map((item) => (
                  <MenuRow key={item.id} item={item} onAdd={() => addToCart(item)} />
                ))}
              </Card>
            </>
          )}
        </div>

        {/* Tóm tắt đơn */}
        <Card className="w-full lg:w-[348px] shrink-0 flex flex-col overflow-hidden lg:sticky lg:top-8 lg:max-h-[calc(100vh-4rem)]">
          <div className="px-5 pt-5 pb-4">
            <p className="text-[13px] font-semibold text-muted">ĐƠN HIỆN TẠI</p>
            <p className="text-[22px] font-bold tracking-[-0.01em] text-ink mt-0.5">{tableNo} · {count} món</p>
          </div>

          <div className="flex-1 lg:overflow-y-auto px-5 divide-y divide-line border-t border-line">
            {cart.length === 0 ? (
              <div className="flex flex-col items-center justify-center gap-2 py-16 text-center text-muted">
                <ShoppingBag className="w-7 h-7 text-faint" strokeWidth={1.5} />
                <p className="text-sm">Chưa có món nào.<br />Chạm “+” để thêm.</p>
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
                      <Stepper onClick={() => changeQty(c.id, -1)}><Minus className="w-4 h-4" /></Stepper>
                      <span className="text-[15px] font-semibold text-ink w-5 text-center tnum">{c.qty}</span>
                      <Stepper onClick={() => changeQty(c.id, 1)}><Plus className="w-4 h-4" /></Stepper>
                    </div>
                    <button onClick={() => removeLine(c.id)} className="text-faint hover:text-danger cursor-pointer" aria-label="Xóa">
                      <X className="w-4 h-4" />
                    </button>
                  </div>
                  <input
                    value={c.note}
                    onChange={(e) => setNote(c.id, e.target.value)}
                    placeholder="Ghi chú..."
                    className="mt-2.5 w-full text-[13px] text-ink bg-bg rounded-[10px] px-3 py-2 placeholder:text-faint focus:outline-none focus:bg-ink/[0.04]"
                  />
                </div>
              ))
            )}
          </div>

          <div className="px-5 py-5 border-t border-line">
            <div className="flex items-baseline justify-between mb-3.5">
              <span className="text-[15px] text-muted">Tổng cộng</span>
              <span className="text-[26px] font-bold tracking-[-0.01em] text-ink tnum">{formatVnd(total)}</span>
            </div>
            <Button onClick={submit} disabled={!cart.length || submitting} className="w-full h-12">
              {submitting ? 'Đang gửi...' : 'Gửi đơn'}
            </Button>
          </div>
        </Card>
      </div>

      <Toast toast={toast} onClose={() => setToast(null)} />
    </div>
  )
}

function MenuRow({ item, onAdd }) {
  const off = !item.available
  return (
    <div className={`flex items-center justify-between gap-4 px-5 py-4 ${off ? 'opacity-55' : ''}`}>
      <div className="min-w-0">
        <p className="text-[15px] font-medium text-ink">{item.name}</p>
        <p className={`text-[13px] mt-0.5 ${off ? 'text-danger font-medium' : 'text-muted'}`}>
          {off ? 'Tạm hết' : item.description}
        </p>
      </div>
      <div className="flex items-center gap-4 shrink-0">
        <span className="text-[15px] font-semibold text-ink tnum">{formatVnd(item.price)}</span>
        <button
          onClick={onAdd}
          disabled={off}
          aria-label={`Thêm ${item.name}`}
          className="w-9 h-9 rounded-full bg-accent-soft text-accent flex items-center justify-center hover:brightness-95 active:scale-95 transition disabled:opacity-40 disabled:active:scale-100 cursor-pointer"
        >
          <Plus className="w-[18px] h-[18px]" strokeWidth={2.5} />
        </button>
      </div>
    </div>
  )
}

function Stepper({ onClick, children }) {
  return (
    <button
      onClick={onClick}
      className="w-7 h-7 rounded-full bg-ink/[0.06] text-ink flex items-center justify-center hover:bg-ink/[0.1] active:scale-95 transition cursor-pointer"
    >
      {children}
    </button>
  )
}
