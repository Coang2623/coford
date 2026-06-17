import { NavLink } from 'react-router-dom'
import { LayoutGrid, ReceiptText, ChefHat, TrendingUp, BookOpen } from 'lucide-react'
import { hasRole } from '../auth'

const NAV = [
  { to: '/', label: 'Tạo đơn', icon: LayoutGrid, end: true },
  { to: '/orders', label: 'Đơn', icon: ReceiptText },
  { to: '/kitchen', label: 'Bếp', icon: ChefHat },
  { to: '/reports', label: 'Báo cáo', icon: TrendingUp, role: 'MANAGER' },
  { to: '/menu', label: 'Menu', icon: BookOpen, role: 'MANAGER' },
]

// Thanh tab dưới đáy cho điện thoại (kiểu iOS tab bar). Ẩn trên desktop.
export default function BottomNav() {
  const items = NAV.filter((i) => !i.role || hasRole(i.role))
  return (
    <nav
      className="lg:hidden fixed bottom-0 inset-x-0 z-30 bg-surface/95 backdrop-blur border-t border-line flex"
      style={{ paddingBottom: 'env(safe-area-inset-bottom)' }}
    >
      {items.map(({ to, label, icon: Icon, end }) => (
        <NavLink
          key={to}
          to={to}
          end={end}
          className={({ isActive }) =>
            `flex-1 flex flex-col items-center justify-center gap-1 h-16 text-[11px] font-medium transition-colors ${
              isActive ? 'text-accent' : 'text-muted'
            }`
          }
        >
          <Icon className="w-[22px] h-[22px]" strokeWidth={2} />
          <span>{label}</span>
        </NavLink>
      ))}
    </nav>
  )
}
