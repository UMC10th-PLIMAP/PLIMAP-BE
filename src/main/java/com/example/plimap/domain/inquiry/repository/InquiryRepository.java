package com.example.plimap.domain.inquiry.repository;

import com.example.plimap.domain.inquiry.entity.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
}
