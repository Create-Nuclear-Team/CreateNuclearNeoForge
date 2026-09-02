package net.nuclearteam.createnuclear.content.multiblock;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.lang.Lang;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.nuclearteam.createnuclear.CreateNuclear;
import net.nuclearteam.createnuclear.api.multiblock.rods.RodType.TypeRodPredicate;
import net.nuclearteam.createnuclear.foundation.utility.CreateNuclearLang;
import net.nuclearteam.createnuclear.infrastructure.config.CNConfigs;
import net.nuclearteam.createnuclear.infrastructure.config.CReactorHeat;

public interface IHeat extends IWrenchable {
    enum HeatLevel {
        NONE(ChatFormatting.DARK_GRAY),
        SAFETY(ChatFormatting.GREEN),
        CAUTION(ChatFormatting.YELLOW),
        WARNING(ChatFormatting.GOLD),
        DANGER(ChatFormatting.RED),
        ;

        private final ChatFormatting color;

        HeatLevel(ChatFormatting textColor) {
            this.color = textColor;
        }

        public ChatFormatting getTextColor() {
            return color;
        }

        public static HeatLevel of(int heat, int reactorSize) {
            if (heat < 0) return NONE;

            heat = Math.abs(heat);

            CReactorHeat config = CNConfigs.server().reactorHeat;
            int danger;

            switch (reactorSize) {
                case 7 -> danger = config.size7Danger.get();
                case 9 -> danger = config.size9Danger.get();
                default -> danger = config.size5Danger.get();
            }

            int caution = (int) (danger * 0.75);
            int warning = (int) (danger * 0.90);

            if (heat > 0 && heat < caution) return SAFETY;
            if (heat >= caution && heat < warning) return CAUTION;
            if (heat >= warning && heat <= danger) return WARNING;
            if (heat > danger) return DANGER;

            return NONE;
        }

        public static boolean isNotDanger(int heat, int reactorSize) {
            return of(heat, reactorSize) != DANGER;
        }

        public static LangBuilder getFormattedHeatText(int heat, int reactorSize) {
            HeatLevel heatLevel = of(heat, reactorSize);
            LangBuilder builder = CreateLang.builder(CreateNuclear.MOD_ID).text(TooltipHelper.makeProgressBar(5, heatLevel.ordinal()+1));

            builder.translate("tooltip.heatLevel." + Lang.asId(heatLevel.name()))
                    .space()
                    .text("(")
                    .add(CreateNuclearLang.number(Math.abs(heat)))
                    .space()
                    .translate("generic.unit.heat")
                    .text(")")
                    .space();

            if (heatLevel == DANGER) builder.style(DANGER.getTextColor()).style(ChatFormatting.STRIKETHROUGH);
            else builder.style(heatLevel.getTextColor());

            return builder;
        }

        public static LangBuilder getName(String name) {
            LangBuilder builder = CreateNuclearLang.builder(CreateNuclear.MOD_ID);
            builder.translate("gui." + name + ".info_header.title");

            return builder;
        }
    }
}
