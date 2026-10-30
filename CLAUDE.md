# TicketRush: hướng dẫn cho Claude

## Cập nhật tài liệu sau mỗi phần việc

Mỗi khi xong một phần việc có thay đổi code (một tính năng, một bản sửa lỗi, một giai đoạn), và **trước khi commit
phần việc đó**, cập nhật hai file:

- `docs/plan.md`: tổng quan dự án. Cập nhật khi có service, lớp (layer), API, topic Kafka, bảng dữ liệu, quy tắc bảo
  mật hay thành phần hạ tầng mới, và khi trạng thái lộ trình thay đổi.
- `docs/learn.md`: tài liệu để người dùng **học** code. Với mỗi kỹ thuật mới, thêm hoặc sửa một mục theo đúng mạch
  sẵn có: vấn đề → cách làm sai hay gặp → dự án làm thế nào → code nằm ở đâu (đường dẫn file thật) → tự thử để hiểu.
  Lỗi thật gặp phải trong lúc làm thì thêm vào bảng "Bài học hạ tầng"; kỹ thuật mới thì cân nhắc thêm bài tập và câu
  hỏi phỏng vấn tương ứng.

Quy tắc:
- Viết bằng tiếng Việt, giữ nguyên thuật ngữ kỹ thuật tiếng Anh.
- Chỉ ghi điều đúng với code hiện tại. Đổi tên hay xoá class nào thì sửa các chỗ nhắc tới nó; kiểm tra link tương
  đối còn trỏ đúng file.
- Phần việc không đổi kiến trúc hay kỹ thuật (đổi text, sửa nhỏ) thì không cần cập nhật.
- Commit tài liệu cùng phần việc đó, hoặc ngay sau với message `docs: ...`.

## Chạy test

- Trong lúc phát triển: chỉ chạy các IT liên quan, ví dụ
  `./mvnw -ntp verify -DskipJib -Dtest=none -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=ReservationServiceIT`.
- Chạy `./mvnw -ntp verify -DskipJib` đầy đủ của một service một lần, ngay trước khi commit service đó.
- Testcontainers đang bật chế độ dùng lại container: không sửa changeset Liquibase đã áp dụng, mà thêm changeset mới.

## Sinh code JHipster

Không chạy `jhipster jdl --force` trực tiếp trong repo, vì nó ghi đè code viết tay. Sinh trong một git worktree tạm,
chép sang những phần cần, rồi chạy `python scripts/harden-compose.py`. Chi tiết trong README, mục "Regenerating code".
