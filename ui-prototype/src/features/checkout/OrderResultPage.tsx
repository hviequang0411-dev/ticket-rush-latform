import React, { useEffect, useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { CheckCircle2, XCircle, AlertTriangle, RotateCcw, Wallet, ArrowRight, Loader2, CreditCard } from 'lucide-react';
import { QRCodeSVG } from 'qrcode.react';
import { ApiService } from '../../api/apiClient';
import type { OrderSummary, ApiErrorProblem } from '../../types/models';
import { formatVND, formatDateTimeVN } from '../../lib/format';
import { ErrorState } from '../../components/DataStates';

// The payment result arrives asynchronously (webhook -> Kafka -> order saga), so the page polls until it settles
const IN_FLIGHT: OrderSummary['status'][] = ['PENDING', 'RESERVED', 'AWAITING_PAYMENT'];
const POLL_MS = 2000;

function secondsLeft(expiresAt?: string): number {
  return expiresAt ? Math.max(0, Math.floor((new Date(expiresAt).getTime() - Date.now()) / 1000)) : 0;
}

export const OrderResultPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const orderCode = searchParams.get('orderCode') ?? '';

  const [order, setOrder] = useState<OrderSummary | null>(null);
  const [error, setError] = useState<ApiErrorProblem | null>(null);
  const [now, setNow] = useState<number>(() => Date.now());

  useEffect(() => {
    if (!orderCode) return;
    let active = true;
    let timer: ReturnType<typeof setTimeout>;
    const poll = async () => {
      try {
        const data = await ApiService.getOrderByCode(orderCode);
        if (!active) return;
        setOrder(data);
        setError(null);
        if (IN_FLIGHT.includes(data.status)) timer = setTimeout(poll, POLL_MS);
      } catch (err: unknown) {
        if (active) setError(err as ApiErrorProblem);
      }
    };
    poll();
    return () => {
      active = false;
      clearTimeout(timer);
    };
  }, [orderCode]);

  useEffect(() => {
    const tick = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(tick);
  }, []);

  if (!orderCode) {
    return (
      <div className="tr-container py-5">
        <ErrorState title="Thiếu mã đơn hàng" message="Mở đơn hàng từ Lịch sử đơn hàng." status={400} />
        <Link to="/my-orders" className="btn btn-tr-primary mt-3">Lịch sử đơn hàng</Link>
      </div>
    );
  }

  if (error && !order) {
    return (
      <div className="tr-container py-5">
        <ErrorState title={error.title} message={error.detail} status={error.status} />
      </div>
    );
  }

  if (!order) {
    return (
      <div className="tr-container py-5 text-center">
        <div className="tr-skeleton w-50 mx-auto mb-3" style={{ height: 40 }} />
        <div className="tr-skeleton w-75 mx-auto" style={{ height: 200 }} />
      </div>
    );
  }

  const status = order.status;

  // 0. Still in flight: seats held, waiting for the buyer to pay or for the payment result
  if (IN_FLIGHT.includes(status)) {
    const left = now > 0 ? secondsLeft(order.expiresAt) : 0;
    return (
      <div className="tr-container py-5 d-flex justify-content-center">
        <div className="tr-card p-4 p-md-5 shadow-md bg-surface text-center" style={{ maxWidth: 560, width: '100%' }}>
          <div className="d-inline-flex p-3 rounded-circle bg-primary-subtle text-primary mb-3">
            <Loader2 size={48} className="tr-spin" />
          </div>
          <h1 className="fs-3 fw-bold mb-2">{status === 'AWAITING_PAYMENT' ? 'Đang chờ thanh toán' : 'Đang giữ chỗ cho bạn'}</h1>
          <p className="text-muted small mb-4">
            Đơn <strong className="font-monospace text-main">{orderCode}</strong> • {formatVND(order.totalAmount)}. Trang tự cập nhật khi có kết quả thanh toán.
          </p>
          {status === 'AWAITING_PAYMENT' && (
            <>
              {order.expiresAt && (
                <div className="fs-4 fw-bold tabular-nums mb-3">
                  {String(Math.floor(left / 60)).padStart(2, '0')}:{String(left % 60).padStart(2, '0')}
                </div>
              )}
              {order.paymentUrl && (
                <a href={order.paymentUrl} className="btn btn-tr-primary px-4 py-3 d-inline-flex align-items-center gap-2">
                  <CreditCard size={18} /> Tiếp tục thanh toán
                </a>
              )}
            </>
          )}
        </div>
      </div>
    );
  }

  // 1. CONFIRMED Case
  if (status === 'CONFIRMED') {
    return (
      <div className="tr-container py-5 d-flex justify-content-center">
        <div className="tr-card p-4 p-md-5 shadow-md bg-surface" style={{ maxWidth: 640, width: '100%' }}>
          <div className="text-center mb-4">
            <div className="d-inline-flex p-3 rounded-circle bg-success-subtle text-success mb-3">
              <CheckCircle2 size={48} />
            </div>
            <h1 className="fs-3 fw-bold mb-1">Thanh toán &amp; Giữ vé thành công!</h1>
            <p className="text-muted small">
              Mã xác nhận đơn hàng: <strong className="font-monospace text-main">{orderCode}</strong>
            </p>
          </div>

          {/* E-Ticket Pass Preview */}
          <div className="p-4 rounded-3 border border-subtle bg-surface-subtle mb-4 position-relative overflow-hidden">
            <div className="d-flex justify-content-between align-items-start mb-3 border-bottom border-subtle pb-3">
              <div>
                <span className="badge bg-primary text-white text-uppercase tracking-wider small mb-1">VÉ ĐIỆN TỬ CHÍNH THỨC</span>
                <h3 className="fs-5 fw-bold mb-0 text-main">{order.event.name}</h3>
              </div>
              <QRCodeSVG
                value={`TICKETRUSH::${orderCode}::VALID`}
                size={72}
                level="M"
                aria-label={`Mã QR cho đơn hàng ${orderCode}`}
              />
            </div>

            <div className="row g-2 small text-muted mb-3">
              <div className="col-12 col-sm-6">
                <span className="text-muted">Thời gian:</span>
                <div className="fw-semibold text-main">{formatDateTimeVN(order.event.startTime)}</div>
              </div>
              <div className="col-12 col-sm-6">
                <span className="text-muted">Địa điểm:</span>
                <div className="fw-semibold text-main">{order.event.venueName}</div>
              </div>
              <div className="col-12 col-sm-6">
                <span className="text-muted">Người sở hữu:</span>
                <div className="fw-semibold text-main">{order.customerName}</div>
              </div>
              <div className="col-12 col-sm-6">
                <span className="text-muted">Tổng thanh toán:</span>
                <div className="fw-bold text-primary tabular-nums">{formatVND(order.totalAmount)}</div>
              </div>
            </div>

            <div className="small text-muted border-top border-subtle pt-2" style={{ fontSize: '0.75rem' }}>
              Vé đã được tự động lưu vào <strong>Ví vé TicketRush</strong>. Bạn có thể mở vé bất cứ lúc nào không cần kết nối mạng.
            </div>
          </div>

          <div className="d-flex flex-column flex-sm-row gap-3">
            <Link to="/wallet" className="btn btn-tr-primary flex-grow-1 py-3 d-flex align-items-center justify-content-center gap-2">
              <Wallet size={18} /> Mở Ví vé để soát vé
            </Link>
            <Link to="/my-orders" className="btn btn-tr-secondary flex-grow-1 py-3 d-flex align-items-center justify-content-center gap-2">
              Xem lịch sử đơn hàng <ArrowRight size={18} />
            </Link>
          </div>
        </div>
      </div>
    );
  }

  // 2. FAILED Case
  if (status === 'FAILED') {
    return (
      <div className="tr-container py-5 d-flex justify-content-center">
        <div className="tr-card p-4 p-md-5 shadow-md bg-surface text-center" style={{ maxWidth: 560, width: '100%' }}>
          <div className="d-inline-flex p-3 rounded-circle bg-danger-subtle text-danger mb-3">
            <XCircle size={48} />
          </div>
          <h1 className="fs-3 fw-bold mb-2 text-danger">Giao dịch không thành công</h1>
          <p className="text-muted small mb-4">
            Ngân hàng phát hành hoặc cổng thanh toán đã từ chối giao dịch hoặc tài khoản của bạn chưa trừ tiền. Ghế đã được trả lại kho; bạn có thể đặt lại.
          </p>

          <div className="p-3 bg-surface-subtle rounded-3 border border-subtle mb-4 text-start small">
            <div className="fw-semibold mb-1">Mã đơn hàng: {orderCode}</div>
            <div className="text-muted">Lý do: {order.failureReason || "Cổng thanh toán từ chối giao dịch."}</div>
          </div>

          <div className="d-flex flex-column flex-sm-row gap-3">
            <Link to="/events" className="btn btn-tr-primary flex-grow-1 py-3 d-flex align-items-center justify-content-center gap-2">
              <RotateCcw size={18} /> Đặt lại vé
            </Link>
            <Link to="/" className="btn btn-tr-secondary flex-grow-1 py-3">
              Quay về trang chủ
            </Link>
          </div>
        </div>
      </div>
    );
  }

  // 3. EXPIRED Case
  if (status === 'EXPIRED' || status === 'CANCELLED') {
    return (
      <div className="tr-container py-5 d-flex justify-content-center">
        <div className="tr-card p-4 p-md-5 shadow-md bg-surface text-center" style={{ maxWidth: 560, width: '100%' }}>
          <div className="d-inline-flex p-3 rounded-circle bg-warning-subtle text-warning mb-3">
            <AlertTriangle size={48} />
          </div>
          <h1 className="fs-3 fw-bold mb-2">{status === 'CANCELLED' ? 'Đơn hàng đã huỷ' : 'Đã hết thời gian giữ chỗ'}</h1>
          <p className="text-muted small mb-4">
            {status === 'CANCELLED' ? `Đơn hàng ${orderCode} đã được huỷ.` : `Đơn hàng ${orderCode} đã quá 10 phút giữ chỗ.`} Số ghế này đã được tự động hoàn trả vào kho vé để khách hàng khác có thể đặt.
          </p>
          <Link to="/events" className="btn btn-tr-primary px-4 py-3">
            Tìm và đặt sự kiện khác
          </Link>
        </div>
      </div>
    );
  }

  // 4. REFUNDED Case
  return (
    <div className="tr-container py-5 d-flex justify-content-center">
      <div className="tr-card p-4 p-md-5 shadow-md bg-surface text-center" style={{ maxWidth: 560, width: '100%' }}>
        <div className="d-inline-flex p-3 rounded-circle bg-secondary-subtle text-secondary mb-3">
          <RotateCcw size={48} />
        </div>
        <h1 className="fs-3 fw-bold mb-2">Đơn hàng đã được hoàn tiền</h1>
        <p className="text-muted small mb-4">
          {order.failureReason || 'Đơn hàng gặp sự cố đồng bộ hoặc huỷ theo yêu cầu. Số tiền đã được hoàn 100% về tài khoản ban đầu.'}
        </p>
        <Link to="/my-orders" className="btn btn-tr-secondary px-4 py-3">
          Kiểm tra lịch sử đơn hàng
        </Link>
      </div>
    </div>
  );
};
