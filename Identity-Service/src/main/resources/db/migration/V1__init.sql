-- src/main/resources/db/migration/V1__init.sql

CREATE TABLE roles (
                       id BIGSERIAL PRIMARY KEY, -- Đổi sang BIGSERIAL
                       name VARCHAR(50) UNIQUE NOT NULL,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE permissions (
                             id BIGSERIAL PRIMARY KEY, -- Đổi sang BIGSERIAL
                             code VARCHAR(100) UNIQUE NOT NULL,
                             created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE membership_tiers (
                                  id BIGSERIAL PRIMARY KEY, -- Đổi sang BIGSERIAL
                                  tier_name VARCHAR(30) UNIQUE,
                                  min_points INT DEFAULT 0,
                                  discount_rate DECIMAL(5,2) DEFAULT 0
);

CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY, -- Đổi sang BIGSERIAL
                       email VARCHAR(120) UNIQUE NOT NULL,
                       password_hash TEXT,
                       full_name VARCHAR(120),
                       phone VARCHAR(20),
                       address TEXT,
                       avatar_url TEXT,
                       dob DATE,
                       gender VARCHAR(20),
                       status VARCHAR(20) DEFAULT 'ACTIVE',
                       current_points INT DEFAULT 0,
                       tier_id BIGINT REFERENCES membership_tiers(id), -- Phải là BIGINT để khớp với FK
                       email_verified BOOLEAN DEFAULT FALSE,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_roles (
                            user_id BIGINT REFERENCES users(id), -- Đổi sang BIGINT
                            role_id BIGINT REFERENCES roles(id), -- Đổi sang BIGINT
                            PRIMARY KEY(user_id, role_id)
);

CREATE TABLE role_permissions (
                                  role_id BIGINT REFERENCES roles(id),       -- Đổi sang BIGINT
                                  permission_id BIGINT REFERENCES permissions(id), -- Đổi sang BIGINT
                                  PRIMARY KEY(role_id, permission_id)
);

CREATE TABLE social_accounts (
                                 id BIGSERIAL PRIMARY KEY, -- Đổi sang BIGSERIAL
                                 user_id BIGINT REFERENCES users(id), -- Đổi sang BIGINT
                                 provider VARCHAR(50),
                                 provider_id VARCHAR(120),
                                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);