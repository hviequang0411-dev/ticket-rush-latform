import React, { useEffect, useState } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import { Users, ShieldCheck, CheckCircle2 } from 'lucide-react';
import { ApiService, type QueueStatus } from '../../api/apiClient';
import { signIn, useAccount } from '../../api/auth';
import type { ApiErrorProblem } from '../../types/models';
import { ErrorState } from '../../components/DataStates';

const POLL_MS = 2000;

/**
 * The waiting room of a hot sale. The place is kept server-side per account, so reloading or opening another tab
 * does not lose it. Once admitted the buyer gets a pass (valid ~10 minutes) and goes on to checkout with the seats
 * chosen before queueing.
 */
export const WaitingRoomPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const eventId = Number(searchParams.get('event'));
  const slug = searchParams.get('slug') ?? '';
  const { account, loading } = useAccount();

  const [status, setStatus] = useState<QueueStatus | null>(null);
  const [error, setError] = useState<ApiErrorProblem | null>(null);
  // Where the buyer started, for the progress bar
  const [firstAhead, setFirstAhead] = useState<number | null>(null);

  useEffect(() => {
    if (!account || !eventId) return;
    let active = true;
    let timer: ReturnType<typeof setTimeout>;
    const step = async (join: boolean) => {
      try {
        const next = join ? await ApiService.joinQueue(eventId) : await ApiService.queueStatus(eventId);
        if (!active) return;
        setFirstAhead(first => first ?? next.ahead);
        setStatus(next);
        setError(null);
        if (next.admitted) {
          timer = setTimeout(() => navigate('/checkout'), 1200);
          return;
        }
      } catch (e: unknown) {
        if (active) setError(e as ApiErrorProblem);
      }
      timer = setTimeout(() => step(false), POLL_MS);
    };
    step(true);
    return () => {
      active = false;
      clearTimeout(timer);
    };
  }, [account, eventId, navigate]);

  if (!eventId) {
    return (
      <div className="tr-container py-5">
        <ErrorState title="Không rõ sự kiện" message="Hãy vào phòng chờ từ trang sự kiện." status={400} />
        <Link to="/events" className="btn btn-tr-primary mt-3">Xem sự kiện</Link>
      </div>
    );
  }

  if (!loading && !account) {
    return (
      <div className="tr-container py-5 d-flex justify-content-center">
        <div className="tr-card p-4 p-md-5 text-center" style={{ maxWidth: 520 }}>
          <Users size={40} className="text-primary mb-3" />
          <h1 className="fs-4 fw-bold mb-2">Đăng nhập để vào hàng chờ</h1>
          <p className="text-muted mb-4">Mỗi tài khoản giữ một chỗ trong hàng; lựa chọn vé của bạn vẫn được giữ.</p>
          <button className="btn btn-tr-primary w-100 py-2" onClick={() => signIn(`/queue?event=${eventId}&slug=${encodeURIComponent(slug)}`)}>
            Đăng nhập hoặc tạo tài khoản
          </button>
        </div>
      </div>
    );
  }

  const ahead = status?.ahead ?? 0;
  const start = Math.max(firstAhead ?? ahead, 1);
  const progress = status?.admitted ? 100 : Math.min(99, Math.round(((start - ahead) / start) * 100));

  return (
    <div className="tr-container py-5 d-flex justify-content-center align-items-center" style={{ minHeight: '70vh' }}>
      <div className="tr-card p-4 p-md-5 shadow-md text-center bg-surface" style={{ maxWidth: 580, width: '100%' }}>
        <div className={`d-inline-flex align-items-center justify-content-center p-3 rounded-circle mb-3 ${status?.admitted ? 'bg-success-subtle text-success' : 'bg-primary-subtle text-primary'}`}>
          {status?.admitted ? <CheckCircle2 size={36} /> : <Users size={36} />}
        </div>

        <h1 className="fs-3 fw-bold mb-2">{status?.admitted ? 'Đến lượt bạn!' : 'Phòng chờ TicketRush'}</h1>
        <p className="text-muted small mb-4">
          {status?.admitted
            ? 'Đang chuyển sang thanh toán. Lượt vào có hiệu lực khoảng 10 phút.'
            : 'Đợt mở bán đang rất đông. Mọi người được cho vào đặt vé theo đúng thứ tự xếp hàng, với tốc độ hệ thống chịu được.'}
        </p>

        {error && <div className="alert alert-danger small text-start">{error.detail}. Đang thử lại...</div>}

        <div className="row g-3 mb-4" aria-live="polite">
          <div className="col-6">
            <div className="p-3 bg-surface-subtle rounded-3 border border-subtle">
              <div className="small text-muted mb-1">Người đứng trước bạn</div>
              <div className="fs-3 fw-bold text-primary tabular-nums">{status ? (status.admitted ? 0 : ahead.toLocaleString('vi-VN')) : '…'}</div>
            </div>
          </div>
          <div className="col-6">
            <div className="p-3 bg-surface-subtle rounded-3 border border-subtle">
              <div className="small text-muted mb-1">Thời gian chờ ước tính</div>
              <div className="fs-3 fw-bold text-main tabular-nums">{status ? (status.admitted ? '0s' : `~${status.etaSeconds}s`) : '…'}</div>
            </div>
          </div>
        </div>

        <div className="mb-4 text-start">
          <div className="d-flex justify-content-between small text-muted mb-1">
            <span>Tiến độ hàng chờ</span>
            <span className="fw-semibold">{progress}%</span>
          </div>
          <div className="progress" style={{ height: 8 }} role="progressbar" aria-valuenow={progress} aria-valuemin={0} aria-valuemax={100} aria-label="Tiến độ hàng chờ">
            <div className="progress-bar bg-primary progress-bar-striped progress-bar-animated" style={{ width: `${progress}%` }} />
          </div>
        </div>

        <div className="small text-muted mb-4">Tải lại trang hay mở tab khác không làm mất chỗ: vị trí được giữ theo tài khoản.</div>

        <div className="d-flex align-items-center justify-content-center gap-2 small text-muted">
          <ShieldCheck size={16} className="text-success" />
          <span>Không có lượt vào thì không đặt được vé, kể cả khi gọi thẳng API</span>
        </div>
        {slug && (
          <Link to={`/events/${slug}`} className="btn btn-sm btn-link text-muted mt-3">
            Rời hàng chờ, quay lại sự kiện
          </Link>
        )}
      </div>
    </div>
  );
};
