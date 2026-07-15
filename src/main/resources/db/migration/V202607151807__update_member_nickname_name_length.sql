-- 닉네임 최대 길이를 7자에서 10자로 확장하고, 이름 최소 길이를 1자에서 2자로 강화한다.
-- CHECK 제약은 NOT VALID로만 추가한다. VALIDATE CONSTRAINT는 같은 트랜잭션(Flyway 스크립트)에
-- 넣으면 이 ALTER TABLE이 잡은 락이 커밋 전까지 유지되어 무의미해지므로 다음 마이그레이션으로 분리한다.
ALTER TABLE member
    DROP CONSTRAINT chk_member_nickname_length,
    ALTER COLUMN nickname TYPE VARCHAR(10),
    ADD CONSTRAINT chk_member_nickname_length
        CHECK (nickname IS NULL OR char_length(nickname) BETWEEN 2 AND 10) NOT VALID;

ALTER TABLE member
    DROP CONSTRAINT chk_member_name_length,
    ADD CONSTRAINT chk_member_name_length
        CHECK (name IS NULL OR char_length(name) BETWEEN 2 AND 7) NOT VALID;
