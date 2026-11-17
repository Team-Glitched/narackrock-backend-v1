# 오디오 분석과 코드 추천

`POST /api/v1/projects/{projectId}/clips/{clipId}/audio-analysis`는 본인의 녹음 또는
업로드 클립에 대한 분석을 저장한다. JSON 요청은 `soundKind` (`HUMMING` 또는
`INSTRUMENT`)와 선택 항목 `refresh`를 받는다. 같은 입력 종류와 분석 버전의 결과가
있으면 재사용한다. 새 추출기나 체크포인트를 배포할 때 `HTI_AUDIO_ANALYSIS_VERSION`
값을 바꾸거나 사용자가 다시 분석을 선택하면 결과를 갱신한다.

`GET /api/v1/projects/{projectId}/tracks`의 오디오 클립에 `audioAnalysis`를
반환한다. 노트 위치는 클립에 상대적이며 분석 당시의 BPM, PPQ와 함께 저장한다.
프로젝트 템포 변경은 추천을 계산할 때 환산한다. 원본 파일이나 분석 구간이 변경되면
기존 분석을 제거한다. 오디오 클립의 재생과 내보내기는 원본을 사용한다.

스키마 자동 갱신을 사용하지 않는 환경은 배포 전 `project-audio-analysis.sql`을
적용한다. `HTI_SERVICE_BASE_URL`은 오디오 분석 API를 포함한 HTI 서버를 가리켜야
한다. Basic Pitch는 HTI 서버의 격리 환경에서 활성화한다.
