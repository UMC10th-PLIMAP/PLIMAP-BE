package com.example.plimap.domain.member.controller;

import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.member.controller.docs.MemberControllerDocs;
import com.example.plimap.domain.member.converter.MemberConverter;
import com.example.plimap.domain.member.dto.Pagination;
import com.example.plimap.domain.member.dto.request.MemberReqDTO;
import com.example.plimap.domain.member.dto.response.MemberResDTO;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.enums.NicknameCheckFailReason;
import com.example.plimap.domain.member.exception.MemberSuccessCode;
import com.example.plimap.domain.member.service.command.MemberCommandService;
import com.example.plimap.domain.member.service.query.MemberQueryService;
import com.example.plimap.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController implements MemberControllerDocs {

    private final MemberQueryService memberQueryService;
    private final MemberCommandService memberCommandService;

    @Override
    @GetMapping("/nickname/check")
    public ApiResponse<MemberResDTO.NicknameCheck> checkNickname(@RequestParam String nickname) {
        NicknameCheckFailReason reason = memberQueryService.checkNicknameFailReason(nickname);
        return ApiResponse.success(MemberSuccessCode.NICKNAME_CHECKED, MemberConverter.toNicknameCheck(nickname, reason));
    }

    @Override
    @GetMapping("/me")
    public ApiResponse<MemberResDTO.MyProfile> getMyProfile(@AuthenticationPrincipal AuthMember authMember) {
        MemberResDTO.MyProfile profile = memberQueryService.getMyProfile(authMember.getMember().getId());
        return ApiResponse.success(MemberSuccessCode.MY_PROFILE_FETCHED, profile);
    }

    @Override
    @GetMapping("/{memberId}")
    public ApiResponse<MemberResDTO.OtherProfile> getOtherProfile(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long memberId
    ) {
        MemberResDTO.OtherProfile profile = memberQueryService.getOtherProfile(authMember.getMember().getId(), memberId);
        return ApiResponse.success(MemberSuccessCode.OTHER_PROFILE_FETCHED, profile);
    }

    @Override
    @PatchMapping("/me")
    public ApiResponse<MemberResDTO.Profile> updateProfile(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody MemberReqDTO.UpdateProfile request
    ) {
        Member member = memberCommandService.updateProfile(authMember.getMember().getId(), request);
        return ApiResponse.success(MemberSuccessCode.PROFILE_UPDATED, MemberConverter.toProfile(member));
    }

    @Override
    @PostMapping("/{memberId}/follow")
    public ApiResponse<Void> follow(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long memberId
    ) {
        memberCommandService.follow(authMember.getMember().getId(), memberId);
        return ApiResponse.success(MemberSuccessCode.FOLLOWED, null);
    }

    @Override
    @DeleteMapping("/{memberId}/follow")
    public ApiResponse<Void> unfollow(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long memberId
    ) {
        memberCommandService.unfollow(authMember.getMember().getId(), memberId);
        return ApiResponse.success(MemberSuccessCode.UNFOLLOWED, null);
    }

    @Override
    @GetMapping("/{memberId}/followers")
    public ApiResponse<Pagination<MemberResDTO.FollowerItem>> getFollowers(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long memberId,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String cursor
    ) {
        Pagination<MemberResDTO.FollowerItem> response =
                memberQueryService.findFollowers(authMember.getMember().getId(), memberId, cursor, pageSize);
        return ApiResponse.success(MemberSuccessCode.FOLLOWERS_FETCHED, response);
    }

    @Override
    @GetMapping("/{memberId}/following")
    public ApiResponse<Pagination<MemberResDTO.FollowingItem>> getFollowing(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long memberId,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String cursor
    ) {
        Pagination<MemberResDTO.FollowingItem> response =
                memberQueryService.findFollowing(authMember.getMember().getId(), memberId, cursor, pageSize);
        return ApiResponse.success(MemberSuccessCode.FOLLOWING_FETCHED, response);
    }
}
