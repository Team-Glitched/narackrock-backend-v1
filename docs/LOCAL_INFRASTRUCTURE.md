# Compose 배포

`deploy/build/docker-compose.yml` 하나로 Spring Boot 애플리케이션, PostgreSQL, Redis를 함께 실행한다.

## 구성

- `backend`: Spring Boot 애플리케이션
- `postgres`: PostgreSQL 데이터베이스
- `redis`: 조회수 및 반응 캐시

세 서비스는 `adlips-application` Docker 네트워크에서 통신한다. 애플리케이션은 `postgres:5432`와 `redis:6379`로 연결한다. PostgreSQL과 Redis 포트는 로컬 개발용으로 `127.0.0.1`에만 바인딩된다.

## 실행

운영 비밀값은 Git에 커밋하지 않는다. 예시 파일을 복사한 뒤 서버에서 실제 값을 설정한다.

```shell
cp deploy/env/.env.example deploy/env/.env
vi deploy/env/.env
sudo install -d -o 10001 -g 10001 /var/lib/adlips/storage
docker compose --env-file deploy/env/.env -f deploy/build/docker-compose.yml up -d --build
```

## Jenkins 자동 배포

`kolog-BE`의 Jenkins 빌드·실행·상태 확인·롤백 흐름을 이 저장소의 Compose 구성에 맞춰 적용했다.
Jenkins job은 **Pipeline script from SCM**으로 설정하고, 브랜치를 배포 대상 브랜치로 지정하며,
Script Path는 `deploy/script/Jenkinsfile`로 설정한다. 배포 서버의 Jenkins 에이전트에
`adlips-deploy` 라벨을 지정하고,
Docker 데몬 및 Docker Compose v2에 접근할 수 있어야 한다. 같은 Compose 프로젝트를 수동으로
동시에 변경하지 않는다.

Jenkins Credentials에 운영 `deploy/env/.env`를 **Secret file**로 등록한다.
Credential ID는 `adlips-be-compose-env`이다. `deploy/env/.env.example`을 참고하여 실제 값을 채우고,
`ADLIPS_APP_IMAGE=adlips-backend:latest`를 유지한다. 업로드 저장 경로는 서버에 미리 생성한다.

파이프라인은 체크아웃 후 운영 환경 파일을 준비하고, 기존 실행 컨테이너의 이미지를
`adlips-backend:rollback`으로 보관한 뒤 새 이미지를 빌드한다. 컨테이너를 재생성하고
`/actuator/health`가 Docker에서 `healthy`가 될 때까지 기다린다. 실행 또는 상태 확인이
실패하면 이전 이미지를 다시 실행하고 상태를 확인한다. 최초 배포처럼 이전 컨테이너가
없으면 복원할 이미지도 없다. 빌드 실패 시에는 실행 중인 컨테이너를 건드리지 않는다.
실패한 빌드의 운영 환경 파일은 작업 종료 시 워크스페이스에서 삭제된다.

`/actuator/health`는 DB와 Redis를 포함한 Spring Boot 상태 확인에 사용한다.
애플리케이션 포트는 기본적으로 `127.0.0.1`에만 바인딩된다.

기본적으로 애플리케이션 포트는 `127.0.0.1:8080`에만 바인딩되어 같은 서버의 리버스 프록시를 통해 공개할 수 있다. 직접 포트를 공개해야 한다면 `ADLIPS_APP_BIND_ADDRESS`를 의도한 인터페이스 주소로 변경한다.

상태와 로그 확인:

```shell
docker compose --env-file deploy/env/.env -f deploy/build/docker-compose.yml ps
docker compose --env-file deploy/env/.env -f deploy/build/docker-compose.yml logs -f backend
```

PostgreSQL과 Redis 상태 확인:

```shell
docker compose --env-file deploy/env/.env -f deploy/build/docker-compose.yml exec postgres \
  pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB"
docker compose --env-file deploy/env/.env -f deploy/build/docker-compose.yml exec redis \
  redis-cli --no-auth-warning -a "$REDIS_PASSWORD" ping
```

## IntelliJ에서 로컬 인프라 사용

백엔드는 IntelliJ에서 실행하고 PostgreSQL과 Redis만 컨테이너로 실행할 수 있다.
포트는 외부 네트워크가 아닌 `127.0.0.1`에만 공개된다.

```shell
cp deploy/env/.env.example deploy/env/.env
cp .env.local.example .env.local
docker compose --env-file deploy/env/.env \
  -f deploy/build/docker-compose.yml \
  up -d postgres redis
```

`deploy/env/.env`의 `POSTGRES_PASSWORD`, `REDIS_PASSWORD`와 `.env.local`의 대응하는
비밀번호는 반드시 같아야 한다. IntelliJ에서 실행한 애플리케이션은 `.env`를 먼저
읽고 `.env.local`의 로컬 DB/Redis 설정으로 덮어쓴다.
기본 로컬 포트는 PostgreSQL `15432`, Redis `16379`이며 `deploy/env/.env`의
`POSTGRES_LOCAL_PORT`, `REDIS_LOCAL_PORT`로 변경할 수 있다.

상태 확인과 종료:

```shell
docker compose --env-file deploy/env/.env \
  -f deploy/build/docker-compose.yml ps
docker compose --env-file deploy/env/.env \
  -f deploy/build/docker-compose.yml down
```

## 데이터 보존

PostgreSQL과 Redis는 각각 Docker volume에 데이터를 보존한다.

- PostgreSQL: `adlips-postgres-data`
- Redis: `adlips-redis-data` (AOF `everysec`)

업로드 파일은 `ADLIPS_STORAGE_PATH`의 서버 디렉터리에 저장한다. 해당 경로는 컨테이너의 `/var/lib/adlips/storage`에 연결된다. 컨테이너는 UID/GID `10001`의 비루트 사용자로 실행되므로 서버 저장 경로에도 해당 사용자의 쓰기 권한이 필요하다.

- 프로필 이미지: `profiles/{userId}/...`
- 일반 이미지: `images/{userId}/...`
- 오디오: `audio/{userId}/...`
- 비디오: `videos/{userId}/...`
- waveform JSON: `waveforms/{userId}/...`

파일 URL은 `/files/**` 경로로 제공된다.

## 조회수 집계

숏폼과 게시글 조회수는 Redis에서 먼저 중복을 제거하고 증가분을 모은 뒤 PostgreSQL에 주기적으로 반영한다.

- `POST /api/v1/shorts/{shortId}/views`
- `POST /api/v1/posts/{postId}/views`
- 로그인 요청은 `Authorization` 헤더의 사용자 ID를 기준으로 중복을 판단한다.
- 비로그인 요청은 `X-Device-Id` 헤더가 필요하며, 원문 대신 SHA-256 해시만 Redis 키에 사용한다.
- 기본 중복 방지 시간은 30분이고, API는 `202 Accepted`를 반환한다.

필요하면 `deploy/env/.env`에 다음 설정을 추가한다.

```properties
REDIS_VIEW_DEDUPLICATION_WINDOW=PT30M
REDIS_VIEW_SYNC_ENABLED=true
REDIS_VIEW_SYNC_INTERVAL=PT10S
REDIS_VIEW_SYNC_INITIAL_DELAY=PT10S
REDIS_VIEW_SYNC_BATCH_SIZE=100
```
