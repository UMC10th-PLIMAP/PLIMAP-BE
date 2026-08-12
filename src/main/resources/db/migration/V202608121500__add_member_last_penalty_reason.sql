-- 관리자가 최종 제재(기간·사유 선택) 시점에 선택/작성한 사유를 스냅샷으로 보관한다.
-- 벌점 4점 누적/영구 선택으로 자동 탈퇴되어도 프론트 안내 모달에 사유를 계속 보여줘야 하므로
-- withdrawByPenalty()가 지우는 nickname/introduction 등과 달리 이 컬럼들은 별도로 둔다.
ALTER TABLE member
    ADD COLUMN last_penalty_category TEXT,
    ADD COLUMN last_penalty_detail   TEXT;

-- CHECK 제약은 NOT VALID로만 추가하고, VALIDATE는 락 회피를 위해 다음 마이그레이션으로 분리한다.
ALTER TABLE member
    ADD CONSTRAINT chk_member_last_penalty_category
        CHECK (
            last_penalty_category IS NULL OR last_penalty_category IN (
                'PERSONAL_INFORMATION_EXPOSURE',
                'OBSCENE_OR_HARMFUL',
                'ABUSE_OR_HATE_SPEECH',
                'COMMERCIAL_OR_PROMOTIONAL',
                'OTHER'
            )
        ) NOT VALID,
    ADD CONSTRAINT chk_member_last_penalty_detail
        CHECK (
            (last_penalty_category = 'OTHER' AND last_penalty_detail IS NOT NULL AND last_penalty_detail ~ '[^[:space:]]')
            OR (last_penalty_category IS DISTINCT FROM 'OTHER' AND last_penalty_detail IS NULL)
        ) NOT VALID;
