#!/usr/bin/env bash

set -e

export PGPASSWORD=postgres
psql -U program -d target <<-EOSQL
  CREATE TABLE users
  (
      uid      UUID PRIMARY KEY,
      name     VARCHAR(80) NOT NULL,
      age      INT CHECK (age BETWEEN 18 AND 99),
      location VARCHAR(255)
  );
EOSQL
