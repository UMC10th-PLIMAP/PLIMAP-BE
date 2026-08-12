-- V202608121500에서 NOT VALID로 추가한 제약을 별도 마이그레이션(트랜잭션)에서 검증한다.
ALTER TABLE member VALIDATE CONSTRAINT chk_member_last_penalty_category;
ALTER TABLE member VALIDATE CONSTRAINT chk_member_last_penalty_detail;
