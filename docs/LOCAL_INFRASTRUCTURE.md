# Compose 배포

`compose.yml` 하나로 Spring Boot 애플리케이션, PostgreSQL, Redis를 함께 실행한다.

## 구성

- `backend`: Spring Boot 애플리케이션
- `postgres`: PostgreSQL 데이터베이스
- `redis`: 조회수 및 반응 캐시

세 서비스는 `adlips-application` Docker 네트워크에서 통신한다. 애플리케이션은 `postgres:5432`와 `redis:6379`로 연결하며, PostgreSQL과 Redis 포트는 호스트에 공개하지 않는다.

## 실행

운영 비밀값은 Git에 커밋하지 않는다. 예시 파일을 복사한 뒤 서버에서 실제 값을 설정한다.

```shell
cp compose.env.example compose.env
vi compose.env
sudo install -d -o 10001 -g 10001 /var/lib/adlips/storage
docker compose --env-file compose.env up -d --build
```

기본적으로 애플리케이션 포트는 `127.0.0.1:8080`에만 바인딩되어 같은 서버의 리버스 프록시를 통해 공개할 수 있다. 직접 포트를 공개해야 한다면 `ADLIPS_APP_BIND_ADDRESS`를 의도한 인터페이스 주소로 변경한다.

상태와 로그 확인:

```shell
docker compose --env-file compose.env ps
docker compose --env-file compose.env logs -f backend
```

PostgreSQL과 Redis 상태 확인:

```shell
docker compose --env-file compose.env exec postgres \
  pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB"
docker compose --env-file compose.env exec redis \
  redis-cli --no-auth-warning -a "$REDIS_PASSWORD" ping
```

## IntelliJ에서 로컬 인프라 사용

백엔드는 IntelliJ에서 실행하고 PostgreSQL과 Redis만 컨테이너로 실행할 때는
로컬 오버라이드 파일을 함께 사용한다. 포트는 외부 네트워크가 아닌
`127.0.0.1`에만 공개된다.

```shell
cp compose.env.example compose.env
cp .env.local.example .env.local
docker compose --env-file compose.env \
  -f compose.yml -f compose.local.yml \
  up -d postgres redis
```

`compose.env`의 `POSTGRES_PASSWORD`, `REDIS_PASSWORD`와 `.env.local`의 대응하는
비밀번호는 반드시 같아야 한다. IntelliJ에서 실행한 애플리케이션은 `.env`를 먼저
읽고 `.env.local`의 로컬 DB/Redis 설정으로 덮어쓴다.
기본 로컬 포트는 PostgreSQL `15432`, Redis `16379`이며 `compose.env`의
`POSTGRES_LOCAL_PORT`, `REDIS_LOCAL_PORT`로 변경할 수 있다.

상태 확인과 종료:

```shell
docker compose --env-file compose.env \
  -f compose.yml -f compose.local.yml ps
docker compose --env-file compose.env \
  -f compose.yml -f compose.local.yml down
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

필요하면 `compose.env`에 다음 설정을 추가한다.

```properties
REDIS_VIEW_DEDUPLICATION_WINDOW=PT30M
REDIS_VIEW_SYNC_ENABLED=true
REDIS_VIEW_SYNC_INTERVAL=PT10S
REDIS_VIEW_SYNC_INITIAL_DELAY=PT10S
REDIS_VIEW_SYNC_BATCH_SIZE=100
```
