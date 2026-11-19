package net.nuclearteam.createnuclear.foundation.telemetry.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/**
 * What an event is about: a reactor, an output, the server. <b>Never a player.</b>
 * <p>
 * Formatted as {@code type}, {@code type@dimension} or {@code type@dimension[x,y,z]}.
 * Being a record, it can be used as a map key (health tracking per subject).
 *
 * @param type      snake_case subject type, e.g. {@code reactor}, {@code reactor_output}, {@code server}
 * @param dimension dimension id, e.g. {@code minecraft:overworld}, or {@code null}
 * @param pos       block position, or {@code null}; always stored as an immutable copy
 */
public record Subject(String type, @Nullable String dimension, @Nullable BlockPos pos) {
    /** The server itself, for events not tied to a block. */
    public static final Subject SERVER = of("server");

    /**
     * Validates the type and copies the position.
     *
     * @throws IllegalArgumentException if {@code type} is not a valid id
     */
    public Subject {
        // Use the parameter: in a compact constructor, fields are only assigned at the end.
        TelemetryIds.require(type, "subject type");
        if (pos != null) pos = pos.immutable(); // a MutableBlockPos would break equals/hashCode
    }

    /**
     * Creates a subject with no location.
     *
     * @param type snake_case subject type
     * @return a subject formatted as {@code type}
     */
    public static Subject of(String type) {
        return new Subject(type, null, null);
    }

    /**
     * Creates a subject located at a block.
     *
     * @param type  snake_case subject type
     * @param level the level holding the block; only its dimension id is kept
     * @param pos   the block position
     * @return a subject formatted as {@code type@dimension[x,y,z]}
     */
    public static Subject atBlock(String type, Level level, BlockPos pos) {
        return new Subject(type, level.dimension().location().toString(), pos);
    }

    @Override
    public @NotNull String toString() {
        if (pos == null) return dimension == null ? type : type + "@" + dimension;
        return type + "@" + dimension + "[" + pos.getX() + "," + pos.getY() + "," + pos.getZ() + "]";
    }
}
