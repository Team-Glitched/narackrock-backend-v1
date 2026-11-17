-- PostgreSQL. 백엔드 배포 전에 적용한다. Hibernate가 enum 컬럼에 만든 CHECK 제약이 있으면
-- 새 운영 기록 유형(GRANT_MANAGER, REVOKE_MANAGER)의 저장을 막는다(ddl-auto=update는 이 제약을 갱신하지 않는다).
ALTER TABLE gallery_moderation_logs DROP CONSTRAINT IF EXISTS gallery_moderation_logs_action_type_check;
