import React, { useState, useEffect } from 'react';
import { QRCodeSVG } from 'qrcode.react';
import { Wallet, Maximize2, X, Calendar, MapPin } from 'lucide-react';
import { ApiService } from '../../api/apiClient';
import type { TicketPass } from '../../types/models';
import { EmptyState, ErrorState, DemoBanner } from '../../components/DataStates';

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
      <DemoBanner>vé lấy từ đơn đã thanh toán; mã QR chỉ minh hoạ, chưa có dịch vụ soát vé.</DemoBanner>
      <div className="d-flex align-items-center justify-content-between mb-4">
        <div>
          <h1 className="fs-3 fw-bold mb-1">Ví vé điện tử của bạn</h1>
          <p className="text-muted small mb-0">Xuất trình mã QR tại cổng kiểm soát để làm thủ tục check-in vào sự kiện</p>
        </div>
      </div>

      {error && <ErrorState message={error} onRetry={loadPasses} />}

      {loading ? (
        <div className="row g-4">
          {[1, 2].map(n => (
            <div key={n} className="col-12 col-md-6">
              <div className="tr-skeleton w-100 rounded-3" style={{ height: 260 }} />
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
            <div key={pass.id} className="col-12 col-md-6 col-lg-6">
              {/* E-Ticket Card styled like flight boarding pass */}
              <div className="tr-card bg-surface shadow-sm overflow-hidden d-flex flex-column h-100 border border-subtle">
                {/* Header colored banner */}
                <div className="p-3 bg-dark text-white d-flex justify-content-between align-items-center">
                  <div className="d-flex align-items-center gap-2">
                    <span className="badge bg-primary text-uppercase font-monospace">{pass.id}</span>
                    <span className="small text-white-50">TICKETRUSH PASS</span>
                  </div>
                  <span className={`badge ${pass.status === 'VALID' ? 'bg-success' : 'bg-secondary'}`}>
                    {pass.status === 'VALID' ? 'Hợp lệ • Chưa soát' : 'Đã sử dụng'}
                  </span>
                </div>

                <div className="p-4 d-flex flex-column flex-grow-1 justify-content-between">
                  <div>
                    <h3 className="fs-5 fw-bold text-main mb-2">{pass.eventTitle}</h3>
                    <div className="d-flex align-items-center gap-2 small text-muted mb-1">
                      <Calendar size={16} className="text-primary flex-shrink-0" />
                      <span>{pass.eventDate}</span>
                    </div>
                    <div className="d-flex align-items-center gap-2 small text-muted mb-3">
                      <MapPin size={16} className="text-primary flex-shrink-0" />
                      <span className="text-truncate">{pass.venueName}</span>
                    </div>

                    <div className="p-3 bg-surface-subtle rounded-3 border border-subtle row g-2 mb-3">
                      <div className="col-6">
                        <span className="small text-muted">Loại vé:</span>
                        <div className="fw-bold text-main">{pass.ticketTypeName}</div>
                      </div>
                      <div className="col-6">
                        <span className="small text-muted">Vị trí:</span>
                        <div className="fw-bold text-primary">{pass.seatInfo}</div>
                      </div>
                      <div className="col-12 border-top border-subtle pt-2 mt-2">
                        <span className="small text-muted">Khách hàng: </span>
                        <span className="fw-semibold text-main">{pass.customerName}</span>
                      </div>
                    </div>
                  </div>

                  {/* QR code and check-in trigger */}
                  <div className="pt-3 border-top border-subtle d-flex align-items-center justify-content-between">
                    <div className="d-flex align-items-center gap-3">
                      <div className="bg-white p-2 rounded-2 border border-subtle shadow-xs">
                        <QRCodeSVG value={pass.qrPayload} size={64} level="M" />
                      </div>
                      <div className="small text-muted" style={{ fontSize: '0.75rem' }}>
                        <div>Mã kiểm soát:</div>
                        <span className="font-monospace text-main fw-semibold">{pass.id}</span>
                      </div>
                    </div>

                    <div className="d-flex gap-2">
                      <button
                        onClick={() => addToCalendar(pass)}
                        className="btn btn-sm btn-tr-secondary p-2"
                        title="Thêm vào lịch"
                        aria-label="Thêm vào lịch"
                      >
                        <Calendar size={16} />
                      </button>
                      <button
                        onClick={() => setSelectedPass(pass)}
                        className="btn btn-sm btn-tr-primary d-flex align-items-center gap-1"
                      >
                        <Maximize2 size={16} /> Soát vé
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Fullscreen Check-in Modal */}
      {selectedPass && (
        <div
          className="position-fixed top-0 start-0 w-100 h-100 bg-dark bg-opacity-75 z-3 d-flex align-items-center justify-content-center p-3"
          style={{ backdropFilter: 'blur(6px)' }}
          onClick={() => setSelectedPass(null)}
        >
          <div
            className="tr-card p-4 p-md-5 bg-surface text-center shadow-lg"
            style={{ maxWidth: 460, width: '100%' }}
            onClick={(e) => e.stopPropagation()}
          >
            <div className="d-flex justify-content-between align-items-center mb-3">
              <span className="badge bg-primary text-uppercase font-monospace">{selectedPass.id}</span>
              <button className="btn btn-tr-ghost p-1" onClick={() => setSelectedPass(null)}>
                <X size={20} />
              </button>
            </div>

            <h3 className="fs-5 fw-bold text-main mb-1">{selectedPass.eventTitle}</h3>
            <p className="small text-muted mb-4">{selectedPass.ticketTypeName} • {selectedPass.seatInfo}</p>

            {/* High Contrast Bright QR for scanning guns */}
            <div className="p-4 bg-white rounded-3 border d-inline-block shadow-sm mb-4">
              <QRCodeSVG value={selectedPass.qrPayload} size={220} level="H" />
            </div>

            <div className="fw-semibold text-main mb-1">{selectedPass.customerName}</div>
            <div className="font-monospace small text-muted mb-3">{selectedPass.qrPayload}</div>

            <div className="alert alert-info py-2 small mb-4">
              Hãy tăng độ sáng màn hình điện thoại tối đa để máy quét tại cổng đọc vé nhanh nhất.
            </div>

            <button className="btn btn-tr-secondary w-100" onClick={() => setSelectedPass(null)}>
              Đóng lại
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
