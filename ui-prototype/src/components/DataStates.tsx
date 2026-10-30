import React from 'react';
import { AlertCircle, RefreshCw, Inbox, Lock, LogIn } from 'lucide-react';
import { signIn } from '../api/auth';

interface EmptyStateProps {
  title: string;
  description: string;
  actionText?: string;
  onAction?: () => void;
  icon?: React.ReactNode;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  title,
  description,
  actionText,
  onAction,
  icon,
}) => {
  return (
    <div className="tr-card p-5 text-center my-3">
      <div
        className="d-inline-flex align-items-center justify-content-center rounded-circle mb-3 text-primary"
        style={{
          width: 72,
          height: 72,
          backgroundColor: 'var(--color-primary-subtle)',
          border: '1px solid var(--color-primary-border)',
        }}
      >
        {icon || <Inbox size={32} />}
      </div>
      <h3 className="fs-5 fw-bold mb-2 text-main">{title}</h3>
      <p className="text-muted small mx-auto mb-4" style={{ maxWidth: 440 }}>
        {description}
      </p>
      {actionText && onAction && (
        <button onClick={onAction} className="btn btn-tr-primary">
          {actionText}
        </button>
      )}
    </div>
  );
};

interface ErrorStateProps {
  title?: string;
  message: string;
  status?: number;
  onRetry?: () => void;
}

export const ErrorState: React.FC<ErrorStateProps> = ({
  title = 'Không tải được dữ liệu',
  message,
  status,
  onRetry,
}) => {
  if (status === 401) {
    // Not an error from the buyer's point of view: they just need to sign in, and come back here afterwards
    return (
      <div className="tr-card p-5 text-center my-3">
        <div className="tr-state-icon mb-3">
          <Lock size={28} />
        </div>
        <h3 className="fs-5 fw-semibold mb-2 text-main">Đăng nhập để tiếp tục</h3>
        <p className="text-muted small mx-auto mb-4" style={{ maxWidth: 380 }}>
          Bạn cần đăng nhập để xem trang này. Sau khi đăng nhập, bạn sẽ quay lại đúng chỗ đang xem.
        </p>
        <button type="button" onClick={() => signIn()} className="btn btn-tr-primary">
          <LogIn size={16} /> Đăng nhập
        </button>
      </div>
    );
  }
  return (
    <div
      className="tr-card p-4 my-3"
      style={{
        borderColor: 'var(--color-danger-border)',
        backgroundColor: 'var(--bg-surface)',
      }}
      role="alert"
    >
      <div className="d-flex align-items-start gap-3">
        <div
          className="p-2 rounded-3 flex-shrink-0 d-flex align-items-center justify-content-center"
          style={{
            backgroundColor: 'var(--color-danger-bg)',
            color: 'var(--color-danger)',
          }}
        >
          <AlertCircle size={24} />
        </div>
        <div className="flex-grow-1">
          <h4 className="fs-6 fw-semibold mb-1 text-main">{title}</h4>
          <p className="text-muted small mb-3">{message}</p>
          {onRetry && (
            <button
              onClick={onRetry}
              className="btn btn-sm btn-tr-secondary d-inline-flex align-items-center gap-2"
            >
              <RefreshCw size={15} />
              <span>Thử tải lại</span>
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

export const EventCardSkeleton: React.FC = () => (
  <div className="tr-card overflow-hidden h-100 d-flex flex-column">
    <div className="tr-skeleton w-100" style={{ height: 200, borderRadius: 0 }} />
    <div className="p-3 d-flex flex-column gap-2 flex-grow-1">
      <div className="tr-skeleton w-50" style={{ height: 14 }} />
      <div className="tr-skeleton w-100" style={{ height: 22 }} />
      <div className="tr-skeleton w-75" style={{ height: 16 }} />
      <div className="mt-auto pt-3 d-flex justify-content-between align-items-center">
        <div className="tr-skeleton w-25" style={{ height: 20 }} />
        <div className="tr-skeleton w-25" style={{ height: 34 }} />
      </div>
    </div>
  </div>
);
