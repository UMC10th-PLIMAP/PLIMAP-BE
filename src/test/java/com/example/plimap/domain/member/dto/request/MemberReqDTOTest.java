package com.example.plimap.domain.member.dto.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MemberReqDTOTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void 이름이_빈_문자열이면_검증을_통과한다() {
        // given
        MemberReqDTO.UpdateProfile request = updateProfile("");

        // when
        Set<ConstraintViolation<MemberReqDTO.UpdateProfile>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    void 이름이_null이면_검증을_통과한다() {
        // given
        MemberReqDTO.UpdateProfile request = updateProfile(null);

        // when
        Set<ConstraintViolation<MemberReqDTO.UpdateProfile>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    void 이름이_2에서_7자_사이면_검증을_통과한다() {
        // given
        MemberReqDTO.UpdateProfile request = updateProfile("이예림");

        // when
        Set<ConstraintViolation<MemberReqDTO.UpdateProfile>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    void 이름이_한_글자면_검증을_위반한다() {
        // given
        MemberReqDTO.UpdateProfile request = updateProfile("이");

        // when
        Set<ConstraintViolation<MemberReqDTO.UpdateProfile>> violations = validator.validate(request);

        // then
        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("name");
    }

    @Test
    void 이름이_여덟_글자_이상이면_검증을_위반한다() {
        // given
        MemberReqDTO.UpdateProfile request = updateProfile("여덟글자이름입니다");

        // when
        Set<ConstraintViolation<MemberReqDTO.UpdateProfile>> violations = validator.validate(request);

        // then
        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("name");
    }

    @Test
    void 이름에_특수문자가_있으면_검증을_위반한다() {
        // given
        MemberReqDTO.UpdateProfile request = updateProfile("이예림!");

        // when
        Set<ConstraintViolation<MemberReqDTO.UpdateProfile>> violations = validator.validate(request);

        // then
        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("name");
    }

    private MemberReqDTO.UpdateProfile updateProfile(String name) {
        return new MemberReqDTO.UpdateProfile(null, name, null);
    }
}
