SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE purchases;
TRUNCATE TABLE payments;
TRUNCATE TABLE holds;
TRUNCATE TABLE seances;
TRUNCATE TABLE event_performers;
TRUNCATE TABLE performers;
TRUNCATE TABLE events;

SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO events (id, type, name, description, summary, start_date, end_date)
VALUES
  (1, 'CONCERT', 'Mgmeral Live Concert', 'Demo concert event', 'Concert summary',
   '2026-02-01 18:00:00', '2026-02-01 23:00:00'),
  (2, 'THEATER', 'Mgmeral Theater Night', 'Demo theater event', 'Theater summary',
   '2026-03-10 19:30:00', '2026-03-10 22:00:00'),
  (3, 'SPORT', 'Mgmeral Championship', 'Demo sports event', 'Sports summary',
   '2026-04-05 16:00:00', '2026-04-05 19:30:00');

INSERT INTO performers (id, name, role, description)
VALUES
  (1, 'DJ Alpha',        'DJ',     'Electronic set'),
  (2, 'Singer Beta',     'VOCAL',  'Pop vocals'),
  (3, 'Actor Gamma',     'ACTOR',  'Lead performer'),
  (4, 'Band Delta',      'BAND',   'Live band'),
  (5, 'Commentator Zed', 'HOST',   'Stage host');

INSERT INTO event_performers (event_id, performer_id)
VALUES
  (1, 1),
  (1, 2),
  (1, 4),
  (2, 3),
  (2, 5),
  (3, 5);

INSERT INTO seances (id, event_id, capacity, start_date)
VALUES
  (1, 1, 100, '2026-02-01 20:00:00'),
  (2, 1,  50, '2026-02-02 20:00:00'),
  (3, 2,  30, '2026-03-10 20:00:00'),
  (4, 3,  25, '2026-04-05 17:00:00'),
  (5, 3,  10, '2026-04-05 18:30:00');

INSERT INTO holds (id, user_id, seance_id, quantity, status, idempotency_key, expires_at, released_at)
VALUES
  (1, 101, 1, 2, 'HELD',     'hold-seed-1', DATE_ADD(NOW(), INTERVAL 5 MINUTE),  NULL),
  (2, 102, 1, 3, 'HELD',     'hold-seed-2', DATE_ADD(NOW(), INTERVAL 2 MINUTE),  NULL),

  (3, 103, 1, 4, 'HELD',     'hold-seed-expired', DATE_SUB(NOW(), INTERVAL 10 MINUTE), NULL),

  (4, 104, 2, 1, 'RELEASED', 'hold-seed-released', DATE_ADD(NOW(), INTERVAL 5 MINUTE), NOW()),

  (5, 105, 3, 2, 'CONSUMED', 'hold-seed-consumed', DATE_ADD(NOW(), INTERVAL 5 MINUTE), NULL),

  (6, 106, 4, 2, 'HELD',     'hold-seed-4', DATE_ADD(NOW(), INTERVAL 8 MINUTE),  NULL);

INSERT INTO payments (id, payment_ref, status, amount)
VALUES
  (1, 'PAY-SEED-OK-1', 'AUTHORIZED', 200.00),
  (2, 'PAY-SEED-OK-2', 'AUTHORIZED', 150.00),
  (3, 'PAY-SEED-NO-1', 'DECLINED',   100.00);

INSERT INTO purchases (id, hold_id, seance_id, user_id, quantity, amount, payment_ref, status, idempotency_key)
VALUES
  (1, 1, 1, 101, 2, 200.00, 'PAY-SEED-OK-1', 'SOLD',   'purchase-seed-1'),
  (2, 6, 4, 106, 2, 150.00, 'PAY-SEED-OK-2', 'SOLD',   'purchase-seed-2'),
  (3, 2, 1, 102, 3, 300.00, 'PAY-SEED-NO-1', 'FAILED', 'purchase-seed-3');

UPDATE holds SET status='CONSUMED' WHERE id IN (1,6);

INSERT INTO events (id, type, name, description, summary, start_date, end_date)
VALUES
  (10, 'CONCERT', 'Capacity Edge Case Concert', 'Oversell test event', 'Edge case',
   '2026-05-01 18:00:00', '2026-05-01 23:00:00');

INSERT INTO seances (id, event_id, capacity, start_date)
VALUES
  (10, 10, 5, '2026-05-01 20:00:00');

INSERT INTO payments (id, payment_ref, status, amount)
VALUES
  (10, 'PAY-EDGE-OK', 'AUTHORIZED', 300.00);

INSERT INTO holds (id, user_id, seance_id, quantity, status, idempotency_key, expires_at, released_at)
VALUES
  (10, 201, 10, 3, 'CONSUMED', 'hold-edge-sold-1', DATE_ADD(NOW(), INTERVAL 5 MINUTE), NULL);

INSERT INTO purchases (id, hold_id, seance_id, user_id, quantity, amount, payment_ref, status, idempotency_key)
VALUES
  (10, 10, 10, 201, 3, 300.00, 'PAY-EDGE-OK', 'SOLD', 'purchase-edge-sold-1');

INSERT INTO holds (id, user_id, seance_id, quantity, status, idempotency_key, expires_at, released_at)
VALUES
  (11, 202, 10, 2, 'HELD', 'hold-edge-held-1', DATE_ADD(NOW(), INTERVAL 5 MINUTE), NULL);

INSERT INTO holds (id, user_id, seance_id, quantity, status, idempotency_key, expires_at, released_at)
VALUES
  (12, 203, 10, 1, 'HELD', 'hold-edge-expired-1', DATE_SUB(NOW(), INTERVAL 5 MINUTE), NULL);
