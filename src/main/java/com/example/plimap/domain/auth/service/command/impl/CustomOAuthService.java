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
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuthService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;
    private final SocialAccountRepository socialAccountRepository;

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
                if (!(oAuthUser.getAttribute("id") instanceof Long id)) {
                    throw new MemberException(MemberErrorCode.INVALID_SOCIAL_PROFILE);
                }
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
                .orElseGet(() -> {
                    Member newMember = MemberConverter.toMember(dto);
                    memberRepository.save(newMember);
                    SocialAccount socialAccount = SocialAccount.create(
                            newMember, dto.getProvider(), dto.getProviderSubject(), dto.getEmail());
                    socialAccountRepository.save(socialAccount);
                    return newMember;
                });

        return new OAuthMember(member, oAuthUser.getAttributes());
    }
}
