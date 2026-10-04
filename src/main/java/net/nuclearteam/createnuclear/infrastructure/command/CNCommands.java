package net.nuclearteam.createnuclear.infrastructure.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class CNCommands {
    // Server Commands
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("cn")
            .requires(cs -> cs.hasPermission(0))
            .then(RadiationInfoCommand.register())
        ;
        LiteralCommandNode<CommandSourceStack> createNuclearRoot = dispatcher.register(root);
    }
}
