import express from 'express';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { CurrencyService } from './currency.js';
import { compareMoney, percentageUsed, subtractMoney, toMoney } from './money.js';

const categories = ['Food', 'Travel', 'Shopping', 'Education', 'Entertainment'];
const types = ['INCOME', 'EXPENSE'];
const appDirectory = path.dirname(fileURLToPath(import.meta.url));

export function createApp({ pool, currencyService = new CurrencyService(), frontendOrigin = 'http://localhost:3000' }) {
  const app = express();
  app.use(express.json({ limit: '64kb' }));
  app.use((req, res, next) => {
    const origin = req.headers.origin;
    if (!origin || origin === frontendOrigin) {
      if (origin) res.setHeader('Access-Control-Allow-Origin', origin);
      res.setHeader('Vary', 'Origin');
      res.setHeader('Access-Control-Allow-Methods', 'GET,POST,PUT,DELETE,OPTIONS');
      res.setHeader('Access-Control-Allow-Headers', 'Content-Type');
      if (req.method === 'OPTIONS') return res.sendStatus(204);
    }
    next();
  });

  const asyncRoute = (handler) => (req, res, next) => Promise.resolve(handler(req, res, next)).catch(next);
  const badRequest = (message) => Object.assign(new Error(message), { status: 400 });
  const notFound = (name) => Object.assign(new Error(`${name} not found`), { status: 404 });
  const validateTransaction = (body) => {
    const type = String(body.type ?? '').toUpperCase();
    const amount = String(body.amount ?? '');
    const category = body.category == null ? null : String(body.category);
    if (!types.includes(type)) throw badRequest('type must be INCOME or EXPENSE');
    if (!/^\d{1,12}(?:\.\d{1,2})?$/.test(amount) || Number(amount) <= 0) throw badRequest('amount must be positive with at most 2 decimal places');
    if (type === 'EXPENSE' && !categories.includes(category)) throw badRequest(`category must be one of: ${categories.join(', ')}`);
    if (type === 'INCOME' && !String(body.incomeSource ?? '').trim()) throw badRequest('incomeSource is required for income');
    if (type === 'EXPENSE' && !String(body.paymentMode ?? '').trim()) throw badRequest('paymentMode is required for expenses');
    if (!isValidDateOnly(body.transactionDate)) throw badRequest('transactionDate must be a valid YYYY-MM-DD date');
    return {
      type, amount: toMoney(amount), category: type === 'EXPENSE' ? category : null,
      description: String(body.description ?? '').trim().slice(0, 300), transactionDate: body.transactionDate,
      paymentMode: type === 'EXPENSE' ? String(body.paymentMode).trim().slice(0, 80) : null,
      incomeSource: type === 'INCOME' ? String(body.incomeSource).trim().slice(0, 120) : null
    };
  };
  const writeActivity = async (client, action, description, transactionId = null, budgetId = null, relatedId = transactionId ?? budgetId) => {
    await client.query('INSERT INTO activity (action, description, transaction_id, budget_id, related_id) VALUES ($1,$2,$3,$4,$5)', [action, description, transactionId, budgetId, relatedId]);
  };
  const withClientTransaction = async (callback) => {
    const client = await pool.connect();
    try { await client.query('BEGIN'); const result = await callback(client); await client.query('COMMIT'); return result; }
    catch (error) { await client.query('ROLLBACK'); throw error; }
    finally { client.release(); }
  };

  app.get('/api/health', asyncRoute(async (_req, res) => { await pool.query('SELECT 1'); res.json({ status: 'ok', database: 'connected' }); }));

  app.get('/api/transactions', asyncRoute(async (req, res) => {
    const page = Number(req.query.page ?? 1), pageSize = Number(req.query.pageSize ?? 20);
    if (!Number.isInteger(page) || page < 1 || !Number.isInteger(pageSize) || pageSize < 1 || pageSize > 100) throw badRequest('page must be positive and pageSize must be between 1 and 100');
    const filters = [], values = [];
    const add = (sql, value) => { values.push(value); filters.push(sql.replace('?', `$${values.length}`)); };
    if (req.query.type) { const value = String(req.query.type).toUpperCase(); if (!types.includes(value)) throw badRequest('Invalid transaction type'); add('type = ?', value); }
    if (req.query.category) add('category = ?', String(req.query.category));
    for (const dateValue of [req.query.from, req.query.to]) {
      if (dateValue && !isValidDateOnly(dateValue)) throw badRequest('Dates must use a valid YYYY-MM-DD date');
    }
    if (req.query.from) add('transaction_date >= ?::date', req.query.from);
    if (req.query.to) add('transaction_date <= ?::date', req.query.to);
    if (Boolean(req.query.month) !== Boolean(req.query.year)) throw badRequest('month and year must be provided together');
    if (req.query.month && req.query.year) {
      if (Number(req.query.month) < 1 || Number(req.query.month) > 12 || !Number.isInteger(Number(req.query.year))) throw badRequest('Invalid month or year');
      values.push(Number(req.query.year), Number(req.query.month));
      filters.push("date_trunc('month', transaction_date) = make_date($" + (values.length - 1) + ', $' + values.length + ', 1)');
    }
    if (req.query.incomeSource) add('income_source = ?', req.query.incomeSource);
    if (req.query.q) {
      values.push(req.query.q, req.query.q, req.query.q);
      const index = values.length - 2;
      filters.push(`(description ILIKE '%' || $${index} || '%' OR category ILIKE '%' || $${index + 1} || '%' OR income_source ILIKE '%' || $${index + 2} || '%')`);
    }
    if (req.query.paymentMode) add('payment_mode = ?', req.query.paymentMode);
    const where = filters.length ? `WHERE ${filters.join(' AND ')}` : '';
    const count = await pool.query(`SELECT count(*)::int AS total FROM transactions ${where}`, values);
    const rows = await pool.query(`SELECT * FROM transactions ${where} ORDER BY transaction_date DESC, id DESC LIMIT $${values.length + 1} OFFSET $${values.length + 2}`, [...values, pageSize, (page - 1) * pageSize]);
    res.json({ items: rows.rows, page, pageSize, total: count.rows[0].total });
  }));

  app.get('/api/transactions/:id', asyncRoute(async (req, res) => {
    const result = await pool.query('SELECT * FROM transactions WHERE id = $1', [req.params.id]);
    if (!result.rowCount) throw notFound('Transaction');
    res.json(result.rows[0]);
  }));
  app.post('/api/transactions', asyncRoute(async (req, res) => {
    const input = validateTransaction(req.body);
    const row = await withClientTransaction(async (client) => {
      const result = await client.query('INSERT INTO transactions (type, amount, category, description, transaction_date, payment_mode, income_source) VALUES ($1,$2,$3,$4,$5,$6,$7) RETURNING *', [input.type, input.amount, input.category, input.description, input.transactionDate, input.paymentMode, input.incomeSource]);
      await writeActivity(client, input.type === 'INCOME' ? 'INCOME_ADDED' : 'EXPENSE_ADDED', `${input.type === 'INCOME' ? 'Income' : 'Expense'} recorded: ${input.description || input.category || input.incomeSource}`, result.rows[0].id);
      return result.rows[0];
    });
    res.status(201).json(row);
  }));
  app.put('/api/transactions/:id', asyncRoute(async (req, res) => {
    const input = validateTransaction(req.body);
    const row = await withClientTransaction(async (client) => {
      const result = await client.query('UPDATE transactions SET type=$1, amount=$2, category=$3, description=$4, transaction_date=$5, payment_mode=$6, income_source=$7, updated_at=now() WHERE id=$8 RETURNING *', [input.type, input.amount, input.category, input.description, input.transactionDate, input.paymentMode, input.incomeSource, req.params.id]);
      if (!result.rowCount) throw notFound('Transaction');
      await writeActivity(client, 'TRANSACTION_UPDATED', `Transaction ${result.rows[0].id} updated`, result.rows[0].id);
      return result.rows[0];
    });
    res.json(row);
  }));
  app.delete('/api/transactions/:id', asyncRoute(async (req, res) => {
    await withClientTransaction(async (client) => {
      const result = await client.query('DELETE FROM transactions WHERE id=$1 RETURNING *', [req.params.id]);
      if (!result.rowCount) throw notFound('Transaction');
      await writeActivity(client, 'TRANSACTION_DELETED', `Transaction ${result.rows[0].id} deleted`, null, null, result.rows[0].id);
    });
    res.sendStatus(204);
  }));

  app.get('/api/budgets', asyncRoute(async (req, res) => {
    const month = Number(req.query.month ?? new Date().getMonth() + 1), year = Number(req.query.year ?? new Date().getFullYear());
    if (!Number.isInteger(month) || month < 1 || month > 12 || !Number.isInteger(year)) throw badRequest('Invalid budget period');
    const result = await pool.query("SELECT b.*, COALESCE(sum(t.amount),0)::numeric(14,2) AS spent FROM budgets b LEFT JOIN transactions t ON t.type='EXPENSE' AND t.category=b.category AND date_trunc('month',t.transaction_date)=make_date(b.year,b.month,1) WHERE b.month=$1 AND b.year=$2 GROUP BY b.id ORDER BY b.category", [month, year]);
    res.json(result.rows.map(budgetStatus));
  }));
  app.get('/api/budgets/:id', asyncRoute(async (req, res) => {
    const result = await pool.query("SELECT b.*, COALESCE(sum(t.amount),0)::numeric(14,2) AS spent FROM budgets b LEFT JOIN transactions t ON t.type='EXPENSE' AND t.category=b.category AND date_trunc('month',t.transaction_date)=make_date(b.year,b.month,1) WHERE b.id=$1 GROUP BY b.id", [req.params.id]);
    if (!result.rowCount) throw notFound('Budget');
    res.json(budgetStatus(result.rows[0]));
  }));
  app.post('/api/budgets', asyncRoute(async (req, res) => {
    const input = validateBudget(req.body, badRequest);
    const row = await withClientTransaction(async (client) => {
      const result = await client.query('INSERT INTO budgets (category, monthly_limit, month, year) VALUES ($1,$2,$3,$4) RETURNING *', [input.category, input.monthlyLimit, input.month, input.year]);
      await writeActivity(client, 'BUDGET_CREATED', `Budget created for ${input.category}, ${input.year}-${String(input.month).padStart(2, '0')}`, null, result.rows[0].id);
      return await getBudgetStatus(client, result.rows[0]);
    });
    res.status(201).json(row);
  }));
  app.put('/api/budgets/:id', asyncRoute(async (req, res) => {
    const input = validateBudget(req.body, badRequest);
    const row = await withClientTransaction(async (client) => {
      const result = await client.query('UPDATE budgets SET category=$1, monthly_limit=$2, month=$3, year=$4, updated_at=now() WHERE id=$5 RETURNING *', [input.category, input.monthlyLimit, input.month, input.year, req.params.id]);
      if (!result.rowCount) throw notFound('Budget');
      await writeActivity(client, 'BUDGET_UPDATED', `Budget ${result.rows[0].id} updated`, null, result.rows[0].id);
      return await getBudgetStatus(client, result.rows[0]);
    });
    res.json(row);
  }));
  app.delete('/api/budgets/:id', asyncRoute(async (req, res) => {
    await withClientTransaction(async (client) => {
      const result = await client.query('DELETE FROM budgets WHERE id=$1 RETURNING *', [req.params.id]);
      if (!result.rowCount) throw notFound('Budget');
      await writeActivity(client, 'BUDGET_DELETED', `Budget ${result.rows[0].id} deleted`, null, null, result.rows[0].id);
    });
    res.sendStatus(204);
  }));

  app.get('/api/dashboard/summary', asyncRoute(async (req, res) => {
    const { month, year } = dashboardPeriod(req.query, badRequest);
    const totals = await pool.query("SELECT COALESCE(sum(amount) FILTER (WHERE type='INCOME'),0)::numeric(14,2) AS income, COALESCE(sum(amount) FILTER (WHERE type='EXPENSE'),0)::numeric(14,2) AS expenses, (COALESCE(sum(amount) FILTER (WHERE type='INCOME'),0) - COALESCE(sum(amount) FILTER (WHERE type='EXPENSE'),0))::numeric(14,2) AS balance, count(*)::int AS count FROM transactions WHERE date_trunc('month', transaction_date)=make_date($1,$2,1)", [year, month]);
    const row = totals.rows[0];
    const cats = await pool.query("SELECT category, sum(amount)::numeric(14,2) AS amount FROM transactions WHERE type='EXPENSE' AND date_trunc('month',transaction_date)=make_date($1,$2,1) GROUP BY category ORDER BY sum(amount) DESC", [year, month]);
    res.json({ month, year, totalIncome: row.income, totalExpenses: row.expenses, balance: row.balance, transactionCount: row.count, categorySpending: cats.rows, highestSpendingCategory: cats.rows[0]?.category ?? null });
  }));
  app.get('/api/dashboard/category-spending', asyncRoute(async (req, res) => {
    const { month, year } = dashboardPeriod(req.query, badRequest);
    const result = await pool.query("SELECT category, sum(amount)::numeric(14,2) AS amount FROM transactions WHERE type='EXPENSE' AND date_trunc('month', transaction_date)=make_date($1,$2,1) GROUP BY category ORDER BY amount DESC", [year, month]);
    res.json(result.rows);
  }));
  app.get('/api/dashboard/monthly-summary', asyncRoute(async (_req, res) => {
    const result = await pool.query("SELECT extract(year from transaction_date)::int AS year, extract(month from transaction_date)::int AS month, COALESCE(sum(amount) FILTER (WHERE type='INCOME'),0)::numeric(14,2) AS income, COALESCE(sum(amount) FILTER (WHERE type='EXPENSE'),0)::numeric(14,2) AS expenses FROM transactions GROUP BY 1,2 ORDER BY 1 DESC,2 DESC LIMIT 12");
    res.json(result.rows);
  }));
  app.get('/api/dashboard/budget-status', asyncRoute(async (req, res) => {
    const { month, year } = dashboardPeriod(req.query, badRequest);
    const result = await pool.query("SELECT b.*, COALESCE(sum(t.amount),0)::numeric(14,2) AS spent FROM budgets b LEFT JOIN transactions t ON t.type='EXPENSE' AND t.category=b.category AND date_trunc('month',t.transaction_date)=make_date(b.year,b.month,1) WHERE b.month=$1 AND b.year=$2 GROUP BY b.id ORDER BY b.category", [month, year]);
    res.json(result.rows.map(budgetStatus));
  }));
  app.get('/api/dashboard/recent-transactions', asyncRoute(async (_req, res) => {
    const result = await pool.query('SELECT * FROM transactions ORDER BY transaction_date DESC, id DESC LIMIT 10');
    res.json(result.rows);
  }));

  app.get('/api/activity', asyncRoute(async (req, res) => {
    const limit = Number(req.query.limit ?? 50);
    if (!Number.isInteger(limit) || limit < 1) throw badRequest('limit must be a positive integer');
    const result = await pool.query('SELECT * FROM activity ORDER BY created_at DESC, id DESC LIMIT $1', [Math.min(100, limit)]);
    res.json(result.rows);
  }));
  app.get('/api/activity/:id', asyncRoute(async (req, res) => {
    const result = await pool.query('SELECT * FROM activity WHERE id=$1', [req.params.id]);
    if (!result.rowCount) throw notFound('Activity');
    res.json(result.rows[0]);
  }));
  app.get('/api/currency/rates', asyncRoute(async (req, res) => res.json(await currencyService.getRates(req.query.base ?? 'INR', req.query.date))));
  app.get('/api/currency/convert', asyncRoute(async (req, res) => res.json(await currencyService.convert(req.query.amount, req.query.from ?? 'INR', req.query.to ?? 'USD', req.query.date))));

  app.use(express.static(path.resolve(appDirectory, '../../frontend')));
  app.use((req, res) => res.status(404).json({ error: 'Not found' }));
  app.use((error, _req, res, _next) => {
    if (error.code === '23505') return res.status(409).json({ error: 'A budget for this category and period already exists' });
    if (error.code === '23503') return res.status(409).json({ error: 'This record is referenced by another record' });
    const status = error.status ?? (error.type === 'entity.parse.failed' ? 400 : 500);
    if (status >= 500) console.error('Request failed:', error.message);
    return res.status(status).json({ error: status >= 500 ? 'The request could not be completed' : error.message });
  });
  return app;
}

