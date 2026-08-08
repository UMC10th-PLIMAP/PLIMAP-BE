package com.example.plimap.domain.auth.service.query;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AuthQueryService {

    Optional<String> findEmailByMemberId(Long memberId);

    Map<Long, String> findEmailsByMemberIds(List<Long> memberIds);
}
