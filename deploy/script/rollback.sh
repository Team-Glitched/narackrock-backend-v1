#!/usr/bin/env bash
set -euo pipefail

if [[ ! -f ".deploy-started-${BUILD_NUMBER:?BUILD_NUMBER is required}" ]]; then
    echo '[rollback] Deployment did not start; the running container was not changed.'
    exit 0
fi

if [[ ! -f ".deploy-rollback-ready-$BUILD_NUMBER" ]]; then
    echo '[rollback] No previously running backend image is available.' >&2
    exit 0
fi

docker tag adlips-backend:rollback adlips-backend:latest
docker compose --env-file deploy/env/.env -f deploy/build/docker-compose.yml up -d --no-build --force-recreate backend
bash deploy/script/health-check.sh 180
echo '[rollback] Previous backend image restored.'
