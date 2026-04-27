package com.platform.desktop.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.platform.desktop.api.dto.ApiResponse;
import com.platform.desktop.api.dto.CreateIdeaRequest;
import com.platform.desktop.api.dto.Idea;
import com.platform.desktop.api.dto.PagedResult;

import java.util.List;
import java.util.Map;

/**
 * Idea endpoints ({@code /ideas/**}). The backend enforces role-based
 * filtering off the {@code X-User-Role} header injected by the dispatcher,
 * so the same {@link #list(int, int)} returns:
 * <ul>
 *   <li>own ideas only — when called by a FOUNDER</li>
 *   <li>VERIFIED ideas only — when called by an INVESTOR</li>
 *   <li>everything — when called by an ADMIN</li>
 * </ul>
 */
public class IdeaApi {

    private final ApiClient client;

    public IdeaApi(ApiClient client) {
        this.client = client;
    }

    /** Calls {@code GET /ideas/paged} which uses the generic {@code ApiResponse<PagedResult<Idea>>} envelope. */
    public PagedResult<Idea> list(int page, int size) {
        ApiResponse<PagedResult<Idea>> resp = client.get(
                "/ideas/paged?page=" + page + "&size=" + size,
                new TypeReference<ApiResponse<PagedResult<Idea>>>() {}
        );
        if (resp == null || resp.data == null) {
            PagedResult<Idea> empty = new PagedResult<>();
            empty.items = List.of();
            empty.size = size;
            return empty;
        }
        return resp.data;
    }

    public Idea get(String id) {
        return client.get("/ideas/" + id, Idea.class);
    }

    public Idea create(CreateIdeaRequest req) {
        return client.post("/ideas", req, Idea.class);
    }

    public Idea update(String id, CreateIdeaRequest req) {
        return client.put("/ideas/" + id, req, Idea.class);
    }

    public void delete(String id) {
        client.delete("/ideas/" + id);
    }

    public Idea verify(String id) {
        return client.patch("/ideas/" + id + "/verify", null, Idea.class);
    }

    public Idea reject(String id, String reason) {
        return client.patch("/ideas/" + id + "/reject",
                Map.of("reason", reason), Idea.class);
    }
}
