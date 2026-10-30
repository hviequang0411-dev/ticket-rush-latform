# Học TicketRush: code và các kỹ thuật xử lý

> Tài liệu này dành cho việc **hiểu và học** từ chính code của dự án. Mỗi kỹ thuật được trình bày theo cùng một
> mạch: **vấn đề là gì → cách làm sai hay gặp → dự án làm thế nào → code nằm ở đâu → tự thử để hiểu**.
>
> Đọc [plan.md](plan.md) trước để biết hệ thống gồm những gì. Các quyết định lớn có giải thích sâu hơn trong
> [docs/adr/](adr/).

## Mục lục

- [Phần 0. Cách đọc repo này](#phần-0-cách-đọc-repo-này)
- [Phần 1. Nền tảng: microservice, gateway, đăng nhập](#phần-1-nền-tảng-microservice-gateway-đăng-nhập)
- [Phần 2. Spring cơ bản cần nắm trước](#phần-2-spring-cơ-bản-cần-nắm-trước)
- [Phần 3. Chống bán quá số ghế (oversell)](#phần-3-chống-bán-quá-số-ghế-oversell)
- [Phần 4. Giữ ghế có thời hạn và job dọn dẹp](#phần-4-giữ-ghế-có-thời-hạn-và-job-dọn-dẹp)
- [Phần 5. Idempotency: gọi lại không gây hậu quả](#phần-5-idempotency-gọi-lại-không-gây-hậu-quả)
- [Phần 6. Ranh giới transaction](#phần-6-ranh-giới-transaction)
- [Phần 7. Kafka và Spring Cloud Stream](#phần-7-kafka-và-spring-cloud-stream)
- [Phần 8. Transactional Outbox](#phần-8-transactional-outbox)
- [Phần 9. Saga orchestration và bù trừ](#phần-9-saga-orchestration-và-bù-trừ)
- [Phần 10. Thanh toán và webhook](#phần-10-thanh-toán-và-webhook)
- [Phần 11. Resilience4j: chịu lỗi khi gọi service khác](#phần-11-resilience4j-chịu-lỗi-khi-gọi-service-khác)
- [Phần 12. Gateway: rate limit, phòng chờ ảo, lập trình reactive](#phần-12-gateway-rate-limit-phòng-chờ-ảo-lập-trình-reactive)
- [Phần 13. Bảo mật](#phần-13-bảo-mật)
- [Phần 14. Kiểm thử](#phần-14-kiểm-thử)
- [Phần 15. Quan sát hệ thống (observability)](#phần-15-quan-sát-hệ-thống-observability)
- [Phần 16. Bài học hạ tầng: những lỗi thật đã gặp](#phần-16-bài-học-hạ-tầng-những-lỗi-thật-đã-gặp)
- [Phần 17. Lộ trình tự học và bài tập](#phần-17-lộ-trình-tự-học-và-bài-tập)

---

## Phần 0. Cách đọc repo này

**Đừng đọc từ đầu đến cuối.** Mỗi service có hàng trăm file, phần lớn do JHipster sinh ra và gần như giống hệt nhau
giữa các entity: `Venue`, `Event`, `TicketType` đều có Entity, Repository, Service, ServiceImpl, DTO, Mapper,
Resource và ResourceIT. Đọc một bộ là hiểu cả trăm file.

Thứ tự gợi ý:

1. **Một bộ CRUD sinh sẵn** để hiểu khung, ví dụ `catalog/.../domain/Venue.java` → `repository/VenueRepository.java` → `service/impl/VenueServiceImpl.java` → `web/rest/VenueResource.java`.
2. **Code tự viết**, theo đúng thứ tự các phần dưới đây. Danh sách file tự viết nằm ở mục 4 của [plan.md](plan.md).
3. **Test đi kèm** với từng phần. Test là "tài liệu chạy được": nó cho biết code được kỳ vọng làm gì trong tình huống xấu nhất.

Mẹo: `git log --oneline` và `git show <commit>` cho thấy từng tính năng được thêm vào như thế nào. Message commit
viết rõ vấn đề và cách giải quyết, ví dụ `git show c84d1a7` là commit chống oversell.

---

## Phần 1. Nền tảng: microservice, gateway, đăng nhập

### 1.1. Vì sao tách thành nhiều service

Mỗi service sở hữu **một vùng nghiệp vụ và dữ liệu của nó**: catalog biết giá, inventory biết còn bao nhiêu ghế,
payment biết tiền đã về chưa. Lợi ích là mỗi phần có thể scale và deploy riêng, và một phần hỏng không kéo theo
tất cả. Cái giá phải trả là **dữ liệu không còn nằm chung một transaction**. Gần như mọi kỹ thuật khó trong dự án
(outbox, saga, idempotency) đều sinh ra để trả cái giá này.

> **Database per service:** inventory không bao giờ `SELECT` vào DB của order. Nếu cần biết đơn hàng, nó chỉ giữ
> `orderId` (một con số), không có foreign key. Quan hệ thật (khoá ngoại) chỉ tồn tại *bên trong* một service, ví
> dụ `stock_hold.stock_id → ticket_stock.id`.

### 1.2. Gateway: cửa ngõ duy nhất

Trình duyệt chỉ nói chuyện với **gateway**. Gateway nhìn đường dẫn `/services/order/api/checkout` → bỏ 2 đoạn đầu →
gửi `/api/checkout` tới một instance của service `order`. Gateway không biết trước địa chỉ IP của order: nó hỏi
**Consul**.

- Cấu hình định tuyến: `gateway/src/main/resources/config/application.yml`, mục `discovery.locator` (tự tạo route cho mọi service trong Consul) và `routes` (route riêng cho checkout có rate limit).

### 1.3. Service discovery với Consul

Khi khởi động, mỗi service tự **đăng ký** với Consul ("tôi là `inventory`, ở `172.20.0.12:8082`") và Consul liên
tục gọi `/management/health` để biết nó còn sống không. Khi order cần gọi inventory, nó gọi `http://inventory/...`.
Spring Cloud LoadBalancer sẽ đổi `inventory` thành một địa chỉ thật lấy từ Consul.

> Đọc thêm: [ADR 0001](adr/0001-service-discovery-consul.md), và lỗi "gọi nhầm instance đã chết" ở [Phần 16](#phần-16-bài-học-hạ-tầng-những-lỗi-thật-đã-gặp).

### 1.4. Đăng nhập: OAuth2/OIDC với Keycloak

- **Người dùng** đăng nhập qua Keycloak và nhận một **JWT** (token có chữ ký). Trong token có claim `roles`, ví dụ `["ROLE_USER"]`.
- Mỗi service là một **resource server**: kiểm tra chữ ký của JWT bằng khoá công khai của Keycloak. Service không cần hỏi lại Keycloak cho mỗi request.
- **Service gọi service** dùng luồng **client credentials**: order tự xin token cho chính nó (client `internal`), và token đó mang `ROLE_SERVICE`.

Vì sao order không chuyển tiếp token của người mua xuống inventory? Vì khi đó API giữ ghế phải mở cho `ROLE_USER`,
và người mua có thể tự gọi `POST /api/internal/reservations` với số lượng tối đa để **giữ hết vé** mà không cần thanh
toán. Tách token đảm bảo chỉ service mới gọi được API nội bộ.

- Code: `order/.../config/ServiceClientsConfiguration.java` (`OAuth2ClientHttpRequestInterceptor` tự gắn token của service), và mỗi `SecurityConfiguration.java` với dòng `.requestMatchers("/api/internal/**").hasAuthority(AuthoritiesConstants.SERVICE)`.

---

## Phần 2. Spring cơ bản cần nắm trước

Nếu chưa vững các khái niệm dưới đây, hãy nắm chúng trước khi đọc tiếp. Chúng xuất hiện ở khắp nơi.

| Khái niệm | Cần hiểu tới mức | Ví dụ trong dự án |
|---|---|---|
| Bean và constructor injection | Spring tạo object và đưa vào constructor; không dùng `@Autowired` trên field | Constructor của mọi `@Service` |
| `@Transactional` | Mở transaction trước method, commit khi xong, rollback khi có `RuntimeException`. Chỉ có tác dụng khi gọi **qua proxy** (gọi từ class khác), không có tác dụng khi gọi `this.method()` | `ReservationService` (đặt ở cấp class) |
| JPA dirty checking | Trong transaction, sửa field của entity đang được quản lý là đủ: Hibernate tự `UPDATE` khi commit, không cần gọi `save()` | `hold.setStatus(HoldStatus.RELEASED)` trong `ReservationService.release` |
| `@Version` | Optimistic locking: mỗi UPDATE kèm `WHERE version = ?`; nếu ai đó đã sửa trước thì ném lỗi, và client nhận 409 | `TicketStock`, `Order`, `Payment` |
| `@Scheduled` | Chạy method định kỳ | `HoldExpiryJob`, `OutboxRelay.relay`, `PaymentExpiryJob` |
| Record (Java 16+) | Class bất biến gọn gàng cho DTO và sự kiện | `ReservationRequest`, `PaymentResultEvent` |
| `Clock` được inject | Không gọi `Instant.now()` trực tiếp, để test "tua nhanh thời gian" được | `ClockConfiguration` + `MutableClock` trong test |

---

## Phần 3. Chống bán quá số ghế (oversell)

> Code: [`inventory/.../repository/TicketStockRepository.java`](../inventory/src/main/java/com/ticketrush/inventory/repository/TicketStockRepository.java),
> [`inventory/.../service/reservation/ReservationService.java`](../inventory/src/main/java/com/ticketrush/inventory/service/reservation/ReservationService.java).
> Quyết định: [ADR 0003](adr/0003-oversell-prevention.md). Test: `ReservationServiceIT`.

### 3.1. Vấn đề: race condition

Còn **1 ghế**. Hai người bấm mua cùng lúc. Code "tự nhiên" nhất:

```java
TicketStock stock = repo.findById(id);           // A đọc: còn 1      B đọc: còn 1
if (stock.getAvailable() >= qty) {               // A: 1 >= 1 đúng    B: 1 >= 1 đúng
    stock.setAvailable(stock.getAvailable() - qty);
    repo.save(stock);                            // A ghi 0           B ghi 0
}                                                // → bán 2 vé cho 1 ghế
```

Giữa lúc **đọc** và lúc **ghi** có một khoảng hở; request khác chen vào đúng khoảng đó. Đây gọi là lỗi
**read-modify-write** hay **lost update**.

### 3.2. Các cách sửa và vì sao không chọn

| Cách | Vấn đề |
|---|---|
| `synchronized` trong Java | Chỉ khoá trong *một* JVM. Chạy 3 instance inventory thì vô dụng |
| Chỉ dùng `@Version` | Đúng, nhưng khi mở bán, gần như mọi request đều thất bại vì xung đột version rồi phải retry. Hiệu năng sụp |
| Redis lock (Redisson) | Đúng *nếu* lock đúng. Lock có thời hạn: nếu JVM bị dừng do GC lâu hơn thời hạn, hai bên cùng giữ lock. Thêm một hệ thống phải luôn sống mới bán được vé |
| `SELECT ... FOR UPDATE` | Đúng, nhưng giữ khoá trong lúc chạy code Java, dễ deadlock |

### 3.3. Cách dự án làm: một câu UPDATE có điều kiện

```java
@Modifying(flushAutomatically = true)
@Query("update TicketStock s set s.availableQuantity = s.availableQuantity - :quantity, s.version = s.version + 1 " +
       "where s.id = :id and s.availableQuantity >= :quantity")
int tryTake(@Param("id") Long id, @Param("quantity") int quantity);
```

**Kiểm tra và ghi nằm trong cùng một câu lệnh.** PostgreSQL khoá dòng trong lúc thực thi câu UPDATE, nên request thứ
hai phải chờ request thứ nhất xong, rồi mới đánh giá lại `WHERE available >= qty` trên giá trị *mới*. Kết quả trả về:
`1` là lấy được ghế, `0` là hết ghế. Không có khoảng hở nào để chen vào.

### 3.4. Ba lớp bảo vệ bổ sung

1. **All-or-nothing cho đơn nhiều loại vé.** Một đơn mua 2 VIP và 1 Thường: thiếu loại nào thì ném `InsufficientStockException`, và `@Transactional` rollback cả phần đã trừ.
2. **Khoá theo thứ tự cố định để tránh deadlock.** Đơn A giữ VIP rồi chờ Thường, trong khi đơn B giữ Thường rồi chờ VIP: hai bên chờ nhau mãi. Cách sửa là luôn cập nhật theo `id` tăng dần:
   ```java
   stocks.sort(Comparator.comparing(TicketStock::getId));
   ```
   Test `concurrentMultiItemOrdersDoNotDeadlockOrOversell` cố tình cho một nửa số người mua liệt kê loại vé theo thứ tự ngược lại.
3. **Database tự bảo vệ:** ràng buộc `CHECK (available_quantity >= 0 AND available_quantity <= total_quantity)` trong `inventory/.../liquibase/changelog/20261029120000_inventory_concurrency.xml`. Kể cả khi code có lỗi, DB vẫn từ chối việc bán quá thay vì âm thầm bán.

### 3.5. Một lỗi tinh vi: `clearAutomatically`

Bản đầu tiên viết `@Modifying(flushAutomatically = true, clearAutomatically = true)`. `clear` xoá persistence context
sau câu UPDATE, khiến các entity `StockHold` đang được quản lý trở thành **detached**. Hậu quả: `hold.setStatus(...)`
sau đó **không được ghi xuống DB**. Ghế đã trả về kho nhưng hold vẫn `HELD`, và lần quét sau lại trả ghế thêm lần
nữa. Bài học: hiểu rõ từng tuỳ chọn trước khi bật, và comment trong repository đã ghi lại lý do.

### 3.6. Cùng kỹ thuật cho soát vé: một vé chỉ vào cổng một lần

> Code: `order/.../repository/TicketRepository.java` (`checkIn`), `order/.../service/ticket/TicketService.java`. Test: `TicketResourceIT`.

Khi đơn CONFIRMED, saga phát mỗi ghế một vé (bảng `ticket`) trong **cùng transaction** với việc xác nhận đơn, nên không
có đơn CONFIRMED nào thiếu vé; ràng buộc unique `(order_id, ticket_type_id, seat_no)` chặn phát trùng.

Soát vé là bài toán oversell thu nhỏ: hai cổng quét cùng một mã QR (ví dụ người mua chụp màn hình gửi cho bạn) cùng
lúc. Cách sai là đọc vé, thấy `VALID`, rồi `save(USED)`: cả hai cổng đều đọc thấy `VALID`. Dự án dùng lại đúng kỹ thuật
ở 3.3: `UPDATE ticket SET status = 'USED' ... WHERE code = ? AND status = 'VALID'`. Lệnh nào khớp 1 dòng thì cho vào,
lệnh kia khớp 0 dòng và trả `ALREADY_USED`. `manyGatesScanningTheSameTicketAdmitItOnce` cho 16 cổng quét cùng lúc.

### 3.7. Tự thử để hiểu

1. Chạy `ReservationServiceIT` (xem cách chạy ở [Phần 14](#phần-14-kiểm-thử)): 200 luồng tranh 50 ghế cho ra đúng 50 hold.
2. **Phá thử:** xoá ` and s.availableQuantity >= :quantity` khỏi `tryTake` rồi chạy lại. 3 test sẽ báo đỏ, và log có `ck_ticket_stock__available_in_range`: DB đã chặn hộ bạn.
3. **Phá thử tiếp:** xoá dòng `stocks.sort(...)`. Chạy test nhiều đơn, có thể gặp `deadlock detected`.
4. Câu hỏi tự kiểm tra: vì sao test đặt `spring.datasource.hikari.maximum-pool-size=20`? (Gợi ý: profile test mặc định chỉ có pool **1** kết nối. Với 1 kết nối, các request phải xếp hàng tuần tự, nên test sẽ đạt *dù code có sai*.)

---

## Phần 4. Giữ ghế có thời hạn và job dọn dẹp

> Code: `ReservationService.expireDueHolds`, `HoldExpiryJob`, `StockHoldRepository.lockExpiredHolds`.

**Vấn đề:** người mua giữ ghế rồi bỏ đi. Nếu không có cơ chế trả lại, ghế bị "chết" mãi.

**Cách làm:** mỗi `StockHold` có `expiresAt = now + 10 phút`. `HoldExpiryJob` chạy mỗi vài giây, lấy các hold
`HELD` đã quá hạn, trả ghế về kho, đánh dấu `EXPIRED` và phát sự kiện `inventory.hold-expired.v1`.

**Điểm hay: `FOR UPDATE SKIP LOCKED`.**

```sql
select * from stock_hold where status = 'HELD' and expires_at < :now
order by expires_at limit :limit for update skip locked
```

Khi chạy 3 instance inventory, cả 3 cùng chạy job. `FOR UPDATE` khoá các dòng mà instance này lấy được; `SKIP LOCKED`
bảo các instance khác **bỏ qua** những dòng đang bị khoá thay vì đứng chờ. Kết quả: 3 instance chia nhau xử lý, không
có hold nào bị trả hai lần, và không cần "leader election".

**Xử lý theo lô (batch):** mỗi lần chỉ lấy tối đa `expiryBatchSize` dòng trong một transaction ngắn, rồi lặp cho tới
khi lô trả về ít hơn kích thước lô. Transaction ngắn giúp không khoá DB lâu.

**Hai lớp kiểm tra hết hạn:** job có thể chạy trễ vài giây. Vì vậy `confirm()` tự kiểm tra
`hold.getExpiresAt().isAfter(now)`, không phụ thuộc việc job đã chạy hay chưa (test `holdPastItsDeadlineCannotBeConfirmedEvenBeforeTheJobRuns`).

---

## Phần 5. Idempotency: gọi lại không gây hậu quả

**Định nghĩa:** một thao tác là idempotent nếu gọi 1 lần hay 10 lần đều cho cùng kết quả. Đây là nền móng của mọi
thứ khác: muốn retry khi mạng lỗi, muốn xử lý lại sự kiện Kafka bị giao trùng, thì thao tác phải idempotent.

### 5.1. Checkout với `Idempotency-Key`

> Code: `order/.../service/checkout/CheckoutService.java`, `web/rest/CheckoutResource.java`. Test: `CheckoutServiceIT`.

Người mua bấm "Mua", mạng chập chờn, trình duyệt gửi lại. Nếu không cẩn thận sẽ thành 2 đơn. Client sinh một UUID
cho mỗi lần thử mua và gửi trong header `Idempotency-Key`; khi gửi lại thì dùng đúng key đó.

- Cột `idempotency_key` có ràng buộc **unique**.
- Lần đầu: tạo đơn, trả **201**.
- Lần sau với cùng key: tìm thấy đơn cũ, trả **200** cùng đơn đó, không giữ ghế thêm.
- Key bị dùng bởi user khác: trả 409 (key không được phép "mượn" đơn của người khác).

**Trường hợp khó: hai request cùng key đến cùng lúc.** Cả hai đều kiểm tra "chưa có đơn" rồi cùng `INSERT`. Unique
constraint chỉ cho một bên thắng; bên thua nhận `DataIntegrityViolationException`, bắt lỗi đó rồi đọc lại đơn của
bên thắng để trả về. Test `concurrentRetriesWithSameKeyCreateOneOrder` bắn 8 luồng cùng key, và chỉ có 1 đơn, 1 lần giữ ghế.

> **Quy tắc vàng:** "kiểm tra rồi mới insert" (`if !exists then insert`) **không** an toàn khi chạy đồng thời. Chỉ có
> ràng buộc unique ở DB mới đảm bảo được. Code Java chỉ xử lý gọn gàng trường hợp thua.

### 5.2. Các API nội bộ keyed by `orderId`

- Inventory: unique `(order_id, stock_id)` trong `stock_hold`. Gọi `reserve` lại cho cùng đơn trả về hold cũ; nếu cùng đơn mà đòi số ghế khác thì trả 409 (`ReservationConflictException`).
- Payment: mỗi đơn có tối đa một payment `PENDING`, đảm bảo bằng **partial unique index** `CREATE UNIQUE INDEX ... ON payment (order_id) WHERE status = 'PENDING'`.
- `confirm`, `release`, `cancel`, `refund`: nếu đã làm rồi thì không làm gì nữa.

### 5.3. Consumer chống trùng: bảng `processed_event`

> Code: `inventory/.../repository/ProcessedEvents.java` (order cũng có bản tương tự).

```java
jdbcTemplate.update(
    "insert into processed_event (consumer, event_id, processed_at) values (?, ?, ?) on conflict do nothing", ...) == 1
```

Trả về `true` ở lần giao đầu tiên, `false` ở các lần giao lại. Điều quan trọng là câu INSERT này chạy **trong cùng
transaction** với việc áp dụng sự kiện: hoặc cả hai cùng commit, hoặc cả hai cùng rollback.

Vì sao cần cả khi dữ liệu là "tổng tuyệt đối"? Xem test `staleRedeliveryIsIgnored`: sự kiện "tổng 120" đến, sau đó
"tổng 80", rồi bản "120" cũ được **giao lại**. Không có bảng chống trùng, tổng sẽ bị lùi về 120, một giá trị đã lỗi thời.

---

## Phần 6. Ranh giới transaction

Đây là phần dễ sai nhất khi viết microservice.

### 6.1. Quy tắc: không gọi HTTP khi đang mở transaction DB

```java
@Transactional                          // ❌ SAI
public void checkout(...) {
    orderRepository.save(order);
    catalogClient.getTicketType(id);    // service khác chậm 5 giây → giữ 1 kết nối DB suốt 5 giây
}
```

Khi mở bán, 100 request cùng chờ catalog sẽ chiếm hết pool kết nối DB, và toàn bộ order đứng hình dù DB rảnh. Dự án
dùng `TransactionTemplate` để mở transaction **chỉ quanh phần ghi DB**:

```java
List<TicketType> types = ...catalogClient...;              // ngoài transaction
Order order = transactionTemplate.execute(s -> createPendingOrder(...));   // transaction ngắn
inventoryClient.reserve(...);                              // ngoài transaction
update(order.getId(), o -> o.status(RESERVED));            // transaction ngắn khác
```

Trong quá trình phát triển, bản đầu của `CheckoutService` đã từng gọi catalog **bên trong** transaction, và sau đó
được sửa lại. Comment đầu class ghi rõ quy tắc này.

### 6.2. Các kiểu propagation đáng nhớ

| Propagation | Nghĩa | Dùng ở đâu, vì sao |
|---|---|---|
| `REQUIRED` (mặc định) | Có transaction thì tham gia, chưa có thì mở mới | Hầu hết service |
| `MANDATORY` | **Bắt buộc** đang có transaction, không thì ném lỗi | `Outbox.append`, `ProcessedEvents.firstDelivery`: ghi outbox mà không đi kèm thay đổi nghiệp vụ là vô nghĩa. Lỗi lộ ngay khi viết sai (test `appendingOutsideATransactionIsRefused`) |
| `REQUIRES_NEW` | Luôn mở transaction riêng, tạm treo transaction ngoài | `WebhookInbox.record`: lưu webhook phải **sống sót** kể cả khi phần xử lý sau đó lỗi |
| `NEVER` | Cấm chạy trong transaction | `WebhookInbox.record`: bảo đảm không ai vô tình gọi nó từ trong một transaction khác |

### 6.3. Bẫy: `@Transactional` gọi nội bộ không có tác dụng

`this.otherMethod()` không đi qua proxy của Spring, nên `@Transactional` trên `otherMethod` bị bỏ qua. Đó là một lý
do dự án dùng `TransactionTemplate` ở những chỗ cần nhiều transaction nhỏ trong cùng một class (`CheckoutService`, `OrderSaga`, `PaymentFlowService`).

---

## Phần 7. Kafka và Spring Cloud Stream

### 7.1. Khái niệm tối thiểu

| Khái niệm | Hiểu đơn giản |
|---|---|
| Topic | Một "kênh" sự kiện, ví dụ `payment.result.v1`. Hậu tố `.v1` để sau này đổi định dạng mà không làm vỡ consumer cũ |
| Partition | Topic được chia thành nhiều phần. **Thứ tự chỉ được đảm bảo trong một partition** |
| Key | Sự kiện cùng key luôn vào cùng partition. Dự án dùng id của aggregate (`orderId`, `ticketTypeId`) làm key, nên các sự kiện của cùng một đơn luôn theo đúng thứ tự |
| Consumer group | Các instance cùng group chia nhau partition; mỗi sự kiện được *một* instance trong group xử lý |
| At-least-once | Kafka đảm bảo giao **ít nhất một lần**, có thể nhiều hơn. Vì vậy consumer phải idempotent ([Phần 5.3](#53-consumer-chống-trùng-bảng-processed_event)) |

### 7.2. Spring Cloud Stream trong dự án

- **Gửi:** `StreamBridge.send(topic, message)`, gọi từ `OutboxRelay`.
- **Nhận:** khai báo một bean `Consumer<Message<T>>`, ví dụ `@Component("ticketTypeEvents") class TicketTypeEventsConsumer`. Tên bean khớp với binding `ticketTypeEvents-in-0` trong `application.yml`, nơi khai báo topic và `group`.
- Nhận `Message<T>` (thay vì chỉ `T`) để đọc được header `eventId`.

### 7.3. Test Kafka không cần Kafka thật

`@ImportAutoConfiguration(TestChannelBinderConfiguration.class)` thay Kafka bằng binder trong bộ nhớ:
`InputDestination.send(...)` giả lập sự kiện đến, `OutputDestination.receive(...)` bắt sự kiện gửi đi. Hai điểm cần nhớ:

- Classpath có cả binder Kafka, nên phải đặt `spring.cloud.stream.default-binder=integration`, nếu không message sẽ âm thầm đi vào binder Kafka.
- `src/test/resources/config/application.yml` **thay thế hoàn toàn** file chính khi chạy test. Binding nào cần cho test thì phải khai báo lại ở file test.

---

## Phần 8. Transactional Outbox

> Code: `*/service/outbox/Outbox.java`, `OutboxRelay.java`, `domain/OutboxEvent.java`. Quyết định: [ADR 0004](adr/0004-transactional-outbox.md). Test: `TicketTypeOutboxIT`.

### 8.1. Vấn đề "ghi hai nơi" (dual write)

Payment cần làm hai việc: cập nhật DB (`status = SUCCEEDED`) **và** gửi sự kiện lên Kafka. Không có transaction nào
bao được cả DB lẫn Kafka.

| Thứ tự | Nếu crash ở giữa |
|---|---|
| Commit DB rồi gửi Kafka | DB ghi "đã trả tiền" nhưng sự kiện mất, đơn hàng kẹt ở "chờ thanh toán" mãi |
| Gửi Kafka rồi commit DB | Sự kiện "đã trả tiền" đã đi, nhưng DB rollback: báo nhầm cho order |

Phiên bản đầu của dự án dùng `@TransactionalEventListener(AFTER_COMMIT)`: tốt hơn (không bao giờ gửi cho thay đổi bị
rollback) nhưng vẫn **mất sự kiện** nếu crash giữa lúc commit và lúc gửi.

### 8.2. Cách giải: ghi sự kiện vào chính DB

1. Trong **cùng transaction** với thay đổi nghiệp vụ, insert một dòng vào bảng `outbox_event`. Commit là có cả hai, rollback là mất cả hai.
2. Một **relay** chạy nền đọc các dòng chưa gửi, gửi lên Kafka, rồi đánh dấu đã gửi.

```java
// OutboxRelay.relayBatch (rút gọn)
List<OutboxEvent> batch = repo.lockUnpublished(batchSize);    // FOR UPDATE SKIP LOCKED
for (OutboxEvent e : batch) {
    try { streamBridge.send(e.getTopic(), toMessage(e)); }    // producer sync: chờ Kafka ack
    catch (RuntimeException ex) { e.markFailed(ex.getMessage()); break; }   // dừng lô: không để sự kiện sau vượt sự kiện trước
    e.markPublished(clock.instant());
}
```

### 8.3. Những chi tiết làm nên sự đúng đắn

| Chi tiết | Vì sao |
|---|---|
| `spring.cloud.stream.kafka.default.producer.sync: true` | Mặc định producer gửi bất đồng bộ: `send()` trả về trước khi Kafka nhận. Bật sync để chỉ đánh dấu "đã gửi" khi Kafka đã **ack** |
| `break` khi gửi lỗi | Giữ đúng thứ tự. Sự kiện lỗi sẽ được gửi lại ở lượt sau, trước các sự kiện mới hơn |
| `SKIP LOCKED` | Nhiều instance relay cùng lúc mà không gửi trùng |
| Header `eventId` (UUID của dòng outbox) | Nếu crash sau khi gửi nhưng trước khi commit "đã gửi", lô sẽ bị gửi lại. Consumer dùng `eventId` để bỏ qua bản trùng |
| Key = `aggregateId` | Sự kiện của cùng một đơn đi vào cùng partition, nên giữ đúng thứ tự |
| Header `traceparent` | Trace của request gốc được nối tiếp qua relay và Kafka ([Phần 15](#phần-15-quan-sát-hệ-thống-observability)) |
| Dọn rác hằng ngày | `deletePublished` xoá các dòng đã gửi quá 7 ngày |

**Kết luận cần nhớ:** outbox cho **at-least-once**, consumer chống trùng biến nó thành **effectively-once**.

---

## Phần 9. Saga orchestration và bù trừ

> Code: [`order/.../service/saga/OrderSaga.java`](../order/src/main/java/com/ticketrush/order/service/saga/OrderSaga.java). Quyết định: [ADR 0002](adr/0002-saga-orchestration.md). Test: `OrderSagaIT`.

### 9.1. Vấn đề

Mua vé là một chuỗi bước trên 3 service: giữ ghế (inventory) → thu tiền (payment) → xác nhận (inventory + order).
Không có transaction phân tán. Nếu bước 2 thất bại, bước 1 phải được **hoàn tác bằng một hành động ngược lại**
(trả ghế). Hành động ngược đó gọi là **compensation** (bù trừ).

### 9.2. Orchestration hay choreography

| | Choreography | Orchestration (dự án chọn) |
|---|---|---|
| Cách làm | Mỗi service nghe sự kiện rồi tự quyết bước tiếp theo | Một bộ điều phối (`OrderSaga`) ra lệnh và theo dõi trạng thái |
| Ưu | Không có điểm trung tâm | Toàn bộ quy trình nằm ở **một chỗ đọc được**, dễ test từng nhánh |
| Nhược | Logic rải rác, khó biết đơn đang ở bước nào | Order phụ thuộc vào API của các service khác |

### 9.3. Máy trạng thái

```
RESERVED --mở thanh toán--> AWAITING_PAYMENT --trả tiền, xác nhận ghế--> CONFIRMED (phát hành vé)
                                 |  thanh toán lỗi / quá hạn: trả ghế ------> FAILED / EXPIRED
                                 |  ghế hết hạn: huỷ thanh toán ------------> EXPIRED
                                 |  đã trả tiền nhưng ghế đã mất: hoàn tiền -> REFUNDED
                                 |  người mua huỷ: huỷ thanh toán, trả ghế -> CANCELLED
```

### 9.4. Các nguyên tắc thiết kế (đọc kèm code)

1. **Trạng thái nằm ngay trong dòng `ticket_order`**, có `@Version`. Order restart thì saga đọc trạng thái và chạy tiếp; không cần bảng "saga log" riêng phải giữ đồng bộ.
2. **Chuyển trạng thái có điều kiện:** `transition(orderId, from, to, reason)` chỉ chuyển khi trạng thái hiện tại thuộc tập `from`. Nhờ vậy sự kiện đến muộn hoặc giao lại không đưa đơn quay lùi.
3. **Bước gọi service khác chạy ngoài transaction và idempotent ở phía bên kia.** Có thể lặp lại bước đó an toàn khi sự kiện bị giao lại.
4. **Sau đó, ba thứ commit cùng nhau:** thay đổi trạng thái, bản ghi `processed_event`, và sự kiện `order.events.v1` trong outbox.

### 9.5. Nhánh khó nhất: tiền về sau khi ghế đã hết hạn

Người mua mở trang thanh toán, để đó 12 phút rồi mới trả tiền. Ghế đã được trả cho người khác từ phút thứ 10.
Payment vẫn ghi nhận tiền (không được làm mất tiền của khách) và phát sự kiện kèm `late = true`. Saga thử xác nhận
ghế; inventory trả **409** vì hold không còn hiệu lực; saga gọi **hoàn tiền**, và đơn chuyển sang `REFUNDED`. Đây là
ví dụ điển hình của bù trừ: không thể "huỷ" việc đã trả tiền, chỉ có thể làm một việc ngược lại.

---

## Phần 10. Thanh toán và webhook

> Code: `payment/.../service/flow/PaymentFlowService.java`, `WebhookInbox.java`, `service/provider/MockPaymentProvider.java`. Test: `PaymentFlowServiceIT`, `PaymentApiSecurityIT`.

### 10.1. Provider abstraction

Interface `PaymentProviderGateway` gồm `createPaymentUrl`, `parseWebhook`, `refund`. `MockPaymentProvider` mô phỏng
một cổng thật: có trang thanh toán và gửi callback có chữ ký. Thêm VNPay hay MoMo chỉ cần thêm một implementation;
logic trạng thái và idempotency nằm ở `PaymentFlowService`, dùng chung cho mọi provider.

### 10.2. Chữ ký HMAC: tin webhook thế nào

Endpoint webhook phải **mở công khai**, vì cổng thanh toán không có token Keycloak. Vậy ai cũng gọi được? Đúng,
nhưng mỗi callback kèm `signature = HMAC-SHA256(secret, "ref|result|amount|transactionId")`. Chỉ ai biết `secret`
mới tạo được chữ ký đúng.

```java
boolean valid = MessageDigest.isEqual(expected.getBytes(UTF_8), signature.getBytes(UTF_8));
```

`MessageDigest.isEqual` so sánh với **thời gian không đổi**. `String.equals` dừng ngay ở ký tự sai đầu tiên, nên kẻ
tấn công có thể đo thời gian phản hồi để đoán dần từng ký tự (timing attack).

### 10.3. Áp dụng webhook đúng một lần: ba lớp

Cổng thanh toán thường gửi lại cùng một callback nhiều lần, có khi đồng thời.

1. **Inbox:** `WebhookInbox.record` lưu callback trong transaction **riêng** (`REQUIRES_NEW`), unique theo `(provider, event_key)`. Callback được lưu lại kể cả khi xử lý sau đó lỗi (dùng cho kiểm toán và xử lý lại). Nếu đã có `processedAt` thì đây là bản giao lại và được bỏ qua.
2. **Khoá dòng payment:** `lockByProviderRef` dùng `PESSIMISTIC_WRITE` (`SELECT ... FOR UPDATE`). Mười bản callback đến cùng lúc phải xếp hàng.
3. **Chỉ chuyển khỏi `PENDING`:** bản thứ hai khi vào được thì thấy payment đã `SUCCEEDED`, nên không làm gì và không phát sự kiện thứ hai.

Test `concurrentDuplicateCallbacksPublishOneResult`: 10 luồng gửi cùng một callback cho ra đúng 1 sự kiện và 1 dòng inbox.

Lưu một lần không có nghĩa là quên các lần gửi lại: mỗi lần cổng gửi trùng, cột `deliveries` của dòng inbox tăng 1
(`PaymentWebhookEventRepository.countRedelivery`, một câu UPDATE nên không mất lượt khi đồng thời). Trang quản trị
thanh toán hiển thị số này, nên đối soát thấy được "cổng gửi 3 lần, hệ thống áp dụng 1 lần"; test trên khẳng định
`deliveries == 10`.

### 10.4. Không tin dữ liệu callback

- Chữ ký sai: vẫn lưu lại (`signatureValid = false`) để điều tra, trả 400, **không** áp dụng.
- Số tiền khác với payment: chuyển `FAILED` với lý do `AMOUNT_MISMATCH`.
- Đơn đã hoàn tiền: không cho tạo thanh toán mới (lỗi này được test phát hiện trong lúc phát triển và đã sửa).

---

## Phần 11. Resilience4j: chịu lỗi khi gọi service khác

> Code: `order/.../client/ResilientClient.java`, `config/ServiceClientsConfiguration.java`, cấu hình `resilience4j:` trong `order/src/main/resources/config/application.yml`. Test: `ResilientClientTest` (dùng WireMock).

| Cơ chế | Giải quyết | Cấu hình trong dự án |
|---|---|---|
| **Timeout** | Service bên kia treo thì không chờ mãi | connect 2s, read 5s |
| **Retry** | Lỗi thoáng qua (mạng, instance đang restart) | tối đa 3 lần, cách nhau 200ms, **chỉ cho lời gọi idempotent** |
| **Circuit breaker** | Service bên kia chết hẳn thì ngừng gọi một lúc, trả lỗi ngay thay vì để mọi request chờ timeout | cửa sổ 20 lần gọi, lỗi ≥ 50% thì mở mạch 10s |
| **Bulkhead** | Một service chậm không được chiếm hết thread của order | giới hạn số lời gọi đồng thời |

**Thứ tự bọc rất quan trọng:** Retry (ngoài) → CircuitBreaker → Bulkhead (trong). Retry ở ngoài nên mỗi lần thử lại
đều hỏi circuit breaker: mạch đã mở thì dừng retry ngay, không tiếp tục dội vào một service đang chết.

**Vì sao retry an toàn?** Vì `reserve` và `createPayment` đều keyed by `orderId` ([Phần 5.2](#52-các-api-nội-bộ-keyed-by-orderid)). Retry một thao tác **không** idempotent, ví dụ "trừ tiền" mà không có key, sẽ trừ tiền hai lần.

**Chi tiết hạ tầng đáng học:** comment trong `ServiceClientsConfiguration` giải thích vì sao dùng
`SimpleClientHttpRequestFactory`. Read timeout của nó áp dụng cho *từng lần đọc socket*, còn của
`JdkClientHttpRequestFactory` là một hạn chót tính từ lúc bắt đầu request. Trên máy bận, hạn chót đó cắt ngang cả
những response 200 đang đọc dở.

---

## Phần 12. Gateway: rate limit, phòng chờ ảo, lập trình reactive

### 12.1. Rate limit bằng token bucket trên Redis

> Code: `gateway/.../config/RateLimitConfiguration.java`, route `order-checkout` trong `gateway/src/main/resources/config/application.yml`. Test: `CheckoutRateLimitIT`.

Hình dung mỗi user có một cái xô chứa tối đa 5 token, mỗi giây được thêm 1 token, và mỗi lần checkout tốn 1 token.
Xô cạn thì trả **429** kèm các header `X-RateLimit-*`. Xô nằm trong Redis nên dùng chung được cho mọi instance gateway.

- **Theo user, không theo IP:** một tài khoản không thể spam từ nhiều IP, và nhiều người dùng chung một mạng (NAT) không bị chung xô.
- **Fail-open:** Redis sập thì cho request đi qua, và Redis không được tính vào health của gateway. Thà tạm không giới hạn còn hơn để cả trang sập theo Redis.

### 12.2. Bài học reactive: không được chặn event-loop

Gateway chạy trên **WebFlux/Netty**: một nhóm nhỏ thread (event-loop) phục vụ *toàn bộ* request. Nếu một thread bị
chặn chờ I/O (blocking), mọi request khác xếp hàng sau nó. Test của gateway dùng **BlockHound** để phát hiện lời gọi
blocking. Nhờ đó dự án phát hiện 3 lỗi thật:

| Lỗi | Cách sửa |
|---|---|
| Lettuce kết nối Redis theo kiểu blocking ở request đầu tiên | Mở kết nối lúc ứng dụng sẵn sàng (`ApplicationReadyEvent`) |
| Script Lua của rate limiter được đọc từ file jar ở lần gọi đầu, và đọc lại khi Redis báo `NOSCRIPT` | Nạp sẵn script bằng `SCRIPT LOAD` lúc khởi động |
| LoadBalancer tạo context con cho mỗi service ở request đầu tiên | `spring.cloud.loadbalancer.eager-load.clients` |

### 12.3. Phòng chờ ảo (virtual waiting room)

> Code: `gateway/.../service/waitingroom/WaitingRoomService.java`, `gateway/.../web/rest/WaitingRoomResource.java`,
> `order/.../service/checkout/QueuePasses.java`. Test: `WaitingRoomIT` (gateway), `CheckoutServiceIT` (order).

Ý tưởng: thay vì để 10.000 người cùng đập vào checkout, cho họ **xếp hàng** và chỉ cho vào với tốc độ hệ thống chịu được.
Organiser bật phòng chờ cho từng sự kiện (cờ `waitingRoom` của `Event` trong catalog).

- Mỗi sự kiện có một hàng chờ trong Redis: người vào nhận số thứ tự (`INCR`, lưu theo user nên vào lại vẫn giữ chỗ), còn con trỏ "đã phục vụ" tăng dần với tốc độ `admitPerSecond`.
- **Cách làm sai hay gặp:** một job chạy mỗi giây cộng thêm N vào con trỏ. Chạy 3 instance gateway thì tốc độ thành 3N; hàng vắng một lúc thì con trỏ vượt xa số người, lần sau 10.000 người ùa vào cùng lúc.
- **Dự án làm:** con trỏ được tính lại theo **thời gian trôi qua kể từ lần cập nhật trước**, trong một script Lua (chạy nguyên tử trong Redis), và không bao giờ vượt quá số người đã vào hàng. Bao nhiêu instance hay bao nhiêu người poll thì tốc độ vẫn đúng N/giây, và hàng vắng không "để dành" lượt.
- Đến lượt thì nhận một **pass có chữ ký**: `<eventId>.<base64url(user)>.<hết hạn>.<HMAC>`, hạn ~10 phút.
- **Ai kiểm tra pass?** Không phải gateway: gateway chỉ thấy danh sách `ticketTypeId` trong body, không biết chúng thuộc sự kiện nào. Order thì vốn đã hỏi catalog về từng loại vé, nên biết sự kiện và cờ `waitingRoom`; order kiểm pass bằng khoá dùng chung với gateway (`QUEUE_PASS_SECRET`). Gọi thẳng API để "vượt hàng" không có pass hợp lệ, và order trả **428**.

**Tự thử:** trong `WaitingRoomIT`, đồng hồ do test điều khiển. Đổi `admit-per-second` trong test config rồi xem số người được vào sau 1 giây thay đổi ra sao; xoá dòng `math.min(served + add, last)` trong script và chạy `anIdleQueueDoesNotSaveUpAdmissions` để thấy lỗi "để dành lượt".

---

## Phần 13. Bảo mật

| Kỹ thuật | Ở đâu | Bài học |
|---|---|---|
| CRUD sinh sẵn chỉ cho admin | `catalog/.../config/SecurityConfiguration` | JHipster mặc định chỉ đòi `authenticated()`. Khi ai cũng tự đăng ký được, "đã đăng nhập" nghĩa là "bất kỳ ai": người mua tự tạo sự kiện, sửa giá. Khoá theo vai trò, và mở hẹp đúng thứ service khác cần (`GET /api/ticket-types/*` cho `ROLE_SERVICE`) |
| Sự kiện nháp không bán được | `CheckoutService.validate` | Ẩn sự kiện khỏi danh sách là chưa đủ: ai đoán được `ticketTypeId` vẫn gọi checkout được. Order kiểm tra `event.status == PUBLISHED` từ catalog |
| Phân quyền theo đường dẫn | `SecurityConfiguration` của mỗi service | Quy tắc cụ thể đặt **trước** quy tắc chung (`/api/internal/**` trước `/api/**`) |
| API admin chỉ đọc | `@readOnly` trong JDL, `scripts/make-readonly*.py` | Nếu admin được tạo hay sửa đơn trực tiếp, việc đó sẽ bỏ qua giữ ghế và thanh toán. CRUD sinh sẵn không phải lúc nào cũng an toàn về nghiệp vụ |
| CSRF ở gateway | `gateway/.../SecurityConfiguration` | Bảo vệ người dùng dùng cookie phiên. Request có bearer token được miễn; webhook công khai cũng được miễn vì đã có chữ ký |
| Secret ngoài code | `docker-compose/.env` (git ignore), `${VAR:?}` | `:?` khiến compose **dừng ngay** nếu thiếu biến, thay vì chạy với giá trị rỗng |
| Mã vé QR có HMAC | `order/.../service/ticket/TicketCodes.java` | Máy soát vé không cần tin người cầm vé: sửa một ký tự là chữ ký sai. Đoán được mã hợp lệ tương đương đoán 128 bit |
| Không tin dữ liệu client | `CheckoutService` lấy giá từ catalog | Mọi thứ đến từ trình duyệt đều có thể bị sửa |

---

## Phần 14. Kiểm thử

### 14.1. Testcontainers: test với hạ tầng thật

Integration test (`*IT.java`) chạy trên **PostgreSQL, Redis, Kafka thật** trong Docker. Lý do: những thứ như
`FOR UPDATE SKIP LOCKED`, partial index, `ON CONFLICT DO NOTHING` hay ràng buộc CHECK không thể kiểm chứng bằng H2 hay mock.

```bash
# Chạy một test (nhanh, dùng khi đang phát triển)
./mvnw -ntp verify -DskipJib -Dtest=none -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=ReservationServiceIT
# Chạy toàn bộ một service (trước khi commit)
./mvnw -ntp verify -DskipJib
```

Testcontainers đang bật chế độ **dùng lại container** (`testcontainers.reuse.enable=true` trong `~/.testcontainers.properties`).
Vì DB được giữ lại giữa các lần chạy, **không được sửa một changeset Liquibase đã áp dụng**; muốn đổi thì thêm changeset mới.

### 14.2. Kỹ thuật viết test đồng thời

```java
CountDownLatch start = new CountDownLatch(1);
for (...) executor.submit(() -> { start.await(); return reserve(...); });   // mọi luồng đứng chờ ở vạch xuất phát
start.countDown();                                                           // cùng chạy một lúc
```

Ba điều kiện để test đồng thời có ý nghĩa:

1. Test **không** đặt `@Transactional`, để mỗi luồng commit thật.
2. Pool kết nối đủ lớn, để các transaction thật sự tranh nhau ([Phần 3.7](#37-tự-thử-để-hiểu)).
3. **Mutation testing:** phá code cố ý và kiểm tra test có báo đỏ không. Một test chưa từng thấy đỏ thì chưa chứng minh được gì.

### 14.3. Điều khiển thời gian: `MutableClock`

Muốn test "hết 10 phút giữ ghế" mà không phải `sleep(600_000)`: inject `Clock`, và trong test thay bằng
`MutableClock` rồi gọi `clock.advance(Duration.ofMinutes(11))`. Xem `ReservationServiceIT`, `PaymentFlowServiceIT`.

### 14.4. Các loại test trong dự án

| Loại | Công cụ | Ví dụ |
|---|---|---|
| Unit test | JUnit, Mockito | Các test `*Test` của domain và mapper |
| Integration test | Testcontainers, `@SpringBootTest` | `ReservationServiceIT`, `OrderSagaIT` |
| Kiểm tra kiến trúc | ArchUnit | `TechnicalStructureTest` |
| Client với service lỗi | WireMock | `ResilientClientTest` |
| End-to-end | Python + stack Docker thật | `scripts/e2e/` |
| Tải (load test) | k6 | `load/k6/on-sale.js`: tiêu chí là số vé bán ra đúng bằng số ghế, không bán quá, p95 dưới ngưỡng |

**Đọc kết quả k6 cho đúng:** mã 409 (hết vé) là câu trả lời **đúng** khi mở bán, không phải lỗi. Script đếm riêng
`sold_out_answers` và `tickets_sold`. Độ trễ đo trên laptop đang tải nặng sẽ cao hơn nhiều so với môi trường thật.

---

## Phần 15. Quan sát hệ thống (observability)

> Thư mục: `observability/` (Prometheus, Grafana, Tempo).

| Trụ cột | Công cụ | Trả lời câu hỏi |
|---|---|---|
| Metrics | Micrometer → Prometheus → Grafana | Bán được bao nhiêu vé mỗi phút? Bao nhiêu lượt giữ chỗ đang hoạt động? p95 checkout là bao nhiêu? |
| Tracing | OpenTelemetry → Tempo | Một request checkout đi qua những service nào, chậm ở bước nào? |
| Logs | Logback | Chuyện gì đã xảy ra với đơn `TR...`? |

**Điểm hay: trace đi xuyên qua outbox và Kafka.** Thông thường trace bị đứt ở chỗ "ghi DB rồi job khác mới gửi
Kafka". Dự án lưu `traceparent` (chuẩn W3C) của request gốc vào cột `outbox_event.trace_parent`. Khi relay gửi
sự kiện, nó khôi phục trace đó, nên trên Tempo có thể thấy trọn vẹn: checkout → order → inventory → payment → webhook → Kafka → saga.

---

## Phần 16. Bài học hạ tầng: những lỗi thật đã gặp

Những lỗi dưới đây đều xảy ra thật trong quá trình làm dự án. Chúng là loại kiến thức không sách nào dạy, nhưng hay
được hỏi khi phỏng vấn.

| Triệu chứng | Nguyên nhân gốc | Cách sửa |
|---|---|---|
| Healthcheck của Keycloak luôn lỗi; image không chạy | File `.sh` bị **CRLF** do Git trên Windows | `.gitattributes`: `*.sh text eol=lf` |
| Mọi integration test lỗi trên Windows | JVM dùng múi giờ `Asia/Saigon`, PostgreSQL 18 từ chối tên cũ này | `-Duser.timezone=UTC` trong `argLine` |
| Inventory không nhận được sự kiện nào, cũng không có log lỗi | Image `kafka-native` bỏ cấu hình single-node khi có biến `KAFKA_*`, nên `__consumer_offsets` cần replication 3 nhưng chỉ có 1 broker | Đặt `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1` |
| Order gọi inventory bị `Connection refused` sau khi restart | Consul vẫn giữ bản đăng ký cũ (critical) và load balancer chọn trúng nó | `query-passing: true` (chỉ lấy instance khoẻ) |
| Sau đó gateway **mất route** vĩnh viễn khi một service restart | Với `query-passing`, service đang khởi động bị loại khỏi danh sách; Consul không báo khi health đổi | Tắt `query-passing` riêng cho gateway |
| Service bị đánh dấu unhealthy dù vẫn khởi động bình thường | Máy chậm, khởi động lâu hơn thời gian healthcheck cho phép | `start_period` cho healthcheck |
| JDL sinh Angular cho microservice | Lỗi của JHipster 9.4: importer bỏ qua `skipClient` khi tạo app mới | Sinh lại app đó riêng từ `.yo-rc.json` |
| Sinh lại code xoá mất code viết tay | `jhipster jdl --force` ghi đè | Sinh trong **git worktree tạm** rồi chỉ chép phần cần |
| Khoá CRUD catalog cho admin xong thì checkout thật hỏng, dù mọi test đều xanh | Order lấy giá qua `GET /api/ticket-types/{id}` bằng token service (`ROLE_SERVICE`); trong test, `CatalogClient` bị mock nên không ai gọi thật | Mở riêng `GET /api/ticket-types/*` cho `ROLE_SERVICE` kèm test; script e2e cũng phải đổi sang token admin. Đổi quy tắc bảo mật thì tìm mọi bên gọi API đó, kể cả service khác |
| Test gateway đỏ ở `TechnicalStructureTest` sau khi thêm tính năng | Package mới `waitingroom` không thuộc layer nào ArchUnit cho phép | Đặt code vào layer có sẵn: `service/waitingroom`, `web/rest` |
| `bad class file ... NoSuchFileException` khi build, rồi `mvn clean` cũng không xoá được `target/` | Java language server của IDE (VS Code/Antigravity) đang biên dịch vào cùng `target/` | Xoá `target/` rồi build lại; tránh build bằng Maven khi IDE đang import project |
| POST ẩn danh vào API của gateway trả 403 thay vì 401 | Filter CSRF chạy trước bước xác thực: request không có token CSRF bị chặn trước khi hỏi "bạn là ai" | Đúng thiết kế; test 401 phải gửi kèm token CSRF |

Bài học chung: **luôn chạy hệ thống thật end-to-end**. Unit test và Testcontainers đều đạt, nhưng 4 lỗi đầu trong
bảng chỉ lộ ra khi chạy cả stack bằng docker-compose.

---

## Phần 17. Lộ trình tự học và bài tập

### 17.1. Thứ tự học đề xuất

| Tuần | Chủ đề | Đọc | Làm |
|---|---|---|---|
| 1 | Khung JHipster, phân lớp, JPA | Phần 0–2, một bộ CRUD của `Venue` | Thêm một trường vào `Venue` qua JDL trong worktree tạm, xem những file nào thay đổi |
| 2 | Concurrency, chống oversell | Phần 3–4, ADR 0003 | Bài tập 1, 2 |
| 3 | Idempotency, transaction | Phần 5–6 | Bài tập 3 |
| 4 | Kafka, outbox | Phần 7–8, ADR 0004 | Bài tập 4 |
| 5 | Saga, thanh toán | Phần 9–10, ADR 0002 | Bài tập 5, 6 |
| 6 | Resilience, gateway, bảo mật | Phần 11–13 | Bài tập 7 |
| 7 | Test, observability, hạ tầng | Phần 14–16 | Bài tập 8 |

### 17.2. Bài tập (tự làm trên một nhánh riêng)

1. **Phá chống oversell:** đổi `tryTake` thành kiểu đọc rồi ghi (`findById`, kiểm tra, `save`). Chạy `ReservationServiceIT` và giải thích vì sao test đỏ.
2. **Bỏ khoá theo thứ tự:** xoá `stocks.sort(...)`, chạy test nhiều đơn nhiều lần, quan sát lỗi deadlock trong log PostgreSQL.
3. **Idempotency:** viết thêm test cho `CheckoutService`: cùng `Idempotency-Key` nhưng danh sách vé khác thì hệ thống nên phản hồi thế nào? Đọc code để xem hiện tại nó xử lý ra sao, rồi cân nhắc có nên trả 409 không.
4. **Outbox:** tắt `producer.sync` trong `application-kafka.yml`. Giải thích vì sao relay giờ có thể đánh dấu "đã gửi" một sự kiện chưa tới Kafka.
5. **Saga:** vẽ lại máy trạng thái của `OrderSaga` từ code (không nhìn ADR), rồi so với ADR 0002. Tìm trong `OrderSagaIT` test nào ứng với mỗi mũi tên.
6. **Webhook:** viết test gửi một callback có `amount` bị sửa nhưng chữ ký vẫn tính trên số tiền gốc. Kết quả là gì, và vì sao?
7. **Resilience:** trong `ResilientClientTest`, đổi thứ tự bọc thành CircuitBreaker ngoài và Retry trong. Test nào thay đổi hành vi?
8. **Load test:** chạy `load/run.sh` khi đã tắt bớt ứng dụng khác trên máy, so p95 với file trong `load/results/`, và giải thích sự khác biệt.

### 17.3. Câu hỏi phỏng vấn bạn nên trả lời được bằng chính dự án này

- Làm sao chống bán quá vé khi có hàng nghìn request đồng thời? Vì sao không dùng Redis lock?
- Outbox giải quyết vấn đề gì? Nó đảm bảo exactly-once hay at-least-once? Phần còn lại do ai lo?
- Saga khác transaction phân tán (2PC) thế nào? Khi bù trừ cũng thất bại thì sao?
- Vì sao retry chỉ áp dụng cho lời gọi idempotent? Lấy ví dụ trong dự án.
- Webhook bị gửi 10 lần cùng lúc, làm sao chỉ xử lý một lần?
- Vì sao không được gọi HTTP bên trong transaction DB?
- Rate limit nên đặt theo IP hay theo user? Khi Redis sập thì nên chặn hay cho qua?
- Hai cổng soát vé quét cùng một mã QR cùng lúc, làm sao chỉ cho vào một lần?
- Phòng chờ ảo chạy trên nhiều instance gateway thì làm sao tốc độ cho vào vẫn đúng? Ai nên kiểm tra pass, và vì sao?
- Kể một lỗi khó nhất bạn đã debug trong dự án này. (Gợi ý: lỗi Kafka không có consumer group, hoặc gateway mất route khi service restart.)
