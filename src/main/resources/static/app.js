const money = (value) => {
  const source = String(value ?? '0');
  const [whole = '0', fraction = ''] = source.split('.');
  const negative = whole.startsWith('-');
  const rupees = BigInt(negative ? whole.slice(1) || '0' : whole || '0');
  const formatted = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(rupees);
  return `${negative ? '-' : ''}${formatted}.${fraction.padEnd(2, '0').slice(0, 2)}`;
};
const escapeHtml = (value) => String(value ?? '').replace(/[&<>"']/g, (char) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[char]);
const now = new Date();
const month = now.getMonth() + 1;
const year = now.getFullYear();
const monthName = now.toLocaleDateString('en-IN', { month: 'long', year: 'numeric' });
let transactionPage = 1;
let transactionQuery = '';
const transactionPageSize = 20;
document.querySelector('#period-label').textContent = monthName;
const form = document.querySelector('#transaction-form');
form.elements.transactionDate.value = new Date().toISOString().slice(0, 10);

async function api(path, options = {}) {
  const response = await fetch(`/api${path}`, { ...options, headers: { 'Content-Type': 'application/json', ...options.headers } });
  if (response.status === 204) return null;
  const body = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(body.error || body.detail || `Request failed (${response.status})`);
  return body;
}
function notice(message, isError = false) {
  const box = document.querySelector('#notice'); box.textContent = message; box.hidden = false; box.classList.toggle('error', isError);
  window.clearTimeout(notice.timer); notice.timer = window.setTimeout(() => { box.hidden = true; }, 5000);
}
async function loadDashboard() {
  try {
    const params = `?month=${month}&year=${year}`;
    const [summary, transactions, budgets, categories, activity] = await Promise.all([
      api(`/dashboard/summary${params}`), api(`/transactions?page=${transactionPage}&pageSize=${transactionPageSize}&q=${encodeURIComponent(transactionQuery)}`), api(`/budgets${params}`), api(`/dashboard/category-spending${params}`), api('/activity?limit=8')
    ]);
    document.querySelector('#income-total').textContent = money(summary.totalIncome);
    document.querySelector('#expense-total').textContent = money(summary.totalExpenses);
    document.querySelector('#balance-total').textContent = money(summary.balance);
    document.querySelector('#transaction-count').textContent = summary.transactionCount;
    drawTransactions(transactions);
    drawBudgets(budgets);
    drawCategories(categories);
    drawActivity(activity);
  } catch (error) { notice(error.message, true); }
}
function drawTransactions(listing) {
  const items = listing.items;
  const body = document.querySelector('#transaction-rows');
  document.querySelector('#transaction-empty').hidden = items.length > 0;
  document.querySelector('#page-status').textContent = `${listing.total ? (listing.page - 1) * listing.pageSize + 1 : 0} to ${Math.min(listing.page * listing.pageSize, listing.total)} of ${listing.total}`;
  document.querySelector('#previous-page').disabled = listing.page <= 1;
  document.querySelector('#next-page').disabled = listing.page * listing.pageSize >= listing.total;
  body.innerHTML = items.map((item) => `<tr><td>${escapeHtml(item.transactionDate?.slice(0, 10))}</td><td>${escapeHtml(item.description || item.incomeSource || item.category)}</td><td>${escapeHtml(item.category || item.incomeSource || 'Uncategorized')}</td><td>${escapeHtml(item.type)}</td><td class="amount ${item.type === 'INCOME' ? 'income' : 'expense'}">${item.type === 'INCOME' ? '+' : '−'}${money(item.amount)}</td><td><div class="row-actions"><button data-edit="${item.id}" type="button">Edit</button><button class="delete" data-delete="${item.id}" type="button">Delete</button></div></td></tr>`).join('');
  body.querySelectorAll('[data-edit]').forEach((button) => button.addEventListener('click', () => editTransaction(items.find((item) => String(item.id) === button.dataset.edit))));
  body.querySelectorAll('[data-delete]').forEach((button) => button.addEventListener('click', () => deleteTransaction(button.dataset.delete)));
}
function drawBudgets(items) {
  const list = document.querySelector('#budget-list');
  document.querySelector('#budget-empty').hidden = items.length > 0;
  list.innerHTML = items.map((item) => `<div class="budget-row"><div class="budget-top"><strong>${escapeHtml(item.category)}</strong><span>${money(item.spent)} of ${money(item.monthlyLimit)}</span></div><div class="track"><span class="${item.warning || item.exceeded ? 'warn' : ''}" style="width:${Math.min(100, item.percentUsed)}%"></span></div><span class="budget-note">${item.exceeded ? `Over by ${money(String(item.remaining).replace('-', ''))}` : `${money(item.remaining)} remaining`} · ${item.percentUsed}% used</span></div>`).join('');
}
function drawCategories(items) {
  const list = document.querySelector('#category-list'), max = Math.max(1, ...items.map((item) => Number(item.amount)));
  document.querySelector('#category-empty').hidden = items.length > 0;
  list.innerHTML = items.map((item) => `<div class="category-item"><span>${escapeHtml(item.category)}</span><div class="category-bar"><span style="width:${Number(item.amount) / max * 100}%"></span></div><span class="category-value">${money(item.amount)}</span></div>`).join('');
}
function drawActivity(items) {
  const list = document.querySelector('#activity-list');
  document.querySelector('#activity-empty').hidden = items.length > 0;
  list.innerHTML = items.map((item) => `<li>${escapeHtml(item.description)}<time>${new Date(item.createdAt).toLocaleString()}</time></li>`).join('');
}
function setTransactionType() {
  const income = form.elements.type.value === 'INCOME';
  document.querySelector('#payment-field').hidden = income;
  document.querySelector('#source-field').hidden = !income;
  form.elements.paymentMode.required = !income;
  form.elements.incomeSource.required = income;
  document.querySelector('#category').disabled = income;
}
document.querySelector('#type').addEventListener('change', setTransactionType);
setTransactionType();
form.addEventListener('submit', async (event) => {
  event.preventDefault();
  const data = Object.fromEntries(new FormData(form));
  try {
    const response = await api('/transactions', { method: 'POST', body: JSON.stringify(data) });
    form.reset(); form.elements.transactionDate.value = new Date().toISOString().slice(0, 10); setTransactionType();
    notice(`Transaction ${response.id} saved.`); await loadDashboard();
  } catch (error) { notice(error.message, true); }
});
async function editTransaction(item) {
  const amount = window.prompt('Enter the updated amount in INR', item.amount);
  if (amount === null) return;
  try {
    const payload = { type: item.type, amount, category: item.category, description: item.description, transactionDate: item.transactionDate.slice(0, 10), paymentMode: item.paymentMode, incomeSource: item.incomeSource };
    await api(`/transactions/${item.id}`, { method: 'PUT', body: JSON.stringify(payload) });
    notice('Transaction updated.'); await loadDashboard();
  } catch (error) { notice(error.message, true); }
}
async function deleteTransaction(id) {
  if (!window.confirm('Delete this transaction?')) return;
  try { await api(`/transactions/${id}`, { method: 'DELETE' }); notice('Transaction deleted.'); await loadDashboard(); }
  catch (error) { notice(error.message, true); }
}
document.querySelector('#budget-toggle').addEventListener('click', () => { document.querySelector('#budget-form').hidden = !document.querySelector('#budget-form').hidden; });
document.querySelector('#budget-form').addEventListener('submit', async (event) => {
  event.preventDefault(); const data = Object.fromEntries(new FormData(event.currentTarget));
  try { await api('/budgets', { method: 'POST', body: JSON.stringify({ ...data, month, year }) }); event.currentTarget.reset(); event.currentTarget.hidden = true; notice('Monthly budget saved.'); await loadDashboard(); }
  catch (error) { notice(error.message, true); }
});
document.querySelector('#currency-form').addEventListener('submit', async (event) => {
  event.preventDefault(); const data = new FormData(event.currentTarget); const result = document.querySelector('#currency-result'); result.textContent = 'Retrieving the current published rate…';
  try { const converted = await api(`/currency/convert?amount=${encodeURIComponent(data.get('amount'))}&from=${encodeURIComponent(data.get('from'))}&to=${encodeURIComponent(data.get('to'))}`); result.textContent = `${converted.amount} ${converted.from} = ${converted.convertedAmount} ${converted.to} · rate ${converted.rate} for ${converted.rateDate} · ${converted.source}${converted.cached ? ' · cached' : ' · retrieved now'}`; }
  catch (error) { result.textContent = error.message; }
});
document.querySelector('#refresh').addEventListener('click', loadDashboard);
document.querySelector('#previous-page').addEventListener('click', () => { transactionPage = Math.max(1, transactionPage - 1); loadDashboard(); });
document.querySelector('#next-page').addEventListener('click', () => { transactionPage += 1; loadDashboard(); });
document.querySelector('#search').addEventListener('input', (event) => {
  transactionQuery = event.target.value.trim(); transactionPage = 1;
  window.clearTimeout(loadDashboard.searchTimer);
  loadDashboard.searchTimer = window.setTimeout(loadDashboard, 250);
});
loadDashboard();
