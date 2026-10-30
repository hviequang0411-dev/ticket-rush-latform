import React, { useState, useEffect } from 'react';
import { useLocation, Link } from 'react-router-dom';
import { ShieldCheck, CreditCard, ArrowLeft, ArrowRight, User, Clock } from 'lucide-react';
import { ApiService } from '../../api/apiClient';
import { displayName, signIn, useAccount } from '../../api/auth';
import type { Event, ApiErrorProblem } from '../../types/models';
import { formatVND, formatDateTimeVN } from '../../lib/format';
import { ErrorState } from '../../components/DataStates';

interface CheckoutSelection {
  eventId: number;
  items: { ticketTypeId: number; quantity: number }[];
}

const SELECTION = 'ticketrush.checkout';

/** The selection survives the round trip to Keycloak when the buyer signs in here. */
function readSelection(state: unknown): CheckoutSelection | null {
  const fromState = state as CheckoutSelection | undefined;
  if (fromState?.eventId && fromState.items?.length) {
    sessionStorage.setItem(SELECTION, JSON.stringify(fromState));
    return fromState;
  }
  const saved = sessionStorage.getItem(SELECTION);
  return saved ? (JSON.parse(saved) as CheckoutSelection) : null;
}

const PROVIDERS = [
  { id: 'MOCK', name: 'Cổng thanh toán giả lập (MOCK)', desc: 'Trang thanh toán thử nghiệm: chọn Thanh toán hoặc Từ chối, kết quả về qua webhook có chữ ký.', enabled: true },
  { id: 'VNPAY', name: 'VNPay (ATM nội địa / QR Pay)', desc: 'Sắp có: cần tài khoản merchant sandbox.', enabled: false },
  { id: 'MOMO', name: 'Ví điện tử MoMo', desc: 'Sắp có.', enabled: false },
];

