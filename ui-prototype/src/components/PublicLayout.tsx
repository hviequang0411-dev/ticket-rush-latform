import React, { useState, useEffect } from 'react';
import { Outlet, Link, NavLink, useLocation, useNavigate } from 'react-router-dom';
import { Ticket, User, Moon, Sun, LogIn, LogOut, Menu, X } from 'lucide-react';
import { consumeReturnTo, displayName, isAdmin, signIn, signOut, useAccount } from '../api/auth';

const NAV_ITEMS = [
  { to: '/events', label: 'Sự kiện' },
  { to: '/my-orders', label: 'Đơn hàng' },
  { to: '/wallet', label: 'Ví vé' },
];

const Brand: React.FC<{ size?: 'md' | 'sm' }> = ({ size = 'md' }) => (
  <span className="d-inline-flex align-items-center gap-2">
    <span className={`tr-brand-mark ${size === 'sm' ? 'tr-brand-mark-sm' : ''}`} aria-hidden="true">
      <Ticket size={size === 'sm' ? 15 : 18} />
    </span>
    <span className="tr-brand-word">
      Ticket<span className="text-primary">Rush</span>
    </span>
  </span>
);

export const PublicLayout: React.FC = () => {
  const [theme, setTheme] = useState<'light' | 'dark'>(() => {
    const saved = localStorage.getItem('ticketrush.theme');
    return saved === 'dark' ? 'dark' : 'light';
  });
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const { account } = useAccount();
  const location = useLocation();
  const navigate = useNavigate();

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('ticketrush.theme', theme);
  }, [theme]);

  useEffect(() => {
    setMobileMenuOpen(false);
  }, [location.pathname]);

  // After Keycloak redirects back to "/", jump to the page that asked for login.
  useEffect(() => {
    if (!account) return;
    const returnTo = consumeReturnTo();
    if (returnTo && returnTo !== location.pathname + location.search) {
      navigate(returnTo, { replace: true });
    }
  }, [account, location.pathname, location.search, navigate]);

  const toggleTheme = () => {
    setTheme(prev => (prev === 'light' ? 'dark' : 'light'));
  };

  const organiser = !!account && isAdmin(account);

  return (
    <div className="d-flex flex-column min-vh-100">
      <header className="tr-header sticky-top">
        <div className="tr-container d-flex align-items-center gap-4 tr-header-row">
          <Link to="/" className="text-decoration-none flex-shrink-0" aria-label="TicketRush, trang chủ">
            <Brand />
          </Link>

          <nav className="d-none d-lg-flex align-items-center gap-1 flex-grow-1" aria-label="Điều hướng chính">
            {NAV_ITEMS.map(item => (
              <NavLink key={item.to} to={item.to} className={({ isActive }) => `tr-nav-link ${isActive ? 'active' : ''}`}>
                {item.label}
              </NavLink>
            ))}
          </nav>

          <div className="d-flex align-items-center gap-2 ms-auto flex-shrink-0">
            <button
              onClick={toggleTheme}
              className="btn-tr-ghost btn-touch-target rounded-circle p-2"
              aria-label={theme === 'light' ? 'Chuyển sang giao diện tối' : 'Chuyển sang giao diện sáng'}
              title={theme === 'light' ? 'Giao diện tối' : 'Giao diện sáng'}
            >
              {theme === 'light' ? <Moon size={18} /> : <Sun size={18} />}
            </button>

            {organiser && (
              <Link to="/admin" className="tr-nav-link d-none d-md-inline-flex">
                Ban tổ chức
              </Link>
            )}

            {account ? (
              <div className="d-none d-sm-inline-flex align-items-center gap-1">
                <Link to="/account" className="tr-account-chip text-decoration-none" title={account.email}>
                  <span className="tr-avatar" aria-hidden="true">
                    {displayName(account).charAt(0).toUpperCase()}
                  </span>
                  <span className="text-truncate" style={{ maxWidth: 140 }}>
                    {displayName(account)}
                  </span>
                </Link>
                <button
                  type="button"
                  onClick={() => signOut()}
                  className="btn-tr-ghost btn-touch-target p-2"
                  aria-label="Đăng xuất"
                  title="Đăng xuất"
                >
                  <LogOut size={18} />
                </button>
              </div>
            ) : (
              <button type="button" onClick={() => signIn()} className="btn-tr-primary d-none d-sm-inline-flex text-nowrap">
                <LogIn size={16} />
                <span>Đăng nhập</span>
              </button>
            )}

            <button
              type="button"
              onClick={() => setMobileMenuOpen(prev => !prev)}
              className="btn-tr-ghost btn-touch-target d-lg-none p-2"
              aria-label={mobileMenuOpen ? 'Đóng menu' : 'Mở menu'}
              aria-expanded={mobileMenuOpen}
            >
              {mobileMenuOpen ? <X size={22} /> : <Menu size={22} />}
            </button>
          </div>
        </div>

        {mobileMenuOpen && (
          <div className="d-lg-none tr-mobile-menu">
            <nav className="d-flex flex-column gap-1 mb-3" aria-label="Điều hướng di động">
              {[{ to: '/', label: 'Trang chủ', end: true }, ...NAV_ITEMS].map(item => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end={'end' in item}
                  className={({ isActive }) => `tr-mobile-link ${isActive ? 'active' : ''}`}
                >
                  {item.label}
                </NavLink>
              ))}
              {organiser && (
                <NavLink to="/admin" className={({ isActive }) => `tr-mobile-link ${isActive ? 'active' : ''}`}>
                  Ban tổ chức
                </NavLink>
              )}
            </nav>
            <div className="pt-3 border-top border-subtle">
              {account ? (
                <div className="d-flex gap-2">
                  <Link to="/account" className="btn-tr-secondary flex-grow-1 text-decoration-none">
                    <User size={16} /> {displayName(account)}
                  </Link>
                  <button type="button" onClick={() => signOut()} className="btn-tr-ghost">
                    <LogOut size={16} /> Đăng xuất
                  </button>
                </div>
              ) : (
                <button type="button" onClick={() => signIn()} className="btn-tr-primary w-100">
                  <LogIn size={16} /> Đăng nhập
                </button>
              )}
            </div>
          </div>
        )}
      </header>

      <main className="flex-grow-1">
        <div key={location.pathname} className="tr-page-enter">
          <Outlet />
        </div>
      </main>

      <footer className="tr-footer">
        <div className="tr-container">
          <div className="row g-4">
            <div className="col-12 col-md-5">
              <Brand size="sm" />
              <p className="text-muted small mt-3 mb-0" style={{ maxWidth: 360 }}>
                Vé concert, lễ hội, sân khấu và thể thao tại Hà Nội, TP. Hồ Chí Minh và Đà Nẵng. Giá niêm yết rõ ràng,
                ghế được giữ cho bạn 10 phút trong lúc thanh toán.
              </p>
            </div>
            <div className="col-6 col-md-2">
              <h6 className="tr-footer-title">Khám phá</h6>
              <ul className="list-unstyled d-flex flex-column gap-2 small mb-0">
                <li><Link to="/events" className="tr-footer-link">Tất cả sự kiện</Link></li>
                <li><Link to="/events?category=CONCERT" className="tr-footer-link">Concert</Link></li>
                <li><Link to="/events?category=FESTIVAL" className="tr-footer-link">Lễ hội</Link></li>
                <li><Link to="/events?category=THEATER" className="tr-footer-link">Sân khấu</Link></li>
              </ul>
            </div>
            <div className="col-6 col-md-2">
              <h6 className="tr-footer-title">Tài khoản</h6>
              <ul className="list-unstyled d-flex flex-column gap-2 small mb-0">
                <li><Link to="/my-orders" className="tr-footer-link">Đơn hàng của tôi</Link></li>
                <li><Link to="/wallet" className="tr-footer-link">Ví vé</Link></li>
                <li><Link to="/account" className="tr-footer-link">Hồ sơ</Link></li>
              </ul>
            </div>
            <div className="col-12 col-md-3">
              <h6 className="tr-footer-title">Ban tổ chức</h6>
              <p className="text-muted small mb-2">Tạo sự kiện, theo dõi bán vé và soát vé tại cổng.</p>
              <Link to="/admin" className="tr-footer-link small fw-semibold">
                Vào trang quản trị →
              </Link>
            </div>
          </div>
          <div className="tr-footer-bottom">
            <span>© 2026 TicketRush</span>
            <Link to="/design-system" className="tr-footer-link">Design system</Link>
          </div>
        </div>
      </footer>
    </div>
  );
};
