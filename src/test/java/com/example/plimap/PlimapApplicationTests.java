package com.example.plimap;

import com.example.plimap.support.PostgisContainerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgisContainerConfiguration.class)
class PlimapApplicationTests {

    private static final List<String> DOMAIN_TABLES = List.of(
            "member",
            "member_follow",
            "social_account",
            "terms",
            "member_terms_agreement",
            "place",
            "place_bookmark",
            "place_search_history",
            "track",
            "place_track",
            "place_track_like",
            "pin",
            "tag",
            "pin_tag",
            "pin_like"
    );

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    void appliesMigrationsToEmptyDatabase() {
        Integer appliedMigrationCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE version = '202607120752'
                  AND success = TRUE
                """, Integer.class);

        List<String> tables = jdbcTemplate.queryForList("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_type = 'BASE TABLE'
                """, String.class);

        assertThat(appliedMigrationCount).isEqualTo(1);
        assertThat(tables).containsAll(DOMAIN_TABLES);
    }

    @Test
    void addsNullablePlaceCategoryColumn() {
        String dataType = jdbcTemplate.queryForObject("""
                SELECT data_type
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'place'
                  AND column_name = 'category'
                """, String.class);
        Integer maxLength = jdbcTemplate.queryForObject("""
                SELECT character_maximum_length
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'place'
                  AND column_name = 'category'
                """, Integer.class);
        String isNullable = jdbcTemplate.queryForObject("""
                SELECT is_nullable
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'place'
                  AND column_name = 'category'
                """, String.class);

        assertThat(dataType).isEqualTo("character varying");
        assertThat(maxLength).isEqualTo(100);
        assertThat(isNullable).isEqualTo("YES");
    }

    @Test
    void enablesPostgisAndSeedsDefaultTags() {
        String postgisVersion = jdbcTemplate.queryForObject(
                "SELECT extversion FROM pg_extension WHERE extname = 'postgis'",
                String.class
        );
        Integer tagCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tag", Integer.class);
        String point = jdbcTemplate.queryForObject(
                "SELECT ST_AsText(ST_SetSRID(ST_MakePoint(127.0, 37.0), 4326))",
                String.class
        );

        assertThat(postgisVersion).isNotBlank();
        assertThat(tagCount).isEqualTo(10);
        assertThat(point).isEqualTo("POINT(127 37)");
    }
}
