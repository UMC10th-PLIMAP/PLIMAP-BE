package com.example.plimap.domain.member.repository;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.entity.MemberFollow;
import com.example.plimap.domain.member.enums.MemberStatus;
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
    private Member suspendedFollower;

    @BeforeEach
    void setup() {
        target = createMember("대상", MemberStatus.ACTIVE);
        activeFollower = createMember("활성팔로워", MemberStatus.ACTIVE);
        withdrawnFollower = createMember("탈퇴한팔로워", MemberStatus.ACTIVE);
        suspendedFollower = createMember("정지된팔로워", MemberStatus.SUSPENDED);
        memberRepository.saveAll(List.of(target, activeFollower, withdrawnFollower, suspendedFollower));

        memberFollowRepository.save(MemberFollow.create(activeFollower, target));
        memberFollowRepository.save(MemberFollow.create(withdrawnFollower, target));
        memberFollowRepository.save(MemberFollow.create(suspendedFollower, target));

        withdrawnFollower.delete();

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void 탈퇴하거나_활성_상태가_아닌_팔로워는_제외하고_조회한다() {
        // when
        List<MemberFollow> result =
                memberFollowRepository.findAllByIdFollowingId(target.getId(), MemberStatus.ACTIVE);

        // then
        assertThat(result)
                .extracting(memberFollow -> memberFollow.getFollower().getNickname())
                .containsExactly("활성팔로워");
    }

    @Test
    void deleteByIdFollowerId는_해당_회원이_팔로워인_관계를_모두_삭제한다() {
        // when
        long deleted = memberFollowRepository.deleteByIdFollowerId(activeFollower.getId());

        // then
        assertThat(deleted).isEqualTo(1);
        assertThat(memberFollowRepository.count()).isEqualTo(2);
    }

    @Test
    void deleteByIdFollowingId는_해당_회원을_팔로우하는_관계를_모두_삭제한다() {
        // when
        long deleted = memberFollowRepository.deleteByIdFollowingId(target.getId());

        // then
        assertThat(deleted).isEqualTo(3);
        assertThat(memberFollowRepository.count()).isEqualTo(0);
    }

    private Member createMember(String nickname, MemberStatus status) {
        return Member.builder()
                .nickname(nickname)
                .name("이름")
                .introduction("소개")
                .profileImageObjectKey("key")
                .status(status)
                .build();
    }
}
