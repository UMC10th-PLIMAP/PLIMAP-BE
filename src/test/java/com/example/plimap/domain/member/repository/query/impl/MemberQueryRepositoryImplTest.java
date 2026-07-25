package com.example.plimap.domain.member.repository.query.impl;

import com.example.plimap.domain.member.dto.Pagination;
import com.example.plimap.domain.member.dto.response.MemberResDTO;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.entity.MemberFollow;
import com.example.plimap.domain.member.repository.MemberFollowRepository;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.domain.member.repository.query.MemberQueryRepository;
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
    private EntityManager entityManager;

    private Member target;
    private Member follower1;
    private Member follower2;
    private Member follower3;

    @BeforeEach
    void setup() {
        target = createMember("대상");
        follower1 = createMember("팔로워1");
        follower2 = createMember("팔로워2");
        follower3 = createMember("탈퇴한팔로워");
        memberRepository.saveAll(List.of(target, follower1, follower2, follower3));

        memberFollowRepository.save(MemberFollow.create(follower1, target));
        memberFollowRepository.save(MemberFollow.create(follower2, target));
        memberFollowRepository.save(MemberFollow.create(follower3, target));

        follower3.delete();

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void 팔로워_목록을_최신순으로_조회한다() {
        Pagination<MemberResDTO.FollowerItem> response =
                memberQueryRepository.findFollowersByMemberId(target.getId(), null, 10);

        assertThat(response.data()).extracting(MemberResDTO.FollowerItem::nickname)
                .containsExactly("팔로워2", "팔로워1");
        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursor()).isNull();
    }

    @Test
    void 탈퇴한_회원은_팔로워_목록에서_제외된다() {
        Pagination<MemberResDTO.FollowerItem> response =
                memberQueryRepository.findFollowersByMemberId(target.getId(), null, 10);

        assertThat(response.data()).extracting(MemberResDTO.FollowerItem::nickname)
                .doesNotContain("탈퇴한팔로워");
    }

    @Test
    void 팔로워_목록을_커서_기반_페이지네이션으로_조회한다() {
        Pagination<MemberResDTO.FollowerItem> firstPage =
                memberQueryRepository.findFollowersByMemberId(target.getId(), null, 1);

        assertThat(firstPage.data()).hasSize(1);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(firstPage.nextCursor()).isNotNull();

        Pagination<MemberResDTO.FollowerItem> secondPage =
                memberQueryRepository.findFollowersByMemberId(target.getId(), firstPage.nextCursor(), 1);

        assertThat(secondPage.data()).hasSize(1);
        assertThat(secondPage.hasNext()).isFalse();
        assertThat(secondPage.nextCursor()).isNull();
        assertThat(secondPage.data().get(0).nickname()).isEqualTo("팔로워1");
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
