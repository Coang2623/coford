// API client gọi backend Spring Boot qua proxy /api (cấu hình trong vite.config.js)
import { keycloak } from './auth'

const BASE = '/api'

async function authHeader() {
  try {
    await keycloak.updateToken(30) // gia hạn nếu sắp hết hạn
  } catch {
    // bỏ qua, sẽ gửi token hiện có
  }
  return keycloak.token ? { Authorization: `Bearer ${keycloak.token}` } : {}
}

async function req(path, opts = {}) {
  const res = await fetch(BASE + path, {
    ...opts,
    headers: { 'Content-Type': 'application/json', ...(await authHeader()), ...(opts.headers || {}) },
  })
  if (!res.ok) {
    let message = res.statusText
    try {
      const body = await res.json()
      message = body.message || message
    } catch {
      // body không phải JSON, giữ statusText
    }
    throw new Error(message || 'Đã có lỗi xảy ra')
  }
  if (res.status === 204) return null
  return res.json()
}

function idemHeader(key) {
  return key ? { 'Idempotency-Key': key } : {}
}

function qs(params = {}) {
  const entries = Object.entries(params).filter(([, v]) => v !== undefined && v !== null && v !== '')
  if (!entries.length) return ''
  return '?' + entries.map(([k, v]) => `${k}=${encodeURIComponent(v)}`).join('&')
}

export const api = {
  menu: {
    categories: () => req('/menu/categories'),
    createCategory: (body) => req('/menu/categories', { method: 'POST', body: JSON.stringify(body) }),
    items: (params) => req('/menu/items' + qs(params)),
    createItem: (body) => req('/menu/items', { method: 'POST', body: JSON.stringify(body) }),
    updateItem: (id, body) => req(`/menu/items/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    deleteItem: (id) => req(`/menu/items/${id}`, { method: 'DELETE' }),
  },
  orders: {
    list: (status) => req('/orders' + qs({ status })),
    get: (id) => req(`/orders/${id}`),
    create: (body, idempotencyKey) =>
      req('/orders', { method: 'POST', body: JSON.stringify(body), headers: idemHeader(idempotencyKey) }),
    cancel: (id) => req(`/orders/${id}/cancel`, { method: 'POST' }),
    prepare: (id) => req(`/orders/${id}/prepare`, { method: 'POST' }),
    updateItems: (id, items) => req(`/orders/${id}/items`, { method: 'PUT', body: JSON.stringify({ items }) }),
    pay: (id, method, idempotencyKey) =>
      req(`/orders/${id}/pay`, { method: 'POST', body: JSON.stringify({ method }), headers: idemHeader(idempotencyKey) }),
    invoice: (id) => req(`/orders/${id}/invoice`),
  },
  reports: {
    dailyRevenue: () => req('/reports/daily-revenue'),
    topItems: (limit = 8) => req(`/reports/top-items${qs({ limit })}`),
  },
  kitchen: {
    board: () => req('/kitchen/orders'),
    streamUrl: '/api/kitchen/stream', // dùng cho EventSource (SSE)
  },
  payment: {
    qrInfo: () => req('/payment/qr-info'),
  },
  bank: {
    status: () => req('/bank/status'),
    login: (username, password) => req('/bank/login', { method: 'POST', body: JSON.stringify({ username, password }) }),
    balance: () => req('/bank/balance'),
    transactions: (days = 30) => req(`/bank/transactions${qs({ days })}`),
    reconcile: (days = 30) => req(`/bank/reconcile${qs({ days })}`),
  },
}
