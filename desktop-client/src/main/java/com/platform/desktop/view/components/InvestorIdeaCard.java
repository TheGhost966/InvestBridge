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
 * Browse-view card for the investor dashboard. Same visual language as the
 * founder's {@link IdeaCard} (FundingRing on the left, StatusBadge top-right)
 * but with a single "Make offer" call-to-action instead of edit/delete.
 *
 * <p>Funding shown as 0% until the deal-service exposes a per-idea aggregate
 * (planned for P1.5 polish).
 */
public class InvestorIdeaCard extends HBox {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneId.systemDefault());

    public InvestorIdeaCard(Idea idea, Consumer<Idea> onMakeOffer) {

        getStyleClass().add("idea-card");
        setSpacing(16);
        setPadding(new Insets(16));
        setAlignment(Pos.CENTER_LEFT);

        // Funding ring — animates 0→0% on construction (no aggregate yet)
        FundingRing ring = new FundingRing();
        ring.setMinSize(120, 120);
        ring.setPrefSize(120, 120);
        ring.setData(0.0, idea.fundingNeededOrZero());

        // Right column
        VBox right = new VBox(8);
        right.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(right, Priority.ALWAYS);

        Label title = new Label(safe(idea.title));
        title.getStyleClass().add("card-title");
        title.setWrapText(true);

        StatusBadge badge = new StatusBadge(idea.status == null ? IdeaStatus.VERIFIED : idea.status);
        Region spacerH = new Region();
        HBox.setHgrow(spacerH, Priority.ALWAYS);
        HBox header = new HBox(8, title, spacerH, badge);
        header.setAlignment(Pos.CENTER_LEFT);

        Label summary = new Label(safe(idea.summary));
        summary.getStyleClass().add("card-summary");
        summary.setWrapText(true);
        summary.setMaxWidth(Double.MAX_VALUE);

        // Meta line
        String created = idea.createdAt != null ? DATE_FMT.format(idea.createdAt) : "—";
        String loc     = notBlank(idea.location) ? idea.location : "—";
        String tags    = (idea.tags == null || idea.tags.isEmpty()) ? "" : "  ·  " + String.join(", ", idea.tags);
        Label meta = new Label(loc + "  ·  Posted " + created + tags);
        meta.getStyleClass().add("card-meta");

        // CTA
        Button offerBtn = new Button("Make offer");
        offerBtn.getStyleClass().add("primary-button");
        offerBtn.setOnAction(e -> { if (onMakeOffer != null) onMakeOffer.accept(idea); });

        Region actionSpacer = new Region();
        HBox.setHgrow(actionSpacer, Priority.ALWAYS);
        HBox actions = new HBox(8, actionSpacer, offerBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        right.getChildren().addAll(header, summary, meta, actions);
        getChildren().addAll(ring, right);
    }

    private static String safe(String s) { return s == null ? "" : s; }
    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }
}
