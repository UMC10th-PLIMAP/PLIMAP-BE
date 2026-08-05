CREATE INDEX idx_pin_place_active
    ON pin (place_id)
    WHERE deleted_at IS NULL;
