CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_name VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(50) NOT NULL,
    role ENUM('USER', 'ADMIN') NOT NULL,
    failed_login_count INT NOT NULL,
    locked_until DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_user_name UNIQUE (user_name),
    INDEX idx_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE foods (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(255) NULL,
    img_url VARCHAR(500) NULL,
    serving_sizeg INT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_foods_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE nutritions (
    food_id BIGINT NOT NULL,
    calories DECIMAL(8, 2) NULL,
    carbohydrate DECIMAL(8, 2) NULL,
    protein DECIMAL(8, 2) NULL,
    fat DECIMAL(8, 2) NULL,
    PRIMARY KEY (food_id),
    CONSTRAINT fk_nutritions_food FOREIGN KEY (food_id) REFERENCES foods (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE menus (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_name VARCHAR(255) NULL,
    menu_name VARCHAR(255) NULL,
    food_id BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_menus_user_menu UNIQUE (user_name, menu_name),
    INDEX idx_menus_user_name (user_name),
    INDEX idx_menus_food_id (food_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE email_verifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(100) NOT NULL,
    code VARCHAR(10) NOT NULL,
    verified BIT NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    attempt_count INT NOT NULL,
    blocked_until DATETIME(6) NULL,
    PRIMARY KEY (id),
    INDEX idx_email_verifications_email_created_at (email, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE password_reset_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(100) NOT NULL,
    code VARCHAR(10) NOT NULL,
    verified BIT NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    attempt_count INT NOT NULL,
    blocked_until DATETIME(6) NULL,
    PRIMARY KEY (id),
    INDEX idx_password_reset_tokens_email_created_at (email, created_at),
    INDEX idx_password_reset_tokens_email_code_created_at (email, code, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
