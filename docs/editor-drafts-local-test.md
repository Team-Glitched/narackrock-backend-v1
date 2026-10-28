# 개인 초안 실제 TEST 연동 검증

검증일: 2026-10-28 (한국 시간)

## 브랜치와 실행 환경

- 백엔드: `TEST`에 `feature/composition-drafts`의 6개 커밋을 cherry-pick했다. 기존 로컬 테스트 인증·H2 설정을 유지했다.
- 프론트엔드: `feature/composition-drafts`, `317e167`.
- 실제 프론트 주소: `http://localhost:5173/shorts/902`.
- 실제 Spring Boot API: `http://127.0.0.1:8081`, `local-test` 프로필.
- 독립 H2 DB: `/private/tmp/adlips-drafts-live.2fRpVs/database.mv.db`.
- 테스트 업로드/렌더링 파일: `/private/tmp/adlips-drafts-live.2fRpVs/uploads`.
- 전용 Redis: `127.0.0.1:16389`, 컨테이너 `adlips-drafts-live-redis`.
- 프론트는 `NARACKROCK_API_PROXY_TARGET=http://127.0.0.1:8081 NEXT_PUBLIC_API_BASE_URL=`로 실행했다.

기존 DB를 초기화하지 않았다. 별도 DB에 TEST의 사용자 1·2를 생성하고, 실제 프로젝트·트랙·MIDI 생성·기여 승인·음원 발행 API로 원곡을 준비했다. 프로젝트 1의 원곡자는 사용자 2, 작곡 참여자는 사용자 1이다. 테스트 숏폼은 902다. 동시 저장 검증은 별도 프로젝트 2에서 진행했다.

브라우저 응답에 모의 데이터를 주입하지 않았다. 정상 앱의 숏폼 → 작곡 참여 → 악기 선택 → 피아노롤 → 스튜디오 → 제출 → 알림 승인 흐름을 사용했다. 브라우저의 사용자 ID 확인에는 `docs/TEST_AUTH.md`의 로컬 세션 방식만 사용했고, 검증용 세션 진입 HTML은 제품 코드에 포함하지 않았다.

## 실제 API와 DB 확인

| 항목 | 결과 |
| --- | --- |
| Authorization 없는 로컬 요청 | TEST 필터를 통해 200 |
| 노트 3개 자동 저장 | 개인 초안에만 저장, 참여자 실제 클립 0개 |
| 원곡 데이터 | 원곡 노트 4개 유지 |
| 다른 계정의 개인 초안 목록 | 0개 |
| 다른 계정이 초안 UUID로 직접 조회 | 404 |
| 동시에 서로 다른 UUID로 같은 대상 최초 저장 | 200 / 409, ACTIVE 초안 1개 |
| 동일 revision에서 두 요청으로 동시 수정 | 200 / 409 |
| 성공한 mutationId 재전송 | 같은 revision 2 반환 |
| 모든 노트 삭제 | 빈 노트 배열을 DB에 저장하고 복원 |
| 명시적 저장 버튼 | 실제 DRAFT 클립 1개 생성, 초안 COMMITTED |
| 초안 버리기 | 편집 초안만 제거되고 이미 저장된 3개 노트 유지 |
| 최신 복구본 제출 | 레이어·클립 각각 1개, 노트 5개, 모두 PENDING |
| 제출 후 늦은 자동 저장 | 403으로 거부, 초안/클립 재생성 없음 |
| 완료된 초안의 commit 재시도 | 200, 동일 클립 ID 반환 |
| 원곡자의 실제 UI 승인 | 레이어·클립 APPROVED, 승인 대기 목록에서 제거 |

## 실제 브라우저 확인

1. 숏폼의 작곡 참여하기에서 피아노 레이어를 만들었다.
2. 노트를 넣기 전에 뒤로 나갔다가 같은 빈 레이어로 재진입했다. 두 번째 레이어는 생성되지 않았다.
3. 노트 3개를 입력한 직후 뒤로가기를 눌렀다. 개인 초안 이어하기가 표시됐다.
4. 실제 페이지를 새로고침하고 같은 숏폼에서 다시 참여해 노트 3개를 복원했다.
5. 키보드로 노트 이동·길이 변경·삭제 후 저장된 tick/길이를 실제 API로 대조했다.
6. 방금 생성한 테스트 초안의 브라우저 복구본만 비우고, 서버 초안만으로 위치·길이를 복원했다.
7. 다른 기기의 수정을 실제 API로 먼저 저장한 뒤 오래된 브라우저 편집을 시도했다. 409 충돌 화면이 표시됐으며 서버 데이터는 덮어써지지 않았다. 서버 작업 불러오기로 해당 내용을 복원했다.
8. 모든 노트를 삭제한 빈 초안을 저장·재진입해 빈 상태가 유지되는 것을 확인했다.
9. 다시 입력한 노트 3개를 저장 버튼으로 실제 클립에 반영했다. 이후 개인 초안에만 추가한 노트를 버렸을 때 실제 클립은 3개 노트를 유지했다.
10. 실제 백엔드 프로세스를 종료하고 다섯 번째 노트를 입력했다. 이 기기에 저장됨 상태가 표시됐고 브라우저 복구본을 남긴 채 나갈 수 있었다. 서버 중단 중 스튜디오의 서버 조회는 실패했다.
11. 같은 DB로 백엔드를 재시작했다. 페이지 새로고침 → 동일 숏폼 재진입 → 이어하기로 노트 5개를 복원하고 실제 서버에 재동기화했다.
12. 복구된 최신 작업을 제출하고 원래 숏폼으로 돌아왔다. 원곡자 계정의 알림에 입력한 설명과 2개 기여 항목이 표시됐다.
13. 원곡자 UI에서 승인했다. 실제 새 믹스 `/files/exports/1/2/mix.wav`가 생성됐으며 프론트 audio 요소의 readyState=4, duration=3.5초, error=null로 로드됐다.

## 자동화 검증

초안 커밋을 반영한 TEST에서 `./gradlew test`: **1,053개 통과, 실패·오류·건너뜀 0개**.

이번 실제 연동 검증에서 제품 코드의 추가 수정은 필요하지 않았다. 기존 기능 브랜치의 프론트 테스트 82개와 타입·린트·빌드 검증 결과도 유지된다.

## 재실행 방법

독립된 새 DB/파일 경로와 사용하지 않는 Redis 포트를 지정한다. 아래 경로는 검증에 사용한 경로이며, 기존 테스트 결과를 다시 읽을 수도 있다.

```sh
./gradlew bootRun --args='--spring.profiles.active=local-test --spring.config.import= --server.address=127.0.0.1 --spring.datasource.url=jdbc:h2:file:/private/tmp/adlips-drafts-live.2fRpVs/database;MODE=MySQL;DATABASE_TO_LOWER=TRUE --spring.data.redis.host=127.0.0.1 --spring.data.redis.port=16389 --spring.data.redis.password= --app.storage.local-root=/private/tmp/adlips-drafts-live.2fRpVs/uploads --app.storage.public-base-url=http://localhost:8081/files --app.shorts.share-base-url=http://localhost:5173/shorts'
```

H2 기반 실제 연동 검증이다. PostgreSQL 운영 환경에서의 검증이나 운영 데이터 변경은 포함하지 않는다. 기존 원격 서비스의 인증 설정도 변경하지 않았다.
