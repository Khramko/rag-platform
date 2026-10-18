#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE DATABASE user_db;
    CREATE DATABASE doc_db;
    CREATE DATABASE vec_db;
    CREATE DATABASE chat_db;
EOSQL

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname vec_db <<-EOSQL
    CREATE EXTENSION IF NOT EXISTS vector;
EOSQL