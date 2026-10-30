import React, { useState, useEffect } from 'react';
import { Outlet, NavLink, Link, useLocation } from 'react-router-dom';
import {
  LayoutDashboard,
  CalendarRange,
  Boxes,
  ShoppingCart,
  Webhook,
  QrCode,
  ArrowLeft,
  Ticket,
  ShieldAlert,
  LogOut,
  Sun,
  Moon,
  Menu,
  X,
} from 'lucide-react';
import { displayName, isAdmin, signIn, signOut, useAccount } from '../api/auth';
import { ForbiddenPage } from '../features/system/SystemPages';

export const AdminLayout: React.FC = () => {
  const { account, loading } = useAccount();
  const location = useLocation();
  const [theme, setTheme] = useState<'light' | 'dark'>(() => {
    const saved = localStorage.getItem('ticketrush.theme');
    return saved === 'dark' ? 'dark' : 'light';
  });
  const [sidebarOpen, setSidebarOpen] = useState(false);

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('ticketrush.theme', theme);
  }, [theme]);

  useEffect(() => {
    setSidebarOpen(false);
  }, [location.pathname]);

  useEffect(() => {
    if (!loading && !account) {
      signIn(location.pathname + location.search);
    }
  }, [loading, account, location.pathname, location.search]);

  if (loading || !account) {
    return (
      <div className="min-vh-100 d-flex align-items-center justify-content-center text-muted small">
        Đang kiểm tra phiên đăng nhập quản trị...
      </div>
    );
  }

  if (!isAdmin(account)) {
    return <ForbiddenPage />;
  }

  const navItems = [
    { to: '/admin', icon: <LayoutDashboard size={18} />, label: 'Tổng quan & KPIs', end: true },
    { to: '/admin/events', icon: <CalendarRange size={18} />, label: 'Sự kiện & Loại vé' },
    { to: '/admin/inventory', icon: <Boxes size={18} />, label: 'Giám sát kho vé' },
    { to: '/admin/orders', icon: <ShoppingCart size={18} />, label: 'Quản lý đơn hàng' },
    { to: '/admin/payments', icon: <Webhook size={18} />, label: 'Thanh toán & Webhook' },
    { to: '/admin/check-in', icon: <QrCode size={18} />, label: 'Soát vé tại cổng' },
  ];

  return (
    <div className="d-flex min-vh-100 bg-surface-subtle">
      {/* Desktop Compact Sidebar */}
      <aside
        className="bg-surface border-end border-subtle d-none d-lg-flex flex-column justify-content-between p-3 sticky-top"
        style={{ width: 256, height: '100vh', flexShrink: 0 }}
      >
        <div>
          <div className="d-flex align-items-center justify-content-between px-2 py-2 mb-3 border-bottom border-subtle">
            <div className="d-flex align-items-center gap-2">
              <div
                className="rounded-3 d-flex align-items-center justify-content-center text-white shadow-xs"
                style={{
                  width: 34,
                  height: 34,
                  background: 'linear-gradient(135deg, var(--color-primary) 0%, #c2410c 100%)',
                }}
              >
                <Ticket size={18} />
              </div>
              <div>
                <div className="fw-bold text-main lh-1" style={{ fontFamily: 'var(--font-display)' }}>
                  TicketRush
                </div>
                <small className="text-primary fw-semibold font-monospace" style={{ fontSize: '0.68rem', letterSpacing: '0.04em' }}>
                  ORGANIZER CONSOLE
                </small>
              </div>
            </div>
          </div>

          <div className="px-2 mb-2 text-muted fw-semibold text-uppercase" style={{ fontSize: '0.68rem', letterSpacing: '0.06em' }}>
            Điều hành hệ thống
          </div>

          <nav className="d-flex flex-column gap-1" aria-label="Điều hướng quản trị">
            {navItems.map(item => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.end}
                className={({ isActive }) =>
                  `d-flex align-items-center gap-2 px-3 py-2 rounded-3 text-decoration-none small transition-all ${
                    isActive
                      ? 'bg-primary text-white fw-semibold shadow-xs'
                      : 'text-muted hover-surface fw-medium'
                  }`
                }
              >
                {item.icon}
                <span>{item.label}</span>
              </NavLink>
            ))}
          </nav>
        </div>

        <div className="border-top border-subtle pt-3">
          <div className="p-2 rounded-3 bg-surface-subtle border border-subtle mb-2 small">
            <div className="d-flex align-items-center gap-2 text-warning fw-semibold mb-1">
              <ShieldAlert size={15} />
              <span style={{ fontSize: '0.78rem' }}>Chế độ vận hành trực tiếp</span>
            </div>
            <p className="text-muted mb-0" style={{ fontSize: '0.72rem' }}>
              Thao tác huỷ sự kiện hoặc điều chỉnh kho vé được ghi nhận vào Audit Log.
            </p>
          </div>
          <Link to="/" className="btn-tr-ghost w-100 justify-content-start text-decoration-none small">
            <ArrowLeft size={16} />
            <span>Trở về trang người mua</span>
          </Link>
        </div>
      </aside>

      {/* Main Content Wrapper */}
      <div className="flex-grow-1 d-flex flex-column min-w-0">
        {/* Topbar */}
        <header className="bg-surface border-bottom border-subtle py-2 px-3 px-md-4 d-flex align-items-center justify-content-between sticky-top" style={{ zIndex: 1010 }}>
          <div className="d-flex align-items-center gap-2">
            <button
              type="button"
              className="btn-tr-ghost btn-touch-target d-lg-none p-2"
              aria-label="Mở menu quản trị"
              onClick={() => setSidebarOpen(prev => !prev)}
            >
              {sidebarOpen ? <X size={20} /> : <Menu size={20} />}
            </button>
            <Link to="/" className="d-lg-none btn-tr-secondary py-1 px-2 small text-decoration-none">
              <ArrowLeft size={16} /> Trang mua vé
            </Link>
            <span className="tr-badge badge-success">
              Cụm máy chủ: Sẵn sàng (99.98% Uptime)
            </span>
          </div>

          <div className="d-flex align-items-center gap-2">
            <button
              type="button"
              onClick={() => setTheme(prev => (prev === 'light' ? 'dark' : 'light'))}
              className="btn-tr-ghost btn-touch-target rounded-circle p-2"
              aria-label={theme === 'light' ? 'Chuyển sang giao diện tối' : 'Chuyển sang giao diện sáng'}
              title={theme === 'light' ? 'Chế độ tối' : 'Chế độ sáng'}
            >
              {theme === 'light' ? <Moon size={17} /> : <Sun size={17} className="text-warning" />}
            </button>
            <span className="small text-muted d-none d-sm-inline">
              Xin chào, <strong className="text-main">{displayName(account)}</strong>
            </span>
            <button
              type="button"
              className="btn btn-sm btn-tr-ghost p-2"
              title="Đăng xuất"
              aria-label="Đăng xuất"
              onClick={() => signOut()}
            >
              <LogOut size={16} />
            </button>
          </div>
        </header>

        {/* Mobile Admin Drawer */}
        {sidebarOpen && (
          <div className="d-lg-none bg-surface border-bottom border-subtle p-3 shadow-md">
            <nav className="d-flex flex-column gap-1">
              {navItems.map(item => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end={item.end}
                  className={({ isActive }) =>
                    `d-flex align-items-center gap-2 px-3 py-2 rounded-3 text-decoration-none small ${
                      isActive ? 'bg-primary text-white fw-semibold' : 'text-main'
                    }`
                  }
                >
                  {item.icon}
                  <span>{item.label}</span>
                </NavLink>
              ))}
            </nav>
          </div>
        )}

        <main className="p-3 p-md-4 flex-grow-1">
          <div key={location.pathname} className="tr-page-enter">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
};
