package io.github.kusoroadeolu.jmhpretty.engine;

import io.github.kusoroadeolu.clique.Clique;
import io.github.kusoroadeolu.clique.components.Table;
import io.github.kusoroadeolu.clique.configuration.FrameAlign;
import io.github.kusoroadeolu.clique.configuration.TableType;
import io.github.kusoroadeolu.clique.style.Ink;
import io.github.kusoroadeolu.jmhpretty.engine.ColorEngine.Result;
import io.github.kusoroadeolu.jmhpretty.engine.ColorEngine.Verdict;
import io.github.kusoroadeolu.jmhpretty.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public record TableRenderer(boolean verbose, RenderTheme renderTheme) {



    private static final TableType DEFAULT_TABLE_TYPE = TableType.COMPACT;
    private static final String N_A = dim("n/a");



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
                if (verbose) for (double value : p.toVerboseArray()) {
                    Verdict verdict = relativeVerdict(value, v.scoreUnit(), relative, scores);
                    row.add(formatCell(value, verdict));
                }
                else for (double value : p.toArray()){
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
        for (BenchmarkVariantResult variant : group.variants()) {
            Mode mode = variant.mode();
            boolean showPercentiles = mode.showsPercentiles();
            List<GroupRole> roles = variant.roles();

            List<String> headers = initHeaders(List.of(), showPercentiles, true);

            List<Double> roleScores = roles.stream().map(GroupRole::score).toList();
            Result relative = ColorEngine.relativeVerdicts(roleScores, mode);

            Table table = Clique.table(DEFAULT_TABLE_TYPE)
                    .headers(headers);

            for (int i = 0; i < roles.size(); i++) {
                GroupRole role = roles.get(i);
                List<String> row = new ArrayList<>();
                row.add(role.name());

                Verdict verdict = relative.verdictFor(i);
                row.add(formatCell(role.score(), verdict));
                row.add(role.error() == null ? N_A : dim("± ") + formatTo3dp(role.error()));

                if (showPercentiles) {
                    PercentileSet p = role.percentiles();
                    if (verbose) for (double value : p.toVerboseArray()) {
                        Verdict lv = relativeVerdict(value, role.scoreUnit(), relative, roleScores);
                        row.add(formatCell(value, lv));
                    }
                    else for (double value : p.toArray()){
                        Verdict lv = relativeVerdict(value, role.scoreUnit(), relative, roleScores);
                        row.add(formatCell(value, lv));
                    }
                }

                row.add(dim(role.scoreUnit().raw()));
                table.row(row);
            }

            String title = group.benchmarkName();
            if (!variant.params().isEmpty()) {
                StringBuilder sb = new StringBuilder(title)
                        .append(" (");

                boolean first = true;
                for (String key : group.paramKeys()) {
                    if (!first) sb.append(", ");
                    sb.append(key).append("=").append(variant.params().getOrDefault(key, ""));
                    first = false;
                }
                sb.append(")");
                title = sb.toString();
            }

            String aggregateLine = renderTheme.aggregate().on(
                    "aggregate: " + formatTo3dp(variant.score())
                            + (variant.error() == null ? "" : dim(" ± ") + renderTheme.aggregate().on(formatTo3dp(variant.error())))
            );

            renderFrame(title, table);

            Clique.parser().print(aggregateLine);
            System.out.println();
        }
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
            if (verbose) percentileList = List.of("p00", "p50", "p90", "p95", "p99", "p99.9", "p99.99", "p99.999", "p99.9999", "p100");
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


}
