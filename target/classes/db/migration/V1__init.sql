CREATE TABLE events (
  id BIGINT NOT NULL AUTO_INCREMENT,
  type VARCHAR(30) NOT NULL,
  name VARCHAR(200) NOT NULL,
  description VARCHAR(2000),
  summary VARCHAR(500),
  start_date TIMESTAMP NULL,
  end_date TIMESTAMP NULL,

  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (id)
);

CREATE TABLE performers (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(200) NOT NULL,
  role VARCHAR(100) NOT NULL,
  description VARCHAR(2000),

  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (id)
);

CREATE TABLE event_performers (
  event_id BIGINT NOT NULL,
  performer_id BIGINT NOT NULL,

  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (event_id, performer_id),
  KEY idx_ep_performer (performer_id),
  CONSTRAINT fk_ep_event FOREIGN KEY (event_id) REFERENCES events(id),
  CONSTRAINT fk_ep_performer FOREIGN KEY (performer_id) REFERENCES performers(id)
);

CREATE TABLE seances (
  id BIGINT NOT NULL AUTO_INCREMENT,
  event_id BIGINT NOT NULL,
  capacity INT NOT NULL,
  start_date TIMESTAMP NOT NULL,

  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  KEY idx_seances_event_start (event_id, start_date),
  CONSTRAINT fk_seances_event FOREIGN KEY (event_id) REFERENCES events(id)
);

CREATE TABLE holds (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  seance_id BIGINT NOT NULL,
  quantity INT NOT NULL,
  status VARCHAR(20) NOT NULL,
  idempotency_key VARCHAR(80) NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  released_at TIMESTAMP NULL,

  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE KEY uk_holds_idempotency (idempotency_key),
  KEY idx_holds_seance_status (seance_id, status),
  KEY idx_holds_seance_expires (seance_id, expires_at),
  CONSTRAINT fk_holds_seance FOREIGN KEY (seance_id) REFERENCES seances(id)
);

CREATE TABLE payments (
  id BIGINT NOT NULL AUTO_INCREMENT,
  payment_ref VARCHAR(64) NOT NULL,
  status VARCHAR(20) NOT NULL,
  amount DECIMAL(18,2) NOT NULL,

  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE KEY uk_payments_ref (payment_ref)
);

CREATE TABLE purchases (
  id BIGINT NOT NULL AUTO_INCREMENT,
  hold_id BIGINT NOT NULL,
  seance_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  quantity INT NOT NULL,
  amount DECIMAL(18,2) NOT NULL,
  payment_ref VARCHAR(64) NOT NULL,
  status VARCHAR(20) NOT NULL,
  idempotency_key VARCHAR(80) NOT NULL,

  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE KEY uk_purchases_idempotency (idempotency_key),
  UNIQUE KEY uk_purchases_paymentref (payment_ref),
  KEY idx_purchases_seance_status (seance_id, status),
  CONSTRAINT fk_purchases_hold FOREIGN KEY (hold_id) REFERENCES holds(id)
);
