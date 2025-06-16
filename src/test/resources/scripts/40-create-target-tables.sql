-- 40 Create target tables
CREATE TABLE users
(
    uid          UUID PRIMARY KEY,
    solve_id     UUID         NOT NULL,
    name         VARCHAR(80)  NOT NULL,
    age          INT CHECK (age BETWEEN 18 AND 99),
    location     VARCHAR(255) NOT NULL,
    created_date TIMESTAMP    NOT NULL,
    checksum     VARCHAR(80)  NOT NULL
);

CREATE TABLE operations
(
    process_id UUID PRIMARY KEY,
    solve_id   UUID        NOT NULL,
    type       VARCHAR(10) NOT NULL
        CHECK ( type IN ('ADD', 'MODIFY', 'REMOVE') ),
    started_at TIMESTAMP   NOT NULL,
    started_by VARCHAR(80) NOT NULL
);
