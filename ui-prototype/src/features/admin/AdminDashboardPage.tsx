import React from 'react';
import { DollarSign, ShoppingCart, Users, Zap, TrendingUp, Flame } from 'lucide-react';
import { ResponsiveContainer, AreaChart, Area, XAxis, YAxis, Tooltip, CartesianGrid } from 'recharts';
import { formatVND } from '../../lib/format';

const revenueData14Days = [
  { date: '17/10', revenue: 42000000 },
  { date: '18/10', revenue: 58000000 },
  { date: '19/10', revenue: 49000000 },
  { date: '20/10', revenue: 75000000 },
  { date: '21/10', revenue: 62000000 },
  { date: '22/10', revenue: 88000000 },
  { date: '23/10', revenue: 110000000 },
  { date: '24/10', revenue: 95000000 },
  { date: '25/10', revenue: 130000000 },
  { date: '26/10', revenue: 145000000 },
  { date: '27/10', revenue: 160000000 },
  { date: '28/10', revenue: 185000000 },
  { date: '29/10', revenue: 210000000 },
  { date: '30/10', revenue: 245000000 },
];

export const AdminDashboardPage: React.FC = () => {
  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h1 className="fs-3 fw-bold mb-1">Bảng điều khiển &amp; Chỉ số KPIs</h1>
          <p className="text-muted small mb-0">Theo dõi trực tiếp lưu lượng giao dịch, doanh số và trạng thái cụm bán vé</p>
        </div>
        <div className="d-flex align-items-center gap-2">
          <span className="badge bg-success-subtle text-success border border-success-subtle px-3 py-2">
            Đồng bộ dữ liệu: Thời gian thực
          </span>
        </div>
      </div>

      {/* 1. KPIs Row */}
      <div className="row g-3 mb-4">
        {/* Doanh thu */}
        <div className="col-12 col-sm-6 col-xl-3">
          <div className="tr-card p-4">
            <div className="d-flex justify-content-between align-items-center mb-2">
              <span className="small text-muted fw-semibold">Doanh thu hôm nay</span>
              <div className="p-2 rounded-2 bg-primary-subtle text-primary">
                <DollarSign size={20} />
              </div>
            </div>
            <div className="fs-4 fw-bold text-main tabular-nums mb-1">245.000.000 ₫</div>
            <div className="small text-success d-flex align-items-center gap-1">
              <TrendingUp size={14} /> +16.8% so với hôm qua
            </div>
          </div>
        </div>

        {/* Đơn hàng */}
        <div className="col-12 col-sm-6 col-xl-3">
          <div className="tr-card p-4">
            <div className="d-flex justify-content-between align-items-center mb-2">
              <span className="small text-muted fw-semibold">Đơn hàng hôm nay</span>
              <div className="p-2 rounded-2 bg-success-subtle text-success">
                <ShoppingCart size={20} />
              </div>
            </div>
            <div className="fs-4 fw-bold text-main tabular-nums mb-1">318 đơn</div>
            <div className="small text-success d-flex align-items-center gap-1">
              <TrendingUp size={14} /> 98.2% giao dịch thành công
            </div>
          </div>
        </div>

        {/* Tỉ lệ bán hết */}
        <div className="col-12 col-sm-6 col-xl-3">
          <div className="tr-card p-4">
            <div className="d-flex justify-content-between align-items-center mb-2">
              <span className="small text-muted fw-semibold">Tỉ lệ lấp đầy vé</span>
              <div className="p-2 rounded-2 bg-warning-subtle text-warning">
                <Zap size={20} />
              </div>
            </div>
            <div className="fs-4 fw-bold text-main tabular-nums mb-1">84.5%</div>
            <div className="small text-muted">Trên 12 sự kiện đang chạy</div>
          </div>
        </div>

        {/* Lượt giữ chỗ đang hoạt động */}
        <div className="col-12 col-sm-6 col-xl-3">
          <div className="tr-card p-4">
            <div className="d-flex justify-content-between align-items-center mb-2">
              <span className="small text-muted fw-semibold">Lượt giữ chỗ active</span>
              <div className="p-2 rounded-2 bg-info-subtle text-info">
                <Users size={20} />
              </div>
            </div>
            <div className="fs-4 fw-bold text-info tabular-nums mb-1">42 lượt</div>
            <div className="small text-muted">Đang trong đồng hồ 10 phút</div>
          </div>
        </div>
      </div>

      {/* 2. Revenue Chart (14 days) */}
      <div className="tr-card p-4 mb-4">
        <div className="d-flex justify-content-between align-items-center mb-4">
          <div>
            <h2 className="fs-5 fw-bold mb-1">Biểu đồ doanh thu 14 ngày qua</h2>
            <p className="text-muted small mb-0">Thống kê dòng tiền bán vé tự động từ VNPay, MoMo và Sandbox</p>
          </div>
          <span className="fw-bold text-primary tabular-nums fs-5">
            Tổng: 1.579.000.000 ₫
          </span>
        </div>

        <div style={{ width: '100%', height: 280 }}>
          <ResponsiveContainer>
            <AreaChart data={revenueData14Days} margin={{ top: 10, right: 10, left: 10, bottom: 0 }}>
              <defs>
                <linearGradient id="colorRev" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#ea580c" stopOpacity={0.4} />
                  <stop offset="95%" stopColor="#ea580c" stopOpacity={0.0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--border-subtle)" />
              <XAxis dataKey="date" stroke="var(--text-muted)" fontSize={12} tickLine={false} />
              <YAxis
                stroke="var(--text-muted)"
                fontSize={12}
                tickLine={false}
                tickFormatter={(val) => `${val / 1000000}Tr`}
              />
              <Tooltip
                formatter={(val) => [formatVND(Number(val)), 'Doanh thu']}
                contentStyle={{
                  backgroundColor: 'var(--bg-surface)',
                  borderColor: 'var(--border-subtle)',
                  borderRadius: 8,
                  fontSize: 13
                }}
              />
              <Area type="monotone" dataKey="revenue" stroke="#ea580c" strokeWidth={2} fillOpacity={1} fill="url(#colorRev)" />
            </AreaChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* 3. Real-time Hot Event Sales Monitor */}
      <div className="tr-card p-4">
        <div className="d-flex align-items-center gap-2 mb-3">
          <Flame size={20} className="text-danger" />
          <h2 className="fs-5 fw-bold mb-0">Theo dõi trực tiếp đợt mở bán (Live Rush Monitor)</h2>
        </div>

        <div className="row g-3">
          <div className="col-12 col-lg-6">
            <div className="p-3 rounded-3 border border-subtle bg-surface-subtle">
              <div className="d-flex justify-content-between align-items-center mb-2">
                <span className="fw-bold text-main">Đêm Nhạc Mùa Thu Hà Nội</span>
                <span className="badge bg-danger">LIVE RUSH</span>
              </div>
              <div className="d-flex justify-content-between small text-muted mb-1">
                <span>Tiến độ bán vé toàn sự kiện</span>
                <span className="fw-semibold text-main">818 / 900 vé (90.8%)</span>
              </div>
              <div className="progress mb-3" style={{ height: 8 }} role="progressbar" aria-valuenow={91} aria-valuemin={0} aria-valuemax={100} aria-label="Tiến độ bán vé 91%">
                <div className="progress-bar bg-primary" style={{ width: '90.8%' }} />
              </div>
              <div className="small text-muted">
                Tốc độ thanh toán: <strong>~24 vé / phút</strong> • Hàng chờ ảo: <strong>0 người</strong>
              </div>
            </div>
          </div>

          <div className="col-12 col-lg-6">
            <div className="p-3 rounded-3 border border-subtle bg-surface-subtle">
              <div className="d-flex justify-content-between align-items-center mb-2">
                <span className="fw-bold text-main">Neon Tide Festival 2026</span>
                <span className="badge bg-success">ĐANG BÁN TỰ DO</span>
              </div>
              <div className="d-flex justify-content-between small text-muted mb-1">
                <span>Tiến độ bán vé toàn sự kiện</span>
                <span className="fw-semibold text-main">6,315 / 8,800 vé (71.7%)</span>
              </div>
              <div className="progress mb-3" style={{ height: 8 }} role="progressbar" aria-valuenow={72} aria-valuemin={0} aria-valuemax={100} aria-label="Tiến độ bán vé 72%">
                <div className="progress-bar bg-success" style={{ width: '71.7%' }} />
              </div>
              <div className="small text-muted">
                Tốc độ thanh toán: <strong>~12 vé / phút</strong> • Phòng chờ: <strong>Tự do truy cập</strong>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

