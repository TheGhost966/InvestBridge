package com.platform.desktop.api;

import com.platform.desktop.api.dto.AuthResponse;
import com.platform.desktop.api.dto.LoginRequest;
import com.platform.desktop.api.dto.RegisterRequest;
import com.platform.desktop.api.dto.Role;
import com.platform.desktop.api.dto.UserProfileResponse;

/**
 * Auth endpoints ({@code /auth/**}) exposed through the dispatcher.
 *
 * <p>Kept deliberately stateless: each method just composes an {@link ApiClient}
 * call. Session state lives in {@link SessionManager}. A successful
 * {@link #login} or {@link #register} writes to the session; {@link #logout}
 * clears it even if the server-side blacklist call fails.
 */
public class AuthApi {

    private final ApiClient client;

    public AuthApi(ApiClient client) {
        this.client = client;
    }

    public AuthResponse register(String email, String password, Role role) {
        AuthResponse resp = client.post("/auth/register",
                new RegisterRequest(email, password, role), AuthResponse.class);
        SessionManager.set(resp);
        return resp;
    }

    public AuthResponse login(String email, String password) {
        AuthResponse resp = client.post("/auth/login",
                new LoginRequest(email, password), AuthResponse.class);
        SessionManager.set(resp);
        return resp;
    }

    /**
     * Best-effort server logout — even if the request fails (e.g. token already
     * expired, dispatcher down) we still clear the local session so the user
     * can re-authenticate.
     */
    public void logout() {
        try {
            client.post("/auth/logout", null, Void.class);
        } catch (ApiException ignored) {
            // fall through — local logout is unconditional
        } finally {
            SessionManager.clear();
        }
    }

    public UserProfileResponse me() {
        return client.get("/auth/me", UserProfileResponse.class);
    }
}
