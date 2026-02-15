SET @TARGET_USERS = 100000;      -- 생성할 users 수
SET @TARGET_FOODS = 1000000;     -- 생성할 foods 수
SET @TARGET_NUTRITIONS = 800000; -- 생성할 nutritions 수 (foods 일부)
SET @TARGET_MENUS = 2000000;     -- 생성할 menus 수

SET SQL_SAFE_UPDATES = 0;
SET FOREIGN_KEY_CHECKS = 0;
SET UNIQUE_CHECKS = 0;

-- =============================
-- 0) 숫자 시퀀스 임시 테이블 생성
-- =============================
DROP TEMPORARY TABLE IF EXISTS tmp_seq_1m;
CREATE TEMPORARY TABLE tmp_seq_1m (
  n INT NOT NULL PRIMARY KEY
) ENGINE=InnoDB;

INSERT INTO tmp_seq_1m(n)
SELECT d6.d*100000 + d5.d*10000 + d4.d*1000 + d3.d*100 + d2.d*10 + d1.d + 1 AS n
FROM (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL
      SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d1
         CROSS JOIN (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL
                     SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d2
         CROSS JOIN (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL
                     SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d3
         CROSS JOIN (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL
                     SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d4
         CROSS JOIN (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL
                     SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d5
         CROSS JOIN (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL
                     SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d6;

-- =============================
-- 1) users 생성
-- 테이블: users(user_name, password, email, role, failed_login_count, locked_until)
-- =============================
INSERT INTO users (user_name, password, email, role, failed_login_count, locked_until)
SELECT
    CONCAT('load_user_', LPAD(n, 6, '0')) AS user_name,
    '$2a$10$8R5r2I5Z8u6n1wRzYkH8I.NxA6nD3m4X9q4n5l6v7k8y1z2a3b4c5' AS password,
    CONCAT('load_user_', LPAD(n, 6, '0'), '@example.com') AS email,
    'USER' AS role,
    0 AS failed_login_count,
    NULL AS locked_until
FROM tmp_seq_1m
WHERE n <= @TARGET_USERS;

-- =============================
-- 2) foods 생성
-- 테이블: foods(name UNIQUE, category, img_url, serving_sizeg)
-- 핫키 분포: 상위 10개 키워드(60%) + 롱테일(40%)
-- =============================
INSERT INTO foods (name, category, img_url, serving_sizeg)
SELECT
    CASE
        WHEN MOD(n, 10) < 6 THEN CONCAT(
                ELT((MOD(n, 6) + 1), '김치찌개','제육볶음','돈까스','비빔밥','된장찌개','햄버거'),
                '_', LPAD(n, 7, '0')
                                 )
        ELSE CONCAT('food_longtail_', LPAD(n, 7, '0'))
        END AS name,
    ELT((MOD(n, 8) + 1), '한식', '중식', '일식', '양식', '분식', '패스트푸드', '샐러드', '기타') AS category,
    CONCAT('https://cdn.example.com/foods/', n, '.jpg') AS img_url,
    80 + MOD(n, 320) AS serving_sizeg
FROM tmp_seq_1m
WHERE n <= @TARGET_FOODS;

-- =============================
-- 3) nutritions 생성
-- 테이블: nutritions(food_id PK/FK, calories, carbohydrate, protein, fat)
-- food_id 1..TARGET_NUTRITIONS 전제(빈 DB에서 순차 생성 시 성립)
-- =============================
INSERT INTO nutritions (food_id, calories, carbohydrate, protein, fat)
SELECT
    n+10 AS food_id,
    ROUND(150 + MOD(n * 17, 650) + (MOD(n, 100) / 100), 2) AS calories,
    ROUND(8 + MOD(n * 13, 90) + (MOD(n, 100) / 100), 2) AS carbohydrate,
    ROUND(3 + MOD(n * 7, 60) + (MOD(n, 100) / 100), 2) AS protein,
    ROUND(2 + MOD(n * 11, 45) + (MOD(n, 100) / 100), 2) AS fat
FROM tmp_seq_1m
WHERE n <= @TARGET_NUTRITIONS;

-- =============================
-- 4) menus 생성
-- 테이블: menus(user_name, menu_name, food_id), UNIQUE(user_name, menu_name)
-- 사용자별 메뉴명 충돌을 피하기 위해 menu_name에 사용자 인덱스를 포함
-- =============================
DROP TEMPORARY TABLE IF EXISTS tmp_seq_2m;
CREATE TEMPORARY TABLE tmp_seq_2m (
  n INT NOT NULL PRIMARY KEY
) ENGINE=InnoDB;

INSERT INTO tmp_seq_2m(n)
SELECT a.n + (b.n - 1) * 1000000
FROM tmp_seq_1m a
         JOIN (SELECT 1 AS n UNION ALL SELECT 2) b;

INSERT INTO menus (user_name, menu_name)
SELECT
    CONCAT('load_user_', LPAD(((n - 1) MOD @TARGET_USERS) + 1, 6, '0')) AS user_name,
    CONCAT('menu_', LPAD(((n - 1) DIV @TARGET_USERS) + 1, 3, '0'), '_', LPAD(((n - 1) MOD @TARGET_USERS) + 1, 6, '0')) AS menu_name
FROM tmp_seq_2m
WHERE n <= @TARGET_MENUS;

SET UNIQUE_CHECKS = 1;
SET FOREIGN_KEY_CHECKS = 1;

ANALYZE TABLE users, foods, nutritions, menus;

SELECT 'seed completed' AS status,
       (SELECT COUNT(*) FROM users) AS users_cnt,
       (SELECT COUNT(*) FROM foods) AS foods_cnt,
       (SELECT COUNT(*) FROM nutritions) AS nutritions_cnt,
       (SELECT COUNT(*) FROM menus) AS menus_cnt;