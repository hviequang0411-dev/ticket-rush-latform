#!/usr/bin/env python3
"""Fills a running stack with realistic demo events, so the buyer UI can be judged with believable data.

    python scripts/seed-demo.py

Goes through the real organiser API (catalog -> outbox -> Kafka -> inventory creates the stock), as an organiser
created through Keycloak's admin API (same helper as the end-to-end test). Idempotent: events whose slug already
exists are skipped. Events left over from end-to-end runs ("E2E ...") are moved to ENDED so buyers don't see them.
Names and artists are fictional.
"""
import os
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path

os.environ.setdefault("TICKETRUSH_GATEWAY", "http://localhost:8080")
sys.path.insert(0, str(Path(__file__).resolve().parent / "e2e"))

import e2e  # noqa: E402  (reuses its Keycloak and HTTP helpers)

HANOI = {"name": "Nhà hát Lớn Hà Nội", "address": "1 Tràng Tiền, Hoàn Kiếm", "city": "Hà Nội", "capacity": 600}
MY_DINH = {"name": "Cung Thể thao Mỹ Đình", "address": "Lê Đức Thọ, Nam Từ Liêm", "city": "Hà Nội", "capacity": 5000}
SAIGON = {"name": "Nhà hát Thành phố", "address": "7 Công Trường Lam Sơn, Quận 1", "city": "TP. Hồ Chí Minh", "capacity": 500}
SECC = {"name": "SECC Quận 7", "address": "799 Nguyễn Văn Linh, Quận 7", "city": "TP. Hồ Chí Minh", "capacity": 8000}
DANANG = {"name": "Công viên Biển Đông", "address": "Võ Nguyên Giáp, Sơn Trà", "city": "Đà Nẵng", "capacity": 10000}
TIEN_SON = {"name": "Cung Thể thao Tiên Sơn", "address": "Phan Đăng Lưu, Hải Châu", "city": "Đà Nẵng", "capacity": 3000}

