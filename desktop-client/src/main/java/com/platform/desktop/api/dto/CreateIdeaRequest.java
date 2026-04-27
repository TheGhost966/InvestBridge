package com.platform.desktop.api.dto;

import java.util.List;

/**
 * Body for {@code POST /ideas} and {@code PUT /ideas/{id}}. The backend
 * validates {@code title} / {@code summary} non-blank and {@code fundingNeeded}
 * positive — the client form does the same checks before sending.
 */
public class CreateIdeaRequest {

    public String title;
    public String summary;
    public String market;
    public String traction;
    public Double fundingNeeded;
    public String location;
    public List<String> tags;

    public CreateIdeaRequest() {}
}
