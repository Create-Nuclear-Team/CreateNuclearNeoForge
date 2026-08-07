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
     * Divergence assumee vs Forge : Forge lit
     * {@code controller.getConfiguredPattern().getOrCreateTag().getDouble("heat")}.
     * En 1.21 la chaleur vit dans le data component {@code CNDataComponents.HEAT} ;
     * relire le tag NBT de la stack renvoie une copie defensive, donc la valeur y est
     * toujours absente. On passe par l'accesseur dedie du controleur.
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