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
 * Card view for a single {@link Idea} on the founder dashboard.
 *
 * <p>Composes the two custom-graphics components:
 * <ul>
 *   <li>{@link FundingRing} on the left — animated funding-progress ring</li>
 *   <li>{@link StatusBadge} top-right — pill-shaped status indicator</li>
 * </ul>
 * Edit / Delete buttons are gated on the idea's status: only DRAFT and
 * REJECTED ideas can be edited (server enforces this too — the buttons just
 * mirror the rule for nicer UX).
 */
public class IdeaCard extends HBox {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneId.systemDefault());

    public IdeaCard(Idea idea,
                    double currentFunding,
                    Consumer<Idea> onEdit,
                    Consumer<Idea> onDelete) {

        getStyleClass().add("idea-card");
        setSpacing(16);
        setPadding(new Insets(16));
        setAlignment(Pos.CENTER_LEFT);

        // Left: funding ring (animated on construction)
        FundingRing ring = new FundingRing();
        ring.setMinSize(120, 120);
        ring.setPrefSize(120, 120);
        ring.setData(currentFunding, idea.fundingNeededOrZero());

        // Right: details column
        VBox right = new VBox(8);
        right.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(right, Priority.ALWAYS);

        // Header row: title + status badge (right-aligned)
        Label title = new Label(safe(idea.title));
        title.getStyleClass().add("card-title");
        title.setWrapText(true);
        StatusBadge badge = new StatusBadge(idea.status == null ? IdeaStatus.DRAFT : idea.status);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(8, title, spacer, badge);
        header.setAlignment(Pos.CENTER_LEFT);

        // Summary
        Label summary = new Label(safe(idea.summary));
        summary.getStyleClass().add("card-summary");
        summary.setWrapText(true);
        summary.setMaxWidth(Double.MAX_VALUE);

        // Optional rejection reason (red) — shown only when REJECTED
        Label rejection = null;
        if (idea.status == IdeaStatus.REJECTED && notBlank(idea.rejectionReason)) {
            rejection = new Label("Rejected: " + idea.rejectionReason);
            rejection.getStyleClass().add("card-rejection");
            rejection.setWrapText(true);
        }

        // Meta line: location · created date
        String created = idea.createdAt != null ? DATE_FMT.format(idea.createdAt) : "—";
        String loc     = notBlank(idea.location) ? idea.location : "—";
        Label meta = new Label(loc + "  ·  Created " + created);
        meta.getStyleClass().add("card-meta");

        // Action buttons — only enabled when editing is allowed
        boolean editable = idea.status == IdeaStatus.DRAFT || idea.status == IdeaStatus.REJECTED;
        boolean deletable = idea.status == IdeaStatus.DRAFT;

        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("ghost-button");
        editBtn.setDisable(!editable);
        editBtn.setOnAction(e -> { if (onEdit != null) onEdit.accept(idea); });

        Button delBtn = new Button("Delete");
        delBtn.getStyleClass().addAll("ghost-button", "danger-button");
        delBtn.setDisable(!deletable);
        delBtn.setOnAction(e -> { if (onDelete != null) onDelete.accept(idea); });

        Region actionSpacer = new Region();
        HBox.setHgrow(actionSpacer, Priority.ALWAYS);
        HBox actions = new HBox(8, actionSpacer, editBtn, delBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        right.getChildren().add(header);
        right.getChildren().add(summary);
        if (rejection != null) right.getChildren().add(rejection);
        right.getChildren().addAll(meta, actions);

        getChildren().addAll(ring, right);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
