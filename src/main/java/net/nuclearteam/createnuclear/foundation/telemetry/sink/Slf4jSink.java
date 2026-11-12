package net.nuclearteam.createnuclear.foundation.telemetry.sink;

import net.nuclearteam.createnuclear.foundation.telemetry.api.ITelemetrySink;
import net.nuclearteam.createnuclear.foundation.telemetry.api.RegisterTelemetrySink;
import net.nuclearteam.createnuclear.foundation.telemetry.api.SessionInfo;
import net.nuclearteam.createnuclear.foundation.telemetry.api.TelemetryEvent;
import net.nuclearteam.createnuclear.foundation.telemetry.api.TelemetryLevel;
import net.nuclearteam.createnuclear.foundation.telemetry.config.TelemetryConfigs;
import net.nuclearteam.createnuclear.foundation.telemetry.config.TelemetryCoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Built-in sink {@value #ID}: copies the events at or above {@code latest_log_level} to the game log,
 * through one SLF4J logger per module, named {@code CreateNuclear/Telemetry/<module>}.
 * <p>
 * Lines use {@link TelemetryFormat#line}, the same layout as the session file, so one {@code grep}
 * finds an event in both. The line is passed as an argument, never as the message pattern, so a
 * {@code {}} in it is written as is.
 * <p>
 * The threshold is read once, in {@link #open}: changing the config during a session has no effect
 * until the next one. Minecraft's log4j setup writes only {@code INFO} and above to {@code latest.log};
 * {@code DEBUG} and {@code TRACE} end up in {@code debug.log}.
 * <p>
 * Thread-safe: events may be accepted from any thread.
 */
@RegisterTelemetrySink
public final class Slf4jSink implements ITelemetrySink {
    /** Sink id, used for the {@code sink_log} config key. */
    public static final String ID = "log";
    /** Prefix of the logger names, {@code CreateNuclear/Telemetry/}; the module id is appended. */
    public static final String LOGGER_PREFIX = String.join("/", "CreateNuclear", "Telemetry", "");

    /** One logger per module id, created on its first event. */
    private final Map<String, Logger> loggers = new ConcurrentHashMap<>();
    /** Copy of {@code latest_log_level} taken in {@link #open}; {@code OFF} until then. */
    private volatile TelemetryLevel threshold = TelemetryLevel.OFF;

    /** Called by the registry, once per session. */
    public Slf4jSink() {}

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String description() {
        return "Copies the events at or above latest_log_level to the game log.";
    }

    /**
     * Reads {@code latest_log_level}. Never fails: if the config is not loaded, the sink stays open
     * and copies nothing.
     *
     * @param info the session information, unused
     */
    @Override
    public void open(SessionInfo info) {
        TelemetryCoreConfig core = TelemetryConfigs.core();
        threshold = core == null ? TelemetryLevel.OFF : core.latestLogLevel.get();
    }

    /**
     * Logs the event at its own level if the threshold allows it.
     *
     * @param event the cleaned event
     */
    @Override
    public void accept(TelemetryEvent event) {
        if (!threshold.allows(event.level())) return;
        Logger logger = loggers.computeIfAbsent(event.module(), m -> LoggerFactory.getLogger(LOGGER_PREFIX + m));
        String line = TelemetryFormat.line(event);
        switch (event.level()) {
            case ERROR -> logger.error("{}", line);
            case WARN -> logger.warn("{}", line);
            case INFO -> logger.info("{}", line);
            case DEBUG -> logger.debug("{}", line);
            case TRACE -> logger.trace("{}", line);
            case OFF -> { }
        }
    }

    /**
     * Forgets the cached loggers. Nothing else to release: the loggers belong to SLF4J.
     *
     * @param info the session information, unused
     */
    @Override
    public void close(SessionInfo info) {
        loggers.clear();
    }
}
