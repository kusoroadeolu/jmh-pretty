package io.github.kusoroadeolu.jmhpretty.mapper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.kusoroadeolu.jmhpretty.model.*;

import java.io.IOException;
import java.io.Reader;
import java.util.*;

/**
 * Parses JMH's `-rf json` output into the lean internal model.
 * <p>
 * Uses Gson's tree API (JsonObject/JsonArray) rather than fixed-shape POJOs,
 * since JMH JSON has some optional/nullable fields (error, params, secondaryMetrics)
 * that are easier to handle defensively as a tree.
 * <p>
 * Deliberately does NOT parse: RunMetadata fields (jvm, jdkVersion, vmVersion,
 * jmhVersion, warmup/measurement iteration counts, jvmArgs), rawData, rawDataHistogram,
 * scoreConfidence. secondaryMetrics IS parsed, into GroupRole, for @Group benchmarks.
 */
public final class RawResultMapper {

    private static final String MODE = "mode";
    private static final String BENCHMARK = "benchmark";
    private static final String PARAMS = "params";
    private static final String PRIMARY_METRIC = "primaryMetric";
    private static final String SCORE = "score";
    private static final String SCORE_ERROR = "scoreError";
    private static final String SCORE_UNIT = "scoreUnit";
    private static final String SCORE_PERCENTILES = "scorePercentiles";
    private static final String SECONDARY_METRICS = "secondaryMetrics";

    private RawResultMapper() {}

    public static ParsedRun parse(Reader jsonReader) throws IOException {
        JsonElement root = JsonParser.parseReader(jsonReader);
        if (!root.isJsonArray()) {
            throw new IllegalArgumentException("Expected top-level JSON array from JMH -rf json output.");
        }
        JsonArray entries = root.getAsJsonArray();

        // benchmarkName -> ordered params keys (first-encountered) + variants
        SequencedMap<String, List<String>> paramKeysByBenchmark = new LinkedHashMap<>();
        SequencedMap<String, List<BenchmarkVariantResult>> variantsByBenchmark = new LinkedHashMap<>();

        for (JsonElement elem : entries) {
            JsonObject obj = elem.getAsJsonObject();
            String benchmarkName = requiredString(obj, BENCHMARK);
            Mode mode = Mode.fromJmhCode(requiredString(obj, MODE));

            Map<String, String> params = parseParams(obj.getAsJsonObject(PARAMS));
            paramKeysByBenchmark.computeIfAbsent(benchmarkName, k -> new ArrayList<>());

            for (String key : params.keySet()) {
                List<String> existing = paramKeysByBenchmark.get(benchmarkName);
                if (!existing.contains(key)) {
                    existing.add(key);
                }
            }

            JsonObject primary = obj.getAsJsonObject(PRIMARY_METRIC);

            if (primary == null) throw new IllegalArgumentException("Missing primaryMetric for benchmark: " + benchmarkName);


            double score = requiredDouble(primary, SCORE);
            Double error = optionalDouble(primary, SCORE_ERROR);
            ScoreUnit scoreUnit = ScoreUnit.parse(requiredString(primary, SCORE_UNIT));
            PercentileSet percentiles = parsePercentiles(primary.getAsJsonObject(SCORE_PERCENTILES), score);

            List<GroupRole> roles = parseSecondaryMetrics(obj.getAsJsonObject(SECONDARY_METRICS));

            BenchmarkVariantResult variant = new BenchmarkVariantResult(
                    params, mode, score, error, scoreUnit, percentiles, roles
            );

            variantsByBenchmark.computeIfAbsent(benchmarkName, k -> new ArrayList<>())
                    .add(variant);
        }

        List<BenchmarkGroup> groups = new ArrayList<>();
        for (String benchmarkName : variantsByBenchmark.keySet()) {
            groups.add(new BenchmarkGroup(
                    benchmarkName,
                    paramKeysByBenchmark.get(benchmarkName),
                    variantsByBenchmark.get(benchmarkName)
            ));
        }
        return new ParsedRun(groups);
    }

    private static Map<String, String> parseParams(JsonObject paramsObj) {
        if (paramsObj == null) return Map.of();

        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : paramsObj.entrySet()) {
            out.put(e.getKey(), e.getValue().getAsString());
        }
        return out;
    }

    private static boolean isRedundantPercentileDump(String key) {
        if (key.contains(":p")) return true;
        return key.matches("p\\d+\\.\\d+");
    }

    private static List<GroupRole> parseSecondaryMetrics(JsonObject secondaryObj) {
        if (secondaryObj == null || secondaryObj.entrySet().isEmpty()) {
            return List.of();
        }
        List<GroupRole> roles = new ArrayList<>();
        for (Map.Entry<String, JsonElement> e : secondaryObj.entrySet()) {
            String roleName = e.getKey();
            if (isRedundantPercentileDump(roleName)) continue;

            JsonObject roleObj = e.getValue().getAsJsonObject();
            double score = requiredDouble(roleObj, SCORE);
            Double error = optionalDouble(roleObj, SCORE_ERROR);
            ScoreUnit scoreUnit = ScoreUnit.parse(requiredString(roleObj, SCORE_UNIT));
            PercentileSet percentiles = parsePercentiles(roleObj.getAsJsonObject(SCORE_PERCENTILES), score);
            roles.add(new GroupRole(roleName, score, error, scoreUnit, percentiles));
        }
        return roles;
    }

    private static PercentileSet parsePercentiles(JsonObject percentilesObj, double fallbackScore) {
        if (percentilesObj == null || percentilesObj.entrySet().isEmpty()) return PercentileSet.degenerate(fallbackScore);

        return new PercentileSet(
                percentileOrFallback(percentilesObj, "0.0", fallbackScore),
                percentileOrFallback(percentilesObj, "50.0", fallbackScore),
                percentileOrFallback(percentilesObj, "90.0", fallbackScore),
                percentileOrFallback(percentilesObj, "95.0", fallbackScore),
                percentileOrFallback(percentilesObj, "99.0", fallbackScore),
                percentileOrFallback(percentilesObj, "99.9", fallbackScore),
                percentileOrFallback(percentilesObj, "99.99", fallbackScore),
                percentileOrFallback(percentilesObj, "99.999", fallbackScore),
                percentileOrFallback(percentilesObj, "99.9999", fallbackScore),
                percentileOrFallback(percentilesObj, "100.0", fallbackScore)
        );
    }

    private static double percentileOrFallback(JsonObject obj, String key, double fallback) {
        JsonElement el = obj.get(key);
        return el == null || el.isJsonNull() ? fallback : el.getAsDouble();
    }

    private static String requiredString(JsonObject obj, String field) {
        JsonElement el = obj.get(field);
        if (el == null || el.isJsonNull()) throw new IllegalArgumentException("Missing required field '" + field + "' in JMH JSON entry.");
        return el.getAsString();
    }

    private static double requiredDouble(JsonObject obj, String field) {
        JsonElement el = obj.get(field);
        if (el == null || el.isJsonNull()) throw new IllegalArgumentException("Missing required field '" + field + "' in JMH JSON entry.");
        return el.getAsDouble();
    }

    private static Double optionalDouble(JsonObject obj, String field) {
        JsonElement el = obj.get(field);
        if (el == null || el.isJsonNull()) return null;

        String s = el.getAsString();
        if ("NaN".equalsIgnoreCase(s)) return null;

        try {
            return el.getAsDouble();
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
