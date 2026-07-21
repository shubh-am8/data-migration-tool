-- Idempotent seed for test_source sample tables (hot/cold playground data)
INSERT INTO test_source.orders_cold (order_code, amount_cents, created_at)
SELECT
  'SEED-COLD-' || g,
  1000 + (g % 500),
  NOW() - INTERVAL '30 days' - (g || ' hours')::INTERVAL
FROM generate_series(1, 200) AS g
ON CONFLICT (order_code) DO NOTHING;

INSERT INTO test_source.orders_hot_cold (order_code, amount_cents, updated_at)
SELECT
  'SEED-HOT-' || g,
  2000 + (g % 300),
  CASE
    WHEN g <= 50 THEN NOW() - (g || ' hours')::INTERVAL
    ELSE NOW() - INTERVAL '14 days' - (g || ' hours')::INTERVAL
  END
FROM generate_series(1, 200) AS g
ON CONFLICT (order_code) DO NOTHING;
