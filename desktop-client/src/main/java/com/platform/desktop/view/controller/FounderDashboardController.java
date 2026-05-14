package com.platform.desktop.view.controller;

import com.platform.desktop.api.Services;
import com.platform.desktop.api.dto.CreateIdeaRequest;
import com.platform.desktop.api.dto.Idea;
import com.platform.desktop.api.dto.Match;
import com.platform.desktop.api.dto.Offer;
import com.platform.desktop.api.dto.OfferStatus;
import com.platform.desktop.api.dto.PagedResult;
import com.platform.desktop.util.AsyncUi;
import com.platform.desktop.util.UiUtil;
import com.platform.desktop.view.components.IdeaCard;
import com.platform.desktop.view.components.StatusBadge;
import com.platform.desktop.view.dialog.IdeaFormDialog;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Founder dashboard — three tabs:
 * <ol>
 *   <li><b>My ideas</b> — own ideas with real funding rings (sum of accepted offer amounts)</li>
 *   <li><b>Received offers</b> — offers investors have made on the founder's ideas, with Accept/Reject buttons on each PENDING one</li>
 *   <li><b>My matches</b> — investments locked in</li>
 * </ol>
 *
 * <p>The "My ideas" tab loads ideas + offers in parallel on a background thread,
 * then computes per-idea funding totals client-side before rendering — that
 * keeps the {@link com.platform.desktop.view.components.FundingRing} animations
 * data-driven without needing a server-side aggregate endpoint.
 */
