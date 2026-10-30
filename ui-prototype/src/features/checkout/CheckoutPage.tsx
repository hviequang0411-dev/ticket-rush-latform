import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate, Link } from 'react-router-dom';
import { ShieldCheck, CreditCard, Lock, ArrowLeft, CheckCircle2, Clock, Sparkles } from 'lucide-react';
import { ApiService } from '../../api/apiClient';
import { displayName, signIn, useAccount } from '../../api/auth';
import type { Event, OrderItem, ApiErrorProblem } from '../../types/models';
import { ErrorState } from '../../components/DataStates';
import { formatVND, formatDateTimeVN } from '../../lib/format';

interface CheckoutSelection {
  event: Event;
  items: OrderItem[];
  totalAmount: number;
}

function readSelection(state: unknown): CheckoutSelection | null {
  if (state && typeof state === 'object' && 'event' in state) {
    return state as CheckoutSelection;
  }
  const saved = sessionStorage.getItem('ticketrush.checkout');
  if (!saved) return null;
  try {
    return JSON.parse(saved) as CheckoutSelection;
  } catch {
    return null;
  }
}

export const CheckoutPage: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { account } = useAccount();

  const state = readSelection(location.state);

  const [customerName, setCustomerName] = useState('Nguyễn Minh Quân');
  const [customerEmail, setCustomerEmail] = useState('minhquan.nguyen@example.com');
  const [customerPhone, setCustomerPhone] = useState('0912345678');
  const [provider, setProvider] = useState<'MOCK' | 'VNPAY' | 'MOMO'>('VNPAY');

  useEffect(() => {
    if (!account) return;
    setCustomerName(displayName(account));
    if (account.email) setCustomerEmail(account.email);
  }, [account]);

  const [formErrors, setFormErrors] = useState<{ name?: string; email?: string; phone?: string }>({});
  const [submitting, setSubmitting] = useState(false);
  const [apiError, setApiError] = useState<ApiErrorProblem | null>(null);

  if (!state || !state.event || !state.items || state.items.length === 0) {
    return (
      <div className="tr-container py-5 text-center" style={{ maxWidth: 560 }}>
        <div className="tr-card p-4 p-md-5">
          <h2 className="fs-4 fw-bold mb-2">Chưa có thông tin chọn vé</h2>
          <p className="text-muted small mb-4">Vui lòng chọn sự kiện và số lượng vé trước khi tiến hành thanh toán.</p>
          <Link to="/events" className="btn btn-tr-primary text-decoration-none">Khám phá sự kiện</Link>
        </div>
      </div>
    );
  }

  const validateForm = () => {
    const errs: { name?: string; email?: string; phone?: string } = {};
    if (!customerName.trim()) errs.name = 'Vui lòng nhập họ và tên người nhận vé.';
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(customerEmail)) errs.email = 'Địa chỉ email không hợp lệ.';
    if (!/^(0|\+84)[0-9]{9}$/.test(customerPhone.trim())) errs.phone = 'Số điện thoại phải gồm 10 chữ số hợp lệ.';
    setFormErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleConfirmOrder = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validateForm()) return;

    try {
      setSubmitting(true);
      setApiError(null);
      const order = await ApiService.createOrder({
        eventId: state.event.id,
        items: state.items,
      });
      sessionStorage.removeItem('ticketrush.checkout');
      if (order.paymentUrl) {
        window.location.href = order.paymentUrl;
      } else {
        navigate(`/order-result?orderCode=${encodeURIComponent(order.code)}`);
      }
    } catch (err: unknown) {
      const errorObj = err as ApiErrorProblem;
      if (errorObj.status === 401) {
        signIn('/checkout');
        return;
      }
      if (errorObj.status === 428) {
        navigate(`/queue?event=${encodeURIComponent(state.event.slug)}`);
        return;
      }
      setApiError(errorObj);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="tr-container py-4">
      <div className="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4 pb-3 border-bottom border-subtle">
        <div>
          <Link to={`/events/${state.event.slug}`} className="text-muted text-decoration-none small d-inline-flex align-items-center gap-1 mb-1">
            <ArrowLeft size={16} /> Quay lại trang sự kiện
          </Link>
          <h1 className="fs-3 fw-bold mb-0">Xác nhận đặt vé &amp; Thanh toán</h1>
        </div>
        <div className="tr-badge badge-warning py-2 px-3">
          <Clock size={15} />
          <span>Đồng hồ giữ ghế <strong>10:00</strong> kích hoạt ngay khi bấm xác nhận</span>
        </div>
      </div>

      {apiError && (
        <ErrorState
          title={apiError.title}
          message={apiError.detail}
          status={apiError.status}
          onRetry={() => setApiError(null)}
        />
      )}

      <div className="row g-4">
        <div className="col-12 col-lg-7">
          <form onSubmit={handleConfirmOrder} noValidate>
            <div className="tr-card p-4 mb-4">
              <div className="d-flex align-items-center justify-content-between mb-3">
                <h2 className="fs-5 fw-bold mb-0">1. Thông tin người nhận vé điện tử</h2>
                <span className="tr-badge badge-info">Vé QR gửi qua Email</span>
              </div>
              <p className="text-muted small mb-3">
                Mã vé QR và biên lai điện tử sẽ được cấp cho tài khoản của bạn ngay sau khi cổng thanh toán xác nhận.
              </p>

              <div className="mb-3">
                <label htmlFor="buyerName" className="form-label small fw-semibold">
                  Họ và tên người tham dự <span className="text-danger">*</span>
                </label>
                <input
                  id="buyerName"
                  type="text"
                  className={`form-control ${formErrors.name ? 'is-invalid' : ''}`}
                  value={customerName}
                  onChange={(e) => setCustomerName(e.target.value)}
                />
                {formErrors.name && <div className="invalid-feedback">{formErrors.name}</div>}
              </div>

              <div className="row g-3">
                <div className="col-12 col-md-6">
                  <label htmlFor="buyerEmail" className="form-label small fw-semibold">
                    Địa chỉ Email nhận vé <span className="text-danger">*</span>
                  </label>
                  <input
                    id="buyerEmail"
                    type="email"
                    className={`form-control ${formErrors.email ? 'is-invalid' : ''}`}
                    value={customerEmail}
                    onChange={(e) => setCustomerEmail(e.target.value)}
                  />
                  {formErrors.email && <div className="invalid-feedback">{formErrors.email}</div>}
                </div>

                <div className="col-12 col-md-6">
                  <label htmlFor="buyerPhone" className="form-label small fw-semibold">
                    Số điện thoại liên hệ <span className="text-danger">*</span>
                  </label>
                  <input
                    id="buyerPhone"
                    type="tel"
                    className={`form-control tabular-nums ${formErrors.phone ? 'is-invalid' : ''}`}
                    value={customerPhone}
                    onChange={(e) => setCustomerPhone(e.target.value)}
                  />
                  {formErrors.phone && <div className="invalid-feedback">{formErrors.phone}</div>}
                </div>
              </div>
            </div>

            <div className="tr-card p-4 mb-4">
              <h2 className="fs-5 fw-bold mb-3">2. Chọn phương thức thanh toán</h2>
              <div className="d-flex flex-column gap-2">
                {[
                  { id: 'VNPAY', name: 'Cổng thanh toán VNPay (QR Ngân hàng / Thẻ ATM / Quốc tế)', badge: 'Khuyên dùng' },
                  { id: 'MOCK', name: 'Cổng Thanh Toán Giả Lập TicketRush (Sandbox Test)', badge: 'Test nhanh' },
                  { id: 'MOMO', name: 'Ví điện tử MoMo (Quét mã thanh toán tức thì)', badge: 'Phổ biến' },
                ].map((method) => {
                  const isSelected = provider === method.id;
                  return (
                    <label
                      key={method.id}
                      className={`p-3 rounded-3 border d-flex align-items-center justify-content-between transition-all ${
                        isSelected ? 'border-primary' : 'border-subtle bg-surface'
                      }`}
                      style={{
                        cursor: 'pointer',
                        backgroundColor: isSelected ? 'var(--color-primary-subtle)' : 'var(--bg-surface)',
                      }}
                    >
                      <div className="d-flex align-items-center gap-3">
                        <input
                          type="radio"
                          name="paymentProvider"
                          className="form-check-input mt-0"
                          checked={isSelected}
                          onChange={() => setProvider(method.id as 'MOCK' | 'VNPAY' | 'MOMO')}
                        />
                        <div>
                          <div className="fw-semibold text-main d-flex align-items-center gap-2">
                            <CreditCard size={16} className="text-primary" />
                            <span>{method.name}</span>
                          </div>
                        </div>
                      </div>
                      <span className="tr-badge badge-neutral">{method.badge}</span>
                    </label>
                  );
                })}
              </div>
            </div>

            <button
              type="submit"
              disabled={submitting}
              className="btn btn-tr-primary w-100 py-3 fs-6"
            >
              {submitting ? (
                <>
                  <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true" />
                  Đang khoá vé &amp; khởi tạo giao dịch...
                </>
              ) : (
                <>
                  <Lock size={18} />
                  <span>Xác nhận giữ chỗ &amp; Thanh toán ({formatVND(state.totalAmount)})</span>
                </>
              )}
            </button>
          </form>
        </div>

        <div className="col-12 col-lg-5">
          <div className="tr-card p-4 sticky-top" style={{ top: '84px' }}>
            <div className="d-flex align-items-center justify-content-between mb-3 pb-2 border-bottom border-subtle">
              <h2 className="fs-5 fw-bold mb-0">Chi tiết đơn đặt vé</h2>
              <span className="tr-badge badge-success">
                <Sparkles size={12} /> Giá niêm yết
              </span>
            </div>

            <div className="mb-3 pb-3 border-bottom border-subtle">
              <div className="fw-bold text-main mb-1">{state.event.name}</div>
              <div className="small text-muted mb-1 tabular-nums">{formatDateTimeVN(state.event.startTime)}</div>
              <div className="small text-muted">{state.event.venue.name}, {state.event.venue.city}</div>
            </div>

            <div className="d-flex flex-column gap-2 mb-3 pb-3 border-bottom border-subtle">
              {state.items.map((item) => (
                <div key={item.ticketTypeId} className="d-flex justify-content-between small">
                  <span>
                    <strong className="text-main">{item.ticketTypeName}</strong> × {item.quantity}
                  </span>
                  <span className="fw-semibold text-main tabular-nums">
                    {formatVND(item.unitPrice * item.quantity)}
                  </span>
                </div>
              ))}
              <div className="d-flex justify-content-between small text-muted">
                <span>Phí xử lý hệ thống &amp; Bảo hiểm vé</span>
                <span className="text-success fw-medium">0 ₫ (Miễn phí)</span>
              </div>
            </div>

            <div className="d-flex justify-content-between align-items-center mb-3">
              <span className="fw-bold text-main">Tổng thanh toán</span>
              <span className="fs-4 fw-bold text-primary tabular-nums">{formatVND(state.totalAmount)}</span>
            </div>

            <div className="p-3 bg-surface-subtle rounded-3 border border-subtle small text-muted">
              <div className="d-flex align-items-center gap-2 mb-1 text-main fw-semibold">
                <ShieldCheck size={16} className="text-success" />
                <span>Thanh toán an toàn</span>
              </div>
              <div className="d-flex align-items-start gap-2 mt-1">
                <CheckCircle2 size={14} className="text-success flex-shrink-0 mt-1" />
                <span>
                  Nếu bạn thanh toán xong đúng lúc thời gian giữ ghế vừa hết, bạn sẽ được <strong>hoàn tiền đầy đủ</strong> tự động.
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
