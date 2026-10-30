import React from 'react';
import { AlertCircle, RotateCcw } from 'lucide-react';

export const EmptyState: React.FC<{
  title: string;
  description: string;
  actionText?: string;
  onAction?: () => void;
  icon?: React.ReactNode;
}> = ({ title, description, actionText, onAction, icon }) => {
  return (
    <div className="text-center py-5 px-3 my-4 tr-card bg-surface">
      <div className="mb-3 text-muted d-inline-block p-3 rounded-circle bg-surface-subtle">
        {icon || <AlertCircle size={36} />}
      </div>
      <h4 className="fw-semibold mb-2">{title}</h4>
      <p className="text-muted mx-auto mb-4" style={{ maxWidth: '440px' }}>
        {description}
      </p>
      {actionText && onAction && (
        <button className="btn btn-tr-secondary" onClick={onAction}>
          {actionText}
        </button>
      )}
    </div>
  );
};

export const ErrorState: React.FC<{
  title?: string;
  message: string;
  onRetry?: () => void;
  status?: number;
}> = ({ title = 'Đã có lỗi xảy ra', message, onRetry, status }) => {
  return (
    <div className="p-4 my-4 rounded-3 border border-danger-subtle bg-danger-subtle text-danger-emphasis">
      <div className="d-flex align-items-start gap-3">
        <AlertCircle size={24} className="text-danger flex-shrink-0 mt-1" />
        <div className="flex-grow-1">
          <h5 className="fw-bold mb-1 text-danger">
            {title} {status ? `(Mã lỗi: ${status})` : ''}
          </h5>
          <p className="mb-3 small">{message}</p>
          {onRetry && (
            <button className="btn btn-sm btn-outline-danger d-inline-flex align-items-center gap-2" onClick={onRetry}>
              <RotateCcw size={14} /> Thử lại ngay
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
