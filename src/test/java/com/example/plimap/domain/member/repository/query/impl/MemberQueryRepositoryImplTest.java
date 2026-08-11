package com.example.plimap.domain.member.repository.query.impl;

import com.example.plimap.domain.auth.entity.SocialAccount;
import com.example.plimap.domain.auth.enums.AuthProvider;
import com.example.plimap.domain.auth.repository.SocialAccountRepository;
import com.example.plimap.domain.member.dto.Pagination;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.entity.MemberFollow;
import com.example.plimap.domain.member.enums.MemberStatus;
import com.example.plimap.domain.member.exception.MemberErrorCode;
import com.example.plimap.domain.member.exception.MemberException;
import com.example.plimap.domain.member.repository.MemberFollowRepository;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.domain.member.repository.query.MemberFollowRow;
import com.example.plimap.domain.member.repository.query.MemberQueryRepository;
import com.example.plimap.domain.member.repository.query.MemberSearchRow;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

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
    private SocialAccountRepository socialAccountRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    void 팔로워_목록에서_대상이_뷰어를_팔로우하는_경우에만_isFollowingViewer가_true다() {
        // given: target의 팔로워인 follower1이 뷰어(outsider)를 팔로우한다(follower2는 팔로우하지 않는다).
        // outsider는 follower1/follower2 둘 다 팔로우하지 않으므로 isFollowing은 항상 false지만,
        // "그들이 나를 팔로우하는지"는 isFollowingViewer로 별도 확인해야 한다.
        memberFollowRepository.save(MemberFollow.create(follower1, outsider));
        entityManager.flush();
        entityManager.clear();

        // when
        Pagination<MemberFollowRow> response =
                memberQueryRepository.findFollowersByMemberId(outsider.getId(), target.getId(), null, 10);

        // then
        assertThat(response.data())
                .filteredOn(item -> item.nickname().equals("팔로워1"))
                .extracting(MemberFollowRow::isFollowing, MemberFollowRow::isFollowingViewer)
                .containsExactly(tuple(false, true));
        assertThat(response.data())
                .filteredOn(item -> item.nickname().equals("팔로워2"))
                .extracting(MemberFollowRow::isFollowing, MemberFollowRow::isFollowingViewer)
                .containsExactly(tuple(false, false));
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
    void 팔로잉_목록에서_대상이_뷰어를_팔로우하는_경우에만_isFollowingViewer가_true다() {
        // given: outsider는 following1만 팔로우하지만(isFollowing 기준), 실제로 뷰어를 팔로우하는 건 following2다.
        // 버그였던 예전 로직은 isFollowing만으로 맞팔을 판단해 following1을 맞팔로 잘못 취급했다.
        // isFollowingViewer는 following1=false, following2=true로 정확히 구분해야 한다.
        memberFollowRepository.save(MemberFollow.create(following2, outsider));
        entityManager.flush();
        entityManager.clear();

        // when
        Pagination<MemberFollowRow> response =
                memberQueryRepository.findFollowingByMemberId(outsider.getId(), source.getId(), null, 10);

        // then
        assertThat(response.data())
                .filteredOn(item -> item.nickname().equals("팔로잉1"))
                .extracting(MemberFollowRow::isFollowing, MemberFollowRow::isFollowingViewer)
                .containsExactly(tuple(true, false));
        assertThat(response.data())
                .filteredOn(item -> item.nickname().equals("팔로잉2"))
                .extracting(MemberFollowRow::isFollowing, MemberFollowRow::isFollowingViewer)
                .containsExactly(tuple(false, true));
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

    @Test
    void searchMembers는_닉네임으로_부분일치_검색한다() {
        Page<Member> result =
                memberQueryRepository.searchMembers("팔로워", null, PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(Member::getNickname)
                .containsExactlyInAnyOrder("팔로워1", "팔로워2", "탈퇴한팔로워");
    }

    @Test
    void searchMembers는_이메일로_검색한다() {
        socialAccountRepository.save(
                SocialAccount.create(target, AuthProvider.KAKAO, "target-subject", "target@example.com"));
        entityManager.flush();
        entityManager.clear();

        Page<Member> result =
                memberQueryRepository.searchMembers("target@example.com", null, PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(Member::getId).containsExactly(target.getId());
    }

    @Test
    void searchMembers는_상태로_필터링한다() {
        ReflectionTestUtils.setField(follower1, "status", MemberStatus.SUSPENDED);
        memberRepository.save(follower1);
        entityManager.flush();
        entityManager.clear();

        Page<Member> result =
                memberQueryRepository.searchMembers(null, MemberStatus.SUSPENDED, PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(Member::getId).containsExactly(follower1.getId());
    }

    @Test
    void searchMembers는_삭제된_회원도_포함한다() {
        Page<Member> result =
                memberQueryRepository.searchMembers("탈퇴한팔로워", null, PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(Member::getId).containsExactly(follower3.getId());
    }

    @Test
    void searchMembers는_페이지네이션을_지원한다() {
        Page<Member> firstPage =
                memberQueryRepository.searchMembers(null, null, PageRequest.of(0, 3));

        assertThat(firstPage.getTotalElements()).isEqualTo(9);
        assertThat(firstPage.getContent()).hasSize(3);
    }

    @Test
    void 키워드가_비어있으면_전체_활성_회원을_최신순으로_반환한다() {
        Pagination<MemberSearchRow> response =
                memberQueryRepository.searchActiveMembers(outsider.getId(), "", null, 20);

        assertThat(response.data()).extracting(MemberSearchRow::nickname)
                .contains("팔로워1", "팔로워2", "출발점", "팔로잉1", "팔로잉2")
                .doesNotContain("탈퇴한팔로워", "탈퇴한팔로잉", "제3자");
    }

    @Test
    void 키워드가_비어있어도_팔로우_그룹_가입일_id_순서는_그대로_적용된다() {
        // given: 키워드가 없으면 닉네임/이름 점수는 전원 0으로 동점이라, 실제로 순서를 가르는 건
        // 팔로우 그룹(미팔로우 우선) -> 가입일 내림차순 -> id 내림차순뿐이다.
        Member viewer = createMember("검색자6");
        Member followed = createMember("zzo팔로우중");
        Member older = createMember("zzo오래된비팔로우");
        Member newer = createMember("zzo최근비팔로우");
        Member tiedFirst = createMember("zzo동시각1");
        Member tiedSecond = createMember("zzo동시각2");
        memberRepository.saveAll(List.of(viewer, followed, older, newer, tiedFirst, tiedSecond));
        memberFollowRepository.save(MemberFollow.create(viewer, followed));
        entityManager.flush();

        Instant now = Instant.now();
        forceCreatedAt(followed, now);
        forceCreatedAt(newer, now.minusSeconds(600));
        Instant tiedAt = now.minusSeconds(1800);
        forceCreatedAt(tiedFirst, tiedAt);
        forceCreatedAt(tiedSecond, tiedAt);
        forceCreatedAt(older, now.minusSeconds(3600));
        entityManager.clear();

        // when
        Pagination<MemberSearchRow> response =
                memberQueryRepository.searchActiveMembers(viewer.getId(), "", null, 20);

        // then: 공유 픽스처(target/source 등)가 섞여 있으므로 이번 테스트가 만든 회원만 걸러
        // 상대 순서를 확인한다. tiedSecond가 tiedFirst보다 나중에 저장돼 id가 더 크므로
        // (createdAt 동점 -> id 내림차순) tiedSecond가 먼저 와야 한다.
        List<String> orderedRelevantNicknames = response.data().stream()
                .map(MemberSearchRow::nickname)
                .filter(nickname -> nickname.startsWith("zzo"))
                .toList();

        assertThat(orderedRelevantNicknames).containsExactly(
                "zzo최근비팔로우", "zzo동시각2", "zzo동시각1", "zzo오래된비팔로우", "zzo팔로우중");
    }

    @Test
    void 키워드가_공백뿐이면_전체_활성_회원을_반환한다() {
        Pagination<MemberSearchRow> response =
                memberQueryRepository.searchActiveMembers(outsider.getId(), "   ", null, 20);

        assertThat(response.data()).extracting(MemberSearchRow::nickname)
                .contains("팔로워1");
    }

    @Test
    void 키워드와_닉네임_이름_모두_불일치하는_회원은_결과에서_제외된다() {
        Pagination<MemberSearchRow> response =
                memberQueryRepository.searchActiveMembers(outsider.getId(), "존재하지않는검색어zz", null, 20);

        assertThat(response.data()).isEmpty();
    }

    @Test
    void 자기_자신은_검색_결과에서_제외된다() {
        Pagination<MemberSearchRow> response =
                memberQueryRepository.searchActiveMembers(outsider.getId(), "", null, 20);

        assertThat(response.data()).extracting(MemberSearchRow::nickname)
                .doesNotContain("제3자");
    }

    @Test
    void 닉네임이_키워드로_시작하면_포함만_하는_경우보다_먼저_노출된다() {
        Member viewer = createMember("검색자1");
        Member prefixMatch = createMember("zzs시작");
        Member containsMatch = createMember("가zzs포함");
        memberRepository.saveAll(List.of(viewer, prefixMatch, containsMatch));
        entityManager.flush();
        entityManager.clear();

        Pagination<MemberSearchRow> response =
                memberQueryRepository.searchActiveMembers(viewer.getId(), "zzs", null, 20);

        assertThat(response.data()).extracting(MemberSearchRow::nickname)
                .containsExactly("zzs시작", "가zzs포함");
    }

    @Test
    void 닉네임_점수가_같으면_이름_일치도로_2차_정렬한다() {
        Member viewer = createMember("검색자2");
        Member namePrefixMatch = Member.builder().nickname("무관1").name("zzn시작").build();
        Member nameContainsMatch = Member.builder().nickname("무관2").name("가zzn포함").build();
        memberRepository.saveAll(List.of(viewer, namePrefixMatch, nameContainsMatch));
        entityManager.flush();
        entityManager.clear();

        Pagination<MemberSearchRow> response =
                memberQueryRepository.searchActiveMembers(viewer.getId(), "zzn", null, 20);

        assertThat(response.data()).extracting(MemberSearchRow::nickname)
                .containsExactly("무관1", "무관2");
    }

    @Test
    void 이름이_없는_회원은_이름_불일치_회원과_동일한_순위로_취급된다() {
        Member viewer = createMember("검색자3");
        Member noNameMatch = Member.builder().nickname("zzm이름없음").name(null).build();
        Member nameMismatch = Member.builder().nickname("zzm이름불일치").name("전혀다른값").build();
        memberRepository.saveAll(List.of(viewer, noNameMatch, nameMismatch));
        entityManager.flush();
        entityManager.clear();

        Pagination<MemberSearchRow> response =
                memberQueryRepository.searchActiveMembers(viewer.getId(), "zzm", null, 20);

        // 닉네임 점수(둘 다 시작 일치)와 이름 점수(둘 다 0)가 동점이므로,
        // 이름이 없다고 밀리지 않고 나머지 타이브레이크(가입일/ID)로만 순서가 갈린다.
        assertThat(response.data()).extracting(MemberSearchRow::nickname)
                .containsExactlyInAnyOrder("zzm이름없음", "zzm이름불일치");
    }

    @Test
    void 내가_팔로우하지_않은_회원이_팔로우_중인_회원보다_먼저_노출된다() {
        Member viewer = createMember("검색자4");
        Member notFollowed = createMember("zzf안팔로우");
        Member followed = createMember("zzf팔로우중");
        memberRepository.saveAll(List.of(viewer, notFollowed, followed));
        memberFollowRepository.save(MemberFollow.create(viewer, followed));
        entityManager.flush();
        entityManager.clear();

        Pagination<MemberSearchRow> response =
                memberQueryRepository.searchActiveMembers(viewer.getId(), "zzf", null, 20);

        assertThat(response.data()).extracting(MemberSearchRow::nickname)
                .containsExactly("zzf안팔로우", "zzf팔로우중");
        assertThat(response.data())
                .filteredOn(item -> item.nickname().equals("zzf팔로우중"))
                .extracting(MemberSearchRow::isFollowing)
                .containsExactly(true);
    }

    @Test
    void 정지되거나_탈퇴한_회원은_검색_결과에서_제외된다() {
        Member suspended = createMember("zzs정지회원");
        Member withdrawn = createMember("zzs탈퇴회원");
        memberRepository.saveAll(List.of(suspended, withdrawn));
        ReflectionTestUtils.setField(suspended, "status", MemberStatus.SUSPENDED);
        memberRepository.save(suspended);
        withdrawn.delete();
        memberRepository.save(withdrawn);
        entityManager.flush();
        entityManager.clear();

        Pagination<MemberSearchRow> response =
                memberQueryRepository.searchActiveMembers(outsider.getId(), "zzs", null, 20);

        assertThat(response.data()).extracting(MemberSearchRow::nickname)
                .doesNotContain("zzs정지회원", "zzs탈퇴회원");
    }

    @Test
    void 신고_누적_10회_이상인_회원과_내가_신고한_회원은_검색_결과에서_제외된다() {
        Member overReported = createMember("zzr신고누적");
        Member reportedByMe = createMember("zzr내가신고");
        memberRepository.saveAll(List.of(overReported, reportedByMe));
        for (int i = 0; i < 10; i++) {
            memberRepository.increaseReportCount(overReported.getId());
        }
        reportRepository.save(Report.createMemberReport(outsider, reportedByMe, ReportCategory.OBSCENE_OR_HARMFUL, null));
        entityManager.flush();
        entityManager.clear();

        Pagination<MemberSearchRow> response =
                memberQueryRepository.searchActiveMembers(outsider.getId(), "zzr", null, 20);

        assertThat(response.data()).extracting(MemberSearchRow::nickname)
                .doesNotContain("zzr신고누적", "zzr내가신고");
    }

    @Test
    void 검색_결과를_커서_기반_페이지네이션으로_조회한다() {
        Member viewer = createMember("검색자5");
        Member notFollowedPrefix = createMember("zzc시작");
        Member notFollowedContains = createMember("가zzc포함");
        memberRepository.saveAll(List.of(viewer, notFollowedPrefix, notFollowedContains));
        entityManager.flush();
        entityManager.clear();

        Pagination<MemberSearchRow> firstPage =
                memberQueryRepository.searchActiveMembers(viewer.getId(), "zzc", null, 1);

        assertThat(firstPage.data()).extracting(MemberSearchRow::nickname).containsExactly("zzc시작");
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(firstPage.nextCursor()).isNotNull();

        // 닉네임 점수 경계(시작 일치 -> 포함 일치)를 실제로 가로지르는 페이지 전환을 검증한다.
        Pagination<MemberSearchRow> secondPage =
                memberQueryRepository.searchActiveMembers(viewer.getId(), "zzc", firstPage.nextCursor(), 1);

        assertThat(secondPage.data()).extracting(MemberSearchRow::nickname).containsExactly("가zzc포함");
        assertThat(secondPage.hasNext()).isFalse();
        assertThat(secondPage.nextCursor()).isNull();
    }

    @Test
    void 검색_결과_커서가_팔로우_그룹_경계를_넘어_페이지네이션된다() {
        Member viewer = createMember("검색자7");
        Member notFollowed = createMember("zzg안팔로우");
        Member followed = createMember("zzg팔로우중");
        memberRepository.saveAll(List.of(viewer, notFollowed, followed));
        memberFollowRepository.save(MemberFollow.create(viewer, followed));
        entityManager.flush();
        entityManager.clear();

        Pagination<MemberSearchRow> firstPage =
                memberQueryRepository.searchActiveMembers(viewer.getId(), "zzg", null, 1);

        assertThat(firstPage.data()).extracting(MemberSearchRow::nickname).containsExactly("zzg안팔로우");
        assertThat(firstPage.hasNext()).isTrue();

        // 팔로우 그룹 경계(미팔로우 -> 팔로우 중)를 실제로 가로지르는 페이지 전환을 검증한다.
        Pagination<MemberSearchRow> secondPage =
                memberQueryRepository.searchActiveMembers(viewer.getId(), "zzg", firstPage.nextCursor(), 1);

        assertThat(secondPage.data()).extracting(MemberSearchRow::nickname).containsExactly("zzg팔로우중");
        assertThat(secondPage.hasNext()).isFalse();
    }

    @Test
    void 검색_결과_커서가_이름_점수_경계를_넘어_페이지네이션된다() {
        Member viewer = createMember("검색자8");
        // 닉네임은 keyword와 무관하게 만들어 두 회원의 nicknameScore를 0으로 동점 처리하고,
        // name 필드 점수(시작 일치 -> 포함 일치)만으로 순위가 갈리게 한다.
        Member namePrefixMatch = Member.builder().nickname("무관가나다1").name("zzh시작").build();
        Member nameContainsMatch = Member.builder().nickname("무관가나다2").name("다른zzh포함").build();
        memberRepository.saveAll(List.of(viewer, namePrefixMatch, nameContainsMatch));
        entityManager.flush();
        entityManager.clear();

        Pagination<MemberSearchRow> firstPage =
                memberQueryRepository.searchActiveMembers(viewer.getId(), "zzh", null, 1);

        assertThat(firstPage.data()).extracting(MemberSearchRow::nickname).containsExactly("무관가나다1");
        assertThat(firstPage.hasNext()).isTrue();

        // 이름 점수 경계(이름 시작 일치 -> 이름 포함 일치)를 실제로 가로지르는 페이지 전환을 검증한다.
        Pagination<MemberSearchRow> secondPage =
                memberQueryRepository.searchActiveMembers(viewer.getId(), "zzh", firstPage.nextCursor(), 1);

        assertThat(secondPage.data()).extracting(MemberSearchRow::nickname).containsExactly("무관가나다2");
        assertThat(secondPage.hasNext()).isFalse();
    }

    @Test
    void 검색_결과_커서가_가입일_경계를_넘어_페이지네이션된다() {
        Member viewer = createMember("검색자9");
        Member older = createMember("zzd회원1");
        Member newer = createMember("zzd회원2");
        memberRepository.saveAll(List.of(viewer, older, newer));
        entityManager.flush();
        Instant now = Instant.now();
        forceCreatedAt(older, now.minusSeconds(3600));
        forceCreatedAt(newer, now);
        entityManager.clear();

        Pagination<MemberSearchRow> firstPage =
                memberQueryRepository.searchActiveMembers(viewer.getId(), "zzd", null, 1);

        assertThat(firstPage.data()).extracting(MemberSearchRow::nickname).containsExactly("zzd회원2");
        assertThat(firstPage.hasNext()).isTrue();

        // 가입일 경계를 실제로 가로지르는 페이지 전환을 검증한다(닉네임/이름 점수는 둘 다 동일).
        Pagination<MemberSearchRow> secondPage =
                memberQueryRepository.searchActiveMembers(viewer.getId(), "zzd", firstPage.nextCursor(), 1);

        assertThat(secondPage.data()).extracting(MemberSearchRow::nickname).containsExactly("zzd회원1");
        assertThat(secondPage.hasNext()).isFalse();
    }

    @Test
    void 검색_결과_커서가_id_경계를_넘어_페이지네이션된다() {
        Member viewer = createMember("검색자10");
        Member firstInserted = createMember("zzi회원1");
        Member secondInserted = createMember("zzi회원2");
        memberRepository.saveAll(List.of(viewer, firstInserted, secondInserted));
        entityManager.flush();
        Instant tiedAt = Instant.now();
        forceCreatedAt(firstInserted, tiedAt);
        forceCreatedAt(secondInserted, tiedAt);
        entityManager.clear();

        // 닉네임/이름 점수와 가입일이 모두 같으므로 id 내림차순(나중에 저장돼 id가 더 큰 쪽이 먼저)만 남는다.
        Pagination<MemberSearchRow> firstPage =
                memberQueryRepository.searchActiveMembers(viewer.getId(), "zzi", null, 1);

        assertThat(firstPage.data()).extracting(MemberSearchRow::nickname).containsExactly("zzi회원2");
        assertThat(firstPage.hasNext()).isTrue();

        // id 경계를 실제로 가로지르는 페이지 전환을 검증한다.
        Pagination<MemberSearchRow> secondPage =
                memberQueryRepository.searchActiveMembers(viewer.getId(), "zzi", firstPage.nextCursor(), 1);

        assertThat(secondPage.data()).extracting(MemberSearchRow::nickname).containsExactly("zzi회원1");
        assertThat(secondPage.hasNext()).isFalse();
    }

    @Test
    void 잘못된_형식의_커서로_검색하면_예외가_발생한다() {
        assertThatThrownBy(() ->
                memberQueryRepository.searchActiveMembers(outsider.getId(), "", "invalid-cursor", 10))
                .isInstanceOfSatisfying(MemberException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.INVALID_CURSOR));
    }

    // created_at 컬럼은 @Column(updatable = false)라 ReflectionTestUtils.setField 후 재저장해도
    // Hibernate가 UPDATE 문에서 그 컬럼을 제외해 DB에는 반영되지 않는다. 정렬 순서를 결정적으로
    // 재현해야 하는 테스트에서는 JPQL/엔티티 저장을 우회하는 직접 SQL UPDATE로 강제한다.
    private void forceCreatedAt(Member member, Instant createdAt) {
        jdbcTemplate.update("UPDATE member SET created_at = ? WHERE id = ?", Timestamp.from(createdAt), member.getId());
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
