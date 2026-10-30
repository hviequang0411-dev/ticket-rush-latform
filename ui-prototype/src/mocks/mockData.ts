import type { Event, OrderSummary, TicketPass, WebhookLog } from '../types/models';

export const mockEvents: Event[] = [
  {
    id: 1,
    name: 'Đêm Nhạc Mùa Thu Hà Nội - Giai Điệu Ký Ức',
    slug: 'dem-nhac-mua-thu-ha-noi',
    description: 'Một không gian âm nhạc acoustic sâu lắng giữa lòng thủ đô với dàn nhạc giao hưởng thính phòng và các bản phối độc quyền.',
    category: 'Concert',
    startTime: '2026-11-15T20:00:00+07:00',
    endTime: '2026-11-15T23:00:00+07:00',
    status: 'PUBLISHED',
    isHot: true,
    posterBg: {
      gradient: 'linear-gradient(135deg, #451a03 0%, #1c1917 100%)',
      accent: '#f97316',
      pattern: 'waves'
    },
    venue: {
      id: 1,
      name: 'Nhà Hát Lớn Hà Nội',
      address: 'Số 1 Tràng Tiền, Quận Hoàn Kiếm',
      city: 'Hà Nội',
      capacity: 900
    },
    ticketTypes: [
      { id: 101, name: 'Vé VIP Thính Phòng', price: 1800000, currency: 'VND', totalQuantity: 150, availableQuantity: 12, description: 'Hàng ghế A-C, kèm quà lưu niệm và đồ uống nhẹ đón tiếp.', event: { id: 1 } },
      { id: 102, name: 'Vé Khán Đài Tầng 1', price: 950000, currency: 'VND', totalQuantity: 400, availableQuantity: 160, description: 'Tầm nhìn trực diện sân khấu, âm thanh vòm chuẩn rạp hát.', event: { id: 1 } },
      { id: 103, name: 'Vé Khán Đài Tầng 2', price: 550000, currency: 'VND', totalQuantity: 350, availableQuantity: 0, description: 'Hết vé sớm trong đợt Early Bird.', event: { id: 1 } }
    ]
  },
  {
    id: 2,
    name: 'Neon Tide Festival 2026 - Đại Tiệc Ánh Sáng & Điện Tử',
    slug: 'neon-tide-festival-2026',
    description: 'Lễ hội âm nhạc điện tử ven biển lớn nhất miền Trung quy tụ các producer quốc tế và hiệu ứng laser visual 3D hoành tráng.',
    category: 'Festival',
    startTime: '2026-12-05T16:00:00+07:00',
    endTime: '2026-12-06T00:00:00+07:00',
    status: 'PUBLISHED',
    isHot: true,
    posterBg: {
      gradient: 'linear-gradient(135deg, #0f172a 0%, #0369a1 100%)',
      accent: '#38bdf8',
      pattern: 'mesh'
    },
    venue: {
      id: 2,
      name: 'Công Viên Biển Đông',
      address: 'Đường Võ Nguyên Giáp, Phước Mỹ, Sơn Trà',
      city: 'Đà Nẵng',
      capacity: 12000
    },
    ticketTypes: [
      { id: 201, name: 'Early Bird GA', price: 650000, currency: 'VND', totalQuantity: 2000, availableQuantity: 0, event: { id: 2 } },
      { id: 202, name: 'General Admission (GA)', price: 850000, currency: 'VND', totalQuantity: 6000, availableQuantity: 2400, event: { id: 2 } },
      { id: 203, name: 'VIP Lounge Deck', price: 2200000, currency: 'VND', totalQuantity: 800, availableQuantity: 85, event: { id: 2 } }
    ]
  },
  {
    id: 3,
    name: 'Kịch Nghệ Sài Gòn: Chuyện Phố Khuya',
    slug: 'kich-nghe-sai-gon-chuyen-pho-khuya',
    description: 'Vở kịch sân khấu tâm lý xã hội đặc sắc tái hiện những mảnh đời chân thật nơi góc phố hoa lệ qua nhiều thế hệ.',
    category: 'Sân khấu',
    startTime: '2026-11-20T19:30:00+07:00',
    endTime: '2026-11-20T22:00:00+07:00',
    status: 'PUBLISHED',
    isHot: false,
    posterBg: {
      gradient: 'linear-gradient(135deg, #311313 0%, #1f2937 100%)',
      accent: '#ef4444',
      pattern: 'circles'
    },
    venue: {
      id: 3,
      name: 'Sân Khấu Kịch Trung Tâm',
      address: 'Quận 1, TP. Hồ Chí Minh',
      city: 'TP. Hồ Chí Minh',
      capacity: 650
    },
    ticketTypes: [
      { id: 301, name: 'Ghế Standard', price: 350000, currency: 'VND', totalQuantity: 300, availableQuantity: 180, event: { id: 3 } },
      { id: 302, name: 'Ghế Premium', price: 500000, currency: 'VND', totalQuantity: 200, availableQuantity: 45, event: { id: 3 } }
    ]
  },
  {
    id: 4,
    name: 'Giải Chạy Bán Marathon Bờ Đông Sông Hàn',
    slug: 'giai-chay-ban-marathon-song-han',
    description: 'Cung đường marathon đạt chuẩn quốc tế ngắm trọn bình minh sông Hàn và các cây cầu biểu tượng.',
    category: 'Thể thao',
    startTime: '2026-12-12T04:30:00+07:00',
    status: 'PUBLISHED',
    isHot: false,
    posterBg: {
      gradient: 'linear-gradient(135deg, #052e16 0%, #14532d 100%)',
      accent: '#22c55e',
      pattern: 'grid'
    },
    venue: {
      id: 4,
      name: 'Cung Thể Thao Tiên Sơn',
      address: 'Phan Đăng Lưu, Hải Châu',
      city: 'Đà Nẵng',
      capacity: 5000
    },
    ticketTypes: [
      { id: 401, name: 'Bib 10KM Run', price: 450000, currency: 'VND', totalQuantity: 1500, availableQuantity: 320, event: { id: 4 } },
      { id: 402, name: 'Bib 21KM Half Marathon', price: 750000, currency: 'VND', totalQuantity: 2000, availableQuantity: 120, event: { id: 4 } }
    ]
  },
  {
    id: 5,
    name: 'Workshop Nhiếp Ảnh Đường Phố & Thiết Bị Cổ Điển',
    slug: 'workshop-nhiep-anh-duong-pho',
    description: 'Khóa học thực chiến góc nhìn bố cục, ánh sáng tự nhiên và kỹ thuật tráng film truyền thống.',
    category: 'Workshop',
    startTime: '2026-11-28T09:00:00+07:00',
    status: 'PUBLISHED',
    isHot: false,
    posterBg: {
      gradient: 'linear-gradient(135deg, #18181b 0%, #27272a 100%)',
      accent: '#e4e4e7',
      pattern: 'grid'
    },
    venue: {
      id: 5,
      name: 'Không Gian Sáng Tạo Phố Cổ',
      address: 'Đào Duy Từ, Hàng Buồm, Hoàn Kiếm',
      city: 'Hà Nội',
      capacity: 50
    },
    ticketTypes: [
      { id: 501, name: 'Vé Học Viên Tiêu Chuẩn', price: 400000, currency: 'VND', totalQuantity: 40, availableQuantity: 8, event: { id: 5 } }
    ]
  },
  {
    id: 6,
    name: 'Bản Hòa Âm Tinh Cầu - Live Symphony Experience',
    slug: 'ban-hoa-am-tinh-cau',
    description: 'Sự kết hợp giữa nhạc phim khoa học viễn tưởng và dàn nhạc giao hưởng 80 nhạc công với màn chiếu mái vòm tinh vân.',
    category: 'Concert',
    startTime: '2027-01-15T19:30:00+07:00',
    status: 'DRAFT',
    isHot: true,
    posterBg: {
      gradient: 'linear-gradient(135deg, #1e1b4b 0%, #312e81 100%)',
      accent: '#818cf8',
      pattern: 'circles'
    },
    venue: {
      id: 6,
      name: 'Trung Tâm Hội Nghị Triển Lãm SECC',
      address: '799 Nguyễn Văn Linh, Quận 7',
      city: 'TP. Hồ Chí Minh',
      capacity: 4500
    },
    ticketTypes: [
      { id: 601, name: 'Vé Hạng Bạc', price: 600000, currency: 'VND', totalQuantity: 1500, availableQuantity: 1500, event: { id: 6 } },
      { id: 602, name: 'Vé Hạng Vàng', price: 1200000, currency: 'VND', totalQuantity: 1200, availableQuantity: 1200, event: { id: 6 } },
      { id: 603, name: 'Vé Hạng Kim Cương', price: 2500000, currency: 'VND', totalQuantity: 500, availableQuantity: 500, event: { id: 6 } }
    ]
  },
  {
    id: 7,
    name: 'Vũ Điệu Lửa & Băng - Nhạc Kịch Broadway Phỏng Tác',
    slug: 'vu-dieu-lua-va-bang',
    description: 'Tác phẩm sân khấu được đầu tư phục trang và âm thanh chuẩn quốc tế, mang lại trải nghiệm mãn nhãn cho khán giả.',
    category: 'Sân khấu',
    startTime: '2026-10-15T20:00:00+07:00',
    status: 'ENDED',
    posterBg: {
      gradient: 'linear-gradient(135deg, #172554 0%, #1e293b 100%)',
      accent: '#60a5fa',
      pattern: 'waves'
    },
    venue: {
      id: 7,
      name: 'Nhà Hát Thành Phố',
      address: 'Số 7 Lam Sơn, Bến Nghé, Quận 1',
      city: 'TP. Hồ Chí Minh',
      capacity: 800
    },
    ticketTypes: [
      { id: 701, name: 'Ghế Standard', price: 500000, currency: 'VND', totalQuantity: 400, availableQuantity: 0, event: { id: 7 } },
      { id: 702, name: 'Ghế VIP Box', price: 1500000, currency: 'VND', totalQuantity: 100, availableQuantity: 0, event: { id: 7 } }
    ]
  },
  {
    id: 8,
    name: 'Chung Kết Tranh Cúp Thể Thao Điện Tử Quốc Gia 2026',
    slug: 'chung-ket-esports-quoc-gia-2026',
    description: 'Trận thư hùng nảy lửa giữa 4 đội tuyển mạnh nhất cả nước trên sân khấu đấu trường ánh sáng công nghệ cao.',
    category: 'Thể thao',
    startTime: '2026-11-22T13:00:00+07:00',
    status: 'PUBLISHED',
    isHot: true,
    posterBg: {
      gradient: 'linear-gradient(135deg, #18181b 0%, #7f1d1d 100%)',
      accent: '#f87171',
      pattern: 'mesh'
    },
    venue: {
      id: 8,
      name: 'Nhà Thi Đấu Cầu Giấy',
      address: '35 Trần Quý Kiên, Cầu Giấy',
      city: 'Hà Nội',
      capacity: 3200
    },
    ticketTypes: [
      { id: 801, name: 'Vé Khán Đài A', price: 300000, currency: 'VND', totalQuantity: 1200, availableQuantity: 34, event: { id: 8 } },
      { id: 802, name: 'Vé Khu Vực Sàn Fan Zone', price: 600000, currency: 'VND', totalQuantity: 800, availableQuantity: 0, event: { id: 8 } }
    ]
  },
  {
    id: 9,
    name: 'Sunset Acoustic Chill: Hoàng Hôn Sơn Trà',
    slug: 'sunset-acoustic-chill-son-tra',
    description: 'Buổi chiều hoàng hôn lãng mạn bên sườn bán đảo, thưởng thức cà phê và những bản tình ca mộc mạc.',
    category: 'Concert',
    startTime: '2026-11-29T16:30:00+07:00',
    status: 'PUBLISHED',
    posterBg: {
      gradient: 'linear-gradient(135deg, #7c2d12 0%, #1e1b4b 100%)',
      accent: '#fb923c',
      pattern: 'circles'
    },
    venue: {
      id: 9,
      name: 'Vọng Cảnh Hilltop',
      address: 'Bán Đảo Sơn Trà',
      city: 'Đà Nẵng',
      capacity: 250
    },
    ticketTypes: [
      { id: 901, name: 'Vé Sunset View (Kèm Cocktail)', price: 350000, currency: 'VND', totalQuantity: 180, availableQuantity: 28, event: { id: 9 } }
    ]
  },
  {
    id: 10,
    name: 'Hội Thảo Thiết Kế Trải Nghiệm Số Tương Lai (UX Next 2026)',
    slug: 'hoi-thao-ux-next-2026',
    description: 'Diễn đàn thường niên kết nối hơn 500 chuyên gia thiết kế sản phẩm, AI UI và kỹ sư hệ thống trên toàn quốc.',
    category: 'Workshop',
    startTime: '2026-12-18T08:30:00+07:00',
    status: 'PUBLISHED',
    posterBg: {
      gradient: 'linear-gradient(135deg, #042f2e 0%, #0f172a 100%)',
      accent: '#14b8a6',
      pattern: 'grid'
    },
    venue: {
      id: 10,
      name: 'Khách Sạn Melia Hà Nội',
      address: '44 Lý Thường Kiệt, Hoàn Kiếm',
      city: 'Hà Nội',
      capacity: 600
    },
    ticketTypes: [
      { id: 1001, name: 'Standard Pass', price: 900000, currency: 'VND', totalQuantity: 300, availableQuantity: 110, event: { id: 10 } },
      { id: 1002, name: 'VIP Networking Pass', price: 1900000, currency: 'VND', totalQuantity: 100, availableQuantity: 15, event: { id: 10 } }
    ]
  },
  {
    id: 11,
    name: 'Indie Wave Saigon: Giai Điệu Tuổi Trẻ Tập 4',
    slug: 'indie-wave-saigon-tap-4',
    description: 'Sân chơi độc lập tràn đầy năng lượng với các ban nhạc indie đang được thế hệ trẻ yêu thích nhất hiện nay.',
    category: 'Festival',
    startTime: '2026-11-21T18:00:00+07:00',
    status: 'PUBLISHED',
    posterBg: {
      gradient: 'linear-gradient(135deg, #3730a3 0%, #831843 100%)',
      accent: '#f472b6',
      pattern: 'waves'
    },
    venue: {
      id: 11,
      name: 'Nhà Thi Đấu Hồ Xuân Hương',
      address: 'Số 2 Hồ Xuân Hương, Phường 6, Quận 3',
      city: 'TP. Hồ Chí Minh',
      capacity: 2200
    },
    ticketTypes: [
      { id: 1101, name: 'Vé Đứng Sàn Nhảy', price: 420000, currency: 'VND', totalQuantity: 1200, availableQuantity: 450, event: { id: 11 } },
      { id: 1102, name: 'Vé Khán Đài B', price: 320000, currency: 'VND', totalQuantity: 800, availableQuantity: 210, event: { id: 11 } }
    ]
  },
  {
    id: 12,
    name: 'Hòa Nhạc Cổ Điển Mùa Đông Vienna',
    slug: 'hoa-nhac-co-dien-mua-dong-vienna',
    description: 'Thưởng thức các trích đoạn kinh điển của Mozart, Beethoven và Strauss trong không gian kiến trúc Pháp cổ điển.',
    category: 'Concert',
    startTime: '2026-12-24T20:00:00+07:00',
    status: 'PUBLISHED',
    posterBg: {
      gradient: 'linear-gradient(135deg, #1c1917 0%, #292524 100%)',
      accent: '#d97706',
      pattern: 'mesh'
    },
    venue: {
      id: 12,
      name: 'Nhà Hát Tuổi Trẻ',
      address: '11 Ngô Thì Nhậm, Hai Bà Trưng',
      city: 'Hà Nội',
      capacity: 700
    },
    ticketTypes: [
      { id: 1201, name: 'Vé Ghế Tầng 1', price: 750000, currency: 'VND', totalQuantity: 300, availableQuantity: 65, event: { id: 12 } },
      { id: 1202, name: 'Vé Ghế Tầng 2', price: 450000, currency: 'VND', totalQuantity: 250, availableQuantity: 90, event: { id: 12 } }
    ]
  }
];

