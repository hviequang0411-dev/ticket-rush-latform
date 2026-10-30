import React, { useState } from 'react';
import { CreditCard, CheckCircle2, XCircle, Code2 } from 'lucide-react';
import { mockWebhooks } from '../../mocks/mockData';
import type { WebhookLog } from '../../types/models';
import { formatVND, formatDateTimeVN } from '../../lib/format';

export const AdminPaymentsPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'PAYMENTS' | 'WEBHOOKS'>('WEBHOOKS');
  const [webhooks] = useState<WebhookLog[]>(mockWebhooks);
  const [selectedWebhook, setSelectedWebhook] = useState<WebhookLog | null>(null);

  return (
    <div>
      <div className="mb-4">
        <h1 className="fs-3 fw-bold mb-1">Giao dịch thanh toán &amp; Webhook đối tác</h1>
        <p className="text-muted small mb-0">Theo dõi dòng tiền đối soát và kiểm tra nhật ký Webhook (VNPay, MoMo, Sandbox)</p>
      </div>

      <div className="d-flex gap-2 border-bottom border-subtle mb-4 pb-1">
        <button
          onClick={() => setActiveTab('WEBHOOKS')}
          className={`btn btn-sm btn-touch-target border-0 pb-2 px-3 fw-semibold rounded-0 ${
            activeTab === 'WEBHOOKS' ? 'border-bottom border-primary text-primary border-3' : 'text-muted'
          }`}
        >
          Nhật ký Webhook đã nhận ({webhooks.length})
        </button>
        <button
          onClick={() => setActiveTab('PAYMENTS')}
          className={`btn btn-sm btn-touch-target border-0 pb-2 px-3 fw-semibold rounded-0 ${
            activeTab === 'PAYMENTS' ? 'border-bottom border-primary text-primary border-3' : 'text-muted'
          }`}
        >
          Lịch sử giao dịch thanh toán
        </button>
      </div>

      {activeTab === 'WEBHOOKS' ? (
        <div className="tr-card overflow-hidden">
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead className="table-light">
                <tr className="small text-muted">
                  <th>ID WEBHOOK</th>
                  <th>CỔNG</th>
                  <th>MÃ ĐƠN TICKETRUSH</th>
                  <th>SỐ TIỀN</th>
                  <th>CHỮ KÝ (HMAC SHA512)</th>
                  <th>XỬ LÝ</th>
                  <th>THỜI GIAN NHẬN</th>
                  <th className="text-end">PAYLOAD</th>
                </tr>
              </thead>
              <tbody>
                {webhooks.map(wh => (
                  <tr key={wh.id}>
                    <td className="font-monospace fw-bold text-main">{wh.id}</td>
                    <td><span className="badge bg-secondary-subtle text-secondary">{wh.provider}</span></td>
                    <td className="font-monospace text-main">{wh.orderCode}</td>
                    <td className="fw-bold text-primary tabular-nums">{formatVND(wh.amount)}</td>
                    <td>
                      {wh.signatureValid ? (
                        <span className="badge bg-success-subtle text-success d-inline-flex align-items-center gap-1">
                          <CheckCircle2 size={12} /> Hợp lệ
                        </span>
                      ) : (
                        <span className="badge bg-danger-subtle text-danger d-inline-flex align-items-center gap-1">
                          <XCircle size={12} /> Không hợp lệ
                        </span>
                      )}
                    </td>
                    <td>
                      {wh.processed ? (
                        <span className="badge bg-success text-white">Đã xử lý xong</span>
                      ) : (
                        <span className="badge bg-warning text-dark">Chưa xử lý</span>
                      )}
                    </td>
                    <td className="small text-muted">{formatDateTimeVN(wh.timestamp)}</td>
                    <td className="text-end">
                      <button
                        className="btn btn-sm btn-tr-secondary d-inline-flex align-items-center gap-1"
                        onClick={() => setSelectedWebhook(wh)}
                      >
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
        <div className="tr-card p-4 text-center text-muted">
          <CreditCard size={36} className="mb-2 text-muted" />
          <p className="mb-0">Dữ liệu thanh toán ngân hàng đang được đồng bộ tự động với cổng đối soát.</p>
        </div>
      )}

      {/* Webhook Payload JSON Viewer Modal */}
      {selectedWebhook && (
        <div
          className="position-fixed top-0 start-0 w-100 h-100 bg-dark bg-opacity-75 z-3 d-flex align-items-center justify-content-center p-3"
          onClick={() => setSelectedWebhook(null)}
        >
          <div
            className="tr-card p-4 p-md-5 bg-surface shadow-lg"
            style={{ maxWidth: 600, width: '100%' }}
            onClick={(e) => e.stopPropagation()}
          >
            <h4 className="fs-5 fw-bold mb-2">Chi tiết Webhook: {selectedWebhook.id}</h4>
            <div className="small text-muted mb-3">
              Cổng: <strong>{selectedWebhook.provider}</strong> • Giao dịch: <strong>{selectedWebhook.providerRef}</strong>
            </div>

            <div className="mb-3">
              <label className="form-label small fw-semibold">Raw JSON Payload:</label>
              <pre className="p-3 bg-dark text-white rounded-3 small font-monospace overflow-auto" style={{ maxHeight: 220 }}>
                {JSON.stringify(JSON.parse(selectedWebhook.payload), null, 2)}
              </pre>
            </div>

            <button className="btn btn-tr-secondary w-100" onClick={() => setSelectedWebhook(null)}>
              Đóng lại
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

