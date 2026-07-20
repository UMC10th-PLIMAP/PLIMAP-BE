package com.example.plimap.domain.member.repository;

import com.example.plimap.domain.member.entity.Member;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByNicknameIgnoreCaseAndDeletedAtIsNull(String nickname);

    Optional<Member> findByIdAndDeletedAtIsNull(Long id);
}
