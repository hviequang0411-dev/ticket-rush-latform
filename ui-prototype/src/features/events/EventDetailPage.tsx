import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { Calendar, MapPin, Shield, Plus, Minus, ShoppingBag, Users, ArrowLeft, Clock, Sparkles, CheckCircle2 } from 'lucide-react';
import { ApiService } from '../../api/apiClient';
import type { Event } from '../../types/models';
import { EventPoster } from '../../components/EventPoster';
import { StatusBadge } from '../../components/StatusBadge';
import { ErrorState } from '../../components/DataStates';
import { formatVND, formatDateTimeVN } from '../../lib/format';

export const EventDetailPage: React.FC = () => {
  const { slug } = useParams<{ slug: string }>();
  const navigate = useNavigate();

  const [event, setEvent] = useState<Event | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Ticket quantity selection map: ticketTypeId -> quantity
  const [quantities, setQuantities] = useState<Record<number, number>>({});

  const loadEventDetail = async () => {
    if (!slug) return;
    try {
      setLoading(true);
      setError(null);
      const data = await ApiService.getEventBySlug(slug);
      setEvent(data);
    } catch (err: unknown) {
      const e = err as { detail?: string; message?: string };
      setError(e.detail || e.message || 'Không tìm thấy sự kiện.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadEventDetail();
  }, [slug]);

  const updateQuantity = (ticketTypeId: number, delta: number, available: number) => {
    setQuantities((prev) => {
      const current = prev[ticketTypeId] || 0;
      const maxAllowed = Math.min(10, available);
      const next = Math.max(0, Math.min(maxAllowed, current + delta));
      return { ...prev, [ticketTypeId]: next };
    });
  };

  const selectedItems = event
    ? event.ticketTypes
        .filter((tt) => (quantities[tt.id] || 0) > 0)
        .map((tt) => ({
          ticketType: tt,
          quantity: quantities[tt.id],
          subtotal: tt.price * quantities[tt.id],
        }))
    : [];

  const totalTickets = selectedItems.reduce((sum, item) => sum + item.quantity, 0);
  const totalAmount = selectedItems.reduce((sum, item) => sum + item.subtotal, 0);

  const handleProceedCheckout = () => {
    if (!event || selectedItems.length === 0) return;
    const selection = {
      event,
      items: selectedItems.map((i) => ({
        eventId: event.id,
        ticketTypeId: i.ticketType.id,
        ticketTypeName: i.ticketType.name,
        quantity: i.quantity,
        unitPrice: i.ticketType.price,
      })),
      totalAmount,
    };
    sessionStorage.setItem('ticketrush.checkout', JSON.stringify(selection));
    if (event.waitingRoom && !ApiService.queuePass(event.id)) {
      navigate(`/queue?event=${encodeURIComponent(event.slug)}`);
      return;
    }
    navigate('/checkout', { state: selection });
  };

  if (loading) {
    return (
      <div className="tr-container py-4">
        <div className="tr-skeleton w-100 mb-4 rounded-4" style={{ height: 320 }} />
        <div className="row g-4">
          <div className="col-12 col-lg-8">
            <div className="tr-skeleton w-50 mb-3" style={{ height: 32 }} />
            <div className="tr-skeleton w-100 mb-3" style={{ height: 120 }} />
            <div className="tr-skeleton w-100" style={{ height: 240 }} />
          </div>
          <div className="col-12 col-lg-4">
            <div className="tr-skeleton w-100 rounded-3" style={{ height: 320 }} />
          </div>
        </div>
      </div>
    );
  }

  if (error || !event) {
    return (
      <div className="tr-container py-5">
        <ErrorState
          title="Không thể tải thông tin sự kiện"
          message={error || 'Sự kiện không tồn tại hoặc đã bị gỡ.'}
          onRetry={loadEventDetail}
        />
        <Link to="/events" className="btn btn-tr-secondary mt-3 text-decoration-none">
          <ArrowLeft size={16} /> Quay lại danh sách sự kiện
        </Link>
      </div>
    );
  }

  return (
    <div className="pb-5">
      {/* Immersive Concert Header */}
      <div
        className="tr-hero-stage py-4 py-md-5"
        style={{
          background: event.posterBg.gradient,
        }}
      >
        <div
          className="position-absolute top-0 start-0 w-100 h-100 pointer-events-none"
          style={{
            background: 'linear-gradient(95deg, rgba(9, 13, 22, 0.88) 0%, rgba(9, 13, 22, 0.62) 55%, rgba(9, 13, 22, 0.35) 100%)',
          }}
        />
        <div
          className="tr-hero-glow"
          style={{ top: '-160px', right: '10%', background: event.posterBg.accent }}
        />
        <div className="tr-container position-relative z-1">
          <Link
            to="/events"
            className="text-decoration-none small d-inline-flex align-items-center gap-1 mb-3 px-3 py-1 rounded-pill"
            style={{
              backgroundColor: 'rgba(255,255,255,0.1)',
              color: 'rgba(255,255,255,0.85)',
              border: '1px solid rgba(255,255,255,0.16)',
            }}
          >
            <ArrowLeft size={15} /> Danh sách sự kiện
          </Link>

          <div className="row g-4 align-items-center">
            <div className="col-12 col-lg-7">
              <div className="d-flex flex-wrap align-items-center gap-2 mb-3">
                <StatusBadge type="event" status={event.status} />
                <span
                  className="badge rounded-pill px-3 py-1"
                  style={{
                    backgroundColor: 'rgba(255,255,255,0.15)',
                    color: '#ffffff',
                    border: '1px solid rgba(255,255,255,0.22)',
                  }}
                >
                  {event.category}
                </span>
                {event.waitingRoom && (
                  <span className="badge rounded-pill bg-warning text-dark fw-semibold px-3 py-1">
                    Phòng chờ công bằng đang bật
                  </span>
                )}
              </div>

              <h1 className="display-6 fw-bold mb-3 text-white" style={{ fontFamily: 'var(--font-display)' }}>
                {event.name}
              </h1>

              <div className="row g-3 small" style={{ color: 'rgba(255,255,255,0.9)' }}>
                <div className="col-12 col-sm-6 d-flex align-items-start gap-2">
                  <Calendar size={18} className="text-warning flex-shrink-0 mt-1" />
                  <div>
                    <div className="fw-semibold text-white">Thời gian biểu diễn</div>
                    <div className="tabular-nums" style={{ color: 'rgba(255,255,255,0.75)' }}>
                      {formatDateTimeVN(event.startTime)}
                    </div>
                  </div>
                </div>
                <div className="col-12 col-sm-6 d-flex align-items-start gap-2">
                  <MapPin size={18} className="text-warning flex-shrink-0 mt-1" />
                  <div>
                    <div className="fw-semibold text-white">{event.venue.name}</div>
                    <div style={{ color: 'rgba(255,255,255,0.75)' }}>
                      {event.venue.address}, {event.venue.city}
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div className="col-12 col-lg-5">
              <div
                className="rounded-4 overflow-hidden"
                style={{
                  border: '1px solid rgba(255,255,255,0.2)',
                  boxShadow: '0 20px 44px -12px rgba(0,0,0,0.6)',
                }}
              >
                <EventPoster
                  title={event.name}
                  category={event.category}
                  dateText={formatDateTimeVN(event.startTime)}
                  city={event.venue.city}
                  posterBg={event.posterBg}
                  height={250}
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Main Content & Sticky Ticket Selection */}
      <div className="tr-container py-4">
        <div className="row g-4">
          <div className="col-12 col-lg-8">
            <div className="tr-card p-4 mb-4">
              <h2 className="fs-5 fw-bold mb-3">Giới thiệu sự kiện</h2>
              <p className="text-muted mb-3">{event.description}</p>
              <div className="p-3 rounded-3 bg-surface-subtle border border-subtle d-flex flex-wrap align-items-center justify-content-between gap-2 small">
                <div>
                  <span className="text-muted">Địa điểm: </span>
                  <strong className="text-main tabular-nums">{event.venue.name} ({event.venue.capacity.toLocaleString('vi-VN')} chỗ)</strong>
                </div>
              </div>
            </div>

            {/* Interactive-Style Stage & Seating Map SVG */}
            <div className="tr-card p-4 mb-4">
              <div className="d-flex justify-content-between align-items-center mb-3">
                <h2 className="fs-5 fw-bold mb-0">Sơ đồ phân khu khán đài</h2>
                <span className="small text-muted d-inline-flex align-items-center gap-1">
                  <Sparkles size={14} className="text-primary" /> Hình minh hoạ, không phải sơ đồ ghế
                </span>
              </div>
              <div className="p-3 bg-surface-subtle rounded-3 border border-subtle text-center">
                <svg viewBox="0 0 560 210" className="w-100" style={{ maxHeight: 210 }} role="img" aria-label="Sơ đồ sân khấu">
                  <defs>
                    <linearGradient id="stageGlow" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stopColor="#ea580c" stopOpacity="0.35" />
                      <stop offset="100%" stopColor="#ea580c" stopOpacity="0.0" />
                    </linearGradient>
                  </defs>
                  <polygon points="150,42 410,42 490,195 70,195" fill="url(#stageGlow)" />
                  <rect x="140" y="10" width="280" height="34" rx="8" fill="#0f172a" />
                  <text x="280" y="32" textAnchor="middle" fill="#ffffff" fontSize="12" fontWeight="bold" letterSpacing="1.5">
                    SÂN KHẤU CHÍNH (MAIN STAGE)
                  </text>
                  <rect x="115" y="58" width="330" height="40" rx="8" fill="#ea580c" fillOpacity="0.18" stroke="#ea580c" strokeWidth="1.5" />
                  <text x="280" y="83" textAnchor="middle" fill="#ea580c" fontSize="12" fontWeight="bold">
                    KHU VỰC HẠNG VVIP / VIP (GẦN SÂN KHẤU)
                  </text>
                  <rect x="75" y="110" width="410" height="44" rx="8" fill="#0d9488" fillOpacity="0.15" stroke="#0d9488" strokeWidth="1.5" />
                  <text x="280" y="137" textAnchor="middle" fill="#0d9488" fontSize="12" fontWeight="bold">
                    KHU VỰC TIÊU CHUẨN / PHỔ THÔNG (TẦNG 1 &amp; GA)
                  </text>
                  <rect x="45" y="164" width="470" height="32" rx="8" fill="#64748b" fillOpacity="0.14" stroke="#64748b" strokeWidth="1.2" />
                  <text x="280" y="185" textAnchor="middle" fill="#64748b" fontSize="11" fontWeight="bold">
                    KHÁN ĐÀI TẦNG 2 &amp; CÁNH (BALCONY)
                  </text>
                </svg>
              </div>
            </div>

            {/* Ticket Tiers Selection List */}
            <div className="tr-card p-4">
              <div className="d-flex flex-wrap justify-content-between align-items-center mb-3 gap-2">
                <div>
                  <h2 className="fs-5 fw-bold mb-0">Chọn hạng vé &amp; số lượng</h2>
                  <span className="small text-muted">Giới hạn tối đa 10 vé cho mỗi hạng vé trong một giao dịch</span>
                </div>
                <Link
                  to={`/queue?event=${event.slug}`}
                  className="btn btn-sm btn-tr-secondary py-1 px-3 text-decoration-none"
                >
                  <Users size={14} className="text-primary" /> Vào phòng chờ ảo
                </Link>
              </div>

              <div className="d-flex flex-column gap-3">
                {event.ticketTypes.map((tt) => {
                  const qty = quantities[tt.id] || 0;
                  const isSoldOut = tt.availableQuantity === 0;
                  const percentLeft = tt.totalQuantity > 0
                    ? Math.round((tt.availableQuantity / tt.totalQuantity) * 100)
                    : 0;
                  const isUrgent = !isSoldOut && percentLeft <= 30;

                  const stockBarColor = isSoldOut
                    ? 'bg-danger'
                    : isUrgent
                    ? 'bg-warning'
                    : 'bg-success';

                  return (
                    <div
                      key={tt.id}
                      className={`p-3 p-md-4 rounded-3 border transition-all ${
                        qty > 0 ? 'border-primary' : 'border-subtle bg-surface'
                      }`}
                      style={{
                        backgroundColor: qty > 0 ? 'var(--color-primary-subtle)' : 'var(--bg-surface)',
                      }}
                    >
                      <div className="row align-items-center g-3">
                        <div className="col-12 col-sm-7">
                          <div className="d-flex flex-wrap align-items-center gap-2 mb-1">
                            <h3 className="fs-6 fw-bold mb-0 text-main">{tt.name}</h3>
                            {isSoldOut ? (
                              <span className="tr-badge badge-danger">Hết vé</span>
                            ) : isUrgent ? (
                              <span className="tr-badge badge-warning">Sắp hết ({tt.availableQuantity} vé)</span>
                            ) : (
                              <span className="tr-badge badge-success">Còn vé</span>
                            )}
                          </div>
                          {tt.description && <p className="small text-muted mb-2">{tt.description}</p>}

                          {/* Color-coded Stock Progress Bar */}
                          <div className="mt-2" style={{ maxWidth: 280 }}>
                            <div className="d-flex justify-content-between small mb-1" style={{ fontSize: '0.73rem' }}>
                              <span className={isSoldOut ? 'text-danger fw-semibold' : isUrgent ? 'text-warning fw-semibold' : 'text-muted'}>
                                {isSoldOut
                                  ? 'Đã bán hết 100%'
                                  : `Còn ${tt.availableQuantity.toLocaleString('vi-VN')} / ${tt.totalQuantity.toLocaleString('vi-VN')} vé`}
                              </span>
                              <span className="tabular-nums text-muted">{percentLeft}%</span>
                            </div>
                            <div className="progress" style={{ height: 6 }}>
                              <div
                                className={`progress-bar ${stockBarColor}`}
                                style={{ width: `${isSoldOut ? 100 : Math.max(6, percentLeft)}%` }}
                              />
                            </div>
                          </div>
                        </div>

                        <div className="col-12 col-sm-5 d-flex flex-sm-column align-items-center align-items-sm-end justify-content-between gap-2">
                          <div className="text-sm-end">
                            <div className="fs-5 fw-bold text-primary tabular-nums">
                              {formatVND(tt.price)}
                            </div>
                            <span className="text-muted" style={{ fontSize: '0.72rem' }}>/ vé</span>
                          </div>

                          {event.status === 'PUBLISHED' && !isSoldOut ? (
                            <div className="tr-stepper-group">
                              <button
                                type="button"
                                className="tr-stepper-btn"
                                onClick={() => updateQuantity(tt.id, -1, tt.availableQuantity)}
                                disabled={qty === 0}
                                aria-label={`Giảm số lượng ${tt.name}`}
                              >
                                <Minus size={16} />
                              </button>
                              <span
                                className="fw-bold px-3 tabular-nums text-center text-main"
                                style={{ minWidth: 38 }}
                                aria-live="polite"
                              >
                                {qty}
                              </span>
                              <button
                                type="button"
                                className="tr-stepper-btn"
                                onClick={() => updateQuantity(tt.id, 1, tt.availableQuantity)}
                                disabled={qty >= Math.min(10, tt.availableQuantity)}
                                aria-label={`Tăng số lượng ${tt.name}`}
                              >
                                <Plus size={16} />
                              </button>
                            </div>
                          ) : (
                            <button className="btn btn-sm btn-tr-secondary" disabled>
                              {isSoldOut ? 'Đã bán hết' : 'Chưa mở bán'}
                            </button>
                          )}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>

          <div className="col-12 col-lg-4">
            <div className="tr-card p-4 sticky-top" style={{ top: '84px' }}>
              <div className="d-flex align-items-center justify-content-between mb-3 pb-2 border-bottom border-subtle">
                <div className="d-flex align-items-center gap-2">
                  <ShoppingBag size={20} className="text-primary" />
                  <h2 className="fs-5 fw-bold mb-0">Tóm tắt vé đã chọn</h2>
                </div>
                {totalTickets > 0 && (
                  <span className="badge rounded-pill bg-primary text-white tabular-nums">
                    {totalTickets} vé
                  </span>
                )}
              </div>

              {selectedItems.length === 0 ? (
                <div className="text-center py-4 text-muted small">
                  <div className="d-inline-flex p-3 rounded-circle bg-surface-subtle text-muted mb-2">
                    <ShoppingBag size={24} />
                  </div>
                  <p className="mb-1 fw-medium text-main">Chưa có vé nào được chọn</p>
                  <span style={{ fontSize: '0.78rem' }}>
                    Chọn số lượng ở từng hạng vé bên trái để khoá ghế ngay.
                  </span>
                </div>
              ) : (
                <>
                  <div className="d-flex flex-column gap-2 mb-3">
                    {selectedItems.map((item) => (
                      <div key={item.ticketType.id} className="d-flex justify-content-between align-items-start small py-1">
                        <div>
                          <div className="fw-semibold text-main">{item.ticketType.name}</div>
                          <div className="text-muted tabular-nums">
                            {formatVND(item.ticketType.price)} × {item.quantity}
                          </div>
                        </div>
                        <span className="fw-bold text-main tabular-nums">
                          {formatVND(item.subtotal)}
                        </span>
                      </div>
                    ))}
                  </div>

                  <hr className="border-subtle" />

                  <div className="d-flex justify-content-between align-items-center mb-3">
                    <span className="fw-semibold text-muted">Tổng tạm tính</span>
                    <span className="fs-4 fw-bold text-primary tabular-nums">
                      {formatVND(totalAmount)}
                    </span>
                  </div>
                </>
              )}

              <button
                type="button"
                onClick={handleProceedCheckout}
                disabled={selectedItems.length === 0}
                className="btn btn-tr-primary w-100 py-2 mb-3"
              >
                {selectedItems.length === 0
                  ? 'Chọn ít nhất 1 vé để tiếp tục'
                  : `Giữ chỗ & Thanh toán (${totalTickets} vé)`}
              </button>

              <div className="d-flex flex-column gap-2 small text-muted bg-surface-subtle p-3 rounded-3 border border-subtle">
                <div className="d-flex align-items-center gap-2">
                  <Clock size={15} className="text-primary flex-shrink-0" />
                  <span>Ghế được giữ cho bạn <strong>10 phút</strong> để thanh toán</span>
                </div>
                <div className="d-flex align-items-center gap-2">
                  <Shield size={15} className="text-success flex-shrink-0" />
                  <span>Bấm lại khi mạng chập chờn không bị tính tiền hai lần</span>
                </div>
                <div className="d-flex align-items-center gap-2">
                  <CheckCircle2 size={15} className="text-info flex-shrink-0" />
                  <span>Nhận vé điện tử QR tức thì sau khi thanh toán</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {selectedItems.length > 0 && (
        <div
          className="d-lg-none position-fixed bottom-0 start-0 w-100 bg-surface border-top border-strong p-3 shadow-lg"
          style={{ zIndex: 1030 }}
        >
          <div className="d-flex align-items-center justify-content-between gap-3">
            <div>
              <div className="small text-muted">Đã chọn {totalTickets} vé</div>
              <div className="fs-5 fw-bold text-primary tabular-nums">{formatVND(totalAmount)}</div>
            </div>
            <button onClick={handleProceedCheckout} className="btn btn-tr-primary px-4">
              Tiếp tục đặt vé
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
