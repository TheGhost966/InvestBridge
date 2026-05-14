package com.platform.desktop.view.components;

import com.platform.desktop.api.dto.Match;
import javafx.animation.AnimationTimer;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Force-directed graph of investor ↔ idea matches — the fourth and most
 * complex custom-graphics component. Renders entirely on a {@link Canvas}
 * with an {@link AnimationTimer}-driven physics simulation.
 *
 * <p><b>Physics</b>: each frame applies three forces to every node:
 * <ul>
 *   <li><i>Repulsion</i> — Coulomb-style 1/d² push between every pair of
 *       nodes, so the layout doesn't collapse on top of itself</li>
 *   <li><i>Spring</i> — Hooke's-law pull along every edge towards a rest
 *       length, so connected nodes cluster</li>
 *   <li><i>Centering</i> — a weak constant pull towards the canvas centre,
 *       so the graph never drifts off-screen</li>
 * </ul>
 * Velocity is integrated with simple Euler steps and damped each frame so the
 * system settles without oscillating forever.
 *
 * <p><b>Interaction</b>: click-drag any node to move it; while dragging the
 * node is fixed and other nodes still react. Releasing rejoins it to the
 * simulation. New data resets node positions to a randomised circle around
 * the centre.
 *
 * <p>Investor nodes are rendered blue; idea nodes green; edges as soft grey
 * lines drawn behind the nodes so circles always sit on top.
 */
public class MatchGraph extends Region {

    // ── physics tuning ────────────────────────────────────────────────────────
    private static final double REPULSION       = 9_000;  // k_r in F = k_r / d²
    private static final double SPRING_K        = 0.04;   // Hooke's stiffness
    private static final double SPRING_LENGTH   = 130;    // edge rest length, px
    private static final double CENTERING       = 0.005;  // weak pull to centre
    private static final double DAMPING         = 0.85;   // velocity decay per frame
    private static final double MAX_VELOCITY    = 12;     // clamp so things don't explode
    private static final double NODE_RADIUS     = 18;
    private static final double DEFAULT_HEIGHT  = 360;

    private static final Color COLOR_INVESTOR   = Color.web("#2563eb"); // blue-600
    private static final Color COLOR_IDEA       = Color.web("#16a34a"); // green-600
    private static final Color COLOR_EDGE       = Color.web("#cbd5e1"); // slate-300
    private static final Color COLOR_LABEL      = Color.WHITE;

    private enum NodeType { INVESTOR, IDEA }

    private static final class GraphNode {
        final String id;
        final NodeType type;
        final String label;
        double x, y;
        double vx = 0, vy = 0;
        boolean fixed = false;

        GraphNode(String id, NodeType type, double x, double y) {
            this.id = id;
            this.type = type;
            this.x = x;
            this.y = y;
            this.label = id == null ? "?" : id.substring(0, Math.min(4, id.length()));
        }
    }

    private static final class Edge {
        final GraphNode a, b;
        Edge(GraphNode a, GraphNode b) { this.a = a; this.b = b; }
    }

    private final Canvas canvas = new Canvas(600, DEFAULT_HEIGHT);

    private List<GraphNode> nodes = new ArrayList<>();
    private List<Edge> edges = new ArrayList<>();
    private GraphNode dragging;

    private final AnimationTimer timer;
    private final Random rng = new Random(42);

