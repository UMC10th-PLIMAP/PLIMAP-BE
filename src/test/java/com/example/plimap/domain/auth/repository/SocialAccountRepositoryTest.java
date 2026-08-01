package com.example.plimap.domain.auth.repository;

import com.example.plimap.domain.auth.entity.SocialAccount;
import com.example.plimap.domain.auth.enums.AuthProvider;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.support.PostgisContainerConfiguration;
import jakarta.persistence.EntityManager;
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
class SocialAccountRepositoryTest {

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager entityManager;

    private Member member;
    private Member otherMember;

    @BeforeEach
    void setup() {
        member = memberRepository.save(Member.builder().build());
        otherMember = memberRepository.save(Member.builder().build());

        socialAccountRepository.save(
                SocialAccount.create(member, AuthProvider.KAKAO, "kakao-subject", "kakao@example.com"));
        socialAccountRepository.save(
                SocialAccount.create(member, AuthProvider.GOOGLE, "google-subject", "google@example.com"));
        socialAccountRepository.save(
                SocialAccount.create(otherMember, AuthProvider.KAKAO, "other-kakao-subject", "other@example.com"));

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void deleteByMemberId는_해당_회원의_소셜_계정만_모두_삭제한다() {
        // when
        long deleted = socialAccountRepository.deleteByMemberId(member.getId());

        // then
        assertThat(deleted).isEqualTo(2);
        assertThat(socialAccountRepository.count()).isEqualTo(1);
        assertThat(socialAccountRepository.findByProviderAndProviderSubject(AuthProvider.KAKAO, "other-kakao-subject"))
                .isPresent();
    }
}
