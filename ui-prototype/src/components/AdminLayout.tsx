import React from 'react';
import { NavLink, Outlet, Link } from 'react-router-dom';
import { LayoutDashboard, Calendar, Layers, ShoppingBag, CreditCard, QrCode, ArrowLeft, Ticket } from 'lucide-react';
import { DemoBanner } from './DataStates';

export const AdminLayout: React.FC = () => {
  return (
    <div className="d-flex min-vh-100 bg-app">
      {/* Sidebar */}
      <aside className="d-flex flex-column flex-shrink-0 p-3 bg-surface border-end border-subtle" style={{ width: 260 }}>
        <div className="d-flex align-items-center gap-2 mb-4 px-2">
          <div className="rounded-2 p-1 d-flex align-items-center justify-content-center" style={{ backgroundColor: 'var(--color-primary)', color: '#fff' }}>
            <Ticket size={22} />
          </div>
          <div>
            <div className="fw-bold text-main fs-6 lh-1">TicketRush</div>
            <span className="badge bg-secondary-subtle text-secondary small" style={{ fontSize: '0.65rem' }}>BÀN QUẢN TRỊ BTC</span>
          </div>
        </div>

        <nav className="nav nav-pills flex-column gap-1 flex-grow-1">
          <NavLink
            to="/admin"
            end
            className={({ isActive }) =>
              `nav-link d-flex align-items-center gap-3 px-3 py-2 fw-medium ${
                isActive ? 'active bg-primary text-white' : 'text-muted'
              }`
            }
          >
            <LayoutDashboard size={18} />
            <span>Tổng quan (KPIs)</span>
          </NavLink>

          <NavLink
            to="/admin/events"
            className={({ isActive }) =>
              `nav-link d-flex align-items-center gap-3 px-3 py-2 fw-medium ${
                isActive ? 'active bg-primary text-white' : 'text-muted'
              }`
            }
          >
            <Calendar size={18} />
            <span>Quản lý sự kiện</span>
          </NavLink>

          <NavLink
            to="/admin/inventory"
            className={({ isActive }) =>
              `nav-link d-flex align-items-center gap-3 px-3 py-2 fw-medium ${
                isActive ? 'active bg-primary text-white' : 'text-muted'
              }`
            }
          >
            <Layers size={18} />
            <span>Giám sát kho vé</span>
          </NavLink>

          <NavLink
            to="/admin/orders"
            className={({ isActive }) =>
              `nav-link d-flex align-items-center gap-3 px-3 py-2 fw-medium ${
                isActive ? 'active bg-primary text-white' : 'text-muted'
              }`
            }
          >
            <ShoppingBag size={18} />
            <span>Đơn hàng & Vé</span>
          </NavLink>

          <NavLink
            to="/admin/payments"
            className={({ isActive }) =>
              `nav-link d-flex align-items-center gap-3 px-3 py-2 fw-medium ${
                isActive ? 'active bg-primary text-white' : 'text-muted'
              }`
            }
          >
            <CreditCard size={18} />
            <span>Thanh toán & Webhook</span>
          </NavLink>

          <NavLink
            to="/admin/check-in"
            className={({ isActive }) =>
              `nav-link d-flex align-items-center gap-3 px-3 py-2 fw-medium ${
                isActive ? 'active bg-primary text-white' : 'text-muted'
              }`
            }
          >
            <QrCode size={18} />
            <span>Soát vé tại cổng</span>
          </NavLink>
        </nav>

        <div className="pt-3 border-top border-subtle">
          <Link to="/" className="btn btn-sm btn-tr-secondary w-100 d-flex align-items-center justify-content-center gap-2">
            <ArrowLeft size={16} /> Quay lại trang mua vé
          </Link>
        </div>
      </aside>

      {/* Main Admin Area */}
      <div className="flex-grow-1 d-flex flex-column overflow-hidden">
        <header className="bg-surface border-bottom border-subtle px-4 py-3 d-flex align-items-center justify-content-between shadow-sm">
          <div className="d-flex align-items-center gap-2">
            <span className="badge bg-success-subtle text-success border border-success-subtle px-2 py-1">
              Hệ thống trực tuyến • 100% Sẵn sàng
            </span>
          </div>
          <div className="d-flex align-items-center gap-3">
            <span className="small text-muted">Múi giờ: <strong>Asia/Ho_Chi_Minh</strong></span>
            <div className="vr" />
            <span className="small fw-semibold text-main">Tài khoản: Organizer Admin</span>
          </div>
        </header>

        <main className="p-4 flex-grow-1 overflow-auto" id="admin-main">
          <DemoBanner>khu quản trị dùng dữ liệu mẫu; quản lý thật nằm ở trang JHipster admin.</DemoBanner>
          <Outlet />
        </main>
      </div>
    </div>
  );
};

