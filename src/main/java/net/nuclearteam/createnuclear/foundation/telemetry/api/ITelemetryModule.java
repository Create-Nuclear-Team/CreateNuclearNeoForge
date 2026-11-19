package net.nuclearteam.createnuclear.foundation.telemetry.api;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;

/**
 * An observed domain, such as {@code reactor} or {@code environment}.
 * <p>
 * Annotate the implementation with {@link RegisterTelemetryModule} and give it a public no-arg
 * constructor. It then gets its own {@code createnuclear-telemetry-<id>.toml} config and appears in
 * {@code /cn telemetry}. Any exception thrown from a hook removes the module from the current
 * session; the game continues.
 * <p>
 * Part of the public contract used by addons: new methods must have a default implementation.
 */
public interface ITelemetryModule {
    /**
     * Returns the module id.
     *
     * @return a snake_case id, used for the config file name and command arguments;
     *         never changes once published
     */
    String id();

    /**
     * Returns a short description, shown by {@code /cn telemetry modules}.
     *
     * @return a one-line, human-readable description
     */
    String description();

    /**
     * Returns the module's own categories, always the same instances.
     * {@link BuiltinCategory#HEALTH health} and {@link BuiltinCategory#METRICS metrics} are added
     * automatically and must not be returned here.
     *
     * @return the module's categories, with unique ids
     */
    List<? extends TelemetryCategory> categories();

    /**
     * Declares module-specific options, appended to the module's config file.
     * Called once, while the config is built. Keep the returned suppliers in fields.
     *
     * @param options the builder to declare options with
     */
    default void defineOptions(Options options) {}

    /**
     * Called when a session recording this module starts, after the sinks are opened.
     *
     * @param context the session context
     */
    default void onSessionStart(TelemetryContext context) {}

    /**
     * Called at the end of every server tick while a session recording this module runs.
     * Must stay cheap: never iterate over the world from here.
     *
     * @param context the session context
     */
    default void onServerTick(TelemetryContext context) {}

    /**
     * Called when a session recording this module stops, before the sinks are closed.
     *
     * @param context the session context
     */
    default void onSessionStop(TelemetryContext context) {}

    /**
     * Builder for module-specific options.
     * <p>
     * Keys are snake_case. Each returned supplier reads the current config value, so it follows
     * config reloads.
     */
    interface Options {
        /**
         * Declares a boolean option.
         *
         * @param key          snake_case config key
         * @param defaultValue value written when the config is first generated
         * @param comment      config comment
         * @return a supplier reading the current value
         */
        BooleanSupplier bool(String key, boolean defaultValue, String comment);

        /**
         * Declares an integer option.
         *
         * @param key          snake_case config key
         * @param defaultValue value written when the config is first generated
         * @param min          minimum accepted value, inclusive
         * @param max          maximum accepted value, inclusive
         * @param comment      config comment
         * @return a supplier reading the current value
         */
        IntSupplier integer(String key, int defaultValue, int min, int max, String comment);

        /**
         * Declares a decimal option.
         *
         * @param key          snake_case config key
         * @param defaultValue value written when the config is first generated
         * @param min          minimum accepted value, inclusive
         * @param max          maximum accepted value, inclusive
         * @param comment      config comment
         * @return a supplier reading the current value
         */
        DoubleSupplier decimal(String key, double defaultValue, double min, double max, String comment);
    }
}
