package com.platform.desktop.view.controller;

import com.platform.desktop.api.Services;
import com.platform.desktop.api.dto.Role;
import com.platform.desktop.util.AsyncUi;
import com.platform.desktop.util.UiUtil;
import com.platform.desktop.view.Router;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Controller for {@code register.fxml}. On success, the JWT is already in
 * the session (via {@link com.platform.desktop.api.AuthApi#register}) so we
 * jump straight to the role-appropriate dashboard — no "please log in now"
 * step.
 */
public class RegisterController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<Role> roleBox;
    @FXML private Button registerButton;
    @FXML private Label statusLabel;

    @FXML
    private void initialize() {
        roleBox.setItems(FXCollections.observableArrayList(Role.values()));
    }

    @FXML
    private void onRegister() {
        hideStatus();

        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();
        Role role = roleBox.getValue();

        if (email.isEmpty() || password.isEmpty() || role == null) {
            showError("Email, password and role are required.");
            return;
        }
        if (password.length() < 8) {
            showError("Password must be at least 8 characters.");
            return;
        }

        setBusy(true);
        AsyncUi.run(
                () -> Services.authApi().register(email, password, role),
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
    private void onGoToLogin() {
        Router.toLogin();
    }

    private void setBusy(boolean busy) {
        registerButton.setDisable(busy);
        registerButton.setText(busy ? "Creating account…" : "Create account");
        emailField.setDisable(busy);
        passwordField.setDisable(busy);
        roleBox.setDisable(busy);
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
