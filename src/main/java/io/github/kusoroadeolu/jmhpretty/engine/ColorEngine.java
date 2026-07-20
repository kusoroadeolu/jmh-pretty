package io.github.kusoroadeolu.jmhpretty.engine;

import io.github.kusoroadeolu.jmhpretty.model.Mode;
import io.github.kusoroadeolu.jmhpretty.model.RenderTheme;
import io.github.kusoroadeolu.jmhpretty.model.ScoreUnit;

import java.io.File;
import java.util.List;


public final class ColorEngine {

    private static final double BEST_TOLERANCE_RATIO = 0.1;
    private static final double WORST_TOLERANCE_RATIO = 0.5;

    private ColorEngine() {}

    public enum Verdict { BEST, WORST, NEUTRAL, RELATIVE_BEST, RELATIVE_WORST }

    public static Result relativeVerdicts(List<Double> scores, Mode mode) {
        if (scores.size() < 2) return Result.NONE;

        boolean higherIsBetter = mode.higherIsBetter();

        int bestIdx = 0, worstIdx = 0;
        for (int i = 1; i < scores.size(); i++) {
            double v = scores.get(i);
            double bestV = scores.get(bestIdx);
            double worstV = scores.get(worstIdx);
            if ((higherIsBetter ? v > bestV : v < bestV) && v > 0) bestIdx = i;
            if ((higherIsBetter ? v < worstV : v > worstV) && v > 0) worstIdx = i;
        }

        if (bestIdx == worstIdx) return Result.NONE;

        return new Result(bestIdx, worstIdx);
    }

    public record Result(int bestIndex, int worstIndex) {

        public static final Result NONE = new Result(-1, -1);

        boolean isNone() {
            return this == Result.NONE;
        }

        public Verdict verdictFor(int index) {
            if (index == bestIndex) return Verdict.BEST;
            if (index == worstIndex) return Verdict.WORST;
            return Verdict.NEUTRAL;
        }
    }

    /** Wraps already-formatted cell text in Clique markup per verdict. Neutral is untouched. */
    public static String applyMarkup(String cellText, Verdict verdict, RenderTheme renderTheme) {
        return switch (verdict) {
            case BEST -> renderTheme.best().on(cellText);
            case WORST -> renderTheme.worst().on(cellText);
            case RELATIVE_WORST -> renderTheme.relativeWorst().on(cellText);
            case RELATIVE_BEST -> renderTheme.relativeBest().on(cellText);
            case NEUTRAL -> cellText;
        };
    }


    public static Verdict relativeVerdict(double rawValue, ScoreUnit unit, Result result, List<Double> scores) {
        if (result.isNone()) return Verdict.NEUTRAL;

        if (rawValue <= 0) return Verdict.NEUTRAL;

        double bestScore = scores.get(result.bestIndex());
        double worstScore = scores.get(result.worstIndex());
        boolean higherIsBetter = unit.kind() == ScoreUnit.Kind.THROUGHPUT;

        //Normalize direction
        double bestRange = higherIsBetter
                ? bestScore - (bestScore * BEST_TOLERANCE_RATIO)
                : bestScore + (bestScore * BEST_TOLERANCE_RATIO);

        double worstRange = higherIsBetter
                ? worstScore + (worstScore * WORST_TOLERANCE_RATIO)
                : worstScore - (worstScore * WORST_TOLERANCE_RATIO);

        // Guard against the tolerance bands crossing when best/worst are close together.
        double mid = (bestScore + worstScore) / 2;
        if (higherIsBetter) if (bestRange < worstRange) { bestRange = mid; worstRange = mid; }
        else if (bestRange > worstRange) { bestRange = mid; worstRange = mid; }

        boolean inBestZone = higherIsBetter ? rawValue >= bestRange : rawValue <= bestRange;
        boolean inWorstZone = higherIsBetter ? rawValue <= worstRange : rawValue >= worstRange;

        if (inBestZone) return Verdict.RELATIVE_BEST;
        else if (inWorstZone) return Verdict.RELATIVE_WORST;
        else return Verdict.NEUTRAL;
    }


}