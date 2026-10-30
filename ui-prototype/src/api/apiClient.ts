import type { Event, OrderSummary, TicketPass, ApiErrorProblem, TicketType } from '../types/models';
import { displayName, fetchAccount } from './auth';
import { get, post } from './http';

// The buying flow talks to the real services through the gateway:
//   catalog   /services/catalog/api/public/events        (anonymous)
//   inventory /services/inventory/api/public/availability (anonymous)
//   order     /services/order/api/checkout, my-orders, my-tickets (signed in)
//   gateway   /api/waiting-room/{eventId}                  (signed in)
// Organiser screens use ./adminApi.

interface PublicEvent {
  id: number;
  name: string;
  slug: string;
  description?: string;
  startTime: string;
  endTime?: string;
  status: Event['status'];
  posterUrl?: string;
  waitingRoom?: boolean;
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
export function toEvent(event: PublicEvent, availability: Map<number, Availability>): Event {
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
    waitingRoom: !!event.waitingRoom,
    ticketTypes,
  } as Event;
}

export type { PublicEvent, Availability };

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
    const headers: Record<string, string> = { 'Idempotency-Key': idempotencyKey(selection) };
    const pass = ApiService.queuePass(orderPayload.eventId);
    if (pass) {
      headers['X-Queue-Pass'] = pass;
    }
    const order = await post<OrderResponse>('/services/order/api/checkout', { items }, headers);
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

  /** The signed-in user's e-tickets, one per seat; the QR code is signed by the order service. */
  static async getPasses(): Promise<TicketPass[]> {
    const [tickets, account] = await Promise.all([get<TicketView[]>('/services/order/api/my-tickets'), fetchAccount()]);
    return Promise.all(tickets.map(ticket => toPass(ticket, account ? displayName(account) : ticket.userLogin)));
  }

  /** Gate scan (organisers only). Exactly one scan of a ticket admits it, however many gates scan at once. */
  static async checkInPass(qrPayload: string): Promise<{ success: boolean; pass?: TicketPass; reason?: string }> {
    const result = await post<{ outcome: 'ADMITTED' | 'ALREADY_USED' | 'CANCELLED' | 'INVALID'; ticket?: TicketView }>(
      '/services/order/api/admin/check-in',
      { qrPayload },
    );
    const pass = result.ticket ? await toPass(result.ticket, result.ticket.userLogin) : undefined;
    switch (result.outcome) {
      case 'ADMITTED':
        return { success: true, pass };
      case 'ALREADY_USED':
        return { success: false, pass, reason: `Vé này đã được quét lúc ${pass?.checkInAt ?? 'trước đó'}.` };
      case 'CANCELLED':
        return { success: false, pass, reason: 'Vé này đã bị huỷ cùng đơn hàng.' };
      default:
        return { success: false, reason: 'Mã QR không hợp lệ, bị sửa hoặc không do TicketRush phát hành.' };
    }
  }

  /** Takes (or keeps) a place in the event's waiting room. */
  static async joinQueue(eventId: number): Promise<QueueStatus> {
    return rememberPass(await post<QueueStatus>(`/api/waiting-room/${eventId}`));
  }

  static async queueStatus(eventId: number): Promise<QueueStatus> {
    return rememberPass(await get<QueueStatus>(`/api/waiting-room/${eventId}`));
  }

  /** A still-valid admission pass for the event, if the buyer has been let in. */
  static queuePass(eventId: number): string | null {
    const entry = savedPasses()[eventId];
    return entry && new Date(entry.expiresAt).getTime() > Date.now() + 5000 ? entry.pass : null;
  }
}

export interface QueueStatus {
  eventId: number;
  inQueue: boolean;
  admitted: boolean;
  ahead: number;
  etaSeconds: number;
  pass?: string;
  passExpiresAt?: string;
}

const QUEUE_PASSES = 'ticketrush.queuePasses';

function savedPasses(): Record<string, { pass: string; expiresAt: string }> {
  return JSON.parse(sessionStorage.getItem(QUEUE_PASSES) ?? '{}') as Record<string, { pass: string; expiresAt: string }>;
}

function rememberPass(status: QueueStatus): QueueStatus {
  if (status.admitted && status.pass && status.passExpiresAt) {
    const saved = savedPasses();
    saved[status.eventId] = { pass: status.pass, expiresAt: status.passExpiresAt };
    sessionStorage.setItem(QUEUE_PASSES, JSON.stringify(saved));
  }
  return status;
}

export interface TicketView {
  code: string;
  qrPayload: string;
  orderCode: string;
  userLogin: string;
  eventId: number;
  ticketTypeId: number;
  seatNo: number;
  seatsOfType: number;
  status: TicketPass['status'];
  issuedAt: string;
  checkedInAt?: string;
}

async function toPass(ticket: TicketView, holder: string): Promise<TicketPass> {
  const event = await publicEvent(ticket.eventId);
  return {
    id: ticket.code,
    orderCode: ticket.orderCode,
    eventId: ticket.eventId,
    eventTitle: event?.name ?? `Sự kiện #${ticket.eventId}`,
    eventDate: event?.startTime ?? ticket.issuedAt,
    venueName: event?.venue.name ?? '',
    venueAddress: event ? `${event.venue.address}, ${event.venue.city}` : '',
    ticketTypeName: event?.ticketTypes.find(type => type.id === ticket.ticketTypeId)?.name ?? `Loại vé #${ticket.ticketTypeId}`,
    seatInfo: `Vé ${ticket.seatNo}/${ticket.seatsOfType}`,
    qrPayload: ticket.qrPayload,
    customerName: holder,
    status: ticket.status,
    checkInAt: ticket.checkedInAt ? new Date(ticket.checkedInAt).toLocaleString('vi-VN') : undefined,
  };
}
