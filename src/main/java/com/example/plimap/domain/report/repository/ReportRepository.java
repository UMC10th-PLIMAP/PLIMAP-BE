package com.example.plimap.domain.report.repository;

import com.example.plimap.domain.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporter_IdAndReportedMember_Id(Long reporterId, Long reportedMemberId);

    boolean existsByReporter_IdAndReportedPin_Id(Long reporterId, Long reportedPinId);
}
