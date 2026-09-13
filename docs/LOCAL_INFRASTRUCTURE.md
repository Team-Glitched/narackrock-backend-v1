# 로컬 인프라 실행

## 파일 저장소

현재 파일은 애플리케이션 서버의 로컬 디스크에 저장한다. kolog 프로젝트와 같은 방식으로 외부 저장 경로와 공개 URL을 환경 변수로 주입한다.

```properties
ADLIPS_UPLOAD_DIR=/var/lib/adlips/storage
ADLIPS_PUBLIC_FILE_URL=https://api.example.com/files
```

- 프로필 이미지: `profiles/{userId}/...`
- 일반 이미지: `images/{userId}/...`
- 오디오: `audio/{userId}/...`
- 비디오: `videos/{userId}/...`
- waveform JSON: `waveforms/{userId}/...`

컨테이너로 애플리케이션을 실행할 때는 `ADLIPS_UPLOAD_DIR` 경로를 Docker volume 또는 서버의 영속 디렉터리에 연결해야 한다. 파일 URL은 `/files/**` 경로로 제공된다.

S3 설정은 `application.properties`에 비활성 예시로 남겨 두었다. 버킷과 자격 증명이 준비된 이후 S3 어댑터 구현과 함께 활성화한다.

## Redis

Redis 컨테이너를 시작한다.

```shell
docker compose -f compose.redis.yml up -d
```

연결 확인:

```shell
docker exec adlips-redis redis-cli -a adlips-local ping
```

기본 개발 설정:

```properties
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=adlips-local
```

사설 서버에서는 반드시 `REDIS_PASSWORD`를 별도 비밀값으로 변경한다. Redis 포트는 기본 설정상 서버의 `127.0.0.1`에만 공개된다. AOF는 Docker volume `adlips-redis-data`에 저장된다.
