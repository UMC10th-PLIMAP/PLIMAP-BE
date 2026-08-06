package com.example.plimap.domain.member.controller.docs;

import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.member.dto.Pagination;
import com.example.plimap.domain.member.dto.request.MemberReqDTO;
import com.example.plimap.domain.member.dto.response.MemberResDTO;
import com.example.plimap.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Member", description = "회원 API")
public interface MemberControllerDocs {

    @Operation(
            summary = "닉네임 중복 확인",
            description = """
                    온보딩/프로필 수정 전에 닉네임 사용 가능 여부를 확인합니다.

                    `available`이 false이면 `reason`으로 실패 사유를 알려줍니다.
                    - TOO_SHORT: 두 글자 이상 입력해 주세요.
                    - TOO_LONG: 최대 10자까지만 입력할 수 있어요.
                    - INVALID_FORMAT: 한글, 영문, 숫자만 사용 가능하며 공백은 포함할 수 없어요.
                    - FORBIDDEN_WORD: 부적절하거나 사용할 수 없는 단어가 포함되어 있어요.
                    - DUPLICATE: 이미 사용 중인 닉네임이에요.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            content = @Content(examples = {
                    @ExampleObject(name = "사용 가능", value = """
                            {
                              "isSuccess": true,
                              "code": "MEMBER_200_NICKNAME_CHECKED",
                              "message": "닉네임 사용 가능 여부를 조회했습니다.",
                              "result": { "nickname": "예림", "available": true, "reason": null }
                            }
                            """),
                    @ExampleObject(name = "이미 사용 중", value = """
                            {
                              "isSuccess": true,
                              "code": "MEMBER_200_NICKNAME_CHECKED",
                              "message": "닉네임 사용 가능 여부를 조회했습니다.",
                              "result": { "nickname": "예림", "available": false, "reason": "DUPLICATE" }
                            }
                            """)
            })
    )
    ApiResponse<MemberResDTO.NicknameCheck> checkNickname(String nickname);

    @Operation(
            summary = "내 프로필 조회",
            description = "로그인한 회원 자신의 프로필 정보를 조회합니다. 팔로워/팔로잉 수를 함께 반환합니다."
    )
    ApiResponse<MemberResDTO.MyProfile> getMyProfile(AuthMember authMember);

    @Operation(
            summary = "다른 사용자 프로필 조회",
            description = "경로의 memberId에 해당하는 회원의 프로필을 조회합니다. 팔로워/팔로잉 수, 내가 이 회원을 팔로우 중인지 여부, 이 회원이 작성한 핀 개수(pinCount)를 함께 반환합니다. pinCount는 삭제되지 않고 피드에 공개된 핀만 집계한 값입니다. 본인의 memberId로는 조회할 수 없으며, 내 프로필 조회는 `GET /api/v1/members/me`를 이용해야 합니다."
    )
    ApiResponse<MemberResDTO.OtherProfile> getOtherProfile(AuthMember authMember, Long memberId);

    @Operation(
            summary = "내 프로필 수정",
            description = "닉네임, 이름, 소개를 수정합니다. 요청에 포함하지 않은 필드는 변경되지 않습니다. 프로필 이미지는 `POST /api/v1/members/me/profile-image`를 이용해 주세요."
    )
    ApiResponse<MemberResDTO.Profile> updateProfile(AuthMember authMember, @Valid MemberReqDTO.UpdateProfile request);

    @Operation(
            summary = "프로필 이미지 업로드",
            description = """
                    로그인한 회원 자신의 프로필 이미지를 업로드합니다. multipart/form-data의 `image` 파트로 WebP 이미지 파일을 전달해 주세요.

                    - 이미지는 클라이언트에서 WebP로 인코딩해 전달해야 합니다(서버는 별도로 포맷을 변환하지 않습니다).
                    - 파일 크기는 5MB를 초과할 수 없습니다.
                    - 이미 프로필 이미지가 있던 회원이 다시 업로드하면 기존 이미지는 새 이미지로 교체되고, 이전 이미지는 스토리지에서 삭제됩니다.
                    - 응답의 imageUrl로 즉시 접근 가능한 공개 URL을 반환합니다.
                    """
    )
    ApiResponse<MemberResDTO.ProfileImage> uploadProfileImage(AuthMember authMember, MultipartFile image);

    @Operation(
            summary = "팔로우",
            description = "경로의 memberId에 해당하는 회원을 팔로우합니다. 자기 자신은 팔로우할 수 없고, 이미 팔로우 중이면 실패합니다."
    )
    ApiResponse<Void> follow(AuthMember authMember, Long memberId);

    @Operation(
            summary = "언팔로우",
            description = "경로의 memberId에 해당하는 회원을 언팔로우합니다. 자기 자신은 언팔로우할 수 없고, 팔로우 중이 아니면 실패합니다."
    )
    ApiResponse<Void> unfollow(AuthMember authMember, Long memberId);

    @Operation(
            summary = "팔로워 목록 조회",
            description = """
                    경로의 memberId에 해당하는 회원을 팔로우하는 회원 목록을 최신순으로 조회합니다.

                    커서 기반 페이지네이션을 사용합니다. 첫 페이지는 cursor 없이 요청하고, 이후에는 응답의 nextCursor를 그대로 다음 요청의 cursor로 전달합니다. pageSize는 1~50 사이여야 하며 기본값은 10입니다.

                    각 항목의 isFollowing은 목록 대상(memberId)이 아니라 로그인한 나(요청자)를 기준으로, 내가 그 사람을 팔로우하고 있는지를 나타냅니다. 즉 맞팔 여부를 판단할 때 씁니다.
                    """
    )
    ApiResponse<Pagination<MemberResDTO.FollowerItem>> getFollowers(
            AuthMember authMember,
            Long memberId,
            @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
            @Max(value = 50, message = "페이지 크기는 50 이하여야 합니다.")
            Integer pageSize,
            String cursor
    );

    @Operation(
            summary = "팔로잉 목록 조회",
            description = """
                    경로의 memberId에 해당하는 회원이 팔로우하는 회원 목록을 최신순으로 조회합니다.

                    커서 기반 페이지네이션을 사용합니다. 첫 페이지는 cursor 없이 요청하고, 이후에는 응답의 nextCursor를 그대로 다음 요청의 cursor로 전달합니다. pageSize는 1~50 사이여야 하며 기본값은 10입니다.

                    각 항목의 isFollowing은 로그인한 나(요청자)를 기준으로, 내가 그 사람을 팔로우하고 있는지를 나타냅니다. memberId 본인의 팔로잉 목록을 조회하는 경우 항상 true입니다.
                    """
    )
    ApiResponse<Pagination<MemberResDTO.FollowingItem>> getFollowing(
            AuthMember authMember,
            Long memberId,
            @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
            @Max(value = 50, message = "페이지 크기는 50 이하여야 합니다.")
            Integer pageSize,
            String cursor
    );

    @Operation(
            summary = "회원 탈퇴",
            description = """
                    로그인한 회원 자신을 자발적으로 탈퇴 처리합니다.

                    - 닉네임은 `플리맵사용자{memberId}`로 마스킹되고(원래 닉네임은 관리자 확인용으로 보존), 프로필 이미지와 자기소개는 삭제됩니다.
                    - 좋아요 목록과 등록한 핀은 유지됩니다.
                    - 팔로우/팔로워 관계는 양방향 모두 삭제됩니다.
                    - 소셜 계정 연동 정보는 삭제되어, 이후 동일한 소셜 계정으로 재가입할 수 있습니다.
                    - 처리 후 현재 세션의 액세스/리프레시 토큰을 무효화하고 로그아웃 쿠키를 반환합니다.
                    """
    )
    ApiResponse<Void> withdraw(AuthMember authMember, HttpServletRequest request, HttpServletResponse response);
}
