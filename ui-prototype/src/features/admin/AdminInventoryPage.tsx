import React, { useEffect, useState } from 'react';
import { Clock } from 'lucide-react';
import { AdminApi, type ActiveHold } from '../../api/adminApi';
import { formatVND } from '../../lib/format';
import { ErrorState } from '../../components/DataStates';
import { useAdminEvents } from './useAdminEvents';

const REFRESH_MS = 5000;

const pct = (part: number, total: number) => (total > 0 ? Math.round((part / total) * 100) : 0);

function remaining(expiresAt: string, now: number): string {
  const seconds = Math.max(0, Math.floor((new Date(expiresAt).getTime() - now) / 1000));
  return `${String(Math.floor(seconds / 60)).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}`;
}

export const AdminInventoryPage: React.FC = () => {
  const { events, loading, error, reload, eventName, ticketTypeName } = useAdminEvents(REFRESH_MS);
  const [holds, setHolds] = useState<ActiveHold[]>([]);
  const [now, setNow] = useState(() => Date.now());

  useEffect(() => {
    const loadHolds = () => AdminApi.expiringHolds(20).then(setHolds).catch(() => setHolds([]));
    loadHolds();
    const poll = setInterval(loadHolds, REFRESH_MS);
    const tick = setInterval(() => setNow(Date.now()), 1000);
    return () => {
      clearInterval(poll);
      clearInterval(tick);
    };
  }, []);

  const onSale = events.filter(event => event.status === 'PUBLISHED' || event.status === 'DRAFT');

  return (
    <div>
      <div className="mb-4">
        <h1 className="fs-3 fw-bold mb-1">Giám sát kho vé &amp; Phiên giữ chỗ</h1>
        <p className="text-muted small mb-0">
          Đã bán / Đang giữ / Còn lại theo từng loại vé, tự làm mới mỗi {REFRESH_MS / 1000} giây. Mỗi loại vé luôn có còn lại + đang giữ + đã bán = tổng.
        </p>
      </div>

      {error && <ErrorState title={error.title} message={error.detail} status={error.status} onRetry={reload} />}
      {loading && <div className="tr-skeleton w-100 rounded-3 mb-4" style={{ height: 200 }} />}
      {!loading && onSale.length === 0 && <div className="tr-card p-4 text-muted mb-4">Chưa có sự kiện đang bán.</div>}

      <div className="row g-4 mb-4">
        {onSale.map(event => (
          <div key={event.id} className="col-12">
            <div className="tr-card p-4">
              <div className="d-flex justify-content-between align-items-center mb-3 pb-2 border-bottom border-subtle">
                <div>
                  <h2 className="fs-5 fw-bold text-main mb-0">{event.name}</h2>
                  <span className="small text-muted">{event.venue.name} • {event.venue.city}</span>
                </div>
                <span className="badge bg-secondary-subtle text-secondary">{event.status === 'DRAFT' ? 'Nháp' : 'Đang bán'}</span>
              </div>

              <div className="d-flex flex-column gap-3">
                {event.ticketTypes.map(tt => {
                  const level = event.stock[tt.id];
                  if (!level) {
                    return (
                      <div key={tt.id} className="p-3 bg-surface-subtle rounded-3 border border-subtle small text-muted">
                        {tt.name}: kho đang đồng bộ từ catalog...
                      </div>
                    );
                  }
                  const total = level.totalQuantity;
                  const soldPct = pct(level.soldQuantity, total);
                  const heldPct = pct(level.heldQuantity, total);
                  const availablePct = Math.max(0, 100 - soldPct - heldPct);
                  return (
                    <div key={tt.id} className="p-3 bg-surface-subtle rounded-3 border border-subtle">
                      <div className="d-flex justify-content-between align-items-center mb-2">
                        <div>
                          <span className="fw-bold text-main">{tt.name}</span>
                          <span className="text-muted small ms-2">({formatVND(tt.price)})</span>
                        </div>
                        <div className="small text-muted">Tổng cung: <strong>{total} vé</strong></div>
                      </div>
                      <div
                        className="progress mb-2"
                        style={{ height: 16 }}
                        role="img"
                        aria-label={`Đã bán ${level.soldQuantity}, đang giữ ${level.heldQuantity}, còn lại ${level.availableQuantity} trên ${total} vé`}
                      >
                        <div className="progress-bar bg-success" style={{ width: `${soldPct}%` }} />
                        <div className="progress-bar bg-warning" style={{ width: `${heldPct}%` }} />
                        <div className="progress-bar bg-secondary bg-opacity-25" style={{ width: `${availablePct}%` }} />
                      </div>
                      <div className="d-flex flex-wrap justify-content-between gap-2 small text-muted">
                        <span className="d-flex align-items-center gap-1">
                          <span className="rounded-circle d-inline-block bg-success" style={{ width: 8, height: 8 }} />
                          Đã thanh toán: <strong className="tabular-nums">{level.soldQuantity} ({soldPct}%)</strong>
                        </span>
                        <span className="d-flex align-items-center gap-1">
                          <span className="rounded-circle d-inline-block bg-warning" style={{ width: 8, height: 8 }} />
                          Đang giữ chỗ (10p): <strong className="tabular-nums">{level.heldQuantity} ({heldPct}%)</strong>
                        </span>
                        <span className="d-flex align-items-center gap-1">
                          <span className="rounded-circle d-inline-block bg-secondary" style={{ width: 8, height: 8 }} />
                          Còn lại: <strong className="tabular-nums">{level.availableQuantity} ({availablePct}%)</strong>
                        </span>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        ))}
      </div>

      <div className="tr-card p-4">
        <div className="d-flex align-items-center gap-2 mb-3">
          <Clock size={20} className="text-warning" />
          <h2 className="fs-5 fw-bold mb-0">Phiên giữ chỗ sắp hết hạn</h2>
        </div>
        <div className="table-responsive">
          <table className="table table-hover align-middle mb-0">
            <thead className="table-light">
              <tr className="small text-muted">
                <th>ĐƠN (ID)</th>
                <th>SỰ KIỆN</th>
                <th>SỐ LƯỢNG</th>
                <th>CÒN LẠI</th>
              </tr>
            </thead>
            <tbody>
              {holds.length === 0 && (
                <tr>
                  <td colSpan={4} className="text-center text-muted py-3 small">Không có ghế nào đang được giữ.</td>
                </tr>
              )}
              {holds.map(hold => {
                const left = new Date(hold.expiresAt).getTime() - now;
                return (
                  <tr key={hold.holdId}>
                    <td className="font-monospace fw-bold text-main">#{hold.orderId}</td>
                    <td>{eventName(hold.eventId)}</td>
                    <td>{hold.quantity} vé {ticketTypeName(hold.ticketTypeId)}</td>
                    <td>
                      <span className={`badge font-monospace ${left < 120000 ? 'bg-danger text-white' : 'bg-warning text-dark'}`}>
                        {left > 0 ? remaining(hold.expiresAt, now) : 'Đang trả về kho'}
                      </span>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
