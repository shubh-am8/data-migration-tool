-- Legacy schemas (pre test_source/test_destination model)
DROP SCHEMA IF EXISTS app CASCADE;
DROP SCHEMA IF EXISTS test CASCADE;

CREATE SCHEMA IF NOT EXISTS test_source;
CREATE SCHEMA IF NOT EXISTS test_destination;

CREATE TABLE IF NOT EXISTS test_source.orders_cold (
  id BIGSERIAL PRIMARY KEY,
  order_code TEXT NOT NULL UNIQUE,
  amount_cents INT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS test_source.orders_hot_cold (
  id BIGSERIAL PRIMARY KEY,
  order_code TEXT NOT NULL UNIQUE,
  amount_cents INT NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL
);
