import React, { useState, useEffect } from 'react';
import { ShoppingBag, CheckCircle2, Clock, RotateCcw } from 'lucide-react';
import { ApiService } from '../../api/apiClient';
import type { OrderSummary, ApiErrorProblem } from '../../types/models';
import { signIn } from '../../api/auth';
import { StatusBadge } from '../../components/StatusBadge';
import { formatVND, formatDateTimeVN } from '../../lib/format';
import { EmptyState, ErrorState } from '../../components/DataStates';

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
      <div className="d-flex align-items-center justify-content-between mb-4">
        <div>
          <h1 className="fs-3 fw-bold mb-1">Đơn hàng của tôi</h1>
          <p className="text-muted small mb-0">Theo dõi tiến trình giữ chỗ, thanh toán và lịch sử các vé đã mua</p>
        </div>
      </div>

      {/* Tabs */}
      <div className="d-flex gap-2 border-bottom border-subtle mb-4 pb-1 overflow-auto">
        {[
          { id: 'ALL', label: 'Tất cả đơn' },
          { id: 'CONFIRMED', label: 'Đã xác nhận' },
          { id: 'AWAITING_PAYMENT', label: 'Chờ thanh toán' },
          { id: 'COMPLETED_REFUNDED', label: 'Hết hạn / Hoàn tiền' }
        ].map(tab => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id as typeof activeTab)}
            className={`btn btn-sm btn-touch-target border-0 pb-2 px-3 fw-semibold rounded-0 transition-all ${
              activeTab === tab.id
                ? 'border-bottom border-primary text-primary border-3'
                : 'text-muted'
            }`}
          >
            {tab.label}
          </button>
        ))}
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
          {/* Order list column */}
          <div className="col-12 col-lg-5">
            <div className="d-flex flex-column gap-3">
              {filteredOrders.map(order => (
                <div
                  key={order.id}
                  onClick={() => setSelectedOrder(order)}
                  className={`tr-card p-3 cursor-pointer transition-all ${
                    selectedOrder?.id === order.id ? 'border-primary shadow-sm bg-surface' : 'bg-surface'
                  }`}
                  style={{ cursor: 'pointer' }}
                >
                  <div className="d-flex justify-content-between align-items-center mb-2">
                    <span className="font-monospace fw-bold text-main small">{order.code}</span>
                    <StatusBadge type="order" status={order.status} />
                  </div>
                  <h4 className="fs-6 fw-bold text-main mb-1 text-truncate">{order.event.name}</h4>
                  <div className="small text-muted mb-2">{formatDateTimeVN(order.event.startTime)}</div>

                  <div className="d-flex justify-content-between align-items-center pt-2 border-top border-subtle">
                    <span className="small text-muted">{order.items.reduce((s, i) => s + i.quantity, 0)} vé</span>
                    <span className="fw-bold text-primary tabular-nums">{formatVND(order.totalAmount)}</span>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Order Detail & Timeline column */}
          <div className="col-12 col-lg-7">
            {selectedOrder && (
              <div className="tr-card p-4 shadow-sm bg-surface sticky-top" style={{ top: '88px' }}>
                <div className="d-flex justify-content-between align-items-start mb-3 pb-3 border-bottom border-subtle">
                  <div>
                    <span className="small text-muted">Chi tiết đơn hàng:</span>
                    <h3 className="fs-5 fw-bold text-main font-monospace mb-0">{selectedOrder.code}</h3>
                  </div>
                  <StatusBadge type="order" status={selectedOrder.status} />
                </div>

                {/* Timeline progress steps */}
                <h4 className="fs-6 fw-bold mb-3">Tiến trình xử lý đơn hàng (Saga Timeline)</h4>
                <div className="position-relative ps-4 mb-4 border-start border-2 border-primary-subtle ms-2">
                  {/* Step 1: Tạo đơn */}
                  <div className="position-relative mb-3">
                    <div className="position-absolute top-0 start-0 translate-middle rounded-circle bg-success text-white p-1 d-flex align-items-center justify-content-center" style={{ width: 22, height: 22, left: '-17px' }}>
                      <CheckCircle2 size={14} />
                    </div>
                    <div className="fw-semibold text-main small">1. Đã tiếp nhận yêu cầu đặt vé</div>
                    <div className="text-muted" style={{ fontSize: '0.75rem' }}>{formatDateTimeVN(selectedOrder.createdAt)}</div>
                  </div>

                  {/* Step 2: Giữ ghế */}
                  <div className="position-relative mb-3">
                    <div className="position-absolute top-0 start-0 translate-middle rounded-circle bg-success text-white p-1 d-flex align-items-center justify-content-center" style={{ width: 22, height: 22, left: '-17px' }}>
                      <CheckCircle2 size={14} />
                    </div>
                    <div className="fw-semibold text-main small">2. Kho vé khoá giữ ghế 10 phút</div>
                    <div className="text-muted" style={{ fontSize: '0.75rem' }}>Khoá ghế thành công tại Inventory Service</div>
                  </div>

                  {/* Step 3: Thanh toán */}
                  <div className="position-relative mb-3">
                    <div className={`position-absolute top-0 start-0 translate-middle rounded-circle p-1 d-flex align-items-center justify-content-center text-white ${
                      selectedOrder.status === 'CONFIRMED' ? 'bg-success' : selectedOrder.status === 'AWAITING_PAYMENT' ? 'bg-warning' : 'bg-secondary'
                    }`} style={{ width: 22, height: 22, left: '-17px' }}>
                      {selectedOrder.status === 'CONFIRMED' ? <CheckCircle2 size={14} /> : <Clock size={14} />}
                    </div>
                    <div className="fw-semibold text-main small">3. Trạng thái thanh toán</div>
                    <div className="text-muted" style={{ fontSize: '0.75rem' }}>
                      {selectedOrder.status === 'CONFIRMED'
                        ? 'Thanh toán thành công qua cổng liên kết'
                        : selectedOrder.status === 'AWAITING_PAYMENT'
                        ? 'Đang chờ nhận tín hiệu Webhook từ đối tác thanh toán'
                        : 'Không hoàn tất giao dịch'}
                    </div>
                  </div>

                  {/* Step 4: Nhánh hoàn thành hoặc bù trừ */}
                  <div className="position-relative">
                    <div className={`position-absolute top-0 start-0 translate-middle rounded-circle p-1 d-flex align-items-center justify-content-center text-white ${
                      selectedOrder.status === 'CONFIRMED'
                        ? 'bg-success'
                        : selectedOrder.status === 'REFUNDED'
                        ? 'bg-info'
                        : selectedOrder.status === 'EXPIRED'
                        ? 'bg-secondary'
                        : 'bg-secondary'
                    }`} style={{ width: 22, height: 22, left: '-17px' }}>
                      {selectedOrder.status === 'CONFIRMED' ? <CheckCircle2 size={14} /> : <RotateCcw size={14} />}
                    </div>
                    <div className="fw-semibold text-main small">
                      {selectedOrder.status === 'CONFIRMED'
                        ? '4. Đã xuất vé điện tử'
                        : selectedOrder.status === 'REFUNDED'
                        ? '4. Nhánh bù trừ: Hoàn trả tiền vào tài khoản'
                        : selectedOrder.status === 'EXPIRED'
                        ? '4. Nhánh bù trừ: Hết hạn giữ chỗ, hoàn ghế vào kho'
                        : '4. Kết quả'}
                    </div>
                    {selectedOrder.failureReason && (
                      <div className="text-danger small mt-1">{selectedOrder.failureReason}</div>
                    )}
                  </div>
                </div>

                {/* Event info & Customer */}
                <div className="bg-surface-subtle p-3 rounded-3 border border-subtle mb-3">
                  <div className="fw-bold text-main mb-1">{selectedOrder.event.name}</div>
                  <div className="small text-muted mb-2">{selectedOrder.event.venueAddress}</div>
                  <div className="small text-muted">
                    Người đặt: <strong>{selectedOrder.customerName}</strong>
                  </div>
                </div>

                {/* Items */}
                <div className="mb-4">
                  <div className="fw-semibold small mb-2">Chi tiết loại vé:</div>
                  {selectedOrder.items.map(item => (
                    <div key={item.ticketTypeId} className="d-flex justify-content-between py-1 border-bottom border-subtle small">
                      <span>{item.ticketTypeName} × {item.quantity}</span>
                      <span className="fw-semibold tabular-nums">{formatVND(item.unitPrice * item.quantity)}</span>
                    </div>
                  ))}
                  <div className="d-flex justify-content-between pt-2">
                    <span className="fw-bold">Tổng thanh toán:</span>
                    <span className="fw-bold text-primary tabular-nums fs-5">{formatVND(selectedOrder.totalAmount)}</span>
                  </div>
                </div>

                {/* Action buttons */}
                {selectedOrder.status === 'AWAITING_PAYMENT' && (
                  <div className="d-flex gap-2">
                    <button
                      onClick={() => handleCancelOrder(selectedOrder.code)}
                      className="btn btn-sm btn-outline-danger"
                    >
                      Huỷ giữ chỗ
                    </button>
                    <a
                      href={selectedOrder.paymentUrl ?? `/order-result?orderCode=${encodeURIComponent(selectedOrder.code)}`}
                      className="btn btn-sm btn-tr-primary flex-grow-1 text-center"
                    >
                      Tiếp tục thanh toán ngay
                    </a>
                  </div>
                )}

                {selectedOrder.status === 'CONFIRMED' && (
                  <a href="/wallet" className="btn btn-tr-primary w-100">
                    Xem vé trong Ví vé
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
