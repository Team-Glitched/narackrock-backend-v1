-- PostgreSQL. 백엔드 배포 전에 적용한다. Hibernate가 enum 컬럼에 만든 CHECK 제약이 있으면
-- 새 신고 대상(OPEN_SOURCE, OPEN_SOURCE_COMMENT)의 신고 접수와 처리 기록 저장을 막는다(ddl-auto=update는 이 제약을 갱신하지 않는다).
ALTER TABLE reports DROP CONSTRAINT IF EXISTS reports_target_type_check;
ALTER TABLE moderation_actions DROP CONSTRAINT IF EXISTS moderation_actions_target_type_check;
