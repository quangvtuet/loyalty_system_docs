package com.loyalty.capstone;

import com.loyalty.capstone.gateway.ApiGateway;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.loyalty.capstone.TestRunner.assertEquals;
import static com.loyalty.capstone.TestRunner.assertTrue;

/**
 * G4 drift guard. Reads openapi.yaml and checks it against what the runtime actually serves,
 * so a documented status that the code no longer returns fails the build instead of passing review.
 */
public final class OpenApiDriftTests {

    private OpenApiDriftTests() { }

    static void register(TestRunner runner) {

        runner.check("G4-D01", "openapi.yaml documents exactly the routes the gateway serves", () -> {
            Map<String, Set<Integer>> documented = readOpenApi();
            Set<String> documentedPaths = new LinkedHashSet<>(documented.keySet());
            Set<String> servedPaths = new LinkedHashSet<>(ApiGateway.routes());

            assertEquals(servedPaths, documentedPaths,
                    "documented paths must equal served paths, with no extra operation");
        });

        runner.check("G4-D02", "every status the runtime returned is documented for that path", () -> {
            Map<String, Set<Integer>> documented = readOpenApi();
            Map<String, Set<Integer>> observed = observeRuntimeStatuses();

            for (Map.Entry<String, Set<Integer>> entry : observed.entrySet()) {
                Set<Integer> documentedForPath = documented.get(entry.getKey());
                assertTrue(documentedForPath != null, "path documented: " + entry.getKey());
                for (Integer status : entry.getValue()) {
                    assertTrue(documentedForPath.contains(status),
                            "status " + status + " on " + entry.getKey() + " is documented");
                }
            }
        });

        runner.check("G4-D03", "openapi.yaml documents no status the runtime cannot produce", () -> {
            Map<String, Set<Integer>> documented = readOpenApi();
            Map<String, Set<Integer>> producible = observeRuntimeStatuses();

            for (Map.Entry<String, Set<Integer>> entry : documented.entrySet()) {
                Set<Integer> producibleForPath = producible.get(entry.getKey());
                assertTrue(producibleForPath != null, "documented path is served: " + entry.getKey());
                for (Integer status : entry.getValue()) {
                    assertTrue(producibleForPath.contains(status),
                            "documented status " + status + " on " + entry.getKey() + " is reachable");
                }
            }
        });

        runner.check("G4-D04", "the six I-6 states are the only ones in the documented order schema", () -> {
            String yaml = readRaw();
            int description = yaml.indexOf("description: The six I-6 states");
            assertTrue(description >= 0, "order state schema is identified in openapi.yaml");
            int at = yaml.indexOf("enum: [", description);
            assertTrue(at >= 0, "order state enum present in openapi.yaml");
            int lineEnd = yaml.indexOf('\n', at);
            assertTrue(lineEnd >= 0, "order state enum is on a complete line");
            String line = yaml.substring(at + "enum: [".length(), lineEnd).trim();
            assertEquals(Arrays.asList("PENDING", "IN_PROGRESS", "FULFILLED", "FAILED", "CANCELLED", "REVERSED"),
                    Arrays.asList(line.substring(0, line.length() - 1).split(", ")),
                    "exactly the six I-6 states are documented in order");
        });
    }

