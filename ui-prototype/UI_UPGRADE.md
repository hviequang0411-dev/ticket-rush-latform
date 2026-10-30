# TicketRush — Báo Cáo Nâng Cấp Toàn Diện Giao Diện (UI Upgrade 2.0)

Tài liệu này mô tả chi tiết các quyết định thiết kế, hệ thống Design Tokens 2.0, danh sách tinh chỉnh trên từng màn hình và bằng chứng kiểm thử đối chiếu Trước / Sau (`docs/screenshots/`).

---

## 1. Triết lý & Bản sắc thị giác (Visual Identity)

TicketRush 2.0 kết hợp **năng lượng sân khấu sự kiện** (Poster nghệ thuật SVG, ánh sáng sân khấu lan tỏa ở Hero, vé điện tử đục lỗ có hiệu ứng ánh kim) với **độ chính xác tài chính của ứng dụng Fintech** (mọi con số giá tiền, đồng hồ giữ chỗ, vị trí hàng chờ đều dùng `font-variant-numeric: tabular-nums`, phân cấp thông tin rõ ràng, bảng quản trị mật độ cao).

### Điểm mới nổi bật trong bản nâng cấp:
1. **Hệ thống Typography hỗ trợ tiếng Việt tuyệt đối**:
   - **Display / Headings**: `Lexend` (weights 500, 600, 700) — nét chữ hiện đại, hình học cân đối, hiển thị rõ mọi dấu thanh và dấu mũ tiếng Việt (`Đặt vé · Ưu đãi · Tưởng · Thưởng`).
   - **Body / UI Controls**: `Be Vietnam Pro` (weights 400, 500, 600, 700) — tối ưu hóa cho trải nghiệm đọc tiếng Việt dài và nhãn giao diện.
   - **Monospace / Mã vé / Payload**: `JetBrains Mono` / `Fira Code`.
2. **Hệ thống Màu sắc Sáng / Tối (Light & Dark Mode) lưu trong `localStorage`**:
   - **Light Mode**: Nền giấy ngà ấm (`#faf8f5`), bề mặt thẻ trắng tinh (`#ffffff`) và đá ấm (`#f3f0ea`), chữ chính xanh mực đậm (`#0f172a`).
   - **Dark Mode**: Nền than ấm (`#0c1017` — tuyệt đối không dùng `#000000` gây chói), bề mặt phân lớp (`#151b26`, `#1d2534`), khung mã QR luôn giữ nền trắng tinh (`.tr-qr-frame`) để máy quét quang học tại cổng đọc chính xác 100%.
   - **Màu nhấn (CTA Accent)**: Đỏ cam Vermilion (`#ea580c` ở Light / `#f97316` ở Dark) kết hợp màu phụ Deep Teal (`#0d9488`) cho chỉ số dữ liệu.
3. **Kỷ luật Đổ bóng & Viền (2-Level Elevation)**:
   - Tối đa 2 cấp shadow mềm (`--shadow-sm`, `--shadow-md`), ưu tiên phân tách khối bằng viền mảnh `1px solid var(--border-subtle)`.

---

## 2. Điểm nhấn "Wow" trên từng trang (Signature Highlights)

