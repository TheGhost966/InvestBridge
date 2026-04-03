package com.platform.idea.repository;

import com.platform.idea.domain.Idea;
import com.platform.idea.domain.IdeaStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface IdeaRepository extends MongoRepository<Idea, String> {

    Page<Idea> findByStatus(IdeaStatus status, Pageable pageable);

    Page<Idea> findByFounderId(String founderId, Pageable pageable);

    List<Idea> findByFounderIdAndStatus(String founderId, IdeaStatus status);
}
