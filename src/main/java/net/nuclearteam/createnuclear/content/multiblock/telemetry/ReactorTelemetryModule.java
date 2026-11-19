package net.nuclearteam.createnuclear.content.multiblock.telemetry;

import net.nuclearteam.createnuclear.foundation.telemetry.api.ITelemetryModule;
import net.nuclearteam.createnuclear.foundation.telemetry.api.RegisterTelemetryModule;
import net.nuclearteam.createnuclear.foundation.telemetry.api.TelemetryCategory;

import java.util.List;
import java.util.function.IntSupplier;

/**
 * Telemetry module {@value #ID}: nuclear reactors. Events and checks come from the reactor's own
 * ticks through {@link ReactorTelemetry}; the module itself has no tick hook, so it never iterates
 * over the world. It only observes: no option changes the gameplay.
 * <p>
 * Option, read by the orphan output detection: {@code orphan_grace_ticks}.
 */
@RegisterTelemetryModule
public final class ReactorTelemetryModule implements ITelemetryModule {
    /** Module id, used for {@code createnuclear-telemetry-reactor.toml} and commands. */
    public static final String ID = "reactor";

    // Static: the registry creates a single instance, and the reactor code has no reference to it.
    /** Option {@code orphan_grace_ticks}; replaced in {@link #defineOptions}. */
    private static volatile IntSupplier orphanGraceTicks = () -> 100;

    /** Called by the registry, once per game. */
    public ReactorTelemetryModule() {}

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String description() {
        return "Nuclear reactors: assembly, state, outputs, fuel, heat and invariants.";
    }

    @Override
    public List<? extends TelemetryCategory> categories() {
        return List.of(ReactorCategory.values());
    }

    /** Declares {@code orphan_grace_ticks}. */
    @Override
    public void defineOptions(Options options) {
        orphanGraceTicks = options.integer("orphan_grace_ticks", 100, 0, 6000,
                "Ticks after an output is loaded during which a missing controller is not yet reported"
                        + " as orphan_output_spinning (20 ticks = 1 second).");
    }

    /**
     * Returns the grace delay of the orphan output detection.
     *
     * @return the current value of {@code orphan_grace_ticks}
     */
    public static int orphanGraceTicks() {
        return orphanGraceTicks.getAsInt();
    }
}
