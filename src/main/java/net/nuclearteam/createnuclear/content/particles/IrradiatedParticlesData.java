package net.nuclearteam.createnuclear.content.particles;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.foundation.particle.ICustomParticleDataWithSprite;
import net.minecraft.client.particle.ParticleEngine.SpriteParticleRegistration;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.nuclearteam.createnuclear.CNParticleTypes;

import java.util.Locale;

public class IrradiatedParticlesData implements ParticleOptions, ICustomParticleDataWithSprite<IrradiatedParticlesData> {

    public static final com.mojang.serialization.MapCodec<IrradiatedParticlesData> CODEC = RecordCodecBuilder.mapCodec(i ->
            i.group(
                            Codec.INT.optionalFieldOf("t", 0).forGetter(p -> p.t)
                    )
                    .apply(i, IrradiatedParticlesData::new)
    );

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, IrradiatedParticlesData> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.composite(
            net.minecraft.network.codec.ByteBufCodecs.INT,
            p -> p.t,
            IrradiatedParticlesData::new
    );



    int t;

    public IrradiatedParticlesData(int _t) {
        t = _t;
    }

    public IrradiatedParticlesData() {
        this(0);
    }

    @Override
    public ParticleType<?> getType() {
        return CNParticleTypes.IRRADIATED_PARTICLES.get();
    }

    public void writeToNetwork(FriendlyByteBuf buffer) {
        buffer.writeInt(t);
    }

    public String writeToString() {
        return String.format(Locale.ROOT, "%s %d", CNParticleTypes.IRRADIATED_PARTICLES.parameter(), t);
    }



    public com.mojang.serialization.MapCodec<IrradiatedParticlesData> getCodec(ParticleType<IrradiatedParticlesData> type) {
        return CODEC;
    }

    public net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, IrradiatedParticlesData> getStreamCodec() {
        return STREAM_CODEC;
    }

    @Override
    public SpriteParticleRegistration<IrradiatedParticlesData> getMetaFactory() {
        return IrradiatedParticles.Provider::new;
    }






}