package me.emvoh.ae2fluidterminalsrework.mixin.client;

import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEFluidStack;
import appeng.container.AEBaseContainer;
import appeng.fluids.container.ContainerMEPortableFluidCell;
import appeng.helpers.InventoryAction;
import appeng.util.Platform;
import me.emvoh.ae2fluidterminalsrework.helper.FluidTerminalActionHelper;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ContainerMEPortableFluidCell.class, remap = false)
public abstract class MixinContainerMEPortableFluidCell {

    @Shadow @Final
    private IMEMonitor<IAEFluidStack> monitor;

    @Shadow
    private IAEFluidStack clientRequestedTargetFluid;

    @Shadow @Final
    private int slot;

    @Unique
    private IEnergySource asg$getPowerSource() {
        return ((AEBaseContainer) (Object) this).getPowerSource();
    }

    @Unique
    private IActionSource asg$getActionSource() {
        return ((AEBaseContainer) (Object) this).getActionSource();
    }

    @Inject(
            method = "doAction(Lnet/minecraft/entity/player/EntityPlayerMP;Lappeng/helpers/InventoryAction;IJ)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void asg$fillFromInventoryWhenCursorEmpty(EntityPlayerMP player, InventoryAction action, int slotId, long id, CallbackInfo ci) {
        if (action != InventoryAction.FILL_ITEM) {
            return;
        }

        if (Platform.isClient()) {
            return;
        }

        final ItemStack cursor = player.inventory.getItemStack();
        if (!cursor.isEmpty()) {
            return;
        }

        if (this.clientRequestedTargetFluid == null) {
            return;
        }

        final int lockedSlot = (this.slot >= 0) ? this.slot : -1;
        final IAEFluidStack request = this.clientRequestedTargetFluid.copy();
        final boolean fillAll = (id == 1L) || player.isSneaking();

        boolean changed = false;

        if (fillAll) {
            changed = FluidTerminalActionHelper.fillAllFillableContainers(
                    player,
                    this.monitor,
                    asg$getPowerSource(),
                    asg$getActionSource(),
                    request,
                    lockedSlot
            );
        } else {
            final int invSlot = FluidTerminalActionHelper.findFirstFillableContainerSlot(
                    player,
                    request.getFluidStack(),
                    lockedSlot
            );
            if (invSlot >= 0) {
                final int res = FluidTerminalActionHelper.fillOnceFromInvSlot(
                        player,
                        invSlot,
                        this.monitor,
                        asg$getPowerSource(),
                        asg$getActionSource(),
                        request,
                        lockedSlot
                );
                changed = res > 0;
            }
        }

        if (!changed) {
            return;
        }

        player.inventory.markDirty();
        ((AEBaseContainer) (Object) this).detectAndSendChanges();
        ci.cancel();
    }
}