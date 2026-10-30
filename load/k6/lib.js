// Shared setup for the TicketRush load tests. Runs inside the compose network (see load/run.sh), so tokens are issued
// by http://keycloak:9080 like the services expect, and the gateway is reached by its service name.
import http from 'k6/http';
import { check, fail, sleep } from 'k6';

export const GATEWAY = __ENV.GATEWAY || 'http://gateway:8080';
const KEYCLOAK = __ENV.KEYCLOAK || 'http://keycloak:9080';

export function serviceToken() {
  const res = http.post(`${KEYCLOAK}/realms/jhipster/protocol/openid-connect/token`, {
    grant_type: 'client_credentials',
    client_id: 'internal',
    client_secret: __ENV.KEYCLOAK_INTERNAL_CLIENT_SECRET,
    scope: 'openid',
  });
  if (res.status !== 200) {
    fail(`token request failed: ${res.status} ${res.body}`);
  }
  return res.json('access_token');
}

export function headers(token, extra = {}) {
  return { headers: Object.assign({ Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }, extra) };
}

function post(token, path, body) {
  const res = http.post(`${GATEWAY}${path}`, JSON.stringify(body), headers(token));
  if (res.status !== 201) {
    fail(`POST ${path} failed: ${res.status} ${res.body}`);
  }
  return res.json();
}

export function stockOf(token, ticketTypeId) {
  const res = http.get(`${GATEWAY}/services/inventory/api/ticket-stocks?size=2000`, headers(token));
  if (res.status !== 200) {
    return null;
  }
  return res.json().find(stock => stock.ticketTypeId === ticketTypeId) || null;
}

/** Creates an event with one ticket type of `seats` seats and waits until inventory has its stock (via Kafka). */
export function createOnSale(token, seats) {
  const run = `${Date.now()}`;
  const venue = post(token, '/services/catalog/api/venues', { name: `Load ${run}`, address: 'Me Tri', city: 'Ha Noi', capacity: seats });
  const event = post(token, '/services/catalog/api/events', {
    name: `Load test ${run}`,
    slug: `load-${run}`,
    startTime: '2031-12-31T12:00:00Z',
    status: 'PUBLISHED',
    venue: { id: venue.id },
  });
  const ticketType = post(token, '/services/catalog/api/ticket-types', {
    name: 'GA',
    price: 500000,
    currency: 'VND',
    totalQuantity: seats,
    event: { id: event.id },
  });
  for (let i = 0; i < 60; i++) {
    const stock = stockOf(token, ticketType.id);
    if (stock && stock.availableQuantity === seats) {
      return { eventId: event.id, ticketTypeId: ticketType.id };
    }
    sleep(1);
  }
  fail(`stock for ticket type ${ticketType.id} never appeared in inventory`);
}

export function expectAvailable(token, ticketTypeId, expected) {
  const stock = stockOf(token, ticketTypeId);
  check(stock, { [`inventory has ${expected} seats left`]: s => s !== null && s.availableQuantity === expected });
  return stock;
}
