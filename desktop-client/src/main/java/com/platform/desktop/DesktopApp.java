package com.platform.desktop;

import com.platform.desktop.view.Router;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * JavaFX entry point for the InvestBridge desktop client.
 *
 * <p>Initialises the {@link Router} with the primary Stage and navigates to the
 * login view. All subsequent scene changes (register, role-aware dashboards)
 * flow through the Router so the rest of the app never touches the Stage.
 */
public class DesktopApp extends Application {

    public static final String APP_TITLE = "InvestBridge";

    @Override
    public void start(Stage stage) {
        stage.setTitle(APP_TITLE);
        stage.setMinWidth(960);
        stage.setMinHeight(640);

        Router.init(stage);
        Router.toLogin();

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
