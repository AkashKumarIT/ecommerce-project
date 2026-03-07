-- Prevent multiple ACTIVE carts per user
CREATE UNIQUE INDEX IF NOT EXISTS uniq_active_user_cart
ON cart(user_id)
WHERE status = 'ACTIVE';

-- Prevent multiple ACTIVE carts per guest
CREATE UNIQUE INDEX IF NOT EXISTS uniq_active_guest_cart
ON cart(guest_id)
WHERE status = 'ACTIVE';
