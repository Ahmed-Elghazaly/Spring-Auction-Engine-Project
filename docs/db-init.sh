#!/bin/bash


SCHEMA_FILE="/opt/bidforge/schema.sql"
PDB="${ORACLE_DATABASE:-FREEPDB1}"

echo "CONTAINER: applying ${SCHEMA_FILE} as ${APP_USER}@${PDB}"

sqlplus -s "${APP_USER}/${APP_USER_PASSWORD}@localhost/${PDB}" <<SQL
WHENEVER SQLERROR EXIT SQL.SQLCODE
@${SCHEMA_FILE}
exit
SQL

if [ $? -ne 0 ]; then
  echo "CONTAINER: ERROR: schema.sql failed to apply." >&2
else
  echo "CONTAINER: schema applied to ${APP_USER}@${PDB}"
fi
