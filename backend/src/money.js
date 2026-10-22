const DECIMAL_PATTERN = /^-?\d+(?:\.\d+)?$/;

export function decimalParts(value) {
  const source = String(value);
  if (!DECIMAL_PATTERN.test(source)) throw new TypeError('Invalid decimal value');
  const negative = source.startsWith('-');
  const unsigned = negative ? source.slice(1) : source;
  const [whole, fraction = ''] = unsigned.split('.');
  return { negative, digits: BigInt(`${whole}${fraction}`), scale: fraction.length };
}

export function multiplyDecimal(amount, rate, places = 2) {
  const left = decimalParts(amount);
  const right = decimalParts(rate);
  const product = left.digits * right.digits;
  const scale = left.scale + right.scale;
  const divisor = 10n ** BigInt(Math.max(0, scale - places));
  const rounded = scale > places ? (product + divisor / 2n) / divisor : product * (10n ** BigInt(places - scale));
  const padded = rounded.toString().padStart(places + 1, '0');
  const sign = left.negative !== right.negative && rounded !== 0n ? '-' : '';
  return `${sign}${padded.slice(0, -places)}.${padded.slice(-places)}`;
}

export function toMoney(value) {
  const parsed = decimalParts(value);
  const normalized = parsed.scale <= 2
    ? parsed.digits * 10n ** BigInt(2 - parsed.scale)
    : (parsed.digits + 10n ** BigInt(parsed.scale - 3) * 5n) / 10n ** BigInt(parsed.scale - 2);
  const padded = normalized.toString().padStart(3, '0');
  return `${parsed.negative && normalized !== 0n ? '-' : ''}${padded.slice(0, -2)}.${padded.slice(-2)}`;
}

function cents(value) {
  const parsed = decimalParts(value);
  const scaled = parsed.scale <= 2
    ? parsed.digits * 10n ** BigInt(2 - parsed.scale)
    : (parsed.digits + 10n ** BigInt(parsed.scale - 3) * 5n) / 10n ** BigInt(parsed.scale - 2);
  return parsed.negative ? -scaled : scaled;
}

function fromCents(value) {
  const negative = value < 0n;
  const digits = (negative ? -value : value).toString().padStart(3, '0');
  return `${negative ? '-' : ''}${digits.slice(0, -2)}.${digits.slice(-2)}`;
}

export function subtractMoney(left, right) {
  return fromCents(cents(left) - cents(right));
}

export function compareMoney(left, right) {
  const a = cents(left), b = cents(right);
  return a < b ? -1 : a > b ? 1 : 0;
}

export function percentageUsed(spent, limit) {
  const numerator = cents(spent) * 10000n;
  const denominator = cents(limit);
  if (denominator <= 0n) return 0;
  const rounded = (numerator + denominator / 2n) / denominator;
  return Number(rounded) / 100;
}
