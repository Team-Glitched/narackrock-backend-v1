# 보낸 참여 요청 조회

`GET /api/v1/users/me/contributions`는 인증된 사용자가 보낸 요청을 프로젝트에 관계없이 최신 요청 ID 순으로 조회한다. 사용자 ID는 Bearer 토큰에서 확인하며 클라이언트가 지정하지 않는다. 삭제된 프로젝트는 제외한다. 데이터베이스 스키마 변경은 없다.

- `cursor`: 선택. 이전 응답의 `page.nextCursor`. 양의 요청 ID.
- `size`: 기본 20, 범위 1~100.
- `data.items`: `contributionId`, `projectId`, `projectTitle`, `description`, `approvalStatus`, `createdAt`.
- `data.page`: `nextCursor`, `hasMore`, `size`. `hasMore`가 true일 때만 다음 페이지를 요청한다.
- 상태는 `PENDING`, `APPROVED`, `REJECTED` 모두 포함한다. 브라우저 방문 기록은 필요하지 않다.
- 인증 실패는 401, 잘못된 조회 조건은 기존 프로젝트 API의 검증 오류 응답을 사용한다.

기존 `GET /api/v1/projects/{projectId}/contributions`도 기본 조회에 세 상태를 모두 포함하며 `status=REJECTED`를 허용한다. 명시적인 `status=PENDING` 조회는 기존과 동일하다. 프로젝트 소유자·편집자는 기존 권한대로 프로젝트 요청을 조회하고, 조회자(VIEWER)는 본인의 요청만 조회한다. 프로젝트 상세와 트랙 조회 권한은 변경하지 않는다.

프론트 PR #69의 보낸 요청 목록은 이 API를 사용한다. 백엔드를 먼저 반영한 뒤 프론트를 배포해야 한다.
