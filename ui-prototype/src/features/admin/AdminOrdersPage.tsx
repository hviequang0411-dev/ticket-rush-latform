import React, { useState } from 'react';
import { Search, Eye } from 'lucide-react';
import { mockOrders } from '../../mocks/mockData';
import type { OrderSummary } from '../../types/models';
import { StatusBadge } from '../../components/StatusBadge';
import { formatVND, formatDateTimeVN } from '../../lib/format';

export const AdminOrdersPage: React.FC = () => {
  const [orders] = useState<OrderSummary[]>(mockOrders);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedOrder, setSelectedOrder] = useState<OrderSummary | null>(null);

  const filtered = orders.filter(o =>
    o.code.toLowerCase().includes(searchTerm.toLowerCase()) ||
    o.customerName.toLowerCase().includes(searchTerm.toLowerCase()) ||
    o.event.name.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div>
      <div className="mb-4">
        <h1 className="fs-3 fw-bold mb-1">Quản lý Đơn hàng &amp; Giao dịch vé</h1>
        <p className="text-muted small mb-0">Tra cứu chi tiết mọi đơn hàng được khởi tạo từ hệ thống bán vé</p>
      </div>

      <div className="tr-card p-3 mb-4">
        <div className="input-group" style={{ maxWidth: 400 }}>
          <span className="input-group-text bg-surface-subtle border-end-0">
            <Search size={16} className="text-muted" />
          </span>
          <input
            type="text"
            className="form-control border-start-0"
            placeholder="Tìm theo mã đơn, khách hàng..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
      </div>

      <div className="tr-card overflow-hidden mb-4">
        <div className="table-responsive">
          <table className="table table-hover align-middle mb-0">
            <thead className="table-light">
              <tr className="small text-muted">
                <th>MÃ ĐƠN</th>
                <th>SỰ KIỆN</th>
                <th>KHÁCH HÀNG</th>
                <th>TỔNG TIỀN</th>
                <th>TRẠNG THÁI</th>
                <th>THỜI GIAN</th>
                <th className="text-end">CHI TIẾT</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map(order => (
                <tr key={order.id}>
                  <td className="font-monospace fw-bold text-main">{order.code}</td>
                  <td>
                    <div className="fw-semibold text-main text-truncate" style={{ maxWidth: 220 }}>
                      {order.event.name}
                    </div>
                  </td>
                  <td>
                    <div className="text-main fw-medium">{order.customerName}</div>
                    <div className="small text-muted">{order.customerPhone}</div>
                  </td>
                  <td className="fw-bold text-primary tabular-nums">{formatVND(order.totalAmount)}</td>
                  <td><StatusBadge type="order" status={order.status} /></td>
                  <td className="small text-muted">{formatDateTimeVN(order.createdAt)}</td>
                  <td className="text-end">
                    <button
                      className="btn btn-sm btn-tr-secondary d-inline-flex align-items-center gap-1"
                      onClick={() => setSelectedOrder(order)}
                    >
                      <Eye size={14} /> Xem
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Detail Modal */}
      {selectedOrder && (
        <div
          className="position-fixed top-0 start-0 w-100 h-100 bg-dark bg-opacity-75 z-3 d-flex align-items-center justify-content-center p-3"
          onClick={() => setSelectedOrder(null)}
        >
          <div
            className="tr-card p-4 p-md-5 bg-surface shadow-lg"
            style={{ maxWidth: 540, width: '100%' }}
            onClick={(e) => e.stopPropagation()}
          >
            <div className="d-flex justify-content-between align-items-center mb-3 pb-2 border-bottom border-subtle">
              <span className="font-monospace fw-bold fs-5 text-main">{selectedOrder.code}</span>
              <StatusBadge type="order" status={selectedOrder.status} />
            </div>

            <div className="mb-3">
              <span className="small text-muted">Sự kiện:</span>
              <div className="fw-bold text-main">{selectedOrder.event.name}</div>
              <div className="small text-muted">{selectedOrder.event.venueAddress}</div>
            </div>

            <div className="mb-3">
              <span className="small text-muted">Khách hàng nhận vé:</span>
              <div className="fw-medium text-main">{selectedOrder.customerName}</div>
              <div className="small text-muted">{selectedOrder.customerEmail} • {selectedOrder.customerPhone}</div>
            </div>

            <div className="p-3 bg-surface-subtle rounded-3 border border-subtle mb-4">
              <div className="fw-semibold small mb-2">Hạng vé đã đặt:</div>
              {selectedOrder.items.map(item => (
                <div key={item.ticketTypeId} className="d-flex justify-content-between small py-1">
                  <span>{item.ticketTypeName} × {item.quantity}</span>
                  <span className="fw-semibold tabular-nums">{formatVND(item.unitPrice * item.quantity)}</span>
                </div>
              ))}
              <div className="d-flex justify-content-between pt-2 border-top border-subtle mt-2">
                <span className="fw-bold">Tổng thanh toán:</span>
                <span className="fw-bold text-primary tabular-nums fs-5">{formatVND(selectedOrder.totalAmount)}</span>
              </div>
            </div>

            <button className="btn btn-tr-secondary w-100" onClick={() => setSelectedOrder(null)}>
              Đóng
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

