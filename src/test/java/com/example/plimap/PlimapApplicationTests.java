package com.example.plimap;

import com.example.plimap.support.PostgisContainerConfiguration;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
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
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    void appliesInitialMigrationToEmptyDatabase() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("1");

        List<String> tables = jdbcTemplate.queryForList("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_type = 'BASE TABLE'
                """, String.class);

        assertThat(tables).containsAll(DOMAIN_TABLES);
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