export const mockOrders: OrderSummary[] = [
  {
    id: 9001,
    code: 'TR-ORD-88231',
    status: 'CONFIRMED',
    totalAmount: 2750000,
    currency: 'VND',
    createdAt: '2026-10-28T14:20:00+07:00',
    customerName: 'Nguyễn Văn An',
    customerEmail: 'an.nguyen@example.com',
    customerPhone: '0912345678',
    event: {
      id: 1,
      name: 'Đêm Nhạc Mùa Thu Hà Nội - Giai Điệu Ký Ức',
      startTime: '2026-11-15T20:00:00+07:00',
      venueName: 'Nhà Hát Lớn Hà Nội',
      venueAddress: 'Số 1 Tràng Tiền, Quận Hoàn Kiếm, Hà Nội'
    },
    items: [
      { eventId: 1, ticketTypeId: 101, ticketTypeName: 'Vé VIP Thính Phòng', quantity: 1, unitPrice: 1800000 },
      { eventId: 1, ticketTypeId: 102, ticketTypeName: 'Vé Khán Đài Tầng 1', quantity: 1, unitPrice: 950000 }
    ]
  },
  {
    id: 9002,
    code: 'TR-ORD-88232',
    status: 'AWAITING_PAYMENT',
    totalAmount: 1700000,
    currency: 'VND',
    createdAt: new Date(Date.now() - 4 * 60 * 1000).toISOString(),
    expiresAt: new Date(Date.now() + 6 * 60 * 1000).toISOString(),
    customerName: 'Trần Thị Mai',
    customerEmail: 'mai.tran@example.com',
    customerPhone: '0987654321',
    event: {
      id: 2,
      name: 'Neon Tide Festival 2026 - Đại Tiệc Ánh Sáng & Điện Tử',
      startTime: '2026-12-05T16:00:00+07:00',
      venueName: 'Công Viên Biển Đông',
      venueAddress: 'Đường Võ Nguyên Giáp, Đà Nẵng'
    },
    items: [
      { eventId: 2, ticketTypeId: 202, ticketTypeName: 'General Admission (GA)', quantity: 2, unitPrice: 850000 }
    ]
  },
  {
    id: 9003,
    code: 'TR-ORD-88233',
    status: 'EXPIRED',
    totalAmount: 500000,
    currency: 'VND',
    createdAt: '2026-10-25T11:00:00+07:00',
    failureReason: 'Quá thời gian thanh toán quy định (10 phút). Ghế đã tự động hoàn trả vào kho vé.',
    customerName: 'Lê Hoàng Nam',
    customerEmail: 'nam.le@example.com',
    customerPhone: '0903112233',
    event: {
      id: 3,
      name: 'Kịch Nghệ Sài Gòn: Chuyện Phố Khuya',
      startTime: '2026-11-20T19:30:00+07:00',
      venueName: 'Sân Khấu Kịch Trung Tâm',
      venueAddress: 'Quận 1, TP. Hồ Chí Minh'
    },
    items: [
      { eventId: 3, ticketTypeId: 302, ticketTypeName: 'Ghế Premium', quantity: 1, unitPrice: 500000 }
    ]
  },
  {
    id: 9004,
    code: 'TR-ORD-88234',
    status: 'REFUNDED',
    totalAmount: 600000,
    currency: 'VND',
    createdAt: '2026-10-20T09:15:00+07:00',
    failureReason: 'Lỗi đồng bộ tồn kho do 2 phiên thanh toán đồng thời cùng mili-giây. Hệ thống đã chủ động hoàn 100% tiền qua phương thức gốc.',
    customerName: 'Phạm Quỳnh Anh',
    customerEmail: 'quynhanh@example.com',
    customerPhone: '0934567890',
    event: {
      id: 8,
      name: 'Chung Kết Tranh Cúp Thể Thao Điện Tử Quốc Gia 2026',
      startTime: '2026-11-22T13:00:00+07:00',
      venueName: 'Nhà Thi Đấu Cầu Giấy',
      venueAddress: '35 Trần Quý Kiên, Cầu Giấy, Hà Nội'
    },
    items: [
      { eventId: 8, ticketTypeId: 802, ticketTypeName: 'Vé Khu Vực Sàn Fan Zone', quantity: 1, unitPrice: 600000 }
    ]
  }
];

