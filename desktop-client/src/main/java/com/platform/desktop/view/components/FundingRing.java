package com.platform.desktop.view.components;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/**
 * Circular funding-progress ring, rendered with a {@link Canvas} —
 * <strong>the marquee custom-graphics component for the project.</strong>
 *
 * <p>The component is fully self-contained:
 * <ul>
 *   <li>a faint background ring (full circle) drawn in slate-200</li>
 *   <li>a coloured progress arc starting at 12 o'clock and sweeping
 *       clockwise to {@code (current / goal) × 360°}</li>
 *   <li>a centred percentage label</li>
 *   <li>a small subtitle showing the funding goal formatted as €K / €M</li>
 * </ul>
 *
 * <p>The progress sweep is <em>animated</em> from 0% to the target value over
 * 900&nbsp;ms using a {@link Timeline} on a {@link DoubleProperty} —
 * proving that the canvas re-renders frame-by-frame, not just statically.
 *
 * <p>Colour scales with progress: deep blue under 50%, sky blue 50–100%,
 * green at or above 100% (over-funded).
 */
public class FundingRing extends Region {

    private static final double DEFAULT_SIZE = 130;

    private final Canvas canvas = new Canvas(DEFAULT_SIZE, DEFAULT_SIZE);
    private final DoubleProperty animatedProgress = new SimpleDoubleProperty(0.0);

    private double current = 0;
    private double goal    = 0;

    public FundingRing() {
        getChildren().add(canvas);
        animatedProgress.addListener((obs, oldV, newV) -> redraw());
    }

    /**
     * Update the ring with new values and animate the sweep from 0 → progress.
     * Call from the FX thread.
     */
    public void setData(double current, double goal) {
        this.current = Math.max(0, current);
        this.goal    = Math.max(0, goal);
        double target = (this.goal > 0) ? Math.min(1.5, this.current / this.goal) : 0.0;

        animatedProgress.set(0);
        Timeline timeline = new Timeline(new KeyFrame(
                Duration.millis(900),
                new KeyValue(animatedProgress, target, Interpolator.EASE_OUT)
        ));
        timeline.play();
    }

    // ── rendering --------------------------------------------------------------

    private void redraw() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w <= 0 || h <= 0) return;

        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, w, h);

        double size      = Math.min(w, h);
        double cx        = w / 2.0;
        double cy        = h / 2.0;
        double thickness = Math.max(8, size * 0.11);
        double radius    = size / 2.0 - thickness / 2.0 - 2;

        // Background ring — drawn always, anchors the visual even at 0%.
        g.setStroke(Color.web("#e2e8f0"));
        g.setLineWidth(thickness);
        g.setLineCap(StrokeLineCap.BUTT);
        g.strokeOval(cx - radius, cy - radius, radius * 2, radius * 2);

        double pct = animatedProgress.get();
        if (pct > 0.001) {
            g.setStroke(colourFor(pct));
            g.setLineCap(StrokeLineCap.ROUND);
            // JavaFX arc convention: 0° at 3 o'clock, positive sweep is CCW.
            // We want to start at 12 o'clock (90°) and move clockwise → negative sweep.
            double sweep = -360.0 * Math.min(1.0, pct);
            g.strokeArc(cx - radius, cy - radius,
                        radius * 2, radius * 2,
                        90, sweep, ArcType.OPEN);
        }

        // Centre percentage
        g.setFill(Color.web("#0f172a"));
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        g.setFont(Font.font("Segoe UI", FontWeight.BOLD, size * 0.22));
        g.fillText(String.format("%.0f%%", pct * 100), cx, cy - size * 0.04);

        // Subtitle: funding goal formatted compactly
        g.setFill(Color.web("#64748b"));
        g.setFont(Font.font("Segoe UI", FontWeight.NORMAL, size * 0.10));
        g.fillText(goal > 0 ? "of " + formatMoney(goal) : "no goal set",
                   cx, cy + size * 0.20);
    }

    private static Color colourFor(double pct) {
        if (pct >= 1.0)  return Color.web("#16a34a"); // green-600 — fully funded
        if (pct >= 0.5)  return Color.web("#0ea5e9"); // sky-500
        return Color.web("#1d4ed8");                  // indigo-700
    }

    private static String formatMoney(double v) {
        if (v >= 1_000_000) return String.format("€%.1fM", v / 1_000_000.0);
        if (v >= 1_000)     return String.format("€%.0fk", v / 1_000.0);
        return String.format("€%.0f", v);
    }

    // ── layout -----------------------------------------------------------------

    @Override
    protected void layoutChildren() {
        double size = Math.min(getWidth(), getHeight());
        if (size <= 0) size = DEFAULT_SIZE;
        canvas.setWidth(size);
        canvas.setHeight(size);
        canvas.relocate((getWidth() - size) / 2.0, (getHeight() - size) / 2.0);
        redraw();
    }

    @Override protected double computePrefWidth(double h)  { return DEFAULT_SIZE; }
    @Override protected double computePrefHeight(double w) { return DEFAULT_SIZE; }
    @Override protected double computeMinWidth(double h)   { return 80; }
    @Override protected double computeMinHeight(double w)  { return 80; }
}
