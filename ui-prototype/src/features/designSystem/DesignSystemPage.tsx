import React, { useState } from 'react';
import { Sparkles, CheckCircle2 } from 'lucide-react';
import { ReservationTimer } from '../../components/ReservationTimer';
import { StatusBadge } from '../../components/StatusBadge';
import { ErrorState, EmptyState } from '../../components/DataStates';

export const DesignSystemPage: React.FC = () => {
  const [modalOpen, setModalOpen] = useState(false);
  const [toastOpen, setToastOpen] = useState(false);

  return (
    <div className="tr-container py-5">
      <div className="mb-5 pb-4 border-bottom border-subtle">
        <span className="tr-badge badge-info font-monospace mb-2">
          <Sparkles size={13} /> TICKETRUSH DESIGN SYSTEM 2.0
        </span>
        <h1 className="display-6 fw-bold text-main mb-2">Hệ thống Thiết kế &amp; Thành phần Giao diện</h1>
        <p className="text-muted mb-0" style={{ maxWidth: 720 }}>
          Chuẩn hoá toàn diện Tokens (Bảng màu Sáng/Tối, Typography tiếng Việt Lexend &amp; Be Vietnam Pro, Spacing 4/8px, 2 cấp độ đổ bóng) và các component đặc trưng ngành vé sự kiện tuân thủ WCAG 2.1 AA.
        </p>
      </div>

      {/* 1. Color Palette */}
      <section className="mb-5">
        <h2 className="fs-4 fw-bold mb-3">1. Bảng màu chủ đạo &amp; Trạng thái (Color Tokens)</h2>
        <div className="row g-3">
          <div className="col-6 col-md-4 col-lg-2">
            <div className="p-3 rounded-3 text-white shadow-xs h-100" style={{ backgroundColor: 'var(--color-primary)' }}>
              <div className="fw-bold small">Primary CTA</div>
              <div className="small opacity-75 font-monospace" style={{ fontSize: '0.72rem' }}>#ea580c / #f97316</div>
              <div className="small opacity-75 mt-2" style={{ fontSize: '0.72rem' }}>Cam Vermilion</div>
            </div>
          </div>
          <div className="col-6 col-md-4 col-lg-2">
            <div className="p-3 rounded-3 text-white shadow-xs h-100" style={{ backgroundColor: 'var(--color-accent)' }}>
              <div className="fw-bold small">Data Accent</div>
              <div className="small opacity-75 font-monospace" style={{ fontSize: '0.72rem' }}>#0d9488 / #2dd4bf</div>
              <div className="small opacity-75 mt-2" style={{ fontSize: '0.72rem' }}>Deep Teal</div>
            </div>
          </div>
          <div className="col-6 col-md-4 col-lg-2">
            <div className="p-3 rounded-3 text-white shadow-xs h-100" style={{ backgroundColor: '#0f172a' }}>
              <div className="fw-bold small">Ink Navy Text</div>
              <div className="small opacity-75 font-monospace" style={{ fontSize: '0.72rem' }}>#0f172a</div>
              <div className="small opacity-75 mt-2" style={{ fontSize: '0.72rem' }}>Chữ chính sắc nét</div>
            </div>
          </div>
          <div className="col-6 col-md-4 col-lg-2">
            <div className="p-3 rounded-3 border border-strong shadow-xs h-100" style={{ backgroundColor: '#faf8f5', color: '#0f172a' }}>
              <div className="fw-bold small">Warm Ivory BG</div>
              <div className="small opacity-75 font-monospace" style={{ fontSize: '0.72rem' }}>#faf8f5</div>
              <div className="small opacity-75 mt-2" style={{ fontSize: '0.72rem' }}>Nền giấy ngà ấm</div>
            </div>
          </div>
          <div className="col-6 col-md-4 col-lg-2">
            <div className="p-3 rounded-3 text-white shadow-xs h-100" style={{ backgroundColor: '#0c1017', border: '1px solid #263042' }}>
              <div className="fw-bold small">Warm Charcoal</div>
              <div className="small opacity-75 font-monospace" style={{ fontSize: '0.72rem' }}>#0c1017</div>
              <div className="small opacity-75 mt-2" style={{ fontSize: '0.72rem' }}>Dark Mode chuẩn</div>
            </div>
          </div>
          <div className="col-6 col-md-4 col-lg-2">
            <div className="p-3 rounded-3 text-white shadow-xs h-100" style={{ backgroundColor: 'var(--color-success)' }}>
              <div className="fw-bold small">Semantic AA</div>
              <div className="small opacity-75 font-monospace" style={{ fontSize: '0.72rem' }}>#15803d</div>
              <div className="small opacity-75 mt-2" style={{ fontSize: '0.72rem' }}>Tương phản ≥ 4.5:1</div>
            </div>
          </div>
        </div>
      </section>

      {/* 2. Typography Scale & Vietnamese Diacritics Check */}
      <section className="mb-5">
        <h2 className="fs-4 fw-bold mb-3">2. Thang chữ &amp; Kiểm chuẩn Dấu Tiếng Việt (Lexend + Be Vietnam Pro)</h2>
        <div className="tr-card p-4 d-flex flex-column gap-3">
          <div className="p-3 rounded-3 bg-surface-subtle border border-subtle d-flex flex-wrap align-items-center justify-content-between gap-2">
            <div>
              <div className="small text-muted mb-1">Chuỗi kiểm thử dấu thanh &amp; mũ tiếng Việt (Display &amp; Body):</div>
              <div className="fs-3 fw-bold text-primary" style={{ fontFamily: 'var(--font-display)' }}>
                Đặt vé · Ưu đãi · Tưởng · Thưởng — Đêm Nhạc Mùa Thu Hà Nội
              </div>
            </div>
            <span className="tr-badge badge-success">
              <CheckCircle2 size={13} /> Không vỡ dấu
            </span>
          </div>

          <div className="d-flex align-items-baseline justify-content-between pb-2 border-bottom border-subtle">
            <span className="display-6 fw-bold">Tiêu đề cấp 1 (Display H1)</span>
            <span className="small text-muted font-monospace">48px / 36px • Lexend 600</span>
          </div>
          <div className="d-flex align-items-baseline justify-content-between pb-2 border-bottom border-subtle">
            <span className="fs-3 fw-bold">Tiêu đề cấp 2 (Section H2)</span>
            <span className="small text-muted font-monospace">28px • Lexend 600</span>
          </div>
          <div className="d-flex align-items-baseline justify-content-between pb-2 border-bottom border-subtle">
            <span className="fs-5 fw-bold">Tiêu đề cấp 3 (Card Title H3)</span>
            <span className="small text-muted font-monospace">22px • Lexend 600</span>
          </div>
          <div className="d-flex align-items-baseline justify-content-between pb-2 border-bottom border-subtle">
            <span>Văn bản thân bài (Body) — Trải nghiệm đọc tự nhiên tiếng Việt, line-height 1.6</span>
            <span className="small text-muted font-monospace">16px • Be Vietnam Pro 400</span>
          </div>
          <div className="d-flex align-items-baseline justify-content-between">
            <span className="fw-bold tabular-nums text-primary fs-5">Đồng hồ &amp; Tiền tệ: 2.500.000 ₫ (09:58)</span>
            <span className="small text-muted font-monospace">font-variant-numeric: tabular-nums</span>
          </div>
        </div>
      </section>

      {/* 3. Button Hierarchy */}
      <section className="mb-5">
        <h2 className="fs-4 fw-bold mb-3">3. Phân cấp Nút bấm (Button Hierarchy - Touch Target ≥ 44×44px)</h2>
        <div className="tr-card p-4 d-flex flex-wrap align-items-center gap-3">
          <button className="btn btn-tr-primary">Nút chính (Primary CTA)</button>
          <button className="btn btn-tr-secondary">Nút phụ (Secondary)</button>
          <button className="btn btn-tr-ghost">Nút ẩn (Ghost)</button>
          <button className="btn btn-tr-danger">Nút nguy hiểm (Danger)</button>
          <button className="btn btn-tr-primary" disabled>Bị vô hiệu (Disabled)</button>
        </div>
      </section>

      {/* 4. Badges */}
      <section className="mb-5">
        <h2 className="fs-4 fw-bold mb-3">4. Huy hiệu trạng thái (Status Badges)</h2>
        <div className="tr-card p-4 d-flex flex-wrap gap-2">
          <StatusBadge type="event" status="PUBLISHED" />
          <StatusBadge type="event" status="DRAFT" />
          <StatusBadge type="event" status="ENDED" />
          <StatusBadge type="order" status="CONFIRMED" />
          <StatusBadge type="order" status="AWAITING_PAYMENT" />
          <StatusBadge type="order" status="EXPIRED" />
          <StatusBadge type="order" status="REFUNDED" />
          <StatusBadge type="payment" status="SUCCEEDED" />
          <StatusBadge type="payment" status="FAILED" />
        </div>
      </section>

      {/* 5. Circular Reservation Timer */}
      <section className="mb-5">
        <h2 className="fs-4 fw-bold mb-3">5. Đồng hồ giữ chỗ vòng tròn SVG (Hold Countdown Timer)</h2>
        <div className="tr-card p-4 d-flex flex-wrap gap-4 align-items-center">
          <div>
            <div className="small text-muted mb-2">Trạng thái bình thường (&gt; 2 phút):</div>
            <ReservationTimer initialSeconds={450} />
          </div>
          <div>
            <div className="small text-muted mb-2">Trạng thái cảnh báo khẩn cấp (&lt; 2 phút):</div>
            <ReservationTimer initialSeconds={90} />
          </div>
          <div>
            <div className="small text-muted mb-2">Đã hết giờ giữ chỗ:</div>
            <ReservationTimer initialSeconds={0} />
          </div>
        </div>
      </section>

      {/* 6. Form Inputs */}
      <section className="mb-5">
        <h2 className="fs-4 fw-bold mb-3">6. Trường nhập liệu &amp; Kiểm lỗi trực tiếp (Form Inputs)</h2>
        <div className="tr-card p-4" style={{ maxWidth: 640 }}>
          <div className="mb-3">
            <label className="form-label small fw-semibold" htmlFor="dsValidInput">
              Trường hợp lệ (Nhãn cố định phía trên)
            </label>
            <input id="dsValidInput" type="text" className="form-control" defaultValue="Nguyễn Văn An" />
          </div>
          <div className="mb-0">
            <label className="form-label small fw-semibold" htmlFor="dsInvalidInput">
              Trường báo lỗi (Inline validation)
            </label>
            <input id="dsInvalidInput" type="email" className="form-control is-invalid" defaultValue="email-khong-hop-le" />
            <div className="invalid-feedback">Vui lòng nhập địa chỉ email hợp lệ.</div>
          </div>
        </div>
      </section>

      {/* 7. Skeleton & Empty & Error states */}
      <section className="mb-5">
        <h2 className="fs-4 fw-bold mb-3">7. Trạng thái Skeleton, Empty &amp; Error States</h2>
        <div className="row g-4">
          <div className="col-12 col-md-4">
            <div className="tr-card p-4 h-100">
              <div className="small fw-semibold text-muted mb-3">Shimmer Skeleton State:</div>
              <div className="tr-skeleton w-75 mb-2" style={{ height: 22 }} />
              <div className="tr-skeleton w-50 mb-3" style={{ height: 16 }} />
              <div className="tr-skeleton w-100" style={{ height: 48 }} />
            </div>
          </div>
          <div className="col-12 col-md-4">
            <ErrorState
              title="Lỗi tải dữ liệu"
              message="Không thể kết nối tới máy chủ phân phối vé."
              status={503}
              onRetry={() => alert('Thử lại')}
            />
          </div>
          <div className="col-12 col-md-4">
            <EmptyState
              title="Danh sách trống"
              description="Chưa có dữ liệu phát sinh trong bộ lọc hiện tại."
            />
          </div>
        </div>
      </section>

      {/* 8. Interactive Toast & Modal triggers */}
      <section className="mb-5">
        <h2 className="fs-4 fw-bold mb-3">8. Hộp thoại tương tác (Modal &amp; Toast)</h2>
        <div className="tr-card p-4 d-flex flex-wrap gap-3">
          <button className="btn btn-tr-secondary" onClick={() => setModalOpen(true)}>
            Mở Modal mẫu
          </button>
          <button className="btn btn-tr-secondary" onClick={() => setToastOpen(true)}>
            Hiện Toast thông báo
          </button>
        </div>

        {modalOpen && (
          <div
            className="position-fixed top-0 start-0 w-100 h-100 bg-dark bg-opacity-75 d-flex align-items-center justify-content-center p-3"
            style={{ zIndex: 1060 }}
            onClick={() => setModalOpen(false)}
          >
            <div
              className="tr-card p-4 bg-surface shadow-lg"
              style={{ maxWidth: 460, width: '100%' }}
              onClick={e => e.stopPropagation()}
            >
              <h3 className="fs-5 fw-bold mb-2">Tiêu đề Modal chuẩn</h3>
              <p className="text-muted small mb-4">
                Đây là component modal chuẩn hoá với focus ring rõ ràng và vùng chạm tối thiểu 44×44px.
              </p>
              <div className="d-flex justify-content-end gap-2">
                <button className="btn btn-tr-secondary" onClick={() => setModalOpen(false)}>Đóng</button>
                <button className="btn btn-tr-primary" onClick={() => setModalOpen(false)}>Xác nhận</button>
              </div>
            </div>
          </div>
        )}

        {toastOpen && (
          <div
            className="position-fixed bottom-0 end-0 m-4 tr-card p-3 shadow-lg bg-surface border-primary"
            style={{ maxWidth: 340, zIndex: 1060 }}
          >
            <div className="fw-bold small text-primary mb-1">Thông báo hệ thống</div>
            <div className="small text-muted mb-2">Vé điện tử của bạn đã được đồng bộ thành công!</div>
            <button className="btn btn-sm btn-tr-ghost p-0 small text-decoration-none" onClick={() => setToastOpen(false)}>
              Đóng thông báo
            </button>
          </div>
        )}
      </section>
    </div>
  );
};
