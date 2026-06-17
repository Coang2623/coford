import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { ArrowLeft, Banknote, CreditCard, QrCode, Printer, Check } from 'lucide-react'
import { api } from '../api'
import { formatVnd, formatDateTime } from '../lib/format'
import { Button, Card, Loading, ErrorState, Toast } from '../components/ui'

const METHODS = [
  ['CASH', 'Tiền mặt', Banknote],
  ['CARD', 'Thẻ ngân hàng', CreditCard],
  ['TRANSFER', 'Chuyển khoản', QrCode],
]
const METHOD_LABEL = { CASH: 'Tiền mặt', CARD: 'Thẻ ngân hàng', TRANSFER: 'Chuyển khoản' }

export default function PaymentPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [order, setOrder] = useState(null)
  const [invoice, setInvoice] = useState(null)
  const [method, setMethod] = useState('CASH')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [paying, setPaying] = useState(false)
  const [toast, setToast] = useState(null)
  const [qrInfo, setQrInfo] = useState(null)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const o = await api.orders.get(id)
      setOrder(o)
      if (o.status === 'PAID') setInvoice(await api.orders.invoice(id))
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }
  function applyQrConfig(backendInfo) {
    try {
      const configStr = localStorage.getItem('coford_qr_config')
      if (configStr) {
        const config = JSON.parse(configStr)
        if (config.source === 'MANUAL' && config.manual) {
          setQrInfo(config.manual)
          return
        }
        if (config.source === 'COREBANK' && config.corebank) {
          setQrInfo(config.corebank)
          return
        }
        if (config.source === 'CUSTOM_IMAGE' && config.customImage) {
          setQrInfo({ customImage: config.customImage })
          return
        }
      }
    } catch (e) {
      console.error('Error applying QR config from localStorage:', e)
    }
    if (backendInfo) {
      setQrInfo(backendInfo)
    }
  }

  function loadQrInfo() {
    api.payment.qrInfo()
      .then(backendInfo => {
        applyQrConfig(backendInfo)
      })
      .catch((err) => {
        console.error('Không tải được thông tin QR từ backend:', err)
        applyQrConfig(null)
      })
  }

  useEffect(() => {
    load()
    loadQrInfo()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id])

  useEffect(() => {
    if (method === 'TRANSFER' && !qrInfo) {
      loadQrInfo()
    }
  }, [method, qrInfo])

  async function confirmPay() {
    setPaying(true)
    try {
      await api.orders.pay(id, method, crypto.randomUUID())
      const [o, inv] = await Promise.all([api.orders.get(id), api.orders.invoice(id)])
      setOrder(o)
      setInvoice(inv)
      setToast({ type: 'success', message: `Đã thu ${formatVnd(o.totalAmount)} (${METHOD_LABEL[method]})` })
    } catch (e) {
      setToast({ type: 'error', message: e.message })
    } finally {
      setPaying(false)
    }
  }

  const paid = order?.status === 'PAID'

  return (
    <div className="px-4 sm:px-6 lg:px-8 py-6 lg:py-8 flex flex-col gap-6 lg:gap-7 min-h-full">
      <header className="flex items-center gap-4 no-print">
        <button onClick={() => navigate('/orders')} className="w-10 h-10 rounded-full bg-ink/[0.05] flex items-center justify-center text-ink hover:bg-ink/[0.09] cursor-pointer" aria-label="Quay lại">
          <ArrowLeft className="w-5 h-5" />
        </button>
        <div>
          <h1 className="text-[30px] font-bold tracking-[-0.02em] leading-none text-ink">
            {paid ? `Hóa đơn #${id}` : `Thanh toán đơn #${id}`}
          </h1>
          <p className="text-[15px] text-muted mt-2">
            {paid ? 'Đơn đã thanh toán' : 'Chọn phương thức và xác nhận thu tiền'}
          </p>
        </div>
      </header>

      {loading ? (
        <Card><Loading /></Card>
      ) : error ? (
        <Card><ErrorState message={error} onRetry={load} /></Card>
      ) : order.status === 'CANCELLED' ? (
        <Card><p className="text-muted p-10 text-center">Đơn này đã bị hủy, không thể thanh toán.</p></Card>
      ) : (
        <div className="flex flex-col lg:flex-row gap-6 lg:gap-7 lg:items-start">
          {!paid && (
            <div className="w-full lg:w-[360px] shrink-0 flex flex-col gap-5 no-print">
              <Card className="px-5 py-5">
                <p className="text-[13px] font-semibold text-muted">SỐ TIỀN CẦN THU · {order.tableNo}</p>
                <p className="text-[40px] font-bold tracking-[-0.02em] leading-tight text-ink tnum mt-1">{formatVnd(order.totalAmount)}</p>
              </Card>

              <div>
                <p className="text-[13px] font-semibold text-muted px-1 mb-2">PHƯƠNG THỨC THANH TOÁN</p>
                <Card className="overflow-hidden divide-y divide-line">
                  {METHODS.map(([val, label, Icon]) => {
                    const sel = method === val
                    return (
                      <button
                        key={val}
                        onClick={() => setMethod(val)}
                        className={`w-full flex items-center gap-3 px-4 py-3.5 transition-colors cursor-pointer ${sel ? 'bg-accent-soft' : 'hover:bg-ink/[0.02]'}`}
                      >
                        <Icon className={`w-5 h-5 ${sel ? 'text-accent' : 'text-muted'}`} strokeWidth={2} />
                        <span className={`flex-1 text-left text-[15px] ${sel ? 'font-semibold text-ink' : 'text-ink'}`}>{label}</span>
                        <span className={`w-[22px] h-[22px] rounded-full flex items-center justify-center ${sel ? 'bg-accent' : 'border-2 border-line-strong'}`}>
                          {sel && <Check className="w-3.5 h-3.5 text-white" strokeWidth={3} />}
                        </span>
                      </button>
                    )
                  })}
                </Card>
              </div>

              {method === 'TRANSFER' && qrInfo && (
                <QrCard info={qrInfo} amount={order.totalAmount} content={`COFORD${order.id}`} />
              )}

              <Button onClick={confirmPay} disabled={paying} className="w-full h-12 text-base">
                {paying ? 'Đang xử lý...' : 'Xác nhận thanh toán'}
              </Button>
            </div>
          )}

          <div className="flex-1 min-w-0 w-full flex flex-col items-center gap-5">
            <InvoiceCard
              orderId={order.id}
              tableNo={order.tableNo}
              lines={paid ? invoice.lines : order.items}
              total={order.totalAmount}
              method={paid ? invoice.method : method}
              dateTime={paid ? invoice.paidAt : order.createdAt}
              paid={paid}
            />
            {paid && (
              <div className="flex gap-3 w-full max-w-[400px] no-print">
                <Button onClick={() => window.print()} className="flex-1 h-12">
                  <Printer className="w-[18px] h-[18px]" /> In hóa đơn
                </Button>
                <Button variant="ghost" onClick={() => navigate('/orders')} className="flex-1 h-12">
                  Về danh sách
                </Button>
              </div>
            )}
          </div>
        </div>
      )}

      <Toast toast={toast} onClose={() => setToast(null)} />
    </div>
  )
}

