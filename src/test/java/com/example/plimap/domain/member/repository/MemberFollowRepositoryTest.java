package com.example.plimap.domain.member.repository;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.entity.MemberFollow;
import com.example.plimap.support.PostgisContainerConfiguration;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgisContainerConfiguration.class)
@Transactional
class MemberFollowRepositoryTest {

    @Autowired
    private MemberFollowRepository memberFollowRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager entityManager;

    private Member target;
    private Member activeFollower;
    private Member withdrawnFollower;

    @BeforeEach
    void setup() {
        target = createMember("대상");
        activeFollower = createMember("활성팔로워");
        withdrawnFollower = createMember("탈퇴한팔로워");
        memberRepository.saveAll(List.of(target, activeFollower, withdrawnFollower));

        memberFollowRepository.save(MemberFollow.create(activeFollower, target));
        memberFollowRepository.save(MemberFollow.create(withdrawnFollower, target));

        withdrawnFollower.delete();

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void 탈퇴한_팔로워는_제외하고_조회한다() {
        // when
        List<MemberFollow> result = memberFollowRepository.findAllByIdFollowingId(target.getId());

        // then
        assertThat(result)
                .extracting(memberFollow -> memberFollow.getFollower().getNickname())
                .containsExactly("활성팔로워");
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
