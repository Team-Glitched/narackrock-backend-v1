# Compose 배포

애플리케이션 서버와 사설 인프라 서버를 서로 독립된 Compose 프로젝트로 실행한다.

- `compose.application.yml`: Spring Boot 애플리케이션
- `compose.infrastructure.yml`: PostgreSQL, Redis

운영 비밀값은 Git에 커밋하지 않는다. 예시 파일을 복사한 뒤 실제 서버에서만 값을 채운다.

## 사설 인프라 서버

`PRIVATE_BIND_ADDRESS`에는 외부 공개 주소가 아닌 사설 네트워크 인터페이스 주소를 입력한다.

```shell
cp infrastructure.env.example infrastructure.env
vi infrastructure.env
docker compose --env-file infrastructure.env -f compose.infrastructure.yml up -d
```

PostgreSQL과 Redis는 각각 Docker volume에 데이터를 보존한다.

- PostgreSQL: `adlips-postgres-data`
- Redis: `adlips-redis-data` (AOF `everysec`)

방화벽에서는 애플리케이션 서버의 사설 IP만 PostgreSQL(기본 `5432`)과 Redis(기본 `6379`)에 접근하도록 허용한다. 두 포트를 공인 인터넷에 공개하지 않는다.

상태 확인:

```shell
docker compose --env-file infrastructure.env -f compose.infrastructure.yml ps
docker compose --env-file infrastructure.env -f compose.infrastructure.yml exec postgres \
  pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB"
docker compose --env-file infrastructure.env -f compose.infrastructure.yml exec redis \
  redis-cli --no-auth-warning -a "$REDIS_PASSWORD" ping
```

## 애플리케이션 서버

`application.env`의 PostgreSQL 및 Redis 호스트에는 사설 인프라 서버 주소를 입력한다. 업로드 파일 경로도 서버의 영속 디렉터리로 지정한다.

```shell
cp application.env.example application.env
vi application.env
sudo install -d -o 10001 -g 10001 /var/lib/adlips/storage
docker compose --env-file application.env -f compose.application.yml up -d --build
```

기본적으로 애플리케이션 포트는 `127.0.0.1:8080`에만 바인딩되어 같은 서버의 리버스 프록시를 통해 공개할 수 있다. 직접 포트를 공개해야 한다면 `ADLIPS_APP_BIND_ADDRESS`를 의도한 인터페이스 주소로 변경한다.

상태와 로그 확인:

```shell
docker compose --env-file application.env -f compose.application.yml ps
docker compose --env-file application.env -f compose.application.yml logs -f backend
```

## 파일 저장소

현재 파일은 애플리케이션 서버의 로컬 디스크에 저장한다. `ADLIPS_STORAGE_PATH`의 서버 디렉터리가 컨테이너의 `/var/lib/adlips/storage`에 연결된다. 컨테이너는 UID/GID `10001`의 비루트 사용자로 실행되므로 서버 저장 경로에도 해당 사용자의 쓰기 권한이 필요하다.

- 프로필 이미지: `profiles/{userId}/...`
- 일반 이미지: `images/{userId}/...`
- 오디오: `audio/{userId}/...`
- 비디오: `videos/{userId}/...`
- waveform JSON: `waveforms/{userId}/...`

파일 URL은 `/files/**` 경로로 제공된다. S3 설정은 `application.properties`에 비활성 예시로 남겨 두었으며, 버킷과 자격 증명이 준비된 이후 S3 어댑터와 함께 활성화한다.

## 조회수 집계

숏폼과 게시글 조회수는 Redis에서 먼저 중복을 제거하고 증가분을 모은 뒤 PostgreSQL에 주기적으로 반영한다.

- `POST /api/v1/shorts/{shortId}/views`
- `POST /api/v1/posts/{postId}/views`
- 로그인 요청은 `Authorization` 헤더의 사용자 ID를 기준으로 중복을 판단한다.
- 비로그인 요청은 `X-Device-Id` 헤더가 필요하며, 원문 대신 SHA-256 해시만 Redis 키에 사용한다.
- 기본 중복 방지 시간은 30분이고, API는 비동기 반영을 의미하는 `202 Accepted`를 반환한다.

운영 환경에서는 필요에 따라 `application.env`에 다음 값을 추가한다.

```properties
REDIS_VIEW_DEDUPLICATION_WINDOW=PT30M
REDIS_VIEW_SYNC_ENABLED=true
REDIS_VIEW_SYNC_INTERVAL=PT10S
REDIS_VIEW_SYNC_INITIAL_DELAY=PT10S
REDIS_VIEW_SYNC_BATCH_SIZE=100
```

DB 반영 배치 ID는 `content_view_sync_batches` 테이블에 기록한다. 서버가 Redis 증가분을 DB에 반영한 직후 중단되더라도 같은 배치가 중복 반영되는 것을 막기 위한 기록이다.
