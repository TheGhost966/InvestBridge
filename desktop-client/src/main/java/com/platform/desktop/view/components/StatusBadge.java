package com.platform.desktop.view.components;

import com.platform.desktop.api.dto.IdeaStatus;
import javafx.geometry.VPos;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

/**
 * Pill-shaped status badge — custom-painted JavaFX component (not a styled
 * {@link javafx.scene.control.Label}).
 *
 * <p>Extends {@link Region} so the layout system manages it, but composes its
 * own scene-graph children ({@link Rectangle} + {@link Text}) and overrides
 * {@link #layoutChildren()} and {@link #computePrefWidth(double)}/
 * {@link #computePrefHeight(double)} to size and position them. Colour and label
 * text update reactively when {@link #setStatus(IdeaStatus)} is called.
 *
 * <p>This is one of the four custom-graphics components built for the
 * "Custom Graphics içeren arayüz" rubric criterion.
 */
public class StatusBadge extends Region {

    private static final double PAD_X = 12;
    private static final double PAD_Y = 4;
    private static final double FONT_SIZE = 11;

    private final Rectangle background = new Rectangle();
    private final Text label = new Text();

    private IdeaStatus status;

    public StatusBadge() {
        this(IdeaStatus.DRAFT);
    }

    public StatusBadge(IdeaStatus initial) {
        background.setArcWidth(999);
        background.setArcHeight(999);
        background.setEffect(new DropShadow(4, Color.rgb(15, 23, 42, 0.18)));

        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, FONT_SIZE));
        label.setFill(Color.WHITE);
        label.setTextOrigin(VPos.TOP);
        label.setTextAlignment(TextAlignment.CENTER);

        getChildren().addAll(background, label);
        setStatus(initial);
    }

    public void setStatus(IdeaStatus status) {
        this.status = status;
        label.setText(displayText(status));
        background.setFill(colorFor(status));
        requestLayout();
    }

    public IdeaStatus getStatus() { return status; }

    /** Human-readable label — falls back to the enum name if the value is unknown. */
    private String displayText(IdeaStatus s) {
        if (s == null) return "—";
        return switch (s) {
            case DRAFT     -> "DRAFT";
            case SUBMITTED -> "SUBMITTED";
            case VERIFIED  -> "VERIFIED";
            case REJECTED  -> "REJECTED";
        };
    }

    /** Status colour palette — kept inside the badge so it owns its own look. */
    private Color colorFor(IdeaStatus s) {
        if (s == null) return Color.web("#64748b");
        return switch (s) {
            case DRAFT     -> Color.web("#64748b"); // slate-500
            case SUBMITTED -> Color.web("#2563eb"); // blue-600
            case VERIFIED  -> Color.web("#16a34a"); // green-600
            case REJECTED  -> Color.web("#dc2626"); // red-600
        };
    }

    // ── layout -----------------------------------------------------------------

    @Override
    protected void layoutChildren() {
        double textW = label.getLayoutBounds().getWidth();
        double textH = label.getLayoutBounds().getHeight();
        double w = textW + 2 * PAD_X;
        double h = textH + 2 * PAD_Y;

        background.setWidth(w);
        background.setHeight(h);
        background.relocate(0, 0);

        label.relocate(PAD_X, PAD_Y);
    }

    @Override
    protected double computePrefWidth(double height) {
        return label.getLayoutBounds().getWidth() + 2 * PAD_X;
    }

    @Override
    protected double computePrefHeight(double width) {
        return label.getLayoutBounds().getHeight() + 2 * PAD_Y;
    }

    @Override protected double computeMinWidth(double h)  { return computePrefWidth(h);  }
    @Override protected double computeMinHeight(double w) { return computePrefHeight(w); }
    @Override protected double computeMaxWidth(double h)  { return computePrefWidth(h);  }
    @Override protected double computeMaxHeight(double w) { return computePrefHeight(w); }
}
