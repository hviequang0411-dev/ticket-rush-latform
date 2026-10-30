import React, { useCallback, useEffect, useState } from 'react';
import { CheckCircle2, XCircle, Code2, Repeat } from 'lucide-react';
import { AdminApi, type PaymentRow, type WebhookRow } from '../../api/adminApi';
import type { ApiErrorProblem } from '../../types/models';
import { StatusBadge } from '../../components/StatusBadge';
import { ErrorState } from '../../components/DataStates';
import { formatVND, formatDateTimeVN } from '../../lib/format';

const OUTCOME: Record<WebhookRow['outcome'], { label: string; className: string }> = {
  PROCESSED: { label: 'Đã xử lý', className: 'bg-success-subtle text-success' },
  REJECTED: { label: 'Từ chối (sai chữ ký)', className: 'bg-danger-subtle text-danger' },
  UNMATCHED: { label: 'Không khớp giao dịch', className: 'bg-warning-subtle text-warning' },
  PENDING: { label: 'Đang xử lý', className: 'bg-secondary-subtle text-secondary' },
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
          Mỗi callback chỉ được áp dụng một lần dù cổng gửi lại bao nhiêu lần: {retries} lần gửi lại đã bị bỏ qua an toàn.
        </p>
      </div>

      <div className="d-flex gap-2 border-bottom border-subtle mb-4 pb-1" role="tablist">
        {(['WEBHOOKS', 'PAYMENTS'] as const).map(tab => (
          <button
            key={tab}
            role="tab"
            aria-selected={activeTab === tab}
            onClick={() => setActiveTab(tab)}
            className={`btn btn-sm btn-touch-target border-0 pb-2 px-3 fw-semibold rounded-0 ${
              activeTab === tab ? 'border-bottom border-primary text-primary border-3' : 'text-muted'
            }`}
          >
            {tab === 'WEBHOOKS' ? `Nhật ký Webhook (${webhooks.length})` : `Giao dịch thanh toán (${payments.length})`}
          </button>
        ))}
      </div>

      {error && <ErrorState title={error.title} message={error.detail} status={error.status} onRetry={load} />}

      {activeTab === 'WEBHOOKS' ? (
        <div className="tr-card overflow-hidden">
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead className="table-light">
                <tr className="small text-muted">
                  <th>ID</th>
                  <th>CỔNG</th>
                  <th>MÃ ĐƠN</th>
                  <th>SỐ TIỀN</th>
                  <th>CHỮ KÝ HMAC</th>
                  <th>KẾT QUẢ</th>
                  <th>SỐ LẦN GỬI</th>
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
                    <td><span className="badge bg-secondary-subtle text-secondary">{wh.provider}</span></td>
                    <td className="font-monospace text-main">{wh.orderCode ?? '—'}</td>
                    <td className="fw-bold text-primary tabular-nums">{wh.amount != null ? formatVND(wh.amount) : '—'}</td>
                    <td>
                      {wh.signatureValid ? (
                        <span className="d-inline-flex align-items-center gap-1 small text-success"><CheckCircle2 size={14} /> Hợp lệ</span>
                      ) : (
                        <span className="d-inline-flex align-items-center gap-1 small text-danger"><XCircle size={14} /> Sai</span>
                      )}
                    </td>
                    <td><span className={`badge ${OUTCOME[wh.outcome].className}`}>{OUTCOME[wh.outcome].label}</span></td>
                    <td className="tabular-nums">
                      {wh.deliveries > 1 ? (
                        <span className="d-inline-flex align-items-center gap-1 text-warning" title="Cổng gửi lại; chỉ lần đầu được áp dụng">
                          <Repeat size={14} /> {wh.deliveries}
                        </span>
                      ) : (
                        1
                      )}
                    </td>
                    <td className="small text-muted">{formatDateTimeVN(wh.receivedAt)}</td>
                    <td className="text-end">
                      <button className="btn btn-sm btn-tr-secondary d-inline-flex align-items-center gap-1" onClick={() => setSelected(wh)}>
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
        <div className="tr-card overflow-hidden">
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead className="table-light">
                <tr className="small text-muted">
                  <th>MÃ ĐƠN</th>
                  <th>CỔNG</th>
                  <th>MÃ GIAO DỊCH</th>
                  <th>SỐ TIỀN</th>
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
                    <td><span className="badge bg-secondary-subtle text-secondary">{p.provider}</span></td>
                    <td className="font-monospace small">{p.providerRef}</td>
                    <td className="fw-bold text-primary tabular-nums">{formatVND(p.amount)}</td>
                    <td><StatusBadge type="payment" status={p.status} /></td>
                    <td className="small font-monospace text-muted">{p.failureReason ?? ''}</td>
                    <td className="small text-muted">{formatDateTimeVN(p.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {selected && (
        <div
          className="position-fixed top-0 start-0 w-100 h-100 bg-dark bg-opacity-75 z-3 d-flex align-items-center justify-content-center p-3"
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
            <h4 className="fs-5 fw-bold mb-2">Webhook #{selected.id}</h4>
            <div className="small text-muted mb-3">
              Cổng <strong>{selected.provider}</strong> • event key <span className="font-monospace">{selected.eventKey}</span> • gửi{' '}
              {selected.deliveries} lần{selected.lastReceivedAt ? `, lần cuối ${formatDateTimeVN(selected.lastReceivedAt)}` : ''}
            </div>
            <label className="form-label small fw-semibold">Payload gốc</label>
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
