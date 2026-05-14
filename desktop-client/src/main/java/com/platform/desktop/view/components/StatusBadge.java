package com.platform.desktop.view.components;

import com.platform.desktop.api.dto.IdeaStatus;
import com.platform.desktop.api.dto.OfferStatus;
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
 * {@link #computePrefHeight(double)} to size and position them.
 *
 * <p>Reusable across status enums: pass an {@link IdeaStatus} or
 * {@link OfferStatus} to the constructor / {@link #apply(IdeaStatus)} /
 * {@link #apply(OfferStatus)}. Each enum has its own colour palette owned by
 * this class — keeps the visual language consistent across dashboards.
 *
 * <p>One of the four custom-graphics components built for the
 * "Custom Graphics içeren arayüz" rubric criterion.
 */
public class StatusBadge extends Region {

    private static final double PAD_X = 12;
    private static final double PAD_Y = 4;
    private static final double FONT_SIZE = 11;
    private static final Color  NEUTRAL = Color.web("#64748b");

    private final Rectangle background = new Rectangle();
    private final Text label = new Text();

    public StatusBadge() {
        background.setArcWidth(999);
        background.setArcHeight(999);
        background.setEffect(new DropShadow(4, Color.rgb(15, 23, 42, 0.18)));

        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, FONT_SIZE));
        label.setFill(Color.WHITE);
        label.setTextOrigin(VPos.TOP);
        label.setTextAlignment(TextAlignment.CENTER);

        getChildren().addAll(background, label);
        applyRaw("—", NEUTRAL);
    }

    public StatusBadge(IdeaStatus status)  { this(); apply(status);  }
    public StatusBadge(OfferStatus status) { this(); apply(status);  }

    // ── public API: apply-by-enum (overloaded) ────────────────────────────────

    public void apply(IdeaStatus s) {
        applyRaw(textFor(s), colorFor(s));
    }

    public void apply(OfferStatus s) {
        applyRaw(textFor(s), colorFor(s));
    }

    private void applyRaw(String text, Color color) {
        label.setText(text);
        background.setFill(color);
        requestLayout();
    }

    // ── palette ----------------------------------------------------------------

    private static String textFor(IdeaStatus s) {
        return s == null ? "—" : s.name();
    }

    private static Color colorFor(IdeaStatus s) {
        if (s == null) return NEUTRAL;
        return switch (s) {
            case DRAFT     -> Color.web("#64748b"); // slate-500
            case SUBMITTED -> Color.web("#2563eb"); // blue-600
            case VERIFIED  -> Color.web("#16a34a"); // green-600
            case REJECTED  -> Color.web("#dc2626"); // red-600
        };
    }

    private static String textFor(OfferStatus s) {
        return s == null ? "—" : s.name();
    }

    private static Color colorFor(OfferStatus s) {
        if (s == null) return NEUTRAL;
        return switch (s) {
            case PENDING  -> Color.web("#d97706"); // amber-600 — awaiting decision
            case ACCEPTED -> Color.web("#16a34a"); // green-600 — match created
            case REJECTED -> Color.web("#dc2626"); // red-600
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
