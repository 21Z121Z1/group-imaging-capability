-- Baseline schema managed by Flyway. Production uses ddl-auto=validate.

CREATE TABLE auth_token (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  token_hash VARCHAR(64) NOT NULL,
  user_id BIGINT NOT NULL,
  expires_at TIMESTAMP(6) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_auth_token_token_hash UNIQUE (token_hash)
);

CREATE TABLE app_user (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  username VARCHAR(64) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  display_name VARCHAR(64) NOT NULL,
  role VARCHAR(16) NOT NULL,
  enabled BOOLEAN NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_app_user_username UNIQUE (username)
);

CREATE TABLE lab_reservation (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  lab_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  title VARCHAR(255) NOT NULL,
  purpose VARCHAR(2000),
  start_time TIMESTAMP(6) NOT NULL,
  end_time TIMESTAMP(6) NOT NULL,
  status VARCHAR(255) NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE laboratory (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  name VARCHAR(255) NOT NULL,
  building VARCHAR(255),
  room VARCHAR(255),
  capacity INT NOT NULL,
  equipment VARCHAR(2000),
  open_time VARCHAR(255),
  close_time VARCHAR(255),
  status VARCHAR(255) NOT NULL,
  PRIMARY KEY (id)
);

CREATE INDEX idx_auth_token_expires_at ON auth_token (expires_at);

ALTER TABLE auth_token ADD CONSTRAINT fk_auth_token_user_id FOREIGN KEY (user_id) REFERENCES app_user(id);
ALTER TABLE lab_reservation ADD CONSTRAINT fk_lab_reservation_user_id FOREIGN KEY (user_id) REFERENCES app_user(id);
ALTER TABLE lab_reservation ADD CONSTRAINT fk_lab_reservation_lab_id FOREIGN KEY (lab_id) REFERENCES laboratory(id);

CREATE INDEX idx_lab_reservation_user_id ON lab_reservation (user_id);
CREATE INDEX idx_lab_reservation_lab_id_start_time_end_time ON lab_reservation (lab_id, start_time, end_time);
