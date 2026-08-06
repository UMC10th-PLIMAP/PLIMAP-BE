package com.example.plimap.domain.member.service.query.impl;

import com.example.plimap.domain.member.converter.MemberConverter;
import com.example.plimap.domain.member.dto.Pagination;
import com.example.plimap.domain.member.dto.response.MemberResDTO;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.entity.MemberFollow;
import com.example.plimap.domain.member.entity.MemberFollowId;
import com.example.plimap.domain.member.enums.MemberStatus;
import com.example.plimap.domain.member.enums.NicknameCheckFailReason;
import com.example.plimap.domain.member.exception.MemberErrorCode;
import com.example.plimap.domain.member.exception.MemberException;
import com.example.plimap.domain.member.repository.MemberFollowRepository;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.domain.member.repository.query.MemberFollowRow;
import com.example.plimap.domain.member.repository.query.MemberQueryRepository;
import com.example.plimap.domain.member.service.query.MemberQueryService;
import com.example.plimap.domain.pin.service.query.PinQueryService;
import com.example.plimap.global.external.storage.ProfileImageStorage;
import com.vane.badwordfiltering.BadWordFiltering;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberQueryServiceImpl implements MemberQueryService {

    private static final int NICKNAME_MIN_LENGTH = 2;
    private static final int NICKNAME_MAX_LENGTH = 10;
    private static final Pattern NICKNAME_FORMAT = Pattern.compile("^[가-힣A-Za-z0-9]+$");
    // BadWordFiltering.check()는 대소문자를 구분하는 완전 일치 substring 검사라 브랜드 사칭 방지용 단어는 별도로 대소문자 무시 검사한다.
    private static final List<String> CUSTOM_FORBIDDEN_WORDS = List.of("plimap", "플리맵운영자", "플리맵사용자");
    // 벌점 부여 시 프로필 닉네임을 강제 치환할 때 사용하는 후보 풀(조류 이름).
    // 관리자 페이지에서 후보 풀을 직접 관리하는 기능은 별도 스코프.
    private static final List<String> PENALTY_NICKNAME_POOL = List.of(
            "참새", "까치", "제비", "부엉이", "올빼미", "딱따구리", "종달새", "뻐꾸기", "두루미", "백로",
            "왜가리", "갈매기", "앵무새", "공작새", "독수리", "기러기", "오리", "백조", "홍학", "펭귄",
            "타조", "벌새", "딱새", "굴뚝새", "박새", "직박구리", "물총새", "소쩍새", "뜸부기", "꾀꼬리",
            "방울새", "콩새", "되새", "개똥지빠귀", "파랑새", "후투티", "황조롱이", "붉은배새매", "매사촌", "검은딱새"
    );
    private static final Random RANDOM = new Random();
    // 조류 이름 x 0~999 조합(최대 40,000개)도 이론상 소진될 수 있으므로 무한 루프 대신 시도 횟수를 제한한다.
    private static final int FALLBACK_MAX_ATTEMPTS = 100;

    private final MemberRepository memberRepository;
    private final MemberFollowRepository memberFollowRepository;
    private final MemberQueryRepository memberQueryRepository;
    private final ProfileImageStorage profileImageStorage;
    private final PinQueryService pinQueryService;
    private final BadWordFiltering badWordFiltering = new BadWordFiltering();

    @Override
    public Member getActiveMember(Long memberId) {
        return memberRepository.findByIdAndStatusAndDeletedAtIsNull(memberId, MemberStatus.ACTIVE)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    @Override
    public List<Member> findAllFollowers(Long memberId) {
        return memberFollowRepository.findAllByIdFollowingId(memberId, MemberStatus.ACTIVE).stream()
                .map(MemberFollow::getFollower)
                .toList();
    }

    @Override
    public boolean isNicknameAvailable(String nickname) {
        return !memberRepository.existsByNicknameIgnoreCaseAndDeletedAtIsNull(nickname);
    }

    @Override
    public String pickAvailablePenaltyNickname() {
        List<String> shuffled = new ArrayList<>(PENALTY_NICKNAME_POOL);
        Collections.shuffle(shuffled);
        for (String candidate : shuffled) {
            if (isNicknameAvailable(candidate)) {
                return candidate;
            }
        }
        // 후보 풀이 전부 소진된 극단적 상황을 대비한 폴백. 시도 횟수를 제한해 폴백 공간마저
        // 소진됐을 때 무한 루프에 빠지지 않고 명시적인 오류로 실패한다.
        for (int attempt = 0; attempt < FALLBACK_MAX_ATTEMPTS; attempt++) {
            String base = PENALTY_NICKNAME_POOL.get(RANDOM.nextInt(PENALTY_NICKNAME_POOL.size()));
            String fallback = base + RANDOM.nextInt(1000);
            if (isNicknameAvailable(fallback)) {
                return fallback;
            }
        }
        throw new MemberException(MemberErrorCode.PENALTY_NICKNAME_POOL_EXHAUSTED);
    }

    @Override
    public boolean isNicknameForbidden(String nickname) {
        String lowerNickname = nickname.toLowerCase(Locale.ROOT);
        return badWordFiltering.check(nickname) || CUSTOM_FORBIDDEN_WORDS.stream().anyMatch(lowerNickname::contains);
    }

    @Override
    public NicknameCheckFailReason checkNicknameFailReason(String nickname) {
        if (nickname.length() < NICKNAME_MIN_LENGTH) {
            return NicknameCheckFailReason.TOO_SHORT;
        }
        if (nickname.length() > NICKNAME_MAX_LENGTH) {
            return NicknameCheckFailReason.TOO_LONG;
        }
        if (!NICKNAME_FORMAT.matcher(nickname).matches()) {
            return NicknameCheckFailReason.INVALID_FORMAT;
        }
        if (isNicknameForbidden(nickname)) {
            return NicknameCheckFailReason.FORBIDDEN_WORD;
        }
        if (!isNicknameAvailable(nickname)) {
            return NicknameCheckFailReason.DUPLICATE;
        }
        return null;
    }

    @Override
    public MemberResDTO.MyProfile getMyProfile(Long memberId) {
        Member member = getActiveMember(memberId);
        long followerCount = memberFollowRepository.countByIdFollowingId(memberId);
        long followingCount = memberFollowRepository.countByIdFollowerId(memberId);
        String profileImageUrl = profileImageStorage.getPublicUrlOrNull(member.getProfileImageObjectKey());
        return MemberConverter.toMyProfile(member, profileImageUrl, followerCount, followingCount);
    }

    @Override
    public MemberResDTO.OtherProfile getOtherProfile(Long viewerId, Long targetMemberId) {
        if (viewerId.equals(targetMemberId)) {
            throw new MemberException(MemberErrorCode.CANNOT_VIEW_SELF_PROFILE);
        }

        Member member = getVisibleActiveMember(targetMemberId, viewerId);
        long followerCount = memberFollowRepository.countByIdFollowingId(targetMemberId);
        long followingCount = memberFollowRepository.countByIdFollowerId(targetMemberId);
        boolean isFollowing = memberFollowRepository.existsById(new MemberFollowId(viewerId, targetMemberId));
        long pinCount = pinQueryService.countPinsByMemberId(targetMemberId);
        String profileImageUrl = profileImageStorage.getPublicUrlOrNull(member.getProfileImageObjectKey());
        return MemberConverter.toOtherProfile(member, profileImageUrl, followerCount, followingCount, isFollowing, pinCount);
    }

    @Override
    public Pagination<MemberResDTO.FollowerItem> findFollowers(Long viewerId, Long memberId, String cursor, Integer pageSize) {
        getVisibleActiveMember(memberId, viewerId);
        Pagination<MemberFollowRow> rows = memberQueryRepository.findFollowersByMemberId(viewerId, memberId, cursor, pageSize);

        List<MemberResDTO.FollowerItem> data = rows.data().stream()
                .map(row -> MemberConverter.toFollowerItem(row, profileImageStorage.getPublicUrlOrNull(row.profileImageObjectKey())))
                .toList();

        return MemberConverter.toPagination(data, rows.nextCursor(), rows.hasNext(), rows.pageSize());
    }

    @Override
    public Pagination<MemberResDTO.FollowingItem> findFollowing(Long viewerId, Long memberId, String cursor, Integer pageSize) {
        getVisibleActiveMember(memberId, viewerId);
        Pagination<MemberFollowRow> rows = memberQueryRepository.findFollowingByMemberId(viewerId, memberId, cursor, pageSize);

        List<MemberResDTO.FollowingItem> data = rows.data().stream()
                .map(row -> MemberConverter.toFollowingItem(row, profileImageStorage.getPublicUrlOrNull(row.profileImageObjectKey())))
                .toList();

        return MemberConverter.toPagination(data, rows.nextCursor(), rows.hasNext(), rows.pageSize());
    }

    private Member getVisibleActiveMember(Long targetMemberId, Long viewerId) {
        return memberQueryRepository.findVisibleActiveMember(targetMemberId, viewerId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    }
}
