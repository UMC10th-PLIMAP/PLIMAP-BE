package com.example.plimap.domain.inquiry.repository;

import com.example.plimap.domain.inquiry.entity.Inquiry;
import com.example.plimap.domain.inquiry.enums.InquiryCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    // createdAt만으로는 동일 시각 문의 사이의 순서가 보장되지 않아 페이지 경계에서 중복·누락이 생길 수 있으므로
    // id를 보조 정렬 키로 둔다.
    Page<Inquiry> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    Page<Inquiry> findByCategoryOrderByCreatedAtDescIdDesc(InquiryCategory category, Pageable pageable);
}
