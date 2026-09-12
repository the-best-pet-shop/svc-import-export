#!/usr/bin/env bash
set -euo pipefail
rg -q 'queued|running|rolled_back' docs/openapi.yaml
rg -q 'schemaVersion|organizationId|payload' docs/asyncapi.yaml
rg -q 'Idempotency-Key|checksum|uploadSignature' docs/openapi.yaml
