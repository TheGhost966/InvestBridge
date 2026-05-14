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

import java.util.ArrayList;
import java.util.List;

/**
 * Donut-shaped distribution chart, rendered with a {@link Canvas}. Used on the
 * admin dashboard to visualise the breakdown of ideas across the four statuses
 * (DRAFT / SUBMITTED / VERIFIED / REJECTED) — but written generically so it
 * accepts any list of {@link Slice}s.
 *
 * <p>One of the four custom-graphics components built for the
 * "Custom Graphics içeren arayüz" rubric criterion. Sibling of {@link FundingRing}
 * but with the additional complexity of multi-segment drawing, segment
 * proportionality, and an animated reveal that paints all segments at once
 * from 0° → their target sweep over 1.1&nbsp;s.
 *
 * <p>Centred numeric counter is also tweened — counts up from 0 to the total
 * in lockstep with the arc animation. Empty data shows a faint background ring
 * and "No data" so the component is never visually empty.
 */
public class DonutChart extends Region {

    public record Slice(String label, double value, Color color) {}

    private static final double DEFAULT_SIZE = 220;

    private final Canvas canvas = new Canvas(DEFAULT_SIZE, DEFAULT_SIZE);
    private final DoubleProperty animatedProgress = new SimpleDoubleProperty(0.0);

    private List<Slice> slices = List.of();
    private double total = 0;

    public DonutChart() {
        getChildren().add(canvas);
        animatedProgress.addListener((obs, ov, nv) -> redraw());
    }

    /** Replace the data and animate the new shape in from 0% sweep. */
    public void setData(List<Slice> data) {
        this.slices = data == null ? List.of() : new ArrayList<>(data);
        this.total  = slices.stream().mapToDouble(Slice::value).sum();

        animatedProgress.set(0);
        Timeline t = new Timeline(new KeyFrame(
                Duration.millis(1100),
                new KeyValue(animatedProgress, 1.0, Interpolator.EASE_OUT)
        ));
        t.play();
    }

    public List<Slice> getSlices() { return slices; }
    public double getTotal()       { return total; }

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
        double thickness = Math.max(14, size * 0.13);
        double radius    = size / 2.0 - thickness / 2.0 - 4;

        // Faint backdrop ring so the area always reads as a chart, not a void.
        g.setStroke(Color.web("#eef2f7"));
        g.setLineWidth(thickness);
        g.setLineCap(StrokeLineCap.BUTT);
        g.strokeOval(cx - radius, cy - radius, radius * 2, radius * 2);

        double progress = animatedProgress.get();

        if (total > 0) {
            // Each slice draws from its cumulative start angle, swept proportional
            // to (value / total) * 360°, multiplied by the current animation
            // progress for a clean "draw-in" reveal across all slices at once.
            double cumulativeDeg = 0;
            for (Slice s : slices) {
                if (s.value() <= 0) continue;
                double fraction = s.value() / total;
                double sweep    = fraction * 360.0 * progress;
                if (sweep < 0.05) continue;

                g.setStroke(s.color());
                g.setLineCap(StrokeLineCap.BUTT);
                // 12 o'clock is 90° in JavaFX arc coords; CW sweep is negative.
                g.strokeArc(cx - radius, cy - radius,
                            radius * 2, radius * 2,
                            90 - cumulativeDeg, -sweep, ArcType.OPEN);

                cumulativeDeg += fraction * 360.0; // advance start by full slice (not animated)
            }
        }

        // Centre — animated total counter, dimmed if empty.
        long shownTotal = Math.round(total * progress);
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        if (total > 0) {
            g.setFill(Color.web("#0f172a"));
            g.setFont(Font.font("Segoe UI", FontWeight.BOLD, size * 0.18));
            g.fillText(String.valueOf(shownTotal), cx, cy - size * 0.04);
            g.setFill(Color.web("#64748b"));
            g.setFont(Font.font("Segoe UI", FontWeight.NORMAL, size * 0.07));
            g.fillText("ideas total", cx, cy + size * 0.13);
        } else {
            g.setFill(Color.web("#94a3b8"));
            g.setFont(Font.font("Segoe UI", FontWeight.NORMAL, size * 0.09));
            g.fillText("No data", cx, cy);
        }
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
    @Override protected double computeMinWidth(double h)   { return 140; }
    @Override protected double computeMinHeight(double w)  { return 140; }
}
