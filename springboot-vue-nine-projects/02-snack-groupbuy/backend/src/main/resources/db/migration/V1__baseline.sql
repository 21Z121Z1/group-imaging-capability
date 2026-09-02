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

CREATE TABLE group_campaign (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  product_id BIGINT NOT NULL,
  title VARCHAR(255) NOT NULL,
  group_price DECIMAL(12,2) NOT NULL,
  target_quantity INT NOT NULL,
  sold_quantity INT NOT NULL,
  start_time TIMESTAMP(6) NOT NULL,
  end_time TIMESTAMP(6) NOT NULL,
  status VARCHAR(255) NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE group_order (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  campaign_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  quantity INT NOT NULL,
  total_amount DECIMAL(12,2) NOT NULL,
  status VARCHAR(255) NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE snack_product (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  name VARCHAR(255) NOT NULL,
  brand VARCHAR(255),
  category VARCHAR(255),
  price DECIMAL(12,2) NOT NULL,
  image_url VARCHAR(255),
  stock INT NOT NULL,
  status VARCHAR(255) NOT NULL,
  PRIMARY KEY (id)
);

CREATE INDEX idx_auth_token_expires_at ON auth_token (expires_at);

ALTER TABLE auth_token ADD CONSTRAINT fk_auth_token_user_id FOREIGN KEY (user_id) REFERENCES app_user(id);
ALTER TABLE group_campaign ADD CONSTRAINT fk_group_campaign_product_id FOREIGN KEY (product_id) REFERENCES snack_product(id);
ALTER TABLE group_order ADD CONSTRAINT fk_group_order_campaign_id FOREIGN KEY (campaign_id) REFERENCES group_campaign(id);
ALTER TABLE group_order ADD CONSTRAINT fk_group_order_user_id FOREIGN KEY (user_id) REFERENCES app_user(id);

CREATE INDEX idx_group_campaign_product_id ON group_campaign (product_id);
CREATE INDEX idx_group_order_user_id ON group_order (user_id);
CREATE INDEX idx_group_order_campaign_id ON group_order (campaign_id);
