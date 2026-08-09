package com.example.plimap.domain.inquiry.repository;

import com.example.plimap.domain.inquiry.entity.Inquiry;
import com.example.plimap.domain.inquiry.enums.InquiryCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    Page<Inquiry> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Inquiry> findByCategoryOrderByCreatedAtDesc(InquiryCategory category, Pageable pageable);
}
