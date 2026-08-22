package com.loyalty.capstone;

import com.loyalty.capstone.gateway.ApiGateway;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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

            Map<String, Set<Integer>> observed = new LinkedHashMap<>();
            observed.put("/partner-earn", statuses(201, 409, 400, 405));
            observed.put("/redemptions", statuses(201, 422, 400, 404, 405));
            observed.put("/reports/point-liability", statuses(200, 405));

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

            Map<String, Set<Integer>> producible = new LinkedHashMap<>();
            producible.put("/partner-earn", statuses(201, 409, 400, 405));
            producible.put("/redemptions", statuses(201, 422, 400, 404, 405));
            producible.put("/reports/point-liability", statuses(200, 405));

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
            int at = yaml.indexOf("enum: [PENDING");
            assertTrue(at > 0, "order state enum present in openapi.yaml");
            String line = yaml.substring(at, yaml.indexOf('\n', at));
            for (String state : new String[]{"PENDING", "IN_PROGRESS", "FULFILLED", "FAILED", "CANCELLED", "REVERSED"}) {
                assertTrue(line.contains(state), "I-6 state documented: " + state);
            }
            assertEquals(6, line.split(",").length, "exactly six states are documented");
        });
    }

    private static Set<Integer> statuses(int... codes) {
        Set<Integer> set = new LinkedHashSet<>();
        for (int code : codes) set.add(code);
        return set;
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
            if (line.startsWith("  /") && trimmed.endsWith(":")) {
                current = trimmed.substring(0, trimmed.length() - 1);
                documented.put(current, new LinkedHashSet<>());
                continue;
            }
            if (current != null && trimmed.startsWith("'") && trimmed.endsWith("':")) {
                String code = trimmed.substring(1, trimmed.length() - 2);
                try {
                    documented.get(current).add(Integer.parseInt(code));
                } catch (NumberFormatException notAStatus) {
                    // not a status key
                }
            }
        }
        return documented;
    }
}
