import React, { useCallback, useEffect, useState } from 'react';
import { Search, Eye } from 'lucide-react';
import { AdminApi, type AdminOrder } from '../../api/adminApi';
import type { ApiErrorProblem, OrderStatus } from '../../types/models';
import { StatusBadge } from '../../components/StatusBadge';
import { ErrorState } from '../../components/DataStates';
import { formatVND, formatDateTimeVN } from '../../lib/format';
import { useAdminEvents } from './useAdminEvents';

const STATUSES: { value: OrderStatus | ''; label: string }[] = [
  { value: '', label: 'Mọi trạng thái' },
  { value: 'AWAITING_PAYMENT', label: 'Chờ thanh toán' },
  { value: 'CONFIRMED', label: 'Đã xác nhận' },
  { value: 'EXPIRED', label: 'Hết hạn' },
  { value: 'FAILED', label: 'Thất bại' },
  { value: 'CANCELLED', label: 'Đã huỷ' },
  { value: 'REFUNDED', label: 'Đã hoàn tiền' },
];

const TICKET_STATUS: Record<string, string> = { VALID: 'Chưa dùng', USED: 'Đã vào cổng', CANCELLED: 'Đã huỷ' };

export const AdminOrdersPage: React.FC = () => {
  const { eventName, ticketTypeName } = useAdminEvents();
  const [orders, setOrders] = useState<AdminOrder[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<ApiErrorProblem | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [status, setStatus] = useState<OrderStatus | ''>('');
  const [selected, setSelected] = useState<AdminOrder | null>(null);

  const load = useCallback(async (q: string, s: OrderStatus | '') => {
    try {
      setLoading(true);
      setError(null);
      setOrders(await AdminApi.orders(q, s || undefined));
    } catch (e: unknown) {
      setError(e as ApiErrorProblem);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const timer = setTimeout(() => load(searchTerm, status), 300);
    return () => clearTimeout(timer);
  }, [searchTerm, status, load]);

  const openDetail = async (code: string) => {
    try {
      setSelected(await AdminApi.order(code));
    } catch (e: unknown) {
      window.alert((e as ApiErrorProblem).detail);
    }
  };

  return (
    <div>
      <div className="mb-4">
        <h1 className="fs-3 fw-bold mb-1">Quản lý Đơn hàng &amp; Giao dịch vé</h1>
        <p className="text-muted small mb-0">Mọi đơn hàng của mọi người mua, mới nhất trước (tối đa 100 đơn mỗi lần lọc)</p>
      </div>

      <div className="tr-card p-3 mb-4 d-flex flex-column flex-sm-row gap-2">
        <div className="input-group" style={{ maxWidth: 400 }}>
          <span className="input-group-text bg-surface-subtle border-end-0">
            <Search size={16} className="text-muted" />
          </span>
          <input
            type="search"
            className="form-control border-start-0"
            placeholder="Mã đơn hoặc tên đăng nhập người mua..."
            aria-label="Tìm đơn hàng"
            value={searchTerm}
            onChange={e => setSearchTerm(e.target.value)}
          />
        </div>
        <select className="form-select" style={{ maxWidth: 220 }} aria-label="Lọc theo trạng thái" value={status} onChange={e => setStatus(e.target.value as OrderStatus | '')}>
          {STATUSES.map(s => (
            <option key={s.value} value={s.value}>{s.label}</option>
          ))}
        </select>
      </div>

      {error && <ErrorState title={error.title} message={error.detail} status={error.status} onRetry={() => load(searchTerm, status)} />}

      <div className="tr-card overflow-hidden mb-4">
        <div className="table-responsive">
          <table className="table table-hover align-middle mb-0">
            <thead className="table-light">
              <tr className="small text-muted">
                <th>MÃ ĐƠN</th>
                <th>SỰ KIỆN</th>
                <th>NGƯỜI MUA</th>
                <th>TỔNG TIỀN</th>
                <th>TRẠNG THÁI</th>
                <th>THỜI GIAN</th>
                <th className="text-end">CHI TIẾT</th>
              </tr>
            </thead>
            <tbody>
              {!loading && orders.length === 0 && (
                <tr>
                  <td colSpan={7} className="text-center text-muted py-4">Không có đơn hàng phù hợp.</td>
                </tr>
              )}
              {orders.map(order => (
                <tr key={order.code}>
                  <td className="font-monospace fw-bold text-main">{order.code}</td>
                  <td className="small">{order.items[0] ? eventName(order.items[0].eventId) : '—'}</td>
                  <td className="text-main fw-medium">{order.userLogin}</td>
                  <td className="fw-bold text-primary tabular-nums">{formatVND(order.totalAmount)}</td>
                  <td><StatusBadge type="order" status={order.status} /></td>
                  <td className="small text-muted">{formatDateTimeVN(order.createdAt)}</td>
                  <td className="text-end">
                    <button className="btn btn-sm btn-tr-ghost p-1" aria-label={`Xem đơn ${order.code}`} onClick={() => openDetail(order.code)}>
                      <Eye size={16} />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {selected && (
        <div
          className="position-fixed top-0 start-0 w-100 h-100 bg-dark bg-opacity-75 z-3 d-flex align-items-center justify-content-center p-3"
          onClick={() => setSelected(null)}
        >
          <div
            role="dialog"
            aria-modal="true"
            aria-label={`Đơn hàng ${selected.code}`}
            className="tr-card p-4 p-md-5 bg-surface shadow-lg"
            style={{ maxWidth: 600, width: '100%', maxHeight: '90vh', overflowY: 'auto' }}
            onClick={e => e.stopPropagation()}
          >
            <div className="d-flex justify-content-between align-items-center mb-3 pb-2 border-bottom border-subtle">
              <span className="font-monospace fw-bold fs-5 text-main">{selected.code}</span>
              <StatusBadge type="order" status={selected.status} />
            </div>

            <div className="row g-2 small mb-3">
              <div className="col-6">
                <span className="text-muted">Người mua:</span>
                <div className="fw-medium text-main">{selected.userLogin}</div>
              </div>
              <div className="col-6">
                <span className="text-muted">Đặt lúc:</span>
                <div className="text-main">{formatDateTimeVN(selected.createdAt)}</div>
              </div>
              {selected.failureReason && (
                <div className="col-12">
                  <span className="text-muted">Lý do:</span> <span className="font-monospace text-danger">{selected.failureReason}</span>
                </div>
              )}
            </div>

            <div className="p-3 bg-surface-subtle rounded-3 border border-subtle mb-3">
              <div className="fw-semibold small mb-2">{selected.items[0] ? eventName(selected.items[0].eventId) : ''}</div>
              {selected.items.map(item => (
                <div key={item.ticketTypeId} className="d-flex justify-content-between small py-1">
                  <span>{ticketTypeName(item.ticketTypeId)} × {item.quantity}</span>
                  <span className="fw-semibold tabular-nums">{formatVND(item.unitPrice * item.quantity)}</span>
                </div>
              ))}
              <div className="d-flex justify-content-between pt-2 border-top border-subtle mt-2">
                <span className="fw-bold">Tổng thanh toán:</span>
                <span className="fw-bold text-primary tabular-nums fs-5">{formatVND(selected.totalAmount)}</span>
              </div>
            </div>

            {selected.tickets.length > 0 && (
              <div className="mb-4">
                <div className="fw-semibold small mb-2">Vé điện tử ({selected.tickets.length})</div>
                {selected.tickets.map(ticket => (
                  <div key={ticket.code} className="d-flex justify-content-between small py-1 border-bottom border-subtle">
                    <span className="font-monospace">{ticket.code}</span>
                    <span>{ticketTypeName(ticket.ticketTypeId)} {ticket.seatNo}/{ticket.seatsOfType}</span>
                    <span className={ticket.status === 'USED' ? 'text-success' : 'text-muted'}>
                      {TICKET_STATUS[ticket.status]}
                      {ticket.checkedInAt ? ` (${formatDateTimeVN(ticket.checkedInAt)})` : ''}
                    </span>
                  </div>
                ))}
              </div>
            )}

            <button className="btn btn-tr-secondary w-100" onClick={() => setSelected(null)}>Đóng</button>
          </div>
        </div>
      )}
    </div>
  );
};
