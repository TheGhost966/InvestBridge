package com.platform.desktop.view.dialog;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Optional;

/**
 * Dialog used by an admin to reject an idea — the backend requires a non-blank
 * reason, which the founder will see in their REJECTED card so they can revise
 * and resubmit.
 *
 * <p>Returns {@link Optional#empty()} on cancel; otherwise the trimmed,
 * non-blank reason string.
 */
public final class RejectReasonDialog {

    private RejectReasonDialog() {}

    public static Optional<String> showFor(String ideaTitle) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Reject idea");
        dialog.setHeaderText("Rejecting: " + (ideaTitle == null ? "(untitled)" : ideaTitle));

        ButtonType okType = new ButtonType("Reject with reason", ButtonType.OK.getButtonData());
        dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        TextArea reasonArea = new TextArea();
        reasonArea.setPromptText("Explain what's missing (market analysis, traction, financials, …) "
                + "so the founder can revise and resubmit.");
        reasonArea.setWrapText(true);
        reasonArea.setPrefRowCount(5);
        VBox.setVgrow(reasonArea, Priority.ALWAYS);
        reasonArea.setMaxWidth(Double.MAX_VALUE);

        Label error = new Label();
        error.getStyleClass().add("error-label");
        error.setWrapText(true);
        error.setManaged(false);
        error.setVisible(false);

        VBox box = new VBox(8,
                new Label("Reason*"),
                reasonArea,
                error
        );
        box.setPadding(new Insets(20, 24, 8, 24));
        box.setFillWidth(true);

        dialog.getDialogPane().setContent(box);
        dialog.getDialogPane().setPrefWidth(520);

        dialog.getDialogPane().lookupButton(okType).addEventFilter(
                javafx.event.ActionEvent.ACTION, ev -> {
                    String reason = reasonArea.getText() == null ? "" : reasonArea.getText().trim();
                    if (reason.isEmpty()) {
                        error.setText("Reason is required.");
                        error.setManaged(true);
                        error.setVisible(true);
                        ev.consume();
                    }
                });

        dialog.setResultConverter(button -> {
            if (button != okType) return null;
            return reasonArea.getText().trim();
        });

        return dialog.showAndWait();
    }
}
