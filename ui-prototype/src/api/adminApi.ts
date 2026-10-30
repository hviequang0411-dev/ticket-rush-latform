import type { Event, EventStatus, OrderStatus, PaymentProvider, PaymentStatus } from '../types/models';
import { toEvent, type Availability, type PublicEvent, type TicketView } from './apiClient';
import { get, post, request } from './http';

// Organiser screens. Every call goes through the gateway to /api/admin/** on a service, which only admins reach.

export interface StockLevel {
  ticketTypeId: number;
  eventId: number;
  totalQuantity: number;
  availableQuantity: number;
  heldQuantity: number;
  soldQuantity: number;
}

export interface ActiveHold {
  holdId: number;
  orderId: number;
  ticketTypeId: number;
  eventId: number;
  quantity: number;
  expiresAt: string;
}

export interface AdminOrder {
  code: string;
  userLogin: string;
  status: OrderStatus;
  totalAmount: number;
  currency: 'VND';
  createdAt: string;
  updatedAt?: string;
  expiresAt?: string;
  failureReason?: string;
  items: { eventId: number; ticketTypeId: number; quantity: number; unitPrice: number }[];
  tickets: TicketView[];
}

export interface PaymentRow {
  id: number;
  orderId: number;
  orderCode: string;
  amount: number;
  currency: string;
  provider: PaymentProvider;
  status: PaymentStatus;
  providerRef: string;
  failureReason?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface WebhookRow {
  id: number;
  provider: PaymentProvider;
  eventKey: string;
  orderCode?: string;
  amount?: number;
  signatureValid: boolean;
  outcome: 'PROCESSED' | 'REJECTED' | 'UNMATCHED' | 'PENDING';
  deliveries: number;
  receivedAt: string;
  lastReceivedAt?: string;
  processedAt?: string;
  payload: string;
}

export interface SalesStats {
  zone: string;
  today: SalesDay;
  yesterday: SalesDay;
  activeHolds: number;
  daily: { date: string; revenue: number; orders: number }[];
  paceMinutes: number;
  pace: { eventId: number; seats: number }[];
}

export interface SalesDay {
  date: string;
  revenue: number;
  ordersPlaced: number;
  ordersConfirmed: number;
  ordersFinished: number;
}

export interface QueueStats {
  eventId: number;
  waiting: number;
  admitted: number;
  joined: number;
  admitPerSecond: number;
}

export interface EventForm {
  name: string;
  slug: string;
  description?: string;
  startTime: string;
  endTime?: string;
  waitingRoom: boolean;
  venue: { name: string; address?: string; city: string; capacity?: number };
  ticketTypes: { id?: number; name: string; price: number; totalQuantity: number }[];
}

/** An event with where its seats are; ticket types carry the stock numbers. */
export interface AdminEvent extends Event {
  stock: Record<number, StockLevel>;
}

const CATALOG = '/services/catalog/api/admin/events';

async function withStock(events: PublicEvent[]): Promise<AdminEvent[]> {
  const levels = await stock(events.flatMap(event => event.ticketTypes.map(type => type.id)));
  const byType = new Map(levels.map(level => [level.ticketTypeId, level]));
  const availability = new Map<number, Availability>(levels.map(level => [level.ticketTypeId, level]));
  return events.map(event => ({
    ...toEvent(event, availability),
    stock: Object.fromEntries(event.ticketTypes.filter(type => byType.has(type.id)).map(type => [type.id, byType.get(type.id)!])),
  }));
}

async function stock(ticketTypeIds: number[]): Promise<StockLevel[]> {
  const levels: StockLevel[] = [];
  for (let i = 0; i < ticketTypeIds.length; i += 500) {
    levels.push(...(await get<StockLevel[]>(`/services/inventory/api/admin/stock?ticketTypeIds=${ticketTypeIds.slice(i, i + 500).join(',')}`)));
  }
  return levels;
}

function query(params: Record<string, string | number | undefined>): string {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== '') search.set(key, String(value));
  });
  return search.toString();
}

export const AdminApi = {
  async events(q?: string): Promise<AdminEvent[]> {
    return withStock(await get<PublicEvent[]>(`${CATALOG}?${query({ q, size: 200 })}`));
  },

  async createEvent(form: EventForm, publish: boolean): Promise<AdminEvent> {
    const [event] = await withStock([await post<PublicEvent>(`${CATALOG}?publish=${publish}`, form)]);
    return event;
  },

  async updateEvent(id: number, form: EventForm): Promise<AdminEvent> {
    const [event] = await withStock([await request<PublicEvent>('PUT', `${CATALOG}/${id}`, form)]);
    return event;
  },

  async changeEventStatus(id: number, status: EventStatus): Promise<AdminEvent> {
    const [event] = await withStock([await post<PublicEvent>(`${CATALOG}/${id}/status`, { status })]);
    return event;
  },

  expiringHolds(limit = 20): Promise<ActiveHold[]> {
    return get<ActiveHold[]>(`/services/inventory/api/admin/holds/expiring?limit=${limit}`);
  },

  orders(q?: string, status?: OrderStatus): Promise<AdminOrder[]> {
    return get<AdminOrder[]>(`/services/order/api/admin/orders?${query({ q, status, size: 100 })}`);
  },

  order(code: string): Promise<AdminOrder> {
    return get<AdminOrder>(`/services/order/api/admin/orders/${encodeURIComponent(code)}`);
  },

  sales(days = 14): Promise<SalesStats> {
    return get<SalesStats>(`/services/order/api/admin/stats/sales?${query({ days, zone: 'Asia/Ho_Chi_Minh', paceMinutes: 10 })}`);
  },

  payments(q?: string, status?: PaymentStatus): Promise<PaymentRow[]> {
    return get<PaymentRow[]>(`/services/payment/api/admin/payments?${query({ q, status, size: 100 })}`);
  },

  webhooks(): Promise<WebhookRow[]> {
    return get<WebhookRow[]>('/services/payment/api/admin/webhooks?size=100');
  },

  queue(eventId: number): Promise<QueueStats> {
    return get<QueueStats>(`/api/admin/waiting-room/${eventId}`);
  },
};
