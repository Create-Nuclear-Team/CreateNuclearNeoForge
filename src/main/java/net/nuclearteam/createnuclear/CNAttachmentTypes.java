package net.nuclearteam.createnuclear;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.nuclearteam.createnuclear.content.radiation.capability.RadiationCapability;

import java.util.function.Supplier;

public class CNAttachmentTypes {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, CreateNuclear.MOD_ID);

    public static final Supplier<AttachmentType<RadiationCapability>> RADIATION = ATTACHMENT_TYPES.register("radiation", () -> AttachmentType
        .builder(RadiationCapability::new)
        .serialize(RadiationCapability.CODEC)
        .sync(RadiationCapability.STREAM_CODEC)
        .build()
    );

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

}
