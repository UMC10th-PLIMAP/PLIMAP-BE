package com.example.plimap.domain.member.service.query;

import com.example.plimap.domain.member.entity.Terms;
import com.example.plimap.domain.member.enums.TermsType;
import java.util.List;

public interface TermsQueryService {

    List<Terms> getActiveTerms();

    Terms getActiveTermsByType(TermsType type);
}
