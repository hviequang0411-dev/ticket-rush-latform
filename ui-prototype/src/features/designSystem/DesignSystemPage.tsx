import React, { useState } from 'react';
import { ReservationTimer } from '../../components/ReservationTimer';
import { StatusBadge } from '../../components/StatusBadge';
import { ErrorState } from '../../components/DataStates';

export const DesignSystemPage: React.FC = () => {
  const [modalOpen, setModalOpen] = useState(false);
  const [toastOpen, setToastOpen] = useState(false);

  return (
    <div className="tr-container py-5">
      <div className="mb-5 pb-3 border-bottom border-subtle">
        <span className="badge bg-primary text-uppercase font-monospace mb-2">TICKETRUSH DESIGN SYSTEM 1.0</span>
        <h1 className="fs-2 fw-bold text-main mb-1">Hệ thống Thiết kế &amp; Thành phần Giao diện</h1>
        <p className="text-muted small mb-0">
          Tài liệu chuẩn hóa Tokens (Màu sắc, Typography, Spacing, Shadows) và tập hợp component dùng chung tuân thủ WCAG AA.
        </p>
      </div>

      {/* 1. Color Palette */}
      <section className="mb-5">
        <h2 className="fs-4 fw-bold mb-3">1. Bảng màu (Color Palette)</h2>
        <div className="row g-3">
          <div className="col-6 col-md-3">
            <div className="p-4 rounded-3 text-white text-center shadow-sm" style={{ backgroundColor: 'var(--color-primary)' }}>
              <div className="fw-bold">Primary Brand</div>
              <div className="small opacity-75 font-monospace">#ea580c (Cam Vermilion)</div>
            </div>
          </div>
          <div className="col-6 col-md-3">
            <div className="p-4 rounded-3 text-white text-center shadow-sm" style={{ backgroundColor: 'var(--text-main)' }}>
              <div className="fw-bold">Ink Navy Main</div>
              <div className="small opacity-75 font-monospace">#0f172a (Xanh mực)</div>
            </div>
          </div>
          <div className="col-6 col-md-3">
            <div className="p-4 rounded-3 text-dark text-center border shadow-sm" style={{ backgroundColor: 'var(--bg-app)' }}>
              <div className="fw-bold">Warm Ivory BG</div>
              <div className="small text-muted font-monospace">#faf9f6 (Trắng ngà)</div>
            </div>
          </div>
          <div className="col-6 col-md-3">
            <div className="p-4 rounded-3 text-white text-center shadow-sm" style={{ backgroundColor: 'var(--color-success)' }}>
              <div className="fw-bold">Success Green</div>
              <div className="small opacity-75 font-monospace">#15803d (WCAG AA)</div>
            </div>
          </div>
        </div>
      </section>

      {/* 2. Typography Scale */}
      <section className="mb-5">
        <h2 className="fs-4 fw-bold mb-3">2. Thang chữ (Type Scale - Font Be Vietnam Pro)</h2>
        <div className="tr-card p-4 d-flex flex-column gap-3">
          <div className="d-flex align-items-baseline justify-content-between pb-2 border-bottom border-subtle">
            <span className="display-6 fw-bold">Tiêu đề cấp 1 (H1)</span>
            <span className="small text-muted font-monospace">48px / 36px • Weight 600</span>
          </div>
          <div className="d-flex align-items-baseline justify-content-between pb-2 border-bottom border-subtle">
            <span className="fs-3 fw-bold">Tiêu đề cấp 2 (H2)</span>
            <span className="small text-muted font-monospace">28px • Weight 600</span>
          </div>
          <div className="d-flex align-items-baseline justify-content-between pb-2 border-bottom border-subtle">
            <span className="fs-5 fw-bold">Tiêu đề cấp 3 (H3)</span>
            <span className="small text-muted font-monospace">22px • Weight 600</span>
          </div>
          <div className="d-flex align-items-baseline justify-content-between pb-2 border-bottom border-subtle">
            <span className="fs-6 fw-medium">Tiêu đề cấp 4 (H4)</span>
            <span className="small text-muted font-monospace">18px • Weight 500</span>
          </div>
          <div className="d-flex align-items-baseline justify-content-between pb-2 border-bottom border-subtle">
            <span>Văn bản thân bài (Body) - Trải nghiệm đọc tự nhiên tiếng Việt, line-height 1.6</span>
            <span className="small text-muted font-monospace">16px • Weight 400</span>
          </div>
          <div className="d-flex align-items-baseline justify-content-between">
            <span className="fw-bold tabular-nums text-primary">Đồng hồ &amp; Tiền tệ: 2.500.000 ₫ (10:00)</span>
            <span className="small text-muted font-monospace">tabular-nums</span>
          </div>
        </div>
      </section>

      {/* 3. Button Hierarchy */}
      <section className="mb-5">
        <h2 className="fs-4 fw-bold mb-3">3. Phân cấp Nút bấm (Button Hierarchy)</h2>
        <div className="tr-card p-4 d-flex flex-wrap align-items-center gap-3">
          <button className="btn btn-tr-primary">Nút chính (Primary)</button>
          <button className="btn btn-tr-secondary">Nút phụ (Secondary)</button>
          <button className="btn btn-tr-ghost">Nút ẩn (Ghost)</button>
          <button className="btn btn-tr-danger">Nút nguy hiểm (Danger)</button>
          <button className="btn btn-tr-primary" disabled>Bị vô hiệu (Disabled)</button>
        </div>
      </section>

      {/* 4. Badges */}
      <section className="mb-5">
        <h2 className="fs-4 fw-bold mb-3">4. Huy hiệu trạng thái (Badges)</h2>
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

      {/* 5. Reservation Timer */}
      <section className="mb-5">
        <h2 className="fs-4 fw-bold mb-3">5. Đồng hồ giữ chỗ 10 phút (Hold Countdown Timer)</h2>
        <div className="tr-card p-4 d-flex flex-wrap gap-3">
          <div>
            <div className="small text-muted mb-1">Trạng thái bình thường (&gt; 2 phút):</div>
            <ReservationTimer initialSeconds={450} />
          </div>
          <div>
            <div className="small text-muted mb-1">Trạng thái cảnh báo khẩn cấp (&lt; 2 phút):</div>
            <ReservationTimer initialSeconds={90} />
          </div>
          <div>
            <div className="small text-muted mb-1">Đã hết giờ:</div>
            <ReservationTimer initialSeconds={0} />
          </div>
        </div>
      </section>

      {/* 6. Form Inputs */}
      <section className="mb-5">
        <h2 className="fs-4 fw-bold mb-3">6. Trường nhập liệu (Form Inputs &amp; Validation)</h2>
        <div className="tr-card p-4" style={{ maxWidth: 600 }}>
          <div className="mb-3">
            <label className="form-label small fw-semibold" htmlFor="dsValidInput">Trường hợp lệ (Có nhãn luôn hiển thị)</label>
            <input id="dsValidInput" type="text" className="form-control" defaultValue="Nguyễn Văn An" />
          </div>
          <div className="mb-3">
            <label className="form-label small fw-semibold" htmlFor="dsInvalidInput">Trường báo lỗi (Inline validation)</label>
            <input id="dsInvalidInput" type="email" className="form-control is-invalid" defaultValue="email-khong-hop-le" />
            <div className="invalid-feedback">Vui lòng nhập địa chỉ email hợp lệ.</div>
          </div>
        </div>
      </section>

      {/* 7. Skeleton & Empty & Error states */}
      <section className="mb-5">
        <h2 className="fs-4 fw-bold mb-3">7. Trạng thái rỗng, Lỗi &amp; Skeleton Loading</h2>
        <div className="row g-4">
          <div className="col-12 col-md-6">
            <div className="tr-card p-4">
              <div className="small fw-semibold text-muted mb-2">Skeleton State:</div>
              <div className="tr-skeleton w-75 mb-2" style={{ height: 20 }} />
              <div className="tr-skeleton w-50 mb-3" style={{ height: 16 }} />
              <div className="tr-skeleton w-100" style={{ height: 40 }} />
            </div>
          </div>
          <div className="col-12 col-md-6">
            <ErrorState
              title="Lỗi tải dữ liệu"
              message="Không thể kết nối tới máy chủ phân phối vé."
              status={503}
              onRetry={() => alert('Thử lại')}
            />
          </div>
        </div>
      </section>

      {/* 8. Interactive Toast & Modal triggers */}
      <section className="mb-5">
        <h2 className="fs-4 fw-bold mb-3">8. Hộp thoại (Modal &amp; Toast)</h2>
        <div className="tr-card p-4 d-flex gap-3">
          <button className="btn btn-tr-secondary" onClick={() => setModalOpen(true)}>
            Mở Modal mẫu
          </button>
          <button className="btn btn-tr-secondary" onClick={() => setToastOpen(true)}>
            Hiện Toast thông báo
          </button>
        </div>

        {/* Modal */}
        {modalOpen && (
          <div className="position-fixed top-0 start-0 w-100 h-100 bg-dark bg-opacity-75 z-3 d-flex align-items-center justify-content-center p-3">
            <div className="tr-card p-4 bg-surface shadow-lg" style={{ maxWidth: 460, width: '100%' }}>
              <h3 className="fs-5 fw-bold mb-2">Tiêu đề Modal</h3>
              <p className="text-muted small mb-4">
                Đây là component modal chuẩn hoá với focus trap và vùng chạm đạt chuẩn.
              </p>
              <div className="d-flex justify-content-end gap-2">
                <button className="btn btn-tr-secondary" onClick={() => setModalOpen(false)}>Đóng</button>
                <button className="btn btn-tr-primary" onClick={() => setModalOpen(false)}>Xác nhận</button>
              </div>
            </div>
          </div>
        )}

        {/* Toast */}
        {toastOpen && (
          <div className="position-fixed bottom-0 end-0 m-4 z-3 tr-card p-3 shadow-lg bg-surface border-primary" style={{ maxWidth: 320 }}>
            <div className="fw-bold small text-primary mb-1">Thông báo hệ thống</div>
            <div className="small text-muted mb-2">Vé của bạn đã được cập nhật thành công!</div>
            <button className="btn btn-sm btn-tr-ghost p-0 small text-decoration-none" onClick={() => setToastOpen(false)}>
              Bỏ qua
            </button>
          </div>
        )}
      </section>
    </div>
  );
};
