-- 20 Create source tables
CREATE TABLE users
(
    id           SERIAL PRIMARY KEY,
    name         VARCHAR(80) NOT NULL,
    age          INT CHECK (age BETWEEN 18 AND 99),
    location     VARCHAR(255),
    created_date VARCHAR(80)
);

CREATE TABLE operations
(
    process_id UUID PRIMARY KEY,
    type       VARCHAR(10) NOT NULL
        CHECK ( type IN ('ADD', 'MODIFY', 'REMOVE') ),
    started_at TIMESTAMP   NOT NULL,
    started_by VARCHAR(80) NOT NULL
);
