-- V1: Khoi tao schema cho ung dung order ca phe
-- Danh muc mon (vd: Ca phe, Tra, Banh)
CREATE TABLE category (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    sort_order  INT NOT NULL DEFAULT 0
);

-- Mon trong menu
CREATE TABLE menu_item (
    id           BIGSERIAL PRIMARY KEY,
    category_id  BIGINT NOT NULL REFERENCES category (id),
    name         VARCHAR(150) NOT NULL,
    description  VARCHAR(500),
    price        NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
    available    BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_menu_item_category ON menu_item (category_id);

-- Don hang
CREATE TABLE orders (
    id            BIGSERIAL PRIMARY KEY,
    table_no      VARCHAR(20) NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'NEW',   -- NEW, PAID, CANCELLED
    total_amount  NUMERIC(12, 2) NOT NULL DEFAULT 0,
    note          VARCHAR(500),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_created_at ON orders (created_at);

-- Dong chi tiet trong don (snapshot ten + gia tai thoi diem order)
CREATE TABLE order_item (
    id            BIGSERIAL PRIMARY KEY,
    order_id      BIGINT NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    menu_item_id  BIGINT NOT NULL REFERENCES menu_item (id),
    item_name     VARCHAR(150) NOT NULL,
    unit_price    NUMERIC(12, 2) NOT NULL,
    quantity      INT NOT NULL CHECK (quantity > 0),
    line_total    NUMERIC(12, 2) NOT NULL,
    note          VARCHAR(300)
);
CREATE INDEX idx_order_item_order ON order_item (order_id);

-- Thanh toan
CREATE TABLE payment (
    id         BIGSERIAL PRIMARY KEY,
    order_id   BIGINT NOT NULL UNIQUE REFERENCES orders (id),
    method     VARCHAR(20) NOT NULL,   -- CASH, CARD, TRANSFER
    amount     NUMERIC(12, 2) NOT NULL,
    paid_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
