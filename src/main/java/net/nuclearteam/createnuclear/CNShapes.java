package net.nuclearteam.createnuclear;

import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.BiFunction;

public class CNShapes {
    // Independent Shapers
    public static final VoxelShaper
        REACTOR_OUTPUT = shape(0, 0, 0, 16, 14, 16).forDirectional(),
        REACTOR_INPUT = shape(0,0,0,16,16,16).forDirectional(),
        REACTOR_FLUID_INPUT = shape(0,0,0,16,16,16).forDirectional()
    ;

    private static Builder shape(VoxelShape shape) {
        return new Builder(shape);
    }

    public static Builder shape(double x1, double y1, double z1, double x2, double y2, double z2) {
        return shape(cuboid(x1, y1, z1, x2, y2, z2));
    }

    private static VoxelShape cuboid(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Block.box(x1, y1, z1, x2, y2, z2);
    }

    public static class Builder {

        private final VoxelShape shape;

        public Builder(VoxelShape shape) {
            this.shape = shape;
        }

        private VoxelShaper build(BiFunction<VoxelShape, Direction, VoxelShaper> factory, Direction direction) {
            return factory.apply(shape, direction);
        }

        public VoxelShaper forDirectional(Direction direction) {
            return build(VoxelShaper::forDirectional, direction);
        }

        public VoxelShaper forDirectional() {
            return forDirectional(Direction.UP);
        }

    }

}