import React, { useState, useEffect, useMemo } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { Calendar, MapPin, Plus, Minus, ShieldCheck, Info, ArrowLeft, ArrowRight } from 'lucide-react';
import { ApiService } from '../../api/apiClient';
import type { Event } from '../../types/models';
import { EventPoster } from '../../components/EventPoster';
import { StatusBadge } from '../../components/StatusBadge';
import { formatDateTimeVN, formatVND } from '../../lib/format';
import { ErrorState } from '../../components/DataStates';

export const EventDetailPage: React.FC = () => {
  const { slug } = useParams<{ slug: string }>();
  const navigate = useNavigate();

  const [event, setEvent] = useState<Event | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Selected quantities for ticket types: { [ticketTypeId]: quantity }
  const [selectedTickets, setSelectedTickets] = useState<Record<number, number>>({});

  useEffect(() => {
    if (slug) {
      loadEvent(slug);
    }
  }, [slug]);

  const loadEvent = async (eventSlug: string) => {
    try {
      setLoading(true);
      setError(null);
      const data = await ApiService.getEventBySlug(eventSlug);
      setEvent(data);
    } catch (e: unknown) {
      const err = e as { message?: string };
      setError(err.message || 'Không tìm thấy sự kiện.');
    } finally {
      setLoading(false);
    }
  };

  const handleQuantityChange = (ticketId: number, delta: number, maxAvailable: number) => {
    setSelectedTickets(prev => {
      const current = prev[ticketId] || 0;
      const next = current + delta;
      // Rule: Max 10 per type, min 0, not exceeding availableQuantity
      const capped = Math.min(Math.min(10, maxAvailable), Math.max(0, next));
      if (capped === 0) {
        const copy = { ...prev };
        delete copy[ticketId];
        return copy;
      }
      return { ...prev, [ticketId]: capped };
    });
  };

  const totalQuantity = useMemo(() => {
    return Object.values(selectedTickets).reduce((sum, q) => sum + q, 0);
  }, [selectedTickets]);

  const totalAmount = useMemo(() => {
    if (!event) return 0;
    return Object.entries(selectedTickets).reduce((sum, [idStr, q]) => {
      const id = Number(idStr);
      const tt = event.ticketTypes.find(t => t.id === id);
      return sum + (tt ? tt.price * q : 0);
    }, 0);
  }, [event, selectedTickets]);

  const handleProceedToCheckout = () => {
    if (!event || totalQuantity === 0) return;
    const items = Object.entries(selectedTickets).map(([idStr, q]) => ({
      ticketTypeId: Number(idStr),
      quantity: q
    }));
    if (event.waitingRoom && !ApiService.queuePass(event.id)) {
      // Hot sale: queue first; checkout picks the selection up from session storage once admitted
      sessionStorage.setItem('ticketrush.checkout', JSON.stringify({ eventId: event.id, items }));
      navigate(`/queue?event=${event.id}&slug=${encodeURIComponent(event.slug)}`);
      return;
    }
    navigate('/checkout', {
      state: {
        eventId: event.id,
        items
      }
    });
  };

  if (loading) {
    return (
      <div className="tr-container py-5">
        <div className="tr-skeleton w-100 rounded-3 mb-4" style={{ height: 320 }} />
        <div className="row g-4">
          <div className="col-12 col-lg-8">
            <div className="tr-skeleton w-75 mb-3" style={{ height: 32 }} />
            <div className="tr-skeleton w-100 mb-2" style={{ height: 20 }} />
            <div className="tr-skeleton w-100 mb-2" style={{ height: 20 }} />
          </div>
          <div className="col-12 col-lg-4">
            <div className="tr-skeleton w-100 rounded-3" style={{ height: 250 }} />
          </div>
        </div>
      </div>
    );
  }

  if (error || !event) {
    return (
      <div className="tr-container py-5">
        <Link to="/events" className="btn btn-sm btn-tr-secondary mb-3 d-inline-flex align-items-center gap-2">
          <ArrowLeft size={16} /> Quay lại danh sách
        </Link>
        <ErrorState message={error || 'Sự kiện không tồn tại.'} onRetry={() => slug && loadEvent(slug)} />
      </div>
    );
  }

  const isSaleEnded = event.status === 'ENDED' || event.status === 'CANCELLED';
  const isDraft = event.status === 'DRAFT';

  return (
    <div>
      {/* 1. Large Top Event Poster */}
      <section className="bg-dark text-white">
        <div className="tr-container py-4">
          <Link to="/events" className="btn btn-sm btn-outline-light mb-3 d-inline-flex align-items-center gap-1 border-opacity-25">
            <ArrowLeft size={16} /> Danh sách sự kiện
          </Link>
          <div className="rounded-3 overflow-hidden shadow-lg border border-white border-opacity-10">
            <EventPoster
              title={event.name}
              category={event.category}
              dateStr={formatDateTimeVN(event.startTime)}
              venueName={event.venue.name}
              bgConfig={event.posterBg}
              height={320}
            />
          </div>
        </div>
      </section>

      {/* 2. Main Content & Sticky Ticket Purchase Sidebar */}
      <section className="py-4">
        <div className="tr-container">
          <div className="row g-4 position-relative">
            {/* Left Column: Details & Ticket Type Selection */}
            <div className="col-12 col-lg-8">
              {/* Event Metadata Banner */}
              <div className="tr-card p-4 mb-4">
                <div className="d-flex align-items-center gap-2 mb-2">
                  <StatusBadge type="event" status={event.status} />
                  <span className="badge bg-secondary-subtle text-secondary">{event.category}</span>
                </div>
                <h1 className="fs-2 fw-bold text-main mb-3">{event.name}</h1>
                <div className="row g-3 text-muted small">
                  <div className="col-12 col-sm-6 d-flex align-items-center gap-2">
                    <Calendar size={18} className="text-primary flex-shrink-0" />
                    <div>
                      <div className="fw-semibold text-main">Thời gian bắt đầu</div>
                      <span>{formatDateTimeVN(event.startTime)}</span>
                    </div>
                  </div>
                  <div className="col-12 col-sm-6 d-flex align-items-center gap-2">
                    <MapPin size={18} className="text-primary flex-shrink-0" />
                    <div>
                      <div className="fw-semibold text-main">{event.venue.name}</div>
                      <span>{event.venue.address}, {event.venue.city}</span>
                    </div>
                  </div>
                </div>
              </div>

              {/* Description */}
              <div className="tr-card p-4 mb-4">
                <h2 className="fs-5 fw-bold mb-3">Giới thiệu sự kiện</h2>
                <p className="text-muted mb-4 leading-relaxed" style={{ whiteSpace: 'pre-line' }}>
                  {event.description}
                </p>

                <h3 className="fs-6 fw-bold mb-2">Sơ đồ vị trí sân khấu (Minh hoạ SVG)</h3>
                <div className="p-3 bg-surface-subtle rounded-3 text-center border border-subtle overflow-auto">
                  <svg width="400" height="180" viewBox="0 0 400 180" className="mx-auto d-block">
                    {/* Stage */}
                    <rect x="100" y="10" width="200" height="30" rx="4" fill="var(--color-primary)" />
                    <text x="200" y="30" fill="#ffffff" textAnchor="middle" fontSize="12" fontWeight="600">
                      SÂN KHẤU CHÍNH (STAGE)
                    </text>
                    {/* VIP Area */}
                    <rect x="50" y="60" width="300" height="40" rx="6" fill="var(--color-primary-subtle)" stroke="var(--color-primary-border)" strokeWidth="1.5" />
                    <text x="200" y="85" fill="var(--color-primary-active)" textAnchor="middle" fontSize="12" fontWeight="500">
                      KHU VỰC VIP / ĐỨNG SÀN FANZONE
                    </text>
                    {/* Standard Seating */}
                    <rect x="30" y="115" width="340" height="50" rx="6" fill="var(--bg-surface)" stroke="var(--border-strong)" strokeWidth="1.5" />
                    <text x="200" y="145" fill="var(--text-muted)" textAnchor="middle" fontSize="12">
                      KHÁN ĐÀI TIÊU CHUẨN (TẦNG 1 & TẦNG 2)
                    </text>
                  </svg>
                  <div className="small text-muted mt-2">Sơ đồ phân bổ khu vực chỗ ngồi chính xác theo mã vé</div>
                </div>
              </div>

              {/* Ticket Types Picker */}
              <div className="tr-card p-4 mb-5">
                <div className="d-flex align-items-center justify-content-between mb-3">
                  <h2 className="fs-5 fw-bold mb-0">Chọn loại vé</h2>
                  <span className="small text-muted">Tối đa 10 vé mỗi loại / đơn</span>
                </div>

                {isDraft ? (
                  <div className="alert alert-info d-flex align-items-center gap-2 mb-0">
                    <Info size={18} />
                    <span>Sự kiện đang trong trạng thái sắp mở bán. Vui lòng quay lại vào ngày công bố chính thức!</span>
                  </div>
                ) : isSaleEnded ? (
                  <div className="alert alert-secondary d-flex align-items-center gap-2 mb-0">
                    <Info size={18} />
                    <span>Sự kiện đã kết thúc hoặc đóng cổng đăng ký vé.</span>
                  </div>
                ) : (
                  <div className="d-flex flex-column gap-3">
                    {event.ticketTypes.map(tt => {
                      const qty = selectedTickets[tt.id] || 0;
                      const isSoldOut = tt.availableQuantity === 0;
                      const isLowStock = !isSoldOut && tt.availableQuantity <= tt.totalQuantity * 0.3;
                      const stockPercent = Math.round((tt.availableQuantity / tt.totalQuantity) * 100);

                      return (
                        <div
                          key={tt.id}
                          className={`p-3 rounded-3 border transition-all ${
                            qty > 0
                              ? 'border-primary bg-primary-subtle'
                              : isSoldOut
                              ? 'border-subtle bg-surface-subtle opacity-75'
                              : 'border-subtle bg-surface'
                          }`}
                        >
                          <div className="row align-items-center g-3">
                            <div className="col-12 col-md-6">
                              <div className="d-flex align-items-center gap-2 mb-1">
                                <span className="fw-bold text-main">{tt.name}</span>
                                {isSoldOut ? (
                                  <span className="tr-badge badge-danger">Hết vé</span>
                                ) : isLowStock ? (
                                  <span className="tr-badge badge-warning">Sắp hết ({tt.availableQuantity} vé)</span>
                                ) : (
                                  <span className="tr-badge badge-success">Còn vé</span>
                                )}
                              </div>
                              <div className="text-muted small mb-2">{tt.description || 'Quyền vào cửa tiêu chuẩn cho 01 người.'}</div>
                              {/* Inventory Progress Bar for low stock */}
                              {isLowStock && (
                                <div className="progress" style={{ height: 4, maxWidth: 200 }} role="progressbar" aria-valuenow={stockPercent} aria-valuemin={0} aria-valuemax={100} aria-label={`Tồn kho còn ${stockPercent}%`}>
                                  <div
                                    className="progress-bar bg-warning"
                                    style={{ width: `${stockPercent}%` }}
                                  />
                                </div>
                              )}
                            </div>

                            <div className="col-6 col-md-3">
                              <div className="fs-5 fw-bold text-primary tabular-nums">
                                {formatVND(tt.price)}
                              </div>
                              <div className="small text-muted">/ vé</div>
                            </div>

                            {/* Stepper counter */}
                            <div className="col-6 col-md-3 d-flex justify-content-end">
                              {isSoldOut ? (
                                <button className="btn btn-sm btn-secondary disabled" disabled>
                                  Hết vé
                                </button>
                              ) : (
                                <div className="d-inline-flex align-items-center border border-subtle rounded-2 bg-surface">
                                  <button
                                    onClick={() => handleQuantityChange(tt.id, -1, tt.availableQuantity)}
                                    disabled={qty === 0}
                                    className="btn btn-sm btn-tr-ghost px-2"
                                    aria-label={`Giảm số lượng vé ${tt.name}`}
                                  >
                                    <Minus size={16} />
                                  </button>
                                  <span className="px-3 fw-bold tabular-nums" style={{ minWidth: 32, textAlign: 'center' }}>
                                    {qty}
                                  </span>
                                  <button
                                    onClick={() => handleQuantityChange(tt.id, 1, tt.availableQuantity)}
                                    disabled={qty >= 10 || qty >= tt.availableQuantity}
                                    className="btn btn-sm btn-tr-ghost px-2"
                                    aria-label={`Tăng số lượng vé ${tt.name}`}
                                  >
                                    <Plus size={16} />
                                  </button>
                                </div>
                              )}
                            </div>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>
            </div>

            {/* Right Column: Sticky Summary Box (Desktop) */}
            <div className="col-12 col-lg-4 d-none d-lg-block">
              <div className="sticky-top" style={{ top: '88px' }}>
                <div className="tr-card p-4 shadow-sm">
                  <h3 className="fs-5 fw-bold mb-3 pb-2 border-bottom border-subtle">Tóm tắt đơn đặt vé</h3>

                  {totalQuantity === 0 ? (
                    <div className="text-center py-4 text-muted small">
                      Bạn chưa chọn vé nào. Hãy chọn loại vé và số lượng ở cột bên trái để tiếp tục.
                    </div>
                  ) : (
                    <div className="d-flex flex-column gap-2 mb-3">
                      {Object.entries(selectedTickets).map(([idStr, qty]) => {
                        const tt = event.ticketTypes.find(t => t.id === Number(idStr));
                        if (!tt) return null;
                        return (
                          <div key={tt.id} className="d-flex justify-content-between small">
                            <span>{tt.name} × {qty}</span>
                            <span className="fw-semibold tabular-nums">{formatVND(tt.price * qty)}</span>
                          </div>
                        );
                      })}
                    </div>
                  )}

                  <div className="pt-3 border-top border-subtle mb-4">
                    <div className="d-flex justify-content-between align-items-center mb-1">
                      <span className="text-muted small">Tổng tiền ({totalQuantity} vé):</span>
                      <span className="fs-4 fw-bold text-primary tabular-nums">{formatVND(totalAmount)}</span>
                    </div>
                    <div className="small text-muted" style={{ fontSize: '0.75rem' }}>
                      Đã bao gồm thuế GTGT &amp; bảo hiểm đặt chỗ
                    </div>
                  </div>

                  <button
                    onClick={handleProceedToCheckout}
                    disabled={totalQuantity === 0 || isSaleEnded || isDraft}
                    className="btn btn-tr-primary w-100 py-3"
                  >
                    Tiến hành đặt vé <ArrowRight size={18} />
                  </button>

                  <div className="d-flex align-items-center justify-content-center gap-2 mt-3 small text-muted">
                    <ShieldCheck size={16} className="text-success" />
                    <span>Giữ chỗ 10 phút sau khi xác nhận thông tin</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 3. Mobile Sticky Bottom Bar */}
      <div className="d-lg-none tr-mobile-sticky-bar">
        <div className="d-flex align-items-center justify-content-between gap-3">
          <div>
            <div className="small text-muted" style={{ fontSize: '0.75rem' }}>Tổng thanh toán ({totalQuantity} vé)</div>
            <div className="fw-bold text-primary tabular-nums fs-5">{formatVND(totalAmount)}</div>
          </div>
          <button
            onClick={handleProceedToCheckout}
            disabled={totalQuantity === 0 || isSaleEnded || isDraft}
            className="btn btn-tr-primary px-4 py-2"
          >
            Mua vé <ArrowRight size={16} />
          </button>
        </div>
      </div>
    </div>
  );
};
