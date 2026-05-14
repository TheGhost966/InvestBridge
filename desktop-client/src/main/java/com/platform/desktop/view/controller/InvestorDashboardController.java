package com.platform.desktop.view.controller;

import com.platform.desktop.api.Services;
import com.platform.desktop.api.dto.CreateOfferRequest;
import com.platform.desktop.api.dto.CreateProfileRequest;
import com.platform.desktop.api.dto.Idea;
import com.platform.desktop.api.dto.InvestorProfile;
import com.platform.desktop.api.dto.Match;
import com.platform.desktop.api.dto.Offer;
import com.platform.desktop.api.dto.PagedResult;
import com.platform.desktop.util.AsyncUi;
import com.platform.desktop.util.UiUtil;
import com.platform.desktop.view.components.InvestorIdeaCard;
import com.platform.desktop.view.components.StatusBadge;
import com.platform.desktop.view.dialog.MakeOfferDialog;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Investor dashboard — four tabs:
 * <ol>
 *   <li><b>Browse ideas</b> — verified ideas filtered by the backend (INVESTOR
 *       only sees VERIFIED). Each card has a "Make offer" button.</li>
 *   <li><b>My profile</b> — single editable form (create or update).</li>
 *   <li><b>My offers</b> — outgoing offers with PENDING/ACCEPTED/REJECTED status badges.</li>
 *   <li><b>My matches</b> — accepted offers materialised into matches.</li>
 * </ol>
 *
 * <p>Lazy-loading: only the active tab's data is fetched. The Browse tab loads
 * on dashboard entry; the others fire when the user selects them.
 */
