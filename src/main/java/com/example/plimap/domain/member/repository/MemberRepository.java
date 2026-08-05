package com.example.plimap.domain.member.repository;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.enums.MemberStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByNicknameIgnoreCaseAndDeletedAtIsNull(String nickname);

    Optional<Member> findByIdAndDeletedAtIsNull(Long id);

    Optional<Member> findByIdAndStatusAndDeletedAtIsNull(Long id, MemberStatus status);

    // 벌점 부여의 읽기-갱신 과정을 직렬화한다: 잠금 없이 읽으면 동시에 들어온 두 요청이
    // 같은 penaltyPoint를 읽어 하나의 증가가 유실될 수 있다(4점 경계에서는 자동탈퇴 캐스케이드
    // 중복 시작으로 이어짐). 다른 트랜잭션이 먼저 갱신 중이면 커밋될 때까지 대기한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.id = :memberId and m.deletedAt is null")
    Optional<Member> findByIdAndDeletedAtIsNullForUpdate(@Param("memberId") Long memberId);

    @Modifying
    @Query("""
        update Member m
        set m.reportCount = m.reportCount + 1
        where m.id = :memberId
    """)
    void increaseReportCount(Long memberId);
}
