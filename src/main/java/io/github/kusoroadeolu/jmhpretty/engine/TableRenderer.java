package io.github.kusoroadeolu.jmhpretty.engine;

import io.github.kusoroadeolu.clique.Clique;
import io.github.kusoroadeolu.clique.components.Table;
import io.github.kusoroadeolu.clique.configuration.FrameAlign;
import io.github.kusoroadeolu.clique.configuration.TableType;
import io.github.kusoroadeolu.jmhpretty.engine.ColorEngine.Result;
import io.github.kusoroadeolu.jmhpretty.engine.ColorEngine.Verdict;
import io.github.kusoroadeolu.jmhpretty.model.*;

import java.util.*;


public record TableRenderer(boolean verbose, RenderTheme renderTheme) {



    private static final TableType DEFAULT_TABLE_TYPE = TableType.COMPACT;
    private static final String N_A = dim("NaN");
    private static final String AGGREGATE_ROLE_LABEL = "aggregate";



    public void render(ParsedRun run) {

        Clique.parser().print("%s = %s, %s = %s".formatted(renderTheme.best().on("green"),
                renderTheme.legend().on("fastest/best"),
                renderTheme.worst().on("red"),
                renderTheme.legend().on("slowest/worst")
        ));

        System.out.println();

        for (BenchmarkGroup group : run.groups()) {
            if (isGroupBenchmark(group)) renderGroupBenchmark(group);
            else renderNormalGroup(group);

            System.out.println();
        }
    }

    private boolean isGroupBenchmark(BenchmarkGroup group) {
        return group.variants().stream().anyMatch(BenchmarkVariantResult::isGroupBenchmark);
    }

    // Normal benchmarks (no @Group)
    private void renderNormalGroup(BenchmarkGroup group) {
        List<BenchmarkVariantResult> variants = group.variants();
        Mode mode = variants.getFirst().mode();
        boolean showPercentiles = mode.showsPercentiles();

        List<String> headers = initHeaders(group.paramKeys(), showPercentiles, false);

        // relative coloring pass over scores (mode-aware), only meaningful with >= 2 variants
        List<Double> scores = variants.stream().map(BenchmarkVariantResult::score).toList();
        Result relative = ColorEngine.relativeVerdicts(scores, mode);

        Table table = Clique.table(DEFAULT_TABLE_TYPE)
                .headers(headers);

        for (int i = 0; i < variants.size(); i++) {
            BenchmarkVariantResult v = variants.get(i);
            List<String> row = new ArrayList<>();

            for (String key : group.paramKeys()) {
                row.add(v.params().getOrDefault(key, ""));
            }

            Verdict scoreVerdict = relative.verdictFor(i);
            row.add(formatCell(v.score(), scoreVerdict));
            row.add(v.error() == null ? N_A : dim("± ") + formatTo3dp(v.error()));

            if (showPercentiles) {
                PercentileSet p = v.percentiles();
                double[] array = verbose ? p.toVerboseArray() : p.toArray();
                 for (double value : array){
                    Verdict verdict = relativeVerdict(value, v.scoreUnit(), relative, scores);
                    row.add(formatCell(value, verdict));
                }
            }

            row.add(dim(v.scoreUnit().raw()));
            table.row(row);
        }

        renderFrame(group.benchmarkName(), table);
    }

    private void renderGroupBenchmark(BenchmarkGroup group) {
        Mode mode = group.variants().getFirst().mode();
        boolean showPercentiles = mode.showsPercentiles();
        List<String> headers = initHeaders(group.paramKeys(), showPercentiles, true);
        Table table = Clique.table(DEFAULT_TABLE_TYPE)
                .headers(headers);

        Map<String, List<Double>> scoresByRole = groupScoresByRole(mode, group);

        Map<String, Result> relativeResults = new HashMap<>();
        for (Map.Entry<String, List<Double>> entry : scoresByRole.entrySet()) {
            Result r = ColorEngine.relativeVerdicts(entry.getValue(), mode);
            relativeResults.put(entry.getKey(), r);
        }


        for (BenchmarkVariantResult variant : group.variants()) {
            List<String> paramCells = new ArrayList<>();
            for (String key : group.paramKeys()) {
                paramCells.add(variant.params().getOrDefault(key, ""));
            }

            // Relative coloring scoped to this param combo's roles only.
            List<GroupRole> roles = variant.roles();

            for (int i = 0; i < roles.size(); i++) {
                GroupRole role = roles.get(i);
                List<Double> roleScores = scoresByRole.get(role.name());
                Result relative = relativeResults.get(role.name());
                List<String> row = new ArrayList<>(paramCells);
                row.add(role.name());

                row.add(formatCell(role.score(), relativeVerdict(role.score(), role.scoreUnit(), relative, roleScores)));
                row.add(role.error() == null ? N_A : dim("± ") + formatTo3dp(role.error()));

                if (showPercentiles) appendPercentileCells(row, role.percentiles(), role.scoreUnit(), relative, roleScores);

                row.add(role.scoreUnit().raw());
                table.row(row);
            }

            // Aggregate row: always last in its combo's block, never colored, though made dimmed and italicized to distinguish it from other rows.
            List<String> aggRow = new ArrayList<>(paramCells.stream().map(TableRenderer::italicize).toList());
            aggRow.add(italicize(AGGREGATE_ROLE_LABEL));
            aggRow.add(italicize(formatTo3dp(variant.score())));

            String errorString = variant.error() == null ? italicize(N_A) : italicize("± ") + italicize(formatTo3dp(variant.error()));
            aggRow.add(errorString);

            if (showPercentiles) appendPercentileCellsAggregate(aggRow, variant.percentiles());
            aggRow.add(italicize(roles.getFirst().scoreUnit().raw()));
            table.row(aggRow);
        }

        renderFrame(group.benchmarkName(), table);
    }

