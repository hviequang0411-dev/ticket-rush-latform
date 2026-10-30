import { mockPasses, mockWebhooks } from '../mocks/mockData';
import type { Event, OrderSummary, TicketPass, WebhookLog, ApiErrorProblem, TicketType } from '../types/models';
import { displayName, fetchAccount } from './auth';
import { get, post } from './http';

// The buying flow talks to the real services through the gateway:
//   catalog   /services/catalog/api/public/events        (anonymous)
//   inventory /services/inventory/api/public/availability (anonymous)
//   order     /services/order/api/checkout, my-orders     (signed in)
// Ticket check-in and the webhook log have no backend yet and stay on mock data (marked "demo" in the UI).

interface PublicEvent {
  id: number;
  name: string;
  slug: string;
  description?: string;
  startTime: string;
  endTime?: string;
  status: Event['status'];
  posterUrl?: string;
  venue: { id: number; name: string; address: string; city: string; capacity: number };
  ticketTypes: { id: number; name: string; price: number; currency: 'VND'; totalQuantity: number; saleStart?: string; saleEnd?: string }[];
}

interface Availability {
  ticketTypeId: number;
  totalQuantity: number;
  availableQuantity: number;
}

interface OrderResponse {
  code: string;
  status: OrderSummary['status'];
  totalAmount: number;
  currency: 'VND';
  createdAt: string;
  expiresAt?: string;
  failureReason?: string;
  paymentUrl?: string;
  items: { eventId: number; ticketTypeId: number; quantity: number; unitPrice: number }[];
}

const POSTERS: Event['posterBg'][] = [
  { gradient: 'linear-gradient(135deg, #4f46e5 0%, #9333ea 100%)', accent: '#c4b5fd', pattern: 'waves' },
  { gradient: 'linear-gradient(135deg, #0ea5e9 0%, #2563eb 100%)', accent: '#bae6fd', pattern: 'grid' },
  { gradient: 'linear-gradient(135deg, #f97316 0%, #db2777 100%)', accent: '#fed7aa', pattern: 'dots' },
  { gradient: 'linear-gradient(135deg, #10b981 0%, #0f766e 100%)', accent: '#a7f3d0', pattern: 'waves' },
  { gradient: 'linear-gradient(135deg, #e11d48 0%, #7c3aed 100%)', accent: '#fecdd3', pattern: 'grid' },
];

const CATEGORIES: [RegExp, Event['category']][] = [
  [/festival|lễ hội/i, 'Festival'],
  [/workshop|hội thảo|talk/i, 'Workshop'],
  [/cup|giải|bóng|marathon|thể thao/i, 'Thể thao'],
  [/kịch|sân khấu|opera|ballet/i, 'Sân khấu'],
];

/** The catalog has no category or artwork yet: derive stable ones from the event itself. */
function toEvent(event: PublicEvent, availability: Map<number, Availability>): Event {
  const ticketTypes: TicketType[] = event.ticketTypes.map(type => ({
    id: type.id,
    name: type.name,
    price: Number(type.price),
    currency: 'VND',
    totalQuantity: availability.get(type.id)?.totalQuantity ?? type.totalQuantity,
    // No stock record yet (inventory still syncing): show nothing for sale rather than seats that may not exist
    availableQuantity: availability.get(type.id)?.availableQuantity ?? 0,
    saleStart: type.saleStart,
    saleEnd: type.saleEnd,
    event: { id: event.id },
  }));
  const total = ticketTypes.reduce((sum, type) => sum + type.totalQuantity, 0);
  const left = ticketTypes.reduce((sum, type) => sum + type.availableQuantity, 0);
  return {
    id: event.id,
    name: event.name,
    slug: event.slug,
    description: event.description ?? '',
    category: CATEGORIES.find(([pattern]) => pattern.test(event.name))?.[1] ?? 'Concert',
    startTime: event.startTime,
    endTime: event.endTime,
    status: event.status,
    posterBg: POSTERS[event.id % POSTERS.length],
    venue: { ...event.venue },
    isHot: total > 0 && left / total < 0.3,
    ticketTypes,
  } as Event;
}

async function withAvailability(events: PublicEvent[]): Promise<Event[]> {
  const ids = events.flatMap(event => event.ticketTypes.map(type => type.id));
  const availability = new Map<number, Availability>();
  // At most 100 ids per call
  for (let i = 0; i < ids.length; i += 100) {
    const chunk = await get<Availability[]>(`/services/inventory/api/public/availability?ticketTypeIds=${ids.slice(i, i + 100).join(',')}`);
    chunk.forEach(entry => availability.set(entry.ticketTypeId, entry));
  }
  return events.map(event => toEvent(event, availability));
}

const eventsById = new Map<number, Promise<PublicEvent | null>>();

function publicEvent(id: number): Promise<PublicEvent | null> {
  if (!eventsById.has(id)) {
    eventsById.set(id, get<PublicEvent>(`/services/catalog/api/public/events/by-id/${id}`).catch(() => null));
  }
  return eventsById.get(id)!;
}

