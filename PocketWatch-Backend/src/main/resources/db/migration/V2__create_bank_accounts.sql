CREATE TABLE bank_accounts (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    user_id               BIGINT       NOT NULL,
    nickname              VARCHAR(255) NOT NULL,
    bank_name             VARCHAR(255) NOT NULL,
    account_number_masked VARCHAR(20),
    account_type          VARCHAR(20)  NOT NULL DEFAULT 'CHECKING',
    active                BOOLEAN      NOT NULL DEFAULT TRUE,
    currency              VARCHAR(3)   NOT NULL DEFAULT 'INR',
    created_at            DATETIME     NOT NULL,
    updated_at            DATETIME     NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT fk_bank_accounts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_bank_accounts_user (user_id)
);