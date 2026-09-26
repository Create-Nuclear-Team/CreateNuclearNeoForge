package net.nuclearteam.createnuclear.content.multiblock.pattern;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.nuclearteam.createnuclear.CNBlocks;
import net.nuclearteam.createnuclear.content.multiblock.ReactorAssembler;
import net.nuclearteam.createnuclear.content.multiblock.controller.ReactorControllerBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

public class ReactorPattern {
    @FunctionalInterface
    private interface ControllerVisitor {
        boolean visit(BlockPos controllerPos, ReactorControllerBlockEntity entity);
    }


    private boolean isInReactorRange(@Nullable BoundingBox reactorPos, BlockPos blockPos) {
        return reactorPos != null && reactorPos.isInside(blockPos);
    }

    private void scanControllerCandidates(BlockPos blockPos, Level level, ControllerVisitor visitor) {
        BlockPos newBlock;
        Vec3i pos = new Vec3i(blockPos.getX(), blockPos.getY(), blockPos.getZ());
        for (int y = pos.getY()-5; y != pos.getY()+6; y+=1) {
            for (int x = pos.getX()-9; x != pos.getX()+10; x+=1) {
                for (int z = pos.getZ()-9; z != pos.getZ()+10; z+=1) {
                    newBlock = new BlockPos(x, y, z);
                    if (level.getBlockState(newBlock).is(CNBlocks.REACTOR_CONTROLLER.get())
                            && level.getBlockEntity(newBlock) instanceof ReactorControllerBlockEntity entity) {
                        if (visitor.visit(newBlock, entity)) {
                            return;
                        }
                    }
                }
            }
        }
    }

    private BlockPos findControllerPos(BlockPos pos, Level level, BiConsumer<BlockPos, ReactorControllerBlockEntity> onCandidate) {
        BlockPos[] found = {null};

        // Stops at the first in-range candidate: isInReactorRange can only be true for an already
        // assembled controller (getMultiblockPos() is null before assembly), and two assembled
        // reactors can't have overlapping bounding boxes (a block position belongs to at most one
        // structure), so at most one candidate can ever match — stopping there is safe.
        scanControllerCandidates(pos, level, ((controllerPos, entity) -> {
            onCandidate.accept(controllerPos, entity);
            if (isInReactorRange(entity.getMultiblockPos(), pos)) {
                found[0] = controllerPos;
                return true;
            }
            return false;
        }));

        return found[0];
    }

    public void findController(BlockPos blockPos, Level level, boolean first) {
        findControllerPos(blockPos, level, first);
    }

    public BlockPos findControllerPos(BlockPos blockPos, Level level, boolean first){
        return findControllerPos(blockPos, level, (controllerPos, entity) -> {
            boolean inRange = isInReactorRange(entity.getMultiblockPos(), blockPos);
            if (first) {
                if (inRange || !entity.isAssembled()) ReactorAssembler.assemble(controllerPos, level);
            } else if (inRange) {
                ReactorAssembler.disassemble(controllerPos, level);
            }
        });
    }

    public BlockPos findControllerPos(BlockPos blockPos, Level level){
        return findControllerPos(blockPos, level, (controllerPos, entity) -> ReactorAssembler.assemble(controllerPos, level));
    }
}