async function toOrder(order: OrderResponse): Promise<OrderSummary> {
  const [account, event] = await Promise.all([fetchAccount(), order.items[0] ? publicEvent(order.items[0].eventId) : Promise.resolve(null)]);
  const typeName = (id: number) => event?.ticketTypes.find(type => type.id === id)?.name ?? `Loại vé #${id}`;
  return {
    id: 0,
    code: order.code,
    status: order.status,
    totalAmount: Number(order.totalAmount),
    currency: 'VND',
    createdAt: order.createdAt,
    expiresAt: order.expiresAt,
    failureReason: order.failureReason,
    paymentUrl: order.paymentUrl,
    customerName: account ? displayName(account) : '',
    customerEmail: account?.email ?? '',
    customerPhone: '',
    event: {
      id: event?.id ?? order.items[0]?.eventId ?? 0,
      name: event?.name ?? 'Sự kiện',
      startTime: event?.startTime ?? order.createdAt,
      venueName: event?.venue.name ?? '',
      venueAddress: event ? `${event.venue.address}, ${event.venue.city}` : '',
    },
    items: order.items.map(item => ({ ...item, unitPrice: Number(item.unitPrice), ticketTypeName: typeName(item.ticketTypeId) })),
  };
}

const IDEMPOTENCY_KEYS = 'ticketrush.idempotencyKeys';

/**
 * One key per purchase attempt: retrying the same selection (double click, timeout, 503) reuses it, so the order
 * service returns the first order instead of buying twice. A successful checkout forgets it.
 */
function idempotencyKey(selection: string): string {
  const keys = JSON.parse(sessionStorage.getItem(IDEMPOTENCY_KEYS) ?? '{}') as Record<string, string>;
  keys[selection] ??= crypto.randomUUID();
  sessionStorage.setItem(IDEMPOTENCY_KEYS, JSON.stringify(keys));
  return keys[selection];
}

function forgetIdempotencyKey(selection: string): void {
  const keys = JSON.parse(sessionStorage.getItem(IDEMPOTENCY_KEYS) ?? '{}') as Record<string, string>;
  delete keys[selection];
  sessionStorage.setItem(IDEMPOTENCY_KEYS, JSON.stringify(keys));
}

export class ApiService {
  static async getEvents(): Promise<Event[]> {
    return withAvailability(await get<PublicEvent[]>('/services/catalog/api/public/events?size=100'));
  }

  static async getEventBySlug(slug: string): Promise<Event> {
    const [event] = await withAvailability([await get<PublicEvent>(`/services/catalog/api/public/events/${encodeURIComponent(slug)}`)]);
    return event;
  }

  static async getEventById(id: number): Promise<Event> {
    const event = await publicEvent(id);
    if (!event) {
      throw { status: 404, title: 'Không tìm thấy sự kiện', detail: `Sự kiện #${id} không tồn tại.`, message: 'Sự kiện không tồn tại.' } satisfies ApiErrorProblem;
    }
    const [withSeats] = await withAvailability([event]);
    return withSeats;
  }

  /** Places the order and holds its seats for 10 minutes; the result carries the payment page URL. */
  static async createOrder(orderPayload: { eventId: number; items: { ticketTypeId: number; quantity: number }[] }): Promise<OrderSummary> {
    const items = orderPayload.items.filter(item => item.quantity > 0);
    const selection = JSON.stringify(items);
    const order = await post<OrderResponse>('/services/order/api/checkout', { items }, { 'Idempotency-Key': idempotencyKey(selection) });
    forgetIdempotencyKey(selection);
    return toOrder(order);
  }

  static async getOrders(): Promise<OrderSummary[]> {
    const orders = await get<OrderResponse[]>('/services/order/api/my-orders?size=50');
    return Promise.all(orders.map(toOrder));
  }

  static async getOrderByCode(code: string): Promise<OrderSummary> {
    return toOrder(await get<OrderResponse>(`/services/order/api/my-orders/${encodeURIComponent(code)}`));
  }

  static async cancelOrder(code: string): Promise<OrderSummary> {
    return toOrder(await post<OrderResponse>(`/services/order/api/my-orders/${encodeURIComponent(code)}/cancel`));
  }

  /** E-tickets for confirmed orders, one per seat. The QR code is illustrative: check-in has no backend yet. */
  static async getPasses(): Promise<TicketPass[]> {
    const orders = await ApiService.getOrders();
    return orders
      .filter(order => order.status === 'CONFIRMED')
      .flatMap(order =>
        order.items.flatMap(item =>
          Array.from({ length: item.quantity }, (_, seat) => ({
            id: `${order.code}-${item.ticketTypeId}-${seat + 1}`,
            orderCode: order.code,
            eventTitle: order.event.name,
            eventDate: order.event.startTime,
            venueName: order.event.venueName,
            venueAddress: order.event.venueAddress,
            ticketTypeName: item.ticketTypeName,
            seatInfo: `Vé ${seat + 1}/${item.quantity}`,
            qrPayload: `TICKETRUSH::${order.code}::${item.ticketTypeId}::${seat + 1}`,
            customerName: order.customerName,
            status: 'VALID' as const,
          })),
        ),
      );
  }

  // ---- Demo only: no backend yet ----

  static async checkInPass(qrPayload: string): Promise<{ success: boolean; pass?: TicketPass; reason?: string }> {
    const pass = mockPasses.find(p => p.qrPayload === qrPayload || p.id === qrPayload);
    if (!pass) {
      return { success: false, reason: 'Mã QR không hợp lệ hoặc không có trong hệ thống.' };
    }
    if (pass.status === 'USED') {
      return { success: false, pass, reason: `Vé này đã được quét lúc ${pass.checkInAt || 'trước đó'}.` };
    }
    if (pass.status === 'CANCELLED') {
      return { success: false, pass, reason: 'Vé này đã bị huỷ do đơn hàng hoàn tiền.' };
    }
    pass.status = 'USED';
    pass.checkInAt = new Date().toLocaleTimeString('vi-VN');
    return { success: true, pass };
  }

  static async getWebhooks(): Promise<WebhookLog[]> {
    return [...mockWebhooks];
  }
}
