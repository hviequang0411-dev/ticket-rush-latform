// Browsing load: event listing and ticket type lookups at a constant arrival rate, the read side of an on-sale.
import http from 'k6/http';
import { check } from 'k6';
import { GATEWAY, createOnSale, headers, serviceToken } from './lib.js';

const RATE = Number(__ENV.RATE || 200);

export const options = {
  scenarios: {
    browse: {
      executor: 'constant-arrival-rate',
      rate: RATE,
      timeUnit: '1s',
      duration: __ENV.DURATION || '2m',
      preAllocatedVUs: 100,
      maxVUs: 400,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    'http_req_duration{name:events}': [`p(95)<${__ENV.P95_MS || 300}`],
    'http_req_duration{name:ticket-type}': [`p(95)<${__ENV.P95_MS || 300}`],
  },
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export function setup() {
  const token = serviceToken();
  return { token, ticketTypeId: createOnSale(token, 100).ticketTypeId };
}

export default function (data) {
  const list = http.get(`${GATEWAY}/services/catalog/api/events?page=0&size=20&status.equals=PUBLISHED`, Object.assign(headers(data.token), { tags: { name: 'events' } }));
  check(list, { 'events listed': r => r.status === 200 });
  const type = http.get(`${GATEWAY}/services/catalog/api/ticket-types/${data.ticketTypeId}`, Object.assign(headers(data.token), { tags: { name: 'ticket-type' } }));
  check(type, { 'ticket type read': r => r.status === 200 });
}

export function handleSummary(data) {
  return { [__ENV.RESULT_FILE || '/results/browse-summary.json']: JSON.stringify(data, null, 2) };
}
