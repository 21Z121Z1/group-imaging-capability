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

CREATE TABLE rental_listing (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  title VARCHAR(255) NOT NULL,
  city VARCHAR(255),
  district VARCHAR(255),
  address VARCHAR(255) NOT NULL,
  monthly_rent DECIMAL(12,2) NOT NULL,
  bedrooms INT NOT NULL,
  area_sqm DECIMAL(10,2),
  description VARCHAR(3000),
  contact VARCHAR(255),
  owner_user_id BIGINT NOT NULL,
  status VARCHAR(255) NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE viewing_appointment (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  listing_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  visit_time TIMESTAMP(6) NOT NULL,
  status VARCHAR(255) NOT NULL,
  note VARCHAR(1000),
  PRIMARY KEY (id)
);

CREATE INDEX idx_auth_token_expires_at ON auth_token (expires_at);

ALTER TABLE auth_token ADD CONSTRAINT fk_auth_token_user_id FOREIGN KEY (user_id) REFERENCES app_user(id);
ALTER TABLE rental_listing ADD CONSTRAINT fk_rental_listing_owner_user_id FOREIGN KEY (owner_user_id) REFERENCES app_user(id);
ALTER TABLE viewing_appointment ADD CONSTRAINT fk_viewing_appointment_listing_id FOREIGN KEY (listing_id) REFERENCES rental_listing(id);
ALTER TABLE viewing_appointment ADD CONSTRAINT fk_viewing_appointment_user_id FOREIGN KEY (user_id) REFERENCES app_user(id);

CREATE INDEX idx_rental_listing_owner_user_id ON rental_listing (owner_user_id);
CREATE INDEX idx_viewing_appointment_user_id ON viewing_appointment (user_id);
CREATE INDEX idx_viewing_appointment_listing_id ON viewing_appointment (listing_id);
