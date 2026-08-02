-- Pin 신고 자동숨김(신고 누적 10회 시 전원 숨김)을 위한 캐시 컬럼을 추가한다. like_count와 동일한 패턴.
ALTER TABLE pin
    ADD COLUMN report_count INTEGER NOT NULL DEFAULT 0;

-- report_count는 컬럼 추가 시점 이전에 이미 쌓여있던 report 테이블의 신고 이력을 backfill한다.
-- 이후 신규 신고 건은 애플리케이션에서 +1씩 증가시킨다(Phase 2).
UPDATE pin p
SET report_count = r.cnt
FROM (
    SELECT reported_pin_id, COUNT(*) AS cnt
    FROM report
    WHERE reported_pin_id IS NOT NULL
    GROUP BY reported_pin_id
) r
WHERE p.id = r.reported_pin_id;

-- CHECK 제약은 NOT VALID로만 추가한다. VALIDATE CONSTRAINT는 다음 마이그레이션으로 분리한다.
ALTER TABLE pin
    ADD CONSTRAINT chk_pin_report_count
        CHECK (report_count >= 0) NOT VALID;
