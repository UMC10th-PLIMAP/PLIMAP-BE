-- Pin 신고 자동숨김(신고 누적 10회 시 전원 숨김)을 위한 캐시 컬럼을 추가한다. like_count와 동일한 패턴.
ALTER TABLE pin
    ADD COLUMN report_count INTEGER NOT NULL DEFAULT 0;

-- CHECK 제약은 NOT VALID로만 추가한다. VALIDATE CONSTRAINT는 다음 마이그레이션으로 분리한다.
ALTER TABLE pin
    ADD CONSTRAINT chk_pin_report_count
        CHECK (report_count >= 0) NOT VALID;
