-- 관리자가 최종 제재 시점에 선택한 정지 기간을 스냅샷으로 보관한다.
-- suspended_until(절대 시각)만으로는 제재가 시작된 시점을 몰라 원래 선택된 기간(1일/3일/5일/영구)을
-- 프론트가 역산할 수 없으므로, last_penalty_category/last_penalty_detail과 동일한 패턴으로 별도 컬럼을 둔다.
ALTER TABLE member
    ADD COLUMN last_penalty_period TEXT;

-- CHECK 제약은 NOT VALID로만 추가하고, VALIDATE는 락 회피를 위해 다음 마이그레이션으로 분리한다.
ALTER TABLE member
    ADD CONSTRAINT chk_member_last_penalty_period
        CHECK (
            last_penalty_period IS NULL OR last_penalty_period IN (
                'ONE_DAY',
                'THREE_DAYS',
                'FIVE_DAYS',
                'PERMANENT'
            )
        ) NOT VALID;
