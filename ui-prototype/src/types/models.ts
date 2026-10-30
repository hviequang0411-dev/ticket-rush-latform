export type EventStatus = 'DRAFT' | 'PUBLISHED' | 'CANCELLED' | 'ENDED';
export type OrderStatus = 'PENDING' | 'RESERVED' | 'AWAITING_PAYMENT' | 'CONFIRMED' | 'CANCELLED' | 'EXPIRED' | 'FAILED' | 'REFUNDED';
export type PaymentStatus = 'PENDING' | 'SUCCEEDED' | 'FAILED' | 'EXPIRED' | 'REFUNDED';
export type PaymentProvider = 'MOCK' | 'VNPAY' | 'MOMO';

export interface Venue {
  id: number;
  name: string;
  address: string;
  city: 'Hà Nội' | 'TP. Hồ Chí Minh' | 'Đà Nẵng';
  capacity: number;
}

export interface TicketType {
  id: number;
  name: string;
  price: number;
  currency: 'VND';
  totalQuantity: number;
  availableQuantity: number;
  description?: string;
  saleStart?: string;
  saleEnd?: string;
  event: { id: number };
}

export interface Event {
  id: number;
  name: string;
  slug: string;
  description: string;
  category: 'Concert' | 'Festival' | 'Sân khấu' | 'Thể thao' | 'Workshop';
  startTime: string;
  endTime?: string;
  status: EventStatus;
  posterBg: {
    gradient: string;
    accent: string;
    pattern: string;
  };
  venue: Venue;
  isHot?: boolean;
  ticketTypes: TicketType[];
}

export interface TicketStock {
  ticketTypeId: number;
  eventId: number;
  totalQuantity: number;
  availableQuantity: number;
  reservedQuantity: number;
  soldQuantity: number;
}

export interface OrderItem {
  eventId: number;
  ticketTypeId: number;
  ticketTypeName: string;
  quantity: number;
  unitPrice: number;
}

export interface OrderSummary {
  id: number;
  code: string;
  status: OrderStatus;
  totalAmount: number;
  currency: 'VND';
  createdAt: string;
  expiresAt?: string;
  failureReason?: string;
  /** Where to pay while the order is AWAITING_PAYMENT (the provider's hosted page). */
  paymentUrl?: string;
  customerName: string;
  customerEmail: string;
  customerPhone: string;
  event: {
    id: number;
    name: string;
    startTime: string;
    venueName: string;
    venueAddress: string;
  };
  items: OrderItem[];
}

export interface Payment {
  id: number;
  orderId: number;
  provider: PaymentProvider;
  providerRef: string;
  status: PaymentStatus;
  paymentUrl: string;
  amount: number;
  currency: 'VND';
  createdAt: string;
  expiresAt: string;
  failureReason?: string;
}

export interface ApiErrorProblem {
  status: number;
  title: string;
  detail: string;
  message: string;
  retryAfterSeconds?: number;
}

export interface TicketPass {
  id: string;
  orderCode: string;
  eventTitle: string;
  eventDate: string;
  venueName: string;
  venueAddress: string;
  ticketTypeName: string;
  seatInfo: string;
  qrPayload: string;
  customerName: string;
  status: 'VALID' | 'USED' | 'CANCELLED';
  checkInAt?: string;
}

export interface WebhookLog {
  id: string;
  timestamp: string;
  provider: PaymentProvider;
  providerRef: string;
  orderCode: string;
  amount: number;
  signatureValid: boolean;
  processed: boolean;
  payload: string;
}

