import React, { useState } from 'react';
import { QrCode, CheckCircle2, XCircle, Camera, ShieldCheck } from 'lucide-react';
import { ApiService } from '../../api/apiClient';
import type { TicketPass } from '../../types/models';

export const AdminCheckInPage: React.FC = () => {
  const [qrInput, setQrInput] = useState('TICKETRUSH::PASS-7701::ORD-88231::SEC-VIP-A12');
  const [result, setResult] = useState<{
    success: boolean;
    pass?: TicketPass;
    reason?: string;
  } | null>(null);
  const [scanning, setScanning] = useState(false);

  // Statistics
  const [checkedInCount, setCheckedInCount] = useState(482);
  const [totalExpected] = useState(900);

  const handleScan = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!qrInput.trim()) return;

    setScanning(true);
    const res = await ApiService.checkInPass(qrInput.trim());
    setResult(res);
    if (res.success) {
      setCheckedInCount(prev => prev + 1);
    }
    setScanning(false);
  };

  return (
    <div>
      <div className="mb-4">
        <span className="tr-badge badge-success mb-1">
          <ShieldCheck size={13} /> GATE ACCESS CONTROL
        </span>
        <h1 className="fs-3 fw-bold mb-1">Soát vé tại cổng (On-site Gate Check-in)</h1>
        <p className="text-muted small mb-0">Quét mã QR từ vé điện tử để xác thực và check-in khán giả vào khán phòng</p>
      </div>

      {/* Check-in stats banner */}
      <div className="row g-3 mb-4">
        <div className="col-12 col-sm-4">
          <div className="tr-card p-3 p-md-4 text-center">
            <span className="small text-muted d-block mb-1">Đã soát vé vào cổng</span>
            <div className="fs-3 fw-bold text-success tabular-nums" style={{ fontFamily: 'var(--font-display)' }}>
              {checkedInCount} khách
            </div>
          </div>
        </div>
        <div className="col-12 col-sm-4">
          <div className="tr-card p-3 p-md-4 text-center">
            <span className="small text-muted d-block mb-1">Tổng vé bán ra</span>
            <div className="fs-3 fw-bold text-main tabular-nums" style={{ fontFamily: 'var(--font-display)' }}>
              {totalExpected} vé
            </div>
          </div>
        </div>
        <div className="col-12 col-sm-4">
          <div className="tr-card p-3 p-md-4 text-center">
            <span className="small text-muted d-block mb-1">Tỉ lệ khán giả có mặt</span>
            <div className="fs-3 fw-bold text-primary tabular-nums" style={{ fontFamily: 'var(--font-display)' }}>
              {Math.round((checkedInCount / totalExpected) * 100)}%
            </div>
          </div>
        </div>
      </div>

      <div className="row g-4">
        {/* Left: Scanner Mockup */}
        <div className="col-12 col-lg-6">
          <div className="tr-card p-4 text-center h-100">
            <h3 className="fs-5 fw-bold mb-3">Ống kính quét mã QR (Camera Viewfinder)</h3>

            {/* Simulated Camera Window */}
            <div
              className="position-relative rounded-4 overflow-hidden d-flex flex-column align-items-center justify-content-center mb-3 mx-auto"
              style={{
                height: 250,
                maxWidth: 360,
                background: 'radial-gradient(circle, #1e293b 0%, #090d16 100%)',
                border: '1px solid var(--border-strong)',
              }}
            >
              <div className="text-white-50 d-flex flex-column align-items-center gap-2">
                <Camera size={40} className="text-primary" />
                <span className="small">Đặt mã QR trong khung ngắm quang học</span>
              </div>
              {/* Laser scan line */}
              <div
                className="position-absolute w-75"
                style={{
                  height: 2,
                  background: 'linear-gradient(90deg, transparent, var(--color-primary), transparent)',
                  boxShadow: '0 0 12px var(--color-primary)',
                }}
              />
              {/* Corner targeting marks */}
              <div className="position-absolute top-0 start-0 m-3 border-top border-start border-3 border-primary" style={{ width: 24, height: 24 }} />
              <div className="position-absolute top-0 end-0 m-3 border-top border-end border-3 border-primary" style={{ width: 24, height: 24 }} />
              <div className="position-absolute bottom-0 start-0 m-3 border-bottom border-start border-3 border-primary" style={{ width: 24, height: 24 }} />
              <div className="position-absolute bottom-0 end-0 m-3 border-bottom border-end border-3 border-primary" style={{ width: 24, height: 24 }} />
            </div>

            <form onSubmit={handleScan} className="d-flex flex-column gap-2 text-start">
              <label className="form-label small fw-semibold" htmlFor="qrInput">Mã QR / Chuỗi ký tự từ máy quét cầm tay</label>
              <div className="input-group">
                <span className="input-group-text">
                  <QrCode size={16} />
                </span>
                <input
                  id="qrInput"
                  type="text"
                  className="form-control font-monospace"
                  placeholder="Quét hoặc nhập mã vé..."
                  value={qrInput}
                  onChange={(e) => setQrInput(e.target.value)}
                />
                <button type="submit" disabled={scanning} className="btn btn-tr-primary">
                  {scanning ? 'Đang kiểm tra...' : 'Kiểm tra mã'}
                </button>
              </div>
            </form>

            <div className="d-flex flex-wrap justify-content-center gap-2 mt-3">
              <button
                type="button"
                className="btn btn-sm btn-tr-secondary"
                onClick={() => setQrInput('TICKETRUSH::PASS-7701::ORD-88231::SEC-VIP-A12')}
              >
                Mã mẫu HỢP LỆ
              </button>
              <button
                type="button"
                className="btn btn-sm btn-tr-secondary"
                onClick={() => setQrInput('TICKETRUSH::PASS-INVALID-999')}
              >
                Mã KHÔNG HỢP LỆ
              </button>
            </div>
          </div>
        </div>

        {/* Right: Validation Feedback Result */}
        <div className="col-12 col-lg-6">
          <div className="tr-card p-4 h-100">
            <h3 className="fs-5 fw-bold mb-3">Kết quả xác thực tức thì</h3>

            {!result ? (
              <div className="text-center py-5 text-muted small">
                Sẵn sàng tiếp nhận lượt quét vé. Kết quả hợp lệ / đã dùng sẽ hiển thị ngay tại đây.
              </div>
            ) : result.success ? (
              <div
                className="p-4 rounded-4 text-center"
                style={{
                  backgroundColor: 'var(--color-success-bg)',
                  border: '1px solid var(--color-success-border)',
                }}
              >
                <CheckCircle2 size={48} className="text-success mb-2" />
                <h2 className="fs-4 fw-bold text-success mb-1">VÉ HỢP LỆ - MỜI VÀO CỔNG</h2>
                <div className="small text-muted mb-3">Thủ tục check-in hoàn tất. Đã cập nhật vào cơ sở dữ liệu.</div>

                <div className="bg-surface p-3 rounded-3 text-start small border border-subtle">
                  <div className="d-flex justify-content-between mb-1">
                    <span className="text-muted">Khách hàng:</span>
                    <span className="fw-bold text-main">{result.pass?.customerName}</span>
                  </div>
                  <div className="d-flex justify-content-between mb-1">
                    <span className="text-muted">Sự kiện:</span>
                    <span className="fw-semibold text-main">{result.pass?.eventTitle}</span>
                  </div>
                  <div className="d-flex justify-content-between mb-1">
                    <span className="text-muted">Hạng vé:</span>
                    <span className="fw-bold text-primary">{result.pass?.ticketTypeName}</span>
                  </div>
                  <div className="d-flex justify-content-between">
                    <span className="text-muted">Vị trí:</span>
                    <span className="fw-bold text-primary">{result.pass?.seatInfo}</span>
                  </div>
                </div>
              </div>
            ) : (
              <div
                className="p-4 rounded-4 text-center"
                style={{
                  backgroundColor: 'var(--color-danger-bg)',
                  border: '1px solid var(--color-danger-border)',
                }}
              >
                <XCircle size={48} className="text-danger mb-2" />
                <h2 className="fs-4 fw-bold text-danger mb-1">TỪ CHỐI QUA CỔNG</h2>
                <p className="small text-danger fw-semibold mb-3">{result.reason}</p>

                {result.pass && (
                  <div className="bg-surface p-3 rounded-3 text-start small border border-subtle">
                    <div className="text-muted mb-1">Thông tin vé đã lưu:</div>
                    <div>Khách hàng: <strong>{result.pass.customerName}</strong></div>
                    <div>Mã vé: <span className="font-monospace">{result.pass.id}</span></div>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
