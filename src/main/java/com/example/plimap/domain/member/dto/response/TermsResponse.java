package com.example.plimap.domain.member.dto.response;

import com.example.plimap.domain.member.entity.MemberTermsAgreement;
import com.example.plimap.domain.member.entity.Terms;
import com.example.plimap.domain.member.enums.TermsType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public class TermsResponse {

    public record Item(
            @Schema(example = "SERVICE")
            TermsType type,

            @Schema(example = "v1")
            String version,

            @Schema(description = "약관 제목")
            String title,

            @Schema(description = "약관 본문 내용")
            String content,

            @Schema(example = "true")
            boolean required
    ) {
        public static Item from(Terms terms) {
            return new Item(
                    terms.getType(),
                    terms.getVersion(),
                    terms.getTitle(),
                    terms.getContent(),
                    terms.isRequired()
            );
        }
    }

    public record Result(
            @Schema(example = "SERVICE")
            TermsType type,

            @Schema(example = "true")
            boolean agreed,

            @Schema(example = "2026-07-13T07:19:16.301Z")
            Instant agreedAt
    ) {
        public static Result from(MemberTermsAgreement agreement) {
            return new Result(
                    agreement.getTerms().getType(),
                    agreement.isAgreed(),
                    agreement.getAgreedAt()
            );
        }

        public static Result notAgreed(Terms terms) {
            return new Result(terms.getType(), false, null);
        }
    }
}
