-- file: 10-create-user-and-db.sql
CREATE USER program WITH PASSWORD 'test';
CREATE DATABASE source WITH OWNER program;
CREATE DATABASE target WITH OWNER program;
