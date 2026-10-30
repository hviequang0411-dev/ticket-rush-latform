import React, { useState, useEffect } from 'react';
import { Clock, AlertTriangle } from 'lucide-react';
import { formatTimeRemaining } from '../lib/format';

interface ReservationTimerProps {
  initialSeconds?: number;
  expiresAt?: string;
  onExpire?: () => void;
  className?: string;
}

export const ReservationTimer: React.FC<ReservationTimerProps> = ({
  initialSeconds = 600, // 10 minutes
  expiresAt,
  onExpire,
  className = ''
}) => {
  const [secondsLeft, setSecondsLeft] = useState<number>(() => {
    if (expiresAt) {
      const diff = Math.floor((new Date(expiresAt).getTime() - Date.now()) / 1000);
      return diff > 0 ? diff : 0;
    }
    return initialSeconds;
  });

  useEffect(() => {
    if (secondsLeft <= 0) {
      if (onExpire) onExpire();
      return;
    }

    const timer = setInterval(() => {
      setSecondsLeft(prev => {
        if (prev <= 1) {
          clearInterval(timer);
          if (onExpire) onExpire();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [secondsLeft, onExpire]);

  const isWarning = secondsLeft > 0 && secondsLeft <= 120; // Dưới 2 phút cảnh báo
  const isExpired = secondsLeft === 0;

  return (
    <div
      className={`d-inline-flex align-items-center gap-2 px-3 py-2 rounded-3 border ${
        isExpired
          ? 'bg-light text-muted border-secondary'
          : isWarning
          ? 'bg-danger-subtle text-danger border-danger'
          : 'bg-warning-subtle text-warning-emphasis border-warning'
      } ${className}`}
      role="timer"
      aria-live="polite"
    >
      {isWarning || isExpired ? (
        <AlertTriangle size={18} className="flex-shrink-0" />
      ) : (
        <Clock size={18} className="flex-shrink-0" />
      )}
      <div className="d-flex flex-column lh-1">
        <span className="small opacity-75" style={{ fontSize: '0.75rem' }}>
          {isExpired ? 'Thời gian giữ chỗ' : 'Thời gian giữ chỗ còn lại'}
        </span>
        <span className="fw-bold tabular-nums fs-6">
          {isExpired ? 'Đã hết thời gian giữ chỗ' : formatTimeRemaining(secondsLeft)}
        </span>
      </div>
    </div>
  );
};

