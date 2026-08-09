package com.example.plimap.domain.member.repository;

import com.example.plimap.domain.member.entity.Terms;
import com.example.plimap.domain.member.enums.TermsType;
import com.example.plimap.support.PostgisContainerConfiguration;
import com.example.plimap.support.RedisContainerConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import({PostgisContainerConfiguration.class, RedisContainerConfiguration.class})
@Transactional
class TermsRepositoryIntegrationTest {

    @Autowired
    private TermsRepository termsRepository;

    @Test
    void 시드로_등록된_활성_약관_4종을_유형순으로_조회한다() {
        List<Terms> activeTerms = termsRepository.findAllByActiveTrueOrderByTypeAsc();

        assertThat(activeTerms)
                .extracting(Terms::getType)
                .containsExactly(TermsType.LOCATION, TermsType.MARKETING, TermsType.PRIVACY, TermsType.SERVICE);
    }

    @Test
    void 필수_약관_3종과_선택_약관_1종이_시드되어_있다() {
        List<Terms> activeTerms = termsRepository.findAllByActiveTrueOrderByTypeAsc();

        assertThat(activeTerms)
                .filteredOn(Terms::isRequired)
                .extracting(Terms::getType)
                .containsExactlyInAnyOrder(TermsType.SERVICE, TermsType.PRIVACY, TermsType.LOCATION);

        assertThat(activeTerms)
                .filteredOn(terms -> !terms.isRequired())
                .extracting(Terms::getType)
                .containsExactly(TermsType.MARKETING);
    }

    @Test
    void 유형으로_활성_약관을_조회한다() {
        Terms serviceTerms = termsRepository
                .findFirstByTypeAndActiveTrueOrderByEffectiveAtDesc(TermsType.SERVICE)
                .orElseThrow();

        assertThat(serviceTerms.getTitle()).isEqualTo("플리맵 이용약관");
        assertThat(serviceTerms.isRequired()).isTrue();
    }
}
