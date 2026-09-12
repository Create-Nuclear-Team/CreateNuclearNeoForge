package net.nuclearteam.createnuclear.infrastructure.ponder;

import com.tterrag.registrate.util.entry.RegistryEntry;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import net.nuclearteam.createnuclear.CNBlocks;

import static com.simibubi.create.infrastructure.ponder.AllCreatePonderTags.DISPLAY_SOURCES;
import static com.simibubi.create.infrastructure.ponder.AllCreatePonderTags.KINETIC_SOURCES;

public class CNCreateNuclearPonderTags {
    public static void register(PonderTagRegistrationHelper<ResourceLocation> helper) {
        PonderTagRegistrationHelper<RegistryEntry<?, ?>> HELPER = helper.withKeyFunction(RegistryEntry::getId);

        helper.registerTag(KINETIC_SOURCES)
            .addToIndex()
            .item(CNBlocks.REACTOR_CONTROLLER.asItem())
            .title("Kinetic Nuclear")
            .register();

        HELPER.addToTag(KINETIC_SOURCES)
            .add(CNBlocks.REACTOR_CONTROLLER)
        ;

        HELPER.addToTag(DISPLAY_SOURCES)
            .add(CNBlocks.REACTOR_CONTROLLER)
        ;

    }

}
