package net.nuclearteam.createnuclear.api.multiblock;

import java.util.ArrayList;
import java.util.List;

/**
 * Accumulates the names of unset required fields for a fluent builder and
 * throws a single {@link IllegalStateException} naming every one of them.
 * Shared by the mod's {@code Builder} classes (e.g.
 * {@link net.nuclearteam.createnuclear.api.multiblock.rods.RodType.Builder},
 * {@link net.nuclearteam.createnuclear.api.multiblock.fluid.ReactorFluidType.Builder})
 * so each only has to supply its own field presence checks.
 */
public final class RequiredFieldsValidator {
    private final List<String> missing = new ArrayList<>();

    public RequiredFieldsValidator require(boolean isPresent, String fieldName) {
        if (!isPresent) missing.add(fieldName);
        return this;
    }

    /**
     * @throws IllegalStateException if any field was reported missing via {@link #require},
     *         naming every missing field in the exception message
     */
    public void validate(String typeName) {
        if (!missing.isEmpty())
            throw new IllegalStateException("Missing required " + typeName + " fields: " + String.join(", ", missing));
    }
}
