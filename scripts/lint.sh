#!/usr/bin/env bash
set -euo pipefail
test -f pom.xml
test -f docs/openapi.yaml
test -f docs/asyncapi.yaml
./mvnw -q test -DskipTests
