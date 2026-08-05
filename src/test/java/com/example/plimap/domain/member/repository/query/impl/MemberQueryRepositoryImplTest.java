package com.example.plimap.domain.member.repository.query.impl;

import com.example.plimap.domain.member.dto.Pagination;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.entity.MemberFollow;
import com.example.plimap.domain.member.repository.MemberFollowRepository;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.domain.member.repository.query.MemberFollowRow;
import com.example.plimap.domain.member.repository.query.MemberQueryRepository;
import com.example.plimap.domain.report.entity.Report;
import com.example.plimap.domain.report.enums.ReportCategory;
import com.example.plimap.domain.report.repository.ReportRepository;
import com.example.plimap.support.PostgisContainerConfiguration;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgisContainerConfiguration.class)
@Transactional
class MemberQueryRepositoryImplTest {

    @Autowired
    private MemberQueryRepository memberQueryRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberFollowRepository memberFollowRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private EntityManager entityManager;

    private Member target;
    private Member follower1;
    private Member follower2;
    private Member follower3;
    private Member source;
    private Member following1;
    private Member following2;
    private Member following3;
    private Member outsider;

    @BeforeEach
    void setup() {
        target = createMember("대상");
        follower1 = createMember("팔로워1");
        follower2 = createMember("팔로워2");
        follower3 = createMember("탈퇴한팔로워");
        source = createMember("출발점");
        following1 = createMember("팔로잉1");
        following2 = createMember("팔로잉2");
        following3 = createMember("탈퇴한팔로잉");
        outsider = createMember("제3자");
        memberRepository.saveAll(List.of(
                target, follower1, follower2, follower3,
                source, following1, following2, following3, outsider
        ));

        memberFollowRepository.save(MemberFollow.create(follower1, target));
        memberFollowRepository.save(MemberFollow.create(follower2, target));
        memberFollowRepository.save(MemberFollow.create(follower3, target));
        // target이 follower1은 맞팔, follower2는 맞팔하지 않음
        memberFollowRepository.save(MemberFollow.create(target, follower1));

        memberFollowRepository.save(MemberFollow.create(source, following1));
        memberFollowRepository.save(MemberFollow.create(source, following2));
        memberFollowRepository.save(MemberFollow.create(source, following3));
        // 제3자는 following1만 팔로우
        memberFollowRepository.save(MemberFollow.create(outsider, following1));

        follower3.delete();
        following3.delete();

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void 팔로워_목록을_최신순으로_조회한다() {
        Pagination<MemberFollowRow> response =
                memberQueryRepository.findFollowersByMemberId(target.getId(), target.getId(), null, 10);

        assertThat(response.data()).extracting(MemberFollowRow::nickname)
                .containsExactly("팔로워2", "팔로워1");
        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursor()).isNull();
    }

    @Test
    void 탈퇴한_회원은_팔로워_목록에서_제외된다() {
        Pagination<MemberFollowRow> response =
                memberQueryRepository.findFollowersByMemberId(target.getId(), target.getId(), null, 10);

        assertThat(response.data()).extracting(MemberFollowRow::nickname)
                .doesNotContain("탈퇴한팔로워");
    }

    @Test
    void 팔로워_목록을_커서_기반_페이지네이션으로_조회한다() {
        Pagination<MemberFollowRow> firstPage =
                memberQueryRepository.findFollowersByMemberId(target.getId(), target.getId(), null, 1);

        assertThat(firstPage.data()).hasSize(1);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(firstPage.nextCursor()).isNotNull();

        Pagination<MemberFollowRow> secondPage =
                memberQueryRepository.findFollowersByMemberId(target.getId(), target.getId(), firstPage.nextCursor(), 1);

        assertThat(secondPage.data()).hasSize(1);
        assertThat(secondPage.hasNext()).isFalse();
        assertThat(secondPage.nextCursor()).isNull();
        assertThat(secondPage.data().get(0).nickname()).isEqualTo("팔로워1");
    }

    @Test
    void 팔로워_목록에서_뷰어가_맞팔한_팔로워만_isFollowing이_true다() {
        Pagination<MemberFollowRow> response =
                memberQueryRepository.findFollowersByMemberId(target.getId(), target.getId(), null, 10);

        assertThat(response.data())
                .filteredOn(item -> item.nickname().equals("팔로워1"))
                .extracting(MemberFollowRow::isFollowing)
                .containsExactly(true);
        assertThat(response.data())
                .filteredOn(item -> item.nickname().equals("팔로워2"))
                .extracting(MemberFollowRow::isFollowing)
                .containsExactly(false);
    }

