# 로컬 실행과 배포

## 로컬 실행

백엔드는 PostgreSQL과 Redis가 필요하다. 접속할 인스턴스를 준비한 뒤 예시 파일을 복사해 값을 채운다. `.env.local`은 Git에서 제외되며 실제 비밀값은 이 파일에만 둔다.

```shell
cp .env.local.example .env.local
vi .env.local
./gradlew bootRun
```

애플리케이션은 `./.env`와 `./.env.local`을 순서대로 읽는다(`spring.config.import`). `APP_AUTH_TOKEN_SECRET`은 기본값이 없어 반드시 설정해야 한다. Google 로그인에는 `GOOGLE_CLIENT_ID`가 필요하다. 그 밖의 키는 `src/main/resources/application.properties`의 기본값을 사용한다.

## 컨테이너 이미지

루트의 `Dockerfile`로 실행용 이미지를 만든다. 빌드 컨텍스트 제외 목록은 `Dockerfile.dockerignore`이며 `.env*` 파일은 이미지에 들어가지 않는다.

```shell
docker build -f Dockerfile -t narackrock-backend .
```

컨테이너는 UID/GID `10001`의 비루트 사용자로 실행되고 8080 포트를 사용한다. 환경변수는 로컬 실행과 같다. 업로드 경로(`ADLIPS_UPLOAD_DIR`)는 쓰기 가능한 볼륨으로 연결한다.

## 배포

`main`에 push하면 `.github/workflows/deploy.yml`이 다음을 수행한다.

1. 테스트를 실행한다.
2. `ghcr.io/team-glitched/narackrock-backend-v1`에 이미지를 게시한다.
3. [`parafara/narackrock-deploy`](https://github.com/parafara/narackrock-deploy)의 Deployment 이미지를 digest로 고정해 커밋한다.

Argo CD가 그 커밋을 k3s에 배포한다. 운영 환경변수는 Vault `secret/narackrock/backend`에서 주입된다. 필요한 키 목록은 배포 저장소 README에 있다.

## 업로드 파일

업로드 파일은 `ADLIPS_UPLOAD_DIR`(기본 `~/.adlips/uploads`) 아래에 저장하고 `/files/**` 경로로 제공한다.

- 프로필 이미지: `profiles/{userId}/...`
- 일반 이미지: `images/{userId}/...`
- 오디오: `audio/{userId}/...`
- 비디오: `videos/{userId}/...`
- waveform JSON: `waveforms/{userId}/...`

## 조회수 집계

숏폼과 게시글 조회수는 Redis에서 먼저 중복을 제거하고 증가분을 모은 뒤 PostgreSQL에 주기적으로 반영한다.

- `POST /api/v1/shorts/{shortId}/views`
- `POST /api/v1/posts/{postId}/views`
- 로그인 요청은 `Authorization` 헤더의 사용자 ID를 기준으로 중복을 판단한다.
- 비로그인 요청은 `X-Device-Id` 헤더가 필요하며, 원문 대신 SHA-256 해시만 Redis 키에 사용한다.
- 기본 중복 방지 시간은 30분이고, API는 `202 Accepted`를 반환한다.

필요하면 환경변수로 다음 설정을 바꾼다.

```properties
REDIS_VIEW_DEDUPLICATION_WINDOW=PT30M
REDIS_VIEW_SYNC_ENABLED=true
REDIS_VIEW_SYNC_INTERVAL=PT10S
REDIS_VIEW_SYNC_INITIAL_DELAY=PT10S
REDIS_VIEW_SYNC_BATCH_SIZE=100
```