    /**
     * Exercise every documented response scenario against the gateway. The drift checks must
     * observe the runtime, not repeat a second hand-written copy of its status table.
     */
    private static Map<String, Set<Integer>> observeRuntimeStatuses() {
        Map<String, Set<Integer>> observed = new LinkedHashMap<>();
        HttpTestClient.withRuntime((base, clock, platform) -> {
            record(observed, "/partner-earn", HttpTestClient.post(base + "/partner-earn",
                    "{\"sourceTransactionId\":\"DRIFT-EARN\",\"memberId\":\"DRIFT-MEMBER\",\"amount\":200}"));
            record(observed, "/partner-earn", HttpTestClient.post(base + "/partner-earn",
                    "{\"sourceTransactionId\":\"DRIFT-EARN\",\"memberId\":\"DRIFT-MEMBER\",\"amount\":200}"));
            record(observed, "/partner-earn", HttpTestClient.post(base + "/partner-earn",
                    "{\"memberId\":\"DRIFT-MEMBER\"}"));
            record(observed, "/partner-earn", HttpTestClient.get(base + "/partner-earn"));

            record(observed, "/redemptions", HttpTestClient.post(base + "/redemptions",
                    "{\"memberId\":\"DRIFT-POOR\",\"rewardItemId\":\"RI-VOUCHER-300\"}"));
            record(observed, "/redemptions", HttpTestClient.post(base + "/redemptions",
                    "{\"memberId\":\"DRIFT-MEMBER\",\"rewardItemId\":\"RI-VOUCHER-300\"}"));
            record(observed, "/redemptions", HttpTestClient.post(base + "/redemptions",
                    "{\"memberId\":\"DRIFT-MEMBER\"}"));
            record(observed, "/redemptions", HttpTestClient.post(base + "/redemptions",
                    "{\"memberId\":\"DRIFT-MEMBER\",\"rewardItemId\":\"RI-UNKNOWN\"}"));
            record(observed, "/redemptions", HttpTestClient.get(base + "/redemptions"));

            record(observed, "/reports/point-liability", HttpTestClient.get(base + "/reports/point-liability"));
            record(observed, "/reports/point-liability",
                    HttpTestClient.post(base + "/reports/point-liability", "{}"));
        });
        return observed;
    }

    private static void record(Map<String, Set<Integer>> observed, String path,
                               HttpResponse<String> response) {
        observed.computeIfAbsent(path, ignored -> new LinkedHashSet<>()).add(response.statusCode());
    }

    private static String readRaw() {
        List<Path> candidates = new ArrayList<>();
        candidates.add(Paths.get("openapi.yaml"));
        candidates.add(Paths.get("capstone", "openapi.yaml"));
        candidates.add(Paths.get("..", "openapi.yaml"));
        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                try {
                    return new String(Files.readAllBytes(candidate), StandardCharsets.UTF_8);
                } catch (IOException unreadable) {
                    throw new AssertionError("openapi.yaml unreadable: " + unreadable);
                }
            }
        }
        throw new AssertionError("openapi.yaml not found; run the suite from the capstone directory");
    }

    /** Minimal scan: path keys under 'paths:' and the response status codes beneath each. */
    private static Map<String, Set<Integer>> readOpenApi() {
        Map<String, Set<Integer>> documented = new LinkedHashMap<>();
        String current = null;
        boolean inPaths = false;
        for (String rawLine : readRaw().split("\n")) {
            String line = rawLine.replace("\r", "");
            if (line.startsWith("paths:")) { inPaths = true; continue; }
            if (!inPaths) continue;
            if (line.startsWith("components:")) break;

            String trimmed = line.trim();
            if (line.startsWith("  /") && !line.startsWith("   ") && trimmed.endsWith(":")) {
                current = trimmed.substring(0, trimmed.length() - 1);
                if (documented.containsKey(current)) {
                    throw new AssertionError("duplicate OpenAPI path: " + current);
                }
                documented.put(current, new LinkedHashSet<>());
                continue;
            }
            if (current != null && line.startsWith("        ") && trimmed.endsWith(":")) {
                String code = trimmed.substring(0, trimmed.length() - 1);
                if (code.startsWith("'") && code.endsWith("'")) {
                    code = code.substring(1, code.length() - 1);
                }
                if (code.matches("[1-5][0-9]{2}")) {
                    documented.get(current).add(Integer.parseInt(code));
                }
            }
        }
        if (documented.isEmpty()) {
            throw new AssertionError("openapi.yaml contains no paths");
        }
        return documented;
    }
}
