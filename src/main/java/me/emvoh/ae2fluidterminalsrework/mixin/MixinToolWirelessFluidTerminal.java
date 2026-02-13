package me.emvoh.ae2fluidterminalsrework.mixin;

import appeng.api.AEApi;
import appeng.items.tools.powered.ToolWirelessFluidTerminal;
import appeng.items.tools.powered.ToolWirelessTerminal;

import me.emvoh.ae2fluidterminalsrework.helper.WirelessFluidDrainHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = ToolWirelessFluidTerminal.class, remap = false)
public abstract class MixinToolWirelessFluidTerminal extends ToolWirelessTerminal {

    @Override
    public ActionResult<ItemStack> onItemRightClick(final World world, final EntityPlayer player, final EnumHand hand) {
        final ItemStack stack = player.getHeldItem(hand);

        // Normal click: open GUI like vanilla
        if (!player.isSneaking()) {
            AEApi.instance().registries().wireless().openWirelessTerminalGui(stack, world, player);
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        if (world.isRemote) {
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        // Sneak: try drain; if not targeting a source fluid, open GUI
        if (!world.isRemote) {
            final int slot = (hand == EnumHand.MAIN_HAND) ? player.inventory.currentItem : 40;
            final RayTraceResult hit = this.rayTrace(world, player, true);

            final WirelessFluidDrainHelper.Outcome out = WirelessFluidDrainHelper.tryDrainSourceIntoNetwork(this, world, player, hand, stack, slot, hit);

            if (out == WirelessFluidDrainHelper.Outcome.NO_TARGET) {
                AEApi.instance().registries().wireless().openWirelessTerminalGui(stack, world, player);
            }
        }


        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }
}
