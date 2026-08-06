-- 관리자가 신고를 반려(벌점 미부여)해도 재신고 방지를 위해 report row 자체는 삭제하지 않는다.
-- 반려 이후 새로 들어온 신고만 "현재 사유"로 구분해 조회할 수 있도록 리뷰 여부 플래그를 추가한다.
ALTER TABLE report
    ADD COLUMN reviewed BOOLEAN NOT NULL DEFAULT false;
