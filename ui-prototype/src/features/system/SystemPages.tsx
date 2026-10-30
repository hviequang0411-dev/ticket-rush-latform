import React from 'react';
import { Link } from 'react-router-dom';
import { AlertTriangle, ShieldX, Wrench, ArrowLeft } from 'lucide-react';

export const NotFoundPage: React.FC = () => (
  <div className="tr-container py-5 d-flex align-items-center justify-content-center" style={{ minHeight: '65vh' }}>
    <div className="tr-card tr-card-featured p-4 p-md-5 text-center" style={{ maxWidth: 520 }}>
      <div
        className="d-inline-flex p-3 rounded-circle mb-3"
        style={{ backgroundColor: 'var(--bg-surface-subtle)', color: 'var(--color-primary)' }}
      >
        <AlertTriangle size={44} />
      </div>
      <div className="mb-2">
      </div>
      <h1 className="fs-3 fw-bold mb-2">Không tìm thấy trang</h1>
      <p className="text-muted small mb-4 mx-auto" style={{ maxWidth: 420 }}>
        Trang bạn đang tìm kiếm không tồn tại, đã bị đổi đường dẫn hoặc sự kiện đã kết thúc lưu trữ.
      </p>
      <Link to="/" className="btn btn-tr-primary d-inline-flex align-items-center gap-2 text-decoration-none">
        <ArrowLeft size={16} /> Về trang chủ TicketRush
      </Link>
    </div>
  </div>
);

export const ForbiddenPage: React.FC = () => (
  <div className="tr-container py-5 d-flex align-items-center justify-content-center" style={{ minHeight: '65vh' }}>
    <div className="tr-card tr-card-featured p-4 p-md-5 text-center" style={{ maxWidth: 520 }}>
      <div
        className="d-inline-flex p-3 rounded-circle mb-3"
        style={{ backgroundColor: 'var(--color-danger-bg)', color: 'var(--color-danger)' }}
      >
        <ShieldX size={44} />
      </div>
      <div className="mb-2">
      </div>
      <h1 className="fs-3 fw-bold mb-2 text-main">Bạn không có quyền xem trang này</h1>
      <p className="text-muted small mb-4 mx-auto" style={{ maxWidth: 420 }}>
        Bạn không có quyền quản trị viên Ban Tổ Chức để xem khu vực này. Vui lòng đăng nhập với tài khoản có vai trò phù hợp.
      </p>
      <Link to="/" className="btn btn-tr-secondary d-inline-flex align-items-center gap-2 text-decoration-none">
        <ArrowLeft size={16} /> Trở về trang công khai
      </Link>
    </div>
  </div>
);

export const MaintenancePage: React.FC = () => (
  <div className="tr-container py-5 d-flex align-items-center justify-content-center" style={{ minHeight: '65vh' }}>
    <div className="tr-card tr-card-featured p-4 p-md-5 text-center" style={{ maxWidth: 540 }}>
      <div
        className="d-inline-flex p-3 rounded-circle mb-3"
        style={{ backgroundColor: 'var(--color-warning-bg)', color: 'var(--color-warning)' }}
      >
        <Wrench size={44} />
      </div>
      <div className="mb-2">
        <span className="tr-badge badge-warning font-monospace">SCHEDULED MAINTENANCE</span>
      </div>
      <h1 className="fs-3 fw-bold mb-2">Hệ thống đang bảo trì định kỳ</h1>
      <p className="text-muted small mb-4 mx-auto" style={{ maxWidth: 460 }}>
        TicketRush đang nâng cấp cụm máy chủ chịu tải cao để chuẩn bị cho đợt mở bán vé sắp tới. Toàn bộ vé đã mua trong Ví vé của bạn đều được bảo toàn.
      </p>
      <button onClick={() => window.location.reload()} className="btn btn-tr-primary">
        Tải lại trang kiểm tra
      </button>
    </div>
  </div>
);