| Khu vực / Trang | File nguồn | Nâng cấp giao diện & Vi tương tác (Micro-interactions) |
| :--- | :--- | :--- |
| **Khung bố cục Người mua & Admin** | `src/components/PublicLayout.tsx`<br>`src/components/AdminLayout.tsx` | • Bổ sung thanh trạng thái hệ thống thời gian thực (`.tr-live-dot`).<br>• Đồng bộ và lưu trữ chế độ Sáng/Tối qua `localStorage` (`ticketrush.theme`).<br>• Thêm ngăn kéo điều hướng di động (Mobile Drawer) đáp ứng hoàn hảo ở `375px`.<br>• Chuyển trang mượt mà với `.tr-page-enter` (tự động tắt khi `prefers-reduced-motion: reduce`). |
| **Trang chủ (`/`)** | `src/features/home/HomePage.tsx` | • **Hero Sân khấu**: Hiệu ứng quầng sáng nền (`.tr-hero-glow`) đổi màu theo đúng bảng màu của Poster sự kiện tiêu điểm.<br>• **Thanh tìm kiếm tách biệt (`.tr-search-dock`)**: Khắc phục triệt để lỗi đè lên nút CTA của Hero.<br>• **Bento Strip & Flip-Digit Countdown**: Đồng hồ đếm ngược dạng lật số (`.tr-flip-digit`) cho đợt mở bán nóng tiếp theo. |
| **Chi tiết sự kiện (`/events/:slug`)** | `src/features/events/EventDetailPage.tsx` | • Sơ đồ phân khu khán đài SVG trực quan.<br>• Thanh tiến độ tồn kho đổi màu theo ngữ cảnh (`Còn vé` xanh lá / `Sắp hết` vàng cam / `Hết vé` đỏ).<br>• Bộ chọn số lượng vé xúc giác (`.tr-stepper-btn`) và thanh tổng tiền dính đáy trên Mobile. |
| **Phòng chờ ảo (`/queue`)** | `src/features/queue/WaitingRoomPage.tsx` | • Hiển thị trực quan vị trí hàng chờ `#position` và thời gian chờ dự kiến `~Xs` với `tabular-nums`.<br>• Thanh tiến độ chuyển động mượt và màn hình chúc mừng nổi bật khi được cấp quyền `X-Queue-Pass` (`status.admitted`). |
| **Giữ chỗ & Thanh toán (`/checkout`)** | `src/features/checkout/CheckoutPage.tsx`<br>`src/components/ReservationTimer.tsx` | • **Đồng hồ giữ chỗ vòng tròn SVG**: Vòng tròn tiến độ giảm dần theo thời gian thực, tự động chuyển sang màu cảnh báo đỏ khi còn dưới 2 phút.<br>• Thẻ chọn cổng thanh toán (`VNPay`, `MoMo`, `Sandbox`) rõ ràng và minh bạch. |
| **Kết quả & Ví vé (`/order-result`, `/wallet`)** | `src/features/checkout/OrderResultPage.tsx`<br>`src/features/wallet/TicketWalletPage.tsx` | • **Thẻ vé điện tử đục lỗ (`.tr-ticket-pass`)**: Thiết kế như vé concert/boarding pass thật với rãnh khuyết bán nguyệt 2 bên (`.tr-ticket-divider`) và hiệu ứng ánh kim nhẹ khi di chuột.<br>• Chế độ phóng to toàn màn hình độ tương phản cao phục vụ máy quét tại cổng. |
| **Đơn hàng của tôi (`/my-orders`)** | `src/features/orders/MyOrdersPage.tsx` | • **Saga State Machine Timeline (`OrderSagaTimeline`)**: Minh hoạ trực quan 4 bước xử lý đơn phân tán (`Tiếp nhận → Giữ ghế 10p → Thanh toán → Xuất vé / Nhánh bù trừ Hoàn tiền`). |
| **Bàn Quản trị BTC (`/admin/*`)** | `src/features/admin/*.tsx` | • **KPI Cards kèm Sparkline SVG**: 4 thẻ chỉ số đầu trang có đường biểu đồ xu hướng thu nhỏ.<br>• **Bảng dữ liệu mật độ cao (`.tr-table-card`)**: Header dính (`sticky`), dòng kẻ xen kẽ nhẹ, các cột số tiền và số lượng căn phải chuẩn `tabular-nums`. |

---

## 3. Cam kết Bảo toàn Logic Nghiệp vụ (Zero Logic Regression)

Toàn bộ vùng cấm được giữ nguyên 100% (`git diff --stat -- ui-prototype/src/api ui-prototype/src/types ui-prototype/src/lib ui-prototype/src/routes.tsx` hoàn toàn trống):
- Không thêm bất kỳ thư viện runtime bên thứ ba nào (0 dependencies mới), giúp bundle gọn nhẹ và tương thích tuyệt đối.
- Giữ nguyên toàn bộ API client (`src/api/**`), định dạng tiền tệ & múi giờ (`src/lib/format.ts`), định tuyến (`src/routes.tsx`), và state machine của các trang.

---

## 4. Ảnh chụp màn hình Trước & Sau (`docs/screenshots/`)

Bộ ảnh chụp tự động ở 2 độ phân giải chuẩn **Desktop (`1440×900`)** và **Mobile (`375×812`)** được lưu trực tiếp tại `ui-prototype/docs/screenshots/`:
- `before-home-1440.png` / `after-home-1440.png` & `before-home-375.png` / `after-home-375.png`
- `before-events-1440.png` / `after-events-1440.png` & `before-events-375.png` / `after-events-375.png`
- `before-event-detail-1440.png` / `after-event-detail-1440.png` & `before-event-detail-375.png` / `after-event-detail-375.png`
- `before-queue-1440.png` / `after-queue-1440.png` & `before-queue-375.png` / `after-queue-375.png`
- `before-order-result-1440.png` / `after-order-result-1440.png` & `before-order-result-375.png` / `after-order-result-375.png`
- `before-my-orders-1440.png` / `after-my-orders-1440.png` & `before-my-orders-375.png` / `after-my-orders-375.png`
- `before-wallet-1440.png` / `after-wallet-1440.png` & `before-wallet-375.png` / `after-wallet-375.png`
- `before-design-system-1440.png` / `after-design-system-1440.png` & `before-design-system-375.png` / `after-design-system-375.png`
- `before-admin-dashboard-1440.png` / `after-admin-dashboard-1440.png` & `before-admin-dashboard-375.png` / `after-admin-dashboard-375.png`
- `before-admin-inventory-1440.png` / `after-admin-inventory-1440.png` & `before-admin-inventory-375.png` / `after-admin-inventory-375.png`

