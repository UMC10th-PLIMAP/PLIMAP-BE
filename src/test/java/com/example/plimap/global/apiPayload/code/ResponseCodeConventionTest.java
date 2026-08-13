package com.example.plimap.global.apiPayload.code;

import com.example.plimap.domain.admin.exception.AdminErrorCode;
import com.example.plimap.domain.admin.exception.AdminSuccessCode;
import com.example.plimap.domain.auth.exception.AuthErrorCode;
import com.example.plimap.domain.auth.exception.AuthSuccessCode;
import com.example.plimap.domain.home.exception.HomeSuccessCode;
import com.example.plimap.domain.inquiry.exception.InquiryErrorCode;
import com.example.plimap.domain.inquiry.exception.InquirySuccessCode;
import com.example.plimap.domain.member.exception.MemberErrorCode;
import com.example.plimap.domain.member.exception.MemberSuccessCode;
import com.example.plimap.domain.member.exception.TermsErrorCode;
import com.example.plimap.domain.member.exception.TermsSuccessCode;
import com.example.plimap.domain.notification.exception.NotificationSuccessCode;
import com.example.plimap.domain.pin.exception.PinErrorCode;
import com.example.plimap.domain.pin.exception.PinSuccessCode;
import com.example.plimap.domain.pin.exception.TagErrorCode;
import com.example.plimap.domain.place.exception.PlaceErrorCode;
import com.example.plimap.domain.place.exception.PlaceSuccessCode;
import com.example.plimap.domain.report.exception.ReportErrorCode;
import com.example.plimap.domain.report.exception.ReportSuccessCode;
import com.example.plimap.domain.track.exception.TrackErrorCode;
import com.example.plimap.domain.track.exception.TrackSuccessCode;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseCodeConventionTest {

    private static final Pattern HTTP_STATUS_SEGMENT = Pattern.compile("_[1-5]\\d{2}_");
    private static final Pattern COMMON_ERROR_CODE = Pattern.compile(
            "^COMMON_[1-5]\\d{2}_[A-Z0-9]+(?:_[A-Z0-9]+)*$"
    );

    @Test
    void 성공_코드는_도메인과_유스케이스와_SUCCESS로_구성된다() {
        // given
        List<SuccessCodeGroup> groups = successCodeGroups();

        // when & then
        groups.forEach(group -> group.codes().forEach(code -> {
            Pattern pattern = Pattern.compile(
                    "^" + Pattern.quote(group.domain()) + "_[A-Z0-9]+(?:_[A-Z0-9]+)*_SUCCESS$"
            );
            assertThat(code.getCode())
                    .as("success code: %s", code)
                    .matches(pattern);
            assertThat(HTTP_STATUS_SEGMENT.matcher(code.getCode()).find())
                    .as("success code must not contain HTTP status: %s", code.getCode())
                    .isFalse();
        }));
    }

    @Test
    void 도메인_에러는_도메인과_실패_이유로_구성된다() {
        // given
        List<ErrorCodeGroup> groups = domainErrorCodeGroups();

        // when & then
        groups.forEach(group -> group.codes().forEach(code -> {
            Pattern pattern = Pattern.compile(
                    "^" + Pattern.quote(group.domain()) + "_[A-Z0-9]+(?:_[A-Z0-9]+)*$"
            );
            assertThat(code.getCode())
                    .as("domain error code: %s", code)
                    .matches(pattern)
                    .doesNotEndWith("_SUCCESS");
            assertThat(HTTP_STATUS_SEGMENT.matcher(code.getCode()).find())
                    .as("domain error code must not contain HTTP status: %s", code.getCode())
                    .isFalse();
        }));
    }

    @Test
    void 공통_에러는_COMMON과_HTTP_상태와_실패_이유로_구성된다() {
        // given
        List<GeneralErrorCode> codes = Arrays.asList(GeneralErrorCode.values());

        // when & then
        codes.forEach(code -> {
            assertThat(code.getCode())
                    .as("common error code: %s", code)
                    .matches(COMMON_ERROR_CODE)
                    .startsWith("COMMON_" + code.getStatus().value() + "_");
        });
    }

    @Test
    void 모든_응답_코드_문자열은_중복되지_않는다() {
        // given
        Stream<String> successCodes = successCodeGroups().stream()
                .flatMap(group -> group.codes().stream())
                .map(BaseSuccessCode::getCode);
        Stream<String> domainErrorCodes = domainErrorCodeGroups().stream()
                .flatMap(group -> group.codes().stream())
                .map(BaseErrorCode::getCode);
        Stream<String> commonErrorCodes = Arrays.stream(GeneralErrorCode.values())
                .map(BaseErrorCode::getCode);

        // when
        List<String> allCodes = Stream.concat(successCodes, Stream.concat(domainErrorCodes, commonErrorCodes))
                .toList();

        // then
        assertThat(allCodes).doesNotHaveDuplicates();
    }

    private static List<SuccessCodeGroup> successCodeGroups() {
        return List.of(
                new SuccessCodeGroup("ADMIN", successCodes(AdminSuccessCode.values())),
                new SuccessCodeGroup("AUTH", successCodes(AuthSuccessCode.values())),
                new SuccessCodeGroup("HOME", successCodes(HomeSuccessCode.values())),
                new SuccessCodeGroup("INQUIRY", successCodes(InquirySuccessCode.values())),
                new SuccessCodeGroup("MEMBER", successCodes(MemberSuccessCode.values())),
                new SuccessCodeGroup("TERMS", successCodes(TermsSuccessCode.values())),
                new SuccessCodeGroup("NOTIFICATION", successCodes(NotificationSuccessCode.values())),
                new SuccessCodeGroup("PIN", successCodes(PinSuccessCode.values())),
                new SuccessCodeGroup("PLACE", successCodes(PlaceSuccessCode.values())),
                new SuccessCodeGroup("REPORT", successCodes(ReportSuccessCode.values())),
                new SuccessCodeGroup("TRACK", successCodes(TrackSuccessCode.values())),
                new SuccessCodeGroup("COMMON", successCodes(GeneralSuccessCode.values()))
        );
    }

    private static List<ErrorCodeGroup> domainErrorCodeGroups() {
        return List.of(
                new ErrorCodeGroup("ADMIN", errorCodes(AdminErrorCode.values())),
                new ErrorCodeGroup("AUTH", errorCodes(AuthErrorCode.values())),
                new ErrorCodeGroup("INQUIRY", errorCodes(InquiryErrorCode.values())),
                new ErrorCodeGroup("MEMBER", errorCodes(MemberErrorCode.values())),
                new ErrorCodeGroup("TERMS", errorCodes(TermsErrorCode.values())),
                new ErrorCodeGroup("PIN", errorCodes(PinErrorCode.values())),
                new ErrorCodeGroup("TAG", errorCodes(TagErrorCode.values())),
                new ErrorCodeGroup("PLACE", errorCodes(PlaceErrorCode.values())),
                new ErrorCodeGroup("REPORT", errorCodes(ReportErrorCode.values())),
                new ErrorCodeGroup("TRACK", errorCodes(TrackErrorCode.values()))
        );
    }

    private static List<BaseSuccessCode> successCodes(BaseSuccessCode... codes) {
        return List.of(codes);
    }

    private static List<BaseErrorCode> errorCodes(BaseErrorCode... codes) {
        return List.of(codes);
    }

    private record SuccessCodeGroup(String domain, List<BaseSuccessCode> codes) {}

    private record ErrorCodeGroup(String domain, List<BaseErrorCode> codes) {}
}