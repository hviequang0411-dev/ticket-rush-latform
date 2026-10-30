# TicketRush — High-Performance Event Ticketing Web Application

TicketRush là giao diện web hiện đại cho nền tảng phân phối vé sự kiện, concert và lễ hội âm nhạc chịu tải cao tại Việt Nam. Giao diện kết hợp năng lượng thị giác của sự kiện trực tiếp với độ chính xác của hệ thống giao dịch tài chính thời gian thực.

Xem chi tiết báo cáo nâng cấp giao diện tại **[UI_UPGRADE.md](./UI_UPGRADE.md)**.

---

## 1. Công nghệ & Kiến trúc
- **Framework & Runtime**: React 19 + TypeScript (strict) + Vite 6 + React Router 7
- **Styling & Design System**: Bootstrap 5.3 + SCSS (`@use "./tokens.scss"`) + CSS Custom Properties (hỗ trợ Light & Dark Mode lưu trạng thái trong `localStorage`)
- **Typography**: Google Fonts `Lexend` (Display/Headings) + `Be Vietnam Pro` (Body/UI) hỗ trợ đầy đủ dấu tiếng Việt và `tabular-nums` cho mọi con số
- **Thư viện chuyên dụng**:
  - `lucide-react`: Hệ thống icon vector sắc nét
  - `recharts`: Biểu đồ doanh thu 14 ngày theo chủ đề
  - `qrcode.react`: Tạo mã vé điện tử QR độ tương phản cao
- **Tích hợp Backend thực tế**:
  - Kết nối trực tiếp qua Gateway tới `catalog`, `inventory`, `order`, `payment`, `waiting-room` và định danh OIDC `Keycloak`.

---

## 2. Cách khởi chạy dự án

Từ thư mục gốc `mtp-ticket-platform/`:

```bash
# 1. Di chuyển vào thư mục ui-prototype
cd ui-prototype

# 2. Chạy môi trường phát triển (Dev server trên cổng 9000, proxy sang Gateway :8080)
npm run dev

# 3. Kiểm tra mã nguồn (Lint) & Đóng gói Production (Build)
npm run lint
npm run build
```

---

## 3. Sơ đồ các luồng & Route (Navigation Map)

### A. Dành cho Người Mua Vé (`PublicLayout`)
1. **Trang chủ (`/`)**:
   - Hero sân khấu với hiệu ứng quầng sáng động theo tone màu Poster sự kiện tiêu điểm.
   - Thanh tìm kiếm nổi bật (`.tr-search-dock`) tách biệt hoàn toàn với nút CTA của Hero.
   - Bento Strip với đồng hồ đếm ngược dạng lật số (`FlipCountdown`) và cam kết bảo vệ người mua 3 lớp.
   - Danh sách sự kiện nổi bật và thẻ khám phá theo 3 đô thị lớn (`Hà Nội`, `TP. Hồ Chí Minh`, `Đà Nẵng`).
2. **Danh sách & Tìm kiếm sự kiện (`/events`)**:
   - Lọc theo từ khóa, thành phố, thể loại, trạng thái mở bán và khoảng giá tối đa.
   - Sắp xếp theo ngày / giá tiền; hỗ trợ Bottom Sheet Filter trên thiết bị di động (`375px`).
3. **Chi tiết sự kiện (`/events/:slug`)**:
   - Poster nghệ thuật SVG tự sinh theo từng sự kiện và sơ đồ phân khu khán đài SVG.
   - Thanh tồn kho đổi màu trực quan (`Còn vé` / `Sắp hết` / `Hết vé`) và bộ đếm số lượng xúc giác (tối đa 10 vé/hạng).
   - Khung tổng tiền dính (Sticky Summary) trên Desktop và thanh dính đáy (Sticky Bottom Bar) trên Mobile.
4. **Phòng chờ ảo (`/queue?event=:slug`)**:
   - Hiển thị vị trí thực tế trong hàng chờ (`#position`), thời gian chờ dự kiến và tiến độ nhích lên tự động mỗi 2 giây.
   - Màn hình xác nhận khi được cấp chứng chỉ `X-Queue-Pass` và tự động chuyển hướng vào trang đặt vé.
5. **Xác nhận & Đặt vé (`/checkout`)**:
   - Form thông tin người nhận vé có kiểm lỗi trực tiếp (inline validation) và tự động điền từ hồ sơ Keycloak.
   - Lựa chọn cổng thanh toán (`VNPay`, `MoMo`, `Sandbox MOCK`) bảo vệ bằng khóa `Idempotency-Key`.
6. **Kết quả đơn hàng (`/order-result?orderCode=...`)**:
   - Tự động polling trạng thái đơn hàng và hiển thị đồng hồ giữ chỗ vòng tròn SVG (`ReservationTimer`).
   - Hiển thị thẻ vé điện tử đục lỗ (`.tr-ticket-pass`) ngay khi đơn được xác nhận (`CONFIRMED`).
7. **Đơn hàng của tôi (`/my-orders`)**:
   - Bộ lọc trạng thái đơn và **Saga State Machine Timeline** minh họa 4 bước xử lý đơn & nhánh bù trừ hoàn tiền.
8. **Ví vé điện tử (`/wallet`)**:
   - Danh sách thẻ vé điện tử đục lỗ có hiệu ứng ánh kim nhẹ và chế độ phóng to QR toàn màn hình để quét tại cổng.
9. **Tài khoản cá nhân (`/account`)**:
   - Hồ sơ định danh Single Sign-On (Keycloak OIDC).

### B. Bàn Quản trị Ban Tổ Chức (`AdminLayout`)
10. **Bảng điều khiển (`/admin`)**:
    - 4 thẻ KPI thời gian thực tích hợp đường xu hướng Sparkline SVG.
    - Biểu đồ vùng doanh thu 14 ngày (`recharts`) và bảng theo dõi trực tiếp tốc độ bán vé (`Live Rush Monitor`).
11. **Quản lý sự kiện, địa điểm & loại vé (`/admin/events`)**:
    - Bảng mật độ cao với header dính, tìm kiếm nhanh, chuyển trạng thái sự kiện và hộp thoại tạo/sửa sự kiện.
12. **Giám sát kho vé (`/admin/inventory`)**:
    - Thanh tiến độ xếp chồng 3 màu (`Đã thanh toán` / `Đang giữ chỗ 10p` / `Còn lại`) và danh sách các phiên giữ chỗ sắp hết hạn.
13. **Quản lý Đơn hàng & Thanh toán (`/admin/orders` & `/admin/payments`)**:
    - Tra cứu toàn bộ đơn hàng, kiểm tra chữ ký HMAC-SHA512 của Webhook và xem Raw JSON Payload.
14. **Soát vé tại cổng (`/admin/check-in`)**:
    - Khung ngắm quét mã QR giả lập và phản hồi xác thực tức thì (`Hợp lệ` / `Từ chối qua cổng`).

### C. Design System (`/design-system`)
15. Trình diễn trực quan toàn bộ tokens thiết kế 2.0, kiểm chuẩn dấu tiếng Việt (`Đặt vé · Ưu đãi · Tưởng · Thưởng`), đồng hồ giữ chỗ vòng tròn SVG, nút bấm, huy hiệu, form inputs và các trạng thái dữ liệu.
