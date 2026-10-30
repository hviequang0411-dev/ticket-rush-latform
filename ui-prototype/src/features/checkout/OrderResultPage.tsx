import React, { useState, useEffect } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { CheckCircle2, XCircle, Clock, RotateCcw, Wallet, ArrowRight, RefreshCw, Sparkles, Calendar, MapPin } from 'lucide-react';
import { QRCodeSVG } from 'qrcode.react';
import { ApiService } from '../../api/apiClient';
import type { OrderSummary } from '../../types/models';
import { StatusBadge } from '../../components/StatusBadge';
import { ReservationTimer } from '../../components/ReservationTimer';
import { formatVND, formatDateTimeVN } from '../../lib/format';

const PENDING = ['PENDING', 'RESERVED', 'AWAITING_PAYMENT'];

export const OrderResultPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const orderCode = searchParams.get('orderCode') || 'ORD-88231';
  const urlStatus = searchParams.get('status');
  const failureReason = searchParams.get('reason');

  const [order, setOrder] = useState<OrderSummary | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [cancelling, setCancelling] = useState<boolean>(false);

  const loadOrder = async () => {
    try {
      const data = await ApiService.getOrderByCode(orderCode);
      setOrder(data);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    setLoading(true);
    loadOrder();
  }, [orderCode, urlStatus]);

  useEffect(() => {
    if (!order || !PENDING.includes(order.status)) return;
    const timer = setInterval(loadOrder, 2000);
    return () => clearInterval(timer);
  }, [order?.status, orderCode]);

  const handleCancelOrder = async () => {
    if (!order) return;
    setCancelling(true);
    try {
      const updated = await ApiService.cancelOrder(order.code);
      setOrder(updated);
    } finally {
      setCancelling(false);
    }
  };

  if (loading || !order) {
    return (
      <div className="tr-container py-5" style={{ maxWidth: 680 }}>
        <div className="tr-skeleton w-100 rounded-4" style={{ height: 380 }} />
      </div>
    );
  }

  const isSuccess = order.status === 'CONFIRMED';
  const isRefunded = order.status === 'REFUNDED';
  const isExpired = order.status === 'EXPIRED';
  const isPending = PENDING.includes(order.status);

  return (
    <div className="tr-container py-5" style={{ maxWidth: 740 }}>
      <div className="tr-card tr-card-featured p-4 p-md-5">
        <div className="text-center mb-4 pb-3 border-bottom border-subtle">
          {isSuccess ? (
            <div
              className="d-inline-flex p-3 rounded-circle mb-3"
              style={{ backgroundColor: 'var(--color-success-bg)', color: 'var(--color-success)' }}
            >
              <CheckCircle2 size={48} />
            </div>
          ) : isRefunded ? (
            <div
              className="d-inline-flex p-3 rounded-circle mb-3"
              style={{ backgroundColor: 'var(--color-info-bg)', color: 'var(--color-info)' }}
            >
              <RotateCcw size={48} />
            </div>
          ) : isPending ? (
            <div
              className="d-inline-flex p-3 rounded-circle mb-3"
              style={{ backgroundColor: 'var(--color-warning-bg)', color: 'var(--color-warning)' }}
            >
              <Clock size={48} />
            </div>
          ) : (
            <div
              className="d-inline-flex p-3 rounded-circle mb-3"
              style={{ backgroundColor: 'var(--color-danger-bg)', color: 'var(--color-danger)' }}
            >
              <XCircle size={48} />
            </div>
          )}

          <div className="mb-2">
            <StatusBadge type="order" status={order.status} />
          </div>

          <h1 className="fs-3 fw-bold text-main mb-1">
            {isSuccess
              ? 'Đặt vé thành công! Vé điện tử đã sẵn sàng'
              : isRefunded
              ? 'Đơn hàng đã được hoàn tiền tự động'
              : isExpired
              ? 'Phiên giữ chỗ 10 phút đã hết hạn'
              : isPending
              ? 'Đang chờ xác nhận thanh toán từ cổng'
              : 'Giao dịch thanh toán chưa hoàn tất'}
          </h1>

          <p className="text-muted small mb-0">
            Mã đơn hàng: <strong className="font-monospace text-main">{order.code}</strong> • Khởi tạo lúc{' '}
            <span className="tabular-nums">{formatDateTimeVN(order.createdAt)}</span>
          </p>

          {isPending && order.expiresAt && (
            <div className="mt-3">
              <ReservationTimer expiresAt={order.expiresAt} onExpire={loadOrder} />
            </div>
          )}

          {(failureReason || order.failureReason) && !isSuccess && (
            <div className="alert alert-warning small mt-3 mb-0 text-start">
              <strong>Chi tiết trạng thái:</strong> {failureReason || order.failureReason}
            </div>
          )}
        </div>

        {/* Perforated Boarding-Pass E-Ticket Preview (When CONFIRMED) */}
        {isSuccess && (
          <div className="tr-ticket-pass mb-4">
            <div
              className="px-4 py-3 text-white d-flex justify-content-between align-items-center"
              style={{
                background: 'linear-gradient(135deg, #090d16 0%, #1e293b 100%)',
              }}
            >
              <div className="d-flex align-items-center gap-2">
                <Sparkles size={16} className="text-warning" />
                <span className="small fw-semibold font-monospace" style={{ letterSpacing: '0.06em' }}>
                  E-TICKET BOARDING PASS
                </span>
              </div>
              <span className="badge rounded-pill bg-success text-white">XÁC THỰC QR</span>
            </div>

            <div className="p-4">
              <div className="row align-items-center g-4">
                <div className="col-12 col-sm-4 text-center">
                  <div className="tr-qr-frame p-3 rounded-3 border d-inline-block shadow-xs">
                    <QRCodeSVG
                      value={`TICKETRUSH::PASS::${order.code}::${order.customerEmail}`}
                      size={132}
                      level="M"
                    />
                  </div>
                  <div className="small text-muted font-monospace mt-2" style={{ fontSize: '0.72rem' }}>
                    {order.code}
                  </div>
                </div>
                <div className="col-12 col-sm-8">
                  <h3 className="fs-5 fw-bold text-main mb-2">{order.event.name}</h3>
                  <div className="small text-muted mb-1 d-flex align-items-center gap-2">
                    <Calendar size={15} className="text-primary flex-shrink-0" />
                    <span className="tabular-nums">{formatDateTimeVN(order.event.startTime)}</span>
                  </div>
                  <div className="small text-muted mb-3 d-flex align-items-center gap-2">
                    <MapPin size={15} className="text-primary flex-shrink-0" />
                    <span>{order.event.venueName} — {order.event.venueAddress}</span>
                  </div>
                  <div className="p-2 px-3 rounded-3 bg-surface-subtle border border-subtle small">
                    <div>Người sở hữu: <strong className="text-main">{order.customerName}</strong></div>
                    <div>Email nhận vé: <strong className="text-main">{order.customerEmail}</strong></div>
                  </div>
                </div>
              </div>

              <div className="tr-ticket-divider my-3" />

              <div className="d-flex flex-wrap justify-content-between align-items-center small text-muted">
                <span>Xuất trình mã QR này tại cổng soát vé</span>
                <span className="font-monospace fw-semibold text-primary">HỢP LỆ • CHƯA SOÁT</span>
              </div>
            </div>
          </div>
        )}

        <div className="mb-4">
          <h4 className="fs-6 fw-bold mb-2">Chi tiết hạng vé trong đơn</h4>
          <div className="border border-subtle rounded-3 overflow-hidden">
            {order.items.map((item) => (
              <div
                key={item.ticketTypeId}
                className="p-3 d-flex justify-content-between align-items-center border-bottom border-subtle small bg-surface"
              >
                <div>
                  <span className="fw-semibold text-main">{item.ticketTypeName}</span>
                  <span className="text-muted ms-2">× {item.quantity} vé</span>
                </div>
                <span className="fw-bold text-main tabular-nums">
                  {formatVND(item.unitPrice * item.quantity)}
                </span>
              </div>
            ))}
            <div className="p-3 bg-surface-subtle d-flex justify-content-between align-items-center">
              <span className="fw-bold text-main">Tổng cộng</span>
              <span className="fs-5 fw-bold text-primary tabular-nums">{formatVND(order.totalAmount)}</span>
            </div>
          </div>
        </div>

        <div className="d-flex flex-wrap gap-3 justify-content-between align-items-center pt-2">
          {isSuccess ? (
            <>
              <Link to="/wallet" className="btn btn-tr-primary flex-grow-1 text-decoration-none">
                <Wallet size={18} />
                <span>Mở Ví vé của tôi</span>
              </Link>
              <Link to="/my-orders" className="btn btn-tr-secondary text-decoration-none">
                <span>Lịch sử đơn hàng</span>
                <ArrowRight size={16} />
              </Link>
            </>
          ) : isPending ? (
            <>
              {order.paymentUrl && (
                <a href={order.paymentUrl} className="btn btn-tr-primary flex-grow-1 text-decoration-none">
                  <span>Tiếp tục thanh toán</span>
                  <ArrowRight size={16} />
                </a>
              )}
              <button
                type="button"
                onClick={handleCancelOrder}
                disabled={cancelling}
                className="btn btn-tr-secondary text-danger"
              >
                {cancelling ? 'Đang huỷ...' : 'Huỷ giữ chỗ'}
              </button>
            </>
          ) : (
            <>
              <Link to="/events" className="btn btn-tr-primary flex-grow-1 text-decoration-none">
                <RefreshCw size={18} />
                <span>Đặt lại vé sự kiện</span>
              </Link>
              <Link to="/my-orders" className="btn btn-tr-secondary text-decoration-none">
                <span>Lịch sử đơn hàng</span>
                <ArrowRight size={16} />
              </Link>
            </>
          )}
        </div>
      </div>
    </div>
  );
};
