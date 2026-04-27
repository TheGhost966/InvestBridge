package com.platform.desktop.view.controller;

import com.platform.desktop.api.Services;
import com.platform.desktop.util.AsyncUi;
import com.platform.desktop.util.UiUtil;
import com.platform.desktop.view.Router;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Controller for {@code login.fxml}.
 *
 * <p>On a successful login, {@link com.platform.desktop.api.AuthApi#login}
 * has already pushed the JWT + role into {@link com.platform.desktop.api.SessionManager},
 * so this controller only needs to navigate to the role-appropriate dashboard.
 */
public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button signInButton;
    @FXML private Label statusLabel;

    @FXML
    private void onSignIn() {
        hideStatus();

        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Email and password are required.");
            return;
        }

        setBusy(true);
        AsyncUi.run(
                () -> Services.authApi().login(email, password),
                resp -> {
                    setBusy(false);
                    Router.toDashboardFor(resp.roleEnum());
                },
                err -> {
                    setBusy(false);
                    showError(UiUtil.friendly(err));
                }
        );
    }

    @FXML
    private void onGoToRegister() {
        Router.toRegister();
    }

    private void setBusy(boolean busy) {
        signInButton.setDisable(busy);
        signInButton.setText(busy ? "Signing in…" : "Sign in");
        emailField.setDisable(busy);
        passwordField.setDisable(busy);
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setManaged(true);
        statusLabel.setVisible(true);
    }

    private void hideStatus() {
        statusLabel.setManaged(false);
        statusLabel.setVisible(false);
    }
}
