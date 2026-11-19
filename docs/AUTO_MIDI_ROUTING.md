# 녹음의 자동 MIDI 변환

`POST /api/v1/projects/{projectId}/hti-transform`의 multipart:

- `audio`: 기존 녹음 파일
- `soundKind`: AUTO(기본), HUMMING, INSTRUMENT

기존 프로젝트 멤버의 편집 권한을 확인한 뒤 프로젝트 BPM·PPQ와 함께 Python에 전달한다.
MIDI 재생 악기는 모델 선택 기준으로 사용하지 않는다. 반환된 동시 노트를 그대로 매핑하며,
`routing`(선택 엔진·판정 종류·이유·규칙 버전)을 프론트에 전달한다. 노트 저장은 기존 초안 API다.

Python의 입력 오류(422)는 `INVALID_HTI_AUDIO`로 구분한다. 워커·추론 서버 오류는 기존
`HTI_TRANSFORM_FAILED`로 반환하고 단선율 엔진으로 재시도하지 않는다. AUTO/INSTRUMENT
요청에 routing 정보가 없는 구버전 Python 서버의 결과도 거부한다. Uvicorn과의 multipart
통신은 HTTP/1.1로 고정하며 연결 5초·응답 130초 제한 시간을 사용한다.

환경 변수 `HTI_SERVICE_BASE_URL`은 자동 판별 기능이 있는 Python 서버의 내부 URL을 가리켜야 한다.
Python 서버에는 Basic Pitch ONNX 별도 환경이 필요하다. Python 저장소의 `AUTO_ROUTING.md`에
설치와 제한이 있다. 이 작업은 로컬 연동 검증이며 운영 배포는 하지 않았다.

전체 녹음→변환→코드 추천→미리보기→저장 다이어그램과 샘플 검증은 프론트 저장소의
`AUTO_MIDI_ROUTING.md` / `AUTO_MIDI_ROUTING.svg`에 있다. 코드 추천 자체는 기존 별도 PR의 기능이며,
이번 변경에서는 기존 녹음 변환을 신호 규칙 기반 라우팅에 연결한다.
