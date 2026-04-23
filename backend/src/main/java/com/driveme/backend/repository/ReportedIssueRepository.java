package com.driveme.backend.repository;

import com.driveme.backend.entity.ReportedIssue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReportedIssueRepository extends JpaRepository<ReportedIssue, UUID> {

    List<ReportedIssue> findAllByOrderByCreatedAtDesc();
}