public class FounderDashboardController extends DashboardBase {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneId.systemDefault());

    @FXML private TabPane tabs;

    @FXML private VBox  ideaList;
    @FXML private Label ideasStatus;

    @FXML private VBox  offersList;
    @FXML private Label offersStatus;

    @FXML private VBox  matchesList;
    @FXML private Label matchesStatus;

    private final Set<Integer> loadedTabs = new HashSet<>();

    @Override
    protected void onReady() {
        loadCurrentTab();
        tabs.getSelectionModel().selectedIndexProperty().addListener(
                (obs, oldIdx, newIdx) -> loadCurrentTab());
    }

    private void loadCurrentTab() {
        int idx = tabs.getSelectionModel().getSelectedIndex();
        if (loadedTabs.contains(idx)) return;
        loadedTabs.add(idx);
        switch (idx) {
            case 0 -> loadIdeasAndFunding();
            case 1 -> loadReceivedOffers();
            case 2 -> loadMatches();
            default -> { /* no-op */ }
        }
    }

    /** Mark tabs stale so they re-fetch on next selection — used after mutations. */
    private void invalidateAll() {
        loadedTabs.clear();
        // Force the current tab to re-load right now.
        loadCurrentTab();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tab 1 — My ideas (with real FundingRing values)
    // ─────────────────────────────────────────────────────────────────────────

    /** Tuple held only on the background thread; never escapes the AsyncUi handler. */
    private record IdeasAndOffers(PagedResult<Idea> ideas, List<Offer> offers) {}

    @FXML
    private void onRefreshIdeas() {
        loadedTabs.remove(0);
        loadIdeasAndFunding();
    }

    @FXML
    private void onNewIdea() {
        Optional<CreateIdeaRequest> req = IdeaFormDialog.showCreate();
        req.ifPresent(this::createIdea);
    }

    private void loadIdeasAndFunding() {
        ideasStatus.setText("Loading ideas…");
        AsyncUi.run(
                () -> {
                    PagedResult<Idea> page = Services.ideaApi().list(0, 50);
                    // Offers fetch is best-effort: if deal-service is down,
                    // still render ideas with funding=0 rather than failing the whole tab.
                    List<Offer> offers;
                    try {
                        offers = Services.dealApi().listOffers();
                    } catch (RuntimeException ex) {
                        offers = List.of();
                    }
                    return new IdeasAndOffers(page, offers);
                },
                this::renderIdeasAndFunding,
                err -> ideasStatus.setText("Failed to load ideas — " + UiUtil.friendly(err))
        );
    }

    private void renderIdeasAndFunding(IdeasAndOffers data) {
        ideaList.getChildren().clear();
        List<Idea> items = data.ideas() == null || data.ideas().items == null
                ? List.of() : data.ideas().items;

        if (items.isEmpty()) {
            ideaList.getChildren().add(emptyState(
                    "You have no ideas yet",
                    "Click \"+ New idea\" to publish your first proposal — it will be saved as a DRAFT for an admin to verify."));
            ideasStatus.setText("No ideas yet — click '+ New idea' to get started.");
            return;
        }

        // Sum accepted offer amounts per idea
        Map<String, Double> fundingByIdea = data.offers() == null
                ? Map.of()
                : data.offers().stream()
                        .filter(o -> o.status == OfferStatus.ACCEPTED)
                        .collect(Collectors.groupingBy(
                                o -> o.ideaId,
                                Collectors.summingDouble(Offer::amountOrZero)));

        for (Idea idea : items) {
            double current = fundingByIdea.getOrDefault(idea.id, 0.0);
            ideaList.getChildren().add(new IdeaCard(idea, current, this::editIdea, this::deleteIdea));
        }
        long total = data.ideas().totalElements;
        ideasStatus.setText("Showing " + items.size() + " of " + total
                + " idea" + (total == 1 ? "" : "s") + ".");
    }

    private void createIdea(CreateIdeaRequest req) {
        ideasStatus.setText("Creating idea…");
        AsyncUi.run(
                () -> Services.ideaApi().create(req),
                idea -> { ideasStatus.setText("Idea \"" + idea.title + "\" created."); invalidateAll(); },
                err  -> ideasStatus.setText("Create failed — " + UiUtil.friendly(err))
        );
    }

    private void editIdea(Idea idea) {
        Optional<CreateIdeaRequest> req = IdeaFormDialog.showEdit(idea);
        req.ifPresent(r -> {
            ideasStatus.setText("Saving changes…");
            AsyncUi.run(
                    () -> Services.ideaApi().update(idea.id, r),
                    saved -> { ideasStatus.setText("Saved \"" + saved.title + "\"."); invalidateAll(); },
                    err   -> ideasStatus.setText("Save failed — " + UiUtil.friendly(err))
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

        ideasStatus.setText("Deleting…");
        AsyncUi.run(
                () -> { Services.ideaApi().delete(idea.id); return null; },
                ignored -> { ideasStatus.setText("Deleted."); invalidateAll(); },
                err     -> ideasStatus.setText("Delete failed — " + UiUtil.friendly(err))
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tab 2 — Received offers (incoming, with accept/reject)
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void onRefreshReceivedOffers() {
        loadedTabs.remove(1);
        loadReceivedOffers();
    }

    private void loadReceivedOffers() {
        offersStatus.setText("Loading received offers…");
        AsyncUi.run(
                () -> Services.dealApi().listOffers(),
                this::renderReceivedOffers,
                err -> offersStatus.setText("Failed to load offers — " + UiUtil.friendly(err))
        );
    }

    private void renderReceivedOffers(List<Offer> offers) {
        offersList.getChildren().clear();
        if (offers == null || offers.isEmpty()) {
            offersList.getChildren().add(emptyState(
                    "No offers yet",
                    "Investors will appear here once they make offers on your verified ideas."));
            offersStatus.setText("");
            return;
        }
        // Sort: PENDING first (action needed), then by date desc
        offers = offers.stream()
                .sorted((a, b) -> {
                    int byStatus = Integer.compare(priority(a.status), priority(b.status));
                    if (byStatus != 0) return byStatus;
                    if (a.createdAt == null || b.createdAt == null) return 0;
                    return b.createdAt.compareTo(a.createdAt);
                })
                .toList();

        for (Offer o : offers) offersList.getChildren().add(receivedOfferRow(o));

        long pending = offers.stream().filter(o -> o.status == OfferStatus.PENDING).count();
        offersStatus.setText(offers.size() + " offer" + (offers.size() == 1 ? "" : "s")
                + ", " + pending + " awaiting your decision.");
    }

    private static int priority(OfferStatus s) {
        if (s == OfferStatus.PENDING)  return 0;
        if (s == OfferStatus.ACCEPTED) return 1;
        return 2; // REJECTED + null
    }

    private Node receivedOfferRow(Offer offer) {
        HBox row = new HBox(14);
        row.getStyleClass().add("idea-card");
        row.setPadding(new Insets(14));
        row.setAlignment(Pos.CENTER_LEFT);

        StatusBadge badge = new StatusBadge(offer.status);

        VBox center = new VBox(4);
        HBox.setHgrow(center, Priority.ALWAYS);
        Label amount = new Label(formatMoney(offer.amountOrZero()));
        amount.getStyleClass().add("card-title");
        Label src = new Label("From investor " + shortId(offer.investorId)
                + "  ·  Idea " + shortId(offer.ideaId));
        src.getStyleClass().add("card-meta");
        Label msg = new Label(offer.message == null || offer.message.isBlank()
                ? "(no message)" : offer.message);
        msg.getStyleClass().add("card-summary");
        msg.setWrapText(true);
        center.getChildren().addAll(amount, src, msg);

        Label date = new Label(offer.createdAt == null ? "—" : DATE_FMT.format(offer.createdAt));
        date.getStyleClass().add("card-meta");

        VBox actions = new VBox(6);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button acceptBtn = new Button("Accept");
        acceptBtn.getStyleClass().addAll("primary-button", "small-button");
        Button rejectBtn = new Button("Reject");
        rejectBtn.getStyleClass().addAll("ghost-button", "danger-button", "small-button");

        boolean actionable = offer.status == OfferStatus.PENDING;
        acceptBtn.setDisable(!actionable);
        rejectBtn.setDisable(!actionable);
        acceptBtn.setOnAction(e -> acceptOffer(offer));
        rejectBtn.setOnAction(e -> rejectOffer(offer));
        actions.getChildren().addAll(acceptBtn, rejectBtn);

        row.getChildren().addAll(badge, center, date, actions);
        return row;
    }

    private void acceptOffer(Offer offer) {
        Alert confirm = new Alert(AlertType.CONFIRMATION,
                "Accept this offer of " + formatMoney(offer.amountOrZero())
                        + "? A match will be created and the funding will be locked in.",
                ButtonType.CANCEL, ButtonType.OK);
        confirm.setHeaderText("Accept offer");
        Optional<ButtonType> choice = confirm.showAndWait();
        if (choice.isEmpty() || choice.get() != ButtonType.OK) return;

        offersStatus.setText("Accepting…");
        AsyncUi.run(
                () -> Services.dealApi().acceptOffer(offer.id),
                ok -> { offersStatus.setText("Offer accepted — match created."); invalidateAll(); },
                err -> offersStatus.setText("Accept failed — " + UiUtil.friendly(err))
        );
    }

    private void rejectOffer(Offer offer) {
        offersStatus.setText("Rejecting…");
        AsyncUi.run(
                () -> Services.dealApi().rejectOffer(offer.id),
                ok -> { offersStatus.setText("Offer rejected."); invalidateAll(); },
                err -> offersStatus.setText("Reject failed — " + UiUtil.friendly(err))
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tab 3 — My matches
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void onRefreshMatches() {
        loadedTabs.remove(2);
        loadMatches();
    }

    private void loadMatches() {
        matchesStatus.setText("Loading matches…");
        AsyncUi.run(
                () -> Services.dealApi().getMatches(),
                this::renderMatches,
                err -> matchesStatus.setText("Failed to load matches — " + UiUtil.friendly(err))
        );
    }

    private void renderMatches(List<Match> matches) {
        matchesList.getChildren().clear();
        if (matches == null || matches.isEmpty()) {
            matchesList.getChildren().add(emptyState(
                    "No matches yet",
                    "Once you accept an investor's offer, the match shows up here."));
            matchesStatus.setText("");
            return;
        }
        for (Match m : matches) matchesList.getChildren().add(matchRow(m));
        matchesStatus.setText(matches.size() + " match" + (matches.size() == 1 ? "" : "es") + ".");
    }

    private Node matchRow(Match m) {
        HBox row = new HBox(14);
        row.getStyleClass().add("idea-card");
        row.setPadding(new Insets(14));
        row.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("🤝");
        icon.setStyle("-fx-font-size: 22px;");

        VBox center = new VBox(4);
        HBox.setHgrow(center, Priority.ALWAYS);
        Label title = new Label("Match on idea " + shortId(m.ideaId));
        title.getStyleClass().add("card-title");
        Label sub = new Label("Investor " + shortId(m.investorId)
                + "  ·  Offer " + shortId(m.offerId));
        sub.getStyleClass().add("card-meta");
        center.getChildren().addAll(title, sub);

        Label date = new Label(m.createdAt == null ? "—" : DATE_FMT.format(m.createdAt));
        date.getStyleClass().add("card-meta");

        row.getChildren().addAll(icon, center, date);
        return row;
    }

    // ── shared utilities ──────────────────────────────────────────────────────

    private Node emptyState(String title, String detail) {
        VBox empty = new VBox(8);
        empty.setAlignment(Pos.CENTER);
        empty.getStyleClass().add("empty-state");
        Label t = new Label(title);
        t.getStyleClass().add("placeholder-title");
        Label d = new Label(detail);
        d.getStyleClass().add("placeholder-subtitle");
        d.setWrapText(true);
        d.setMaxWidth(420);
        d.setAlignment(Pos.CENTER);
        empty.getChildren().addAll(t, d);
        return empty;
    }

    private static String formatMoney(double v) {
        if (v >= 1_000_000) return String.format("€%.1fM", v / 1_000_000.0);
        if (v >= 1_000)     return String.format("€%,d", (long) v);
        return String.format("€%.0f", v);
    }

    private static String shortId(String id) {
        if (id == null) return "—";
        return id.length() <= 6 ? id : id.substring(0, 6);
    }
}
