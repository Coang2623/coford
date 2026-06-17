-- V3: co danh dau don da pha che xong (cho man hinh bep)
ALTER TABLE orders ADD COLUMN prepared BOOLEAN NOT NULL DEFAULT FALSE;
