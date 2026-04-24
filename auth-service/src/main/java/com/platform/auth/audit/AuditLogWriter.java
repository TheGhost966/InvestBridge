package com.platform.auth.audit;

import java.util.List;

/**
 * Storage-agnostic contract for writing and querying audit events.
 *
 * <p>Introducing this interface lets {@link com.platform.auth.service.AuthService}
 * depend on a behavior rather than a specific database driver — a direct
 * application of the Dependency Inversion Principle. The current
 * implementation is {@link JdbcAuditLogWriter} (PostgreSQL), but swapping
 * to Mongo, Kafka or an in-memory test double requires zero changes in
 * the service layer.</p>
 */
public interface AuditLogWriter {

    /** Persist the event and return it with a server-assigned {@code id}. */
    AuditEvent log(AuditEvent event);

    /** Most-recent events for a user, newest first. */
    List<AuditEvent> findRecent(String userId, int limit);

    /** Total audit rows currently stored. */
    long count();
}
