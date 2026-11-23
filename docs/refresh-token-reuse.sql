-- PostgreSQL. ddl-auto=validate 또는 none 환경에서 백엔드 배포 전에 적용한다(update 환경은 Hibernate가 컬럼을 추가한다).
-- refresh token을 회전한 뒤 응답을 받지 못한 클라이언트가 유예 시간 안에 다시 회전할 수 있도록 후속 토큰을 기록한다.
ALTER TABLE user_refresh_tokens ADD COLUMN IF NOT EXISTS replaced_by_id bigint;
