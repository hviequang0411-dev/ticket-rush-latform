import React, { useState, useEffect } from 'react';
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { Ticket, Sun, Moon, User, ShieldCheck } from 'lucide-react';
import { consumeReturnTo, displayName, isAdmin, signIn, signOut, useAccount } from '../api/auth';

export const PublicLayout: React.FC = () => {
  const [theme, setTheme] = useState<'light' | 'dark'>('light');
  const { account, loading } = useAccount();
  const navigate = useNavigate();

  // Back from Keycloak: return to the page the user signed in from
  useEffect(() => {
    if (!account) return;
    const returnTo = consumeReturnTo();
    if (returnTo && returnTo !== window.location.pathname + window.location.search) navigate(returnTo, { replace: true });
  }, [account, navigate]);

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
  }, [theme]);

  const toggleTheme = () => {
    setTheme(prev => (prev === 'light' ? 'dark' : 'light'));
  };

  return (
    <div className="d-flex flex-column min-vh-100">
      {/* Top Banner / Announcement */}
      <aside aria-label="Thông báo hệ thống" className="bg-dark text-white-50 py-1 px-3 small text-center border-bottom border-secondary border-opacity-25" style={{ fontSize: '0.8125rem' }}>
        <span>Hệ thống vé điện tử thời gian thực TicketRush • Bảo vệ đặt chỗ &amp; phòng chống vé chợ đen</span>
      </aside>

      {/* Main Header */}
      <header className="sticky-top bg-surface border-bottom border-subtle shadow-sm z-3" style={{ backdropFilter: 'blur(8px)', backgroundColor: 'var(--bg-surface)' }}>
        <div className="tr-container d-flex align-items-center justify-content-between py-2" style={{ minHeight: '64px' }}>
          {/* Logo */}
          <Link to="/" className="d-flex align-items-center gap-2 text-decoration-none" aria-label="TicketRush Trang chủ">
            <div className="rounded-2 p-1 d-flex align-items-center justify-content-center" style={{ backgroundColor: 'var(--color-primary)', color: '#fff' }}>
              <Ticket size={24} />
            </div>
            <span className="fw-bold fs-4 text-main tracking-tight" style={{ letterSpacing: '-0.5px' }}>
              Ticket<span style={{ color: 'var(--color-primary)' }}>Rush</span>
            </span>
          </Link>

          {/* Navigation Links */}
          <nav className="d-none d-md-flex align-items-center gap-4">
            <NavLink to="/" end className={({ isActive }) => `fw-medium text-decoration-none ${isActive ? 'text-primary' : 'text-muted'}`}>
              Khám phá
            </NavLink>
            <NavLink to="/events" className={({ isActive }) => `fw-medium text-decoration-none ${isActive ? 'text-primary' : 'text-muted'}`}>
              Sự kiện
            </NavLink>
            <NavLink to="/my-orders" className={({ isActive }) => `fw-medium text-decoration-none ${isActive ? 'text-primary' : 'text-muted'}`}>
              Đơn hàng của tôi
            </NavLink>
            <NavLink to="/wallet" className={({ isActive }) => `fw-medium text-decoration-none ${isActive ? 'text-primary' : 'text-muted'}`}>
              Ví vé
            </NavLink>
            <NavLink to="/design-system" className={({ isActive }) => `fw-medium text-decoration-none ${isActive ? 'text-primary' : 'text-muted'}`}>
              Design System
            </NavLink>
          </nav>

          {/* User actions & Theme switch */}
          <div className="d-flex align-items-center gap-2">
            <button
              onClick={toggleTheme}
              className="btn btn-tr-ghost p-2"
              title="Chuyển chế độ sáng/tối"
              aria-label="Chuyển chế độ sáng hoặc tối"
            >
              {theme === 'light' ? <Moon size={18} /> : <Sun size={18} />}
            </button>

            {isAdmin(account) && (
              <Link to="/admin" className="btn btn-sm btn-tr-secondary d-none d-lg-inline-flex align-items-center gap-1" title="Khu vực quản trị">
                <ShieldCheck size={16} /> Ban tổ chức
              </Link>
            )}

            {loading ? null : account ? (
              <div className="d-flex align-items-center gap-2 ps-2 border-start border-subtle">
                <Link to="/account" className="btn btn-sm btn-tr-ghost d-flex align-items-center gap-2 px-2" aria-label="Tài khoản cá nhân">
                  <div className="rounded-circle bg-primary-subtle text-primary p-1 d-flex align-items-center justify-content-center" style={{ width: 32, height: 32 }}>
                    <User size={16} />
                  </div>
                  <span className="d-none d-sm-inline fw-medium text-main small">{displayName(account)}</span>
                </Link>
                <button
                  onClick={() => signOut()}
                  className="btn btn-sm btn-link text-muted p-1 text-decoration-none small"
                  style={{ fontSize: '0.8rem' }}
                >
                  Thoát
                </button>
              </div>
            ) : (
              <button
                onClick={() => signIn()}
                className="btn btn-sm btn-tr-primary"
              >
                Đăng nhập
              </button>
            )}
          </div>
        </div>
      </header>

      {/* Main Content View */}
      <main className="flex-grow-1" id="main-content">
        <Outlet />
      </main>

      {/* Modern Footer */}
      <footer className="bg-surface border-top border-subtle mt-5 py-5 text-muted">
        <div className="tr-container">
          <div className="row g-4 justify-content-between">
            <div className="col-12 col-md-4">
              <div className="d-flex align-items-center gap-2 mb-3">
                <div className="rounded-2 p-1 d-flex align-items-center justify-content-center" style={{ backgroundColor: 'var(--color-primary)', color: '#fff' }}>
                  <Ticket size={20} />
                </div>
                <span className="fw-bold fs-5 text-main">TicketRush</span>
              </div>
              <p className="small mb-3">
                Nền tảng công nghệ phân phối vé sự kiện và hòa nhạc chịu tải cao hàng đầu Việt Nam. Tối ưu thời gian thực, chống đầu cơ và hỗ trợ soát vé an toàn qua mã QR bảo mật động.
              </p>
              <div className="small text-subtle">
                © 2026 TicketRush VN Inc. Mọi quyền được bảo lưu.
              </div>
            </div>

            <div className="col-6 col-md-2">
              <h6 className="text-main fw-semibold mb-3">Dành cho bạn</h6>
              <ul className="list-unstyled small d-flex flex-column gap-2 mb-0">
                <li><Link to="/events" className="text-muted text-decoration-none">Tìm kiếm sự kiện</Link></li>
                <li><Link to="/wallet" className="text-muted text-decoration-none">Ví vé điện tử</Link></li>
                <li><Link to="/my-orders" className="text-muted text-decoration-none">Lịch sử đơn hàng</Link></li>
                <li><Link to="/account" className="text-muted text-decoration-none">Cài đặt tài khoản</Link></li>
              </ul>
            </div>

            <div className="col-6 col-md-2">
              <h6 className="text-main fw-semibold mb-3">Tổ chức sự kiện</h6>
              <ul className="list-unstyled small d-flex flex-column gap-2 mb-0">
                <li><Link to="/admin" className="text-muted text-decoration-none">Bảng điều khiển</Link></li>
                <li><Link to="/admin/check-in" className="text-muted text-decoration-none">Soát vé tại cổng</Link></li>
                <li><Link to="/admin/events" className="text-muted text-decoration-none">Tạo sự kiện mới</Link></li>
                <li><Link to="/admin/inventory" className="text-muted text-decoration-none">Giám sát tồn kho</Link></li>
              </ul>
            </div>

            <div className="col-12 col-md-3">
              <h6 className="text-main fw-semibold mb-3">Hỗ trợ khách hàng</h6>
              <p className="small mb-2">Đội ngũ kỹ thuật hỗ trợ trực tiếp 24/7 trong các đợt mở bán vé cao điểm.</p>
              <div className="fw-medium text-main small mb-1">Hotline: 1900 6868 (8:00 - 22:00)</div>
              <div className="small text-muted">Email: hotro@ticketrush.vn</div>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
};
