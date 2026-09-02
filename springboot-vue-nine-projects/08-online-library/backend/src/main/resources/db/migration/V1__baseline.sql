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

CREATE TABLE library_book (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  isbn VARCHAR(255) NOT NULL,
  title VARCHAR(255) NOT NULL,
  author VARCHAR(255),
  publisher VARCHAR(255),
  category VARCHAR(255),
  total_copies INT NOT NULL,
  available_copies INT NOT NULL,
  description VARCHAR(3000),
  status VARCHAR(255) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_library_book_isbn UNIQUE (isbn)
);

CREATE TABLE borrow_record (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  book_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  borrowed_at TIMESTAMP(6) NOT NULL,
  due_at TIMESTAMP(6) NOT NULL,
  returned_at TIMESTAMP(6),
  status VARCHAR(255) NOT NULL,
  PRIMARY KEY (id)
);

CREATE INDEX idx_auth_token_expires_at ON auth_token (expires_at);

ALTER TABLE auth_token ADD CONSTRAINT fk_auth_token_user_id FOREIGN KEY (user_id) REFERENCES app_user(id);
ALTER TABLE borrow_record ADD CONSTRAINT fk_borrow_record_user_id FOREIGN KEY (user_id) REFERENCES app_user(id);
ALTER TABLE borrow_record ADD CONSTRAINT fk_borrow_record_book_id FOREIGN KEY (book_id) REFERENCES library_book(id);

CREATE INDEX idx_borrow_record_user_id ON borrow_record (user_id);
CREATE INDEX idx_borrow_record_book_id ON borrow_record (book_id);
