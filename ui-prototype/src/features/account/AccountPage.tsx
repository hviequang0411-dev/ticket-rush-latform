import React from 'react';
import { User, Shield, LogOut } from 'lucide-react';
import { displayName, isAdmin, signIn, signOut, useAccount } from '../../api/auth';

// The profile lives in Keycloak; this page only shows it.
export const AccountPage: React.FC = () => {
  const { account, loading } = useAccount();

  if (loading) {
    return (
      <div className="tr-container py-4" style={{ maxWidth: 800 }}>
        <div className="tr-skeleton w-50 mb-3" style={{ height: 32 }} />
        <div className="tr-skeleton w-100 rounded-3" style={{ height: 200 }} />
      </div>
    );
  }

  if (!account) {
    return (
      <div className="tr-container py-5 d-flex justify-content-center">
        <div className="tr-card p-4 p-md-5 text-center" style={{ maxWidth: 520 }}>
          <User size={40} className="text-primary mb-3" />
          <h1 className="fs-4 fw-bold mb-2">Bạn chưa đăng nhập</h1>
          <button type="button" className="btn btn-tr-primary w-100 py-2 mt-2" onClick={() => signIn('/account')}>
            Đăng nhập hoặc tạo tài khoản
          </button>
        </div>
      </div>
    );
  }

  const rows: [string, string][] = [
    ['Họ và tên', displayName(account)],
    ['Tên đăng nhập', account.login],
    ['Email', account.email || '—'],
    ['Vai trò', isAdmin(account) ? 'Quản trị viên' : 'Người mua'],
  ];

  return (
    <div className="tr-container py-4" style={{ maxWidth: 800 }}>
      <div className="mb-4">
        <h1 className="fs-3 fw-bold mb-1">Tài khoản</h1>
        <p className="text-muted small mb-0">Thông tin đăng nhập do Keycloak quản lý</p>
      </div>

      <div className="d-flex flex-column gap-4">
        <div className="tr-card p-4">
          <div className="d-flex align-items-center gap-2 mb-3 pb-2 border-bottom border-subtle">
            <User size={20} className="text-primary" />
            <h2 className="fs-5 fw-bold mb-0">Hồ sơ cá nhân</h2>
          </div>
          <dl className="row g-2 mb-0 small">
            {rows.map(([label, value]) => (
              <React.Fragment key={label}>
                <dt className="col-12 col-sm-4 text-muted fw-normal">{label}</dt>
                <dd className="col-12 col-sm-8 fw-semibold text-main mb-0">{value}</dd>
              </React.Fragment>
            ))}
          </dl>
        </div>

        <div className="p-3 bg-surface-subtle rounded-3 border border-subtle d-flex align-items-center gap-3">
          <Shield size={24} className="text-success flex-shrink-0" />
          <div className="small text-muted">
            Đăng nhập qua Keycloak (OIDC). Trình duyệt chỉ giữ cookie phiên; token nằm ở gateway.
          </div>
        </div>

        <button type="button" className="btn btn-tr-secondary align-self-start px-4 d-inline-flex align-items-center gap-2" onClick={() => signOut()}>
          <LogOut size={16} /> Đăng xuất
        </button>
      </div>
    </div>
  );
};
