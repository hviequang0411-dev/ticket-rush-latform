import React from 'react';
import { Link } from 'react-router-dom';
import { AlertTriangle, ShieldX, Wrench, ArrowLeft } from 'lucide-react';

export const NotFoundPage: React.FC = () => (
  <div className="tr-container py-5 text-center" style={{ minHeight: '60vh' }}>
    <div className="d-inline-flex p-3 rounded-circle bg-surface-subtle text-muted mb-3">
      <AlertTriangle size={48} />
    </div>
    <h1 className="display-6 fw-bold mb-2">404 - Không tìm thấy trang</h1>
    <p className="text-muted mb-4 mx-auto" style={{ maxWidth: 440 }}>
      Trang bạn đang tìm kiếm không tồn tại, đã bị đổi tên hoặc tạm thời không khả dụng.
    </p>
    <Link to="/" className="btn btn-tr-primary d-inline-flex align-items-center gap-2">
      <ArrowLeft size={16} /> Về trang chủ TicketRush
    </Link>
  </div>
);

export const ForbiddenPage: React.FC = () => (
  <div className="tr-container py-5 text-center" style={{ minHeight: '60vh' }}>
    <div className="d-inline-flex p-3 rounded-circle bg-danger-subtle text-danger mb-3">
      <ShieldX size={48} />
    </div>
    <h1 className="display-6 fw-bold mb-2 text-danger">403 - Quyền truy cập bị từ chối</h1>
    <p className="text-muted mb-4 mx-auto" style={{ maxWidth: 440 }}>
      Bạn không có quyền quản trị viên ban tổ chức để xem khu vực này. Vui lòng đăng nhập với tài khoản có vai trò phù hợp.
    </p>
    <Link to="/" className="btn btn-tr-secondary d-inline-flex align-items-center gap-2">
      <ArrowLeft size={16} /> Trở về trang công khai
    </Link>
  </div>
);

export const MaintenancePage: React.FC = () => (
  <div className="tr-container py-5 text-center" style={{ minHeight: '60vh' }}>
    <div className="d-inline-flex p-3 rounded-circle bg-warning-subtle text-warning mb-3">
      <Wrench size={48} />
    </div>
    <h1 className="display-6 fw-bold mb-2">Hệ thống đang bảo trì định kỳ</h1>
    <p className="text-muted mb-4 mx-auto" style={{ maxWidth: 480 }}>
      TicketRush đang nâng cấp cụm máy chủ chịu tải cao để chuẩn bị cho đợt mở bán vé sắp tới. Dự kiến hoàn tất lúc 14:00 hôm nay.
    </p>
    <button onClick={() => window.location.reload()} className="btn btn-tr-primary">
      Tải lại trang kiểm tra
    </button>
  </div>
);

