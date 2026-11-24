-- PostgreSQL. 백엔드 배포 전에 적용한다.
-- Hibernate ddl-auto=update는 기존 VARCHAR 컬럼 길이를 자동으로 확장하지 않으므로,
-- 기존 제약(1000자/255자)보다 큰 허용 길이를 가진 컬럼을 안전하게 확장한다.
-- 기존 컬럼을 축소하지 않아 레거시 데이터가 있어도 마이그레이션이 실패하지 않는다.
ALTER TABLE open_source_comments ALTER COLUMN content TYPE text;
ALTER TABLE galleries ALTER COLUMN description TYPE varchar(500);
ALTER TABLE user_bans ALTER COLUMN reason TYPE varchar(500);
ALTER TABLE gallery_bans ALTER COLUMN reason TYPE varchar(500);
ALTER TABLE gallery_moderation_logs ALTER COLUMN reason TYPE varchar(500);
ALTER TABLE moderation_actions ALTER COLUMN reason TYPE varchar(500);
