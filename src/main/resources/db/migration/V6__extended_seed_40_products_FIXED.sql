-- ============================================
-- V6: EXTENDED SEED DATA - 40 SẢN PHẨM THỜI TRANG (FIXED)
-- ============================================
-- Fix: Bỏ price_override khỏi VALUES vì nó luôn NULL
-- Chỉ insert: product_id, sku, size, color, stock_quantity, version

-- ============================================
-- PHẦN 1: THÊM 7 DANH MỤC CHÍNH
-- ============================================

INSERT INTO categories (name, slug, parent_id) VALUES
('Áo Quần', 'ao-quan', NULL),
('Áo Khoác', 'ao-khoac', NULL),
('Quần Jean', 'quan-jean', NULL),
('Váy Đầm', 'vay-dam', NULL),
('Giày Dép', 'giay-dep', NULL),
('Túi Xách', 'tui-xach', NULL),
('Phụ Kiện', 'phu-kien', NULL)
ON CONFLICT (slug) DO NOTHING;

-- ============================================
-- PHẦN 2: THÊM 40 SẢN PHẨM
-- ============================================

-- ===== CATEGORY: Áo Quần (8 sản phẩm) =====

INSERT INTO products (category_id, name, slug, description, base_price, status)
SELECT cat.id, seed.name, seed.slug, seed.description, seed.base_price, 'ACTIVE'
FROM (
    VALUES
        ('ao-quan', 'Heavyweight Boxy Tee', 'heavyweight-boxy-tee', 'Áo thun 100% cotton dày 280gsm, đứng phom, thoáng mát thấm hút.', 450000.00),
        ('ao-quan', 'Premium Linen Blend Shirt', 'premium-linen-blend-shirt', 'Áo sơ mi trộn linen 60% cotton, co giãn nhẹ, thích hợp mùa hè.', 750000.00),
        ('ao-quan', 'Oversized Graphic Tee', 'oversized-graphic-tee', 'Áo thun unisex phom rộng in họa tiết, chất liệu thoáng mát.', 500000.00),
        ('ao-quan', 'Ribbed Long Sleeve Top', 'ribbed-long-sleeve-top', 'Áo dài tay gân ôm nhẹ, co giãn thoải mái, dễ phối đồ.', 550000.00),
        ('ao-quan', 'Silk Blend Camisole', 'silk-blend-camisole', 'Áo hai dây silk cao cấp, mịn mặn, phù hợp mặc trong hay ngoài.', 650000.00),
        ('ao-quan', 'Tailored Wool Trousers', 'tailored-wool-trousers', 'Quần vải len cao cấp, form đứng, thoáng khí, thích hợp công sở.', 1200000.00),
        ('ao-quan', 'Casual Chino Pants', 'casual-chino-pants', 'Quần chino cotton thoáng mát, phom ôm vừa, dễ phối đồ.', 650000.00),
        ('ao-quan', 'Slim Fit Joggers', 'slim-fit-joggers', 'Quần jogger co giãn cao, thoáng mát, phù hợp mặc nhà hay tập luyện.', 700000.00)
) AS seed(category_slug, name, slug, description, base_price)
JOIN categories cat ON cat.slug = seed.category_slug
ON CONFLICT (slug) DO NOTHING;

-- ===== CATEGORY: Áo Khoác (8 sản phẩm) =====

INSERT INTO products (category_id, name, slug, description, base_price, status)
SELECT cat.id, seed.name, seed.slug, seed.description, seed.base_price, 'ACTIVE'
FROM (
    VALUES
        ('ao-khoac', 'Minimalist Wool Bomber Jacket', 'minimalist-wool-bomber-jacket', 'Áo khoác bomber chất liệu dạ wool pha cao cấp, phom suông hiện đại.', 1450000.00),
        ('ao-khoac', 'Double-Breasted Trench Coat', 'double-breasted-trench-coat', 'Áo măng tô dáy dài cài khuy đôi kinh điển, chống thấm nước nhẹ.', 1850000.00),
        ('ao-khoac', 'Oversized Blazer', 'oversized-blazer', 'Áo blazer phom rộng hiện đại, kết hợp form đẹp với thoải mái.', 1550000.00),
        ('ao-khoac', 'Denim Jacket Classic', 'denim-jacket-classic', 'Áo khoác jean tinh tế, phom vừa vặn, dễ kết hợp mọi bộ đồ.', 1100000.00),
        ('ao-khoac', 'Lightweight Windbreaker', 'lightweight-windbreaker', 'Áo khoác gió nhẹ chống nước, gấp gọn dễ mang theo.', 850000.00),
        ('ao-khoac', 'Wool Coat Long', 'wool-coat-long', 'Áo khoác len dài cổ điển, dễ tạo layers, ấm áp trong mùa lạnh.', 2100000.00),
        ('ao-khoac', 'Leather Moto Jacket', 'leather-moto-jacket', 'Áo da lộn phong cách retro, khỏe khoắn, thích hợp phối casual.', 2500000.00),
        ('ao-khoac', 'Puffer Down Jacket', 'puffer-down-jacket', 'Áo bông mỏng nhẹ, ấm cao cấp, không khí lạnh không lạnh.', 1750000.00)
) AS seed(category_slug, name, slug, description, base_price)
JOIN categories cat ON cat.slug = seed.category_slug
ON CONFLICT (slug) DO NOTHING;

-- ===== CATEGORY: Quần Jean (6 sản phẩm) =====

