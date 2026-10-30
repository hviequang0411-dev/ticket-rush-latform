import React, { useCallback, useEffect, useState } from 'react';
import { Plus, Search, Edit2, X, Trash2, Users } from 'lucide-react';
import { AdminApi, type AdminEvent, type EventForm } from '../../api/adminApi';
import type { ApiErrorProblem, EventStatus } from '../../types/models';
import { StatusBadge } from '../../components/StatusBadge';
import { ErrorState } from '../../components/DataStates';
import { formatDateTimeVN, formatVND } from '../../lib/format';

const CITIES = ['Hà Nội', 'TP. Hồ Chí Minh', 'Đà Nẵng'];

/** Moves the organiser may make from each status; the backend enforces the same rules. */
const NEXT: Record<EventStatus, { to: EventStatus; label: string; className: string }[]> = {
  DRAFT: [
    { to: 'PUBLISHED', label: 'Mở bán', className: 'btn-tr-primary' },
    { to: 'CANCELLED', label: 'Huỷ', className: 'btn-outline-danger' },
  ],
  PUBLISHED: [
    { to: 'ENDED', label: 'Kết thúc', className: 'btn-tr-secondary' },
    { to: 'CANCELLED', label: 'Huỷ', className: 'btn-outline-danger' },
  ],
  CANCELLED: [],
  ENDED: [],
};

type TicketRow = { id?: number; name: string; price: number; totalQuantity: number };

const emptyForm = (): EventForm => ({
  name: '',
  slug: '',
  description: '',
  startTime: '',
  waitingRoom: false,
  venue: { name: '', address: '', city: CITIES[0] },
  ticketTypes: [{ name: 'Vé Tiêu Chuẩn', price: 500000, totalQuantity: 200 }],
});

/** datetime-local works in local time without a zone; the API takes an instant. */
const toLocalInput = (iso: string) => {
  const date = new Date(iso);
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
};

const slugify = (text: string) =>
  text
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/(^-|-$)/g, '');

