package com.example.plimap.domain.member.service.query;

import com.example.plimap.domain.member.dto.Pagination;
import com.example.plimap.domain.member.dto.response.MemberResDTO;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.enums.MemberStatus;
import com.example.plimap.domain.member.enums.NicknameCheckFailReason;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MemberQueryService {

    Member getActiveMember(Long memberId);

    Member getMemberById(Long memberId);

    Page<Member> searchMembers(String query, MemberStatus status, Pageable pageable);

    List<Member> findAllFollowers(Long memberId);

    boolean isNicknameAvailable(String nickname);

    String pickAvailablePenaltyNickname();

    boolean isNicknameForbidden(String nickname);

    NicknameCheckFailReason checkNicknameFailReason(String nickname);

    MemberResDTO.MyProfile getMyProfile(Long memberId);

    MemberResDTO.OtherProfile getOtherProfile(Long viewerId, Long targetMemberId);

    Pagination<MemberResDTO.FollowerItem> findFollowers(Long viewerId, Long memberId, String cursor, Integer pageSize);

    Pagination<MemberResDTO.FollowingItem> findFollowing(Long viewerId, Long memberId, String cursor, Integer pageSize);
}
