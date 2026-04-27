package com.platform.desktop.view.dialog;

import com.platform.desktop.api.dto.CreateIdeaRequest;
import com.platform.desktop.api.dto.Idea;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Dialog for creating a new idea or editing an existing one.
 *
 * <p>Returns an {@link Optional} of {@link CreateIdeaRequest} — empty on cancel
 * or validation failure (the dialog displays an inline error and stays open).
 * Built programmatically rather than via FXML because dialogs are short-lived
 * one-shots and the FXML overhead doesn't pay for itself.
 */
public final class IdeaFormDialog {

    private IdeaFormDialog() {}

    public static Optional<CreateIdeaRequest> showCreate() {
        return show("Create idea", "Submit", null);
    }

    public static Optional<CreateIdeaRequest> showEdit(Idea existing) {
        return show("Edit idea", "Save changes", existing);
    }

    private static Optional<CreateIdeaRequest> show(String title,
                                                    String okText,
                                                    Idea existing) {
        Dialog<CreateIdeaRequest> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);

        ButtonType okType = new ButtonType(okText, ButtonType.OK.getButtonData());
        dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        // ── form -------------------------------------------------------------
        TextField titleField = new TextField();
        titleField.setPromptText("e.g. GreenEnergy AI");

        TextArea summaryArea = new TextArea();
        summaryArea.setPromptText("Brief description of the idea");
        summaryArea.setWrapText(true);
        summaryArea.setPrefRowCount(3);

        TextField marketField = new TextField();
        marketField.setPromptText("Target market (e.g. EU SMB)");

        TextArea tractionArea = new TextArea();
        tractionArea.setPromptText("Traction so far (users, MRR, pilots…)");
        tractionArea.setWrapText(true);
        tractionArea.setPrefRowCount(2);

        TextField fundingField = new TextField();
        fundingField.setPromptText("Amount in EUR, e.g. 50000");

        TextField locationField = new TextField();
        locationField.setPromptText("e.g. Istanbul, TR");

        TextField tagsField = new TextField();
        tagsField.setPromptText("Comma-separated, e.g. ai, climate, b2b");

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");
        errorLabel.setWrapText(true);
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);

        // Pre-fill on edit
        if (existing != null) {
            titleField.setText(orEmpty(existing.title));
            summaryArea.setText(orEmpty(existing.summary));
            marketField.setText(orEmpty(existing.market));
            tractionArea.setText(orEmpty(existing.traction));
            fundingField.setText(existing.fundingNeeded == null
                    ? "" : String.valueOf(existing.fundingNeeded.longValue()));
            locationField.setText(orEmpty(existing.location));
            tagsField.setText(existing.tags == null ? "" : String.join(", ", existing.tags));
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(20, 24, 8, 24));
        int row = 0;
        grid.add(new Label("Title*"),    0, row); grid.add(titleField,   1, row++);
        grid.add(new Label("Summary*"),  0, row); grid.add(summaryArea,  1, row++);
        grid.add(new Label("Market"),    0, row); grid.add(marketField,  1, row++);
        grid.add(new Label("Traction"),  0, row); grid.add(tractionArea, 1, row++);
        grid.add(new Label("Funding €*"),0, row); grid.add(fundingField, 1, row++);
        grid.add(new Label("Location"),  0, row); grid.add(locationField,1, row++);
        grid.add(new Label("Tags"),      0, row); grid.add(tagsField,    1, row++);
        grid.add(errorLabel, 1, row);

        // Make the right column stretch
        for (var node : List.of(titleField, summaryArea, marketField,
                                tractionArea, fundingField, locationField, tagsField)) {
            GridPane.setHgrow(node, javafx.scene.layout.Priority.ALWAYS);
            ((javafx.scene.control.Control) node).setMaxWidth(Double.MAX_VALUE);
        }

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(540);

        // Intercept OK so we can validate and stay open on failure
        dialog.getDialogPane().lookupButton(okType).addEventFilter(
                javafx.event.ActionEvent.ACTION, ev -> {
                    String error = validate(titleField.getText(),
                                            summaryArea.getText(),
                                            fundingField.getText());
                    if (error != null) {
                        errorLabel.setText(error);
                        errorLabel.setManaged(true);
                        errorLabel.setVisible(true);
                        ev.consume();
                    }
                });

        dialog.setResultConverter(button -> {
            if (button != okType) return null;
            CreateIdeaRequest req = new CreateIdeaRequest();
            req.title         = titleField.getText().trim();
            req.summary       = summaryArea.getText().trim();
            req.market        = blankToNull(marketField.getText());
            req.traction      = blankToNull(tractionArea.getText());
            req.fundingNeeded = Double.parseDouble(fundingField.getText().trim());
            req.location      = blankToNull(locationField.getText());
            req.tags          = parseTags(tagsField.getText());
            return req;
        });

        return dialog.showAndWait();
    }

    /** @return error message or {@code null} when input is valid. */
    private static String validate(String title, String summary, String funding) {
        if (title == null || title.isBlank())     return "Title is required.";
        if (summary == null || summary.isBlank()) return "Summary is required.";
        if (funding == null || funding.isBlank()) return "Funding amount is required.";
        try {
            double v = Double.parseDouble(funding.trim());
            if (v <= 0) return "Funding amount must be positive.";
        } catch (NumberFormatException nfe) {
            return "Funding amount must be a number (e.g. 50000).";
        }
        return null;
    }

    private static String orEmpty(String s)    { return s == null ? "" : s; }
    private static String blankToNull(String s){ return s == null || s.isBlank() ? null : s.trim(); }

    private static List<String> parseTags(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
