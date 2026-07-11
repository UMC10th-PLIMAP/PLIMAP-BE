package com.example.plimap.domain.auth.repository;

import com.example.plimap.domain.auth.entity.SocialAccount;
import com.example.plimap.domain.auth.enums.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    Optional<SocialAccount> findByProviderAndProviderSubject(AuthProvider provider, String providerSubject);
}
