package com.example.plimap.domain.member.dto.response;

import com.example.plimap.domain.member.entity.MemberTermsAgreement;
import com.example.plimap.domain.member.entity.Terms;
import com.example.plimap.domain.member.enums.TermsType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public class TermsResDTO {

    public record Item(
            TermsType type,
            String version,
            String title,
            String content,
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
    }
}
