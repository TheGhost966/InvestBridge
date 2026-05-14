package com.platform.desktop.view.controller;

import com.platform.desktop.api.Services;
import com.platform.desktop.api.dto.Idea;
import com.platform.desktop.api.dto.IdeaStatus;
import com.platform.desktop.api.dto.Match;
import com.platform.desktop.api.dto.PagedResult;
import com.platform.desktop.util.AsyncUi;
import com.platform.desktop.util.UiUtil;
import com.platform.desktop.view.components.AdminIdeaRow;
import com.platform.desktop.view.components.DonutChart;
import com.platform.desktop.view.components.MatchGraph;
import com.platform.desktop.view.dialog.RejectReasonDialog;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Admin dashboard — operational view of every idea in the platform.
 *
 * <p>Top section: a {@link DonutChart} (custom-graphics) summarising the
 * status distribution, with an interactive legend showing each status' colour
 * and live count. Bottom section: a filterable queue of {@link AdminIdeaRow}s
 * with Verify / Reject actions on DRAFT items.
 *
 * <p>The filter is purely client-side — we hold every idea in memory after a
 * single fetch, then re-render the visible subset when the toggle changes.
 * The DonutChart shows totals across <em>all</em> statuses (not the filtered
 * subset) so an admin always sees the global picture.
 */
public class AdminDashboardController extends DashboardBase {

    /** Palette used by both the DonutChart slices and the legend dots. */
    private static final Map<IdeaStatus, Color> PALETTE = new EnumMap<>(IdeaStatus.class);
    static {
        PALETTE.put(IdeaStatus.DRAFT,     Color.web("#64748b")); // slate
        PALETTE.put(IdeaStatus.SUBMITTED, Color.web("#2563eb")); // blue
        PALETTE.put(IdeaStatus.VERIFIED,  Color.web("#16a34a")); // green
        PALETTE.put(IdeaStatus.REJECTED,  Color.web("#dc2626")); // red
    }

    @FXML private DonutChart   donut;
    @FXML private VBox         legendBox;
    @FXML private MatchGraph   matchGraph;
    @FXML private Label        matchesStatus;
    @FXML private VBox         queueList;
    @FXML private Label        queueStatus;
    @FXML private ToggleGroup  filterGroup;

    /** All ideas fetched on the last load — single source of truth for filter rendering. */
    private List<Idea> allIdeas = List.of();

