CREATE TABLE IF NOT EXISTS cart (
    id UUID PRIMARY KEY,
    user_id VARCHAR(100),
    guest_id VARCHAR(100),
    status VARCHAR(30) NOT NULL,
    total_amount NUMERIC(15,2) NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_cart_user ON cart(user_id);
CREATE INDEX IF NOT EXISTS idx_cart_guest ON cart(guest_id);

CREATE TABLE IF NOT EXISTS cart_item (
    id UUID PRIMARY KEY,
    cart_id UUID NOT NULL,
    product_id UUID NOT NULL,
    sku VARCHAR(100) NOT NULL,
    product_name VARCHAR(255),
    product_image VARCHAR(500),
    price_snapshot NUMERIC(15,2) NOT NULL,
    quantity INTEGER NOT NULL,
    subtotal NUMERIC(15,2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_cart FOREIGN KEY (cart_id)
        REFERENCES cart(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_cart_item_cart ON cart_item(cart_id);
CREATE INDEX IF NOT EXISTS idx_cart_item_sku ON cart_item(sku);

CREATE TABLE IF NOT EXISTS idempotency_record (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    request_hash TEXT NOT NULL,
    response_body TEXT,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS outbox_event (
    id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    published_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_outbox_status ON outbox_event(status);
