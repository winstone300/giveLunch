-- Demo seed data for a small EC2 portfolio deployment.
-- Replace @ADMIN_PASSWORD_BCRYPT with a BCrypt hash before running.

SET @ADMIN_USER_NAME = 'admin';
SET @ADMIN_EMAIL = 'admin@example.com';
SET @ADMIN_PASSWORD_BCRYPT = '$2a$10$replace_this_with_a_real_bcrypt_hash';

INSERT INTO users (user_name, password, email, role, failed_login_count, locked_until)
VALUES (@ADMIN_USER_NAME, @ADMIN_PASSWORD_BCRYPT, @ADMIN_EMAIL, 'ADMIN', 0, NULL)
ON DUPLICATE KEY UPDATE
    email = VALUES(email),
    role = 'ADMIN';

INSERT INTO foods (name, category, img_url, serving_sizeg)
VALUES
    ('김치찌개', '한식', NULL, 450),
    ('제육볶음', '한식', NULL, 350),
    ('돈까스', '일식', NULL, 300),
    ('비빔밥', '한식', NULL, 500),
    ('된장찌개', '한식', NULL, 420),
    ('라멘', '일식', NULL, 600),
    ('짜장면', '중식', NULL, 650),
    ('샐러드', '샐러드', NULL, 250),
    ('햄버거', '패스트푸드', NULL, 220),
    ('파스타', '양식', NULL, 420)
ON DUPLICATE KEY UPDATE
    category = VALUES(category),
    img_url = VALUES(img_url),
    serving_sizeg = VALUES(serving_sizeg);

INSERT INTO nutritions (food_id, calories, carbohydrate, protein, fat)
SELECT id, 310.00, 20.00, 18.00, 15.00 FROM foods WHERE name = '김치찌개'
ON DUPLICATE KEY UPDATE calories = VALUES(calories), carbohydrate = VALUES(carbohydrate), protein = VALUES(protein), fat = VALUES(fat);

INSERT INTO nutritions (food_id, calories, carbohydrate, protein, fat)
SELECT id, 520.00, 35.00, 28.00, 25.00 FROM foods WHERE name = '제육볶음'
ON DUPLICATE KEY UPDATE calories = VALUES(calories), carbohydrate = VALUES(carbohydrate), protein = VALUES(protein), fat = VALUES(fat);

INSERT INTO nutritions (food_id, calories, carbohydrate, protein, fat)
SELECT id, 680.00, 62.00, 24.00, 36.00 FROM foods WHERE name = '돈까스'
ON DUPLICATE KEY UPDATE calories = VALUES(calories), carbohydrate = VALUES(carbohydrate), protein = VALUES(protein), fat = VALUES(fat);

INSERT INTO nutritions (food_id, calories, carbohydrate, protein, fat)
SELECT id, 610.00, 88.00, 19.00, 18.00 FROM foods WHERE name = '비빔밥'
ON DUPLICATE KEY UPDATE calories = VALUES(calories), carbohydrate = VALUES(carbohydrate), protein = VALUES(protein), fat = VALUES(fat);

INSERT INTO nutritions (food_id, calories, carbohydrate, protein, fat)
SELECT id, 260.00, 18.00, 15.00, 10.00 FROM foods WHERE name = '된장찌개'
ON DUPLICATE KEY UPDATE calories = VALUES(calories), carbohydrate = VALUES(carbohydrate), protein = VALUES(protein), fat = VALUES(fat);

INSERT INTO nutritions (food_id, calories, carbohydrate, protein, fat)
SELECT id, 720.00, 82.00, 30.00, 28.00 FROM foods WHERE name = '라멘'
ON DUPLICATE KEY UPDATE calories = VALUES(calories), carbohydrate = VALUES(carbohydrate), protein = VALUES(protein), fat = VALUES(fat);

INSERT INTO nutritions (food_id, calories, carbohydrate, protein, fat)
SELECT id, 780.00, 105.00, 22.00, 24.00 FROM foods WHERE name = '짜장면'
ON DUPLICATE KEY UPDATE calories = VALUES(calories), carbohydrate = VALUES(carbohydrate), protein = VALUES(protein), fat = VALUES(fat);

INSERT INTO nutritions (food_id, calories, carbohydrate, protein, fat)
SELECT id, 180.00, 16.00, 9.00, 8.00 FROM foods WHERE name = '샐러드'
ON DUPLICATE KEY UPDATE calories = VALUES(calories), carbohydrate = VALUES(carbohydrate), protein = VALUES(protein), fat = VALUES(fat);

INSERT INTO nutritions (food_id, calories, carbohydrate, protein, fat)
SELECT id, 560.00, 45.00, 24.00, 30.00 FROM foods WHERE name = '햄버거'
ON DUPLICATE KEY UPDATE calories = VALUES(calories), carbohydrate = VALUES(carbohydrate), protein = VALUES(protein), fat = VALUES(fat);

INSERT INTO nutritions (food_id, calories, carbohydrate, protein, fat)
SELECT id, 640.00, 78.00, 21.00, 23.00 FROM foods WHERE name = '파스타'
ON DUPLICATE KEY UPDATE calories = VALUES(calories), carbohydrate = VALUES(carbohydrate), protein = VALUES(protein), fat = VALUES(fat);
