package net.nuclearteam.createnuclear.content.contraptions.irradiated.spider;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.nuclearteam.createnuclear.CreateNuclear;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * GeckoLib renderer shared by every irradiated spider stage. Resources follow the
 * DefaultedEntityGeoModel conventions (geo/entity, animations/entity, textures/entity) and the
 * "_glowmask" texture is drawn at full brightness by AutoGlowingGeoLayer.
 */
@OnlyIn(Dist.CLIENT)
public class IrradiatedSpiderRenderer<T extends AbstractIrradiatedSpider> extends GeoEntityRenderer<T> {
    public IrradiatedSpiderRenderer(EntityRendererProvider.Context context, String name, float shadowRadius) {
        // true: the "head" bone follows where the spider looks
        super(context, new DefaultedEntityGeoModel<>(CreateNuclear.asResource(name), true));
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        this.shadowRadius = shadowRadius;
    }

    public static IrradiatedSpiderRenderer<IrradiatedSpiderStage2> stage2(EntityRendererProvider.Context context) {
        return new IrradiatedSpiderRenderer<>(context, "irradiated_spider_stage2", 1.0F);
    }

    public static IrradiatedSpiderRenderer<IrradiatedSpiderStage3> stage3(EntityRendererProvider.Context context) {
        return new IrradiatedSpiderRenderer<>(context, "irradiated_spider_stage3", 1.3F);
    }

    /** The death animation lays the spider down itself: no vanilla side rotation. */
    @Override
    protected float getDeathMaxRotation(T animatable) {
        return 0.0F;
    }
}