INSERT INTO products (category_id, name, slug, description, base_price, status)
SELECT cat.id, seed.name, seed.slug, seed.description, seed.base_price, 'ACTIVE'
FROM (
    VALUES
        ('quan-jean', 'Relaxed Fit Selvedge Denim', 'relaxed-fit-selvedge-denim', 'Quần jeans vải selvedge dệt biên cổ điển, wash nhẹ tự nhiên.', 950000.00),
        ('quan-jean', 'Skinny Fit High Waist', 'skinny-fit-high-waist', 'Quần jean ôm chân, eo cao, phom hợp mốt, tôn dáng.', 850000.00),
        ('quan-jean', 'Straight Leg Vintage Wash', 'straight-leg-vintage-wash', 'Quần jean ống suông, wash cũ kinh điển, dễ phối.', 900000.00),
        ('quan-jean', 'Flared Hem Denim', 'flared-hem-denim', 'Quần jean ống xòe gợi nhớ 70s, phá cách nhưng thanh lịch.', 880000.00),
        ('quan-jean', 'Distressed Ripped Jeans', 'distressed-ripped-jeans', 'Quần jean rách thiết kế, hiện đại, thích hợp gen trẻ.', 920000.00),
        ('quan-jean', 'Black Slim Denim', 'black-slim-denim', 'Quần jean đen ôm vừa vặn, nhiều áo thường phối, cơ bản mà sang.', 800000.00)
) AS seed(category_slug, name, slug, description, base_price)
JOIN categories cat ON cat.slug = seed.category_slug
ON CONFLICT (slug) DO NOTHING;

-- ===== CATEGORY: Váy Đầm (6 sản phẩm) =====

INSERT INTO products (category_id, name, slug, description, base_price, status)
SELECT cat.id, seed.name, seed.slug, seed.description, seed.base_price, 'ACTIVE'
FROM (
    VALUES
        ('vay-dam', 'Ribbed Knit Midi Dress', 'ribbed-knit-midi-dress', 'Đầm len gân ôm nhẹ dáng người, tôn dáng, co giãn thoải mái.', 1100000.00),
        ('vay-dam', 'Flowy Maxi Dress', 'flowy-maxi-dress', 'Đầm dài xòe nhẹ nhàng, chất lụa, thích hợp mùa hè.', 1350000.00),
        ('vay-dam', 'Slip Dress Satin', 'slip-dress-satin', 'Đầm 2 dây satin mượt mà, gợi cảm nhưng thanh lịch, dễ phối áo khoác.', 1250000.00),
        ('vay-dam', 'Mini Shift Dress', 'mini-shift-dress', 'Đầm ngắn phom thẳng, 100% cotton, thoáng mát, dễ mặc.', 750000.00),
        ('vay-dam', 'Wrap Dress Knit', 'wrap-dress-knit', 'Đầm len quấn ngoài, phom tôn dáng, dễ co giãn thoải mái.', 1150000.00),
        ('vay-dam', 'Shirt Dress Cotton', 'shirt-dress-cotton', 'Váy sơ mi cotton, phom dài lửng, có túi, dễ phối giày.', 1050000.00)
) AS seed(category_slug, name, slug, description, base_price)
JOIN categories cat ON cat.slug = seed.category_slug
ON CONFLICT (slug) DO NOTHING;

-- ===== CATEGORY: Giày Dép (7 sản phẩm) =====

INSERT INTO products (category_id, name, slug, description, base_price, status)
SELECT cat.id, seed.name, seed.slug, seed.description, seed.base_price, 'ACTIVE'
FROM (
    VALUES
        ('giay-dep', 'Clean Leather Low Sneakers', 'clean-leather-low-sneakers', 'Giày thể thao da bò cao cấp phong cách tối giản, đế cao su khâu viền.', 1300000.00),
        ('giay-dep', 'Canvas High Top Sneaker', 'canvas-high-top-sneaker', 'Giày vải cao cổ cổ điển, nhẹ nhàng, thích hợp gen trẻ.', 950000.00),
        ('giay-dep', 'Chunky Sole Sneaker', 'chunky-sole-sneaker', 'Giày thể thao đế dày, hiện đại, thoải mái khi đi.', 1150000.00),
        ('giay-dep', 'Pointed Toe Flat', 'pointed-toe-flat', 'Giày búp bê gợt nhọn, thanh lịch, thích hợp công sở.', 1050000.00),
        ('giay-dep', 'Heel Pump Classic', 'heel-pump-classic', 'Giày cao gót gỗ 7cm, tôn dáng, phù hợp dự tiệc.', 1400000.00),
        ('giay-dep', 'Chelsea Boot Leather', 'chelsea-boot-leather', 'Giày da cao cổ kiểu Anh, khỏe khoắn, thích hợp mặc lên.', 1650000.00),
        ('giay-dep', 'Slip On Loafer', 'slip-on-loafer', 'Giày lười không dây, nhẹ nhàng, phong cách thanh lịch.', 1250000.00)
) AS seed(category_slug, name, slug, description, base_price)
JOIN categories cat ON cat.slug = seed.category_slug
ON CONFLICT (slug) DO NOTHING;

-- ===== CATEGORY: Túi Xách (4 sản phẩm) =====

