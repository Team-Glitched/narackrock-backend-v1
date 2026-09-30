#!/usr/bin/env bash
set -euo pipefail

timeout_seconds="${1:-180}"
interval_seconds=5
elapsed=0

while (( elapsed < timeout_seconds )); do
    container_id="$(docker compose --env-file deploy/env/.env -f deploy/build/docker-compose.yml ps -q backend)"
    if [[ -z "$container_id" ]]; then
        echo '[health-check] Backend container is missing.' >&2
        exit 1
    fi

    status="$(docker inspect --format '{{.State.Health.Status}}' "$container_id")"
    case "$status" in
        healthy)
            echo '[health-check] Backend is healthy.'
            exit 0
            ;;
        unhealthy)
            docker logs --tail 50 "$container_id" >&2 || true
            echo '[health-check] Backend is unhealthy.' >&2
            exit 1
            ;;
    esac

    sleep "$interval_seconds"
    elapsed=$((elapsed + interval_seconds))
done

echo "[health-check] Backend did not become healthy within ${timeout_seconds}s." >&2
docker compose --env-file deploy/env/.env -f deploy/build/docker-compose.yml logs --tail 50 backend >&2 || true
exit 1
