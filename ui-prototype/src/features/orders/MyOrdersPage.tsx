import React, { useState, useEffect } from 'react';
import { ShoppingBag, CheckCircle2, Clock, RotateCcw, Calendar, MapPin, ArrowRight, XCircle } from 'lucide-react';
import { ApiService } from '../../api/apiClient';
import type { OrderSummary, ApiErrorProblem } from '../../types/models';
import { signIn } from '../../api/auth';
import { StatusBadge } from '../../components/StatusBadge';
import { formatVND, formatDateTimeVN } from '../../lib/format';
import { EmptyState, ErrorState } from '../../components/DataStates';

/** Presentational order timeline: where the order stands, in the buyer's words */
const OrderSagaTimeline: React.FC<{ order: OrderSummary }> = ({ order }) => {
  const isConfirmed = order.status === 'CONFIRMED';
  const isAwaiting = order.status === 'AWAITING_PAYMENT' || order.status === 'RESERVED';
  const isRefunded = order.status === 'REFUNDED';
  const isExpired = order.status === 'EXPIRED';

  const isFailed = order.status === 'FAILED';
  const isCancelled = order.status === 'CANCELLED';
  const soldOut = isFailed && order.failureReason === 'SOLD_OUT';

  const steps = [
    {
      title: 'Đặt đơn',
      desc: formatDateTimeVN(order.createdAt),
      tone: 'success',
      icon: <CheckCircle2 size={14} />,
    },
    {
      title: soldOut ? 'Không còn đủ ghế' : 'Giữ ghế 10 phút',
      desc: soldOut ? 'Rất tiếc, loại vé bạn chọn đã hết' : 'Ghế được giữ riêng cho bạn trong lúc thanh toán',
      tone: soldOut ? 'danger' : 'success',
      icon: soldOut ? <XCircle size={14} /> : <CheckCircle2 size={14} />,
    },
    {
      title: 'Thanh toán',
      desc: isConfirmed || isRefunded
        ? 'Đã nhận thanh toán'
        : isAwaiting
        ? 'Đang chờ bạn hoàn tất thanh toán'
        : 'Chưa thanh toán',
      tone: isConfirmed || isRefunded ? 'success' : isAwaiting ? 'warning' : 'neutral',
      icon: isConfirmed || isRefunded ? <CheckCircle2 size={14} /> : isAwaiting ? <Clock size={14} /> : <XCircle size={14} />,
    },
    {
      title: isConfirmed
        ? 'Đã xuất vé'
        : isRefunded
        ? 'Đã hoàn tiền'
        : isExpired
        ? 'Hết thời gian giữ ghế'
        : isCancelled
        ? 'Đã huỷ đơn'
        : isFailed
        ? 'Đơn không thành công'
        : 'Xuất vé',
      desc: isConfirmed
        ? 'Vé đã có trong Ví vé, quét mã QR tại cổng để vào'
        : isRefunded
        ? 'Ghế không còn khi tiền về nên bạn đã được hoàn tiền'
        : isExpired
        ? 'Ghế đã được trả lại để người khác có thể mua'
        : isCancelled
        ? 'Ghế đã được trả lại'
        : isFailed
        ? 'Bạn chưa bị trừ tiền cho đơn này'
        : 'Vé sẽ có trong Ví vé ngay sau khi thanh toán',
      tone: isConfirmed ? 'success' : isRefunded ? 'info' : isAwaiting ? 'neutral' : 'danger',
      icon: isConfirmed ? <CheckCircle2 size={14} /> : isAwaiting ? <Clock size={14} /> : <RotateCcw size={14} />,
    },
  ];

  return (
    <div className="p-3 p-md-4 rounded-3 bg-surface-subtle border border-subtle mb-4">
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h4 className="fs-6 fw-bold mb-0">Tiến trình đơn hàng</h4>
      </div>

      <div className="position-relative ps-4 ms-2" style={{ borderLeft: '2px solid var(--border-strong)' }}>
        {steps.map((s, idx) => {
          const dotBg =
            s.tone === 'success'
              ? 'var(--color-success)'
              : s.tone === 'warning'
              ? 'var(--color-warning)'
              : s.tone === 'info'
              ? 'var(--color-info)'
              : s.tone === 'danger'
              ? 'var(--color-danger)'
              : 'var(--text-muted)';

          return (
            <div key={idx} className={`position-relative ${idx < steps.length - 1 ? 'mb-3 pb-1' : ''}`}>
              <div
                className="position-absolute top-0 start-0 translate-middle rounded-circle text-white d-flex align-items-center justify-content-center shadow-xs"
                style={{
                  width: 24,
                  height: 24,
                  left: '-25px',
                  top: '10px',
                  backgroundColor: dotBg,
                }}
              >
                {s.icon}
              </div>
              <div className="fw-semibold text-main small">{s.title}</div>
              <div className="text-muted" style={{ fontSize: '0.78rem' }}>
                {s.desc}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export const MyOrdersPage: React.FC = () => {
  const [orders, setOrders] = useState<OrderSummary[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Status Filter tab
  const [activeTab, setActiveTab] = useState<'ALL' | 'CONFIRMED' | 'AWAITING_PAYMENT' | 'COMPLETED_REFUNDED'>('ALL');
  const [selectedOrder, setSelectedOrder] = useState<OrderSummary | null>(null);

  useEffect(() => {
    loadOrders();
  }, []);

  const loadOrders = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await ApiService.getOrders();
      setOrders(data);
      if (data.length > 0) setSelectedOrder(data[0]);
    } catch (e: unknown) {
      const err = e as ApiErrorProblem;
      if (err.status === 401) {
        signIn('/my-orders');
        return;
      }
      setError(err.message || 'Không thể tải lịch sử đơn hàng.');
    } finally {
      setLoading(false);
    }
  };

  const filteredOrders = orders.filter(o => {
    if (activeTab === 'CONFIRMED') return o.status === 'CONFIRMED';
    if (activeTab === 'AWAITING_PAYMENT') return o.status === 'AWAITING_PAYMENT' || o.status === 'RESERVED';
    if (activeTab === 'COMPLETED_REFUNDED') return o.status === 'REFUNDED' || o.status === 'EXPIRED' || o.status === 'CANCELLED';
    return true;
  });

  const handleCancelOrder = async (orderCode: string) => {
    if (window.confirm('Bạn có chắc chắn muốn huỷ giữ chỗ cho đơn hàng này?')) {
      try {
        await ApiService.cancelOrder(orderCode);
      } catch (e: unknown) {
        window.alert((e as ApiErrorProblem).detail);
      }
      loadOrders();
    }
  };

  return (
    <div className="tr-container py-4">
      <div className="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-4">
        <div>
          <span className="tr-badge badge-info mb-2">LỊCH SỬ GIAO DỊCH</span>
          <h1 className="fs-3 fw-bold mb-1">Đơn hàng của tôi</h1>
          <p className="text-muted small mb-0">
            Theo dõi tiến trình giữ ghế, xác nhận thanh toán và nhánh bù trừ hoàn tiền
          </p>
        </div>
      </div>

      {/* Filter Tabs */}
      <div className="d-flex gap-2 border-bottom border-subtle mb-4 pb-2 overflow-auto" role="tablist">
        {[
          { id: 'ALL', label: 'Tất cả đơn', count: orders.length },
          { id: 'CONFIRMED', label: 'Đã xác nhận', count: orders.filter(o => o.status === 'CONFIRMED').length },
          { id: 'AWAITING_PAYMENT', label: 'Chờ thanh toán', count: orders.filter(o => o.status === 'AWAITING_PAYMENT' || o.status === 'RESERVED').length },
          { id: 'COMPLETED_REFUNDED', label: 'Hết hạn / Hoàn tiền', count: orders.filter(o => o.status === 'REFUNDED' || o.status === 'EXPIRED' || o.status === 'CANCELLED').length },
        ].map(tab => {
          const isActive = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              role="tab"
              aria-selected={isActive}
              onClick={() => setActiveTab(tab.id as typeof activeTab)}
              className={`btn btn-sm btn-touch-target rounded-pill px-3 fw-semibold flex-shrink-0 transition-all ${
                isActive ? 'btn-tr-primary' : 'btn-tr-secondary'
              }`}
            >
              <span>{tab.label}</span>
              <span className="badge rounded-pill bg-surface text-main ms-1 tabular-nums">
                {tab.count}
              </span>
            </button>
          );
        })}
      </div>

      {error && <ErrorState message={error} onRetry={loadOrders} />}

      {loading ? (
        <div className="row g-4">
          <div className="col-12 col-lg-5">
            {[1, 2, 3].map(n => (
              <div key={n} className="tr-card p-3 mb-3">
                <div className="tr-skeleton w-50 mb-2" style={{ height: 18 }} />
                <div className="tr-skeleton w-75 mb-3" style={{ height: 14 }} />
                <div className="tr-skeleton w-25" style={{ height: 20 }} />
              </div>
            ))}
          </div>
          <div className="col-12 col-lg-7">
            <div className="tr-skeleton w-100 rounded-3" style={{ height: 360 }} />
          </div>
        </div>
      ) : filteredOrders.length === 0 ? (
        <EmptyState
          title="Chưa có đơn hàng nào"
          description="Bạn chưa có đơn đặt vé nào trong mục này. Hãy khám phá ngay các sự kiện âm nhạc hấp dẫn!"
          actionText="Khám phá sự kiện"
          onAction={() => { window.location.href = '/events'; }}
          icon={<ShoppingBag size={36} />}
        />
      ) : (
        <div className="row g-4">
          {/* Master Order List Column */}
          <div className="col-12 col-lg-5">
            <div className="d-flex flex-column gap-3">
              {filteredOrders.map(order => {
                const isSelected = selectedOrder?.id === order.id;
                return (
                  <div
                    key={order.id}
                    onClick={() => setSelectedOrder(order)}
                    className={`tr-card p-3 p-md-4 transition-all ${
                      isSelected ? 'border-primary shadow-md' : ''
                    }`}
                    style={{
                      cursor: 'pointer',
                      backgroundColor: isSelected ? 'var(--color-primary-subtle)' : 'var(--bg-surface)',
                    }}
                  >
                    <div className="d-flex justify-content-between align-items-center mb-2">
                      <span className="font-monospace fw-bold text-main small">{order.code}</span>
                      <StatusBadge type="order" status={order.status} />
                    </div>
                    <h3 className="fs-6 fw-bold text-main mb-1 text-truncate">{order.event.name}</h3>
                    <div className="small text-muted mb-3 d-flex align-items-center gap-1 tabular-nums">
                      <Calendar size={14} className="text-primary" />
                      <span>{formatDateTimeVN(order.event.startTime)}</span>
                    </div>

                    <div className="d-flex justify-content-between align-items-center pt-2 border-top border-subtle">
                      <span className="small text-muted tabular-nums">
                        {order.items.reduce((s, i) => s + i.quantity, 0)} vé đã chọn
                      </span>
                      <span className="fw-bold text-primary tabular-nums fs-6">
                        {formatVND(order.totalAmount)}
                      </span>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Detail & Saga Timeline Column */}
          <div className="col-12 col-lg-7">
            {selectedOrder && (
              <div className="tr-card p-4 sticky-top" style={{ top: '84px' }}>
                <div className="d-flex justify-content-between align-items-start mb-4 pb-3 border-bottom border-subtle">
                  <div>
                    <span className="small text-muted d-block">Mã tra cứu đơn hàng</span>
                    <h2 className="fs-4 fw-bold text-main font-monospace mb-0">{selectedOrder.code}</h2>
                  </div>
                  <StatusBadge type="order" status={selectedOrder.status} />
                </div>

                {/* Visual Saga Timeline */}
                <OrderSagaTimeline order={selectedOrder} />

                {/* Event info & Customer */}
                <div className="p-3 rounded-3 bg-surface-subtle border border-subtle mb-3">
                  <div className="fw-bold text-main mb-1">{selectedOrder.event.name}</div>
                  <div className="small text-muted mb-2 d-flex align-items-center gap-1">
                    <MapPin size={14} className="text-primary" />
                    <span>{selectedOrder.event.venueAddress}</span>
                  </div>
                  <div className="small text-muted">
                    Người nhận vé: <strong className="text-main">{selectedOrder.customerName}</strong>
                  </div>
                </div>

                {/* Items */}
                <div className="mb-4">
                  <div className="fw-semibold small mb-2 text-muted">Chi tiết hạng vé</div>
                  {selectedOrder.items.map(item => (
                    <div key={item.ticketTypeId} className="d-flex justify-content-between py-2 border-bottom border-subtle small">
                      <span className="text-main fw-medium">{item.ticketTypeName} × {item.quantity}</span>
                      <span className="fw-semibold text-main tabular-nums">{formatVND(item.unitPrice * item.quantity)}</span>
                    </div>
                  ))}
                  <div className="d-flex justify-content-between align-items-center pt-3">
                    <span className="fw-bold text-main">Tổng thanh toán</span>
                    <span className="fw-bold text-primary tabular-nums fs-4">{formatVND(selectedOrder.totalAmount)}</span>
                  </div>
                </div>

                {/* Action buttons */}
                {selectedOrder.status === 'AWAITING_PAYMENT' && (
                  <div className="d-flex flex-wrap gap-2">
                    <button
                      onClick={() => handleCancelOrder(selectedOrder.code)}
                      className="btn btn-tr-secondary text-danger"
                    >
                      Huỷ giữ chỗ
                    </button>
                    <a
                      href={selectedOrder.paymentUrl ?? `/order-result?orderCode=${encodeURIComponent(selectedOrder.code)}`}
                      className="btn btn-tr-primary flex-grow-1 text-decoration-none"
                    >
                      <span>Tiếp tục thanh toán ngay</span>
                      <ArrowRight size={16} />
                    </a>
                  </div>
                )}

                {selectedOrder.status === 'CONFIRMED' && (
                  <a href="/wallet" className="btn btn-tr-primary w-100 text-decoration-none">
                    <span>Xem vé trong Ví vé điện tử</span>
                    <ArrowRight size={16} />
                  </a>
                )}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
