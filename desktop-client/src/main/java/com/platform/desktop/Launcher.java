package com.platform.desktop;

/**
 * Non-JavaFX entry point so the shaded fat JAR can be launched with
 * {@code java -jar desktop-client.jar} without the Java Platform Module System
 * refusing to load {@code javafx.graphics} from the unnamed module.
 *
 * <p>The main class in the manifest MUST NOT extend {@link javafx.application.Application}
 * — this indirection is the standard workaround.
 */
public final class Launcher {

    private Launcher() {}

    public static void main(String[] args) {
        DesktopApp.main(args);
    }
}
