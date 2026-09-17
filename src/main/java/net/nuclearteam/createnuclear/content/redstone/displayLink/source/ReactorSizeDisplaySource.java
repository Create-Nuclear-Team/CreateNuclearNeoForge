package net.nuclearteam.createnuclear.content.redstone.displayLink.source;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.NumericSingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.nuclearteam.createnuclear.content.multiblock.MultiblockHelpers;
import net.nuclearteam.createnuclear.content.multiblock.controller.ReactorControllerBlockEntity;
import net.nuclearteam.createnuclear.foundation.utility.CreateNuclearLang;

public class ReactorSizeDisplaySource extends AbstractReactorStatDisplaySource {

    @Override
    protected String getLabelKey() {
        return "display_source.reactor.size";
    }

    @Override
    protected int getMax() {
        return 3;
    }

    @Override
    protected int getGaugeWidth() {
        return 3;
    }

    @Override
    protected ChatFormatting getColor(int value, ReactorControllerBlockEntity controller) {
        return ChatFormatting.BLUE;
    }

    @Override
    protected int computeValue(ReactorControllerBlockEntity controller, DisplayLinkContext context) {
        return ReactorDisplayConstants.sizeTier(controller.getMultiblockSize());
    }

    @Override
    protected MutableComponent getDefaultDisplay(int value, ReactorControllerBlockEntity controller) {
        return CreateNuclearLang.translateDirect(String.join(".", getLabelKey(), ReactorDisplayConstants.sizeTierKey(value)));
    }

    @Override protected String getTranslationKey() { return "size"; }

}
