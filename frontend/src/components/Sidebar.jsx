import { NavLink } from 'react-router-dom'
import { LayoutGrid, ReceiptText, ChefHat, TrendingUp, BookOpen, Landmark, LogOut } from 'lucide-react'
import { hasRole, currentUser, logout } from '../auth'

const NAV = [
  { to: '/', label: 'Tạo đơn', icon: LayoutGrid, end: true },
  { to: '/orders', label: 'Đơn hàng', icon: ReceiptText },
  { to: '/kitchen', label: 'Màn hình bếp', icon: ChefHat },
  { to: '/bank', label: 'Ngân hàng', icon: Landmark, role: 'MANAGER' },
  { to: '/reports', label: 'Báo cáo', icon: TrendingUp, role: 'MANAGER' },
  { to: '/menu', label: 'Quản lý menu', icon: BookOpen, role: 'MANAGER' },
]

export default function Sidebar() {
  const user = currentUser()
  const items = NAV.filter((i) => !i.role || hasRole(i.role))
  const initials = (user.name || user.username || '?').trim().slice(0, 2).toUpperCase()

  return (
    <aside className="hidden lg:flex w-[248px] shrink-0 flex-col px-4 py-6">
      <div className="flex items-baseline gap-1 px-3 mb-8">
        <span className="text-[26px] font-bold tracking-[-0.02em] text-ink leading-none">Coford</span>
        <span className="w-1.5 h-1.5 rounded-full bg-accent" />
      </div>

      <nav className="flex flex-col gap-1">
        {items.map(({ to, label, icon: Icon, end }) => (
          <NavLink
            key={to}
            to={to}
            end={end}
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 h-11 rounded-[12px] text-[15px] transition-colors ${
                isActive ? 'bg-accent-soft text-accent font-semibold' : 'text-muted hover:bg-ink/[0.04] font-medium'
              }`
            }
          >
            <Icon className="w-[20px] h-[20px]" strokeWidth={2} />
            <span>{label}</span>
          </NavLink>
        ))}
      </nav>

      <div className="mt-auto flex items-center gap-3 px-2 py-2 rounded-[14px] bg-surface shadow-[0_1px_2px_rgba(0,0,0,0.04)]">
        <div className="w-9 h-9 rounded-full bg-accent-soft flex items-center justify-center text-[13px] font-bold text-accent shrink-0">
          {initials}
        </div>
        <div className="flex flex-col leading-tight min-w-0 flex-1">
          <span className="text-sm font-semibold text-ink truncate">{user.name || user.username}</span>
          <span className="text-xs text-muted">{user.role}</span>
        </div>
        <button onClick={logout} title="Đăng xuất" aria-label="Đăng xuất" className="text-muted hover:text-danger cursor-pointer shrink-0">
          <LogOut className="w-[18px] h-[18px]" />
        </button>
      </div>
    </aside>
  )
}
