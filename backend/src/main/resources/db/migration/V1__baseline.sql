-- Baseline migration. Feature tables (users, orders, tickets, ...) are added in later migrations.
CREATE TABLE app_info (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    info_key    VARCHAR(100) NOT NULL UNIQUE,
    info_value  VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO app_info (info_key, info_value) VALUES ('schema_baseline', '1');
