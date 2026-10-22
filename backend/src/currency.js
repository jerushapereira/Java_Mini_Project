import { multiplyDecimal } from './money.js';

const API_ROOT = 'https://api.frankfurter.dev/v2';

export class CurrencyService {
  constructor({ fetchImpl = fetch, now = () => Date.now(), ttlMs = 6 * 60 * 60 * 1000, timeoutMs = 4000 } = {}) {
    this.fetchImpl = fetchImpl;
    this.now = now;
    this.ttlMs = ttlMs;
    this.timeoutMs = timeoutMs;
    this.cache = new Map();
  }

  async getRates(base = 'INR', date) {
    const normalizedBase = this.currencyCode(base);
    const key = `${date ?? 'latest'}:${normalizedBase}`;
    const cached = this.cache.get(key);
    if (cached && cached.expiresAt > this.now()) return { ...cached.value, cached: true };
    const query = new URLSearchParams({ base: normalizedBase });
    if (date) query.set('date', date);
    const url = `${API_ROOT}/rates?${query}`;
    try {
      const response = await this.request(url);
      if (!Array.isArray(response)) throw Object.assign(new Error('Malformed rate list from Frankfurter'), { status: 503 });
      const rows = response.map((row) => this.validateRate(row, normalizedBase));
      const value = { base: normalizedBase, source: 'Frankfurter', rates: rows, cached: false };
      this.cache.set(key, { value, expiresAt: this.now() + this.ttlMs });
      return value;
    } catch (error) {
      if (cached) return { ...cached.value, cached: true, stale: true };
      throw error;
    }
  }

  async convert(amount, from, to, date) {
    if (!/^\d+(?:\.\d{1,8})?$/.test(String(amount)) || Number(amount) <= 0) {
      const error = new Error('amount must be a positive decimal with at most 8 fractional digits');
      error.status = 400;
      throw error;
    }
    const base = this.currencyCode(from);
    const quote = this.currencyCode(to);
    if (base === quote) {
      return { amount: String(amount), from: base, to: quote, rate: '1', convertedAmount: multiplyDecimal(amount, '1'), rateDate: date ?? new Date(this.now()).toISOString().slice(0, 10), source: 'Frankfurter', cached: false };
    }
    const key = `${date ?? 'latest'}:${base}/${quote}`;
    const cached = this.cache.get(key);
    if (cached && cached.expiresAt > this.now()) return this.conversionResult(amount, base, quote, cached.value, true);
    const url = `${API_ROOT}/rate/${base.toLowerCase()}/${quote.toLowerCase()}${date ? `?date=${encodeURIComponent(date)}` : ''}`;
    try {
      const rate = this.validateRate(await this.request(url), base, quote);
      this.cache.set(key, { value: rate, expiresAt: this.now() + this.ttlMs });
      return this.conversionResult(amount, base, quote, rate, false);
    } catch (error) {
      if (cached) return { ...this.conversionResult(amount, base, quote, cached.value, true), stale: true };
      throw error;
    }
  }

  conversionResult(amount, from, to, rate, cached) {
    return { amount: String(amount), from, to, rate: String(rate.rate), convertedAmount: multiplyDecimal(amount, String(rate.rate)), rateDate: rate.date, source: 'Frankfurter', cached };
  }

  currencyCode(value) {
    const code = String(value ?? '').toUpperCase();
    if (!/^[A-Z]{3}$/.test(code)) {
      const error = new Error('Currency must be a three-letter ISO code');
      error.status = 400;
      throw error;
    }
    return code;
  }

  validateRate(row, base, quote) {
    if (!row || row.base !== base || (quote && row.quote !== quote) || typeof row.rate !== 'number' || !Number.isFinite(row.rate) || row.rate <= 0 || !/^\d{4}-\d{2}-\d{2}$/.test(row.date)) {
      throw Object.assign(new Error('Malformed exchange rate from Frankfurter'), { status: 503 });
    }
    return { date: row.date, base: row.base, ...(row.quote ? { quote: row.quote } : {}), rate: row.rate };
  }

  async request(url) {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), this.timeoutMs);
    try {
      const response = await this.fetchImpl(url, { signal: controller.signal, headers: { accept: 'application/json' } });
      if (!response.ok) {
        const error = new Error(response.status === 404 || response.status === 422 ? 'Currency or rate is not supported by Frankfurter' : 'Frankfurter is temporarily unavailable');
        error.status = response.status === 404 || response.status === 422 ? 400 : 503;
        throw error;
      }
      try { return await response.json(); } catch { throw Object.assign(new Error('Malformed JSON from Frankfurter'), { status: 503 }); }
    } catch (cause) {
      if (cause.status) throw cause;
      const error = new Error(cause.name === 'AbortError' ? 'Frankfurter request timed out' : 'Could not reach Frankfurter');
      error.status = 503;
      throw error;
    } finally {
      clearTimeout(timeout);
    }
  }
}
