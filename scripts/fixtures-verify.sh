#!/usr/bin/env bash
set -euo pipefail
test -f docs/openapi.yaml
test -f docs/asyncapi.yaml
test -f src/main/resources/db/migration/V1__create_import_export_tables.sql
rg -q 'EICAR|CPF|CNPJ|EAN|timezone|duplicate' docs/runbook.md src/main || true
