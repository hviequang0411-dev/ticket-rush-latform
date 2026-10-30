import React, { useState, useEffect } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import { ShieldCheck, AlertCircle, CheckCircle2, ArrowRight, Radio, Sparkles } from 'lucide-react';
import { ApiService, type QueueStatus } from '../../api/apiClient';
import type { ApiErrorProblem, Event } from '../../types/models';
import { ErrorState } from '../../components/DataStates';

export const WaitingRoomPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const eventSlug = searchParams.get('event') || 'neon-tide-festival-2026';

  const [event, setEvent] = useState<Event | null>(null);
  const [status, setStatus] = useState<QueueStatus | null>(null);
  const [initialPosition, setInitialPosition] = useState<number>(1);
  const [error, setError] = useState<ApiErrorProblem | null>(null);

  useEffect(() => {
    let cancelled = false;
    let timer: ReturnType<typeof setInterval> | undefined;

    const start = async () => {
      try {
        const ev = await ApiService.getEventBySlug(eventSlug);
        if (cancelled) return;
        setEvent(ev);
        const joined = await ApiService.joinQueue(ev.id);
        if (cancelled) return;
        setStatus(joined);
        setInitialPosition(Math.max(1, joined.ahead + 1));

        timer = setInterval(async () => {
          try {
            const next = await ApiService.queueStatus(ev.id);
            if (cancelled) return;
            setStatus(next);
            if (next.admitted) {
              clearInterval(timer);
            }
          } catch (e: unknown) {
            if (!cancelled) setError(e as ApiErrorProblem);
          }
        }, 2000);
      } catch (e: unknown) {
        if (!cancelled) setError(e as ApiErrorProblem);
      }
    };

    start();
    return () => {
      cancelled = true;
      if (timer) clearInterval(timer);
    };
  }, [eventSlug]);

  useEffect(() => {
    if (!status?.admitted || !event) return;
    const redirect = setTimeout(() => {
      if (sessionStorage.getItem('ticketrush.checkout')) {
        navigate('/checkout');
      } else {
        navigate(`/events/${event.slug}`);
      }
    }, 1200);
    return () => clearTimeout(redirect);
  }, [status?.admitted, event, navigate]);

  if (error) {
    return (
      <div className="tr-container py-5" style={{ maxWidth: 680 }}>
        <ErrorState title={error.title} message={error.detail} status={error.status} />
      </div>
    );
  }

  const position = status ? status.ahead + 1 : 0;
  const progressPercent = status?.admitted
    ? 100
    : Math.min(95, Math.max(8, Math.round(((initialPosition - position) / initialPosition) * 100)));

  return (
    <div className="tr-container py-5" style={{ maxWidth: 680 }}>
      <div className="tr-card tr-card-featured p-4 p-md-5 text-center position-relative overflow-hidden">
        <div
          className="position-absolute top-0 start-0 w-100"
          style={{
            height: 5,
            background: status?.admitted
              ? 'var(--color-success)'
              : 'linear-gradient(90deg, var(--color-primary), var(--color-accent))',
          }}
        />

        <div className="d-inline-flex align-items-center gap-2 tr-badge badge-warning mb-3">
          <Radio size={14} />
          <span>PHÒNG CHỜ ẢO CHỊU TẢI CAO (VIRTUAL WAITING ROOM)</span>
        </div>

        <h1 className="fs-3 fw-bold text-main mb-2">
          {status?.admitted ? 'Đã đến lượt của bạn!' : 'Bạn đang trong hàng chờ công bằng'}
        </h1>
        <p className="text-muted small mb-4 mx-auto" style={{ maxWidth: 500 }}>
          Sự kiện <strong className="text-main">{event?.name ?? eventSlug}</strong> đang mở bán với lưu lượng truy cập lớn.
          Hệ thống điều phối tuần tự để bảo vệ phiên giữ ghế của bạn.
        </p>

        {!status ? (
          <div className="py-5 text-muted small">
            <div className="spinner-border spinner-border-sm text-primary me-2" role="status" />
            Đang cấp số thứ tự hàng chờ...
          </div>
        ) : !status.admitted ? (
          <>
            <div className="p-4 rounded-4 bg-surface-subtle border border-subtle mb-4">
              <div className="row g-3 align-items-center">
                <div className="col-6 border-end border-subtle">
                  <div className="small text-muted mb-1">Vị trí hiện tại của bạn</div>
                  <div
                    className="display-5 fw-bold text-primary tabular-nums mb-0"
                    style={{ fontFamily: 'var(--font-display)' }}
                  >
                    #{position}
                  </div>
                  <span className="small text-muted" style={{ fontSize: '0.75rem' }}>{status.ahead} người phía trước</span>
                </div>
                <div className="col-6">
                  <div className="small text-muted mb-1">Thời gian chờ dự kiến</div>
                  <div
                    className="fs-2 fw-bold text-main tabular-nums mb-0"
                    style={{ fontFamily: 'var(--font-display)' }}
                  >
                    ~{status.etaSeconds}s
                  </div>
                  <span className="small text-success fw-medium" style={{ fontSize: '0.75rem' }}>
                    Đang nhích lên tự động
                  </span>
                </div>
              </div>

              <div className="mt-4">
                <div className="d-flex justify-content-between small text-muted mb-1">
                  <span>Tiến độ hàng chờ</span>
                  <span className="fw-bold text-primary tabular-nums">{progressPercent}%</span>
                </div>
                <div
                  className="progress"
                  style={{ height: 12, borderRadius: 999 }}
                  role="progressbar"
                  aria-valuenow={progressPercent}
                  aria-valuemin={0}
                  aria-valuemax={100}
                  aria-label="Tiến độ phòng chờ ảo"
                >
                  <div
                    className="progress-bar progress-bar-striped progress-bar-animated"
                    style={{
                      width: `${progressPercent}%`,
                      backgroundColor: 'var(--color-primary)',
                      transition: 'width 0.5s ease',
                    }}
                  />
                </div>
              </div>
            </div>

            <div className="p-3 rounded-3 bg-surface border border-subtle text-start small mb-4">
              <div className="d-flex align-items-start gap-2 mb-2">
                <AlertCircle size={18} className="text-warning flex-shrink-0 mt-1" />
                <div>
                  <strong className="text-main">Lưu ý quan trọng: Không tải lại (Refresh) trình duyệt</strong>
                  <div className="text-muted">
                    Phiên xếp hàng của bạn được đồng bộ tự động mỗi 2 giây. Khi tới lượt, bạn được cấp chứng chỉ <code>X-Queue-Pass</code> trong 5 phút.
                  </div>
                </div>
              </div>
              <div className="d-flex align-items-center gap-2 text-muted pt-2 border-top border-subtle">
                <ShieldCheck size={16} className="text-success flex-shrink-0" />
                <span>Chống bot tự động • Xếp hàng FIFO công bằng 100%</span>
              </div>
            </div>

            <Link to={`/events/${eventSlug}`} className="btn btn-tr-ghost small text-muted text-decoration-none">
              Rời khỏi hàng chờ &amp; quay lại trang sự kiện
            </Link>
          </>
        ) : (
          <div className="py-4">
            <div
              className="d-inline-flex p-3 rounded-circle mb-3"
              style={{ backgroundColor: 'var(--color-success-bg)', color: 'var(--color-success)' }}
            >
              <CheckCircle2 size={56} />
            </div>
            <div className="mb-2">
              <span className="tr-badge badge-success">
                <Sparkles size={13} /> ĐÃ CẤP QUYỀN TRUY CẬP X-QUEUE-PASS
              </span>
            </div>
            <h2 className="fs-4 fw-bold text-main mb-2">Mời bạn vào khu vực chọn &amp; đặt vé!</h2>
            <p className="text-muted small mb-4">
              Hệ thống đang tự động chuyển hướng bạn vào trang thanh toán...
            </p>
            <Link
              to={sessionStorage.getItem('ticketrush.checkout') ? '/checkout' : `/events/${eventSlug}`}
              className="btn btn-tr-primary px-4 py-2 text-decoration-none"
            >
              <span>Vào đặt vé ngay</span>
              <ArrowRight size={18} />
            </Link>
          </div>
        )}
      </div>
    </div>
  );
};
