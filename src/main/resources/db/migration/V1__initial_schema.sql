CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(80) NOT NULL UNIQUE
);

INSERT INTO categories (name) VALUES
    ('Food'), ('Travel'), ('Shopping'), ('Education'), ('Entertainment');

CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(12) NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    amount NUMERIC(14, 2) NOT NULL CHECK (amount > 0),
    category_id BIGINT REFERENCES categories(id) ON DELETE RESTRICT,
    description VARCHAR(300) NOT NULL DEFAULT '',
    transaction_date DATE NOT NULL,
    payment_mode VARCHAR(80),
    income_source VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK ((type = 'EXPENSE' AND category_id IS NOT NULL AND payment_mode IS NOT NULL) OR
           (type = 'INCOME' AND category_id IS NULL AND income_source IS NOT NULL))
);

CREATE INDEX transactions_date_id_idx ON transactions (transaction_date DESC, id DESC);
CREATE INDEX transactions_category_date_idx ON transactions (category_id, transaction_date);
CREATE INDEX transactions_type_date_idx ON transactions (type, transaction_date);

CREATE TABLE budgets (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
    monthly_limit NUMERIC(14, 2) NOT NULL CHECK (monthly_limit > 0),
    month INTEGER NOT NULL CHECK (month BETWEEN 1 AND 12),
    year INTEGER NOT NULL CHECK (year BETWEEN 2000 AND 2200),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT budgets_category_period_uq UNIQUE (category_id, month, year)
);

CREATE TABLE activity (
    id BIGSERIAL PRIMARY KEY,
    action VARCHAR(40) NOT NULL,
    description VARCHAR(300) NOT NULL,
    related_type VARCHAR(20),
    related_id BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX activity_created_at_idx ON activity (created_at DESC, id DESC);
