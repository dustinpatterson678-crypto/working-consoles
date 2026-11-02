package com.dustin.wcore.controller;

import com.dustin.wcore.block.ControllerBlock;
import com.dustin.wcore.block.ControllerBlockEntity;
import com.dustin.wcore.item.ConsoleControllerItem;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Everything WorkingConsolesCore.registerController made for one controller type. */
public record RegisteredController(
   ControllerType type, ControllerBlock block, ConsoleControllerItem item, BlockEntityType<ControllerBlockEntity> blockEntity
) {
}
