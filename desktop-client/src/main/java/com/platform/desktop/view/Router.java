package com.platform.desktop.view;

import com.platform.desktop.api.dto.Role;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

/**
 * Central scene-switching utility. Controllers call {@link #toLogin()},
 * {@link #toRegister()}, or {@link #toDashboardFor(Role)} and never touch the
 * {@link Stage} directly — keeping the Stage singleton as an implementation
 * detail of this class.
 */
public final class Router {

    private static final String CSS_PATH = "/css/app.css";

    private static Stage stage;

    private Router() {}

    public static void init(Stage primaryStage) {
        Router.stage = Objects.requireNonNull(primaryStage, "primaryStage");
    }

    public static void toLogin() {
        swap("/fxml/login.fxml");
    }

    public static void toRegister() {
        swap("/fxml/register.fxml");
    }

    /** Role-aware dispatch after successful login. */
    public static void toDashboardFor(Role role) {
        switch (role) {
            case FOUNDER  -> swap("/fxml/founder-dashboard.fxml");
            case INVESTOR -> swap("/fxml/investor-dashboard.fxml");
            case ADMIN    -> swap("/fxml/admin-dashboard.fxml");
        }
    }

    private static void swap(String fxmlPath) {
        requireStage();
        try {
            URL url = Router.class.getResource(fxmlPath);
            if (url == null) {
                throw new IllegalStateException("FXML resource not found on classpath: " + fxmlPath);
            }
            Parent root = FXMLLoader.load(url);
            Scene scene = new Scene(root);
            URL css = Router.class.getResource(CSS_PATH);
            if (css != null) {
                scene.getStylesheets().add(css.toExternalForm());
            }
            stage.setScene(scene);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + fxmlPath, e);
        }
    }

    private static void requireStage() {
        if (stage == null) {
            throw new IllegalStateException("Router.init(Stage) must be called before navigation");
        }
    }
}
