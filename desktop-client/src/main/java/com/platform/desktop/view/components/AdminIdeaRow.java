package com.platform.desktop.view.components;

import com.platform.desktop.api.dto.Idea;
import com.platform.desktop.api.dto.IdeaStatus;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * Compact, information-dense row for the admin idea queue. Different from the
 * founder/investor cards: no FundingRing (admins review many at once and the
 * ring would be noise), and the primary actions are Verify / Reject instead of
 * Edit / Make-offer.
 *
 * <p>Buttons are role-gated by status:
 * <ul>
 *   <li>DRAFT → both Verify and Reject enabled</li>
 *   <li>VERIFIED → both disabled (already approved; no un-verify endpoint)</li>
 *   <li>REJECTED → both disabled (revision is the founder's responsibility)</li>
 * </ul>
 */
public class AdminIdeaRow extends HBox {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM d").withZone(ZoneId.systemDefault());

    public AdminIdeaRow(Idea idea,
                        Consumer<Idea> onVerify,
                        Consumer<Idea> onReject) {

        getStyleClass().add("idea-card");
        setSpacing(14);
        setPadding(new Insets(12, 14, 12, 14));
        setAlignment(Pos.CENTER_LEFT);

        StatusBadge badge = new StatusBadge(idea.status == null ? IdeaStatus.DRAFT : idea.status);

        VBox center = new VBox(3);
        HBox.setHgrow(center, Priority.ALWAYS);

        Label title = new Label(safe(idea.title));
        title.getStyleClass().add("card-title");
        title.setWrapText(true);

        Label summary = new Label(safe(idea.summary));
        summary.getStyleClass().add("card-summary");
        summary.setWrapText(true);
        summary.setMaxWidth(Double.MAX_VALUE);

        String created = idea.createdAt != null ? DATE_FMT.format(idea.createdAt) : "—";
        String loc     = notBlank(idea.location) ? idea.location : "—";
        String funding = idea.fundingNeededOrZero() > 0
                ? formatMoney(idea.fundingNeededOrZero())
                : "—";
        String tags    = (idea.tags == null || idea.tags.isEmpty())
                ? "" : "  ·  " + String.join(", ", idea.tags);
        Label meta = new Label("Founder " + shortId(idea.founderId)
                + "  ·  " + loc + "  ·  " + funding + "  ·  " + created + tags);
        meta.getStyleClass().add("card-meta");

        center.getChildren().addAll(title, summary, meta);

        boolean actionable = idea.status == IdeaStatus.DRAFT;

        Button verifyBtn = new Button("Verify");
        verifyBtn.getStyleClass().addAll("primary-button", "small-button");
        verifyBtn.setDisable(!actionable);
        verifyBtn.setOnAction(e -> { if (onVerify != null) onVerify.accept(idea); });

        Button rejectBtn = new Button("Reject");
        rejectBtn.getStyleClass().addAll("ghost-button", "danger-button", "small-button");
        rejectBtn.setDisable(!actionable);
        rejectBtn.setOnAction(e -> { if (onReject != null) onReject.accept(idea); });

        VBox actions = new VBox(6, verifyBtn, rejectBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.NEVER);

        getChildren().addAll(badge, center, actions);
    }

    private static String safe(String s) { return s == null ? "" : s; }
    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }

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
