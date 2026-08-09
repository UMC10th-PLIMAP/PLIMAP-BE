ALTER TABLE place
    ADD COLUMN administrative_region_code VARCHAR(20),
    ADD COLUMN sido VARCHAR(100),
    ADD COLUMN sigungu VARCHAR(100),
    ADD COLUMN eup_myeon_dong VARCHAR(100);

CREATE INDEX idx_place_active_administrative_region_code
    ON place (administrative_region_code)
    WHERE deleted_at IS NULL
      AND administrative_region_code IS NOT NULL;
