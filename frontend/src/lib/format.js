// Định dạng tiền VND: 25000 -> "25.000đ"
export function formatVnd(value) {
  const n = Number(value || 0)
  return n.toLocaleString('vi-VN') + 'đ'
}

// "2026-06-16T14:32:00Z" -> "16/06/2026 14:32"
export function formatDateTime(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  const pad = (x) => String(x).padStart(2, '0')
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

export function formatTime(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  const pad = (x) => String(x).padStart(2, '0')
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`
}