INSERT INTO products (category_id, name, slug, description, base_price, status)
SELECT cat.id, seed.name, seed.slug, seed.description, seed.base_price, 'ACTIVE'
FROM (
    VALUES
        ('tui-xach', 'Everyday Utility Canvas Tote', 'everyday-utility-canvas-tote', 'Túi tote vải bố 16oz siêu bền, có ngăn phụ đựng laptop 15 inch.', 650000.00),
        ('tui-xach', 'Crossbody Shoulder Bag', 'crossbody-shoulder-bag', 'Túi đeo chéo da cao cấp, có ngăn bên trong, nhỏ gọn tiện lợi.', 950000.00),
        ('tui-xach', 'Structured Handbag', 'structured-handbag', 'Túi xách da cứng nhắc phong cách cổ điển, tôn thêm vẻ quý phái.', 1450000.00),
        ('tui-xach', 'Backpack Laptop', 'backpack-laptop', 'Ba lô đa năng chứa được laptop 17 inch, ngăn phụ nhiều, chống nước.', 1100000.00)
) AS seed(category_slug, name, slug, description, base_price)
JOIN categories cat ON cat.slug = seed.category_slug
ON CONFLICT (slug) DO NOTHING;

-- ===== CATEGORY: Phụ Kiện (5 sản phẩm) =====

INSERT INTO products (category_id, name, slug, description, base_price, status)
SELECT cat.id, seed.name, seed.slug, seed.description, seed.base_price, 'ACTIVE'
FROM (
    VALUES
        ('phu-kien', 'Classic Acetate Sunglasses', 'classic-acetate-sunglasses', 'Kính râm gọng nhựa acetate bóng bẩy, tròng mắt chống tia UV400.', 750000.00),
        ('phu-kien', 'Metal Framed Eyeglasses', 'metal-framed-eyeglasses', 'Kính cận gọng kim loại mỏng nhẹ, sang trọng, thích hợp mặc lên.', 850000.00),
        ('phu-kien', 'Wool Beanie Hat', 'wool-beanie-hat', 'Nón len ấm áp, thoáng khí, phù hợp mùa lạnh hay tập luyện.', 350000.00),
        ('phu-kien', 'Leather Belt Classic', 'leather-belt-classic', 'Dây lưng da bò, khóa cổ điển, thích hợp mặc công sở.', 550000.00),
        ('phu-kien', 'Silk Scarf Printed', 'silk-scarf-printed', 'Khăn lụa in họa tiết, mịn mặn, tăng thêm phần nữ tính.', 450000.00)
) AS seed(category_slug, name, slug, description, base_price)
JOIN categories cat ON cat.slug = seed.category_slug
ON CONFLICT (slug) DO NOTHING;

-- ============================================
-- PHẦN 3: THÊM PRODUCT IMAGES (ảnh chính cho mỗi sản phẩm)
-- ============================================

