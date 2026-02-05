package me.emvoh.ae2fluidterminalsrework.mixin;

import appeng.api.config.Actionable;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEFluidStack;
import appeng.container.AEBaseContainer;
import appeng.core.AELog;
import appeng.fluids.container.ContainerFluidTerminal;
import appeng.helpers.InventoryAction;
import appeng.util.Platform;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ContainerFluidTerminal.class, remap = false)
public abstract class MixinContainerFluidTerminal {

    @Shadow @Final
    private IMEMonitor<IAEFluidStack> monitor;

    @Shadow
    private IAEFluidStack clientRequestedTargetFluid;

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
    private void asg$fillFromInventoryWhenCursorEmpty(EntityPlayerMP player, InventoryAction action, int slot, long id, CallbackInfo ci) {
        if (action != InventoryAction.FILL_ITEM) {
            return;
        }

        if (Platform.isClient()) {
            return;
        }

        ItemStack cursor = player.inventory.getItemStack();
        if (!cursor.isEmpty()) {
            return;
        }

        if (this.clientRequestedTargetFluid == null) {
            return;
        }

        final IAEFluidStack request = this.clientRequestedTargetFluid.copy();

        int invSlot = asg$findFirstFillableContainerSlot(player, request.getFluidStack());
        if (invSlot < 0) {
            return;
        }

        ItemStack held = player.inventory.mainInventory.get(invSlot);
        if (held.isEmpty()) {
            return;
        }

        ItemStack heldCopy = held.copy();
        heldCopy.setCount(1);
        IFluidHandlerItem fh = FluidUtil.getFluidHandler(heldCopy);
        if (fh == null) {
            return;
        }

        request.setStackSize(Integer.MAX_VALUE);
        int amountAllowed = fh.fill(request.getFluidStack(), false);
        if (amountAllowed <= 0) {
            return;
        }

        held = player.inventory.mainInventory.get(invSlot);
        if (held.isEmpty()) {
            return;
        }

        ItemStack oneContainer = held.copy();
        oneContainer.setCount(1);
        fh = FluidUtil.getFluidHandler(oneContainer);
        if (fh == null) {
            return;
        }

        final IAEFluidStack canPull = Platform.poweredExtraction(
                asg$getPowerSource(),
                this.monitor,
                request.setStackSize(amountAllowed),
                asg$getActionSource(),
                Actionable.SIMULATE
        );

        if (canPull == null || canPull.getStackSize() < 1) {
            return;
        }

        final int canFill = fh.fill(canPull.getFluidStack(), false);
        if (canFill == 0) {
            return;
        }

        final IAEFluidStack pulled = Platform.poweredExtraction(
                asg$getPowerSource(),
                this.monitor,
                request.setStackSize(canFill),
                asg$getActionSource()
        );

        if (pulled == null || pulled.getStackSize() < 1) {
            AELog.error("Unable to pull fluid out of the ME system even though the simulation said yes ");
            return;
        }

        final int used = fh.fill(pulled.getFluidStack(), true);
        if (used != canFill) {
            AELog.error("Fluid item [%s] reported a different possible amount than it actually accepted.", held.getDisplayName());
        }

        if (held.getCount() == 1) {
            player.inventory.mainInventory.set(invSlot, fh.getContainer());
        } else {
            held.shrink(1);
            player.inventory.mainInventory.set(invSlot, held);

            if (!player.inventory.addItemStackToInventory(fh.getContainer())) {
                player.dropItem(fh.getContainer(), false);
            }
        }

        player.inventory.markDirty();
        ((AEBaseContainer) (Object) this).detectAndSendChanges();

        ci.cancel();
    }

    @Unique
    private int asg$findFirstFillableContainerSlot(EntityPlayerMP player, FluidStack fluid) {
        // Hotbar first (0-8), then rest (9-35)
        for (int pass = 0; pass < 2; pass++) {
            int start = pass == 0 ? 0 : 9;
            int end = pass == 0 ? 9 : player.inventory.mainInventory.size();

            for (int idx = start; idx < end; idx++) {
                ItemStack s = player.inventory.mainInventory.get(idx);
                if (s.isEmpty()) {
                    continue;
                }

                ItemStack one = s.copy();
                one.setCount(1);

                IFluidHandlerItem fh = FluidUtil.getFluidHandler(one);
                if (fh == null) {
                    continue;
                }

                int canAccept = fh.fill(fluid.copy(), false);
                if (canAccept > 0) {
                    return idx;
                }
            }
        }

        return -1;
    }
}
