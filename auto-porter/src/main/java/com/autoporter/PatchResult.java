package com.autoporter;

import java.util.List;

/** Result of a single patching step. */
public record PatchResult(boolean success, String message, List<String> changes) {

    public static PatchResult success(String message, List<String> changes) {
        return new PatchResult(true, message, changes);
    }

    public static PatchResult failure(String message) {
        return new PatchResult(false, message, List.of());
    }

    public void print() {
        System.out.println("  [" + (success ? "OK" : "FAIL") + "] " + message);
        for (String c : changes) System.out.println("       • " + c);
    }
}
