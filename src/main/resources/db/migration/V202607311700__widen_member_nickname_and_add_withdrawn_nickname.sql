-- 자발적 탈퇴 시 닉네임을 "플리맵사용자{memberId}"로 마스킹하면 10자를 넘을 수 있어 컬럼 길이를 넉넉히 확장한다.
-- 일반 사용자가 고르는 닉네임은 애플리케이션 레벨(NicknameCheckFailReason)에서 여전히 2~10자로 제한되며,
-- 이 컬럼 제약은 시스템이 생성하는 마스킹 값까지 허용하기 위한 상한이다.
ALTER TABLE member
    ALTER COLUMN nickname TYPE VARCHAR(30);

-- 탈퇴로 마스킹되기 직전의 원래 닉네임을 관리자 확인용 이력으로 보존한다.
ALTER TABLE member
    ADD COLUMN withdrawn_nickname VARCHAR(10);

-- CHECK 제약은 NOT VALID로만 추가/변경한다. VALIDATE CONSTRAINT는 다음 마이그레이션으로 분리한다.
ALTER TABLE member
    DROP CONSTRAINT chk_member_nickname_length,
    ADD CONSTRAINT chk_member_nickname_length
        CHECK (nickname IS NULL OR char_length(nickname) BETWEEN 2 AND 30) NOT VALID,
    ADD CONSTRAINT chk_member_withdrawn_nickname_length
        CHECK (withdrawn_nickname IS NULL OR char_length(withdrawn_nickname) BETWEEN 2 AND 10) NOT VALID;
