import { useEffect } from 'react'
import { Loader2, Inbox, AlertCircle, X, CheckCircle2 } from 'lucide-react'

const VARIANTS = {
  primary: 'bg-accent text-white hover:brightness-105 active:brightness-95',
  ink: 'bg-ink text-white hover:bg-ink/90',
  ghost: 'bg-ink/[0.05] text-ink hover:bg-ink/[0.09]',
}

export function Button({ variant = 'primary', className = '', children, ...rest }) {
  return (
    <button
      className={`inline-flex items-center justify-center gap-2 rounded-[12px] font-semibold text-[15px] px-5 h-11 transition active:scale-[0.99] disabled:opacity-50 disabled:active:scale-100 disabled:cursor-not-allowed cursor-pointer ${VARIANTS[variant]} ${className}`}
      {...rest}
    >
      {children}
    </button>
  )
}

export function TextButton({ className = '', children, ...rest }) {
  return (
    <button className={`text-[15px] font-semibold text-accent hover:opacity-70 cursor-pointer disabled:opacity-50 ${className}`} {...rest}>
      {children}
    </button>
  )
}

export function IconButton({ className = '', title, children, ...rest }) {
  return (
    <button
      title={title}
      aria-label={title}
      className={`inline-flex items-center justify-center w-9 h-9 rounded-[10px] text-muted hover:text-ink hover:bg-ink/[0.05] transition cursor-pointer ${className}`}
      {...rest}
    >
      {children}
    </button>
  )
}

export function Card({ className = '', children }) {
  return <div className={`bg-surface rounded-[20px] shadow-[0_1px_2px_rgba(0,0,0,0.04)] ${className}`}>{children}</div>
}
export const Panel = Card

export function PageHeader({ title, subtitle, children }) {
  return (
    <header className="flex items-end justify-between flex-wrap gap-3">
      <div>
        <h1 className="text-[30px] font-bold tracking-[-0.02em] leading-none text-ink">{title}</h1>
        {subtitle && <p className="text-[15px] text-muted mt-2">{subtitle}</p>}
      </div>
      {children}
    </header>
  )
}

export function Segmented({ options, value, onChange }) {
  return (
    <div className="inline-flex p-1 rounded-[12px] bg-ink/[0.06] gap-1">
      {options.map((o) => {
        const sel = o.value === value
        return (
          <button
            key={String(o.value)}
            onClick={() => onChange(o.value)}
            className={`px-4 h-9 rounded-[9px] text-sm transition cursor-pointer ${
              sel ? 'bg-surface text-ink font-semibold shadow-sm' : 'text-muted hover:text-ink font-medium'
            }`}
          >
            {o.label}
          </button>
        )
      })}
    </div>
  )
}

export function StatusText({ status }) {
  const map = {
    NEW: ['Chờ thanh toán', 'bg-accent-soft text-accent'],
    PAID: ['Đã thanh toán', 'bg-success-soft text-success'],
    CANCELLED: ['Đã hủy', 'bg-ink/[0.06] text-muted'],
  }
  const [label, cls] = map[status] || [status, 'bg-ink/[0.06] text-muted']
  return <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold ${cls}`}>{label}</span>
}

export function Spinner({ className = '' }) {
  return <Loader2 className={`animate-spin ${className}`} />
}

export function Loading({ label = 'Đang tải...' }) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-20 text-muted">
      <Spinner className="w-6 h-6 text-accent" />
      <span className="text-sm">{label}</span>
    </div>
  )
}

export function EmptyState({ title = 'Chưa có dữ liệu', hint }) {
  return (
    <div className="flex flex-col items-center justify-center gap-2 py-16 text-center">
      <div className="w-14 h-14 rounded-full bg-ink/[0.05] flex items-center justify-center">
        <Inbox className="w-6 h-6 text-faint" strokeWidth={1.75} />
      </div>
      <p className="font-semibold text-ink mt-1">{title}</p>
      {hint && <p className="text-sm text-muted max-w-xs">{hint}</p>}
    </div>
  )
}

export function ErrorState({ message, onRetry }) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-16 text-center">
      <div className="w-14 h-14 rounded-full bg-danger-soft flex items-center justify-center">
        <AlertCircle className="w-6 h-6 text-danger" strokeWidth={1.75} />
      </div>
      <p className="font-semibold text-ink">Không tải được dữ liệu</p>
      <p className="text-sm text-muted max-w-md">{message}</p>
      {onRetry && <TextButton onClick={onRetry}>Thử lại</TextButton>}
    </div>
  )
}

export function Modal({ open, onClose, title, children, footer }) {
  useEffect(() => {
    if (!open) return
    const onKey = (e) => e.key === 'Escape' && onClose?.()
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open, onClose])

  if (!open) return null
  return (
    <div className="fixed inset-0 z-40 flex items-center justify-center p-4" role="dialog" aria-modal="true">
      <div className="absolute inset-0 bg-ink/30 backdrop-blur-[2px]" onClick={onClose} />
      <div className="relative z-10 w-full max-w-md bg-surface rounded-[22px] shadow-2xl overflow-hidden">
        <div className="flex items-center justify-between px-6 pt-5 pb-3">
          <h2 className="text-xl font-bold tracking-[-0.01em] text-ink">{title}</h2>
          <button onClick={onClose} aria-label="Đóng" className="w-8 h-8 rounded-full bg-ink/[0.05] flex items-center justify-center text-muted hover:text-ink cursor-pointer">
            <X className="w-4 h-4" />
          </button>
        </div>
        <div className="px-6 py-3">{children}</div>
        {footer && <div className="flex justify-end gap-3 px-6 py-4">{footer}</div>}
      </div>
    </div>
  )
}

export function Toast({ toast, onClose }) {
  useEffect(() => {
    if (!toast) return
    const t = setTimeout(() => onClose?.(), 3200)
    return () => clearTimeout(t)
  }, [toast, onClose])

  if (!toast) return null
  const ok = toast.type !== 'error'
  return (
    <div className="fixed bottom-6 right-6 z-50 no-print">
      <div className="flex items-center gap-3 px-4 py-3 rounded-[14px] bg-surface shadow-lg border border-line">
        {ok ? <CheckCircle2 className="w-5 h-5 text-success" /> : <AlertCircle className="w-5 h-5 text-danger" />}
        <span className="text-sm font-medium text-ink">{toast.message}</span>
      </div>
    </div>
  )
}

export function Field({ label, children }) {
  return (
    <label className="flex flex-col gap-1.5">
      <span className="text-[13px] font-semibold text-ink">{label}</span>
      {children}
    </label>
  )
}

export const inputClass =
  'w-full h-11 px-3.5 rounded-[12px] bg-bg border border-transparent text-ink text-[15px] placeholder:text-faint focus:outline-none focus:border-accent focus:bg-surface transition'
