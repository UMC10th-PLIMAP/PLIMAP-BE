-- V202607311600에서 NOT VALID로 추가한 제약을 별도 트랜잭션(마이그레이션)에서 검증한다.
-- 이렇게 분리해야 VALIDATE CONSTRAINT가 SHARE UPDATE EXCLUSIVE 락만으로 동작해
-- 풀 스캔 동안에도 읽기/쓰기를 막지 않는다.
ALTER TABLE member VALIDATE CONSTRAINT chk_member_join_provider;
ALTER TABLE member VALIDATE CONSTRAINT chk_member_penalty_point;
ALTER TABLE member VALIDATE CONSTRAINT chk_member_withdrawal_reason;
ALTER TABLE member VALIDATE CONSTRAINT chk_member_withdrawal_reason_status;
ALTER TABLE member VALIDATE CONSTRAINT chk_member_suspended_until_status;
ALTER TABLE member VALIDATE CONSTRAINT chk_member_role;
ALTER TABLE member VALIDATE CONSTRAINT chk_member_report_count;
