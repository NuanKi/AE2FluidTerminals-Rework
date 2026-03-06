package me.emvoh.ae2fluidterminalsrework.mixin.common;

import appeng.items.tools.powered.ToolWirelessFluidTerminal;
import appeng.items.tools.powered.ToolWirelessTerminal;
import me.emvoh.ae2fluidterminalsrework.helper.WirelessFluidDrainHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = ToolWirelessFluidTerminal.class, remap = false)
public abstract class MixinToolWirelessFluidTerminal extends ToolWirelessTerminal {

    @NotNull
    @Override
    public ActionResult<ItemStack> onItemRightClick(final World world, final EntityPlayer player, final EnumHand hand) {
        final ItemStack stack = player.getHeldItem(hand);

        // Normal click: open GUI like vanilla
        if (!player.isSneaking()) {
            return super.onItemRightClick(world, player, hand);
        }

        if (world.isRemote) {
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        // Server side + sneaking, try drain first
        final int slot = (hand == EnumHand.MAIN_HAND) ? player.inventory.currentItem : 40;
        final RayTraceResult hit = this.rayTrace(world, player, true);

        final WirelessFluidDrainHelper.Outcome out =
                WirelessFluidDrainHelper.tryDrainSourceIntoNetwork(this, world, player, hand, stack, slot, hit);

        // If we did not target a fluid source, fall back to normal behavior (open GUI)
        if (out == WirelessFluidDrainHelper.Outcome.NO_TARGET) {
            return super.onItemRightClick(world, player, hand);
        }

        // Targeted a fluid source, do not open GUI
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }
}