export const mockPasses: TicketPass[] = [
  {
    id: 'PASS-7701',
    orderCode: 'TR-ORD-88231',
    eventTitle: 'Đêm Nhạc Mùa Thu Hà Nội - Giai Điệu Ký Ức',
    eventDate: '20:00, Thứ Bảy 15/11/2026',
    venueName: 'Nhà Hát Lớn Hà Nội',
    venueAddress: 'Số 1 Tràng Tiền, Hoàn Kiếm, Hà Nội',
    ticketTypeName: 'Vé VIP Thính Phòng',
    seatInfo: 'Dãy A - Ghế 12',
    qrPayload: 'TICKETRUSH::PASS-7701::ORD-88231::SEC-VIP-A12',
    customerName: 'Nguyễn Văn An',
    status: 'VALID'
  },
  {
    id: 'PASS-7702',
    orderCode: 'TR-ORD-88231',
    eventTitle: 'Đêm Nhạc Mùa Thu Hà Nội - Giai Điệu Ký Ức',
    eventDate: '20:00, Thứ Bảy 15/11/2026',
    venueName: 'Nhà Hát Lớn Hà Nội',
    venueAddress: 'Số 1 Tràng Tiền, Hoàn Kiếm, Hà Nội',
    ticketTypeName: 'Vé Khán Đài Tầng 1',
    seatInfo: 'Dãy D - Ghế 08',
    qrPayload: 'TICKETRUSH::PASS-7702::ORD-88231::SEC-KD1-D08',
    customerName: 'Nguyễn Văn An',
    status: 'VALID'
  }
];

export const mockWebhooks: WebhookLog[] = [
  {
    id: 'WH-001',
    timestamp: '2026-10-30T10:15:30+07:00',
    provider: 'VNPAY',
    providerRef: 'VNPAY_TXN_99210',
    orderCode: 'TR-ORD-88231',
    amount: 2750000,
    signatureValid: true,
    processed: true,
    payload: '{"vnp_ResponseCode":"00","vnp_TransactionNo":"14982731","vnp_Amount":"275000000"}'
  },
  {
    id: 'WH-002',
    timestamp: '2026-10-30T11:02:15+07:00',
    provider: 'MOMO',
    providerRef: 'MOMO_TXN_44921',
    orderCode: 'TR-ORD-88232',
    amount: 1700000,
    signatureValid: true,
    processed: false,
    payload: '{"resultCode":1000,"transId":"283918239","amount":1700000}'
  },
  {
    id: 'WH-003',
    timestamp: '2026-10-30T11:45:00+07:00',
    provider: 'MOCK',
    providerRef: 'MOCK_ERR_112',
    orderCode: 'TR-ORD-88233',
    amount: 500000,
    signatureValid: false,
    processed: false,
    payload: '{"error":"INVALID_SIGNATURE_HASH"}'
  }
];

