import React, { useCallback, useEffect, useState } from 'react';
import { DollarSign, ShoppingCart, Users, Zap, TrendingUp, TrendingDown, Flame } from 'lucide-react';
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

const Kpi: React.FC<{ label: string; value: string; icon: React.ReactNode; tone: string; note?: React.ReactNode }> = ({ label, value, icon, tone, note }) => (
  <div className="col-12 col-sm-6 col-xl-3">
    <div className="tr-card p-4 h-100">
      <div className="d-flex justify-content-between align-items-center mb-2">
        <span className="small text-muted fw-semibold">{label}</span>
        <div className={`p-2 rounded-2 bg-${tone}-subtle text-${tone}`}>{icon}</div>
      </div>
      <div className="fs-4 fw-bold text-main tabular-nums mb-1">{value}</div>
      <div className="small text-muted">{note}</div>
    </div>
  </div>
);

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
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h1 className="fs-3 fw-bold mb-1">Bảng điều khiển &amp; Chỉ số KPIs</h1>
          <p className="text-muted small mb-0">Doanh số và trạng thái các đợt mở bán, tự làm mới mỗi {REFRESH_MS / 1000} giây (giờ Việt Nam)</p>
        </div>
      </div>

      <div className="row g-3 mb-4">
        <Kpi
          label="Doanh thu hôm nay"
          value={stats ? formatVND(stats.today.revenue) : '—'}
          tone="primary"
          icon={<DollarSign size={20} />}
          note={
            revenueChange ? (
              <span className={`d-flex align-items-center gap-1 ${revenueChange.up ? 'text-success' : 'text-danger'}`}>
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
          icon={<ShoppingCart size={20} />}
          note={successRate !== null ? `${successRate.toFixed(1)}% đơn đã kết thúc là thành công` : 'Chưa có đơn kết thúc'}
        />
        <Kpi
          label="Tỉ lệ lấp đầy vé"
          value={total > 0 ? `${((sold / total) * 100).toFixed(1)}%` : '—'}
          tone="warning"
          icon={<Zap size={20} />}
          note={`${sold.toLocaleString('vi-VN')} / ${total.toLocaleString('vi-VN')} vé các sự kiện đang bán`}
        />
        <Kpi
          label="Đơn đang giữ chỗ"
          value={stats ? `${stats.activeHolds} đơn` : '—'}
          tone="info"
          icon={<Users size={20} />}
          note="Đang trong đồng hồ 10 phút"
        />
      </div>

      <div className="tr-card p-4 mb-4">
        <div className="d-flex flex-column flex-sm-row justify-content-between align-items-sm-center gap-2 mb-4">
          <div>
            <h2 className="fs-5 fw-bold mb-1">Doanh thu 14 ngày qua</h2>
            <p className="text-muted small mb-0">Theo ngày đơn được xác nhận</p>
          </div>
          <span className="fw-bold text-primary tabular-nums fs-5">Tổng: {formatVND(chartTotal)}</span>
        </div>
        <div style={{ width: '100%', height: 280 }}>
          <ResponsiveContainer>
            <AreaChart data={chart} margin={{ top: 10, right: 10, left: 10, bottom: 0 }}>
              <defs>
                <linearGradient id="colorRev" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#ea580c" stopOpacity={0.4} />
                  <stop offset="95%" stopColor="#ea580c" stopOpacity={0.0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--border-subtle)" />
              <XAxis dataKey="date" stroke="var(--text-muted)" fontSize={12} tickLine={false} />
              <YAxis stroke="var(--text-muted)" fontSize={12} tickLine={false} tickFormatter={val => `${val / 1000000}Tr`} />
              <Tooltip
                formatter={val => [formatVND(Number(val)), 'Doanh thu']}
                contentStyle={{ backgroundColor: 'var(--bg-surface)', borderColor: 'var(--border-subtle)', borderRadius: 8, fontSize: 13 }}
              />
              <Area type="monotone" dataKey="revenue" stroke="#ea580c" strokeWidth={2} fillOpacity={1} fill="url(#colorRev)" />
            </AreaChart>
          </ResponsiveContainer>
        </div>
      </div>

      <div className="tr-card p-4">
        <div className="d-flex align-items-center gap-2 mb-3">
          <Flame size={20} className="text-danger" />
          <h2 className="fs-5 fw-bold mb-0">Theo dõi trực tiếp các đợt mở bán</h2>
        </div>
        {live.length === 0 && <div className="small text-muted">Không có sự kiện nào đang bán.</div>}
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
                <div className="p-3 rounded-3 border border-subtle bg-surface-subtle">
                  <div className="d-flex justify-content-between align-items-center mb-2 gap-2">
                    <span className="fw-bold text-main">{event.name}</span>
                    {event.waitingRoom ? <span className="badge bg-danger">PHÒNG CHỜ</span> : <span className="badge bg-success">BÁN TỰ DO</span>}
                  </div>
                  <div className="d-flex justify-content-between small text-muted mb-1">
                    <span>Tiến độ bán vé</span>
                    <span className="fw-semibold text-main tabular-nums">
                      {eventSold.toLocaleString('vi-VN')} / {eventTotal.toLocaleString('vi-VN')} vé ({percent.toFixed(1)}%)
                    </span>
                  </div>
                  <div className="progress mb-3" style={{ height: 8 }} role="progressbar" aria-valuenow={Math.round(percent)} aria-valuemin={0} aria-valuemax={100} aria-label={`Tiến độ bán ${event.name}`}>
                    <div className={`progress-bar ${event.waitingRoom ? 'bg-primary' : 'bg-success'}`} style={{ width: `${percent}%` }} />
                  </div>
                  <div className="small text-muted">
                    Tốc độ: <strong className="tabular-nums">~{perMinute.toFixed(1)} vé / phút</strong> ({stats?.paceMinutes ?? 10} phút gần nhất)
                    {event.waitingRoom && (
                      <>
                        {' '}• Hàng chờ: <strong className="tabular-nums">{queue ? `${queue.waiting} người` : '—'}</strong>
                        {queue && ` (cho vào ${queue.admitPerSecond}/giây)`}
                      </>
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
