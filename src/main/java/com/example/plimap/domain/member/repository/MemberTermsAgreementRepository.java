package com.example.plimap.domain.member.repository;

import com.example.plimap.domain.member.entity.MemberTermsAgreement;
import com.example.plimap.domain.member.entity.MemberTermsAgreementId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberTermsAgreementRepository extends JpaRepository<MemberTermsAgreement, MemberTermsAgreementId> {

    Optional<MemberTermsAgreement> findByMember_IdAndTerms_Id(Long memberId, Long termsId);

    List<MemberTermsAgreement> findAllByMember_Id(Long memberId);
}
