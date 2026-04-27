package com.platform.desktop.view.controller;

import com.platform.desktop.api.Services;
import com.platform.desktop.api.dto.CreateIdeaRequest;
import com.platform.desktop.api.dto.Idea;
import com.platform.desktop.api.dto.PagedResult;
import com.platform.desktop.util.AsyncUi;
import com.platform.desktop.util.UiUtil;
import com.platform.desktop.view.components.IdeaCard;
import com.platform.desktop.view.dialog.IdeaFormDialog;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;

/**
 * Founder dashboard — lists the founder's own ideas, lets them create new
 * ones, and edit/delete those still in DRAFT (or REJECTED, for revision).
 *
 * <p>Each idea is rendered as an {@link IdeaCard} which embeds the two
 * custom-graphics components built in this phase ({@code FundingRing}
 * and {@code StatusBadge}). Funding progress is shown as 0&nbsp;% until
 * Phase 1.3 wires the deal service so we can fetch accepted offers and
 * compute the rolled-up amount per idea.
 */
public class FounderDashboardController extends DashboardBase {

    @FXML private VBox  ideaList;          // parent of the IdeaCards
    @FXML private Label statusLabel;       // small text under the toolbar (loading / errors / counts)

    @Override
    protected void onReady() {
        loadIdeas();
    }

    @FXML
    private void onRefresh() {
        loadIdeas();
    }

    @FXML
    private void onNewIdea() {
        Optional<CreateIdeaRequest> req = IdeaFormDialog.showCreate();
        req.ifPresent(this::createIdea);
    }

    // ── data ops ──────────────────────────────────────────────────────────────

    private void loadIdeas() {
        showStatus("Loading ideas…");
        AsyncUi.run(
                () -> Services.ideaApi().list(0, 50),
                this::renderIdeas,
                err -> showStatus("Failed to load ideas — " + UiUtil.friendly(err))
        );
    }

    private void createIdea(CreateIdeaRequest req) {
        showStatus("Creating idea…");
        AsyncUi.run(
                () -> Services.ideaApi().create(req),
                idea -> { showStatus("Idea \"" + idea.title + "\" created."); loadIdeas(); },
                err  -> showStatus("Create failed — " + UiUtil.friendly(err))
        );
    }

    private void editIdea(Idea idea) {
        Optional<CreateIdeaRequest> req = IdeaFormDialog.showEdit(idea);
        req.ifPresent(r -> {
            showStatus("Saving changes…");
            AsyncUi.run(
                    () -> Services.ideaApi().update(idea.id, r),
                    saved -> { showStatus("Saved \"" + saved.title + "\"."); loadIdeas(); },
                    err   -> showStatus("Save failed — " + UiUtil.friendly(err))
            );
        });
    }

    private void deleteIdea(Idea idea) {
        Alert confirm = new Alert(AlertType.CONFIRMATION,
                "Delete \"" + idea.title + "\"? This cannot be undone.",
                ButtonType.CANCEL, ButtonType.OK);
        confirm.setHeaderText("Delete idea");
        Optional<ButtonType> choice = confirm.showAndWait();
        if (choice.isEmpty() || choice.get() != ButtonType.OK) return;

        showStatus("Deleting…");
        AsyncUi.run(
                () -> { Services.ideaApi().delete(idea.id); return null; },
                ignored -> { showStatus("Deleted."); loadIdeas(); },
                err     -> showStatus("Delete failed — " + UiUtil.friendly(err))
        );
    }

    // ── rendering ─────────────────────────────────────────────────────────────

    private void renderIdeas(PagedResult<Idea> page) {
        ideaList.getChildren().clear();
        List<Idea> items = page == null || page.items == null ? List.of() : page.items;

        if (items.isEmpty()) {
            ideaList.getChildren().add(emptyState());
            showStatus("No ideas yet — click ‘New idea’ to get started.");
            return;
        }

        for (Idea idea : items) {
            // Phase 1.2: no offers data yet, show 0 funding raised.
            // Phase 1.3 will sum accepted offers from deal-service.
            IdeaCard card = new IdeaCard(idea, 0.0, this::editIdea, this::deleteIdea);
            ideaList.getChildren().add(card);
        }
        long total = page.totalElements;
        showStatus("Showing " + items.size() + " of " + total + " idea" + (total == 1 ? "" : "s") + ".");
    }

    private Node emptyState() {
        VBox empty = new VBox(8);
        empty.setAlignment(Pos.CENTER);
        empty.getStyleClass().add("empty-state");
        Label title = new Label("You have no ideas yet");
        title.getStyleClass().add("placeholder-title");
        Label sub = new Label("Click “New idea” to publish your first proposal — it will be saved as a DRAFT for an admin to verify.");
        sub.getStyleClass().add("placeholder-subtitle");
        sub.setWrapText(true);
        sub.setMaxWidth(420);
        sub.setAlignment(Pos.CENTER);
        empty.getChildren().addAll(title, sub);
        return empty;
    }

    private void showStatus(String message) {
        if (statusLabel != null) statusLabel.setText(message);
    }
}
