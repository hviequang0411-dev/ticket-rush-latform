import React, { useCallback, useEffect, useState } from 'react';
import { CheckCircle2, XCircle, Code2, Repeat, X } from 'lucide-react';
import { AdminApi, type PaymentRow, type WebhookRow } from '../../api/adminApi';
import type { ApiErrorProblem } from '../../types/models';
import { StatusBadge } from '../../components/StatusBadge';
import { ErrorState } from '../../components/DataStates';
import { formatVND, formatDateTimeVN } from '../../lib/format';

const OUTCOME: Record<WebhookRow['outcome'], { label: string; className: string }> = {
  PROCESSED: { label: 'Đã xử lý', className: 'badge-success' },
  REJECTED: { label: 'Từ chối (sai chữ ký)', className: 'badge-danger' },
  UNMATCHED: { label: 'Không khớp giao dịch', className: 'badge-warning' },
  PENDING: { label: 'Đang xử lý', className: 'badge-neutral' },
};

/** Payloads are form fields (mock provider) or JSON; show JSON nicely when it is. */
function pretty(payload: string): string {
  try {
    return JSON.stringify(JSON.parse(payload), null, 2);
  } catch {
    return payload;
  }
}

export const AdminPaymentsPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'PAYMENTS' | 'WEBHOOKS'>('WEBHOOKS');
  const [webhooks, setWebhooks] = useState<WebhookRow[]>([]);
  const [payments, setPayments] = useState<PaymentRow[]>([]);
  const [error, setError] = useState<ApiErrorProblem | null>(null);
  const [selected, setSelected] = useState<WebhookRow | null>(null);

  const load = useCallback(async () => {
    try {
      const [hooks, pays] = await Promise.all([AdminApi.webhooks(), AdminApi.payments()]);
      setWebhooks(hooks);
      setPayments(pays);
      setError(null);
    } catch (e: unknown) {
      setError(e as ApiErrorProblem);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const retries = webhooks.reduce((sum, wh) => sum + wh.deliveries - 1, 0);

  return (
    <div>
      <div className="mb-4">
        <h1 className="fs-3 fw-bold mb-1">Giao dịch thanh toán &amp; Webhook đối tác</h1>
        <p className="text-muted small mb-0">
          Mỗi callback chỉ được áp dụng một lần dù cổng gửi lại bao nhiêu lần: <strong className="text-main tabular-nums">{retries}</strong> lần gửi lại đã bị bỏ qua an toàn.
        </p>
      </div>

      <div className="d-flex gap-2 border-bottom border-subtle mb-4 pb-2" role="tablist">
        {(['WEBHOOKS', 'PAYMENTS'] as const).map(tab => {
          const isActive = activeTab === tab;
          return (
            <button
              key={tab}
              role="tab"
              aria-selected={isActive}
              onClick={() => setActiveTab(tab)}
              className={`btn btn-sm btn-touch-target rounded-pill px-3 fw-semibold ${
                isActive ? 'btn-tr-primary' : 'btn-tr-secondary'
              }`}
            >
              {tab === 'WEBHOOKS' ? `Nhật ký Webhook (${webhooks.length})` : `Giao dịch thanh toán (${payments.length})`}
            </button>
          );
        })}
      </div>

      {error && <ErrorState title={error.title} message={error.detail} status={error.status} onRetry={load} />}

      {activeTab === 'WEBHOOKS' ? (
        <div className="tr-table-card">
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>CỔNG</th>
                  <th>MÃ ĐƠN</th>
                  <th className="text-end">SỐ TIỀN</th>
                  <th>CHỮ KÝ HMAC</th>
                  <th>KẾT QUẢ</th>
                  <th className="text-end">SỐ LẦN GỬI</th>
                  <th>NHẬN LÚC</th>
                  <th className="text-end">PAYLOAD</th>
                </tr>
              </thead>
              <tbody>
                {webhooks.length === 0 && (
                  <tr>
                    <td colSpan={9} className="text-center text-muted py-4">Chưa nhận callback nào.</td>
                  </tr>
                )}
                {webhooks.map(wh => (
                  <tr key={wh.id}>
                    <td className="font-monospace fw-bold text-main">#{wh.id}</td>
                    <td><span className="tr-badge badge-neutral font-monospace">{wh.provider}</span></td>
                    <td className="font-monospace text-main">{wh.orderCode ?? '—'}</td>
                    <td className="fw-bold text-primary tabular-nums text-end">{wh.amount != null ? formatVND(wh.amount) : '—'}</td>
                    <td>
                      {wh.signatureValid ? (
                        <span className="d-inline-flex align-items-center gap-1 small text-success fw-medium"><CheckCircle2 size={14} /> Hợp lệ</span>
                      ) : (
                        <span className="d-inline-flex align-items-center gap-1 small text-danger fw-medium"><XCircle size={14} /> Sai</span>
                      )}
                    </td>
                    <td><span className={`tr-badge ${OUTCOME[wh.outcome].className}`}>{OUTCOME[wh.outcome].label}</span></td>
                    <td className="tabular-nums text-end">
                      {wh.deliveries > 1 ? (
                        <span className="d-inline-flex align-items-center gap-1 text-warning fw-bold" title="Cổng gửi lại; chỉ lần đầu được áp dụng">
                          <Repeat size={14} /> {wh.deliveries}
                        </span>
                      ) : (
                        1
                      )}
                    </td>
                    <td className="small text-muted tabular-nums">{formatDateTimeVN(wh.receivedAt)}</td>
                    <td className="text-end">
                      <button className="btn btn-sm btn-tr-secondary py-1 px-2 d-inline-flex align-items-center gap-1" onClick={() => setSelected(wh)}>
                        <Code2 size={14} /> Chi tiết
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      ) : (
        <div className="tr-table-card">
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead>
                <tr>
                  <th>MÃ ĐƠN</th>
                  <th>CỔNG</th>
                  <th>MÃ GIAO DỊCH</th>
                  <th className="text-end">SỐ TIỀN</th>
                  <th>TRẠNG THÁI</th>
                  <th>LÝ DO</th>
                  <th>TẠO LÚC</th>
                </tr>
              </thead>
              <tbody>
                {payments.length === 0 && (
                  <tr>
                    <td colSpan={7} className="text-center text-muted py-4">Chưa có giao dịch.</td>
                  </tr>
                )}
                {payments.map(p => (
                  <tr key={p.id}>
                    <td className="font-monospace fw-bold text-main">{p.orderCode}</td>
                    <td><span className="tr-badge badge-neutral font-monospace">{p.provider}</span></td>
                    <td className="font-monospace small">{p.providerRef}</td>
                    <td className="fw-bold text-primary tabular-nums text-end">{formatVND(p.amount)}</td>
                    <td><StatusBadge type="payment" status={p.status} /></td>
                    <td className="small font-monospace text-muted">{p.failureReason ?? ''}</td>
                    <td className="small text-muted tabular-nums">{formatDateTimeVN(p.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {selected && (
        <div
          className="position-fixed top-0 start-0 w-100 h-100 bg-dark bg-opacity-75 d-flex align-items-center justify-content-center p-3"
          style={{ zIndex: 1060, backdropFilter: 'blur(4px)' }}
          onClick={() => setSelected(null)}
        >
          <div
            role="dialog"
            aria-modal="true"
            aria-label={`Webhook ${selected.id}`}
            className="tr-card p-4 p-md-5 bg-surface shadow-lg"
            style={{ maxWidth: 640, width: '100%' }}
            onClick={e => e.stopPropagation()}
          >
            <div className="d-flex justify-content-between align-items-center mb-2">
              <h4 className="fs-5 fw-bold mb-0">Webhook #{selected.id}</h4>
              <button className="btn btn-tr-ghost p-1" onClick={() => setSelected(null)} aria-label="Đóng">
                <X size={18} />
              </button>
            </div>
            <div className="small text-muted mb-3">
              Cổng <strong>{selected.provider}</strong> • event key <span className="font-monospace">{selected.eventKey}</span> • gửi{' '}
              {selected.deliveries} lần{selected.lastReceivedAt ? `, lần cuối ${formatDateTimeVN(selected.lastReceivedAt)}` : ''}
            </div>
            <label className="form-label small fw-semibold">Payload gốc (Raw Callback Body)</label>
            <pre className="p-3 bg-dark text-white rounded-3 small font-monospace overflow-auto mb-3" style={{ maxHeight: 260 }}>
              {pretty(selected.payload)}
            </pre>
            <button className="btn btn-tr-secondary w-100" onClick={() => setSelected(null)}>Đóng</button>
          </div>
        </div>
      )}
    </div>
  );
};
