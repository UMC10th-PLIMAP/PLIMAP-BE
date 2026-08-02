ALTER TABLE place
    ADD COLUMN normalized_address VARCHAR(255);

ALTER TABLE place
    DROP CONSTRAINT chk_place_source,
    ADD CONSTRAINT chk_place_source
        CHECK (source IN ('PLACE_SEARCH', 'ADDRESS_SEARCH', 'MAP_SELECTION')),
    DROP CONSTRAINT chk_place_provider,
    ADD CONSTRAINT chk_place_provider CHECK (
        (
            source = 'PLACE_SEARCH'
            AND place_provider IS NOT NULL
            AND provider_place_id IS NOT NULL
        )
        OR (
            source = 'ADDRESS_SEARCH'
            AND place_provider = 'KAKAO'
            AND provider_place_id IS NULL
        )
        OR (
            source = 'MAP_SELECTION'
            AND place_provider IS NULL
            AND provider_place_id IS NULL
        )
    ),
    ADD CONSTRAINT chk_place_normalized_address CHECK (
        (source = 'ADDRESS_SEARCH' AND normalized_address IS NOT NULL)
        OR (source <> 'ADDRESS_SEARCH' AND normalized_address IS NULL)
    ),
    ADD CONSTRAINT chk_place_address_search_category CHECK (
        source <> 'ADDRESS_SEARCH' OR category IS NULL
    );

CREATE UNIQUE INDEX uk_place_active_address_search_normalized_address
    ON place (normalized_address)
    WHERE source = 'ADDRESS_SEARCH'
      AND deleted_at IS NULL;
