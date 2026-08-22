package com.loyalty.capstone;

import java.util.ArrayList;
import java.util.List;

/** Tiny runner so the G6 suite executes with a plain JDK and no third-party library. */
public final class TestRunner {

    private final List<String> failures = new ArrayList<>();
    private int passed;

    public void check(String coverageId, String name, Runnable body) {
        try {
            body.run();
            passed++;
            System.out.println("PASS  " + coverageId + "  " + name);
        } catch (AssertionError | RuntimeException failure) {
            failures.add(coverageId + " " + name + " :: " + failure);
            System.out.println("FAIL  " + coverageId + "  " + name + "  :: " + failure);
        }
    }

    public int report() {
        System.out.println();
        System.out.println("passed=" + passed + " failed=" + failures.size());
        for (String failure : failures) System.out.println("  " + failure);
        return failures.isEmpty() ? 0 : 1;
    }

    public static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }

    public static void assertThrows(Class<? extends Throwable> expected, Runnable body, String message) {
        try {
            body.run();
        } catch (RuntimeException | Error thrown) {
            if (expected.isInstance(thrown)) return;
            throw new AssertionError(message + " threw " + thrown.getClass().getName());
        }
        throw new AssertionError(message + " did not throw " + expected.getName());
    }
}
