package com.example.plimap.domain.member.service.query;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.enums.NicknameCheckFailReason;

public interface MemberQueryService {

    Member getActiveMember(Long memberId);

    boolean isNicknameAvailable(String nickname);

    NicknameCheckFailReason checkNicknameFailReason(String nickname);
}
