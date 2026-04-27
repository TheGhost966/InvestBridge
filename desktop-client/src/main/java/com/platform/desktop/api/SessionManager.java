package com.platform.desktop.api;

import com.platform.desktop.api.dto.AuthResponse;
import com.platform.desktop.api.dto.Role;

/**
 * In-memory holder for the authenticated user's JWT, userId and role.
 *
 * <p>Read/write access is safe across FX and background threads: the single
 * field {@link #current} is {@code volatile} and written only once per login.
 * Logout clears it back to {@code null}.
 */
public final class SessionManager {

    public record Session(String token, String userId, Role role) {}

    private static volatile Session current;

    private SessionManager() {}

    public static void set(AuthResponse response) {
        current = new Session(response.token, response.userId, response.roleEnum());
    }

    public static void clear() {
        current = null;
    }

    public static boolean isAuthenticated() {
        return current != null;
    }

    public static String token()   { return current == null ? null : current.token();  }
    public static String userId()  { return current == null ? null : current.userId(); }
    public static Role role()      { return current == null ? null : current.role();   }
    public static Session current() { return current; }
}
