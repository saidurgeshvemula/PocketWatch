CREATE TABLE categories (
    id        BIGINT       NOT NULL AUTO_INCREMENT,
    name      VARCHAR(100) NOT NULL,
    icon      VARCHAR(50),
    color     VARCHAR(7),
    is_system BOOLEAN      NOT NULL DEFAULT TRUE,

    PRIMARY KEY (id),
    UNIQUE KEY uk_categories_name (name)
);

CREATE TABLE file_uploads (
    id                     BIGINT       NOT NULL AUTO_INCREMENT,
    user_id                BIGINT       NOT NULL,
    bank_account_id        BIGINT,
    original_filename      VARCHAR(255) NOT NULL,
    file_type              ENUM('CSV', 'PDF') NOT NULL,
    storage_path           VARCHAR(512) NOT NULL,
    status                 ENUM('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'PENDING',
    transactions_extracted INT          NOT NULL DEFAULT 0,
    error_message          TEXT,
    uploaded_at            DATETIME     NOT NULL,
    processed_at           DATETIME,

    PRIMARY KEY (id),
    CONSTRAINT fk_uploads_user    FOREIGN KEY (user_id)         REFERENCES users (id)         ON DELETE CASCADE,
    CONSTRAINT fk_uploads_account FOREIGN KEY (bank_account_id) REFERENCES bank_accounts (id) ON DELETE SET NULL,
    INDEX idx_uploads_user_status (user_id, status)
);

CREATE TABLE transactions (
    id               BIGINT         NOT NULL AUTO_INCREMENT,
    user_id          BIGINT         NOT NULL,
    bank_account_id  BIGINT,
    file_upload_id   BIGINT,
    transaction_date DATE           NOT NULL,
    description      VARCHAR(512)   NOT NULL,
    amount           DECIMAL(15, 2) NOT NULL,
    type             ENUM('DEBIT', 'CREDIT') NOT NULL,
    category_id      BIGINT,
    merchant_name    VARCHAR(255),
    is_recurring     BOOLEAN        NOT NULL DEFAULT FALSE,
    notes            TEXT,
    created_at       DATETIME       NOT NULL,
    updated_at       DATETIME       NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT fk_tx_user     FOREIGN KEY (user_id)         REFERENCES users (id)         ON DELETE CASCADE,
    CONSTRAINT fk_tx_account  FOREIGN KEY (bank_account_id) REFERENCES bank_accounts (id) ON DELETE SET NULL,
    CONSTRAINT fk_tx_upload   FOREIGN KEY (file_upload_id)  REFERENCES file_uploads (id)  ON DELETE SET NULL,
    CONSTRAINT fk_tx_category FOREIGN KEY (category_id)     REFERENCES categories (id)    ON DELETE SET NULL,

    INDEX idx_tx_user_date     (user_id, transaction_date),
    INDEX idx_tx_user_category (user_id, category_id)
);
