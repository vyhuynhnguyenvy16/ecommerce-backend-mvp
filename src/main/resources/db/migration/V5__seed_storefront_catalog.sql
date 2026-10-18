INSERT INTO categories (name, slug, parent_id)
VALUES ('Thời trang nam', 'thoi-trang-nam', NULL)
ON CONFLICT (slug) DO NOTHING;

INSERT INTO categories (name, slug, parent_id)
SELECT 'Áo khoác', 'ao-khoac', category.id
FROM categories category
WHERE category.slug = 'thoi-trang-nam'
ON CONFLICT (slug) DO NOTHING;

INSERT INTO products (category_id, name, slug, description, base_price, status)
SELECT category.id, seed.name, seed.slug, seed.description, seed.base_price, 'ACTIVE'
FROM (
    VALUES
        ('ao-khoac', 'Áo Khoác Bomber Minimalist', 'ao-khoac-bomber-minimalist',
         'Áo khoác bomber phong cách tối giản, chất liệu bền đẹp.', 650000.00),
        ('ao-khoac', 'Áo Khoác Gió Everyday', 'ao-khoac-gio-everyday',
         'Áo khoác gió nhẹ, phù hợp sử dụng hằng ngày.', 490000.00),
        ('ao-khoac', 'Áo Khoác Denim Classic', 'ao-khoac-denim-classic',
         'Áo khoác denim cổ điển, dễ phối đồ.', 720000.00)
) AS seed(category_slug, name, slug, description, base_price)
JOIN categories category ON category.slug = seed.category_slug
ON CONFLICT (slug) DO NOTHING;

INSERT INTO product_images (product_id, url, sort_order, is_primary)
SELECT product.id, seed.url, 0, TRUE
FROM (
    VALUES
        ('ao-khoac-bomber-minimalist', 'https://images.unsplash.com/photo-1551028719-00167b16eac5'),
        ('ao-khoac-gio-everyday', 'https://images.unsplash.com/photo-1544923246-77307dd654cb'),
        ('ao-khoac-denim-classic', 'https://images.unsplash.com/photo-1515886657613-9f3515b0c78f')
) AS seed(product_slug, url)
JOIN products product ON product.slug = seed.product_slug
WHERE NOT EXISTS (
    SELECT 1
    FROM product_images image
    WHERE image.product_id = product.id AND image.url = seed.url
);

INSERT INTO product_variants (product_id, sku, size, color, price_override, stock_quantity)
SELECT product.id, seed.sku, seed.size, seed.color, NULL, seed.stock_quantity
FROM (
    VALUES
        ('ao-khoac-bomber-minimalist', 'NL-BOMB-BLK-M', 'M', 'Đen', 20),
        ('ao-khoac-bomber-minimalist', 'NL-BOMB-BLK-L', 'L', 'Đen', 15),
        ('ao-khoac-gio-everyday', 'NL-GIO-NAV-M', 'M', 'Xanh navy', 12),
        ('ao-khoac-gio-everyday', 'NL-GIO-NAV-L', 'L', 'Xanh navy', 10),
        ('ao-khoac-denim-classic', 'NL-DENIM-BLU-M', 'M', 'Xanh denim', 8),
        ('ao-khoac-denim-classic', 'NL-DENIM-BLU-L', 'L', 'Xanh denim', 6)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products product ON product.slug = seed.product_slug
ON CONFLICT DO NOTHING;
