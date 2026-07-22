package com.example.plimap.domain.member.service.query;

import com.example.plimap.domain.member.enums.NicknameCheckFailReason;

public interface MemberQueryService {

    boolean isNicknameAvailable(String nickname);

    NicknameCheckFailReason checkNicknameFailReason(String nickname);
}
