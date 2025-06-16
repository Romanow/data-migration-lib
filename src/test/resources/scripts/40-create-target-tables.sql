-- 40 Create target tables
CREATE TABLE users
(
    uid          UUID PRIMARY KEY,
    source       VARCHAR(80)  NOT NULL,
    name         VARCHAR(80)  NOT NULL,
    age          INT CHECK (age BETWEEN 18 AND 99),
    location     VARCHAR(255) NOT NULL,
    created_date TIMESTAMP    NOT NULL,
    checksum     VARCHAR(80)  NOT NULL
);
