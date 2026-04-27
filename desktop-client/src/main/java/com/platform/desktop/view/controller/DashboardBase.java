package com.platform.desktop.view.controller;

import com.platform.desktop.api.Services;
import com.platform.desktop.api.SessionManager;
import com.platform.desktop.util.AsyncUi;
import com.platform.desktop.view.Router;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Common top-bar behaviour for every role dashboard: populate the identity
 * label from the current session and handle logout.
 *
 * <p>Subclasses (Founder/Investor/Admin controllers) wire the FXML
 * identity label through {@link #identityLabel} and the logout button via
 * {@link #onLogout()} — implemented here, so FXML {@code onAction="#onLogout"}
 * just works.
 */
public abstract class DashboardBase {

    @FXML protected Label identityLabel;

    @FXML
    protected void initialize() {
        if (identityLabel != null) {
            SessionManager.Session s = SessionManager.current();
            String who = s == null ? "—" : (s.userId() + "  ·  " + s.role());
            identityLabel.setText(who);
        }
        onReady();
    }

    /** Hook for subclasses to initialise role-specific state. Default: no-op. */
    protected void onReady() {}

    @FXML
    protected void onLogout() {
        AsyncUi.run(
                () -> { Services.authApi().logout(); return null; },
                ignored -> Router.toLogin(),
                err     -> Router.toLogin()  // local session already cleared; go home
        );
    }
}
