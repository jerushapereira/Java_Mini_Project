CREATE TABLE transactions (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  type TEXT NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
  amount NUMERIC(14,2) NOT NULL CHECK (amount > 0),
  category TEXT,
  description TEXT NOT NULL DEFAULT '',
  transaction_date DATE NOT NULL,
  payment_mode TEXT,
  income_source TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK ((type = 'EXPENSE' AND category IN ('Food','Travel','Shopping','Education','Entertainment') AND payment_mode IS NOT NULL AND income_source IS NULL) OR (type = 'INCOME' AND category IS NULL AND income_source IS NOT NULL AND payment_mode IS NULL))
);
CREATE INDEX transactions_date_type_idx ON transactions (transaction_date DESC, type);
CREATE INDEX transactions_category_date_idx ON transactions (category, transaction_date DESC);

CREATE TABLE budgets (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  category TEXT NOT NULL CHECK (category IN ('Food','Travel','Shopping','Education','Entertainment')),
  monthly_limit NUMERIC(14,2) NOT NULL CHECK (monthly_limit > 0),
  month SMALLINT NOT NULL CHECK (month BETWEEN 1 AND 12),
  year SMALLINT NOT NULL CHECK (year BETWEEN 2000 AND 2200),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (category, month, year)
);

CREATE TABLE activity (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  action TEXT NOT NULL,
  description TEXT NOT NULL,
  transaction_id BIGINT REFERENCES transactions(id) ON DELETE SET NULL,
  budget_id BIGINT REFERENCES budgets(id) ON DELETE SET NULL,
  related_id BIGINT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX activity_created_at_idx ON activity (created_at DESC);
