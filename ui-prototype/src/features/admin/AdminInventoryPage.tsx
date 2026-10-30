import React from 'react';
import { Clock } from 'lucide-react';
import { mockEvents } from '../../mocks/mockData';
import { formatVND } from '../../lib/format';

export const AdminInventoryPage: React.FC = () => {
  return (
    <div>
      <div className="mb-4">
        <h1 className="fs-3 fw-bold mb-1">Giám sát kho vé &amp; Phiên giữ chỗ</h1>
        <p className="text-muted small mb-0">Theo dõi phân bổ Đã bán / Đang giữ / Còn lại theo từng loại vé thời gian thực</p>
      </div>

      <div className="row g-4 mb-4">
        {mockEvents.slice(0, 4).map(event => (
          <div key={event.id} className="col-12">
            <div className="tr-card p-4">
              <div className="d-flex justify-content-between align-items-center mb-3 pb-2 border-bottom border-subtle">
                <div>
                  <h3 className="fs-5 fw-bold text-main mb-0">{event.name}</h3>
                  <span className="small text-muted">{event.venue.name} • {event.venue.city}</span>
                </div>
                <span className="badge bg-secondary-subtle text-secondary">{event.category}</span>
              </div>

              {/* Stacked Progress Bars for Each Ticket Type */}
              <div className="d-flex flex-column gap-3">
                {event.ticketTypes.map(tt => {
                  const total = tt.totalQuantity;
                  const available = tt.availableQuantity;
                  // Simulate reserved ~ 5-10% if available
                  const reserved = available > 0 ? Math.min(available, Math.floor(total * 0.08)) : 0;
                  const sold = total - available;

                  const soldPct = Math.round((sold / total) * 100);
                  const reservedPct = Math.round((reserved / total) * 100);
                  const availablePct = 100 - soldPct - reservedPct;

                  return (
                    <div key={tt.id} className="p-3 bg-surface-subtle rounded-3 border border-subtle">
                      <div className="d-flex justify-content-between align-items-center mb-2">
                        <div>
                          <span className="fw-bold text-main">{tt.name}</span>
                          <span className="text-muted small ms-2">({formatVND(tt.price)})</span>
                        </div>
                        <div className="small text-muted">
                          Tổng cung: <strong>{total} vé</strong>
                        </div>
                      </div>

                      {/* Stacked Progress Bar: Sold (Green) | Reserved (Orange) | Available (Gray) */}
                      <div className="progress mb-2" style={{ height: 16 }}>
                        <div
                          className="progress-bar bg-success"
                          style={{ width: `${soldPct}%` }}
                          title={`Đã bán: ${sold} vé (${soldPct}%)`}
                        />
                        <div
                          className="progress-bar bg-warning"
                          style={{ width: `${reservedPct}%` }}
                          title={`Đang giữ: ${reserved} vé (${reservedPct}%)`}
                        />
                        <div
                          className="progress-bar bg-secondary bg-opacity-25"
                          style={{ width: `${availablePct}%` }}
                          title={`Còn lại: ${available} vé (${availablePct}%)`}
                        />
                      </div>

                      <div className="d-flex justify-content-between small text-muted">
                        <span className="d-flex align-items-center gap-1">
                          <span className="rounded-circle d-inline-block bg-success" style={{ width: 8, height: 8 }} />
                          Đã thanh toán: <strong>{sold} ({soldPct}%)</strong>
                        </span>
                        <span className="d-flex align-items-center gap-1">
                          <span className="rounded-circle d-inline-block bg-warning" style={{ width: 8, height: 8 }} />
                          Đang giữ chỗ (10p): <strong>{reserved} ({reservedPct}%)</strong>
                        </span>
                        <span className="d-flex align-items-center gap-1">
                          <span className="rounded-circle d-inline-block bg-secondary" style={{ width: 8, height: 8 }} />
                          Còn lại trên sàn: <strong>{available} ({availablePct}%)</strong>
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

      {/* Expiring Holds Table */}
      <div className="tr-card p-4">
        <div className="d-flex align-items-center gap-2 mb-3">
          <Clock size={20} className="text-warning" />
          <h2 className="fs-5 fw-bold mb-0">Các phiên giữ chỗ sắp hết hạn (Active Hold TTL)</h2>
        </div>

        <div className="table-responsive">
          <table className="table table-hover align-middle mb-0">
            <thead className="table-light">
              <tr className="small text-muted">
                <th>MÃ ĐƠN HÀNG</th>
                <th>SỰ KIỆN</th>
                <th>SỐ LƯỢNG</th>
                <th>THỜI GIAN CÒN LẠI</th>
                <th>TRẠNG THÁI INVENTORY</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td className="font-monospace fw-bold text-main">TR-ORD-88232</td>
                <td>Neon Tide Festival 2026</td>
                <td>2 vé GA</td>
                <td><span className="badge bg-warning text-dark font-monospace">05:42</span></td>
                <td><span className="badge bg-warning-subtle text-warning">LOCKED_IN_REDIS</span></td>
              </tr>
              <tr>
                <td className="font-monospace fw-bold text-main">TR-ORD-88240</td>
                <td>Đêm Nhạc Mùa Thu Hà Nội</td>
                <td>1 vé VIP Thính Phòng</td>
                <td><span className="badge bg-danger text-white font-monospace">01:14 (Sắp giải phóng)</span></td>
                <td><span className="badge bg-warning-subtle text-warning">LOCKED_IN_REDIS</span></td>
              </tr>
              <tr>
                <td className="font-monospace fw-bold text-main">TR-ORD-88241</td>
                <td>Kịch Nghệ Sài Gòn</td>
                <td>2 vé Standard</td>
                <td><span className="badge bg-warning text-dark font-monospace">08:19</span></td>
                <td><span className="badge bg-warning-subtle text-warning">LOCKED_IN_REDIS</span></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
