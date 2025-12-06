-- add vipStatus to users
ALTER TABLE users ADD COLUMN vip_status BOOLEAN DEFAULT FALSE;

-- add reservedStock to products
ALTER TABLE products ADD COLUMN reserved_stock INT DEFAULT 0 NOT NULL;

-- add createdAt timestamp and status to orders
ALTER TABLE orders ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE orders ADD COLUMN status VARCHAR(20) DEFAULT 'PENDING';

-- index on created_at
CREATE INDEX idx_orders_created_at ON orders (created_at);
