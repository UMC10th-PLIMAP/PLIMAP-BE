package com.example.plimap.domain.inquiry.dto.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.example.plimap.domain.inquiry.enums.InquiryCategory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InquiryRequestTest {

    private static final int MAX_LOCAL_PART_LENGTH = 64;
    private static final int MAX_DOMAIN_LABEL_LENGTH = 63;
    private static final String DOMAIN_LABEL = "a".repeat(MAX_DOMAIN_LABEL_LENGTH);
    // Hibernate Validator의 @Email은 local part(최대 64자) + '@' + domain(최대 255자)까지만 유효한 형식으로 인정하므로,
    // 형식을 지키면서 만들 수 있는 이메일의 최대 길이는 정확히 320자다(contact_email VARCHAR(320)과 동일한 경계).
    private static final String DOMAIN_255 = (DOMAIN_LABEL + ".").repeat(3) + DOMAIN_LABEL;

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void 정확히_320자인_유효한_이메일은_검증을_통과한다() {
        // given
        String email320 = "a".repeat(MAX_LOCAL_PART_LENGTH) + "@" + DOMAIN_255;
        assertThat(email320).hasSize(320);
        InquiryRequest.Create request = request(email320);

        // when
        Set<ConstraintViolation<InquiryRequest.Create>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    void 이메일이_321자면_길이_제약을_위반한다() {
        // given
        // local part를 65자로 늘려 형식(@Email)과 길이(@Size) 제약을 동시에 321자로 위반시킨다.
        String email321 = "a".repeat(MAX_LOCAL_PART_LENGTH + 1) + "@" + DOMAIN_255;
        assertThat(email321).hasSize(321);
        InquiryRequest.Create request = request(email321);

        // when
        Set<ConstraintViolation<InquiryRequest.Create>> violations = validator.validate(request);

        // then
        assertThat(violations)
                .extracting(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage
                )
                .contains(tuple("contactEmail", "답변받을 이메일은 320자 이하로 입력해주세요."));
    }

    private InquiryRequest.Create request(String contactEmail) {
        return new InquiryRequest.Create(
                InquiryCategory.OTHER,
                "제목",
                "내용",
                contactEmail
        );
    }
}
