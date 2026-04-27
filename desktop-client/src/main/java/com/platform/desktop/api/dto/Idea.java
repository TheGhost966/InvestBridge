package com.platform.desktop.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;

/**
 * Mirror of the backend {@code com.platform.idea.domain.Idea} document.
 *
 * <p>{@link com.fasterxml.jackson.annotation.JsonIgnoreProperties} is defensive:
 * if the backend grows new fields (e.g. the rumoured {@code currentFunding}
 * roll-up), the client keeps deserialising successfully.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Idea {

    public String id;
    public String title;
    public String summary;
    public String market;
    public String traction;
    public Double fundingNeeded;
    public String location;
    public List<String> tags;
    public String founderId;
    public IdeaStatus status;
    public String rejectionReason;
    public Instant createdAt;

    public Idea() {}

    public double fundingNeededOrZero() {
        return fundingNeeded == null ? 0.0 : fundingNeeded;
    }
}
