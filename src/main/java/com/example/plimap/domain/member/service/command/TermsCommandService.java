package com.example.plimap.domain.member.service.command;

import com.example.plimap.domain.member.dto.request.TermsReqDTO;
import com.example.plimap.domain.member.entity.MemberTermsAgreement;
import java.util.List;

public interface TermsCommandService {

    List<MemberTermsAgreement> agreeToTerms(Long memberId, TermsReqDTO.Agree request);
}
