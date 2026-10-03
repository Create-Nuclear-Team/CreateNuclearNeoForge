package net.nuclearteam.createnuclear.content.redstone.displayLink.source;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import net.minecraft.ChatFormatting;
import net.nuclearteam.createnuclear.content.multiblock.IHeat;
import net.nuclearteam.createnuclear.content.multiblock.controller.ReactorControllerBlockEntity;

public class HeatDisplaySource extends AbstractReactorStatDisplaySource {

    @Override
    protected String getLabelKey() {
        return "display_source.reactor.heat";
    }

    @Override
    protected int getMax() {
        return ReactorDisplayConstants.MAX_HEAT;
    }

    @Override
    protected ChatFormatting getColor(int value, ReactorControllerBlockEntity controller) {
        return IHeat.HeatLevel.of(value, controller.getMultiblockSize()).getTextColor();
    }

    /**
     * Assumed divergence vs Forge: Forge reads
     * {@code controller.getConfiguredPattern().getOrCreateTag().getDouble("heat")}.
     * In 1.21 heat lives in the {@code CNDataComponents.HEAT} data component;
     * re-reading the stack's NBT tag returns a defensive copy, so the value is
     * always absent there. We go through the controller's dedicated accessor instead.
     */
    @Override
    protected int computeValue(ReactorControllerBlockEntity controller, DisplayLinkContext context) {
        return controller.getConfiguredPatternHeat();
    }

    @Override
    protected String getTranslationKey() {
        return "heat";
    }
}