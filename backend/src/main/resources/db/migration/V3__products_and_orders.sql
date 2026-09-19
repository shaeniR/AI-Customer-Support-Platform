CREATE TABLE products (
    id             BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(150)   NOT NULL,
    description    VARCHAR(1000)  NOT NULL,
    price          DECIMAL(12, 2) NOT NULL,
    stock_quantity INT            NOT NULL,
    status         VARCHAR(20)    NOT NULL,
    CONSTRAINT uk_products_name UNIQUE (name),
    CONSTRAINT ck_products_price CHECK (price >= 0),
    CONSTRAINT ck_products_stock CHECK (stock_quantity >= 0),
    CONSTRAINT ck_products_status CHECK (status IN ('ACTIVE', 'OUT_OF_STOCK', 'DISCONTINUED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE orders (
    id                     BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    customer_id            BIGINT         NOT NULL,
    order_number           VARCHAR(20)    NOT NULL,
    status                 VARCHAR(30)    NOT NULL,
    payment_status         VARCHAR(30)    NOT NULL,
    total_amount           DECIMAL(12, 2) NOT NULL,
    expected_delivery_date DATE           NULL,
    created_at             DATETIME(6)    NOT NULL,
    CONSTRAINT uk_orders_order_number UNIQUE (order_number),
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES users (id),
    CONSTRAINT ck_orders_status CHECK (status IN ('PLACED', 'SHIPPED', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED')),
    CONSTRAINT ck_orders_payment_status CHECK (payment_status IN ('PENDING', 'PAID', 'REFUNDED', 'DUPLICATE_CHARGE'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_orders_customer_created ON orders (customer_id, created_at);

CREATE TABLE order_items (
    id         BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id   BIGINT         NOT NULL,
    product_id BIGINT         NOT NULL,
    quantity   INT            NOT NULL,
    unit_price DECIMAL(12, 2) NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT ck_order_items_quantity CHECK (quantity > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
