# TEST 브랜치 로컬 로그인 우회

`TEST` 브랜치에서만 로컬 개발 요청에 테스트 사용자의 인증 헤더를 자동으로 붙일 수 있습니다.

```properties
ADLIPS_TEST_AUTH_ENABLED=true
ADLIPS_TEST_USER_ID=1
```

`ADLIPS_TEST_USER_ID`는 DB에 이미 존재하는 활성 사용자 ID로 설정합니다. 요청이 서버의 루프백 주소(`127.0.0.1` 또는 `::1`)에서 들어오고 `Authorization` 헤더가 없을 때만 적용됩니다. 설정하지 않으면 기본값은 `false`이며 정상 인증 흐름을 사용합니다. 기존 헤더가 있거나 외부 주소에서 들어온 요청은 우회하지 않습니다.

가장 간단한 방법은 `TEST` 브랜치에서 `./gradlew bootRun --args='--spring.profiles.active=local-test'`를 실행하는 것입니다. 포트 `8081`에서 독립된 H2 DB(`/tmp/adlips-composition-local-test.mv.db`)를 사용하고 테스트 사용자 ID `1`을 자동 생성합니다. 프론트 개발 서버는 `NEXT_PUBLIC_API_BASE_URL=http://localhost:8081`로 실행하세요.

수동으로 테스트 인증을 켤 때는 반드시 로컬 PostgreSQL 또는 H2 데이터베이스를 사용해야 합니다. 원격 데이터베이스 URL이면 서버가 시작되지 않습니다. `.env`가 원격 DB를 가리킬 수 있으므로 `SPRING_DATASOURCE_URL`을 로컬 DB로 명시하고 테스트 사용자도 그 DB에 생성하세요.

프론트는 별도의 테스트용 토큰 로직 없이 평소처럼 API를 호출하면 됩니다. 기능 브랜치 `Feature/composition-midi-read`에는 이 우회가 포함되지 않습니다.
