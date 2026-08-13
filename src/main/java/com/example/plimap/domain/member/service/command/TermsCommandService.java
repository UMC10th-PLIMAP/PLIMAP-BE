package com.example.plimap.domain.member.service.command;

import com.example.plimap.domain.member.dto.request.TermsReqDTO;
import com.example.plimap.domain.member.dto.response.TermsResponse;
import java.util.List;

public interface TermsCommandService {

    List<TermsResponse.Result> agreeToTerms(Long memberId, TermsReqDTO.Agree request);
}
