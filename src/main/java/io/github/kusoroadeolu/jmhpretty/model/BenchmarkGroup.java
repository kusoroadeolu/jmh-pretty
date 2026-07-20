package io.github.kusoroadeolu.jmhpretty.model;

import java.util.List;

public record BenchmarkGroup(
        String benchmarkName,
        List<String> paramKeys,             // union across variants, first-encountered order
        List<BenchmarkVariantResult> variants
) {}
