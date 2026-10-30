import React, { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { Users, AlertTriangle, ShieldAlert } from 'lucide-react';
import { DemoBanner } from '../../components/DataStates';

export const WaitingRoomPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const eventSlug = searchParams.get('event') || 'dem-nhac-mua-thu-ha-noi';

  // State in queue
  const [position, setPosition] = useState<number>(428);
  const [initialPos] = useState<number>(428);
  const [estimatedSeconds, setEstimatedSeconds] = useState<number>(35);

  useEffect(() => {
    const timer = setInterval(() => {
      setPosition(prev => {
        if (prev <= 1) {
          clearInterval(timer);
          // Redirect to checkout or event detail when turn arrives
          setTimeout(() => {
            navigate(`/events/${eventSlug}?turn=granted`);
          }, 800);
          return 0;
        }
        // Step decrease simulate queue processing
        const decrease = Math.min(prev, Math.floor(Math.random() * 25) + 10);
        return prev - decrease;
      });

      setEstimatedSeconds(prev => (prev > 2 ? prev - 2 : 1));
    }, 1500);

    return () => clearInterval(timer);
  }, [eventSlug, navigate]);

  const progressPercent = Math.min(100, Math.round(((initialPos - position) / initialPos) * 100));

  return (
    <div className="tr-container py-5 d-flex justify-content-center align-items-center" style={{ minHeight: '70vh' }}>
      <div className="tr-card p-4 p-md-5 shadow-md text-center bg-surface" style={{ maxWidth: '580px', width: '100%' }}>
        <DemoBanner>phòng chờ mô phỏng, chưa nối backend.</DemoBanner>
        <div className="d-inline-flex align-items-center justify-content-center p-3 rounded-circle bg-primary-subtle text-primary mb-3">
          <Users size={36} />
        </div>

        <h1 className="fs-3 fw-bold mb-2">Phòng chờ ảo TicketRush</h1>
        <p className="text-muted small mb-4">
          Lượng truy cập sự kiện đang rất cao. Hệ thống xếp hàng công bằng đang điều phối lượt người vào đặt vé để chống nghẽn mạng và chống tool bot.
        </p>

        {/* Queue Metric Cards */}
        <div className="row g-3 mb-4">
          <div className="col-6">
            <div className="p-3 bg-surface-subtle rounded-3 border border-subtle">
              <div className="small text-muted mb-1">Vị trí của bạn</div>
              <div className="fs-3 fw-bold text-primary tabular-nums">
                {position > 0 ? `#${position}` : 'Đến lượt bạn!'}
              </div>
            </div>
          </div>
          <div className="col-6">
            <div className="p-3 bg-surface-subtle rounded-3 border border-subtle">
              <div className="small text-muted mb-1">Thời gian chờ ước tính</div>
              <div className="fs-3 fw-bold text-main tabular-nums">
                {position > 0 ? `~${estimatedSeconds}s` : '0s'}
              </div>
            </div>
          </div>
        </div>

        {/* Progress Bar */}
        <div className="mb-4 text-start">
          <div className="d-flex justify-content-between small text-muted mb-1">
            <span>Tiến độ hàng chờ</span>
            <span className="fw-semibold">{progressPercent}%</span>
          </div>
          <div className="progress" style={{ height: 8 }} role="progressbar" aria-valuenow={progressPercent} aria-valuemin={0} aria-valuemax={100} aria-label={`Tiến độ hàng chờ ${progressPercent}%`}>
            <div
              className="progress-bar bg-primary progress-bar-striped progress-bar-animated"
              style={{ width: `${progressPercent}%` }}
            />
          </div>
        </div>

        {/* Critical Anti-Refresh Notice */}
        <div className="alert alert-warning d-flex align-items-start gap-3 text-start small mb-4">
          <AlertTriangle size={20} className="text-warning flex-shrink-0 mt-1" />
          <div>
            <strong>Lưu ý quan trọng:</strong> Vui lòng <strong>không tải lại trang</strong> (F5) hoặc đóng trình duyệt. Vị trí của bạn sẽ tự động chuyển vào trang đặt vé ngay khi đến lượt.
          </div>
        </div>

        <div className="d-flex align-items-center justify-content-center gap-2 small text-muted">
          <ShieldAlert size={16} className="text-success" />
          <span>TicketRush Traffic Defender • Thuật toán Token Bucket chống ddos</span>
        </div>
      </div>
    </div>
  );
};