public class InvestorDashboardController extends DashboardBase {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneId.systemDefault());

    // ── FXML hooks: shell ─────────────────────────────────────────────────────
    @FXML private TabPane tabs;

    // Browse
    @FXML private VBox  browseList;
    @FXML private Label browseStatus;

    // Profile
    @FXML private TextArea bioField;
    @FXML private TextField sectorsField;
    @FXML private TextField minField;
    @FXML private TextField maxField;
    @FXML private Button   saveProfileBtn;
    @FXML private Label    profileStatus;
    @FXML private Label    profileError;

    // Offers
    @FXML private VBox  offersList;
    @FXML private Label offersStatus;

    // Matches
    @FXML private VBox  matchesList;
    @FXML private Label matchesStatus;

    private final Set<Integer> loadedTabs = new HashSet<>();
    private InvestorProfile myProfile;  // null until first GET

    @Override
    protected void onReady() {
        // Load whatever tab is initially selected (Browse, by default).
        loadCurrentTab();
        tabs.getSelectionModel().selectedIndexProperty().addListener(
                (obs, oldIdx, newIdx) -> loadCurrentTab());
    }

    private void loadCurrentTab() {
        int idx = tabs.getSelectionModel().getSelectedIndex();
        if (loadedTabs.contains(idx)) return;
        loadedTabs.add(idx);
        switch (idx) {
            case 0 -> loadBrowse();
            case 1 -> loadProfile();
            case 2 -> loadOffers();
            case 3 -> loadMatches();
            default -> { /* no-op */ }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tab 1 — Browse verified ideas
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void onRefreshBrowse() {
        loadBrowse();
    }

    private void loadBrowse() {
        browseStatus.setText("Loading verified ideas…");
        AsyncUi.run(
                () -> Services.ideaApi().list(0, 100),
                this::renderBrowse,
                err -> browseStatus.setText("Failed to load ideas — " + UiUtil.friendly(err))
        );
    }

    private void renderBrowse(PagedResult<Idea> page) {
        browseList.getChildren().clear();
        List<Idea> items = page == null || page.items == null ? List.of() : page.items;
        if (items.isEmpty()) {
            browseList.getChildren().add(emptyState(
                    "No verified ideas to browse yet",
                    "Once an admin verifies a founder's idea, it will appear here for you to invest in."));
            browseStatus.setText("No ideas available right now.");
            return;
        }
        for (Idea idea : items) {
            browseList.getChildren().add(new InvestorIdeaCard(idea, this::openOfferDialog));
        }
        browseStatus.setText("Showing " + items.size() + " idea" + (items.size() == 1 ? "" : "s") + ".");
    }

    private void openOfferDialog(Idea idea) {
        Optional<CreateOfferRequest> req = MakeOfferDialog.showFor(idea);
        req.ifPresent(this::submitOffer);
    }

    private void submitOffer(CreateOfferRequest req) {
        browseStatus.setText("Sending offer…");
        AsyncUi.run(
                () -> Services.dealApi().makeOffer(req),
                offer -> {
                    browseStatus.setText("Offer of " + formatMoney(offer.amountOrZero()) + " sent.");
                    // Invalidate offers cache so next visit re-fetches
                    loadedTabs.remove(2);
                },
                err -> browseStatus.setText("Offer failed — " + UiUtil.friendly(err))
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tab 2 — My profile
    // ─────────────────────────────────────────────────────────────────────────

    private void loadProfile() {
        profileStatus.setText("Loading profile…");
        hideProfileError();
        AsyncUi.run(
                () -> Services.dealApi().getMyProfile(),
                this::renderProfile,
                err -> profileStatus.setText("Failed to load profile — " + UiUtil.friendly(err))
        );
    }

    private void renderProfile(InvestorProfile profile) {
        this.myProfile = profile;
        if (profile == null) {
            profileStatus.setText("You have no profile yet — fill the form to create one.");
            saveProfileBtn.setText("Create profile");
            bioField.clear(); sectorsField.clear(); minField.clear(); maxField.clear();
            return;
        }
        profileStatus.setText("Profile loaded.");
        saveProfileBtn.setText("Save changes");
        bioField.setText(orEmpty(profile.bio));
        sectorsField.setText(profile.sectors == null ? "" : String.join(", ", profile.sectors));
        minField.setText(profile.minInvestment == null ? "" : String.valueOf(profile.minInvestment.longValue()));
        maxField.setText(profile.maxInvestment == null ? "" : String.valueOf(profile.maxInvestment.longValue()));
    }

    @FXML
    private void onSaveProfile() {
        hideProfileError();
        String bio = bioField.getText() == null ? "" : bioField.getText().trim();
        if (bio.isEmpty()) { showProfileError("Bio is required."); return; }

        Double min = parsePositive(minField.getText());
        Double max = parsePositive(maxField.getText());
        if (minField.getText() != null && !minField.getText().isBlank() && min == null) {
            showProfileError("Min must be a positive number."); return;
        }
        if (maxField.getText() != null && !maxField.getText().isBlank() && max == null) {
            showProfileError("Max must be a positive number."); return;
        }
        if (min != null && max != null && max < min) {
            showProfileError("Max must be ≥ Min."); return;
        }

        CreateProfileRequest req = new CreateProfileRequest();
        req.bio = bio;
        req.sectors = parseList(sectorsField.getText());
        req.minInvestment = min;
        req.maxInvestment = max;

        saveProfileBtn.setDisable(true);
        profileStatus.setText("Saving…");

        // Backend currently has no PUT — POST creates, second POST returns 409.
        // For the desktop client a "save" semantically means "create if absent".
        AsyncUi.run(
                () -> Services.dealApi().createProfile(req),
                saved -> {
                    saveProfileBtn.setDisable(false);
                    renderProfile(saved);
                    profileStatus.setText("Profile saved.");
                },
                err -> {
                    saveProfileBtn.setDisable(false);
                    profileStatus.setText("Save failed — " + UiUtil.friendly(err));
                }
        );
    }

    private void showProfileError(String msg) {
        profileError.setText(msg);
        profileError.setManaged(true);
        profileError.setVisible(true);
    }

    private void hideProfileError() {
        profileError.setManaged(false);
        profileError.setVisible(false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tab 3 — My offers
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void onRefreshOffers() {
        loadOffers();
    }

    private void loadOffers() {
        offersStatus.setText("Loading offers…");
        AsyncUi.run(
                () -> Services.dealApi().listOffers(),
                this::renderOffers,
                err -> offersStatus.setText("Failed to load offers — " + UiUtil.friendly(err))
        );
    }

    private void renderOffers(List<Offer> offers) {
        offersList.getChildren().clear();
        if (offers == null || offers.isEmpty()) {
            offersList.getChildren().add(emptyState(
                    "No offers yet",
                    "Browse verified ideas and click \"Make offer\" to invest. Your offers will show up here with their status."));
            offersStatus.setText("");
            return;
        }
        for (Offer o : offers) offersList.getChildren().add(offerRow(o));
        long pending = offers.stream().filter(o -> o.status == com.platform.desktop.api.dto.OfferStatus.PENDING).count();
        offersStatus.setText(offers.size() + " offer" + (offers.size() == 1 ? "" : "s")
                + ", " + pending + " awaiting decision.");
    }

    private Node offerRow(Offer offer) {
        HBox row = new HBox(14);
        row.getStyleClass().add("idea-card");
        row.setPadding(new Insets(14));
        row.setAlignment(Pos.CENTER_LEFT);

        StatusBadge badge = new StatusBadge(offer.status);

        VBox center = new VBox(4);
        HBox.setHgrow(center, Priority.ALWAYS);
        Label amount = new Label(formatMoney(offer.amountOrZero()));
        amount.getStyleClass().add("card-title");
        Label idea = new Label("Idea " + shortId(offer.ideaId));
        idea.getStyleClass().add("card-meta");
        Label msg = new Label(offer.message == null || offer.message.isBlank()
                ? "(no message)" : offer.message);
        msg.getStyleClass().add("card-summary");
        msg.setWrapText(true);
        center.getChildren().addAll(amount, idea, msg);

        Label date = new Label(offer.createdAt == null ? "—" : DATE_FMT.format(offer.createdAt));
        date.getStyleClass().add("card-meta");

        row.getChildren().addAll(badge, center, date);
        return row;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tab 4 — My matches
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void onRefreshMatches() {
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
                    "When a founder accepts one of your offers, the deal is locked in and shows up here."));
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
        Label sub = new Label("Founder " + shortId(m.founderId)
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

    private static String orEmpty(String s) { return s == null ? "" : s; }

    private static List<String> parseList(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private static Double parsePositive(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            double v = Double.parseDouble(raw.trim());
            return v > 0 ? v : null;
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

    private static String formatMoney(double v) {
        if (v >= 1_000_000) return String.format("€%.1fM", v / 1_000_000.0);
        if (v >= 1_000)     return String.format("€%,d", (long) v);
        return String.format("€%.0f", v);
    }

    /** First 6 chars of a Mongo ObjectId — enough to disambiguate in demos. */
    private static String shortId(String id) {
        if (id == null) return "—";
        return id.length() <= 6 ? id : id.substring(0, 6);
    }
}