export const AdminEventsPage: React.FC = () => {
  const [events, setEvents] = useState<AdminEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<ApiErrorProblem | null>(null);
  const [searchTerm, setSearchTerm] = useState('');

  const [editing, setEditing] = useState<AdminEvent | null>(null);
  const [showModal, setShowModal] = useState(false);
  const [form, setForm] = useState<EventForm>(emptyForm);
  const [publish, setPublish] = useState(true);
  const [formErrors, setFormErrors] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState<ApiErrorProblem | null>(null);

  const load = useCallback(async (q?: string) => {
    try {
      setLoading(true);
      setError(null);
      setEvents(await AdminApi.events(q));
    } catch (e: unknown) {
      setError(e as ApiErrorProblem);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const timer = setTimeout(() => load(searchTerm), 300);
    return () => clearTimeout(timer);
  }, [searchTerm, load]);

  const openCreate = () => {
    setEditing(null);
    setForm(emptyForm());
    setPublish(true);
    setFormErrors({});
    setSaveError(null);
    setShowModal(true);
  };

  const openEdit = (event: AdminEvent) => {
    setEditing(event);
    setForm({
      name: event.name,
      slug: event.slug,
      description: event.description,
      startTime: toLocalInput(event.startTime),
      endTime: event.endTime ? toLocalInput(event.endTime) : undefined,
      waitingRoom: !!event.waitingRoom,
      venue: { name: event.venue.name, address: event.venue.address, city: event.venue.city, capacity: event.venue.capacity },
      ticketTypes: event.ticketTypes.map(type => ({ id: type.id, name: type.name, price: type.price, totalQuantity: type.totalQuantity })),
    });
    setFormErrors({});
    setSaveError(null);
    setShowModal(true);
  };

  const validate = () => {
    const errors: Record<string, string> = {};
    if (!form.name.trim()) errors.name = 'Vui lòng nhập tên sự kiện.';
    if (!/^[a-z0-9]+(-[a-z0-9]+)*$/.test(form.slug)) errors.slug = 'Slug chỉ gồm chữ thường không dấu, số và dấu gạch ngang.';
    if (!form.startTime) errors.startTime = 'Vui lòng chọn thời gian bắt đầu.';
    if (!form.venue.name.trim()) errors.venueName = 'Vui lòng nhập tên địa điểm.';
    form.ticketTypes.forEach((type, i) => {
      if (!type.name.trim()) errors[`type${i}`] = 'Tên loại vé không được trống.';
      else if (type.price < 0) errors[`type${i}`] = 'Giá vé không được âm.';
      else if (type.totalQuantity < 1) errors[`type${i}`] = 'Số lượng tối thiểu là 1.';
    });
    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;
    const payload: EventForm = {
      ...form,
      name: form.name.trim(),
      startTime: new Date(form.startTime).toISOString(),
      endTime: form.endTime ? new Date(form.endTime).toISOString() : undefined,
    };
    try {
      setSaving(true);
      setSaveError(null);
      const saved = editing ? await AdminApi.updateEvent(editing.id, payload) : await AdminApi.createEvent(payload, publish);
      setEvents(prev => (editing ? prev.map(ev => (ev.id === saved.id ? saved : ev)) : [saved, ...prev]));
      setShowModal(false);
    } catch (err: unknown) {
      setSaveError(err as ApiErrorProblem);
    } finally {
      setSaving(false);
    }
  };

  const changeStatus = async (event: AdminEvent, to: EventStatus) => {
    const verb = to === 'PUBLISHED' ? 'mở bán' : to === 'CANCELLED' ? 'HUỶ (không hoàn tác được)' : 'kết thúc';
    if (!window.confirm(`Xác nhận ${verb} sự kiện "${event.name}"?`)) return;
    try {
      const saved = await AdminApi.changeEventStatus(event.id, to);
      setEvents(prev => prev.map(ev => (ev.id === saved.id ? saved : ev)));
    } catch (err: unknown) {
      window.alert((err as ApiErrorProblem).detail);
    }
  };

  const setType = (index: number, patch: Partial<TicketRow>) =>
    setForm(f => ({ ...f, ticketTypes: f.ticketTypes.map((type, i) => (i === index ? { ...type, ...patch } : type)) }));

  return (
    <div>
      <div className="d-flex flex-column flex-sm-row justify-content-between align-items-sm-center gap-3 mb-4">
        <div>
          <h1 className="fs-3 fw-bold mb-1">Quản lý sự kiện, địa điểm &amp; loại vé</h1>
          <p className="text-muted small mb-0">Tạo sự kiện, mở bán, bật phòng chờ cho đợt bán nóng; kho vé tự đồng bộ sang inventory</p>
        </div>
        <button className="btn btn-tr-primary d-flex align-items-center gap-2" onClick={openCreate}>
          <Plus size={18} /> Tạo sự kiện mới
        </button>
      </div>

      <div className="tr-card p-3 mb-4">
        <div className="input-group" style={{ maxWidth: 400 }}>
          <span className="input-group-text bg-surface-subtle border-end-0">
            <Search size={16} className="text-muted" />
          </span>
          <input
            type="search"
            className="form-control border-start-0"
            placeholder="Tìm theo tên, slug, thành phố..."
            aria-label="Tìm sự kiện"
            value={searchTerm}
            onChange={e => setSearchTerm(e.target.value)}
          />
        </div>
      </div>

      {error && <ErrorState title={error.title} message={error.detail} status={error.status} onRetry={() => load(searchTerm)} />}

      <div className="tr-card overflow-hidden">
        <div className="table-responsive">
          <table className="table table-hover align-middle mb-0">
            <thead className="table-light">
              <tr className="small text-muted">
                <th>TÊN SỰ KIỆN</th>
                <th>ĐỊA ĐIỂM</th>
                <th>THỜI GIAN</th>
                <th>TRẠNG THÁI</th>
                <th>VÉ ĐÃ BÁN</th>
                <th className="text-end">THAO TÁC</th>
              </tr>
            </thead>
            <tbody>
              {loading && events.length === 0 && (
                <tr>
                  <td colSpan={6} className="text-center text-muted py-4">Đang tải...</td>
                </tr>
              )}
              {!loading && events.length === 0 && (
                <tr>
                  <td colSpan={6} className="text-center text-muted py-4">Chưa có sự kiện nào.</td>
                </tr>
              )}
              {events.map(ev => {
                const levels = Object.values(ev.stock);
                const total = levels.reduce((sum, l) => sum + l.totalQuantity, 0);
                const sold = levels.reduce((sum, l) => sum + l.soldQuantity, 0);
                return (
                  <tr key={ev.id}>
                    <td>
                      <div className="fw-bold text-main d-flex align-items-center gap-2">
                        {ev.name}
                        {ev.waitingRoom && (
                          <span className="badge bg-warning-subtle text-warning d-inline-flex align-items-center gap-1" title="Bán qua phòng chờ">
                            <Users size={12} /> Phòng chờ
                          </span>
                        )}
                      </div>
                      <div className="font-monospace small text-muted" style={{ fontSize: '0.75rem' }}>slug: {ev.slug}</div>
                    </td>
                    <td className="small">
                      <div className="text-main">{ev.venue.name}</div>
                      <div className="text-muted">{ev.venue.city}</div>
                    </td>
                    <td className="small">{formatDateTimeVN(ev.startTime)}</td>
                    <td><StatusBadge type="event" status={ev.status} /></td>
                    <td className="small tabular-nums">
                      {total > 0 ? `${sold} / ${total}` : <span className="text-muted">Đang đồng bộ kho...</span>}
                      <div className="text-muted">{ev.ticketTypes.length} loại vé</div>
                    </td>
                    <td className="text-end">
                      <div className="d-inline-flex gap-1 flex-wrap justify-content-end">
                        {NEXT[ev.status].map(move => (
                          <button key={move.to} className={`btn btn-sm ${move.className}`} onClick={() => changeStatus(ev, move.to)}>
                            {move.label}
                          </button>
                        ))}
                        {(ev.status === 'DRAFT' || ev.status === 'PUBLISHED') && (
                          <button className="btn btn-sm btn-tr-ghost p-1" title="Chỉnh sửa" aria-label={`Sửa ${ev.name}`} onClick={() => openEdit(ev)}>
                            <Edit2 size={16} />
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {showModal && (
        <div
          className="position-fixed top-0 start-0 w-100 h-100 bg-dark bg-opacity-75 z-3 d-flex align-items-center justify-content-center p-3"
          style={{ backdropFilter: 'blur(4px)' }}
          onClick={() => setShowModal(false)}
        >
          <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="eventFormTitle"
            className="tr-card p-4 p-md-5 bg-surface shadow-lg"
            style={{ maxWidth: 720, width: '100%', maxHeight: '90vh', overflowY: 'auto' }}
            onClick={e => e.stopPropagation()}
          >
            <div className="d-flex justify-content-between align-items-center mb-4 pb-2 border-bottom border-subtle">
              <h3 id="eventFormTitle" className="fs-5 fw-bold mb-0">{editing ? `Sửa: ${editing.name}` : 'Tạo sự kiện & loại vé'}</h3>
              <button className="btn btn-tr-ghost p-1" aria-label="Đóng" onClick={() => setShowModal(false)}>
                <X size={20} />
              </button>
            </div>

            {saveError && <div className="alert alert-danger small py-2">{saveError.detail}</div>}

            <form onSubmit={handleSave} noValidate>
              <div className="mb-3">
                <label className="form-label small fw-semibold" htmlFor="evName">Tên sự kiện</label>
                <input
                  id="evName"
                  className={`form-control ${formErrors.name ? 'is-invalid' : ''}`}
                  value={form.name}
                  onChange={e => {
                    const name = e.target.value;
                    setForm(f => ({ ...f, name, slug: editing || (f.slug && f.slug !== slugify(f.name)) ? f.slug : slugify(name) }));
                  }}
                  placeholder="Ví dụ: Đêm Nhạc Mùa Đông Hà Nội"
                />
                {formErrors.name && <div className="invalid-feedback">{formErrors.name}</div>}
              </div>

              <div className="row g-3 mb-3">
                <div className="col-12 col-sm-6">
                  <label className="form-label small fw-semibold" htmlFor="evSlug">Slug URL</label>
                  <input
                    id="evSlug"
                    className={`form-control font-monospace ${formErrors.slug ? 'is-invalid' : ''}`}
                    value={form.slug}
                    onChange={e => setForm(f => ({ ...f, slug: e.target.value }))}
                    placeholder="dem-nhac-mua-dong"
                  />
                  {formErrors.slug && <div className="invalid-feedback">{formErrors.slug}</div>}
                </div>
                <div className="col-12 col-sm-6">
                  <label className="form-label small fw-semibold" htmlFor="evStart">Thời gian bắt đầu</label>
                  <input
                    id="evStart"
                    type="datetime-local"
                    className={`form-control ${formErrors.startTime ? 'is-invalid' : ''}`}
                    value={form.startTime}
                    onChange={e => setForm(f => ({ ...f, startTime: e.target.value }))}
                  />
                  {formErrors.startTime && <div className="invalid-feedback">{formErrors.startTime}</div>}
                </div>
              </div>

              <div className="mb-3">
                <label className="form-label small fw-semibold" htmlFor="evDesc">Mô tả</label>
                <textarea
                  id="evDesc"
                  rows={3}
                  className="form-control"
                  value={form.description ?? ''}
                  onChange={e => setForm(f => ({ ...f, description: e.target.value }))}
                />
              </div>

              <div className="row g-3 mb-3">
                <div className="col-12 col-sm-4">
                  <label className="form-label small fw-semibold" htmlFor="evCity">Thành phố</label>
                  <input
                    id="evCity"
                    list="cityOptions"
                    className="form-control"
                    value={form.venue.city}
                    onChange={e => setForm(f => ({ ...f, venue: { ...f.venue, city: e.target.value } }))}
                  />
                  <datalist id="cityOptions">
                    {CITIES.map(city => (
                      <option key={city} value={city} />
                    ))}
                  </datalist>
                </div>
                <div className="col-12 col-sm-8">
                  <label className="form-label small fw-semibold" htmlFor="evVenue">Địa điểm (cùng tên + thành phố dùng chung)</label>
                  <input
                    id="evVenue"
                    className={`form-control ${formErrors.venueName ? 'is-invalid' : ''}`}
                    value={form.venue.name}
                    onChange={e => setForm(f => ({ ...f, venue: { ...f.venue, name: e.target.value } }))}
                    placeholder="Nhà Hát Lớn, SECC..."
                  />
                  {formErrors.venueName && <div className="invalid-feedback">{formErrors.venueName}</div>}
                </div>
                <div className="col-12">
                  <label className="form-label small fw-semibold" htmlFor="evAddress">Địa chỉ</label>
                  <input
                    id="evAddress"
                    className="form-control"
                    value={form.venue.address ?? ''}
                    onChange={e => setForm(f => ({ ...f, venue: { ...f.venue, address: e.target.value } }))}
                  />
                </div>
              </div>

              <div className="p-3 bg-surface-subtle rounded-3 border border-subtle mb-3">
                <div className="d-flex justify-content-between align-items-center mb-2">
                  <div className="fw-semibold small text-primary">Loại vé</div>
                  <button
                    type="button"
                    className="btn btn-sm btn-tr-secondary d-inline-flex align-items-center gap-1"
                    onClick={() => setForm(f => ({ ...f, ticketTypes: [...f.ticketTypes, { name: '', price: 0, totalQuantity: 100 }] }))}
                  >
                    <Plus size={14} /> Thêm loại vé
                  </button>
                </div>
                {form.ticketTypes.map((type, i) => (
                  <div key={type.id ?? `new-${i}`} className="row g-2 align-items-start mb-2">
                    <div className="col-12 col-sm-5">
                      <input
                        aria-label={`Tên loại vé ${i + 1}`}
                        className={`form-control form-control-sm ${formErrors[`type${i}`] ? 'is-invalid' : ''}`}
                        value={type.name}
                        placeholder="VIP, Thường..."
                        onChange={e => setType(i, { name: e.target.value })}
                      />
                      {formErrors[`type${i}`] && <div className="invalid-feedback">{formErrors[`type${i}`]}</div>}
                    </div>
                    <div className="col-6 col-sm-3">
                      <input
                        aria-label={`Giá loại vé ${i + 1} (VND)`}
                        type="number"
                        min={0}
                        step={1000}
                        className="form-control form-control-sm"
                        value={type.price}
                        onChange={e => setType(i, { price: Number(e.target.value) })}
                      />
                      <div className="form-text small">{formatVND(type.price)}</div>
                    </div>
                    <div className="col-4 col-sm-3">
                      <input
                        aria-label={`Số lượng loại vé ${i + 1}`}
                        type="number"
                        min={1}
                        className="form-control form-control-sm"
                        value={type.totalQuantity}
                        onChange={e => setType(i, { totalQuantity: Number(e.target.value) })}
                      />
                      <div className="form-text small">vé</div>
                    </div>
                    <div className="col-2 col-sm-1 text-end">
                      {!type.id && form.ticketTypes.length > 1 && (
                        <button
                          type="button"
                          className="btn btn-sm btn-tr-ghost p-1 text-danger"
                          aria-label={`Bỏ loại vé ${i + 1}`}
                          onClick={() => setForm(f => ({ ...f, ticketTypes: f.ticketTypes.filter((_, j) => j !== i) }))}
                        >
                          <Trash2 size={16} />
                        </button>
                      )}
                    </div>
                  </div>
                ))}
                <div className="form-text small">Giảm số lượng không bao giờ thu hồi vé đã giữ hoặc đã bán; loại vé đã có không xoá được.</div>
              </div>

              <div className="form-check form-switch mb-2">
                <input
                  id="evWaitingRoom"
                  type="checkbox"
                  className="form-check-input"
                  checked={form.waitingRoom}
                  onChange={e => setForm(f => ({ ...f, waitingRoom: e.target.checked }))}
                />
                <label htmlFor="evWaitingRoom" className="form-check-label small">
                  <strong>Bán qua phòng chờ</strong>: người mua xếp hàng, được cho vào theo lượt; không có vé vào cửa thì không đặt được.
                </label>
              </div>
              {!editing && (
                <div className="form-check form-switch mb-4">
                  <input id="evPublish" type="checkbox" className="form-check-input" checked={publish} onChange={e => setPublish(e.target.checked)} />
                  <label htmlFor="evPublish" className="form-check-label small">Mở bán ngay (tắt để lưu nháp)</label>
                </div>
              )}

              <div className="d-flex justify-content-end gap-2">
                <button type="button" className="btn btn-tr-secondary" onClick={() => setShowModal(false)}>Huỷ</button>
                <button type="submit" className="btn btn-tr-primary" disabled={saving}>
                  {saving ? 'Đang lưu...' : editing ? 'Lưu thay đổi' : publish ? 'Lưu & mở bán' : 'Lưu nháp'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
