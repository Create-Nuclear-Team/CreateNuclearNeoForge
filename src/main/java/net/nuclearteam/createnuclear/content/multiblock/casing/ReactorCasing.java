package net.nuclearteam.createnuclear.content.multiblock.casing;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.nuclearteam.createnuclear.CNBlockEntityTypes;
import net.nuclearteam.createnuclear.CNBlocks;
import net.nuclearteam.createnuclear.content.multiblock.AbstractReactorMultiblockPart;
import net.nuclearteam.createnuclear.content.multiblock.input.item.ReactorRodInput;
import net.nuclearteam.createnuclear.foundation.utility.CreateNuclearLang;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ReactorCasing extends AbstractReactorMultiblockPart implements IWrenchable, IBE<ReactorCasingEntity> {
    private final TypeBlock typeBlock;

    public ReactorCasing(Properties properties, TypeBlock tBlock) {
        super(properties);
        this.typeBlock = tBlock;
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        Player player = context.getPlayer();

        if (player.getItemInHand(InteractionHand.OFF_HAND).is(Blocks.HOPPER.asItem())) {
            level.setBlock(pos, CNBlocks.REACTOR_ROD_INPUT.getDefaultState().setValue(ReactorRodInput.FACING, context.getClickedFace()), 2);
            player.sendSystemMessage(Component.translatable("reactor.update.casing.input"));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public Class<ReactorCasingEntity> getBlockEntityClass() {
        return ReactorCasingEntity.class;
    }

    @Override
    public BlockEntityType<? extends ReactorCasingEntity> getBlockEntityType() {
        return switch (typeBlock) {
            case CORE -> CNBlockEntityTypes.REACTOR_CORE.get();
            case CASING -> CNBlockEntityTypes.REACTOR_CASING.get();
        };

    }

    public enum TypeBlock implements StringRepresentable {
        CASING,
        CORE,
        ;

        @Override
        public String getSerializedName() {
            return CreateNuclearLang.asId(name());
        }
    }
}
