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

CREATE TABLE assignment_info (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  course_id BIGINT NOT NULL,
  title VARCHAR(255) NOT NULL,
  description VARCHAR(3000),
  due_at TIMESTAMP(6) NOT NULL,
  max_score DECIMAL(8,2) NOT NULL,
  status VARCHAR(255) NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE course_info (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  code VARCHAR(255) NOT NULL,
  name VARCHAR(255) NOT NULL,
  teacher VARCHAR(255),
  description VARCHAR(2000),
  status VARCHAR(255) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_course_info_code UNIQUE (code)
);

CREATE TABLE submission_info (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  assignment_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  content VARCHAR(8000),
  attachment_url VARCHAR(255),
  submitted_at TIMESTAMP(6) NOT NULL,
  score DECIMAL(8,2),
  feedback VARCHAR(2000),
  status VARCHAR(255) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_submission_info_assignment_id_user_id UNIQUE (assignment_id, user_id)
);

CREATE INDEX idx_auth_token_expires_at ON auth_token (expires_at);

ALTER TABLE auth_token ADD CONSTRAINT fk_auth_token_user_id FOREIGN KEY (user_id) REFERENCES app_user(id);
ALTER TABLE assignment_info ADD CONSTRAINT fk_assignment_info_course_id FOREIGN KEY (course_id) REFERENCES course_info(id);
ALTER TABLE submission_info ADD CONSTRAINT fk_submission_info_assignment_id FOREIGN KEY (assignment_id) REFERENCES assignment_info(id);
ALTER TABLE submission_info ADD CONSTRAINT fk_submission_info_user_id FOREIGN KEY (user_id) REFERENCES app_user(id);

CREATE INDEX idx_assignment_info_course_id ON assignment_info (course_id);
CREATE INDEX idx_submission_info_user_id ON submission_info (user_id);
