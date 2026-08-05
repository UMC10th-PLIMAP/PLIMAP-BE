package com.example.plimap.domain.report.repository;

import com.example.plimap.domain.report.entity.Report;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporter_IdAndReportedMember_Id(Long reporterId, Long reportedMemberId);

    boolean existsByReporter_IdAndReportedPin_Id(Long reporterId, Long reportedPinId);

    @Modifying
    @Query("delete from Report r where r.reportedPin.id in :pinIds")
    void deleteAllByReportedPinIdIn(List<Long> pinIds);

    @Modifying
    @Query("delete from Report r where r.reportedMember.id = :memberId")
    void deleteAllByReportedMemberId(Long memberId);

    @Modifying
    @Query("delete from Report r where r.reporter.id = :memberId")
    void deleteAllByReporterId(Long memberId);
}
