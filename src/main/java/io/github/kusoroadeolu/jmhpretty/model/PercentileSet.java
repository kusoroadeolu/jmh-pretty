package io.github.kusoroadeolu.jmhpretty.model;

public record PercentileSet(
        double p00, double p50, double p90, double p95,
        double p99, double p99_9, double p99_99, double p99_999,
        double p99_9999, double p100
) {
    public static PercentileSet degenerate(double score) {
        return new PercentileSet(score, score, score, score, score, score, score, score, score, score);
    }

    public double[] toVerboseArray() {
        return new double[]{
                p00(), p50(), p90(), p95(), p99(),
                p99_9(), p99_99(), p99_999(), p99_9999(), p100()
        };
    }

    public double[] toDefaultArray() {
        return new double[]{p99(), p99_9() ,p99_99(), p100()};
    }

}
