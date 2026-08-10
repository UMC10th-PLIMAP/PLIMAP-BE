package com.example.plimap.domain.inquiry.repository;

import com.example.plimap.domain.inquiry.entity.Inquiry;
import com.example.plimap.domain.inquiry.enums.InquiryCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    // createdAt만으로는 동일 시각 문의 사이의 순서가 보장되지 않아 페이지 경계에서 중복·누락이 생길 수 있으므로
    // id를 보조 정렬 키로 둔다. member는 목록 조회 시 항상 함께 쓰이므로 EntityGraph로 한 번에 가져와 N+1을 막는다.
    @EntityGraph(attributePaths = "member")
    Page<Inquiry> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    @EntityGraph(attributePaths = "member")
    Page<Inquiry> findByCategoryOrderByCreatedAtDescIdDesc(InquiryCategory category, Pageable pageable);
}
