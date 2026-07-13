package com.example.plimap.domain.member.repository;

import com.example.plimap.domain.member.entity.Terms;
import com.example.plimap.domain.member.enums.TermsType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermsRepository extends JpaRepository<Terms, Long> {

    List<Terms> findAllByActiveTrueOrderByTypeAsc();

    Optional<Terms> findFirstByTypeAndActiveTrueOrderByEffectiveAtDesc(TermsType type);
}