    public MatchGraph() {
        getChildren().add(canvas);

        canvas.setOnMousePressed(ev -> {
            GraphNode hit = findNodeAt(ev.getX(), ev.getY());
            if (hit != null) {
                dragging = hit;
                hit.fixed = true;
                hit.vx = 0;
                hit.vy = 0;
            }
        });
        canvas.setOnMouseDragged(ev -> {
            if (dragging != null) {
                dragging.x = clamp(ev.getX(), NODE_RADIUS, canvas.getWidth()  - NODE_RADIUS);
                dragging.y = clamp(ev.getY(), NODE_RADIUS, canvas.getHeight() - NODE_RADIUS);
            }
        });
        canvas.setOnMouseReleased(ev -> {
            if (dragging != null) {
                dragging.fixed = false;
                dragging = null;
            }
        });

        timer = new AnimationTimer() {
            @Override public void handle(long now) {
                step();
                redraw();
            }
        };

        // Self-manage the timer's lifecycle: run while attached to a scene,
        // stop when removed (e.g. on logout). Prevents zombie timers from
        // piling up across login/logout cycles.
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) timer.stop();
            else                  timer.start();
        });
    }

    public void start() { timer.start(); }
    public void stop()  { timer.stop();  }

    /**
     * Replace the graph with a fresh layout derived from the given matches.
     * Investor and idea nodes are deduplicated by id; edges are 1:1 with matches.
     */
    public void setMatches(List<Match> matches) {
        Map<String, GraphNode> byKey = new HashMap<>();
        List<Edge> newEdges = new ArrayList<>();

        double cx = (canvas.getWidth()  > 0 ? canvas.getWidth()  : 600) / 2.0;
        double cy = (canvas.getHeight() > 0 ? canvas.getHeight() : DEFAULT_HEIGHT) / 2.0;

        if (matches != null) {
            for (Match m : matches) {
                if (m == null) continue;
                String invKey = "I:" + m.investorId;
                String ideaKey = "D:" + m.ideaId;
                GraphNode inv  = byKey.computeIfAbsent(invKey,
                        k -> randomNode(m.investorId, NodeType.INVESTOR, cx, cy));
                GraphNode idea = byKey.computeIfAbsent(ideaKey,
                        k -> randomNode(m.ideaId, NodeType.IDEA, cx, cy));
                newEdges.add(new Edge(inv, idea));
            }
        }
        this.nodes = new ArrayList<>(byKey.values());
        this.edges = newEdges;
        redraw();
    }

    private GraphNode randomNode(String id, NodeType type, double cx, double cy) {
        // Spawn each node on a small jittered circle around the centre so the
        // initial frame isn't a degenerate stack of overlapping circles.
        double angle = rng.nextDouble() * Math.PI * 2;
        double r     = 80 + rng.nextDouble() * 60;
        return new GraphNode(id, type, cx + Math.cos(angle) * r, cy + Math.sin(angle) * r);
    }

    private GraphNode findNodeAt(double x, double y) {
        for (int i = nodes.size() - 1; i >= 0; i--) {
            GraphNode n = nodes.get(i);
            double dx = n.x - x, dy = n.y - y;
            if (dx * dx + dy * dy <= NODE_RADIUS * NODE_RADIUS) return n;
        }
        return null;
    }

    // ── physics ───────────────────────────────────────────────────────────────

    private void step() {
        if (nodes.isEmpty()) return;
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        double cx = w / 2.0, cy = h / 2.0;

        // Pairwise repulsion
        for (int i = 0; i < nodes.size(); i++) {
            GraphNode a = nodes.get(i);
            if (a.fixed) continue;
            for (int j = 0; j < nodes.size(); j++) {
                if (i == j) continue;
                GraphNode b = nodes.get(j);
                double dx = a.x - b.x;
                double dy = a.y - b.y;
                double d2 = dx * dx + dy * dy;
                if (d2 < 1) d2 = 1;
                double f  = REPULSION / d2;
                double d  = Math.sqrt(d2);
                a.vx += (dx / d) * f / 60.0;   // /60 ≈ scale to per-frame timestep
                a.vy += (dy / d) * f / 60.0;
            }
        }

        // Spring forces along edges
        for (Edge e : edges) {
            double dx = e.b.x - e.a.x;
            double dy = e.b.y - e.a.y;
            double d  = Math.sqrt(dx * dx + dy * dy);
            if (d < 0.01) d = 0.01;
            double force = SPRING_K * (d - SPRING_LENGTH);
            double fx = (dx / d) * force;
            double fy = (dy / d) * force;
            if (!e.a.fixed) { e.a.vx += fx; e.a.vy += fy; }
            if (!e.b.fixed) { e.b.vx -= fx; e.b.vy -= fy; }
        }

        // Weak centering + integration + damping + bounds clamp
        for (GraphNode n : nodes) {
            if (n.fixed) continue;
            n.vx += (cx - n.x) * CENTERING;
            n.vy += (cy - n.y) * CENTERING;
            n.vx *= DAMPING;
            n.vy *= DAMPING;
            n.vx = clamp(n.vx, -MAX_VELOCITY, MAX_VELOCITY);
            n.vy = clamp(n.vy, -MAX_VELOCITY, MAX_VELOCITY);
            n.x += n.vx;
            n.y += n.vy;
            n.x = clamp(n.x, NODE_RADIUS, w - NODE_RADIUS);
            n.y = clamp(n.y, NODE_RADIUS, h - NODE_RADIUS);
        }
    }

    // ── rendering --------------------------------------------------------------

    private void redraw() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w <= 0 || h <= 0) return;

        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, w, h);

        if (nodes.isEmpty()) {
            g.setFill(Color.web("#94a3b8"));
            g.setTextAlign(TextAlignment.CENTER);
            g.setTextBaseline(VPos.CENTER);
            g.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
            g.fillText("No matches yet — investor ↔ idea graph will appear here once founders accept offers.",
                       w / 2.0, h / 2.0);
            return;
        }

        // Edges first so circles draw on top
        g.setStroke(COLOR_EDGE);
        g.setLineWidth(1.5);
        for (Edge e : edges) {
            g.strokeLine(e.a.x, e.a.y, e.b.x, e.b.y);
        }

        // Node circles
        for (GraphNode n : nodes) {
            Color c = n.type == NodeType.INVESTOR ? COLOR_INVESTOR : COLOR_IDEA;
            g.setFill(c);
            g.fillOval(n.x - NODE_RADIUS, n.y - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);
            g.setStroke(c.darker());
            g.setLineWidth(2);
            g.strokeOval(n.x - NODE_RADIUS, n.y - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);
        }

        // Labels last
        g.setFill(COLOR_LABEL);
        g.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        for (GraphNode n : nodes) {
            g.fillText(n.label, n.x, n.y);
        }

        // Tiny in-canvas legend bottom-left
        drawLegend(g, w, h);
    }

    private void drawLegend(GraphicsContext g, double w, double h) {
        double x0 = 12;
        double y0 = h - 22;
        g.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 11));
        g.setTextAlign(TextAlignment.LEFT);
        g.setTextBaseline(VPos.CENTER);

        g.setFill(COLOR_INVESTOR);
        g.fillOval(x0, y0 - 5, 10, 10);
        g.setFill(Color.web("#475569"));
        g.fillText("Investor", x0 + 16, y0);

        double x1 = x0 + 80;
        g.setFill(COLOR_IDEA);
        g.fillOval(x1, y0 - 5, 10, 10);
        g.setFill(Color.web("#475569"));
        g.fillText("Idea", x1 + 16, y0);
    }

    // ── layout -----------------------------------------------------------------

    @Override
    protected void layoutChildren() {
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
    }

    @Override protected double computePrefWidth(double h)  { return 600; }
    @Override protected double computePrefHeight(double w) { return DEFAULT_HEIGHT; }
    @Override protected double computeMinHeight(double w)  { return 240; }

    // ── helpers ---------------------------------------------------------------

    private static double clamp(double v, double min, double max) {
        return v < min ? min : (v > max ? max : v);
    }
}
