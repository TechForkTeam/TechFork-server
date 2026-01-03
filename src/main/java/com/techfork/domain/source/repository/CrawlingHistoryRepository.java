package com.techfork.domain.source.repository;

import com.techfork.domain.source.entity.CrawlingHistory;
import com.techfork.domain.source.enums.ECrawlingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CrawlingHistoryRepository extends JpaRepository<CrawlingHistory, Long> {

    List<CrawlingHistory> findByStatusAndStartedAtBefore(ECrawlingStatus status, LocalDateTime dateTime);

    Optional<CrawlingHistory> findByJobExecutionId(Long jobExecutionId);
}
