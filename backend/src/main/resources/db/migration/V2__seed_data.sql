-- V2: Du lieu mau de test nhanh
INSERT INTO category (name, sort_order) VALUES
    ('Cà phê', 1),
    ('Trà', 2),
    ('Bánh', 3);

INSERT INTO menu_item (category_id, name, description, price, available) VALUES
    ((SELECT id FROM category WHERE name = 'Cà phê'), 'Cà phê đen', 'Đậm đà',           25000, TRUE),
    ((SELECT id FROM category WHERE name = 'Cà phê'), 'Cà phê sữa', 'Béo ngậy',         30000, TRUE),
    ((SELECT id FROM category WHERE name = 'Cà phê'), 'Bạc xỉu',    'Nhiều sữa',        35000, TRUE),
    ((SELECT id FROM category WHERE name = 'Trà'),    'Trà đào',    'Mát lạnh',         40000, TRUE),
    ((SELECT id FROM category WHERE name = 'Trà'),    'Trà sữa',    'Trân châu',        45000, TRUE),
    ((SELECT id FROM category WHERE name = 'Bánh'),   'Bánh mì',    'Pa-tê',            20000, TRUE),
    ((SELECT id FROM category WHERE name = 'Bánh'),   'Croissant',  'Bơ',               35000, FALSE);
