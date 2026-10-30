import React, { useState } from 'react';
import { Plus, Search, Edit2, X } from 'lucide-react';
import { mockEvents } from '../../mocks/mockData';
import type { Event, EventStatus } from '../../types/models';
import { StatusBadge } from '../../components/StatusBadge';
import { formatDateTimeVN } from '../../lib/format';

export const AdminEventsPage: React.FC = () => {
  const [events, setEvents] = useState<Event[]>(mockEvents);
  const [searchTerm, setSearchTerm] = useState('');
  const [showModal, setShowModal] = useState(false);

  // Form states for creating/editing
  const [name, setName] = useState('');
  const [slug, setSlug] = useState('');
  const [category, setCategory] = useState<Event['category']>('Concert');
  const [city, setCity] = useState<'Hà Nội' | 'TP. Hồ Chí Minh' | 'Đà Nẵng'>('Hà Nội');
  const [venueName, setVenueName] = useState('');
  const [venueAddress] = useState('');
  const [startTime, setStartTime] = useState('2026-11-20T20:00');
  const [status] = useState<EventStatus>('PUBLISHED');
  const [ticketPrice, setTicketPrice] = useState(500000);
  const [ticketQuantity, setTicketQuantity] = useState(200);

  // Validation
  const [formErrors, setFormErrors] = useState<{ [key: string]: string }>({});

  const validate = () => {
    const errors: { [key: string]: string } = {};
    if (!name.trim()) errors.name = 'Vui lòng nhập tên sự kiện.';
    // Slug validation rule: a-z0-9-
    const slugRegex = /^[a-z0-9-]+$/;
    if (!slug.trim() || !slugRegex.test(slug)) {
      errors.slug = 'Slug chỉ được chứa chữ thường không dấu, số và dấu gạch ngang (a-z0-9-).';
    }
    if (!venueName.trim()) errors.venueName = 'Vui lòng nhập tên địa điểm.';
    if (ticketPrice < 10000) errors.price = 'Giá vé tối thiểu là 10.000 ₫.';
    if (ticketQuantity < 1) errors.quantity = 'Số lượng vé mở bán tối thiểu là 1.';
    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    const newEvent: Event = {
      id: Math.floor(Math.random() * 9000) + 1000,
      name,
      slug,
      description: 'Sự kiện âm nhạc mới được cấu hình từ Bàn quản trị BTC.',
      category,
      startTime: new Date(startTime).toISOString(),
      status,
      posterBg: {
        gradient: 'linear-gradient(135deg, #1e293b 0%, #0f172a 100%)',
        accent: '#ea580c',
        pattern: 'mesh'
      },
      venue: {
        id: Math.floor(Math.random() * 500) + 1,
        name: venueName,
        address: venueAddress || 'Trung tâm văn hoá',
        city,
        capacity: ticketQuantity * 2
      },
      ticketTypes: [
        {
          id: Math.floor(Math.random() * 90000) + 1000,
          name: 'Vé Tiêu Chuẩn',
          price: ticketPrice,
          currency: 'VND',
          totalQuantity: ticketQuantity,
          availableQuantity: ticketQuantity,
          event: { id: 1 }
        }
      ]
    };

    setEvents([newEvent, ...events]);
    setShowModal(false);
  };

  const filteredEvents = events.filter(e =>
    e.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    e.venue.city.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div>
      <div className="d-flex flex-column flex-sm-row justify-content-between align-items-sm-center gap-3 mb-4">
        <div>
          <h1 className="fs-3 fw-bold mb-1">Quản lý sự kiện, địa điểm &amp; loại vé</h1>
          <p className="text-muted small mb-0">Cấu hình thời gian mở bán, loại vé, địa điểm tổ chức và kiểm duyệt trạng thái</p>
        </div>
        <button className="btn btn-tr-primary d-flex align-items-center gap-2" onClick={() => setShowModal(true)}>
          <Plus size={18} /> Tạo sự kiện mới
        </button>
      </div>

      {/* Search Bar */}
      <div className="tr-card p-3 mb-4">
        <div className="input-group" style={{ maxWidth: 400 }}>
          <span className="input-group-text bg-surface-subtle border-end-0">
            <Search size={16} className="text-muted" />
          </span>
          <input
            type="text"
            className="form-control border-start-0"
            placeholder="Tìm theo tên sự kiện, thành phố..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
      </div>

      {/* Events Table */}
      <div className="tr-card overflow-hidden">
        <div className="table-responsive">
          <table className="table table-hover align-middle mb-0">
            <thead className="table-light">
              <tr className="small text-muted">
                <th>TÊN SỰ KIỆN</th>
                <th>THÀNH PHỐ</th>
                <th>ĐỊA ĐIỂM</th>
                <th>THỜI GIAN DIỄN RA</th>
                <th>TRẠNG THÁI</th>
                <th>SỐ LOẠI VÉ</th>
                <th className="text-end">THAO TÁC</th>
              </tr>
            </thead>
            <tbody>
              {filteredEvents.map(ev => (
                <tr key={ev.id}>
                  <td>
                    <div className="fw-bold text-main">{ev.name}</div>
                    <div className="font-monospace small text-muted" style={{ fontSize: '0.75rem' }}>
                      slug: {ev.slug}
                    </div>
                  </td>
                  <td><span className="badge bg-surface-subtle text-muted border border-subtle">{ev.venue.city}</span></td>
                  <td className="small text-muted">{ev.venue.name}</td>
                  <td className="small">{formatDateTimeVN(ev.startTime)}</td>
                  <td><StatusBadge type="event" status={ev.status} /></td>
                  <td className="fw-semibold small">{ev.ticketTypes.length} hạng vé</td>
                  <td className="text-end">
                    <button className="btn btn-sm btn-tr-ghost p-1" title="Chỉnh sửa">
                      <Edit2 size={16} />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Create / Edit Modal */}
      {showModal && (
        <div
          className="position-fixed top-0 start-0 w-100 h-100 bg-dark bg-opacity-75 z-3 d-flex align-items-center justify-content-center p-3"
          style={{ backdropFilter: 'blur(4px)' }}
          onClick={() => setShowModal(false)}
        >
          <div
            className="tr-card p-4 p-md-5 bg-surface shadow-lg"
            style={{ maxWidth: 640, width: '100%', maxHeight: '90vh', overflowY: 'auto' }}
            onClick={(e) => e.stopPropagation()}
          >
            <div className="d-flex justify-content-between align-items-center mb-4 pb-2 border-bottom border-subtle">
              <h3 className="fs-5 fw-bold mb-0">Tạo sự kiện &amp; Loại vé mở bán</h3>
              <button className="btn btn-tr-ghost p-1" onClick={() => setShowModal(false)}>
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleSave}>
              <div className="mb-3">
                <label className="form-label small fw-semibold" htmlFor="evName">Tên sự kiện</label>
                <input
                  id="evName"
                  type="text"
                  className={`form-control ${formErrors.name ? 'is-invalid' : ''}`}
                  value={name}
                  onChange={(e) => {
                    setName(e.target.value);
                    if (!slug) {
                      setSlug(e.target.value.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, ''));
                    }
                  }}
                  placeholder="Ví dụ: Đêm Nhạc Mùa Đông Hà Nội"
                />
                {formErrors.name && <div className="invalid-feedback">{formErrors.name}</div>}
              </div>

              <div className="row g-3 mb-3">
                <div className="col-12 col-sm-6">
                  <label className="form-label small fw-semibold" htmlFor="evSlug">Slug URL (chuẩn a-z0-9-)</label>
                  <input
                    id="evSlug"
                    type="text"
                    className={`form-control font-monospace ${formErrors.slug ? 'is-invalid' : ''}`}
                    value={slug}
                    onChange={(e) => setSlug(e.target.value)}
                    placeholder="dem-nhac-mua-dong"
                  />
                  {formErrors.slug && <div className="invalid-feedback">{formErrors.slug}</div>}
                </div>

                <div className="col-12 col-sm-6">
                  <label className="form-label small fw-semibold" htmlFor="evCategory">Thể loại</label>
                  <select
                    id="evCategory"
                    className="form-select"
                    value={category}
                    onChange={(e) => setCategory(e.target.value as Event['category'])}
                  >
                    <option value="Concert">Concert</option>
                    <option value="Festival">Festival</option>
                    <option value="Sân khấu">Sân khấu</option>
                    <option value="Thể thao">Thể thao</option>
                    <option value="Workshop">Workshop</option>
                  </select>
                </div>
              </div>

              <div className="row g-3 mb-3">
                <div className="col-12 col-sm-6">
                  <label className="form-label small fw-semibold" htmlFor="evCity">Thành phố</label>
                  <select
                    id="evCity"
                    className="form-select"
                    value={city}
                    onChange={(e) => setCity(e.target.value as typeof city)}
                  >
                    <option value="Hà Nội">Hà Nội</option>
                    <option value="TP. Hồ Chí Minh">TP. Hồ Chí Minh</option>
                    <option value="Đà Nẵng">Đà Nẵng</option>
                  </select>
                </div>

                <div className="col-12 col-sm-6">
                  <label className="form-label small fw-semibold" htmlFor="evVenue">Tên địa điểm tổ chức</label>
                  <input
                    id="evVenue"
                    type="text"
                    className={`form-control ${formErrors.venueName ? 'is-invalid' : ''}`}
                    value={venueName}
                    onChange={(e) => setVenueName(e.target.value)}
                    placeholder="Nhà Hát Lớn, SECC..."
                  />
                  {formErrors.venueName && <div className="invalid-feedback">{formErrors.venueName}</div>}
                </div>
              </div>

              <div className="mb-3">
                <label className="form-label small fw-semibold" htmlFor="evStartTime">Thời gian bắt đầu</label>
                <input
                  id="evStartTime"
                  type="datetime-local"
                  className="form-control"
                  value={startTime}
                  onChange={(e) => setStartTime(e.target.value)}
                />
              </div>

              {/* Initial Ticket Type Config */}
              <div className="p-3 bg-surface-subtle rounded-3 border border-subtle mb-4">
                <div className="fw-semibold small mb-2 text-primary">Cấu hình loại vé mở bán ban đầu</div>
                <div className="row g-3">
                  <div className="col-6">
                    <label className="form-label small fw-semibold" htmlFor="evPrice">Giá vé (Tiền tệ: VND)</label>
                    <input
                      id="evPrice"
                      type="number"
                      className={`form-control ${formErrors.price ? 'is-invalid' : ''}`}
                      value={ticketPrice}
                      onChange={(e) => setTicketPrice(Number(e.target.value))}
                    />
                    {formErrors.price && <div className="invalid-feedback">{formErrors.price}</div>}
                  </div>
                  <div className="col-6">
                    <label className="form-label small fw-semibold" htmlFor="evQty">Số lượng phát hành (≥ 1)</label>
                    <input
                      id="evQty"
                      type="number"
                      className={`form-control ${formErrors.quantity ? 'is-invalid' : ''}`}
                      value={ticketQuantity}
                      onChange={(e) => setTicketQuantity(Number(e.target.value))}
                    />
                    {formErrors.quantity && <div className="invalid-feedback">{formErrors.quantity}</div>}
                  </div>
                </div>
              </div>

              <div className="d-flex justify-content-end gap-2">
                <button type="button" className="btn btn-tr-secondary" onClick={() => setShowModal(false)}>
                  Huỷ
                </button>
                <button type="submit" className="btn btn-tr-primary">
                  Lưu &amp; Xuất bản sự kiện
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
