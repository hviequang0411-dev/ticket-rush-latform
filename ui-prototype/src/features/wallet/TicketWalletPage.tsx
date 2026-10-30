import React, { useState, useEffect } from 'react';
import { QRCodeSVG } from 'qrcode.react';
import { Wallet, Maximize2, X, Calendar, MapPin, Sparkles, ShieldCheck, Sun } from 'lucide-react';
import { ApiService } from '../../api/apiClient';
import type { TicketPass } from '../../types/models';
import { EmptyState, ErrorState } from '../../components/DataStates';

export const TicketWalletPage: React.FC = () => {
  const [passes, setPasses] = useState<TicketPass[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Fullscreen modal check-in state
  const [selectedPass, setSelectedPass] = useState<TicketPass | null>(null);

  useEffect(() => {
    loadPasses();
  }, []);

  const loadPasses = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await ApiService.getPasses();
      setPasses(data);
    } catch (e: unknown) {
      const err = e as { message?: string };
      setError(err.message || 'Không thể tải ví vé.');
    } finally {
      setLoading(false);
    }
  };

  const addToCalendar = (pass: TicketPass) => {
    alert(`Đã thêm sự kiện "${pass.eventTitle}" vào Lịch của bạn.`);
  };

  return (
    <div className="tr-container py-4">
      <div className="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4 pb-3 border-bottom border-subtle">
        <div>
          <span className="tr-badge badge-success mb-2">
            <ShieldCheck size={13} /> VÉ ĐIỆN TỬ ĐÃ XÁC THỰC
          </span>
          <h1 className="fs-3 fw-bold mb-1">Ví vé điện tử của bạn</h1>
          <p className="text-muted small mb-0">
            Xuất trình mã QR tại cổng kiểm soát để làm thủ tục check-in tức thì vào khán phòng
          </p>
        </div>
        {passes.length > 0 && (
          <div className="tr-badge badge-info py-2 px-3 tabular-nums">
            Đang lưu trữ {passes.length} vé hợp lệ
          </div>
        )}
      </div>

      {error && <ErrorState message={error} onRetry={loadPasses} />}

      {loading ? (
        <div className="row g-4">
          {[1, 2].map(n => (
            <div key={n} className="col-12 col-md-6">
              <div className="tr-skeleton w-100 rounded-4" style={{ height: 320 }} />
            </div>
          ))}
        </div>
      ) : passes.length === 0 ? (
        <EmptyState
          title="Ví vé của bạn đang trống"
          description="Bạn chưa sở hữu vé điện tử nào. Vé sẽ tự động hiển thị tại đây ngay sau khi bạn thanh toán thành công."
          actionText="Tìm vé sự kiện"
          onAction={() => { window.location.href = '/events'; }}
          icon={<Wallet size={36} />}
        />
      ) : (
        <div className="row g-4">
          {passes.map(pass => (
            <div key={pass.id} className="col-12 col-md-6">
              {/* Perforated E-Ticket Boarding Pass Card with Holographic Hover Sheen */}
              <div className="tr-ticket-pass d-flex flex-column h-100">
                {/* Concert Pass Header */}
                <div
                  className="px-4 py-3 text-white d-flex justify-content-between align-items-center"
                  style={{
                    background: 'linear-gradient(135deg, #090d16 0%, #1e293b 65%, #334155 100%)',
                  }}
                >
                  <div className="d-flex align-items-center gap-2">
                    <Sparkles size={15} className="text-warning" />
                    <span className="badge bg-primary text-uppercase font-monospace">{pass.id}</span>
                    <span className="small text-white-50 font-monospace" style={{ fontSize: '0.72rem' }}>
                      TICKETRUSH PASS
                    </span>
                  </div>
                  <span className={`badge rounded-pill ${pass.status === 'VALID' ? 'bg-success' : 'bg-secondary'}`}>
                    {pass.status === 'VALID' ? 'Hợp lệ • Chưa soát' : 'Đã sử dụng'}
                  </span>
                </div>

                {/* Main Ticket Body */}
                <div className="p-4 d-flex flex-column flex-grow-1 justify-content-between">
                  <div>
                    <h3 className="fs-5 fw-bold text-main mb-2">{pass.eventTitle}</h3>
                    <div className="d-flex align-items-center gap-2 small text-muted mb-1 tabular-nums">
                      <Calendar size={15} className="text-primary flex-shrink-0" />
                      <span>{pass.eventDate}</span>
                    </div>
                    <div className="d-flex align-items-center gap-2 small text-muted mb-3">
                      <MapPin size={15} className="text-primary flex-shrink-0" />
                      <span className="text-truncate">{pass.venueName}</span>
                    </div>

                    <div className="p-3 bg-surface-subtle rounded-3 border border-subtle row g-2 mx-0">
                      <div className="col-6">
                        <span className="small text-muted d-block" style={{ fontSize: '0.72rem' }}>Hạng vé</span>
                        <div className="fw-bold text-main small">{pass.ticketTypeName}</div>
                      </div>
                      <div className="col-6">
                        <span className="small text-muted d-block" style={{ fontSize: '0.72rem' }}>Khu vực / Ghế</span>
                        <div className="fw-bold text-primary small tabular-nums">{pass.seatInfo}</div>
                      </div>
                      <div className="col-12 border-top border-subtle pt-2 mt-2 d-flex justify-content-between align-items-center">
                        <span className="small text-muted" style={{ fontSize: '0.75rem' }}>Khách tham dự:</span>
                        <span className="fw-semibold text-main small">{pass.customerName}</span>
                      </div>
                    </div>
                  </div>

                  {/* Perforated Notch Divider */}
                  <div className="tr-ticket-divider my-3" />

                  {/* Ticket Stub: QR Code & Gate Check-in Action */}
                  <div className="d-flex align-items-center justify-content-between gap-2">
                    <div className="d-flex align-items-center gap-3">
                      <div className="tr-qr-frame p-2 rounded-3 border shadow-xs">
                        <QRCodeSVG value={pass.qrPayload} size={64} level="M" />
                      </div>
                      <div className="small text-muted" style={{ fontSize: '0.74rem' }}>
                        <div>Mã kiểm soát cổng:</div>
                        <span className="font-monospace text-main fw-bold">{pass.id}</span>
                        <div className="text-success fw-medium">Sẵn sàng quét</div>
                      </div>
                    </div>

                    <div className="d-flex gap-2">
                      <button
                        type="button"
                        onClick={() => addToCalendar(pass)}
                        className="btn btn-sm btn-tr-secondary p-2"
                        title="Thêm vào lịch"
                        aria-label="Thêm vào lịch"
                      >
                        <Calendar size={16} />
                      </button>
                      <button
                        type="button"
                        onClick={() => setSelectedPass(pass)}
                        className="btn btn-sm btn-tr-primary d-flex align-items-center gap-1"
                      >
                        <Maximize2 size={16} />
                        <span>Soát vé</span>
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Fullscreen High-Contrast Gate Check-in Modal */}
      {selectedPass && (
        <div
          className="position-fixed top-0 start-0 w-100 h-100 bg-dark bg-opacity-75 d-flex align-items-center justify-content-center p-3"
          style={{ zIndex: 1060, backdropFilter: 'blur(8px)' }}
          onClick={() => setSelectedPass(null)}
        >
          <div
            role="dialog"
            aria-modal="true"
            aria-label={`Mã QR soát vé ${selectedPass.id}`}
            className="tr-card tr-card-featured p-4 p-md-5 bg-surface text-center"
            style={{ maxWidth: 460, width: '100%' }}
            onClick={(e) => e.stopPropagation()}
          >
            <div className="d-flex justify-content-between align-items-center mb-3">
              <span className="badge bg-primary text-uppercase font-monospace px-3 py-1">
                {selectedPass.id}
              </span>
              <button
                type="button"
                className="btn-tr-ghost p-1 rounded-circle"
                onClick={() => setSelectedPass(null)}
                aria-label="Đóng chế độ soát vé"
              >
                <X size={20} />
              </button>
            </div>

            <h3 className="fs-5 fw-bold text-main mb-1">{selectedPass.eventTitle}</h3>
            <p className="small text-muted mb-4">
              {selectedPass.ticketTypeName} • <strong className="text-primary">{selectedPass.seatInfo}</strong>
            </p>

            {/* Pure White High-Contrast QR Frame for Gate Optical Scanners */}
            <div className="tr-qr-frame p-4 rounded-4 border d-inline-block shadow-sm mb-3">
              <QRCodeSVG value={selectedPass.qrPayload} size={220} level="H" />
            </div>

            <div className="fw-bold text-main mb-1">{selectedPass.customerName}</div>
            <div className="font-monospace small text-muted mb-3 text-break" style={{ fontSize: '0.74rem' }}>
              {selectedPass.qrPayload}
            </div>

            <div className="p-3 rounded-3 bg-surface-subtle border border-subtle small text-muted mb-4 d-flex align-items-center gap-2 text-start">
              <Sun size={18} className="text-warning flex-shrink-0" />
              <span>Hãy tăng độ sáng màn hình điện thoại tối đa để máy quét quang học tại cổng đọc vé nhanh nhất.</span>
            </div>

            <button type="button" className="btn btn-tr-secondary w-100" onClick={() => setSelectedPass(null)}>
              Đóng chế độ toàn màn hình
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
