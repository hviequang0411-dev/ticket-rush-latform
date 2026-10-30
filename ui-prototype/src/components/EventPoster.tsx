import React from 'react';

interface EventPosterProps {
  title: string;
  category: string;
  dateStr: string;
  venueName: string;
  bgConfig?: {
    gradient: string;
    accent: string;
    pattern: string;
  };
  height?: number | string;
  className?: string;
}

export const EventPoster: React.FC<EventPosterProps> = ({
  title,
  category,
  dateStr,
  venueName,
  bgConfig = {
    gradient: 'linear-gradient(135deg, #1e293b 0%, #0f172a 100%)',
    accent: '#ea580c',
    pattern: 'mesh'
  },
  height = 240,
  className = ''
}) => {
  return (
    <div
      className={`position-relative w-100 overflow-hidden d-flex flex-column justify-content-between p-4 ${className}`}
      style={{
        height,
        background: bgConfig.gradient,
        color: '#ffffff',
        userSelect: 'none'
      }}
      role="img"
      aria-label={`Poster sự kiện: ${title}`}
    >
      {/* Dynamic Background SVG Pattern */}
      <svg
        className="position-absolute top-0 start-0 w-100 h-100 opacity-25 pointer-events-none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <defs>
          <radialGradient id={`glow-${title.replace(/\s+/g, '')}`} cx="70%" cy="30%" r="60%">
            <stop offset="0%" stopColor={bgConfig.accent} stopOpacity="0.8" />
            <stop offset="100%" stopColor="#000000" stopOpacity="0" />
          </radialGradient>
        </defs>
        <rect width="100%" height="100%" fill={`url(#glow-${title.replace(/\s+/g, '')})`} />
        {bgConfig.pattern === 'circles' && (
          <>
            <circle cx="85%" cy="20%" r="80" stroke={bgConfig.accent} strokeWidth="1.5" fill="none" opacity="0.6" />
            <circle cx="85%" cy="20%" r="130" stroke={bgConfig.accent} strokeWidth="1" strokeDasharray="4 4" fill="none" opacity="0.4" />
          </>
        )}
        {bgConfig.pattern === 'waves' && (
          <path
            d="M0 180 Q 150 120 300 180 T 600 180 T 900 180"
            fill="none"
            stroke={bgConfig.accent}
            strokeWidth="2"
            opacity="0.4"
          />
        )}
        {bgConfig.pattern === 'grid' && (
          <path
            d="M0 20 H800 M0 60 H800 M0 100 H800 M0 140 H800 M0 180 H800"
            stroke="rgba(255,255,255,0.08)"
            strokeWidth="1"
          />
        )}
      </svg>

      {/* Top Header in Poster */}
      <div className="position-relative z-1 d-flex justify-content-between align-items-center">
        <span
          className="badge text-uppercase tracking-wider px-2 py-1"
          style={{
            backgroundColor: 'rgba(255,255,255,0.18)',
            backdropFilter: 'blur(4px)',
            color: '#fff',
            fontSize: '0.75rem',
            letterSpacing: '0.05em'
          }}
        >
          {category}
        </span>
        <span
          className="small opacity-75 font-monospace"
          style={{ fontSize: '0.8rem' }}
        >
          TICKETRUSH LIVE
        </span>
      </div>

      {/* Title & Info Bottom */}
      <div className="position-relative z-1 mt-auto">
        <h3
          className="text-white mb-2 fw-bold text-truncate-2"
          style={{
            textShadow: '0 2px 8px rgba(0,0,0,0.4)',
            fontSize: typeof height === 'number' && height > 280 ? '1.75rem' : '1.25rem',
            lineHeight: 1.25
          }}
        >
          {title}
        </h3>
        <div className="d-flex flex-wrap align-items-center gap-3 text-white-50 small">
          <span className="text-white opacity-90 fw-medium">{dateStr}</span>
          <span>•</span>
          <span className="text-truncate" style={{ maxWidth: '240px' }}>{venueName}</span>
        </div>
      </div>
    </div>
  );
};