export const CheckoutPage: React.FC = () => {
  const location = useLocation();
  const [selection] = useState<CheckoutSelection | null>(() => readSelection(location.state));
  const { account, loading: accountLoading } = useAccount();

  const [event, setEvent] = useState<Event | null>(null);
  const [loading, setLoading] = useState<boolean>(selection !== null);
  const [submitting, setSubmitting] = useState<boolean>(false);
  const [apiError, setApiError] = useState<ApiErrorProblem | null>(null);
  const [agreeTerms, setAgreeTerms] = useState<boolean>(true);

  useEffect(() => {
    if (!selection) return;
    ApiService.getEventById(selection.eventId)
      .then(setEvent)
      .catch((err: ApiErrorProblem) => setApiError(err))
      .finally(() => setLoading(false));
  }, [selection]);

  const items = selection?.items.filter(item => item.quantity > 0) ?? [];
  const orderTotal = event
    ? items.reduce((sum, item) => sum + (event.ticketTypes.find(t => t.id === item.ticketTypeId)?.price ?? 0) * item.quantity, 0)
    : 0;

  const handleCreateOrder = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!event || !agreeTerms) return;
    try {
      setSubmitting(true);
      setApiError(null);
      const order = await ApiService.createOrder({ eventId: event.id, items });
      sessionStorage.removeItem(SELECTION);
      if (order.paymentUrl) {
        // The provider's hosted page; it sends the buyer back to /order-result
        window.location.assign(order.paymentUrl);
      } else {
        window.location.assign(`/order-result?orderCode=${encodeURIComponent(order.code)}`);
      }
    } catch (err: unknown) {
      setApiError(err as ApiErrorProblem);
      setSubmitting(false);
    }
  };

  if (!selection || items.length === 0) {
    return (
      <div className="tr-container py-5">
        <ErrorState title="Chưa chọn vé" message="Hãy chọn sự kiện và loại vé trước khi thanh toán." status={400} />
        <Link to="/events" className="btn btn-tr-primary mt-3">Xem sự kiện</Link>
      </div>
    );
  }

  if (loading || accountLoading) {
    return (
      <div className="tr-container py-5">
        <div className="tr-skeleton w-50 mb-3" style={{ height: 32 }} />
        <div className="tr-skeleton w-100 rounded-3" style={{ height: 300 }} />
      </div>
    );
  }

  if (!account) {
    return (
      <div className="tr-container py-5 d-flex justify-content-center">
        <div className="tr-card p-4 p-md-5 text-center" style={{ maxWidth: 520 }}>
          <User size={40} className="text-primary mb-3" />
          <h1 className="fs-4 fw-bold mb-2">Đăng nhập để giữ chỗ</h1>
          <p className="text-muted mb-4">Vé được gắn với tài khoản của bạn. Lựa chọn vé vẫn được giữ nguyên sau khi đăng nhập.</p>
          <button type="button" className="btn btn-tr-primary w-100 py-2" onClick={() => signIn('/checkout')}>
            Đăng nhập hoặc tạo tài khoản
          </button>
        </div>
      </div>
    );
  }

  if (!event) {
    return (
      <div className="tr-container py-5">
        <ErrorState title={apiError?.title ?? 'Không tải được sự kiện'} message={apiError?.detail ?? ''} status={apiError?.status} />
      </div>
    );
  }

  return (
    <div className="tr-container py-4">
      <div className="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3 mb-4">
        <div>
          <Link to={`/events/${event.slug}`} className="btn btn-sm btn-tr-secondary mb-2 d-inline-flex align-items-center gap-1">
            <ArrowLeft size={16} /> Quay lại chọn vé
          </Link>
          <h1 className="fs-3 fw-bold mb-0">Xác nhận đơn hàng &amp; Thanh toán</h1>
        </div>
        <div className="d-flex align-items-center gap-2 small text-muted">
          <Clock size={16} /> Ghế được giữ 10 phút kể từ khi bạn bấm Thanh toán
        </div>
      </div>

      {apiError && (
        <ErrorState
          title={apiError.title}
          message={`${apiError.detail} ${apiError.retryAfterSeconds ? `(Thử lại sau ${apiError.retryAfterSeconds} giây)` : ''}`}
          status={apiError.status}
          onRetry={() => setApiError(null)}
        />
      )}

      <form onSubmit={handleCreateOrder} className="row g-4">
        <div className="col-12 col-lg-7">
          <div className="tr-card p-4 mb-4">
            <div className="d-flex align-items-center gap-2 mb-3 pb-2 border-bottom border-subtle">
              <User size={20} className="text-primary" />
              <h2 className="fs-5 fw-bold mb-0">Người mua</h2>
            </div>
            <div className="fw-semibold text-main">{displayName(account)}</div>
            <div className="small text-muted">{account.email || account.login}</div>
            <div className="form-text small mt-2">Vé điện tử được gắn với tài khoản này.</div>
          </div>

          <div className="tr-card p-4 mb-4">
            <div className="d-flex align-items-center gap-2 mb-3 pb-2 border-bottom border-subtle">
              <CreditCard size={20} className="text-primary" />
              <h2 className="fs-5 fw-bold mb-0">Phương thức thanh toán</h2>
            </div>
            <div className="d-flex flex-column gap-3">
              {PROVIDERS.map(method => (
                <label
                  key={method.id}
                  className={`p-3 rounded-3 border d-flex align-items-start gap-3 ${method.enabled ? 'border-primary bg-primary-subtle' : 'border-subtle bg-surface opacity-50'}`}
                  style={{ cursor: method.enabled ? 'pointer' : 'not-allowed' }}
                >
                  <input type="radio" name="paymentProvider" className="form-check-input mt-1" checked={method.enabled} disabled={!method.enabled} readOnly />
                  <div>
                    <div className="fw-bold text-main">{method.name}</div>
                    <div className="small text-muted">{method.desc}</div>
                  </div>
                </label>
              ))}
            </div>
          </div>

          <div className="mb-4">
            <div className="form-check">
              <input id="termsCheck" type="checkbox" className="form-check-input" checked={agreeTerms} onChange={e => setAgreeTerms(e.target.checked)} />
              <label htmlFor="termsCheck" className="form-check-label small text-muted">
                Tôi đồng ý với Điều khoản mua vé và quy định hoàn/huỷ của TicketRush.
              </label>
            </div>
          </div>
        </div>

        <div className="col-12 col-lg-5">
          <div className="tr-card p-4 shadow-sm sticky-top" style={{ top: '88px' }}>
            <h3 className="fs-5 fw-bold mb-3 pb-2 border-bottom border-subtle">Chi tiết đơn hàng</h3>
            <div className="mb-3">
              <div className="fw-bold text-main fs-6 mb-1">{event.name}</div>
              <div className="small text-muted mb-1">{formatDateTimeVN(event.startTime)}</div>
              <div className="small text-muted">{event.venue.name}, {event.venue.city}</div>
            </div>
            <div className="pt-3 border-top border-subtle mb-3">
              <div className="fw-semibold small mb-2">Các vé đã chọn:</div>
              <div className="d-flex flex-column gap-2">
                {items.map(item => {
                  const tt = event.ticketTypes.find(t => t.id === item.ticketTypeId);
                  if (!tt) return null;
                  return (
                    <div key={tt.id} className="d-flex justify-content-between small">
                      <span>{tt.name} × {item.quantity}</span>
                      <span className="fw-semibold tabular-nums">{formatVND(tt.price * item.quantity)}</span>
                    </div>
                  );
                })}
              </div>
            </div>
            <div className="pt-3 border-top border-subtle mb-4">
              <div className="d-flex justify-content-between align-items-center mb-1">
                <span className="text-muted">Tổng số tiền:</span>
                <span className="fs-3 fw-bold text-primary tabular-nums">{formatVND(orderTotal)}</span>
              </div>
              <div className="small text-muted" style={{ fontSize: '0.75rem' }}>
                Giá do hệ thống tính lại khi đặt; không tin giá gửi từ trình duyệt.
              </div>
            </div>
            <button type="submit" disabled={submitting || !agreeTerms} className="btn btn-tr-primary w-100 py-3">
              {submitting ? 'Đang giữ chỗ...' : <>Thanh toán ngay <ArrowRight size={18} /></>}
            </button>
            <div className="d-flex align-items-center justify-content-center gap-2 mt-3 small text-muted">
              <ShieldCheck size={16} className="text-success" />
              <span>Bấm lại khi mạng chập chờn không tạo đơn thứ hai</span>
            </div>
          </div>
        </div>
      </form>
    </div>
  );
};
