package com.kosmoskan.analysis.repository;

import com.kosmoskan.analysis.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
     Optional<Report> findByWorkId(Long workId);

    boolean existsByWorkId(Long workId);
}
