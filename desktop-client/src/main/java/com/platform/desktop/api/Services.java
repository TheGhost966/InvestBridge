package com.platform.desktop.api;

/**
 * Tiny process-wide service locator so FXML controllers (which must have no-arg
 * constructors) can reach the shared {@link ApiClient} and endpoint wrappers.
 *
 * <p>A single {@link ApiClient} is reused across the app — the underlying
 * {@link java.net.http.HttpClient} maintains its own connection pool and is
 * safe for concurrent use, so there is no reason to create more than one.
 */
public final class Services {

    private static final ApiClient CLIENT = new ApiClient();
    private static final AuthApi   AUTH   = new AuthApi(CLIENT);

    private Services() {}

    public static ApiClient apiClient() { return CLIENT; }
    public static AuthApi   authApi()   { return AUTH; }
}
