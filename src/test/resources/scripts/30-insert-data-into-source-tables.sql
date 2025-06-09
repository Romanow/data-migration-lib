-- 30 Insert data into source tables
SELECT *
INTO names
FROM (VALUES ('Alex')
           , ('Mike')
           , ('Jack')
           , ('Harry')
           , ('Jacob')
           , ('Charlie')
           , ('Thomas')
           , ('George')
           , ('Oscar')
           , ('James')
           , ('William')) AS names(name);

SELECT *
INTO countries
FROM (VALUES ('Russia')
           , ('USA')
           , ('Great Britain')
           , ('Mexico')
           , ('Canada')
           , ('Germany')
           , ('France')
           , ('Brazil')
           , ('China')
           , ('Japan')
           , ('Denmark')) AS countries(name);

INSERT INTO users (name, age, location, created_date)
SELECT (SELECT name FROM names ORDER BY RANDOM() LIMIT 1)                              AS name
     , FLOOR(81 * RANDOM() + 18)                                                       AS age
     , (SELECT name FROM countries ORDER BY RANDOM() LIMIT 1)                          AS location
     , TO_CHAR(NOW() - (RANDOM() * INTERVAL '12 month'), 'YYYY-MM-DD"T"HH24:MI:SS"Z"') AS created_date
FROM GENERATE_SERIES(1, 20000) AS s(id);
