package com.platform.desktop.view.dialog;

import com.platform.desktop.api.dto.CreateOfferRequest;
import com.platform.desktop.api.dto.Idea;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.util.Optional;

/**
 * "Make an offer" dialog shown from the investor browse view. Pre-fills the
 * idea / founder context (read-only) and lets the investor choose an amount
 * and an optional pitch message.
 */
public final class MakeOfferDialog {

    private MakeOfferDialog() {}

    public static Optional<CreateOfferRequest> showFor(Idea idea) {
        Dialog<CreateOfferRequest> dialog = new Dialog<>();
        dialog.setTitle("Make an offer");
        dialog.setHeaderText("Investing in: " + (idea.title == null ? "(untitled)" : idea.title));

        ButtonType okType = new ButtonType("Send offer", ButtonType.OK.getButtonData());
        dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        TextField amountField = new TextField();
        amountField.setPromptText("Amount in EUR (e.g. " + suggestedAmount(idea) + ")");

        TextArea messageArea = new TextArea();
        messageArea.setPromptText("Optional message to the founder — why you're investing, terms…");
        messageArea.setWrapText(true);
        messageArea.setPrefRowCount(4);

        Label fundingHint = new Label("Funding goal: " + formatMoney(idea.fundingNeededOrZero()));
        fundingHint.getStyleClass().add("muted-label");

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");
        errorLabel.setWrapText(true);
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(20, 24, 8, 24));
        int row = 0;
        grid.add(new Label("Amount €*"), 0, row); grid.add(amountField, 1, row++);
        grid.add(fundingHint,            1, row++);
        grid.add(new Label("Message"),   0, row); grid.add(messageArea, 1, row++);
        grid.add(errorLabel,             1, row);

        GridPane.setHgrow(amountField, Priority.ALWAYS);
        GridPane.setHgrow(messageArea, Priority.ALWAYS);
        amountField.setMaxWidth(Double.MAX_VALUE);
        messageArea.setMaxWidth(Double.MAX_VALUE);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(500);

        // Validate inline before allowing OK to close the dialog
        dialog.getDialogPane().lookupButton(okType).addEventFilter(
                javafx.event.ActionEvent.ACTION, ev -> {
                    String error = validate(amountField.getText());
                    if (error != null) {
                        errorLabel.setText(error);
                        errorLabel.setManaged(true);
                        errorLabel.setVisible(true);
                        ev.consume();
                    }
                });

        dialog.setResultConverter(button -> {
            if (button != okType) return null;
            String msg = messageArea.getText();
            return new CreateOfferRequest(
                    idea.id,
                    idea.founderId,
                    Double.parseDouble(amountField.getText().trim()),
                    msg == null || msg.isBlank() ? null : msg.trim()
            );
        });

        return dialog.showAndWait();
    }

    private static String validate(String amount) {
        if (amount == null || amount.isBlank()) return "Amount is required.";
        try {
            double v = Double.parseDouble(amount.trim());
            if (v <= 0) return "Amount must be positive.";
        } catch (NumberFormatException nfe) {
            return "Amount must be a number (e.g. 25000).";
        }
        return null;
    }

    private static long suggestedAmount(Idea idea) {
        double goal = idea.fundingNeededOrZero();
        if (goal <= 0) return 10_000;
        // Default suggestion = 10% of the funding goal, rounded to a nice number.
        long suggestion = Math.round(goal / 10.0);
        return Math.max(1_000, suggestion);
    }

    private static String formatMoney(double v) {
        if (v >= 1_000_000) return String.format("€%.1fM", v / 1_000_000.0);
        if (v >= 1_000)     return String.format("€%,d", (long) v);
        return String.format("€%.0f", v);
    }
}
