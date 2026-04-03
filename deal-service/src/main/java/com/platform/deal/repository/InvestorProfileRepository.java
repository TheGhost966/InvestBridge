package com.platform.deal.repository;

import com.platform.deal.domain.InvestorProfile;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface InvestorProfileRepository extends MongoRepository<InvestorProfile, String> {
    Optional<InvestorProfile> findByInvestorId(String investorId);
}
