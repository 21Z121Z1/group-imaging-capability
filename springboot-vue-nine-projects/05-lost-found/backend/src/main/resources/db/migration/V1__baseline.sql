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

CREATE TABLE claim_request (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  post_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  proof VARCHAR(2000),
  status VARCHAR(255) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_claim_request_post_id_user_id UNIQUE (post_id, user_id)
);

CREATE TABLE lost_found_post (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  type VARCHAR(255) NOT NULL,
  title VARCHAR(255) NOT NULL,
  category VARCHAR(255),
  location VARCHAR(255),
  event_time TIMESTAMP(6),
  description VARCHAR(3000),
  contact VARCHAR(255),
  owner_user_id BIGINT NOT NULL,
  status VARCHAR(255) NOT NULL,
  image_url VARCHAR(255),
  PRIMARY KEY (id)
);

CREATE INDEX idx_auth_token_expires_at ON auth_token (expires_at);

ALTER TABLE auth_token ADD CONSTRAINT fk_auth_token_user_id FOREIGN KEY (user_id) REFERENCES app_user(id);
ALTER TABLE lost_found_post ADD CONSTRAINT fk_lost_found_post_owner_user_id FOREIGN KEY (owner_user_id) REFERENCES app_user(id);
ALTER TABLE claim_request ADD CONSTRAINT fk_claim_request_post_id FOREIGN KEY (post_id) REFERENCES lost_found_post(id);
ALTER TABLE claim_request ADD CONSTRAINT fk_claim_request_user_id FOREIGN KEY (user_id) REFERENCES app_user(id);

CREATE INDEX idx_lost_found_post_owner_user_id ON lost_found_post (owner_user_id);
CREATE INDEX idx_claim_request_user_id ON claim_request (user_id);
