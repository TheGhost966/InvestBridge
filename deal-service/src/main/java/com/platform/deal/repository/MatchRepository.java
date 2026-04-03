package com.platform.deal.repository;

import com.platform.deal.domain.Match;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MatchRepository extends MongoRepository<Match, String> {
    List<Match> findByInvestorId(String investorId);
    List<Match> findByFounderId(String founderId);
}
