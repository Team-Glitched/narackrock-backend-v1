# 샘플 악기 정보

샘플의 `instrument`는 작곡에서 사용하는 악기 또는 녹음 파트 ID다. 피아노롤에서 생성된 오디오의 음색과 별도로 DB에 저장해 샘플을 다시 조회할 때도 파트를 식별한다.

- `POST /api/v1/sources`: 선택한 `instrument`를 저장한다. 기존 클라이언트의 생략 또는 `null`은 미지정으로 저장한다.
- `PATCH /api/v1/sources/{id}`: 생략하면 유지하고, `null`이면 미지정으로 변경한다. 기존 소유자 권한 검사를 적용한다.
- 공개 목록·인기 목록·상세, 내 등록 목록·관리 상세, 보관함의 샘플 정보에 `instrument`를 반환한다. 기존 데이터는 `null`이다.
- `InstrumentFamily`의 작곡 악기와 녹음 파트(`vocal`, `backing-vocal`, `rap`, `percussion`, `other`)를 허용한다. 기존 작곡과 같은 별칭 정규화를 적용하고, 빈 문자열·알 수 없는 값·문자열 이외의 값은 400으로 거부한다.
- 기존 `/open-sources` 별칭 경로에도 동일하게 적용한다.

배포 시 `ddl-auto=update`는 nullable 컬럼을 추가한다. `validate` 또는 `none` 환경에서는 [source-instruments.sql](source-instruments.sql)을 먼저 적용한다. 기존 오디오에서 악기를 추정해 채우지 않는다. 백엔드를 먼저 배포한 다음 프론트엔드를 배포한다.

검증은 `OpenSourcePublicationIntegrationTest`에서 HTTP 요청부터 DB 저장과 각 조회 응답까지 확인한다. 프론트엔드는 생성 요청 전송과 응답 손실 후 재시도 시 악기 값 동기화를 검증한다.