function validateBudget(body, badRequest) {
  if (!categories.includes(body.category)) throw badRequest(`category must be one of: ${categories.join(', ')}`);
  const month = Number(body.month), year = Number(body.year), amount = String(body.monthlyLimit ?? '');
  if (!Number.isInteger(month) || month < 1 || month > 12 || !Number.isInteger(year) || year < 2000 || year > 2200) throw badRequest('month and year must identify a valid budget period');
  if (!/^\d{1,12}(?:\.\d{1,2})?$/.test(amount) || Number(amount) <= 0) throw badRequest('monthlyLimit must be positive with at most 2 decimal places');
  return { category: body.category, month, year, monthlyLimit: toMoney(amount) };
}

function dashboardPeriod(query, badRequest) {
  const today = new Date();
  const month = Number(query.month ?? today.getMonth() + 1), year = Number(query.year ?? today.getFullYear());
  if (!Number.isInteger(month) || month < 1 || month > 12 || !Number.isInteger(year)) throw badRequest('Invalid month or year');
  return { month, year };
}

function budgetStatus(row) {
  const spent = String(row.spent ?? '0.00'), limit = String(row.monthly_limit ?? row.monthlyLimit);
  const percentUsed = percentageUsed(spent, limit);
  const exceeded = compareMoney(spent, limit) > 0;
  return { ...row, spent: toMoney(spent), remaining: subtractMoney(limit, spent), percentUsed, warning: percentUsed >= 80 && !exceeded, exceeded };
}

async function getBudgetStatus(client, row) {
  const result = await client.query("SELECT COALESCE(sum(amount),0)::numeric(14,2) AS spent FROM transactions WHERE type='EXPENSE' AND category=$1 AND date_trunc('month',transaction_date)=make_date($2,$3,1)", [row.category, row.year, row.month]);
  return budgetStatus({ ...row, spent: result.rows[0].spent });
}

function isValidDateOnly(value) {
  if (typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
  const [year, month, day] = value.split('-').map(Number);
  const date = new Date(Date.UTC(year, month - 1, day));
  return date.getUTCFullYear() === year && date.getUTCMonth() === month - 1 && date.getUTCDate() === day;
}
