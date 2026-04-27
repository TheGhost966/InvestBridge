package com.platform.desktop.util;

import javafx.concurrent.Task;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Boilerplate-killer for background API calls from FXML controllers.
 *
 * <p>Usage:
 * <pre>{@code
 * AsyncUi.run(
 *     () -> Services.authApi().login(email, pw),
 *     resp -> Router.toDashboardFor(resp.roleEnum()),
 *     err  -> errorLabel.setText(UiUtil.friendly(err))
 * );
 * }</pre>
 *
 * <p>The work runs on a fresh daemon thread; success and failure handlers fire
 * on the JavaFX Application Thread, so they can safely touch the scene graph.
 */
public final class AsyncUi {

    private AsyncUi() {}

    public static <T> void run(Callable<T> work,
                               Consumer<T> onSuccess,
                               Consumer<Throwable> onFailure) {
        Task<T> task = new Task<>() {
            @Override protected T call() throws Exception { return work.call(); }
        };
        task.setOnSucceeded(ev -> onSuccess.accept(task.getValue()));
        task.setOnFailed(ev    -> onFailure.accept(task.getException()));
        Thread t = new Thread(task, "async-api");
        t.setDaemon(true);
        t.start();
    }
}
