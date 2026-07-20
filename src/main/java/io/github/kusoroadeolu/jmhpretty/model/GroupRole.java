package io.github.kusoroadeolu.jmhpretty.model;

/**
 * One entry from a @Group benchmark's secondaryMetrics — a per-role result
 * (e.g. "deleteMin_4_4", "readerEmpty"), shaped identically to primaryMetric.
 */
public record GroupRole(
        String name,
        double score,
        Double error,
        ScoreUnit scoreUnit,
        PercentileSet percentiles
) {}
