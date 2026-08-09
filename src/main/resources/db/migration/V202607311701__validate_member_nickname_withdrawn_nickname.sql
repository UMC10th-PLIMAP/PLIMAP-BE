-- V202607311700에서 NOT VALID로 추가/변경한 제약을 별도 트랜잭션(마이그레이션)에서 검증한다.
ALTER TABLE member VALIDATE CONSTRAINT chk_member_nickname_length;
ALTER TABLE member VALIDATE CONSTRAINT chk_member_withdrawn_nickname_length;
