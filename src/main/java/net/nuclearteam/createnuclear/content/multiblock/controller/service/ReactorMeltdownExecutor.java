package net.nuclearteam.createnuclear.content.multiblock.controller.service;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.nuclearteam.createnuclear.CNEntityType;
import net.nuclearteam.createnuclear.CreateNuclear;
import net.nuclearteam.createnuclear.compat.Mods;
import net.nuclearteam.createnuclear.compat.aeronotics.sable.SableCompat;
import net.nuclearteam.createnuclear.content.explosion.NuclearExplosionEntity;
import net.nuclearteam.createnuclear.foundation.utility.CreateNuclearLang;
import net.nuclearteam.createnuclear.foundation.utility.NotifyUtil;
import net.nuclearteam.createnuclear.infrastructure.worldgen.biome.BiomeIrradiationService;
import net.nuclearteam.createnuclear.infrastructure.worldgen.biome.CNBiomes;
import org.jetbrains.annotations.Nullable;

public class ReactorMeltdownExecutor implements IExplosionService {
    @Override
    public void triggerExplosion(ServerLevel level, BlockPos controllerPos, @Nullable BoundingBox multiblockBounds, int reactorSize, int countFuelRod, int notifyRadius, boolean notifyWarnAll) {
        BlockPos explosionPos = controllerPos.above(5);
        BlockPos globalPos = SableCompat.toGlobal(level, explosionPos);

        NotifyUtil.sendTitle(level, globalPos,
                CreateNuclearLang.translate("notification.reactor.destroyed"),
                CreateNuclearLang.translate("notification.reactor.meltdown_finished"),
                ChatFormatting.DARK_RED, notifyRadius, notifyWarnAll, 10, 60, 20
        );

        float size = computeExplosionSize(reactorSize, countFuelRod);

        NuclearExplosionEntity explosion = new NuclearExplosionEntity(CNEntityType.NUCLEAR_EXPLOSION.get(), level);
        explosion.setPos(explosionPos.getX() + 0.5D, explosionPos.getY() + 10.0D, explosionPos.getZ() - 2.0D);
        explosion.setSize(size);
        explosion.setNoGriefing(!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING));
        level.addFreshEntity(explosion);

        level.destroyBlock(controllerPos, false);
        Mods.SABLE.executeIfInstalled(() -> () -> destroyMultiblockStructure(level, multiblockBounds));

        BiomeIrradiationService.circularArea(level, globalPos, CNBiomes.Irradiated.PLAIN, (int) (size * 30));

    }

    /**
     * Destroys every block of the assembled reactor structure without dropping the blocks
     * themselves — matching the controller's own destruction (level.destroyBlock(pos, false)).
     * Blocks with an inventory (e.g. rod/fluid inputs) still spill their contents, since that
     * happens in their own Block#onRemove regardless of the dropBlocks flag.
     */
    private static void destroyMultiblockStructure(ServerLevel level, @Nullable BoundingBox multiblockBounds) {
        if (multiblockBounds == null) return;

        forceLoadChunks(level, multiblockBounds, true);
        try {
            BlockPos.betweenClosedStream(
                new BlockPos(multiblockBounds.minX(), multiblockBounds.minY(), multiblockBounds.minZ()),
                new BlockPos(multiblockBounds.maxX(), multiblockBounds.maxY(), multiblockBounds.maxZ())
            ).forEach(pos -> {
                BlockPos immutable = pos.immutable();
                try {
                    level.destroyBlock(immutable, false);
                } catch (Exception e) {
                    CreateNuclear.LOGGER.warn("[MeltdownDebug] failed to destroy {} while clearing reactor structure: {}", immutable, e);
                }
            });
        } finally {
            forceLoadChunks(level, multiblockBounds, false);
        }
    }

    private static void forceLoadChunks(ServerLevel level, BoundingBox bounds, boolean load) {
        int minChunkX = SectionPos.blockToSectionCoord(bounds.minX());
        int maxChunkX = SectionPos.blockToSectionCoord(bounds.maxX());
        int minChunkZ = SectionPos.blockToSectionCoord(bounds.minZ());
        int maxChunkZ = SectionPos.blockToSectionCoord(bounds.maxZ());

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                level.setChunkForced(cx, cz, load);
            }
        }
    }

    private static float computeExplosionSize(int reactorSize, int countFuelRod) {
        // Scales with reactor footprint: 5x5 -> 1.0, 7x7 -> 1.4, 9x9 -> 1.8
        float structureFactor = reactorSize / 5f;

        // Square root gives diminishing returns: each extra uranium rod adds less
        // to the radius than the previous one.
        float fuelImpact = Mth.sqrt(countFuelRod) * .3f;

        // Final size: constant base + (fuel impact scaled by structure factor)
        return Mth.clamp(1.5f + (fuelImpact * structureFactor), 1f, 10f);
    }
}
