import React from 'react';
import { User, Shield, LogOut, KeyRound } from 'lucide-react';
import { displayName, isAdmin, signIn, signOut, useAccount } from '../../api/auth';

// The profile lives in Keycloak; this page only shows it.
export const AccountPage: React.FC = () => {
  const { account, loading } = useAccount();

  if (loading) {
    return (
      <div className="tr-container py-4" style={{ maxWidth: 800 }}>
        <div className="tr-skeleton w-50 mb-3" style={{ height: 32 }} />
        <div className="tr-skeleton w-100 rounded-3" style={{ height: 220 }} />
      </div>
    );
  }

  if (!account) {
    return (
      <div className="tr-container py-5 d-flex justify-content-center">
        <div className="tr-card tr-card-featured p-4 p-md-5 text-center" style={{ maxWidth: 520 }}>
          <div
            className="d-inline-flex p-3 rounded-circle mb-3"
            style={{ backgroundColor: 'var(--color-primary-subtle)', color: 'var(--color-primary)' }}
          >
            <User size={36} />
          </div>
          <h1 className="fs-4 fw-bold mb-2">Bạn chưa đăng nhập</h1>
          <p className="text-muted small mb-4">
            Đăng nhập để quản lý đơn hàng và ví vé điện tử của bạn.
          </p>
          <button type="button" className="btn btn-tr-primary w-100 py-2" onClick={() => signIn('/account')}>
            <KeyRound size={16} />
            <span>Đăng nhập hoặc tạo tài khoản</span>
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
        <h1 className="fs-3 fw-bold mb-1">Tài khoản cá nhân</h1>
        <p className="text-muted small mb-0">Thông tin đăng nhập và tuỳ chọn của bạn</p>
      </div>

      <div className="d-flex flex-column gap-4">
        <div className="tr-card p-4">
          <div className="d-flex align-items-center gap-3 mb-4 pb-3 border-bottom border-subtle">
            <div
              className="rounded-circle d-flex align-items-center justify-content-center fw-bold fs-4 text-white flex-shrink-0"
              style={{
                width: 56,
                height: 56,
                background: 'linear-gradient(135deg, var(--color-primary) 0%, #c2410c 100%)',
                fontFamily: 'var(--font-display)',
              }}
            >
              {displayName(account).slice(0, 1).toUpperCase()}
            </div>
            <div>
              <h2 className="fs-5 fw-bold mb-0">{displayName(account)}</h2>
              <span className="small text-muted font-monospace">@{account.login}</span>
            </div>
          </div>

          <dl className="row g-3 mb-0 small">
            {rows.map(([label, value]) => (
              <React.Fragment key={label}>
                <dt className="col-12 col-sm-4 text-muted fw-normal">{label}</dt>
                <dd className="col-12 col-sm-8 fw-semibold text-main mb-0">{value}</dd>
              </React.Fragment>
            ))}
          </dl>
        </div>

        <div className="p-3 p-md-4 bg-surface-subtle rounded-3 border border-subtle d-flex align-items-center gap-3">
          <Shield size={26} className="text-success flex-shrink-0" />
          <div className="small text-muted">
            <strong className="text-main d-block mb-1">Phiên đăng nhập được bảo vệ</strong>
            Mật khẩu không bao giờ đi qua trang này, và phiên đăng nhập không đọc được bằng mã JavaScript trên trang.
          </div>
        </div>

        <button
          type="button"
          className="btn btn-tr-secondary align-self-start px-4 d-inline-flex align-items-center gap-2"
          onClick={() => signOut()}
        >
          <LogOut size={16} /> Đăng xuất khỏi hệ thống
        </button>
      </div>
    </div>
  );
};
