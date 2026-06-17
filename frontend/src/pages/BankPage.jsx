import { useEffect, useState } from 'react'
import { Landmark, ArrowDownLeft, Check, LogIn, ShieldCheck } from 'lucide-react'
import { api } from '../api'
import { formatVnd } from '../lib/format'
import { Button, Card, PageHeader, Segmented, Field, inputClass, Loading, ErrorState, EmptyState, Toast } from '../components/ui'

const COMMON_BANKS = [
  { code: 'MB', name: 'MB - Ngân hàng Quân Đội' },
  { code: 'VCB', name: 'VCB - Vietcombank' },
  { code: 'BIDV', name: 'BIDV - Ngân hàng Đầu tư & Phát triển' },
  { code: 'TCB', name: 'TCB - Techcombank' },
  { code: 'ACB', name: 'ACB - Ngân hàng Á Châu' },
  { code: 'VPB', name: 'VPB - VPBank' },
  { code: 'TPB', name: 'TPB - TPBank' },
  { code: 'ICB', name: 'ICB - VietinBank' },
  { code: 'VIB', name: 'VIB - Ngân hàng Quốc tế' },
]

export default function BankPage() {
  const [status, setStatus] = useState(null)
  const [balance, setBalance] = useState(null)
  const [rows, setRows] = useState([])
  const [days, setDays] = useState(30)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [confirming, setConfirming] = useState(null)
  const [form, setForm] = useState({ username: '', password: '' })
  const [loggingIn, setLoggingIn] = useState(false)
  const [toast, setToast] = useState(null)

  // QR Payment Configuration State
  const [backendQr, setBackendQr] = useState(null)
  const [qrConfig, setQrConfig] = useState({
    source: 'DEFAULT',
    manual: { bank: 'MB', accountNumber: '', accountName: '' },
    corebank: { bank: 'MB', accountNumber: '', accountName: '' },
    customImage: ''
  })

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const st = await api.bank.status()
      setStatus(st)
      if (st.loggedIn) {
        const [bal, rec] = await Promise.all([api.bank.balance(), api.bank.reconcile(days)])
        setBalance(bal)
        setRows(rec)
      }
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [days])

  // Load backend default QR and local storage settings on mount
  useEffect(() => {
    api.payment.qrInfo()
      .then(setBackendQr)
      .catch((e) => console.error('Error fetching backend qrInfo:', e))

    const saved = localStorage.getItem('coford_qr_config')
    if (saved) {
      try {
        const parsed = JSON.parse(saved)
        setQrConfig(prev => ({
          ...prev,
          ...parsed,
          manual: { ...prev.manual, ...(parsed.manual || {}) },
          corebank: { ...prev.corebank, ...(parsed.corebank || {}) }
        }))
      } catch (e) {
        console.error(e)
      }
    }
  }, [])

  // Auto-fill CoreBank details if logged in and corebank settings are empty
  const loggedIn = status?.loggedIn
  useEffect(() => {
    if (loggedIn && balance?.accounts?.length > 0 && !qrConfig.corebank.accountNumber) {
      const firstAcc = balance.accounts[0]
      setQrConfig(prev => ({
        ...prev,
        corebank: {
          ...prev.corebank,
          accountNumber: firstAcc.number,
          accountName: firstAcc.name
        }
      }))
    }
  }, [loggedIn, balance, qrConfig.corebank.accountNumber])

  function handleImageUpload(e) {
    const file = e.target.files[0]
    if (!file) return
    if (!file.type.startsWith('image/')) {
      setToast({ type: 'error', message: 'Vui lòng chọn file ảnh hợp lệ' })
      return
    }
    const reader = new FileReader()
    reader.onload = (event) => {
      setQrConfig(prev => ({ ...prev, customImage: event.target.result }))
    }
    reader.readAsDataURL(file)
  }

  function saveQrConfig(e) {
    if (e) e.preventDefault()
    if (qrConfig.source === 'MANUAL') {
      if (!qrConfig.manual.accountNumber || !qrConfig.manual.accountName) {
        setToast({ type: 'error', message: 'Vui lòng điền đầy đủ số tài khoản và tên chủ tài khoản' })
        return
      }
    }
    if (qrConfig.source === 'COREBANK') {
      if (!qrConfig.corebank.accountNumber || !qrConfig.corebank.accountName) {
        setToast({ type: 'error', message: 'Vui lòng chọn và điền đầy đủ thông tin tài khoản CoreBank' })
        return
      }
    }
    if (qrConfig.source === 'CUSTOM_IMAGE' && !qrConfig.customImage) {
      setToast({ type: 'error', message: 'Vui lòng tải lên ảnh mã QR nhận tiền' })
      return
    }

    localStorage.setItem('coford_qr_config', JSON.stringify(qrConfig))
    setToast({ type: 'success', message: 'Đã lưu cấu hình QR thanh toán thành công' })
  }

  async function doLogin(e) {
    e.preventDefault()
    setLoggingIn(true)
    try {
      const r = await api.bank.login(form.username.trim(), form.password)
      if (r.success) {
        setToast({ type: 'success', message: `Đã đăng nhập: ${r.customerName || form.username}` })
        setForm({ username: '', password: '' })
        load()
      } else {
        setToast({ type: 'error', message: r.message || 'Đăng nhập thất bại' })
      }
    } catch (e) {
      setToast({ type: 'error', message: e.message })
    } finally {
      setLoggingIn(false)
    }
  }

  async function confirmReceipt(orderId) {
    setConfirming(orderId)
    try {
      await api.orders.pay(orderId, 'TRANSFER', crypto.randomUUID())
      setToast({ type: 'success', message: `Đã xác nhận thu cho đơn #${orderId}` })
      load()
    } catch (e) {
      setToast({ type: 'error', message: e.message })
    } finally {
      setConfirming(null)
    }
  }

  const sources = [
    { label: 'Mặc định', value: 'DEFAULT' },
    { label: 'Thủ công', value: 'MANUAL' },
    { label: 'Ảnh QR', value: 'CUSTOM_IMAGE' },
    { label: 'CoreBank', value: 'COREBANK', disabled: !loggedIn },
  ]

  return (
    <div className="px-4 sm:px-6 lg:px-8 py-6 lg:py-8 flex flex-col gap-6 min-h-full">
      <PageHeader title="Ngân hàng" subtitle="Số dư & đối soát chuyển khoản qua CoreBank">
        {loggedIn && (
          <Segmented
            options={[{ label: '7 ngày', value: 7 }, { label: '30 ngày', value: 30 }, { label: '90 ngày', value: 90 }]}
            value={days}
            onChange={setDays}
          />
        )}
      </PageHeader>

      <div className="flex flex-col lg:flex-row gap-6 lg:gap-7 lg:items-start">
        {/* Left column: CoreBank information / Login */}
        <div className="flex-1 min-w-0 w-full flex flex-col gap-6">
          {loading ? (
            <Card><Loading label="Đang gọi CoreBank..." /></Card>
          ) : error ? (
            <Card><ErrorState message={error} onRetry={load} /></Card>
          ) : !loggedIn ? (
            // Chưa đăng nhập -> form đăng nhập CoreBank
            <Card className="w-full p-6">
              <div className="flex items-center gap-2.5 mb-1">
                <div className="w-9 h-9 rounded-full bg-accent-soft flex items-center justify-center">
                  <Landmark className="w-[18px] h-[18px] text-accent" />
                </div>
                <h2 className="text-lg font-bold tracking-[-0.01em] text-ink">Đăng nhập CoreBank</h2>
              </div>
              <p className="text-sm text-muted mb-4">Đăng nhập để xem số dư và đối soát chuyển khoản.</p>
              <form onSubmit={doLogin} className="flex flex-col gap-4">
                <Field label="Tên đăng nhập / Số điện thoại">
                  <input
                    className={inputClass}
                    value={form.username}
                    onChange={(e) => setForm({ ...form, username: e.target.value })}
                    placeholder="0912345678"
                    autoComplete="username"
                  />
                </Field>
                <Field label="Mật khẩu">
                  <input
                    type="password"
                    className={inputClass}
                    value={form.password}
                    onChange={(e) => setForm({ ...form, password: e.target.value })}
                    placeholder="••••••••"
                    autoComplete="current-password"
                  />
                </Field>
                <Button type="submit" disabled={loggingIn} className="w-full h-12">
                  <LogIn className="w-[18px] h-[18px]" />
                  {loggingIn ? 'Đang đăng nhập...' : 'Đăng nhập'}
                </Button>
              </form>
            </Card>
          ) : (
            <>
              {/* Đã đăng nhập */}
              <div className="flex items-center gap-2 -mt-2">
                <span className="inline-flex items-center gap-1.5 px-3 h-8 rounded-full bg-success-soft text-success text-sm font-semibold">
                  <ShieldCheck className="w-4 h-4" />
                  Đã đăng nhập{status?.username ? `: ${status.username}` : ''}
                </span>
              </div>

              {/* Số dư */}
              <Card className="px-5 py-5">
                <div className="flex items-center gap-2 text-muted mb-1">
                  <Landmark className="w-4 h-4" />
                  <span className="text-[13px] font-semibold uppercase tracking-wide">Tổng số dư</span>
                </div>
                <p className="text-[34px] font-bold tracking-[-0.02em] text-ink tnum">
                  {formatVnd(balance?.total)} <span className="text-base font-semibold text-muted">{balance?.currency}</span>
                </p>
                <div className="mt-4 divide-y divide-line border-t border-line">
                  {(balance?.accounts || []).map((a) => (
                    <div key={a.number} className="flex items-center justify-between py-2.5">
                      <div>
                        <p className="text-[15px] font-medium text-ink">{a.name}</p>
                        <p className="text-xs text-muted tnum">{a.number}</p>
                      </div>
                      <span className="text-[15px] font-semibold text-ink tnum">{formatVnd(a.balance)}</span>
                    </div>
                  ))}
                </div>
              </Card>

              {/* Đối soát tiền vào */}
              <div>
                <h2 className="text-[13px] font-semibold uppercase tracking-wide text-muted px-1 mb-2">
                  Tiền vào & đối soát đơn ({days} ngày)
                </h2>
                <Card className="overflow-hidden">
                  {rows.length === 0 ? (
                    <EmptyState title="Chưa có giao dịch tiền vào" hint="Giao dịch chuyển khoản đến sẽ hiện ở đây để khớp với đơn." />
                  ) : (
                    <div className="divide-y divide-line">
                      {rows.map((row, i) => (
                        <div key={i} className="flex items-center gap-4 px-5 py-3.5">
                          <div className="w-9 h-9 rounded-full bg-success-soft flex items-center justify-center shrink-0">
                            <ArrowDownLeft className="w-[18px] h-[18px] text-success" strokeWidth={2} />
                          </div>
                          <div className="flex-1 min-w-0">
                            <p className="text-[15px] font-medium text-ink truncate">
                              {row.txn.counterparty || row.txn.description || 'Chuyển khoản đến'}
                            </p>
                            <p className="text-xs text-muted truncate">
                              {row.txn.date}{row.txn.bank ? ` · ${row.txn.bank}` : ''}{row.txn.refNo ? ` · ${row.txn.refNo}` : ''}
                            </p>
                          </div>
                          <div className="flex flex-col items-end gap-1 shrink-0">
                            <span className="text-[15px] font-semibold text-success tnum">+{formatVnd(row.txn.amount)}</span>
                            {row.matchedOrderId ? (
                              <button
                                onClick={() => confirmReceipt(row.matchedOrderId)}
                                disabled={confirming === row.matchedOrderId}
                                className="inline-flex items-center gap-1 h-8 px-3 rounded-[10px] bg-accent text-white text-xs font-semibold hover:brightness-105 disabled:opacity-50 cursor-pointer"
                              >
                                <Check className="w-3.5 h-3.5" />
                                Thu cho #{row.matchedOrderId} · {row.matchedTable}
                              </button>
                            ) : (
                              <span className="text-xs text-muted">Chưa khớp đơn</span>
                            )}
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </Card>
              </div>
            </>
          )}
        </div>

        {/* Right column: QR Code Payment configuration */}
        <div className="w-full lg:w-[400px] shrink-0">
          <Card className="p-6 flex flex-col gap-5">
            <div>
              <h2 className="text-lg font-bold tracking-[-0.01em] text-ink">Cấu hình QR nhận tiền</h2>
              <p className="text-sm text-muted mt-1">Chọn nguồn thông tin hiển thị mã QR khi thanh toán chuyển khoản.</p>
            </div>

            {/* Segmented Control sources */}
            <div className="flex flex-col gap-1.5">
              <span className="text-[13px] font-semibold text-ink">Nguồn thông tin QR</span>
              <div className="grid grid-cols-2 gap-1.5 bg-ink/[0.06] p-1 rounded-[12px]">
                {sources.map(src => {
                  const active = qrConfig.source === src.value
                  const disabled = src.disabled
                  return (
                    <button
                      key={src.value}
                      type="button"
                      disabled={disabled}
                      onClick={() => setQrConfig(prev => ({ ...prev, source: src.value }))}
                      className={`h-9 rounded-[9px] text-xs font-semibold transition cursor-pointer disabled:opacity-30 disabled:cursor-not-allowed ${
                        active ? 'bg-surface text-ink shadow-sm' : 'text-muted hover:text-ink'
                      }`}
                    >
                      {src.label}
                    </button>
                  )
                })}
              </div>
            </div>

            {/* Source Details Panel */}
            {qrConfig.source === 'DEFAULT' && (
              <div className="flex flex-col gap-2 bg-bg p-4 rounded-[16px] border border-line">
                <p className="text-xs font-semibold text-muted uppercase">Mặc định hệ thống</p>
                {backendQr ? (
                  <div className="text-sm flex flex-col gap-1.5 mt-1">
                    <p><span className="text-muted">Ngân hàng:</span> <span className="font-semibold text-ink">{backendQr.bank}</span></p>
                    <p><span className="text-muted">Số tài khoản:</span> <span className="font-semibold text-ink tnum">{backendQr.accountNumber}</span></p>
                    <p><span className="text-muted">Chủ tài khoản:</span> <span className="font-semibold text-ink">{backendQr.accountName}</span></p>
                  </div>
                ) : (
                  <p className="text-xs text-muted">Đang tải...</p>
                )}
              </div>
            )}

            {qrConfig.source === 'MANUAL' && (
              <div className="flex flex-col gap-4">
                <Field label="Ngân hàng">
                  <select
                    className={`${inputClass} appearance-none pr-8 bg-[url('data:image/svg+xml;charset=US-ASCII,%3Csvg%20xmlns%3D%22http%3A//www.w3.org/2000/svg%22%20width%3D%22292.4%22%20height%3D%22292.4%22%3E%3Cpath%20fill%3D%22%236F6A63%22%20d%3D%22M287%2069.4a17.6%2017.6%200%200%200-13-5.4H18.4c-5%200-9.3%201.8-12.9%205.4A17.6%2017.6%200%200%200%200%2082.2c0%205%201.8%209.3%205.4%2012.9l128%20127.9c3.6%203.6%207.8%205.4%2012.8%205.4s9.2-1.8%2012.8-5.4L287%2095c3.5-3.5%205.4-7.8%205.4-12.8%200-5-1.9-9.2-5.5-12.8z%22/%3E%3C/svg%3E')] bg-[length:12px_12px] bg-[right_14px_center] bg-no-repeat`}
                    value={qrConfig.manual.bank}
                    onChange={(e) => setQrConfig(prev => ({
                      ...prev,
                      manual: { ...prev.manual, bank: e.target.value }
                    }))}
                  >
                    {COMMON_BANKS.map(b => (
                      <option key={b.code} value={b.code}>{b.name}</option>
                    ))}
                  </select>
                </Field>
                <Field label="Số tài khoản">
                  <input
                    type="text"
                    className={inputClass}
                    placeholder="Nhập số tài khoản"
                    value={qrConfig.manual.accountNumber}
                    onChange={(e) => setQrConfig(prev => ({
                      ...prev,
                      manual: { ...prev.manual, accountNumber: e.target.value.replace(/\D/g, '') }
                    }))}
                  />
                </Field>
                <Field label="Tên chủ tài khoản">
                  <input
                    type="text"
                    className={inputClass}
                    placeholder="NGUYEN VAN A"
                    value={qrConfig.manual.accountName}
                    onChange={(e) => setQrConfig(prev => ({
                      ...prev,
                      manual: { ...prev.manual, accountName: e.target.value.toUpperCase() }
                    }))}
                  />
                </Field>
              </div>
            )}

            {qrConfig.source === 'COREBANK' && (
              <div className="flex flex-col gap-4">
                <Field label="Tài khoản CoreBank">
                  <select
                    className={`${inputClass} appearance-none pr-8 bg-[url('data:image/svg+xml;charset=US-ASCII,%3Csvg%20xmlns%3D%22http%3A//www.w3.org/2000/svg%22%20width%3D%22292.4%22%20height%3D%22292.4%22%3E%3Cpath%20fill%3D%22%236F6A63%22%20d%3D%22M287%2069.4a17.6%2017.6%200%200%200-13-5.4H18.4c-5%200-9.3%201.8-12.9%205.4A17.6%2017.6%200%200%200%200%2082.2c0%205%201.8%209.3%205.4%2012.9l128%20127.9c3.6%203.6%207.8%205.4%2012.8%205.4s9.2-1.8%2012.8-5.4L287%2095c3.5-3.5%205.4-7.8%205.4-12.8%200-5-1.9-9.2-5.5-12.8z%22/%3E%3C/svg%3E')] bg-[length:12px_12px] bg-[right_14px_center] bg-no-repeat`}
                    value={qrConfig.corebank.accountNumber}
                    onChange={(e) => {
                      const selectedAcc = balance?.accounts?.find(a => a.number === e.target.value)
                      setQrConfig(prev => ({
                        ...prev,
                        corebank: {
                          ...prev.corebank,
                          accountNumber: e.target.value,
                          accountName: selectedAcc ? selectedAcc.name : prev.corebank.accountName
                        }
                      }))
                    }}
                  >
                    <option value="">-- Chọn tài khoản --</option>
                    {(balance?.accounts || []).map(a => (
                      <option key={a.number} value={a.number}>{a.name} ({a.number})</option>
                    ))}
                  </select>
                </Field>
                <Field label="Ngân hàng của tài khoản">
                  <select
                    className={`${inputClass} appearance-none pr-8 bg-[url('data:image/svg+xml;charset=US-ASCII,%3Csvg%20xmlns%3D%22http%3A//www.w3.org/2000/svg%22%20width%3D%22292.4%22%20height%3D%22292.4%22%3E%3Cpath%20fill%3D%22%236F6A63%22%20d%3D%22M287%2069.4a17.6%2017.6%200%200%200-13-5.4H18.4c-5%200-9.3%201.8-12.9%205.4A17.6%2017.6%200%200%200%200%2082.2c0%205%201.8%209.3%205.4%2012.9l128%20127.9c3.6%203.6%207.8%205.4%2012.8%205.4s9.2-1.8%2012.8-5.4L287%2095c3.5-3.5%205.4-7.8%205.4-12.8%200-5-1.9-9.2-5.5-12.8z%22/%3E%3C/svg%3E')] bg-[length:12px_12px] bg-[right_14px_center] bg-no-repeat`}
                    value={qrConfig.corebank.bank}
                    onChange={(e) => setQrConfig(prev => ({
                      ...prev,
                      corebank: { ...prev.corebank, bank: e.target.value }
                    }))}
                  >
                    {COMMON_BANKS.map(b => (
                      <option key={b.code} value={b.code}>{b.name}</option>
                    ))}
                  </select>
                </Field>
                <Field label="Chủ tài khoản">
                  <input
                    type="text"
                    readOnly
                    className={`${inputClass} bg-ink/[0.03] cursor-not-allowed`}
                    value={qrConfig.corebank.accountName}
                  />
                </Field>
              </div>
            )}

            {qrConfig.source === 'CUSTOM_IMAGE' && (
              <div className="flex flex-col gap-4">
                <Field label="Tải ảnh QR nhận tiền lên">
                  <div className="relative border-2 border-dashed border-line rounded-[12px] p-6 hover:bg-bg transition cursor-pointer group flex flex-col items-center justify-center gap-2">
                    <input
                      type="file"
                      accept="image/*"
                      onChange={handleImageUpload}
                      className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
                    />
                    <span className="text-sm font-semibold text-accent group-hover:underline">Chọn ảnh từ thiết bị</span>
                    <span className="text-xs text-muted">Hỗ trợ định dạng JPG, PNG</span>
                  </div>
                </Field>
                {qrConfig.customImage && (
                  <div className="relative mt-1 w-[160px] h-[160px] mx-auto border border-line rounded-[12px] overflow-hidden bg-bg flex items-center justify-center group">
                    <img src={qrConfig.customImage} alt="QR Preview" className="max-w-full max-h-full object-contain" />
                    <button
                      type="button"
                      onClick={() => setQrConfig(prev => ({ ...prev, customImage: '' }))}
                      className="absolute top-1.5 right-1.5 w-6 h-6 rounded-full bg-danger text-white flex items-center justify-center text-[11px] font-bold cursor-pointer hover:brightness-110 active:scale-90 shadow transition"
                    >
                      ×
                    </button>
                  </div>
                )}
              </div>
            )}

            <Button type="button" onClick={saveQrConfig} className="w-full h-11">
              Lưu cấu hình
            </Button>
          </Card>
        </div>
      </div>

      <Toast toast={toast} onClose={() => setToast(null)} />
    </div>
  )
}
