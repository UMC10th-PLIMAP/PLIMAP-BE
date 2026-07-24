package com.example.plimap.global.external.storage;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ProfileImageObjectKeyGenerator {

    public String generate(Long memberId) {
        if (memberId == null || memberId <= 0) {
            throw new IllegalArgumentException("memberId must be positive");
        }

        return "members/%d/%s.webp".formatted(memberId, UUID.randomUUID());
    }
}
