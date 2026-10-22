import test from 'node:test';
import assert from 'node:assert/strict';
import { CurrencyService } from '../src/currency.js';

const rateResponse = (body, status = 200) => ({ ok: status >= 200 && status < 300, status, json: async () => body });

test('converts through provider data and identifies cached responses', async () => {
  let calls = 0;
  const service = new CurrencyService({ fetchImpl: async () => { calls += 1; return rateResponse({ date: '2026-10-21', base: 'INR', quote: 'USD', rate: 0.011 }); } });
  const first = await service.convert('1000.00', 'inr', 'usd');
  const second = await service.convert('1000.00', 'INR', 'USD');
  assert.equal(first.convertedAmount, '11.00');
  assert.equal(first.cached, false);
  assert.equal(second.cached, true);
  assert.equal(calls, 1);
});

test('does not invent rates when provider fails without cache', async () => {
  const service = new CurrencyService({ fetchImpl: async () => { throw new Error('offline'); } });
  await assert.rejects(service.convert('10', 'INR', 'USD'), { status: 503 });
});

test('rejects malformed currency codes locally', async () => {
  const service = new CurrencyService();
  await assert.rejects(service.convert('1', 'IN', 'USD'), { status: 400 });
});

test('reports malformed provider payloads as unavailable', async () => {
  const service = new CurrencyService({ fetchImpl: async () => rateResponse({ date: 'bad', base: 'INR', quote: 'USD', rate: 1 }) });
  await assert.rejects(service.convert('1', 'INR', 'USD'), { status: 503 });
});

test('requests historical base rates from the documented rates endpoint', async () => {
  let requestedUrl;
  const service = new CurrencyService({ fetchImpl: async (url) => {
    requestedUrl = String(url);
    return rateResponse([{ date: '2026-10-21', base: 'INR', quote: 'USD', rate: 0.011 }]);
  } });
  const result = await service.getRates('inr', '2026-10-21');
  assert.equal(new URL(requestedUrl).pathname, '/v2/rates');
  assert.equal(new URL(requestedUrl).searchParams.get('base'), 'INR');
  assert.equal(new URL(requestedUrl).searchParams.get('date'), '2026-10-21');
  assert.equal(result.rates[0].rate, 0.011);
});
