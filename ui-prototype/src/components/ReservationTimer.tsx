import React, { useState, useEffect } from 'react';
import { AlertTriangle } from 'lucide-react';

interface ReservationTimerProps {
  /** Remaining hold time in seconds when known directly (e.g. design system demo). */
  initialSeconds?: number;
  /** Server-supplied hold deadline ISO instant; takes precedence over initialSeconds. */
  expiresAt?: string;
  onExpire?: () => void;
}

const TOTAL_HOLD_SECONDS = 600;

export const ReservationTimer: React.FC<ReservationTimerProps> = ({
  initialSeconds = 600,
  expiresAt,
  onExpire,
}) => {
  const computeRemaining = () => {
    if (expiresAt) {
      return Math.max(0, Math.floor((new Date(expiresAt).getTime() - Date.now()) / 1000));
    }
    return initialSeconds;
  };

  const [secondsLeft, setSecondsLeft] = useState<number>(computeRemaining);

  useEffect(() => {
    setSecondsLeft(computeRemaining());
  }, [expiresAt, initialSeconds]);

  useEffect(() => {
    if (secondsLeft <= 0) {
      onExpire?.();
      return;
    }
    const timer = setInterval(() => {
      setSecondsLeft((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          onExpire?.();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [secondsLeft, onExpire]);

  const minutes = Math.floor(secondsLeft / 60);
  const seconds = secondsLeft % 60;
  const formattedTime = `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;

  const isLow = secondsLeft <= 120 && secondsLeft > 0;
  const isExpired = secondsLeft === 0;

  // Circular SVG ring progress (radius 11 -> circumference ~69.1)
  const radius = 11;
  const circumference = 2 * Math.PI * radius;
  const ratio = Math.min(1, Math.max(0, secondsLeft / TOTAL_HOLD_SECONDS));
  const strokeDashoffset = circumference * (1 - ratio);

  return (
    <div
      className={`d-inline-flex align-items-center gap-2 px-3 py-2 rounded-pill border ${
        isExpired
          ? 'bg-danger text-white border-danger'
          : isLow
          ? 'badge-danger tr-badge'
          : 'badge-warning tr-badge'
      }`}
      style={{
        backgroundColor: isExpired
          ? 'var(--color-danger)'
          : isLow
          ? 'var(--color-danger-bg)'
          : 'var(--color-warning-bg)',
        color: isExpired
          ? '#ffffff'
          : isLow
          ? 'var(--color-danger)'
          : 'var(--color-warning)',
        borderColor: isExpired
          ? 'var(--color-danger)'
          : isLow
          ? 'var(--color-danger-border)'
          : 'var(--color-warning-border)',
      }}
      role="timer"
      aria-live="polite"
      aria-label={`Thời gian giữ vé còn lại ${formattedTime}`}
    >
      {isLow ? (
        <AlertTriangle size={18} className="flex-shrink-0" />
      ) : (
        <svg width="26" height="26" viewBox="0 0 28 28" className="flex-shrink-0" aria-hidden="true">
          <circle
            cx="14"
            cy="14"
            r={radius}
            fill="none"
            stroke="currentColor"
            strokeOpacity="0.22"
            strokeWidth="3"
          />
          <circle
            cx="14"
            cy="14"
            r={radius}
            fill="none"
            stroke="currentColor"
            strokeWidth="3"
            strokeLinecap="round"
            strokeDasharray={circumference}
            strokeDashoffset={strokeDashoffset}
            transform="rotate(-90 14 14)"
            style={{ transition: 'stroke-dashoffset 0.6s linear' }}
          />
        </svg>
      )}
      <span className="small fw-medium">
        {isExpired ? 'Đã hết hạn giữ chỗ' : 'Giữ vé trong:'}
      </span>
      {!isExpired && (
        <span className="fw-bold fs-6 tabular-nums font-monospace">
          {formattedTime}
        </span>
      )}
    </div>
  );
};
