package com.example.plimap.domain.auth.service.command;

public interface TestTokenCommandService {

    String issueTestToken(Long memberId, String issueKey);
}
