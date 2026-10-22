import test from 'node:test';
import assert from 'node:assert/strict';
import { compareMoney, multiplyDecimal, percentageUsed, subtractMoney, toMoney } from '../src/money.js';

test('decimal multiplication rounds to cents without binary float arithmetic', () => {
  assert.equal(multiplyDecimal('1234.56', '0.92345'), '1140.05');
  assert.equal(multiplyDecimal('0.01', '0.5'), '0.01');
});

test('money normalization rounds half up', () => {
  assert.equal(toMoney('12.345'), '12.35');
  assert.equal(toMoney('9'), '9.00');
});

test('money totals and budget percentages use decimal arithmetic', () => {
  assert.equal(subtractMoney('100000000000.01', '99999999999.99'), '0.02');
  assert.equal(compareMoney('10.00', '9.99'), 1);
  assert.equal(percentageUsed('80.00', '100.00'), 80);
});
