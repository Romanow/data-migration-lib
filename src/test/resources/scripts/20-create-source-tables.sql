-- 20 Create source tables
CREATE TABLE users
(
    id           SERIAL PRIMARY KEY,
    name         VARCHAR(80) NOT NULL,
    age          INT CHECK (age BETWEEN 18 AND 99),
    location     VARCHAR(255),
    created_date VARCHAR(80)
);
