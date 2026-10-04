package net.nuclearteam.createnuclear.content.multiblock;

import com.simibubi.create.foundation.data.SpecialBlockStateGen;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.nuclearteam.createnuclear.content.multiblock.input.fluid.ReactorFluidInput;
import net.nuclearteam.createnuclear.content.multiblock.input.item.ReactorRodInput;
import net.nuclearteam.createnuclear.content.multiblock.output.ReactorOutput;

public class ReactorIOGenerator extends SpecialBlockStateGen {

    private final DirectionProperty facing;
    private final String path;

    public static ReactorIOGenerator reactorInputFluid() {
        return new ReactorIOGenerator(ReactorFluidInput.FACING, "block/reactor/fluid_input/fluid_input");
    }

    public static ReactorIOGenerator reactorInputRods() {
        return new ReactorIOGenerator(ReactorRodInput.FACING, "block/reactor/rod_input/rod_input");
    }

    public static ReactorIOGenerator reactorOutputSU() {
        return new ReactorIOGenerator(ReactorOutput.FACING, "block/reactor/output/output");
    }

    public ReactorIOGenerator(DirectionProperty facing, String path) {
        this.facing = facing;
        this.path = path;
    }

    @Override
    protected int getXRotation(BlockState state) {
        return state.getValue(facing) == Direction.DOWN ? 180 : 0;
    }

    @Override
    protected int getYRotation(BlockState state) {
        return state.getValue(facing).getAxis().isVertical()
            ? 0
            : horizontalAngle(state.getValue(facing));
    }

    @Override
    public <T extends Block> ModelFile getModel(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov, BlockState state) {
        return prov
            .models()
            .getExistingFile(prov
                .modLoc(path + (state.getValue(facing).getAxis().isVertical()
                    ?  "_vertical"
                    : ""
                ))
            )
        ;
    }
}
