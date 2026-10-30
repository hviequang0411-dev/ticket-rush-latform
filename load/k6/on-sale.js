// On-sale spike: many buyers race for a limited number of seats at the same moment.
//
// Pass criteria (thresholds): exactly SEATS tickets sold, nothing oversold, no unexpected errors, checkout p95 bound.
// Every request uses one service account, so run with the load-test override that lifts the per-user rate limit
// (load/compose.loadtest.yml); load/run.sh does that.
import http from 'k6/http';
import { check } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import exec from 'k6/execution';
import { GATEWAY, createOnSale, expectAvailable, headers, serviceToken } from './lib.js';

const SEATS = Number(__ENV.SEATS || 500);
const BUYERS = Number(__ENV.BUYERS || 2000);
const VUS = Number(__ENV.VUS || 200);

const sold = new Counter('tickets_sold');
const soldOut = new Counter('sold_out_answers');
const checkoutTime = new Trend('checkout_duration', true);

// 409 (sold out) is a correct answer, not a failure
http.setResponseCallback(http.expectedStatuses(200, 201, 409));

export const options = {
  scenarios: {
    on_sale: {
      executor: 'shared-iterations',
      vus: VUS,
      iterations: BUYERS,
      maxDuration: '5m',
    },
  },
  thresholds: {
    tickets_sold: [`count==${SEATS}`],
    http_req_failed: ['rate<0.01'],
    checks: ['rate>0.99'],
    'checkout_duration{outcome:accepted}': [`p(95)<${__ENV.P95_MS || 1500}`],
  },
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export function setup() {
  const token = serviceToken();
  const sale = createOnSale(token, SEATS);
  return { token, ticketTypeId: sale.ticketTypeId };
}

export default function (data) {
  const key = `load-${exec.scenario.iterationInTest}-${Date.now()}`;
  const res = http.post(
    `${GATEWAY}/services/order/api/checkout`,
    JSON.stringify({ items: [{ ticketTypeId: data.ticketTypeId, quantity: 1 }] }),
    Object.assign(headers(data.token, { 'Idempotency-Key': key }), { tags: { name: 'checkout' } }),
  );
  const outcome = res.status === 201 ? 'accepted' : res.status === 409 ? 'sold_out' : 'error';
  checkoutTime.add(res.timings.duration, { outcome });
  if (outcome === 'accepted') {
    sold.add(1);
  } else if (outcome === 'sold_out') {
    soldOut.add(1);
  }
  check(res, { 'checkout answered 201 or 409': r => r.status === 201 || r.status === 409 });
}

export function teardown(data) {
  // The invariant the whole design is about: every seat sold once, none sold twice
  expectAvailable(serviceToken(), data.ticketTypeId, 0);
}

export function handleSummary(data) {
  const out = __ENV.RESULT_FILE || '/results/on-sale-summary.json';
  return { [out]: JSON.stringify(data, null, 2), stdout: textSummary(data) };
}

function textSummary(data) {
  const m = data.metrics;
  const p = (name, stat) => (m[name] && m[name].values[stat] !== undefined ? m[name].values[stat].toFixed(1) : 'n/a');
  return [
    '',
    `on-sale: ${BUYERS} buyers, ${VUS} concurrent, ${SEATS} seats`,
    `  tickets sold        ${m.tickets_sold ? m.tickets_sold.values.count : 0}`,
    `  sold-out answers    ${m.sold_out_answers ? m.sold_out_answers.values.count : 0}`,
    `  checkout ms (all)   p50 ${p('checkout_duration', 'med')}  p95 ${p('checkout_duration', 'p(95)')}  p99 ${p('checkout_duration', 'p(99)')}`,
    `  throughput          ${p('http_reqs', 'rate')} req/s`,
    `  failed requests     ${(m.http_req_failed.values.rate * 100).toFixed(2)} %`,
    `  thresholds          ${Object.values(m).every(x => !x.thresholds || Object.values(x.thresholds).every(t => t.ok)) ? 'all passed' : 'FAILED'}`,
    '',
  ].join('\n');
}
