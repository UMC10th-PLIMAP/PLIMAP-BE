package com.example.plimap.support.querydsl;

import com.example.plimap.support.PostgisContainerConfiguration;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(PostgisContainerConfiguration.class)
class QuerydslIntegrationTest {

    @Autowired
    private JPAQueryFactory queryFactory;

    @Test
    void queriesMigratedPostgresqlTableWithGeneratedQType() {
        QQuerydslSampleTag tag = QQuerydslSampleTag.querydslSampleTag;

        String firstTagName = queryFactory
                .select(tag.name)
                .from(tag)
                .where(tag.displayOrder.eq((short) 0))
                .fetchOne();

        assertThat(firstTagName).isEqualTo("감성");
    }
}
