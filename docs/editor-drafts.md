# 개인 MIDI 작업 초안

피아노롤 자동 저장은 `editor_drafts`에만 기록한다. 기존 프로젝트/트랙/클립과 음원, 프로젝트 버전은 자동 저장으로 변경되지 않는다. 저장 버튼이나 작업 제출, 곡 올리기처럼 명시적인 작업에서만 기존 MIDI 생성·수정 유스케이스를 호출한다. 승인된 MIDI를 명시적으로 저장할 때의 기존 수정 정책은 유지한다.

## API

모든 경로의 접두사는 `/api/v1/projects/{projectId}`이며 Bearer 인증이 필요하다. 사용자 ID는 요청 본문 대신 인증 정보로 결정한다.

| 메서드 | 경로 | 동작 |
| --- | --- | --- |
| GET | `/editor-drafts` | 본인의 ACTIVE 초안 조회 |
| GET | `/editor-drafts/{draftId}` | 본인의 초안 또는 완료 기록 조회 |
| PUT | `/tracks/{trackId}/editor-drafts/{draftId}` | 개인 초안 생성/갱신 |
| POST | `/editor-drafts/{draftId}/commit` | 지정 버전의 초안을 클립에 반영 |
| DELETE | `/editor-drafts/{draftId}?expectedRevision=1` | 지정 버전의 초안 버리기 |
| POST | `/editor-drafts/commit` | 지정한 초안을 한 트랜잭션으로 반영 |
| POST | `/editor-drafts/submit` | 초안 반영과 기존 기여 제출을 한 트랜잭션으로 처리 |

PUT 예시:

```json
{
  "expectedRevision": 0,
  "mutationId": "49beebfb-b986-4ac7-bc66-a35c6cb01e60",
  "targetClipId": null,
  "content": {
    "startTick": 0,
    "durationTick": 480,
    "midiNotes": [
      {"pitch":60,"startTick":0,"durationTick":480,"velocity":100,"effectType":null,"effectParams":{}}
    ]
  },
  "baseClip": null
}
```

`draftId`는 클라이언트가 생성하는 UUID다. 최초 저장은 expectedRevision=0이며 응답 revision=1부터 시작한다. 기존 클립 편집은 targetClipId와 편집 시작 시점의 원본 스냅샷(baseClip)을 함께 전달한다. 초안 내 노트는 클립 기준 상대 tick이며 빈 배열도 유효하다. 노트 수 상한은 10,000개이고 프로젝트 최대 길이를 넘길 수 없다.

단건 commit 본문은 `{"expectedRevision":1}`이다. 일괄 반영/제출 본문은 다음과 같다. 제출 시 기존 본인 DRAFT 클립도 함께 수집한다.

```json
{
  "drafts": [{"draftId":"49beebfb-b986-4ac7-bc66-a35c6cb01e60","expectedRevision":1}],
  "description":"멜로디 추가",
  "baseProjectVersion":{"major":1,"minor":1}
}
```

## 충돌과 권한

- 모든 쓰기는 기존 프로젝트 행의 쓰기 잠금을 사용한다. 같은 사용자의 같은 트랙/대상 클립에는 ACTIVE 초안을 하나만 허용한다. 한 사용자당 한 레이어라는 기존 트랙 생성 규칙도 유지된다.
- revision 불일치는 409 `EDITOR_DRAFT_CONFLICT`다. 다른 기기의 데이터를 자동으로 덮어쓰지 않는다.
- 응답 유실 후 동일 mutationId와 동일 내용을 재전송하면 같은 revision을 반환한다.
- commit 당시 원본 스냅샷이 달라졌으면 409 `EDITOR_DRAFT_BASE_CHANGED`를 반환한다.
- COMMITTED/DISCARDED 기록을 남겨 늦게 도착한 자동 저장이 초안을 되살리는 것을 막는다. 같은 버전의 commit 재시도로 클립이 중복 생성되지 않는다.
- 타인의 초안 ID 조회는 404다. 본인의 트랙과 클립, 활성 사용자와 프로젝트 멤버 권한을 검사하며 PENDING/REJECTED 대상 및 완료된 프로젝트의 쓰기는 거부한다.
- 제출에서 초안 반영 또는 기존 기여 생성이 실패하면 클립 생성/수정과 초안 완료 기록도 함께 롤백한다. 요청에서 빠진 ACTIVE 초안이나 버전 변경을 감지하면 제출을 거부한다.

## 배포와 검증

개발 환경의 기존 ddl-auto=update 설정은 새 테이블을 생성한다. validate/none 환경에서는 `docs/editor-drafts.sql`을 먼저 적용하고 백엔드, 프론트엔드 순서로 배포한다. 자동 저장 초안은 기존 음원 생성이나 공동 작업 조회 API에 포함하지 않는다.

`./gradlew test`로 도메인, 접근 제어, 충돌/재시도, JSON 영속성, API 인증, 제출 롤백을 포함한 회귀 테스트를 실행한다. PostgreSQL의 실제 행 잠금은 배포 환경에서도 확인한다. H2 통합 테스트는 JSON 왕복과 트랜잭션 롤백을 검증한다.

적용 범위는 피아노롤 MIDI 편집이다. 녹음 중인 원본 오디오나 파일 업로드 도중의 복구는 포함하지 않는다. 완료 기록과 초안의 자동 만료/정리 정책은 아직 적용하지 않는다.
