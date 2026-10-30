# TicketRush — High-Performance Event Ticketing Web Prototype

TicketRush là nguyên mẫu giao diện web hoàn chỉnh cho nền tảng phân phối vé sự kiện và hòa nhạc chịu tải cao tại Việt Nam.

Giao diện được thiết kế độc lập, sạch, sáng, không dùng template rập khuôn và phản ánh trung thực toàn bộ luồng nghiệp vụ mua vé, hàng chờ ảo, giữ chỗ 10 phút, và quản trị thực tế của hệ thống.

---

## 1. Công nghệ & Kiến trúc
- **Framework & Runtime**: React 19 + TypeScript + Vite 6 + React Router 7
- **Styling & Design System**: Bootstrap 5.3 + SCSS Modules + CSS Variables (Hỗ trợ Dark/Light Theme đầy đủ)
- **Thư viện chuyên dụng**:
  - `lucide-react`: Hệ thống icon chuẩn SVG
  - `recharts`: Biểu đồ doanh thu 14 ngày
  - `qrcode.react`: Tạo vé điện tử QR vector
- **Mock Data & Resilience API Client**:
  - Dữ liệu mock độc lập trong `src/mocks/mockData.ts` (12 sự kiện phong phú tại Hà Nội, TP.HCM, Đà Nẵng, cùng lịch sử đơn hàng, vé điện tử và webhook log).
  - Lớp `api/apiClient.ts` giả lập độ trễ mạng thực (300–800ms) và hỗ trợ giả lập trực tiếp các lỗi RFC 7807 (409 Conflict, 429 Rate Limit, 503 Service Unavailable).

---

## 2. Cách khởi chạy dự án

Từ thư mục gốc `mtp-ticket-platform/`:

```bash
# 1. Di chuyển vào thư mục ui-prototype
cd ui-prototype

# 2. Chạy môi trường phát triển (Dev server)
npm run dev

# 3. Kiểm tra kiểm thử mã & Type check
npm run lint
npm run build
```

Sau khi chạy lệnh `npm run dev`, mở trình duyệt truy cập: `http://localhost:5173`

---

## 3. Sơ đồ các luồng & Route (Navigation Map)

### A. Dành cho Người Mua Vé (Public Layout)
1. **Trang chủ (`/`)**:
   - Hero carousel 3 sự kiện tiêu điểm với typography lớn và gradient động.
   - Thanh tìm kiếm dính nổi bật (Từ khoá + Thành phố + Ngày).
   - Bộ lọc danh mục (Concert, Festival, Sân khấu, Thể thao, Workshop).
   - Danh sách "Sự kiện nổi bật" và khám phá theo thành phố.
2. **Danh sách & Tìm kiếm sự kiện (`/events`)**:
   - Lọc theo từ khóa, thành phố, thể loại, trạng thái mở bán và khoảng giá tối đa.
   - Sắp xếp theo ngày / giá tiền.
   - Chế độ Bottom sheet cho bộ lọc trên thiết bị di động (Responsive mobile-first).
3. **Chi tiết sự kiện (`/events/:slug`)**:
   - Poster đồ hoạ SVG tạo tự động theo tone màu riêng của sự kiện.
   - Sơ đồ vị trí sân khấu SVG vector.
   - Danh sách các hạng vé với bộ đếm số lượng (tối đa 10 vé/loại), hiển thị tồn kho và thanh cảnh báo khi còn dưới 30% vé.
   - Khung tổng tiền dính (Sticky Sidebar) trên Desktop và thanh dính đáy (Sticky Bottom Bar) trên Mobile.
