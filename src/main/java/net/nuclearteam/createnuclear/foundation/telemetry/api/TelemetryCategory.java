package net.nuclearteam.createnuclear.foundation.telemetry.api;

/**
 * A kind of data inside a module, such as {@code reactor/output} or {@code reactor/fuel}.
 * <p>
 * Usually implemented by an enum. Each constant gets a {@code level_<id>} line in the module's
 * config, and its own level. The module is resolved from the instance by
 * {@link net.nuclearteam.createnuclear.foundation.telemetry.TelemetryRegistry#ownerOf(TelemetryCategory) TelemetryRegistry.ownerOf},
 * so the same instance must be returned by {@link ITelemetryModule#categories()}.
 */
public interface TelemetryCategory {
    /**
     * Returns the category id.
     *
     * @return a snake_case id, unique within the module; never changes once published
     */
    String id();

    /**
     * Returns the level written in the config when it is first generated.
     *
     * @return the default threshold of this category
     */
    TelemetryLevel defaultLevel();

    /**
     * Returns a short description, used as the config comment.
     *
     * @return a one-line, human-readable description
     */
    String description();
}
