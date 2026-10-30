import React from 'react';
import type { Event } from '../types/models';

interface EventPosterProps {
  title: string;
  subtitle?: string;
  category: string;
  dateText: string;
  city: string;
  posterBg: Event['posterBg'];
  height?: number | string;
  className?: string;
  compact?: boolean;
}

/** "20:00, Thứ Ba 10/11/2026" or "10/11/2026" → day "10", month "THG 11". */
function dateStamp(dateText: string): { day: string; month: string } | null {
  const match = /(\d{1,2})\/(\d{1,2})/.exec(dateText);
  return match ? { day: match[1].padStart(2, '0'), month: `THG ${Number(match[2])}` } : null;
}

/**
 * Generated artwork for an event (the catalog has no images yet). Purely decorative: the event's name is always
 * shown next to or under the poster, so the poster itself carries only category, date and city.
 */
export const EventPoster: React.FC<EventPosterProps> = ({
  title,
  category,
  dateText,
  city,
  posterBg,
  height = 220,
  className = '',
  compact = false,
}) => {
  const stamp = dateStamp(dateText);
  return (
    <div
      className={`tr-poster ${compact ? 'tr-poster-compact' : ''} ${className}`}
      style={{ height, background: posterBg.gradient }}
      role="img"
      aria-label={`Ảnh minh hoạ sự kiện ${title}`}
    >
      <svg className="tr-poster-art" viewBox="0 0 400 260" preserveAspectRatio="xMidYMid slice" aria-hidden="true">
        <defs>
          <radialGradient id={`glow-${posterBg.pattern}`} cx="78%" cy="18%" r="60%">
            <stop offset="0%" stopColor={posterBg.accent} stopOpacity="0.85" />
            <stop offset="100%" stopColor={posterBg.accent} stopOpacity="0" />
          </radialGradient>
        </defs>
        <rect width="400" height="260" fill={`url(#glow-${posterBg.pattern})`} />
        {posterBg.pattern === 'dots' && (
          <g fill="none" stroke="#fff" strokeOpacity="0.28">
            <circle cx="300" cy="70" r="120" />
            <circle cx="300" cy="70" r="80" />
            <circle cx="300" cy="70" r="40" />
          </g>
        )}
        {posterBg.pattern === 'waves' && (
          <g fill="none" stroke="#fff" strokeOpacity="0.3" strokeWidth="1.5">
            <path d="M-20 170 Q 90 100 200 170 T 420 170" />
            <path d="M-20 200 Q 90 130 200 200 T 420 200" />
            <path d="M-20 230 Q 90 160 200 230 T 420 230" />
          </g>
        )}
        {posterBg.pattern === 'grid' && (
          <g stroke="#fff" strokeOpacity="0.16">
            {[40, 80, 120, 160, 200, 240].map(y => (
              <line key={y} x1="0" y1={y} x2="400" y2={y} />
            ))}
            {[80, 160, 240, 320].map(x => (
              <line key={x} x1={x} y1="0" x2={x} y2="260" />
            ))}
          </g>
        )}
      </svg>

      <div className="tr-poster-top">
        <span className="tr-poster-chip">{category}</span>
        {stamp && !compact && (
          <span className="tr-poster-stamp">
            <span className="tr-poster-day">{stamp.day}</span>
            <span className="tr-poster-month">{stamp.month}</span>
          </span>
        )}
      </div>

      <div className="tr-poster-city">{city}</div>
    </div>
  );
};
