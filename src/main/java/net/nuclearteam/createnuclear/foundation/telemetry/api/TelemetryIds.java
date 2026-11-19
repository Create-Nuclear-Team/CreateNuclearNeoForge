package net.nuclearteam.createnuclear.foundation.telemetry.api;

import java.util.regex.Pattern;

/**
 * Validation of telemetry ids (modules, sinks, categories, invariants, subject types).
 * <p>
 * Ids are used as file names, command arguments and config keys, so they are restricted to
 * lower-case letters, digits and {@code _}. An id never changes once published.
 */
public final class TelemetryIds {
    private static final Pattern VALID = Pattern.compile("[a-z0-9_]+");

    private TelemetryIds() {}

    /**
     * Checks an id against {@code [a-z0-9_]+}.
     *
     * @param id the id to check, may be {@code null}
     * @return {@code true} if the id is non-null and valid
     */
    public static boolean isValid(String id) {
        return id != null && VALID.matcher(id).matches();
    }

    /**
     * Returns the id unchanged if it is valid, otherwise fails.
     *
     * @param id   the id to check
     * @param what what the id names, used in the error message (e.g. {@code "module"})
     * @return {@code id}
     * @throws IllegalArgumentException if {@code id} is {@code null} or does not match {@code [a-z0-9_]+}
     */
    public static String require(String id, String what) {
        if (!isValid(id))
            throw new IllegalArgumentException("Invalid telemetry " + what + " id '" + id + "': expected [a-z0-9_]+");
        return id;
    }
}
