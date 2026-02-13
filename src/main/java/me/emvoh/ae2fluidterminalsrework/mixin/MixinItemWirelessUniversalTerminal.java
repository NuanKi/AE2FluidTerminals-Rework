package me.emvoh.ae2fluidterminalsrework.mixin;

import com.circulation.ae2wut.item.ItemWirelessUniversalTerminal;
import me.emvoh.ae2fluidterminalsrework.helper.WirelessFluidDrainHelper;

import appeng.items.tools.powered.ToolWirelessTerminal;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ItemWirelessUniversalTerminal.class, remap = false)
public abstract class MixinItemWirelessUniversalTerminal extends ToolWirelessTerminal {

    @Shadow
    public abstract boolean hasMode(ItemStack t, byte mode);

    private static final byte FLUID_MODE = 2;

    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(
            method = {
                    "onItemRightClick(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/util/EnumHand;)Lnet/minecraft/util/ActionResult;",
                    "func_77659_a(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/util/EnumHand;)Lnet/minecraft/util/ActionResult;"
            },
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false
    )
    private void ae2fluidterminalsrework$drainAnyModeWhenSneaking(World w, EntityPlayer player, EnumHand hand, CallbackInfoReturnable<ActionResult<ItemStack>> cir) {
        final ItemStack stack = player.getHeldItem(hand);

        // Only if sneaking AND this WUT actually has fluid mode installed/unlocked
        if (!player.isSneaking()) return;
        if (!stack.hasTagCompound()) return;
        if (!hasMode(stack, FLUID_MODE)) return;

        final RayTraceResult hit = this.rayTrace(w, player, true);

        if (w.isRemote) return;

        final int slot = (hand == EnumHand.MAIN_HAND) ? player.inventory.currentItem : 40;

        final WirelessFluidDrainHelper.Outcome out = WirelessFluidDrainHelper.tryDrainSourceIntoNetwork(this, w, player, hand, stack, slot, hit);

        // If we were actually pointing at a fluid source, helper either STORED or FAILED.
        // In both cases, don't open the GUI (cancel WUT's original onItemRightClick).
        if (out != WirelessFluidDrainHelper.Outcome.NO_TARGET) {
            cir.setReturnValue(new ActionResult<>(EnumActionResult.SUCCESS, stack));
            cir.cancel();
        }
    }
}
