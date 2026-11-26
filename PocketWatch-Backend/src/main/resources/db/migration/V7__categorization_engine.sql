-- Hybrid categorization engine: merchant DB, aliases, rules, and
-- explainability columns on transactions.

INSERT INTO categories (name, icon, color, is_system) VALUES
    ('Cash Withdrawal', '🏧', '#8395A7', TRUE),
    ('Loan',             '🏦', '#EE5A6F', TRUE);

CREATE TABLE merchants (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    normalized_name VARCHAR(255) NOT NULL,
    display_name    VARCHAR(255) NOT NULL,
    category_id     BIGINT       NOT NULL,
    confidence      INT          NOT NULL,
    user_defined    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      DATETIME     NOT NULL,
    updated_at      DATETIME     NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_merchants_normalized_name (normalized_name),
    CONSTRAINT fk_merchants_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE RESTRICT
);

CREATE TABLE merchant_aliases (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    merchant_id BIGINT       NOT NULL,
    alias       VARCHAR(255) NOT NULL,
    created_at  DATETIME     NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_merchant_aliases_alias (alias),
    CONSTRAINT fk_aliases_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id) ON DELETE CASCADE
);

CREATE TABLE category_rules (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    keyword     VARCHAR(100) NOT NULL,
    category_id BIGINT       NOT NULL,
    priority    INT          NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_category_rules_keyword (keyword),
    CONSTRAINT fk_rules_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE CASCADE
);

-- TiDB does not support multiple schema changes in one ALTER with a foreign key,
-- so each change is its own statement, with the foreign key last.
ALTER TABLE transactions ADD COLUMN merchant_id BIGINT NULL AFTER merchant_name;
ALTER TABLE transactions ADD COLUMN category_source VARCHAR(20) NULL AFTER merchant_id;
ALTER TABLE transactions ADD COLUMN category_confidence INT NULL AFTER category_source;
ALTER TABLE transactions ADD COLUMN category_reason VARCHAR(255) NULL AFTER category_confidence;
ALTER TABLE transactions ADD CONSTRAINT fk_tx_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id) ON DELETE SET NULL;

INSERT INTO category_rules (keyword, category_id, priority, created_at)
SELECT 'ATM', id, 10, NOW() FROM categories WHERE name = 'Cash Withdrawal'
UNION ALL
SELECT 'SALARY', id, 10, NOW() FROM categories WHERE name = 'Income'
UNION ALL
SELECT 'IRCTC', id, 10, NOW() FROM categories WHERE name = 'Travel'
UNION ALL
SELECT 'FASTAG', id, 10, NOW() FROM categories WHERE name = 'Transport'
UNION ALL
SELECT 'NETFLIX', id, 10, NOW() FROM categories WHERE name = 'Subscriptions'
UNION ALL
SELECT 'MPOKKET', id, 10, NOW() FROM categories WHERE name = 'Loan'
UNION ALL
SELECT 'ELECTRICITY', id, 10, NOW() FROM categories WHERE name = 'Utilities';