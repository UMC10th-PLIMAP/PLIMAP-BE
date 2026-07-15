-- 닉네임 최대 길이를 7자에서 10자로 확장하고, 이름 최소 길이를 1자에서 2자로 강화한다.
ALTER TABLE member
    DROP CONSTRAINT chk_member_nickname_length,
    ALTER COLUMN nickname TYPE VARCHAR(10),
    ADD CONSTRAINT chk_member_nickname_length
        CHECK (nickname IS NULL OR char_length(nickname) BETWEEN 2 AND 10);

ALTER TABLE member
    DROP CONSTRAINT chk_member_name_length,
    ADD CONSTRAINT chk_member_name_length
        CHECK (name IS NULL OR char_length(name) BETWEEN 2 AND 7);
