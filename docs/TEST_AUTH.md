# TEST 브랜치 로컬 로그인 우회

`TEST` 브랜치에서만 로컬 개발 요청에 테스트 사용자의 인증 헤더를 자동으로 붙일 수 있습니다.

```properties
ADLIPS_TEST_AUTH_ENABLED=true
ADLIPS_TEST_USER_ID=1
```

`ADLIPS_TEST_USER_ID`는 DB에 이미 존재하는 활성 사용자 ID로 설정합니다. 요청이 서버의 루프백 주소(`127.0.0.1` 또는 `::1`)에서 들어오고 `Authorization` 헤더가 없을 때만 적용됩니다. 설정하지 않으면 기본값은 `false`이며 정상 인증 흐름을 사용합니다. 기존 헤더가 있거나 외부 주소에서 들어온 요청은 우회하지 않습니다.

가장 간단한 방법은 `TEST` 브랜치에서 `./gradlew bootRun --args='--spring.profiles.active=local-test'`를 실행하는 것입니다. 포트 `8081`에서 독립된 H2 DB(`/tmp/adlips-composition-local-test.mv.db`)를 사용하고 테스트 사용자 ID `1`, 프로필, 게시글 테스트용 기본 갤러리 ID `1`을 자동 생성합니다. 프론트 개발 서버는 `NEXT_PUBLIC_API_BASE_URL=http://localhost:8081`로 실행하세요.

로컬 테스트용 숏폼 ID `901`과 음원 레코드도 생성합니다. 음원을 브라우저에서 재생하려면 `/tmp/adlips-composition-local-uploads/test-short.wav`에 WAV 파일을 둡니다. 북마크 계정 격리를 확인할 두 번째 사용자 `bookmark-observer@localhost`도 생성합니다. 공유 링크는 로컬 프론트의 `/shorts/901`, `/posts/{id}`로 연결됩니다. 게시글은 기본 갤러리 ID `1`에 작성해 테스트할 수 있습니다.

수동으로 테스트 인증을 켤 때는 반드시 로컬 PostgreSQL 또는 H2 데이터베이스를 사용해야 합니다. 원격 데이터베이스 URL이면 서버가 시작되지 않습니다. `.env`가 원격 DB를 가리킬 수 있으므로 `SPRING_DATASOURCE_URL`을 로컬 DB로 명시하고 테스트 사용자도 그 DB에 생성하세요.

API를 직접 호출할 때는 별도의 토큰이 필요하지 않습니다. 다만 프론트의 게시글 상세와 내 프로필 화면은 브라우저 로그인 상태도 확인합니다. `http://localhost:3000`에서 화면까지 테스트하려면 아래 명령이 출력하는 JavaScript를 브라우저 개발자 도구 콘솔에서 실행하세요. 로컬 테스트 DB의 사용자 1 세션만 저장합니다.

```bash
python3 - <<'PY'
import base64, hashlib, hmac, json, time

secret = b'adlips-local-test-token-secret-32-characters-long'
payload = base64.urlsafe_b64encode(f'1:{int(time.time()) + 3600}'.encode()).decode().rstrip('=')
signature = base64.urlsafe_b64encode(hmac.new(secret, payload.encode(), hashlib.sha256).digest()).decode().rstrip('=')
session = json.dumps({'accessToken': payload + '.' + signature, 'refreshToken': '', 'userId': 1})
print('localStorage.setItem("narackrock.auth-session", ' + json.dumps(session) + '); location.reload();')
PY
```

북마크의 계정 격리를 화면에서 확인하려면 위 코드의 `1`을 모두 `2`로 바꿔 두 번째 테스트 사용자로 접속할 수 있습니다. 기능 브랜치 `Feature/composition-midi-read`에는 이 인증 우회가 포함되지 않습니다.

`local-test` 프로필은 프론트 개발 주소 `http://localhost:3000`,
`http://localhost:5173`, `http://127.0.0.1:5173`의 CORS 요청을 명시적으로 허용합니다.
기본 환경 파일의 CORS 설정과 별개로 로컬 업로드 음원을 Web Audio에서 읽을 수 있게 합니다.
