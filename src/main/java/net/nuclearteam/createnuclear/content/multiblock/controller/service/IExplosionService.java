package net.nuclearteam.createnuclear.content.multiblock.controller.service;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.nuclearteam.createnuclear.infrastructure.config.CNConfigs;
import org.jetbrains.annotations.Nullable;

public interface IExplosionService {
    void triggerExplosion(ServerLevel level, BlockPos controllerPos, @Nullable BoundingBox multiblockBounds, int reactorSize, int countFuelRod,
                          int notifyRadius, boolean notifyWarnAll);

    default void triggerExplosion(ServerLevel level, BlockPos controllerPos, @Nullable BoundingBox multiblockBounds, int reactorSize, int countFuelRod) {
        triggerExplosion(level, controllerPos, multiblockBounds, reactorSize,
                            countFuelRod, CNConfigs.server().notify.warningDistance.get(),
                            CNConfigs.server().notify.warnAllPlayers.get()
        );
    }
}
