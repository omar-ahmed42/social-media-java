INSERT INTO role (id, name, created_at)
SELECT * FROM (SELECT 1, 'USER', NOW()) AS tmp
WHERE NOT EXISTS (
    SELECT name FROM role WHERE id = 1
) LIMIT 1;

INSERT INTO role (id, name, created_at)
SELECT * FROM (SELECT 2, 'ADMIN', NOW()) AS tmp
WHERE NOT EXISTS (
    SELECT name FROM role WHERE id = 2
) LIMIT 1;

ALTER TABLE role AUTO_INCREMENT = 2;

INSERT INTO reaction (id, name, created_at) SELECT * FROM (SELECT 1, 'like', NOW()) AS tmp
WHERE NOT EXISTS (
    SELECT name FROM reaction WHERE id = 1
) LIMIT 1;
INSERT INTO reaction (id, name, created_at) SELECT * FROM (SELECT 2, 'love', NOW()) AS tmp
WHERE NOT EXISTS (
    SELECT name FROM reaction WHERE id = 2
) LIMIT 1;

INSERT INTO reaction (id, name, created_at) SELECT * FROM (SELECT 3, 'angry', NOW()) AS tmp
WHERE NOT EXISTS (
    SELECT name FROM reaction WHERE id = 3
) LIMIT 1;

INSERT INTO reaction (id, name, created_at) SELECT * FROM (SELECT 4, 'sad', NOW()) AS tmp
WHERE NOT EXISTS (
    SELECT name FROM reaction WHERE id = 4
) LIMIT 1;

INSERT INTO reaction (id, name, created_at) SELECT * FROM (SELECT 5, 'laugh', NOW()) AS tmp
WHERE NOT EXISTS (
    SELECT name FROM reaction WHERE id = 5
) LIMIT 1;
