-- 자발적/자동 탈퇴, 벌점/정지, 관리자 인가, 프로필 신고 자동숨김에 필요한 컬럼을 추가한다.
ALTER TABLE member
    ADD COLUMN join_provider     VARCHAR(20),
    ADD COLUMN penalty_point     INTEGER     NOT NULL DEFAULT 0,
    ADD COLUMN suspended_until   TIMESTAMPTZ,
    ADD COLUMN withdrawal_reason VARCHAR(20),
    ADD COLUMN role              VARCHAR(20) NOT NULL DEFAULT 'USER',
    ADD COLUMN report_count      INTEGER     NOT NULL DEFAULT 0;

-- report_count는 컬럼 추가 시점 이전에 이미 쌓여있던 report 테이블의 신고 이력을 backfill한다.
-- 이후 신규 신고 건은 애플리케이션에서 +1씩 증가시킨다(Phase 2).
UPDATE member m
SET report_count = r.cnt
FROM (
    SELECT reported_member_id, COUNT(*) AS cnt
    FROM report
    WHERE reported_member_id IS NOT NULL
    GROUP BY reported_member_id
) r
WHERE m.id = r.reported_member_id;

-- CHECK 제약은 NOT VALID로만 추가한다. VALIDATE CONSTRAINT는 같은 트랜잭션(Flyway 스크립트)에
-- 넣으면 이 ALTER TABLE이 잡은 락이 커밋 전까지 유지되어 무의미해지므로 다음 마이그레이션으로 분리한다.
ALTER TABLE member
    ADD CONSTRAINT chk_member_join_provider
        CHECK (join_provider IS NULL OR join_provider IN ('KAKAO', 'GOOGLE', 'APPLE')) NOT VALID,
    ADD CONSTRAINT chk_member_penalty_point
        CHECK (penalty_point >= 0) NOT VALID,
    ADD CONSTRAINT chk_member_withdrawal_reason
        CHECK (withdrawal_reason IS NULL OR withdrawal_reason IN ('VOLUNTARY', 'PENALTY')) NOT VALID,
    ADD CONSTRAINT chk_member_withdrawal_reason_status
        CHECK (withdrawal_reason IS NULL OR status = 'WITHDRAWN') NOT VALID,
    ADD CONSTRAINT chk_member_suspended_until_status
        CHECK (suspended_until IS NULL OR status = 'SUSPENDED') NOT VALID,
    ADD CONSTRAINT chk_member_role
        CHECK (role IN ('USER', 'ADMIN')) NOT VALID,
    ADD CONSTRAINT chk_member_report_count
        CHECK (report_count >= 0) NOT VALID;