function InvoiceCard({ orderId, tableNo, lines, total, method, dateTime, paid }) {
  return (
    <Card className="printable w-full max-w-[400px] px-6 sm:px-8 py-8 flex flex-col gap-5">
      <div className="flex flex-col items-center gap-1.5 pb-1">
        <span className="text-[24px] font-bold tracking-[-0.02em] text-ink leading-none">Coford</span>
        <span className="text-[10px] font-bold tracking-[0.18em] text-accent uppercase">
          {paid ? 'Phiếu thanh toán' : 'Tạm tính'}
        </span>
      </div>

      <div className="flex justify-between text-xs">
        <div className="flex flex-col gap-0.5">
          <span className="font-semibold text-ink text-sm">Đơn #{orderId}</span>
          <span className="text-muted">Bàn {tableNo}</span>
        </div>
        <span className="text-muted self-end tnum">{formatDateTime(dateTime)}</span>
      </div>

      <div className="h-px bg-line" />

      <div className="flex flex-col gap-2.5">
        {lines.map((l, i) => (
          <div key={i} className="flex justify-between text-[15px]">
            <span className="text-ink">{l.itemName} <span className="text-muted">×{l.quantity}</span></span>
            <span className="font-medium text-ink tnum">{formatVnd(l.lineTotal)}</span>
          </div>
        ))}
      </div>

      <div className="h-px bg-line" />

      <div className="flex items-baseline justify-between">
        <span className="text-[15px] text-muted">Tổng cộng</span>
        <span className="text-[24px] font-bold tracking-[-0.01em] text-ink tnum">{formatVnd(total)}</span>
      </div>
      <div className="flex justify-between text-[13px]">
        <span className="text-muted">Phương thức</span>
        <span className="font-medium text-ink">{METHOD_LABEL[method] || method}</span>
      </div>

      <p className="text-center text-xs text-muted pt-1">Cảm ơn quý khách, hẹn gặp lại!</p>
    </Card>
  )
}

function QrCard({ info, amount, content }) {
  const [err, setErr] = useState(false)
  const isCustom = !!info.customImage
  const url = isCustom ? info.customImage :
    `https://img.vietqr.io/image/${info.bank}-${info.accountNumber}-qr_only.png` +
    `?amount=${Math.round(amount)}&addInfo=${encodeURIComponent(content)}&accountName=${encodeURIComponent(info.accountName)}`
  const Row = ({ label, value, strong }) => (
    <div className="flex items-center justify-between gap-3 py-1.5 border-b border-line last:border-0">
      <span className="text-[13px] text-muted">{label}</span>
      <span className={`text-sm text-ink text-right ${strong ? 'font-bold' : 'font-medium'}`}>{value}</span>
    </div>
  )
  return (
    <Card className="p-5 flex flex-col items-center gap-4">
      <p className="text-[13px] font-semibold uppercase tracking-wide text-muted self-start">Quét QR để chuyển khoản</p>
      {err ? (
        <div className="w-[200px] h-[200px] rounded-[12px] bg-bg flex items-center justify-center text-center text-xs text-muted px-4">
          Không tải được QR. Vui lòng chuyển khoản thủ công theo thông tin bên dưới.
        </div>
      ) : (
        <img
          src={url}
          alt="Mã QR chuyển khoản"
          width={200}
          height={200}
          className="rounded-[12px] border border-line"
          onError={() => setErr(true)}
        />
      )}
      <div className="w-full">
        {info.bank && <Row label="Ngân hàng" value={info.bank} />}
        {info.accountNumber && <Row label="Số tài khoản" value={info.accountNumber} />}
        {info.accountName && <Row label="Chủ tài khoản" value={info.accountName} />}
        <Row label="Số tiền" value={formatVnd(amount)} strong />
        <Row label="Nội dung" value={content} />
      </div>
    </Card>
  )
}
