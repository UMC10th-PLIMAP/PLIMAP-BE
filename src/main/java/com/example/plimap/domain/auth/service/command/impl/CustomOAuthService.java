package com.example.plimap.domain.auth.service.command.impl;

import com.example.plimap.domain.auth.dto.KakaoDTO;
import com.example.plimap.domain.auth.dto.OAuthDTO;
import com.example.plimap.domain.auth.entity.OAuthMember;
import com.example.plimap.domain.auth.entity.SocialAccount;
import com.example.plimap.domain.auth.enums.AuthProvider;
import com.example.plimap.domain.auth.repository.SocialAccountRepository;
import com.example.plimap.domain.member.converter.MemberConverter;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.exception.MemberErrorCode;
import com.example.plimap.domain.member.exception.MemberException;
import com.example.plimap.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuthService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final PlatformTransactionManager transactionManager;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuthUser = super.loadUser(userRequest);

        AuthProvider provider;
        try {
            provider = AuthProvider.valueOf(
                    userRequest.getClientRegistration().getRegistrationId().toUpperCase()
            );
        } catch (IllegalArgumentException e) {
            throw new MemberException(MemberErrorCode.NOT_SUPPORT_SOCIAL_PROVIDER, e);
        }

        OAuthDTO dto = switch (provider) {
            case KAKAO -> {
                if (!(oAuthUser.getAttribute("id") instanceof Number idNumber)) {
                    throw new MemberException(MemberErrorCode.INVALID_SOCIAL_PROFILE);
                }
                long id = idNumber.longValue();
                Map<String, Object> kakaoAccount = oAuthUser.getAttribute("kakao_account");
                if (kakaoAccount == null) {
                    throw new MemberException(MemberErrorCode.INVALID_SOCIAL_PROFILE);
                }
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                if (!(profile != null && profile.get("nickname") instanceof String nickname)) {
                    throw new MemberException(MemberErrorCode.INVALID_SOCIAL_PROFILE);
                }
                String providerSubject = String.valueOf(id);
                String email = (String) kakaoAccount.get("email");
                yield new KakaoDTO(providerSubject, email, nickname);
            }
            default -> throw new MemberException(MemberErrorCode.NOT_SUPPORT_SOCIAL_PROVIDER);
        };

        Member member = socialAccountRepository
                .findByProviderAndProviderSubject(provider, dto.getProviderSubject())
                .map(SocialAccount::getMember)
                .orElseGet(() -> createMemberWithSocialAccount(provider, dto));

        return new OAuthMember(member, oAuthUser.getAttributes());
    }

    private Member createMemberWithSocialAccount(AuthProvider provider, OAuthDTO dto) {
        TransactionTemplate requiresNew = new TransactionTemplate(transactionManager);
        requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        try {
            return requiresNew.execute(status -> {
                Member newMember = MemberConverter.toMember(dto);
                memberRepository.save(newMember);
                SocialAccount socialAccount = SocialAccount.create(
                        newMember, dto.getProvider(), dto.getProviderSubject(), dto.getEmail());
                socialAccountRepository.save(socialAccount);
                return newMember;
            });
        } catch (DataIntegrityViolationException e) {
            // 동시에 처음 로그인하는 경우 UNIQUE 제약(provider, provider_subject)에 걸릴 수 있으므로,
            // 먼저 커밋된 계정을 다시 조회해 그 회원으로 로그인 처리한다.
            return socialAccountRepository.findByProviderAndProviderSubject(provider, dto.getProviderSubject())
                    .map(SocialAccount::getMember)
                    .orElseThrow(() -> e);
        }
    }
}
