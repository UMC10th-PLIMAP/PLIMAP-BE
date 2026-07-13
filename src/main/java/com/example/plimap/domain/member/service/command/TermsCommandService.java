package com.example.plimap.domain.member.service.command;

import com.example.plimap.domain.member.dto.request.TermsReqDTO;
import com.example.plimap.domain.member.dto.response.TermsResDTO;
import java.util.List;

public interface TermsCommandService {

    List<TermsResDTO.Result> agreeToTerms(Long memberId, TermsReqDTO.Agree request);
}
