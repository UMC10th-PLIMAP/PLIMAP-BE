package com.example.plimap.domain.inquiry.repository.query.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.plimap.domain.inquiry.dto.Pagination;
import com.example.plimap.domain.inquiry.entity.Inquiry;
import com.example.plimap.domain.inquiry.enums.InquiryCategory;
import com.example.plimap.domain.inquiry.repository.InquiryRepository;
import com.example.plimap.domain.inquiry.repository.query.InquiryQueryRepository;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.support.PostgisContainerConfiguration;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgisContainerConfiguration.class)
@Transactional
class InquiryQueryRepositoryImplTest {

    @Autowired
    private InquiryQueryRepository inquiryQueryRepository;

    @Autowired
    private InquiryRepository inquiryRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 문의_목록을_최신순으로_조회한다() {
        // given
        Inquiry older = inquiryRepository.save(
                Inquiry.create(null, InquiryCategory.OTHER, "오래된 문의", "내용", "a@example.com"));
        Inquiry newer = inquiryRepository.save(
                Inquiry.create(null, InquiryCategory.OTHER, "최신 문의", "내용", "b@example.com"));
        ReflectionTestUtils.setField(older, "createdAt", Instant.now().minusSeconds(60));
        ReflectionTestUtils.setField(newer, "createdAt", Instant.now());
        inquiryRepository.saveAll(List.of(older, newer));
        entityManager.flush();
        entityManager.clear();

        // when
        Pagination<Inquiry> response = inquiryQueryRepository.findInquiries(null, null, 10);

        // then
        assertThat(response.data()).extracting(Inquiry::getTitle)
                .containsExactly("최신 문의", "오래된 문의");
        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursor()).isNull();
    }

    @Test
    void 카테고리로_필터링해서_조회한다() {
        // given
        inquiryRepository.save(Inquiry.create(null, InquiryCategory.OTHER, "기타 문의", "내용", "a@example.com"));
        inquiryRepository.save(
                Inquiry.create(null, InquiryCategory.APP_BUG_OR_ERROR, "버그 문의", "내용", "b@example.com"));
        entityManager.flush();
        entityManager.clear();

        // when
        Pagination<Inquiry> response =
                inquiryQueryRepository.findInquiries(InquiryCategory.APP_BUG_OR_ERROR, null, 10);

        // then
        assertThat(response.data()).extracting(Inquiry::getTitle)
                .containsExactly("버그 문의");
    }

    @Test
    void 문의_목록을_커서_기반_페이지네이션으로_조회한다() {
        // given
        for (int i = 0; i < 3; i++) {
            Inquiry inquiry = inquiryRepository.save(
                    Inquiry.create(null, InquiryCategory.OTHER, "문의" + i, "내용", "a@example.com"));
            ReflectionTestUtils.setField(inquiry, "createdAt", Instant.now().minusSeconds(3L - i));
            inquiryRepository.save(inquiry);
        }
        entityManager.flush();
        entityManager.clear();

        // when
        Pagination<Inquiry> firstPage = inquiryQueryRepository.findInquiries(null, null, 1);

        // then
        assertThat(firstPage.data()).hasSize(1);
        assertThat(firstPage.data().get(0).getTitle()).isEqualTo("문의2");
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(firstPage.nextCursor()).isNotNull();

        // when
        Pagination<Inquiry> secondPage =
                inquiryQueryRepository.findInquiries(null, firstPage.nextCursor(), 1);

        // then
        assertThat(secondPage.data()).hasSize(1);
        assertThat(secondPage.data().get(0).getTitle()).isEqualTo("문의1");
        assertThat(secondPage.hasNext()).isTrue();
    }

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
        Pagination<Inquiry> firstPage = inquiryQueryRepository.findInquiries(null, null, 1);
        Pagination<Inquiry> secondPage = inquiryQueryRepository.findInquiries(null, firstPage.nextCursor(), 1);

        // then: createdAt이 같아도 id 보조 정렬 덕분에 두 페이지를 합치면 중복·누락 없이 정확히 2건이다.
        List<Long> pagedIds = Stream.concat(
                        firstPage.data().stream().map(Inquiry::getId),
                        secondPage.data().stream().map(Inquiry::getId))
                .toList();

        assertThat(pagedIds).containsExactlyInAnyOrder(first.getId(), second.getId());
        assertThat(pagedIds).doesNotHaveDuplicates();
    }

    @Test
    void 로그인_회원의_문의는_작성자_정보와_함께_조회된다() {
        // given
        Member member = memberRepository.save(Member.builder().nickname("작성자").build());
        inquiryRepository.save(Inquiry.create(member, InquiryCategory.OTHER, "제목", "내용", "user@example.com"));
        entityManager.flush();
        entityManager.clear();

        // when
        Pagination<Inquiry> response = inquiryQueryRepository.findInquiries(null, null, 10);

        // then: fetch join으로 가져온 member의 지연 로딩 필드에 트랜잭션 내에서 바로 접근할 수 있다.
        assertThat(response.data().get(0).getMember().getNickname()).isEqualTo("작성자");
    }

    @Test
    void 비로그인_문의도_조회된다() {
        // given
        inquiryRepository.save(Inquiry.create(null, InquiryCategory.OTHER, "제목", "내용", "guest@example.com"));
        entityManager.flush();
        entityManager.clear();

        // when
        Pagination<Inquiry> response = inquiryQueryRepository.findInquiries(null, null, 10);

        // then
        assertThat(response.data()).hasSize(1);
        assertThat(response.data().get(0).getMember()).isNull();
    }
}