    @Test
    void 팔로워_목록에서_뷰어가_신고한_회원은_제외된다() {
        reportRepository.save(Report.createMemberReport(outsider, follower1, ReportCategory.OBSCENE_OR_HARMFUL, null));
        entityManager.flush();
        entityManager.clear();

        Pagination<MemberFollowRow> response =
                memberQueryRepository.findFollowersByMemberId(outsider.getId(), target.getId(), null, 10);

        assertThat(response.data()).extracting(MemberFollowRow::nickname)
                .doesNotContain("팔로워1")
                .contains("팔로워2");
    }

    @Test
    void 팔로워_목록에서_신고누적_10회_이상인_회원은_전원에게_숨겨진다() {
        for (int i = 0; i < 10; i++) {
            memberRepository.increaseReportCount(follower1.getId());
        }
        entityManager.flush();
        entityManager.clear();

        Pagination<MemberFollowRow> response =
                memberQueryRepository.findFollowersByMemberId(target.getId(), target.getId(), null, 10);

        assertThat(response.data()).extracting(MemberFollowRow::nickname)
                .doesNotContain("팔로워1");
    }

    @Test
    void 팔로잉_목록을_최신순으로_조회한다() {
        Pagination<MemberFollowRow> response =
                memberQueryRepository.findFollowingByMemberId(source.getId(), source.getId(), null, 10);

        assertThat(response.data()).extracting(MemberFollowRow::nickname)
                .containsExactly("팔로잉2", "팔로잉1");
        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursor()).isNull();
    }

    @Test
    void 탈퇴한_회원은_팔로잉_목록에서_제외된다() {
        Pagination<MemberFollowRow> response =
                memberQueryRepository.findFollowingByMemberId(source.getId(), source.getId(), null, 10);

        assertThat(response.data()).extracting(MemberFollowRow::nickname)
                .doesNotContain("탈퇴한팔로잉");
    }

    @Test
    void 팔로잉_목록을_커서_기반_페이지네이션으로_조회한다() {
        Pagination<MemberFollowRow> firstPage =
                memberQueryRepository.findFollowingByMemberId(source.getId(), source.getId(), null, 1);

        assertThat(firstPage.data()).hasSize(1);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(firstPage.nextCursor()).isNotNull();

        Pagination<MemberFollowRow> secondPage =
                memberQueryRepository.findFollowingByMemberId(source.getId(), source.getId(), firstPage.nextCursor(), 1);

        assertThat(secondPage.data()).hasSize(1);
        assertThat(secondPage.hasNext()).isFalse();
        assertThat(secondPage.nextCursor()).isNull();
        assertThat(secondPage.data().get(0).nickname()).isEqualTo("팔로잉1");
    }

    @Test
    void 팔로잉_목록을_본인이_조회하면_모든_항목의_isFollowing이_true다() {
        Pagination<MemberFollowRow> response =
                memberQueryRepository.findFollowingByMemberId(source.getId(), source.getId(), null, 10);

        assertThat(response.data()).extracting(MemberFollowRow::isFollowing)
                .containsOnly(true);
    }

    @Test
    void 팔로잉_목록을_제3자가_조회하면_본인이_팔로우한_항목만_isFollowing이_true다() {
        Pagination<MemberFollowRow> response =
                memberQueryRepository.findFollowingByMemberId(outsider.getId(), source.getId(), null, 10);

        assertThat(response.data())
                .filteredOn(item -> item.nickname().equals("팔로잉1"))
                .extracting(MemberFollowRow::isFollowing)
                .containsExactly(true);
        assertThat(response.data())
                .filteredOn(item -> item.nickname().equals("팔로잉2"))
                .extracting(MemberFollowRow::isFollowing)
                .containsExactly(false);
    }

    @Test
    void 활성이고_신고되지_않은_회원은_조회된다() {
        Optional<Member> result = memberQueryRepository.findVisibleActiveMember(target.getId(), outsider.getId());

        assertThat(result).isPresent();
    }

    @Test
    void 뷰어가_신고한_회원은_조회되지_않는다() {
        reportRepository.save(Report.createMemberReport(outsider, target, ReportCategory.OBSCENE_OR_HARMFUL, null));
        entityManager.flush();
        entityManager.clear();

        Optional<Member> result = memberQueryRepository.findVisibleActiveMember(target.getId(), outsider.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void 신고누적_10회_이상인_회원은_전원에게_조회되지_않는다() {
        for (int i = 0; i < 10; i++) {
            memberRepository.increaseReportCount(target.getId());
        }
        entityManager.flush();
        entityManager.clear();

        Optional<Member> result = memberQueryRepository.findVisibleActiveMember(target.getId(), outsider.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void 탈퇴한_회원은_조회되지_않는다() {
        Optional<Member> result = memberQueryRepository.findVisibleActiveMember(follower3.getId(), outsider.getId());

        assertThat(result).isEmpty();
    }

    private Member createMember(String nickname) {
        return Member.builder()
                .nickname(nickname)
                .name("이름")
                .introduction("소개")
                .profileImageObjectKey("key")
                .build();
    }
}
