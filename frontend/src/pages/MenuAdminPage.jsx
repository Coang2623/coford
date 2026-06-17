import { useEffect, useMemo, useState } from 'react'
import { Plus, Pencil, Trash2 } from 'lucide-react'
import { api } from '../api'
import { formatVnd } from '../lib/format'
import { Button, IconButton, Card, PageHeader, Segmented, Loading, ErrorState, EmptyState, Modal, Field, Toast, inputClass } from '../components/ui'

const emptyForm = { name: '', categoryId: '', price: '', description: '', available: true }

export default function MenuAdminPage() {
  const [cats, setCats] = useState([])
  const [items, setItems] = useState([])
  const [activeCat, setActiveCat] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [toast, setToast] = useState(null)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [saving, setSaving] = useState(false)

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

  function openAdd() {
    setEditing(null)
    setForm({ ...emptyForm, categoryId: cats[0]?.id || '' })
    setModalOpen(true)
  }
  function openEdit(item) {
    setEditing(item)
    setForm({
      name: item.name,
      categoryId: item.categoryId,
      price: String(item.price),
      description: item.description || '',
      available: item.available,
    })
    setModalOpen(true)
  }

  async function save() {
    if (!form.name.trim() || !form.categoryId || !form.price) {
      setToast({ type: 'error', message: 'Vui lòng nhập tên, danh mục và giá.' })
      return
    }
    setSaving(true)
    const body = {
      categoryId: Number(form.categoryId),
      name: form.name.trim(),
      description: form.description.trim() || null,
      price: Number(form.price),
      available: form.available,
    }
    try {
      if (editing) await api.menu.updateItem(editing.id, body)
      else await api.menu.createItem(body)
      setModalOpen(false)
      setToast({ type: 'success', message: editing ? 'Đã cập nhật món' : 'Đã thêm món mới' })
      load()
    } catch (e) {
      setToast({ type: 'error', message: e.message })
    } finally {
      setSaving(false)
    }
  }

  async function remove(item) {
    if (!confirm(`Xóa món "${item.name}"?`)) return
    try {
      await api.menu.deleteItem(item.id)
      setToast({ type: 'success', message: 'Đã xóa món' })
      load()
    } catch (e) {
      setToast({ type: 'error', message: e.message })
    }
  }

  async function toggle(item) {
    try {
      await api.menu.updateItem(item.id, {
        categoryId: item.categoryId,
        name: item.name,
        description: item.description,
        price: item.price,
        available: !item.available,
      })
      load()
    } catch (e) {
      setToast({ type: 'error', message: e.message })
    }
  }

  const catOptions = [{ label: 'Tất cả', value: null }, ...cats.map((c) => ({ label: c.name, value: c.id }))]

  const StatusToggle = ({ item }) => (
    <button
      onClick={() => toggle(item)}
      title="Bấm để đổi trạng thái"
      className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold cursor-pointer transition ${
        item.available ? 'bg-success-soft text-success' : 'bg-accent-soft text-accent'
      }`}
    >
      {item.available ? 'Đang bán' : 'Tạm hết'}
    </button>
  )
  const RowActions = ({ item }) => (
    <div className="flex gap-1">
      <IconButton title="Sửa" onClick={() => openEdit(item)}>
        <Pencil className="w-4 h-4" strokeWidth={2} />
      </IconButton>
      <IconButton title="Xóa" onClick={() => remove(item)} className="hover:text-danger">
        <Trash2 className="w-4 h-4" strokeWidth={2} />
      </IconButton>
    </div>
  )

  return (
    <div className="px-4 sm:px-6 lg:px-8 py-6 lg:py-8 flex flex-col gap-6 min-h-full">
      <PageHeader title="Quản lý menu" subtitle="Thêm, sửa món và bật tắt trạng thái bán">
        <Button onClick={openAdd}>
          <Plus className="w-[18px] h-[18px]" strokeWidth={2.5} /> Thêm món
        </Button>
      </PageHeader>

      <div className="overflow-x-auto -mx-1 px-1">
        <Segmented options={catOptions} value={activeCat} onChange={setActiveCat} />
      </div>

      {loading ? (
        <Card><Loading /></Card>
      ) : error ? (
        <Card><ErrorState message={error} onRetry={load} /></Card>
      ) : filtered.length === 0 ? (
        <Card><EmptyState title="Chưa có món nào" hint="Bấm 'Thêm món' để tạo món đầu tiên." /></Card>
      ) : (
        <>
          {/* Desktop: bảng */}
          <Card className="hidden md:block overflow-hidden">
            <div className="divide-y divide-line">
              <div className="flex items-center px-5 py-3 text-[12px] font-semibold uppercase tracking-wide text-faint">
                <div className="flex-1">Món</div>
                <div className="w-40">Danh mục</div>
                <div className="w-36">Giá</div>
                <div className="w-40">Trạng thái</div>
                <div className="w-24 text-right">Thao tác</div>
              </div>
              {filtered.map((item) => (
                <div key={item.id} className="flex items-center px-5 py-3.5 hover:bg-ink/[0.015] transition-colors">
                  <div className="flex-1 text-[15px] font-medium text-ink">
                    {item.name}
                    {item.description && <span className="text-muted font-normal"> · {item.description}</span>}
                  </div>
                  <div className="w-40 text-[15px] text-muted">{item.categoryName}</div>
                  <div className="w-36 text-[15px] font-semibold text-ink tnum">{formatVnd(item.price)}</div>
                  <div className="w-40"><StatusToggle item={item} /></div>
                  <div className="w-24 flex justify-end"><RowActions item={item} /></div>
                </div>
              ))}
            </div>
          </Card>

          {/* Mobile: thẻ xếp dọc */}
          <div className="md:hidden flex flex-col gap-3">
            {filtered.map((item) => (
              <Card key={item.id} className="p-4">
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="text-[15px] font-semibold text-ink">{item.name}</p>
                    <p className="text-sm text-muted">{item.categoryName}{item.description ? ` · ${item.description}` : ''}</p>
                  </div>
                  <RowActions item={item} />
                </div>
                <div className="flex items-center justify-between mt-3">
                  <StatusToggle item={item} />
                  <span className="text-[15px] font-semibold text-ink tnum">{formatVnd(item.price)}</span>
                </div>
              </Card>
            ))}
          </div>
        </>
      )}

      <Modal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        title={editing ? 'Sửa món' : 'Thêm món mới'}
        footer={
          <>
            <Button variant="ghost" onClick={() => setModalOpen(false)}>Hủy</Button>
            <Button onClick={save} disabled={saving}>{saving ? 'Đang lưu...' : 'Lưu'}</Button>
          </>
        }
      >
        <div className="flex flex-col gap-4">
          <Field label="Tên món">
            <input className={inputClass} value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} placeholder="Vd: Cà phê sữa" />
          </Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Danh mục">
              <select className={inputClass} value={form.categoryId} onChange={(e) => setForm({ ...form, categoryId: e.target.value })}>
                {cats.map((c) => (
                  <option key={c.id} value={c.id}>{c.name}</option>
                ))}
              </select>
            </Field>
            <Field label="Giá (đ)">
              <input type="number" min="0" className={inputClass} value={form.price} onChange={(e) => setForm({ ...form, price: e.target.value })} placeholder="30000" />
            </Field>
          </div>
          <Field label="Mô tả">
            <input className={inputClass} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} placeholder="Vd: Béo ngậy" />
          </Field>
          <label className="flex items-center gap-2.5 cursor-pointer">
            <input type="checkbox" checked={form.available} onChange={(e) => setForm({ ...form, available: e.target.checked })} className="w-4 h-4 accent-[var(--color-accent)]" />
            <span className="text-[15px] text-ink">Đang bán</span>
          </label>
        </div>
      </Modal>

      <Toast toast={toast} onClose={() => setToast(null)} />
    </div>
  )
}
