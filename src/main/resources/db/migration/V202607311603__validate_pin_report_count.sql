-- V202607311602에서 NOT VALID로 추가한 제약을 별도 트랜잭션(마이그레이션)에서 검증한다.
ALTER TABLE pin VALIDATE CONSTRAINT chk_pin_report_count;