    private Map<String, List<Double>> groupScoresByRole(Mode mode, BenchmarkGroup group) {
        Map<String, List<Double>> scoresByRole = new HashMap<>();
        if (mode.higherIsBetter()) {
            for (BenchmarkVariantResult variant: group.variants()) {
                List<GroupRole> roles = variant.roles();
                for (GroupRole role : roles) {
                    String name = role.name();
                    var ls = scoresByRole.computeIfAbsent(name, (s) -> new ArrayList<>());
                    ls.add(role.score());
                }
            }
        }else {
            for (BenchmarkVariantResult variant: group.variants()) {
                List<GroupRole> roles = variant.roles();
                for (GroupRole role : roles) {
                    double[] scores = verbose ? role.percentiles().toVerboseArray() : role.percentiles().toArray();
                    String name = role.name();
                    var ls = scoresByRole.computeIfAbsent(name, (s) -> new ArrayList<>());
                    for (double d : scores) ls.add(d);
                }
            }
        }

        return scoresByRole;
    }

    private void appendPercentileCells(List<String> row, PercentileSet p, ScoreUnit unit ,Result result, List<Double> scores) {
        double[] array = verbose ? p.toVerboseArray() : p.toArray();
        for (double pv : array) row.add(formatCell(pv, relativeVerdict(pv, unit, result, scores)));
    }

    // Same as appendPercentileCells but with no absolute-threshold coloring. Used for aggregate rows.
    private void appendPercentileCellsAggregate(List<String> row, PercentileSet p) {
        double[] array = verbose ? p.toVerboseArray() : p.toArray();
        for (double pv : array) row.add(italicize(formatTo3dp(pv)));
    }


    void renderFrame(String title, Table table) {
        Clique.frame()
                .title(renderTheme.title().on(title))
                .nest(table, FrameAlign.LEFT)
                .render();
    }


    // Formatting helpers
    String formatTo3dp(double rawValue) {
        return String.format(Locale.ROOT, "%.3f", rawValue);
    }

    String formatCell(double rawValue, Verdict verdict) {
        return ColorEngine.applyMarkup(formatTo3dp(rawValue), verdict, renderTheme);
    }

    Verdict relativeVerdict(double rawValue, ScoreUnit unit, Result result, List<Double> scores) {
        return ColorEngine.relativeVerdict(rawValue, unit, result, scores);
    }

    List<String> initHeaders(List<String> paramKeys, boolean showPercentiles, boolean isGroup) {
        List<String> headers = new ArrayList<>(applyHeader(paramKeys));
        if (isGroup) headers.add(renderTheme.header().on("Role"));
        headers.add(renderTheme.header().on("Score"));
        headers.add(renderTheme.header().on("Error"));

        if (showPercentiles) {
            List<String> percentileList;
            if (verbose) percentileList = List.of("p00", "p50", "p90", "p95", "p99", "p99.9", "p99.99", "p99.999", "p99.9999", "max");
            else percentileList = List.of("p50", "p99", "max");

            List<String> colored = applyHeader(percentileList);
            headers.addAll(colored);
        }
        headers.add(renderTheme.header().on("Unit"));
        return headers;
    }

    List<String> applyHeader(List<String> list) {
       return list.stream().map(s -> renderTheme.header().on(Character.toString(s.charAt(0)).toUpperCase(Locale.ROOT) + s.substring(1))).toList();
    }

    static String dim(String s) {
        return RenderTheme.DIM.on(s);
    }

    static String italicize(String s) {
        return RenderTheme.DIM.italic().on(s);
    }

}
