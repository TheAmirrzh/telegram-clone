#!/usr/bin/env bash
set -e
DB_NAME=telegramdb
SQL_FILE=sql/01_schema.sql
if [ -z "$PGUSER" ]; then export PGUSER=telegram_user; fi
if [ -z "$PGPASSWORD" ]; then export PGPASSWORD=telegram_pass; fi
psql -h localhost -v ON_ERROR_STOP=1 -U "$PGUSER" -d "$DB_NAME" -f "$SQL_FILE"