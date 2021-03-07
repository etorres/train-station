#!/usr/bin/env bash

set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER train_station WITH PASSWORD 'changeme';
    CREATE DATABASE train_station;
    GRANT ALL PRIVILEGES ON DATABASE train_station TO train_station;
    \connect train_station train_station
    DO \$\$
    DECLARE
      schema_names TEXT[] := ARRAY['public', 'test_expected_trains'];
      schema_name TEXT;
    BEGIN
      FOREACH schema_name IN ARRAY schema_names
      LOOP
        EXECUTE 'CREATE SCHEMA IF NOT EXISTS ' || quote_ident(schema_name);
        EXECUTE 'CREATE TABLE IF NOT EXISTS ' || quote_ident(schema_name) || '.expected_trains (
          id SERIAL PRIMARY KEY,
          train_id VARCHAR(24) NOT NULL,
          origin VARCHAR(24) NOT NULL,
          expected TIMESTAMP WITH TIME ZONE NOT NULL
        )';
      END LOOP;
    END\$\$;
EOSQL