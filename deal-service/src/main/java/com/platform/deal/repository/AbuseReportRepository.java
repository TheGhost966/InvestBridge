package com.platform.deal.repository;

import com.platform.deal.domain.AbuseReport;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AbuseReportRepository extends MongoRepository<AbuseReport, String> {
}
