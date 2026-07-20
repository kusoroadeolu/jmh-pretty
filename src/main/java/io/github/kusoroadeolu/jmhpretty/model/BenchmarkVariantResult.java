package io.github.kusoroadeolu.jmhpretty.model;

import java.util.List;
import java.util.Map;

public record BenchmarkVariantResult(
        Map<String, String> params,   // never null; {} when no @Param
        Mode mode,
        double score,
        Double error,                 // null on "NaN" or absent field
        ScoreUnit scoreUnit,
        PercentileSet percentiles,    // always present; degenerate for thrpt/avgt/ss
        List<GroupRole> roles         // empty for normal benchmarks
) {
    public boolean isGroupBenchmark() {
        return roles != null && !roles.isEmpty();
    }
}