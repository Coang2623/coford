import { Routes, Route } from 'react-router-dom'
import AppShell from './components/AppShell'
import OrderPage from './pages/OrderPage'
import OrdersPage from './pages/OrdersPage'
import OrderDetailPage from './pages/OrderDetailPage'
import PaymentPage from './pages/PaymentPage'
import ReportPage from './pages/ReportPage'
import MenuAdminPage from './pages/MenuAdminPage'
import KitchenPage from './pages/KitchenPage'
import BankPage from './pages/BankPage'

export default function App() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route path="/" element={<OrderPage />} />
        <Route path="/orders" element={<OrdersPage />} />
        <Route path="/orders/:id" element={<OrderDetailPage />} />
        <Route path="/orders/:id/pay" element={<PaymentPage />} />
        <Route path="/kitchen" element={<KitchenPage />} />
        <Route path="/reports" element={<ReportPage />} />
        <Route path="/bank" element={<BankPage />} />
        <Route path="/menu" element={<MenuAdminPage />} />
      </Route>
    </Routes>
  )
}
