package com.platform.deal.repository;

import com.platform.deal.domain.Offer;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface OfferRepository extends MongoRepository<Offer, String> {
    List<Offer> findByInvestorId(String investorId);
    List<Offer> findByFounderId(String founderId);
}
