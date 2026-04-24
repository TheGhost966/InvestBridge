package com.platform.auth.audit;

/**
 * Unchecked exception thrown when the JDBC audit layer cannot persist
 * or retrieve an event. Propagated up to the global exception handler
 * which maps it to HTTP 500.
 */
public class AuditPersistenceException extends RuntimeException {
    public AuditPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