4. **Phòng chờ ảo (`/queue?event=:slug`)**:
   - Mô phỏng cơ chế Virtual Waiting Room khi sự kiện mở bán quá tải.
   - Vị trí trong hàng (#428), thời gian chờ ước tính, thanh tiến độ và cảnh báo không tải lại trang.
   - Tự động chuyển hướng vào đặt vé khi đến lượt.
5. **Xác nhận & Đặt vé (`/checkout`)**:
   - Tóm tắt đơn hàng và đồng hồ đếm ngược giữ chỗ 10 phút (`ReservationTimer`).
   - Form thông tin người nhận vé (có inline validation).
   - Lựa chọn cổng thanh toán (Sandbox MOCK, VNPay, MoMo).
   - Nút kiểm thử trực tiếp lỗi API (409, 429, 503) cho người đánh giá.
6. **Cổng thanh toán giả lập (`/mock-payment?orderCode=...`)**:
   - 2 nút mô phỏng: "Thanh toán thành công" hoặc "Từ chối / Huỷ".
7. **Kết quả đơn hàng (`/order-result?orderCode=...`)**:
   - Trạng thái thành công (`CONFIRMED`) kèm vé điện tử vector.
   - Trạng thái thất bại (`FAILED`), hết hạn (`EXPIRED`) hoặc hoàn tiền (`REFUNDED`).
8. **Đơn hàng của tôi (`/my-orders`)**:
   - Tabs lọc trạng thái đơn: Đã xác nhận / Chờ thanh toán / Hết hạn & Hoàn tiền.
   - Saga Timeline minh họa 4 bước xử lý: Tạo đơn → Giữ ghế → Chờ thanh toán → Xuất vé / Nhánh bù trừ hoàn tiền.
9. **Ví vé điện tử (`/wallet`)**:
   - Danh sách thẻ vé điện tử (E-Ticket boarding pass).
   - Chế độ hiển thị toàn màn hình (Full-screen QR) tối ưu độ tương phản cho máy quét tại cửa.
   - Nút thêm sự kiện vào Lịch.
10. **Tài khoản cá nhân (`/account`)**:
    - Hồ sơ người dùng liên kết Single Sign-On (Keycloak) và tùy chọn thông báo.
11. **Trang hệ thống**:
    - 404 Không tìm thấy trang (`/*`)
    - 403 Từ chối quyền truy cập (`/forbidden`)
    - Bảo trì định kỳ (`/maintenance`)

### B. Bàn Quản trị Ban Tổ Chức (Admin Layout)
12. **Bảng điều khiển (`/admin`)**:
    - KPIs thời gian thực: Doanh thu hôm nay, Đơn hàng, Tỉ lệ lấp đầy vé, Lượt giữ chỗ đang active.
    - Biểu đồ vùng (Area Chart) doanh thu 14 ngày qua bằng `recharts`.
    - Bộ theo dõi bán vé trực tiếp của các đợt mở bán hot (Live Rush Monitor).
13. **Quản lý sự kiện, địa điểm & loại vé (`/admin/events`)**:
    - Bảng danh sách sự kiện kèm bộ lọc và tìm kiếm nhanh.
    - Form tạo sự kiện mới với validation chặt chẽ (slug regex `a-z0-9-`, giá vé, số lượng).
14. **Giám sát kho vé (`/admin/inventory`)**:
    - Thanh tiến độ xếp chồng 3 màu cho từng hạng vé: Đã bán (Xanh lá) / Đang giữ (Vàng) / Còn lại (Xám).
    - Bảng danh sách các phiên giữ chỗ sắp hết hạn (Active Hold TTL).
15. **Quản lý Đơn hàng & Thanh toán (`/admin/orders` & `/admin/payments`)**:
    - Danh sách đơn hàng toàn hệ thống.
    - Tab kiểm tra chữ ký Webhook (HMAC SHA512) và Raw JSON Payload của cổng thanh toán.
16. **Soát vé tại cổng (`/admin/check-in`)**:
    - Khung ngắm camera giả lập.
    - Nhập hoặc quét mã QR kiểm tra tức thì: Hợp lệ / Đã sử dụng trước đó / Không hợp lệ.
    - Thống kê tỷ lệ khách đã check-in theo thời gian thực.

### C. Design System (`/design-system`)
17. Trình diễn trực quan toàn bộ tokens thiết kế:
    - Bảng màu: Nền trắng ngà (`#faf9f6`), chữ xanh mực đậm (`#0f172a`), màu nhấn cam vermilion (`#ea580c`), các màu trạng thái WCAG AA.
    - Thang chữ: Font Google Fonts `Be Vietnam Pro` (400, 500, 600), tabular-nums cho giá tiền và đồng hồ.
    - Button hierarchy, Badges, Đồng hồ giữ chỗ 10 phút, Inputs validation, Skeleton loading, Empty state, Toast và Modal.

---

## 4. Tiêu chuẩn khả năng tiếp cận (Accessibility) & Tương thích
- Mọi vùng chạm tối thiểu 44×44px (`.btn-touch-target`).
- Focus ring rõ ràng (`--focus-ring`).
- Toàn bộ poster sự kiện vẽ bằng SVG/CSS tinh tế, không phụ thuộc ảnh bên ngoài.
- Tuân thủ `prefers-reduced-motion` đối với người dùng nhạy cảm với chuyển động.
- Đáp ứng hoàn hảo trên mọi kích thước màn hình: Mobile (375px), Tablet (768px), Laptop/Desktop (1024px - 1440px).
