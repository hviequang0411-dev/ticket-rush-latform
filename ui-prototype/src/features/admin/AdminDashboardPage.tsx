import React, { useCallback, useEffect, useState } from 'react';
import { DollarSign, ShoppingCart, Users, Zap, TrendingUp, TrendingDown, Flame, Activity } from 'lucide-react';
import { ResponsiveContainer, AreaChart, Area, XAxis, YAxis, Tooltip, CartesianGrid } from 'recharts';
import { AdminApi, type QueueStats, type SalesStats } from '../../api/adminApi';
import type { ApiErrorProblem } from '../../types/models';
import { ErrorState } from '../../components/DataStates';
import { formatVND } from '../../lib/format';
import { useAdminEvents } from './useAdminEvents';

const REFRESH_MS = 10000;

function change(today: number, yesterday: number): { text: string; up: boolean } | null {
  if (yesterday === 0) return null;
  const delta = ((today - yesterday) / yesterday) * 100;
  return { text: `${delta >= 0 ? '+' : ''}${delta.toFixed(1)}% so với hôm qua`, up: delta >= 0 };
}

/** Presentational KPI Card with subtle mini SVG sparkline */
const Kpi: React.FC<{
  label: string;
  value: string;
  icon: React.ReactNode;
  tone: 'primary' | 'success' | 'warning' | 'info';
  sparkPath: string;
  note?: React.ReactNode;
}> = ({ label, value, icon, tone, sparkPath, note }) => {
  const toneColors: Record<string, { fg: string; bg: string }> = {
    primary: { fg: 'var(--color-primary)', bg: 'var(--color-primary-subtle)' },
    success: { fg: 'var(--color-success)', bg: 'var(--color-success-bg)' },
    warning: { fg: 'var(--color-warning)', bg: 'var(--color-warning-bg)' },
    info: { fg: 'var(--color-info)', bg: 'var(--color-info-bg)' },
  };
  const palette = toneColors[tone];

  return (
    <div className="col-12 col-sm-6 col-xl-3">
      <div className="tr-card p-3 p-md-4 h-100 d-flex flex-column justify-content-between">
        <div>
          <div className="d-flex justify-content-between align-items-center mb-2">
            <span className="small text-muted fw-semibold">{label}</span>
            <div
              className="p-2 rounded-3 d-flex align-items-center justify-content-center"
              style={{ backgroundColor: palette.bg, color: palette.fg }}
            >
              {icon}
            </div>
          </div>
          <div className="d-flex align-items-end justify-content-between gap-2 mb-2">
            <div className="fs-4 fw-bold text-main tabular-nums lh-sm" style={{ fontFamily: 'var(--font-display)' }}>
              {value}
            </div>
            {/* Mini Sparkline */}
            <svg width="72" height="26" viewBox="0 0 72 26" aria-hidden="true" className="flex-shrink-0">
              <path
                d={sparkPath}
                fill="none"
                stroke={palette.fg}
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
          </div>
        </div>
        <div className="small text-muted pt-2 border-top border-subtle">{note}</div>
      </div>
    </div>
  );
};