    @Override
    protected void onReady() {
        filterGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            // Toggle groups can briefly have no selection if the user clicks
            // the active button — re-select it so we always have a filter.
            if (newT == null && oldT != null) {
                filterGroup.selectToggle(oldT);
                return;
            }
            renderQueue();
        });
        matchGraph.start();
        loadAll();
        loadMatches();
    }

    @FXML
    private void onRefresh() {
        loadAll();
        loadMatches();
    }

    @FXML
    private void onFilterChanged() {
        renderQueue();
    }

    // ── data ops ──────────────────────────────────────────────────────────────

    private void loadAll() {
        queueStatus.setText("Loading…");
        AsyncUi.run(
                () -> Services.ideaApi().list(0, 500),
                this::renderAll,
                err -> queueStatus.setText("Failed to load ideas — " + UiUtil.friendly(err))
        );
    }

    private void verifyIdea(Idea idea) {
        Alert confirm = new Alert(AlertType.CONFIRMATION,
                "Verify \"" + idea.title + "\"? It will become visible to investors.",
                ButtonType.CANCEL, ButtonType.OK);
        confirm.setHeaderText("Verify idea");
        Optional<ButtonType> choice = confirm.showAndWait();
        if (choice.isEmpty() || choice.get() != ButtonType.OK) return;

        queueStatus.setText("Verifying…");
        AsyncUi.run(
                () -> Services.ideaApi().verify(idea.id),
                ok -> { queueStatus.setText("Verified \"" + ok.title + "\"."); loadAll(); },
                err -> queueStatus.setText("Verify failed — " + UiUtil.friendly(err))
        );
    }

    private void loadMatches() {
        matchesStatus.setText("Loading match graph…");
        AsyncUi.run(
                () -> Services.dealApi().getMatches(),
                this::renderMatches,
                err -> matchesStatus.setText("Failed to load matches — " + UiUtil.friendly(err))
        );
    }

    private void renderMatches(List<Match> matches) {
        matchGraph.setMatches(matches == null ? List.of() : matches);
        long count = matches == null ? 0 : matches.size();
        matchesStatus.setText(count == 0
                ? "No matches yet — drag any node to rearrange the layout."
                : count + " match" + (count == 1 ? "" : "es") + " in the network · drag any node to rearrange.");
    }

    private void rejectIdea(Idea idea) {
        Optional<String> reason = RejectReasonDialog.showFor(idea.title);
        if (reason.isEmpty()) return;

        queueStatus.setText("Rejecting…");
        AsyncUi.run(
                () -> Services.ideaApi().reject(idea.id, reason.get()),
                ok -> { queueStatus.setText("Rejected \"" + ok.title + "\"."); loadAll(); },
                err -> queueStatus.setText("Reject failed — " + UiUtil.friendly(err))
        );
    }

    // ── rendering ─────────────────────────────────────────────────────────────

    private void renderAll(PagedResult<Idea> page) {
        allIdeas = page == null || page.items == null ? List.of() : page.items;
        renderDonut();
        renderQueue();
    }

    private void renderDonut() {
        Map<IdeaStatus, Long> counts = new EnumMap<>(IdeaStatus.class);
        for (IdeaStatus s : IdeaStatus.values()) counts.put(s, 0L);
        for (Idea idea : allIdeas) {
            IdeaStatus s = idea.status == null ? IdeaStatus.DRAFT : idea.status;
            counts.merge(s, 1L, Long::sum);
        }

        // Donut: feed slices in a stable order so colours don't jump as data changes.
        donut.setData(List.of(
                new DonutChart.Slice("DRAFT",     counts.get(IdeaStatus.DRAFT),     PALETTE.get(IdeaStatus.DRAFT)),
                new DonutChart.Slice("SUBMITTED", counts.get(IdeaStatus.SUBMITTED), PALETTE.get(IdeaStatus.SUBMITTED)),
                new DonutChart.Slice("VERIFIED",  counts.get(IdeaStatus.VERIFIED),  PALETTE.get(IdeaStatus.VERIFIED)),
                new DonutChart.Slice("REJECTED",  counts.get(IdeaStatus.REJECTED),  PALETTE.get(IdeaStatus.REJECTED))
        ));

        // Legend
        legendBox.getChildren().clear();
        for (IdeaStatus s : IdeaStatus.values()) {
            legendBox.getChildren().add(legendRow(s, counts.get(s)));
        }
    }

    private Node legendRow(IdeaStatus status, long count) {
        Circle dot = new Circle(6, PALETTE.get(status));
        Label name = new Label(status.name());
        name.getStyleClass().add("legend-name");
        Label num  = new Label(String.valueOf(count));
        num.getStyleClass().add("legend-count");
        HBox row = new HBox(10, dot, name, num);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void renderQueue() {
        queueList.getChildren().clear();
        IdeaStatus filter = currentFilter();
        List<Idea> visible = filter == null
                ? allIdeas
                : allIdeas.stream().filter(i -> i.status == filter).toList();

        if (visible.isEmpty()) {
            String label = filter == null ? "all" : filter.name();
            queueList.getChildren().add(emptyRow("No " + label.toLowerCase() + " ideas to show."));
        } else {
            for (Idea idea : visible) {
                queueList.getChildren().add(new AdminIdeaRow(idea, this::verifyIdea, this::rejectIdea));
            }
        }
        long pending = allIdeas.stream().filter(i -> i.status == IdeaStatus.DRAFT).count();
        queueStatus.setText(visible.size() + " of " + allIdeas.size() + " · " + pending + " awaiting review");
    }

    /** {@code null} for "All", otherwise the chosen {@link IdeaStatus}. */
    private IdeaStatus currentFilter() {
        Toggle t = filterGroup.getSelectedToggle();
        if (t == null) return null;
        Object data = t.getUserData();
        if (data == null || "ALL".equals(data)) return null;
        return IdeaStatus.fromString(data.toString());
    }

    private Node emptyRow(String message) {
        Label l = new Label(message);
        l.getStyleClass().add("muted-label");
        VBox box = new VBox(l);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("empty-state");
        return box;
    }
}
