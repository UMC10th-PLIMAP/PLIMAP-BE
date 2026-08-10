package com.example.plimap.domain.inquiry.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.plimap.domain.inquiry.entity.Inquiry;
import com.example.plimap.domain.inquiry.enums.InquiryCategory;
import com.example.plimap.support.PostgisContainerConfiguration;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgisContainerConfiguration.class)
@Transactional
class InquiryRepositoryIntegrationTest {

    @Autowired
    private InquiryRepository inquiryRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 생성_시각이_같은_문의도_id를_보조_정렬_키로_페이지_경계에서_중복_누락_없이_조회된다() {
        // given: createdAt이 완전히 동일한 문의 두 건을 만든다.
        Inquiry first = inquiryRepository.save(
                Inquiry.create(null, InquiryCategory.OTHER, "제목1", "내용1", "a@example.com"));
        Inquiry second = inquiryRepository.save(
                Inquiry.create(null, InquiryCategory.OTHER, "제목2", "내용2", "b@example.com"));

        Instant tiedCreatedAt = Instant.now();
        ReflectionTestUtils.setField(first, "createdAt", tiedCreatedAt);
        ReflectionTestUtils.setField(second, "createdAt", tiedCreatedAt);
        inquiryRepository.saveAll(List.of(first, second));
        entityManager.flush();
        entityManager.clear();

        // when: 페이지 크기 1로 두 페이지를 나눠 조회한다.
        Page<Inquiry> firstPage = inquiryRepository.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(0, 1));
        Page<Inquiry> secondPage = inquiryRepository.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(1, 1));

        // then: createdAt이 같아도 id 보조 정렬 덕분에 두 페이지를 합치면 중복·누락 없이 정확히 2건이다.
        List<Long> pagedIds = Stream.concat(
                        firstPage.getContent().stream().map(Inquiry::getId),
                        secondPage.getContent().stream().map(Inquiry::getId))
                .toList();

        assertThat(pagedIds).containsExactlyInAnyOrder(first.getId(), second.getId());
        assertThat(pagedIds).doesNotHaveDuplicates();
    }
}