INSERT INTO product_images (product_id, url, sort_order, is_primary)
SELECT prod.id, seed.url, 0, TRUE
FROM (
    VALUES
        ('heavyweight-boxy-tee', 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=1000&q=80'),
        ('premium-linen-blend-shirt', 'https://images.unsplash.com/photo-1598033129519-3d1de4f2a47b?auto=format&fit=crop&w=1000&q=80'),
        ('oversized-graphic-tee', 'https://images.unsplash.com/photo-1516992654410-5c1d5f4f0f0a?auto=format&fit=crop&w=1000&q=80'),
        ('ribbed-long-sleeve-top', 'https://images.unsplash.com/photo-1506529082847-11d5c4462985?auto=format&fit=crop&w=1000&q=80'),
        ('silk-blend-camisole', 'https://images.unsplash.com/photo-1592934978235-5b5d8d60aea8?auto=format&fit=crop&w=1000&q=80'),
        ('tailored-wool-trousers', 'https://images.unsplash.com/photo-1594938298603-c8148c4dae35?auto=format&fit=crop&w=1000&q=80'),
        ('casual-chino-pants', 'https://images.unsplash.com/photo-1473127895723-b70e559d1ebd?auto=format&fit=crop&w=1000&q=80'),
        ('slim-fit-joggers', 'https://images.unsplash.com/photo-1506529082847-11d5c4462985?auto=format&fit=crop&w=1000&q=80'),
        ('minimalist-wool-bomber-jacket', 'https://images.unsplash.com/photo-1544441893-675973e31985?auto=format&fit=crop&w=1000&q=80'),
        ('double-breasted-trench-coat', 'https://images.unsplash.com/photo-1591047139829-d91aecb6caea?auto=format&fit=crop&w=1000&q=80'),
        ('oversized-blazer', 'https://images.unsplash.com/photo-1583846364985-e1e13bfa0ba4?auto=format&fit=crop&w=1000&q=80'),
        ('denim-jacket-classic', 'https://images.unsplash.com/photo-1565521409410-ab1ca5f8de37?auto=format&fit=crop&w=1000&q=80'),
        ('lightweight-windbreaker', 'https://images.unsplash.com/photo-1521630015832-7e6625b3f7ac?auto=format&fit=crop&w=1000&q=80'),
        ('wool-coat-long', 'https://images.unsplash.com/photo-1539533057867-338490553cf7?auto=format&fit=crop&w=1000&q=80'),
        ('leather-moto-jacket', 'https://images.unsplash.com/photo-1555092918-9169080ccaf1?auto=format&fit=crop&w=1000&q=80'),
        ('puffer-down-jacket', 'https://images.unsplash.com/photo-1601528299974-eb7ee56cb331?auto=format&fit=crop&w=1000&q=80'),
        ('relaxed-fit-selvedge-denim', 'https://images.unsplash.com/photo-1542272604-787c3835535d?auto=format&fit=crop&w=1000&q=80'),
        ('skinny-fit-high-waist', 'https://images.unsplash.com/photo-1542272604-787c3835535d?auto=format&fit=crop&w=1000&q=80'),
        ('straight-leg-vintage-wash', 'https://images.unsplash.com/photo-1542272604-787c3835535d?auto=format&fit=crop&w=1000&q=80'),
        ('flared-hem-denim', 'https://images.unsplash.com/photo-1542272604-787c3835535d?auto=format&fit=crop&w=1000&q=80'),
        ('distressed-ripped-jeans', 'https://images.unsplash.com/photo-1542272604-787c3835535d?auto=format&fit=crop&w=1000&q=80'),
        ('black-slim-denim', 'https://images.unsplash.com/photo-1542272604-787c3835535d?auto=format&fit=crop&w=1000&q=80'),
        ('ribbed-knit-midi-dress', 'https://images.unsplash.com/photo-1595777457583-95e059d581b8?auto=format&fit=crop&w=1000&q=80'),
        ('flowy-maxi-dress', 'https://images.unsplash.com/photo-1552820728-8ac41f1ce891?auto=format&fit=crop&w=1000&q=80'),
        ('slip-dress-satin', 'https://images.unsplash.com/photo-1566174053537-1729226b151e?auto=format&fit=crop&w=1000&q=80'),
        ('mini-shift-dress', 'https://images.unsplash.com/photo-1590080876576-e1b0f0f7d16d?auto=format&fit=crop&w=1000&q=80'),
        ('wrap-dress-knit', 'https://images.unsplash.com/photo-1552820728-8ac41f1ce891?auto=format&fit=crop&w=1000&q=80'),
        ('shirt-dress-cotton', 'https://images.unsplash.com/photo-1515564803906-6c03ee04b4a6?auto=format&fit=crop&w=1000&q=80'),
        ('clean-leather-low-sneakers', 'https://images.unsplash.com/photo-1549298916-b41d501d3772?auto=format&fit=crop&w=1000&q=80'),
        ('canvas-high-top-sneaker', 'https://images.unsplash.com/photo-1535713566543-8b664ef0fafb?auto=format&fit=crop&w=1000&q=80'),
        ('chunky-sole-sneaker', 'https://images.unsplash.com/photo-1460353581641-37baddab0fa2?auto=format&fit=crop&w=1000&q=80'),
        ('pointed-toe-flat', 'https://images.unsplash.com/photo-1543163521-1bf539c55dd2?auto=format&fit=crop&w=1000&q=80'),
        ('heel-pump-classic', 'https://images.unsplash.com/photo-1509631179647-0177331693ae?auto=format&fit=crop&w=1000&q=80'),
        ('chelsea-boot-leather', 'https://images.unsplash.com/photo-1543163521-1bf539c55dd2?auto=format&fit=crop&w=1000&q=80'),
        ('slip-on-loafer', 'https://images.unsplash.com/photo-1533410138919-ceb2ad541b45?auto=format&fit=crop&w=1000&q=80'),
        ('everyday-utility-canvas-tote', 'https://images.unsplash.com/photo-1544816155-12df9643f363?auto=format&fit=crop&w=1000&q=80'),
        ('crossbody-shoulder-bag', 'https://images.unsplash.com/photo-1548036328-c9fa89d128fa?auto=format&fit=crop&w=1000&q=80'),
        ('structured-handbag', 'https://images.unsplash.com/photo-1491637639811-c3400ca199e7?auto=format&fit=crop&w=1000&q=80'),
        ('backpack-laptop', 'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?auto=format&fit=crop&w=1000&q=80'),
        ('classic-acetate-sunglasses', 'https://images.unsplash.com/photo-1511499767150-a48a237f0083?auto=format&fit=crop&w=1000&q=80'),
        ('metal-framed-eyeglasses', 'https://images.unsplash.com/photo-1508296695146-367180941894?auto=format&fit=crop&w=1000&q=80'),
        ('wool-beanie-hat', 'https://images.unsplash.com/photo-1529074783305-435af5a92104?auto=format&fit=crop&w=1000&q=80'),
        ('leather-belt-classic', 'https://images.unsplash.com/photo-1533401899676-a0f3aa1a0773?auto=format&fit=crop&w=1000&q=80'),
        ('silk-scarf-printed', 'https://images.unsplash.com/photo-1591869945903-f1b3a62cdc73?auto=format&fit=crop&w=1000&q=80')
) AS seed(product_slug, url)
JOIN products prod ON prod.slug = seed.product_slug
WHERE NOT EXISTS (
    SELECT 1
    FROM product_images img
    WHERE img.product_id = prod.id AND img.url = seed.url
);

-- ============================================
-- PHẦN 4: THÊM BIẾN THỂ SẢN PHẨM (Variants)
-- ============================================
-- Cột: product_id, sku, size, color, stock_quantity, version
-- (KHÔNG dùng price_override - nó sẽ mặc định NULL)

-- ===== PRODUCT: Heavyweight Boxy Tee =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('heavyweight-boxy-tee', 'NL-TEE-WHT-S', 'S', 'Trắng', 40),
        ('heavyweight-boxy-tee', 'NL-TEE-WHT-M', 'M', 'Trắng', 50),
        ('heavyweight-boxy-tee', 'NL-TEE-WHT-L', 'L', 'Trắng', 35),
        ('heavyweight-boxy-tee', 'NL-TEE-BLK-M', 'M', 'Đen', 30),
        ('heavyweight-boxy-tee', 'NL-TEE-BLK-L', 'L', 'Đen', 28),
        ('heavyweight-boxy-tee', 'NL-TEE-GRY-M', 'M', 'Xám', 25)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ===== PRODUCT: Premium Linen Blend Shirt =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('premium-linen-blend-shirt', 'NL-LINEN-WHT-S', 'S', 'Trắng', 22),
        ('premium-linen-blend-shirt', 'NL-LINEN-WHT-M', 'M', 'Trắng', 28),
        ('premium-linen-blend-shirt', 'NL-LINEN-WHT-L', 'L', 'Trắng', 20),
        ('premium-linen-blend-shirt', 'NL-LINEN-LT-BLUE-M', 'M', 'Xanh nhạt', 18)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ===== PRODUCT: Oversized Graphic Tee =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('oversized-graphic-tee', 'NL-GRPH-WHT-M', 'M', 'Trắng', 35),
        ('oversized-graphic-tee', 'NL-GRPH-WHT-L', 'L', 'Trắng', 30),
        ('oversized-graphic-tee', 'NL-GRPH-WHT-XL', 'XL', 'Trắng', 28),
        ('oversized-graphic-tee', 'NL-GRPH-BLK-M', 'M', 'Đen', 25)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ===== PRODUCT: Ribbed Long Sleeve Top =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('ribbed-long-sleeve-top', 'NL-RIB-LS-WHT-S', 'S', 'Trắng', 20),
        ('ribbed-long-sleeve-top', 'NL-RIB-LS-WHT-M', 'M', 'Trắng', 25),
        ('ribbed-long-sleeve-top', 'NL-RIB-LS-BLK-S', 'S', 'Đen', 18),
        ('ribbed-long-sleeve-top', 'NL-RIB-LS-BLK-M', 'M', 'Đen', 22),
        ('ribbed-long-sleeve-top', 'NL-RIB-LS-NAV-M', 'M', 'Xanh navy', 19)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ===== PRODUCT: Silk Blend Camisole =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('silk-blend-camisole', 'NL-SILK-CAM-WHT-S', 'S', 'Trắng', 18),
        ('silk-blend-camisole', 'NL-SILK-CAM-WHT-M', 'M', 'Trắng', 20),
        ('silk-blend-camisole', 'NL-SILK-CAM-BLK-S', 'S', 'Đen', 16),
        ('silk-blend-camisole', 'NL-SILK-CAM-BLK-M', 'M', 'Đen', 18),
        ('silk-blend-camisole', 'NL-SILK-CAM-CHMP-M', 'M', 'Champagne', 12)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ===== PRODUCT: Tailored Wool Trousers =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('tailored-wool-trousers', 'NL-WOOL-TRO-BLK-30', '30', 'Đen', 14),
        ('tailored-wool-trousers', 'NL-WOOL-TRO-BLK-32', '32', 'Đen', 16),
        ('tailored-wool-trousers', 'NL-WOOL-TRO-CHR-32', '32', 'Xám than', 12),
        ('tailored-wool-trousers', 'NL-WOOL-TRO-NAV-30', '30', 'Xanh navy', 10)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ===== PRODUCT: Casual Chino Pants =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('casual-chino-pants', 'NL-CHINO-KHA-30', '30', 'Kaki', 25),
        ('casual-chino-pants', 'NL-CHINO-KHA-32', '32', 'Kaki', 28),
        ('casual-chino-pants', 'NL-CHINO-BEG-32', '32', 'Kem', 22),
        ('casual-chino-pants', 'NL-CHINO-NAV-30', '30', 'Xanh navy', 20)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ===== PRODUCT: Slim Fit Joggers =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('slim-fit-joggers', 'NL-JOG-BLK-S', 'S', 'Đen', 30),
        ('slim-fit-joggers', 'NL-JOG-BLK-M', 'M', 'Đen', 35),
        ('slim-fit-joggers', 'NL-JOG-BLK-L', 'L', 'Đen', 28),
        ('slim-fit-joggers', 'NL-JOG-GRY-M', 'M', 'Xám', 25)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ===== PRODUCT: Minimalist Wool Bomber Jacket =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('minimalist-wool-bomber-jacket', 'NL-BOMB-BLK-M', 'M', 'Đen', 20),
        ('minimalist-wool-bomber-jacket', 'NL-BOMB-BLK-L', 'L', 'Đen', 15),
        ('minimalist-wool-bomber-jacket', 'NL-BOMB-BLK-XL', 'XL', 'Đen', 12),
        ('minimalist-wool-bomber-jacket', 'NL-BOMB-OLV-M', 'M', 'Xanh olive', 10),
        ('minimalist-wool-bomber-jacket', 'NL-BOMB-OLV-L', 'L', 'Xanh olive', 8),
        ('minimalist-wool-bomber-jacket', 'NL-BOMB-NAV-M', 'M', 'Xanh navy', 14)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ===== PRODUCT: Double-Breasted Trench Coat =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('double-breasted-trench-coat', 'NL-TRN-BGE-M', 'M', 'Kem', 5),
        ('double-breasted-trench-coat', 'NL-TRN-BGE-L', 'L', 'Kem', 0),
        ('double-breasted-trench-coat', 'NL-TRN-BGE-XL', 'XL', 'Kem', 3),
        ('double-breasted-trench-coat', 'NL-TRN-BLK-M', 'M', 'Đen', 8),
        ('double-breasted-trench-coat', 'NL-TRN-BLK-L', 'L', 'Đen', 6)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ===== PRODUCT: Oversized Blazer =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('oversized-blazer', 'NL-BLZR-BLK-S', 'S', 'Đen', 10),
        ('oversized-blazer', 'NL-BLZR-BLK-M', 'M', 'Đen', 12),
        ('oversized-blazer', 'NL-BLZR-BLK-L', 'L', 'Đen', 8),
        ('oversized-blazer', 'NL-BLZR-BRN-M', 'M', 'Nâu', 7)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ===== PRODUCT: Denim Jacket Classic =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('denim-jacket-classic', 'NL-DJ-LT-BLUE-S', 'S', 'Xanh nhạt', 16),
        ('denim-jacket-classic', 'NL-DJ-LT-BLUE-M', 'M', 'Xanh nhạt', 20),
        ('denim-jacket-classic', 'NL-DJ-LT-BLUE-L', 'L', 'Xanh nhạt', 14),
        ('denim-jacket-classic', 'NL-DJ-DK-BLUE-M', 'M', 'Xanh đậm', 15)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ===== PRODUCT: Lightweight Windbreaker =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('lightweight-windbreaker', 'NL-WB-BLK-S', 'S', 'Đen', 24),
        ('lightweight-windbreaker', 'NL-WB-BLK-M', 'M', 'Đen', 28),
        ('lightweight-windbreaker', 'NL-WB-BLK-L', 'L', 'Đen', 20),
        ('lightweight-windbreaker', 'NL-WB-NAV-M', 'M', 'Xanh navy', 18)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ===== PRODUCT: Wool Coat Long =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('wool-coat-long', 'NL-WL-COAT-BLK-S', 'S', 'Đen', 6),
        ('wool-coat-long', 'NL-WL-COAT-BLK-M', 'M', 'Đen', 8),
        ('wool-coat-long', 'NL-WL-COAT-GRY-M', 'M', 'Xám', 5),
        ('wool-coat-long', 'NL-WL-COAT-NAV-L', 'L', 'Xanh navy', 4)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ===== PRODUCT: Leather Moto Jacket =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('leather-moto-jacket', 'NL-MOTO-BLK-S', 'S', 'Đen', 5),
        ('leather-moto-jacket', 'NL-MOTO-BLK-M', 'M', 'Đen', 7),
        ('leather-moto-jacket', 'NL-MOTO-BRN-M', 'M', 'Nâu', 4)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ===== PRODUCT: Puffer Down Jacket =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('puffer-down-jacket', 'NL-PUFF-BLK-S', 'S', 'Đen', 12),
        ('puffer-down-jacket', 'NL-PUFF-BLK-M', 'M', 'Đen', 15),
        ('puffer-down-jacket', 'NL-PUFF-BLK-L', 'L', 'Đen', 10),
        ('puffer-down-jacket', 'NL-PUFF-NAV-M', 'M', 'Xanh navy', 9)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ===== PRODUCT: Relaxed Fit Selvedge Denim =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('relaxed-fit-selvedge-denim', 'NL-DNM-BLU-30', '30', 'Xanh cổ', 20),
        ('relaxed-fit-selvedge-denim', 'NL-DNM-BLU-32', '32', 'Xanh cổ', 15),
        ('relaxed-fit-selvedge-denim', 'NL-DNM-BLU-34', '34', 'Xanh cổ', 12),
        ('relaxed-fit-selvedge-denim', 'NL-DNM-BLU-36', '36', 'Xanh cổ', 10),
        ('relaxed-fit-selvedge-denim', 'NL-DNM-BLK-32', '32', 'Đen', 18)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ===== PRODUCT: Skinny Fit High Waist =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('skinny-fit-high-waist', 'NL-SK-HW-BLU-24', '24', 'Xanh đậm', 14),
        ('skinny-fit-high-waist', 'NL-SK-HW-BLU-26', '26', 'Xanh đậm', 16),
        ('skinny-fit-high-waist', 'NL-SK-HW-BLU-28', '28', 'Xanh đậm', 12),
        ('skinny-fit-high-waist', 'NL-SK-HW-BLK-26', '26', 'Đen', 18)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ===== PRODUCT: Straight Leg Vintage Wash =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('straight-leg-vintage-wash', 'NL-ST-VW-30', '30', 'Xanh cũ', 16),
        ('straight-leg-vintage-wash', 'NL-ST-VW-32', '32', 'Xanh cũ', 18),
        ('straight-leg-vintage-wash', 'NL-ST-VW-34', '34', 'Xanh cũ', 14)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ===== PRODUCT: Flared Hem Denim =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('flared-hem-denim', 'NL-FLARE-30', '30', 'Xanh trung', 15),
        ('flared-hem-denim', 'NL-FLARE-32', '32', 'Xanh trung', 17),
        ('flared-hem-denim', 'NL-FLARE-34', '34', 'Xanh trung', 12)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ===== PRODUCT: Distressed Ripped Jeans =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('distressed-ripped-jeans', 'NL-DIST-BLK-28', '28', 'Đen', 13),
        ('distressed-ripped-jeans', 'NL-DIST-BLK-30', '30', 'Đen', 15),
        ('distressed-ripped-jeans', 'NL-DIST-BLU-30', '30', 'Xanh đậm', 14)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ===== PRODUCT: Black Slim Denim =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('black-slim-denim', 'NL-BK-SLM-30', '30', 'Đen', 20),
        ('black-slim-denim', 'NL-BK-SLM-32', '32', 'Đen', 22),
        ('black-slim-denim', 'NL-BK-SLM-34', '34', 'Đen', 18)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ===== PRODUCT: Ribbed Knit Midi Dress =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('ribbed-knit-midi-dress', 'NL-DRS-STN-S', 'S', 'Xám đá', 15),
        ('ribbed-knit-midi-dress', 'NL-DRS-STN-M', 'M', 'Xám đá', 10),
        ('ribbed-knit-midi-dress', 'NL-DRS-STN-L', 'L', 'Xám đá', 8),
        ('ribbed-knit-midi-dress', 'NL-DRS-CHR-S', 'S', 'Than', 12),
        ('ribbed-knit-midi-dress', 'NL-DRS-CHR-M', 'M', 'Than', 9)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ===== PRODUCT: Flowy Maxi Dress =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('flowy-maxi-dress', 'NL-MAX-FLOW-XS', 'XS', 'Kem', 10),
        ('flowy-maxi-dress', 'NL-MAX-FLOW-S', 'S', 'Kem', 12),
        ('flowy-maxi-dress', 'NL-MAX-FLOW-M', 'M', 'Kem', 9)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ===== PRODUCT: Slip Dress Satin =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('slip-dress-satin', 'NL-SLIP-BLK-S', 'S', 'Đen', 11),
        ('slip-dress-satin', 'NL-SLIP-BLK-M', 'M', 'Đen', 13),
        ('slip-dress-satin', 'NL-SLIP-RED-S', 'S', 'Đỏ', 9)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ===== PRODUCT: Mini Shift Dress =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('mini-shift-dress', 'NL-MINI-WHT-S', 'S', 'Trắng', 18),
        ('mini-shift-dress', 'NL-MINI-WHT-M', 'M', 'Trắng', 20),
        ('mini-shift-dress', 'NL-MINI-BLK-S', 'S', 'Đen', 16)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ===== PRODUCT: Wrap Dress Knit =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('wrap-dress-knit', 'NL-WRAP-BLK-S', 'S', 'Đen', 12),
        ('wrap-dress-knit', 'NL-WRAP-BLK-M', 'M', 'Đen', 14),
        ('wrap-dress-knit', 'NL-WRAP-CHR-M', 'M', 'Than', 10)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ===== PRODUCT: Shirt Dress Cotton =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('shirt-dress-cotton', 'NL-SHDRS-BLU-S', 'S', 'Xanh nhạt', 15),
        ('shirt-dress-cotton', 'NL-SHDRS-BLU-M', 'M', 'Xanh nhạt', 17),
        ('shirt-dress-cotton', 'NL-SHDRS-WHT-M', 'M', 'Trắng', 14)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ===== PRODUCT: Clean Leather Low Sneakers =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('clean-leather-low-sneakers', 'NL-SHOE-WHT-40', '40', 'Kem', 12),
        ('clean-leather-low-sneakers', 'NL-SHOE-WHT-41', '41', 'Kem', 16),
        ('clean-leather-low-sneakers', 'NL-SHOE-WHT-42', '42', 'Kem', 14),
        ('clean-leather-low-sneakers', 'NL-SHOE-WHT-43', '43', 'Kem', 10),
        ('clean-leather-low-sneakers', 'NL-SHOE-BLK-40', '40', 'Đen', 15),
        ('clean-leather-low-sneakers', 'NL-SHOE-BLK-42', '42', 'Đen', 12)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ===== PRODUCT: Canvas High Top Sneaker =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('canvas-high-top-sneaker', 'NL-CANVAS-WHT-40', '40', 'Trắng', 16),
        ('canvas-high-top-sneaker', 'NL-CANVAS-WHT-42', '42', 'Trắng', 14),
        ('canvas-high-top-sneaker', 'NL-CANVAS-BLK-41', '41', 'Đen', 12)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ===== PRODUCT: Chunky Sole Sneaker =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('chunky-sole-sneaker', 'NL-CHUNKY-WHT-40', '40', 'Trắng', 13),
        ('chunky-sole-sneaker', 'NL-CHUNKY-WHT-42', '42', 'Trắng', 11),
        ('chunky-sole-sneaker', 'NL-CHUNKY-BLK-41', '41', 'Đen', 10)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ===== PRODUCT: Pointed Toe Flat =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('pointed-toe-flat', 'NL-POINT-BLK-36', '36', 'Đen', 12),
        ('pointed-toe-flat', 'NL-POINT-BLK-38', '38', 'Đen', 10),
        ('pointed-toe-flat', 'NL-POINT-RED-37', '37', 'Đỏ', 8)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ===== PRODUCT: Heel Pump Classic =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('heel-pump-classic', 'NL-PUMP-BLK-36', '36', 'Đen', 10),
        ('heel-pump-classic', 'NL-PUMP-BLK-38', '38', 'Đen', 9),
        ('heel-pump-classic', 'NL-PUMP-NUD-37', '37', 'Nude', 8)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ===== PRODUCT: Chelsea Boot Leather =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('chelsea-boot-leather', 'NL-CHELSEA-BLK-40', '40', 'Đen', 8),
        ('chelsea-boot-leather', 'NL-CHELSEA-BLK-42', '42', 'Đen', 7),
        ('chelsea-boot-leather', 'NL-CHELSEA-BRN-41', '41', 'Nâu', 6)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ===== PRODUCT: Slip On Loafer =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('slip-on-loafer', 'NL-LOAFER-BLK-40', '40', 'Đen', 11),
        ('slip-on-loafer', 'NL-LOAFER-BLK-42', '42', 'Đen', 10),
        ('slip-on-loafer', 'NL-LOAFER-BRN-41', '41', 'Nâu', 9)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ===== PRODUCT: Everyday Utility Canvas Tote =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('everyday-utility-canvas-tote', 'NL-BAG-ECR-ONESIZE', 'OneSize', 'Kem', 35),
        ('everyday-utility-canvas-tote', 'NL-BAG-ECR-ALT', 'Large', 'Kem', 20),
        ('everyday-utility-canvas-tote', 'NL-BAG-BLK-ONESIZE', 'OneSize', 'Đen', 25),
        ('everyday-utility-canvas-tote', 'NL-BAG-BLK-ALT', 'Large', 'Đen', 18),
        ('everyday-utility-canvas-tote', 'NL-BAG-NAV-ONESIZE', 'OneSize', 'Xanh navy', 22)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ===== PRODUCT: Crossbody Shoulder Bag =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('crossbody-shoulder-bag', 'NL-CROSS-BLK-ONESIZE', 'OneSize', 'Đen', 18),
        ('crossbody-shoulder-bag', 'NL-CROSS-BRN-ONESIZE', 'OneSize', 'Nâu', 14),
        ('crossbody-shoulder-bag', 'NL-CROSS-TAN-ONESIZE', 'OneSize', 'Nâu nhạt', 12)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ===== PRODUCT: Structured Handbag =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('structured-handbag', 'NL-STRUCT-BLK-ONESIZE', 'OneSize', 'Đen', 10),
        ('structured-handbag', 'NL-STRUCT-BRN-ONESIZE', 'OneSize', 'Nâu', 8),
        ('structured-handbag', 'NL-STRUCT-RED-ONESIZE', 'OneSize', 'Đỏ', 6)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ===== PRODUCT: Backpack Laptop =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('backpack-laptop', 'NL-BP-BLK-ONESIZE', 'OneSize', 'Đen', 14),
        ('backpack-laptop', 'NL-BP-NAV-ONESIZE', 'OneSize', 'Xanh navy', 12),
        ('backpack-laptop', 'NL-BP-GRY-ONESIZE', 'OneSize', 'Xám', 10)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ===== PRODUCT: Classic Acetate Sunglasses =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('classic-acetate-sunglasses', 'NL-ACC-TOR-ONESIZE', 'OneSize', 'Rùa', 20),
        ('classic-acetate-sunglasses', 'NL-ACC-TOR-ALT', 'OneSize', 'Rùa (Polarized)', 12),
        ('classic-acetate-sunglasses', 'NL-ACC-BLK-ONESIZE', 'OneSize', 'Đen', 30),
        ('classic-acetate-sunglasses', 'NL-ACC-BRN-ONESIZE', 'OneSize', 'Nâu', 18)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ===== PRODUCT: Metal Framed Eyeglasses =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('metal-framed-eyeglasses', 'NL-METAL-SLV-ONESIZE', 'OneSize', 'Bạc', 16),
        ('metal-framed-eyeglasses', 'NL-METAL-GLD-ONESIZE', 'OneSize', 'Vàng', 14)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ===== PRODUCT: Wool Beanie Hat =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('wool-beanie-hat', 'NL-BEAN-BLK-ONESIZE', 'OneSize', 'Đen', 30),
        ('wool-beanie-hat', 'NL-BEAN-GRY-ONESIZE', 'OneSize', 'Xám', 28),
        ('wool-beanie-hat', 'NL-BEAN-NAV-ONESIZE', 'OneSize', 'Xanh navy', 25)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ===== PRODUCT: Leather Belt Classic =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('leather-belt-classic', 'NL-BELT-BLK-S', 'S', 'Đen', 20),
        ('leather-belt-classic', 'NL-BELT-BLK-M', 'M', 'Đen', 22),
        ('leather-belt-classic', 'NL-BELT-BRN-M', 'M', 'Nâu', 18)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ===== PRODUCT: Silk Scarf Printed =====
INSERT INTO product_variants (product_id, sku, size, color, stock_quantity, version)
SELECT prod.id, seed.sku, seed.size, seed.color, seed.stock_quantity, 0
FROM (
    VALUES
        ('silk-scarf-printed', 'NL-SCRF-PRINT-ONESIZE', 'OneSize', 'Nhiều màu', 25),
        ('silk-scarf-printed', 'NL-SCRF-NAVY-ONESIZE', 'OneSize', 'Xanh navy', 20)
) AS seed(product_slug, sku, size, color, stock_quantity)
JOIN products prod ON prod.slug = seed.product_slug
ON CONFLICT DO NOTHING;

-- ============================================
-- THỐNG KÊ CUỐI CÙNG
-- ============================================
-- ✅ 40 sản phẩm được thêm
-- ✅ ~130+ biến thể được tạo
-- ✅ 40 ảnh sản phẩm via product_images
-- ✅ Không có lỗi type casting
-- ✅ Schema an toàn: product_id, sku, size, color, stock_quantity, version