# name, slug, days from now, local hour, venue, waiting room, description, [(ticket, seats, price)]
EVENTS = [
    ("Đêm nhạc Mùa Thu Hà Nội", "dem-nhac-mua-thu-ha-noi", 12, 20, HANOI, False,
     "Một đêm acoustic ấm áp giữa lòng phố cổ, với những bản tình ca về Hà Nội mùa thu do dàn nhạc thính phòng trình diễn.",
     [("Hạng A", 120, 1_200_000), ("Hạng B", 240, 800_000), ("Ban công", 180, 450_000)]),
    ("Neon Tide Festival 2026", "neon-tide-festival-2026", 25, 17, DANANG, True,
     "Lễ hội âm nhạc điện tử ven biển kéo dài từ hoàng hôn đến nửa đêm, với ba sân khấu và hơn 20 DJ trong nước và quốc tế.",
     [("VIP Deck", 300, 3_500_000), ("GA Early Bird", 2000, 1_200_000), ("GA", 4000, 1_600_000)]),
    ("Vũ Điệu Thành Phố Live Tour", "vu-dieu-thanh-pho-live-tour", 18, 19, SECC, True,
     "Concert lớn nhất năm của ban nhạc pop đình đám, sân khấu 360 độ cùng hệ thống ánh sáng và pháo hoa trong nhà.",
     [("SVIP", 200, 4_800_000), ("VIP", 800, 3_200_000), ("CAT 1", 2500, 1_800_000), ("CAT 2", 3000, 990_000)]),
    ("Kịch nói: Người Đàn Bà Đi Qua Gió", "kich-noi-nguoi-dan-ba-di-qua-gio", 9, 20, SAIGON, False,
     "Vở kịch tâm lý xã hội được đạo diễn trẻ dàn dựng lại, đề cử Giải thưởng Sân khấu năm nay.",
     [("Ghế trung tâm", 150, 650_000), ("Ghế cánh", 200, 450_000)]),
    ("Giải Chạy Đêm Hồ Tây", "giai-chay-dem-ho-tay", 33, 19, MY_DINH, False,
     "Giải marathon đêm quanh Hồ Tây với các cự ly 5 km, 10 km và 21 km. Bib, áo thi đấu và huy chương cho mọi người về đích.",
     [("21 km", 1500, 850_000), ("10 km", 2500, 650_000), ("5 km", 3000, 450_000)]),
    ("Workshop: Sản xuất nhạc điện tử từ A đến Z", "workshop-san-xuat-nhac-dien-tu", 6, 9, TIEN_SON, False,
     "Một ngày thực hành cùng producer nhiều năm kinh nghiệm: từ ý tưởng, beat, mixing đến phát hành trên nền tảng số.",
     [("Vé thực hành (kèm máy)", 30, 1_500_000), ("Vé dự thính", 80, 500_000)]),
    ("Sài Gòn Jazz Night", "sai-gon-jazz-night", 15, 21, SAIGON, False,
     "Đêm jazz thân mật với bộ tứ piano, contrabass, trống và kèn saxophone, phục vụ kèm cocktail đặc biệt.",
     [("Bàn VIP (2 người)", 20, 2_400_000), ("Ghế thường", 160, 690_000)]),
    ("Lễ hội Ánh Sáng Sông Hàn", "le-hoi-anh-sang-song-han", 40, 18, DANANG, False,
     "Ba đêm trình diễn ánh sáng, nghệ thuật sắp đặt và âm nhạc đường phố dọc hai bờ sông Hàn.",
     [("Vé 3 đêm", 1500, 690_000), ("Vé 1 đêm", 5000, 290_000)]),
    ("Ballet Hồ Thiên Nga", "ballet-ho-thien-nga", 21, 20, HANOI, False,
     "Vở ballet kinh điển do đoàn múa quốc gia biểu diễn cùng dàn nhạc giao hưởng chơi trực tiếp.",
     [("Hạng A", 100, 1_800_000), ("Hạng B", 200, 1_100_000), ("Ban công", 150, 600_000)]),
    ("Rap Việt All-Stars Showcase", "rap-viet-all-stars-showcase", 28, 19, MY_DINH, True,
     "Đêm hội tụ những rapper được yêu thích nhất, với các màn kết hợp lần đầu tiên xuất hiện trên sân khấu.",
     [("Fanzone", 1000, 2_200_000), ("Standing", 3000, 1_100_000)]),
    ("Giải Bóng Rổ Mùa Đông", "giai-bong-ro-mua-dong", 11, 18, TIEN_SON, False,
     "Trận chung kết giải bóng rổ chuyên nghiệp mùa đông giữa hai đội dẫn đầu bảng xếp hạng.",
     [("Courtside", 40, 1_500_000), ("Khán đài A", 1200, 350_000), ("Khán đài B", 1500, 200_000)]),
    ("Acoustic Chill trên Sân Thượng", "acoustic-chill-tren-san-thuong", 4, 20, SAIGON, False,
     "Buổi diễn acoustic nhỏ trên sân thượng nhìn ra trung tâm thành phố. Số ghế có hạn, gần như đã kín chỗ.",
     [("Ghế đệm", 12, 550_000)]),
]


def main():
    admin = e2e.fetch_admin_token()
    status, existing = e2e.call("GET", f"{e2e.CATALOG}/admin/events?size=500", admin)
    if status != 200:
        raise SystemExit(f"Could not list events: {status} {existing}")
    existing = existing if isinstance(existing, list) else existing.get("content", [])
    slugs = {event["slug"] for event in existing}

    for event in existing:
        if event["name"].startswith("E2E ") and event["status"] == "PUBLISHED":
            e2e.call("POST", f"{e2e.CATALOG}/admin/events/{event['id']}/status", admin, {"status": "ENDED"})
            print(f"ended   {event['name']}")

    today = datetime.now(timezone.utc).replace(hour=0, minute=0, second=0, microsecond=0)
    for name, slug, days, hour, venue, waiting_room, description, ticket_types in EVENTS:
        if slug in slugs:
            print(f"skipped {name} (already exists)")
            continue
        start = today + timedelta(days=days, hours=hour - 7)  # local time is UTC+7
        status, body = e2e.call(
            "POST", f"{e2e.CATALOG}/admin/events?publish=true", admin,
            {
                "name": name, "slug": slug, "description": description,
                "startTime": start.strftime("%Y-%m-%dT%H:%M:%SZ"),
                "endTime": (start + timedelta(hours=3)).strftime("%Y-%m-%dT%H:%M:%SZ"),
                "waitingRoom": waiting_room, "venue": venue,
                "ticketTypes": [{"name": t, "totalQuantity": seats, "price": price} for t, seats, price in ticket_types],
            },
        )
        print(f"{'created' if status == 201 else f'FAILED {status}'} {name}" + ("" if status == 201 else f": {body}"))


if __name__ == "__main__":
    main()
