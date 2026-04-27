package com.driveme.backend.service;

import com.driveme.backend.dto.ReportIssueRequest;
import com.driveme.backend.dto.ReportedIssueDTO;
import com.driveme.backend.entity.ReportedIssue;
import com.driveme.backend.repository.ReportedIssueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportedIssueService {

    private final ReportedIssueRepository repository;

    /**
     * Saves a new reported issue from a passenger or driver.
     */
    public ReportedIssueDTO submit(UUID reporterId, String reporterName,
                                   String reporterRole, ReportIssueRequest request) {
        ReportedIssue issue = new ReportedIssue();
        issue.setReporterId(reporterId);
        issue.setReporterName(reporterName);
        issue.setReporterRole(reporterRole);
        issue.setTripId(request.getTripId());
        issue.setDescription(request.getDescription());
        issue.setResolved(false);

        ReportedIssue saved = repository.save(issue);
        return toDTO(saved);
    }

    /**
     * Returns all reported issues ordered by newest first.
     */
    public List<ReportedIssueDTO> findAll() {
        return repository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Marks an issue as resolved.
     */
    public ReportedIssueDTO resolve(UUID id) {
        ReportedIssue issue = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Issue not found: " + id));
        issue.setResolved(true);
        return toDTO(repository.save(issue));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private ReportedIssueDTO toDTO(ReportedIssue issue) {
        ReportedIssueDTO dto = new ReportedIssueDTO();
        dto.setId(issue.getId());
        dto.setReporterId(issue.getReporterId());
        dto.setReporterName(issue.getReporterName());
        dto.setReporterRole(issue.getReporterRole());
        dto.setTripId(issue.getTripId());
        dto.setDescription(issue.getDescription());
        dto.setResolved(issue.isResolved());
        dto.setCreatedAt(issue.getCreatedAt());
        return dto;
    }
}