export const AdminDashboardPage: React.FC = () => {
  const { events } = useAdminEvents(REFRESH_MS);
  const [stats, setStats] = useState<SalesStats | null>(null);
  const [queues, setQueues] = useState<Record<number, QueueStats>>({});
  const [error, setError] = useState<ApiErrorProblem | null>(null);

  const load = useCallback(async () => {
    try {
      setStats(await AdminApi.sales(14));
      setError(null);
    } catch (e: unknown) {
      setError(e as ApiErrorProblem);
    }
  }, []);

  useEffect(() => {
    load();
    const timer = setInterval(load, REFRESH_MS);
    return () => clearInterval(timer);
  }, [load]);

  const live = events.filter(event => event.status === 'PUBLISHED');
  const queued = live.filter(event => event.waitingRoom).map(event => event.id).join(',');

  useEffect(() => {
    if (!queued) return;
    const ids = queued.split(',').map(Number);
    Promise.all(ids.map(id => AdminApi.queue(id).catch(() => null))).then(results =>
      setQueues(Object.fromEntries(results.filter((q): q is QueueStats => q !== null).map(q => [q.eventId, q]))),
    );
  }, [queued, stats]);

  if (error && !stats) {
    return <ErrorState title={error.title} message={error.detail} status={error.status} onRetry={load} />;
  }

  const levels = live.flatMap(event => Object.values(event.stock));
  const total = levels.reduce((sum, l) => sum + l.totalQuantity, 0);
  const sold = levels.reduce((sum, l) => sum + l.soldQuantity, 0);
  const revenueChange = stats ? change(stats.today.revenue, stats.yesterday.revenue) : null;
  const successRate = stats && stats.today.ordersFinished > 0 ? (stats.today.ordersConfirmed / stats.today.ordersFinished) * 100 : null;
  const chart = (stats?.daily ?? []).map(day => ({ date: day.date.slice(8, 10) + '/' + day.date.slice(5, 7), revenue: Number(day.revenue) }));
  const chartTotal = chart.reduce((sum, d) => sum + d.revenue, 0);
  const pace = new Map((stats?.pace ?? []).map(p => [p.eventId, p.seats]));

  return (
    <div>
      <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-4">
        <div>
          <span className="tr-badge badge-info mb-1">
            <Activity size={13} /> REAL-TIME TELEMETRY
          </span>
          <h1 className="fs-3 fw-bold mb-1">Bảng điều khiển &amp; Chỉ số KPIs</h1>
          <p className="text-muted small mb-0">
            Doanh số và trạng thái các đợt mở bán, tự làm mới mỗi {REFRESH_MS / 1000} giây (giờ Việt Nam)
          </p>
        </div>
      </div>

      <div className="row g-3 mb-4">
        <Kpi
          label="Doanh thu hôm nay"
          value={stats ? formatVND(stats.today.revenue) : '—'}
          tone="primary"
          icon={<DollarSign size={18} />}
          sparkPath="M2 22 L16 18 L30 20 L44 12 L58 14 L70 4"
          note={
            revenueChange ? (
              <span className={`d-flex align-items-center gap-1 fw-medium ${revenueChange.up ? 'text-success' : 'text-danger'}`}>
                {revenueChange.up ? <TrendingUp size={14} /> : <TrendingDown size={14} />} {revenueChange.text}
              </span>
            ) : (
              'Chỉ tính đơn đã xác nhận'
            )
          }
        />
        <Kpi
          label="Đơn hàng hôm nay"
          value={stats ? `${stats.today.ordersPlaced} đơn` : '—'}
          tone="success"
          icon={<ShoppingCart size={18} />}
          sparkPath="M2 20 L18 16 L34 17 L48 9 L60 11 L70 5"
          note={successRate !== null ? `${successRate.toFixed(1)}% đơn đã kết thúc là thành công` : 'Chưa có đơn kết thúc'}
        />
        <Kpi
          label="Tỉ lệ lấp đầy vé"
          value={total > 0 ? `${((sold / total) * 100).toFixed(1)}%` : '—'}
          tone="warning"
          icon={<Zap size={18} />}
          sparkPath="M2 21 L18 19 L34 14 L50 10 L70 6"
          note={`${sold.toLocaleString('vi-VN')} / ${total.toLocaleString('vi-VN')} vé các sự kiện đang bán`}
        />
        <Kpi
          label="Đơn đang giữ chỗ"
          value={stats ? `${stats.activeHolds} đơn` : '—'}
          tone="info"
          icon={<Users size={18} />}
          sparkPath="M2 14 L16 9 L30 16 L46 8 L58 15 L70 10"
          note="Đang trong đồng hồ 10 phút"
        />
      </div>

      <div className="tr-card p-4 mb-4">
        <div className="d-flex flex-column flex-sm-row justify-content-between align-items-sm-center gap-2 mb-4 pb-2 border-bottom border-subtle">
          <div>
            <h2 className="fs-5 fw-bold mb-1">Biểu đồ doanh thu 14 ngày qua</h2>
            <p className="text-muted small mb-0">Tổng hợp theo ngày đơn được xác nhận thanh toán thành công</p>
          </div>
          <div className="p-2 px-3 rounded-3 bg-surface-subtle border border-subtle">
            <span className="small text-muted me-2">Tổng 14 ngày:</span>
            <span className="fw-bold text-primary tabular-nums fs-5">{formatVND(chartTotal)}</span>
          </div>
        </div>
        <div style={{ width: '100%', height: 290 }}>
          <ResponsiveContainer>
            <AreaChart data={chart} margin={{ top: 10, right: 12, left: 8, bottom: 0 }}>
              <defs>
                <linearGradient id="colorRev" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#ea580c" stopOpacity={0.38} />
                  <stop offset="95%" stopColor="#ea580c" stopOpacity={0.0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--border-subtle)" />
              <XAxis dataKey="date" stroke="var(--text-muted)" fontSize={12} tickLine={false} />
              <YAxis stroke="var(--text-muted)" fontSize={12} tickLine={false} tickFormatter={val => `${val / 1000000}Tr`} />
              <Tooltip
                formatter={val => [formatVND(Number(val)), 'Doanh thu']}
                contentStyle={{
                  backgroundColor: 'var(--bg-surface)',
                  borderColor: 'var(--border-strong)',
                  borderRadius: 10,
                  fontSize: 13,
                  color: 'var(--text-main)',
                  boxShadow: 'var(--shadow-md)',
                }}
              />
              <Area type="monotone" dataKey="revenue" stroke="#ea580c" strokeWidth={2.5} fillOpacity={1} fill="url(#colorRev)" />
            </AreaChart>
          </ResponsiveContainer>
        </div>
      </div>

      <div className="tr-card p-4">
        <div className="d-flex align-items-center justify-content-between mb-3 pb-2 border-bottom border-subtle">
          <div className="d-flex align-items-center gap-2">
            <Flame size={20} className="text-danger" />
            <h2 className="fs-5 fw-bold mb-0">Theo dõi trực tiếp các đợt mở bán (Live Rush Monitor)</h2>
          </div>
          <span className="tr-badge badge-success">{live.length} sự kiện đang mở</span>
        </div>

        {live.length === 0 && <div className="small text-muted py-3">Không có sự kiện nào đang bán.</div>}
        <div className="row g-3">
          {live.map(event => {
            const eventLevels = Object.values(event.stock);
            const eventTotal = eventLevels.reduce((sum, l) => sum + l.totalQuantity, 0);
            const eventSold = eventLevels.reduce((sum, l) => sum + l.soldQuantity, 0);
            const percent = eventTotal > 0 ? (eventSold / eventTotal) * 100 : 0;
            const perMinute = (pace.get(event.id) ?? 0) / (stats?.paceMinutes ?? 10);
            const queue = queues[event.id];
            return (
              <div key={event.id} className="col-12 col-lg-6">
                <div className="p-3 p-md-4 rounded-3 border border-subtle bg-surface-subtle h-100">
                  <div className="d-flex justify-content-between align-items-start mb-2 gap-2">
                    <span className="fw-bold text-main">{event.name}</span>
                    {event.waitingRoom ? (
                      <span className="tr-badge badge-danger">PHÒNG CHỜ</span>
                    ) : (
                      <span className="tr-badge badge-success">BÁN TỰ DO</span>
                    )}
                  </div>
                  <div className="d-flex justify-content-between small text-muted mb-1">
                    <span>Tiến độ bán vé</span>
                    <span className="fw-semibold text-main tabular-nums">
                      {eventSold.toLocaleString('vi-VN')} / {eventTotal.toLocaleString('vi-VN')} vé ({percent.toFixed(1)}%)
                    </span>
                  </div>
                  <div
                    className="progress mb-3"
                    style={{ height: 8, borderRadius: 999 }}
                    role="progressbar"
                    aria-valuenow={Math.round(percent)}
                    aria-valuemin={0}
                    aria-valuemax={100}
                    aria-label={`Tiến độ bán ${event.name}`}
                  >
                    <div
                      className={`progress-bar ${event.waitingRoom ? 'bg-primary' : 'bg-success'}`}
                      style={{ width: `${percent}%` }}
                    />
                  </div>
                  <div className="small text-muted d-flex flex-wrap gap-2 justify-content-between">
                    <span>
                      Tốc độ: <strong className="text-main tabular-nums">~{perMinute.toFixed(1)} vé / phút</strong> ({stats?.paceMinutes ?? 10}p gần nhất)
                    </span>
                    {event.waitingRoom && (
                      <span>
                        Hàng chờ: <strong className="text-primary tabular-nums">{queue ? `${queue.waiting} người` : '—'}</strong>
                        {queue && ` (${queue.admitPerSecond}/s)`}
                      </span>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};
