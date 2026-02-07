package me.emvoh.ae2fluidterminalsrework.mixin;

import appeng.api.config.Actionable;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEFluidStack;
import appeng.container.AEBaseContainer;
import appeng.core.AELog;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketInventoryAction;
import appeng.fluids.container.ContainerMEPortableFluidCell;
import appeng.fluids.util.AEFluidStack;
import appeng.helpers.InventoryAction;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
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

import java.io.IOException;

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

    @Unique
    private void asg$updateHeld(EntityPlayerMP p) {
        if (Platform.isServer()) {
            try {
                NetworkHandler.instance().sendTo(
                        new PacketInventoryAction(
                                InventoryAction.UPDATE_HAND,
                                0,
                                AEItemStack.fromItemStack(p.inventory.getItemStack())
                        ),
                        p
                );
            } catch (final IOException e) {
                AELog.debug(e);
            }
        }
    }

    @Inject(
            method = "doAction(Lnet/minecraft/entity/player/EntityPlayerMP;Lappeng/helpers/InventoryAction;IJ)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void asg$doAction(EntityPlayerMP player, InventoryAction action, int slotId, long id, CallbackInfo ci) {
        if (action != InventoryAction.FILL_ITEM && action != InventoryAction.EMPTY_ITEM) {
            return;
        }

        ci.cancel();

        ItemStack held = player.inventory.getItemStack();

        if (action == InventoryAction.FILL_ITEM) {
            if (this.clientRequestedTargetFluid == null) {
                return;
            }

            final IAEFluidStack stack = this.clientRequestedTargetFluid.copy();

            final boolean usingCursor = !held.isEmpty();
            int invSlot = -1;

            if (!usingCursor) {
                invSlot = asg$findFirstFillableContainerSlot(player, stack.getFluidStack());
                if (invSlot < 0) {
                    return;
                }

                held = player.inventory.mainInventory.get(invSlot);
                if (held.isEmpty()) {
                    return;
                }
            }

            ItemStack heldCopy = held.copy();
            heldCopy.setCount(1);
            IFluidHandlerItem fh = FluidUtil.getFluidHandler(heldCopy);
            if (fh == null) {
                return;
            }

            stack.setStackSize(Integer.MAX_VALUE);
            int amountAllowed = fh.fill(stack.getFluidStack(), false);

            int heldAmount = held.getCount();
            if (!usingCursor) {
                heldAmount = 1;
            }

            for (int i = 0; i < heldAmount; i++) {

                if (!usingCursor) {
                    held = player.inventory.mainInventory.get(invSlot);
                    if (held.isEmpty()) {
                        return;
                    }
                }

                ItemStack copiedFluidContainer = held.copy();
                copiedFluidContainer.setCount(1);
                fh = FluidUtil.getFluidHandler(copiedFluidContainer);

                if (fh == null) {
                    return;
                }

                final IAEFluidStack canPull = Platform.poweredExtraction(
                        asg$getPowerSource(),
                        this.monitor,
                        stack.setStackSize(amountAllowed),
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
                        stack.setStackSize(canFill),
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

                if (usingCursor) {
                    if (held.getCount() == 1) {
                        player.inventory.setItemStack(fh.getContainer());
                    } else {
                        player.inventory.getItemStack().shrink(1);
                        if (!player.inventory.addItemStackToInventory(fh.getContainer())) {
                            player.dropItem(fh.getContainer(), false);
                        }
                    }
                } else {
                    if (held.getCount() == 1) {
                        player.inventory.mainInventory.set(invSlot, fh.getContainer());
                        break;
                    } else {
                        held.shrink(1);
                        player.inventory.mainInventory.set(invSlot, held);

                        if (!player.inventory.addItemStackToInventory(fh.getContainer())) {
                            player.dropItem(fh.getContainer(), false);
                        }
                        break;
                    }
                }
            }

            if (usingCursor) {
                asg$updateHeld(player);
            } else {
                player.inventory.markDirty();
                this.asg$detectAndSendChanges();
            }

            return;
        }

        if (action == InventoryAction.EMPTY_ITEM) {
            ItemStack heldCopy = held.copy();
            heldCopy.setCount(1);
            IFluidHandlerItem fh = FluidUtil.getFluidHandler(heldCopy);
            if (fh == null) {
                return;
            }

            int heldAmount = held.getCount();
            for (int i = 0; i < heldAmount; i++) {
                ItemStack copiedFluidContainer = held.copy();
                copiedFluidContainer.setCount(1);
                fh = FluidUtil.getFluidHandler(copiedFluidContainer);

                final FluidStack extract = fh.drain(Integer.MAX_VALUE, false);
                if (extract == null || extract.amount < 1) {
                    return;
                }

                final IAEFluidStack notStorable = Platform.poweredInsert(
                        asg$getPowerSource(),
                        this.monitor,
                        AEFluidStack.fromFluidStack(extract),
                        asg$getActionSource(),
                        Actionable.SIMULATE
                );

                if (notStorable != null && notStorable.getStackSize() > 0) {
                    final int toStore = (int) (extract.amount - notStorable.getStackSize());
                    final FluidStack storable = fh.drain(toStore, false);

                    if (storable == null || storable.amount == 0) {
                        return;
                    } else {
                        extract.amount = storable.amount;
                    }
                }

                final FluidStack drained = fh.drain(extract, true);
                extract.amount = drained.amount;

                final IAEFluidStack notInserted = Platform.poweredInsert(
                        asg$getPowerSource(),
                        this.monitor,
                        AEFluidStack.fromFluidStack(extract),
                        asg$getActionSource()
                );

                if (notInserted != null && notInserted.getStackSize() > 0) {
                    IAEFluidStack spill = this.monitor.injectItems(notInserted, Actionable.MODULATE, asg$getActionSource());
                    if (spill != null && spill.getStackSize() > 0) {
                        fh.fill(spill.getFluidStack(), true);
                    }
                }

                if (held.getCount() == 1) {
                    player.inventory.setItemStack(fh.getContainer());
                } else {
                    player.inventory.getItemStack().shrink(1);
                    if (!player.inventory.addItemStackToInventory(fh.getContainer())) {
                        player.dropItem(fh.getContainer(), false);
                    }
                }
            }

            asg$updateHeld(player);
        }
    }

    @Unique
    private int asg$findFirstFillableContainerSlot(EntityPlayerMP player, FluidStack fluid) {
        final int lockedSlot = (this.slot >= 0) ? this.slot : -1;

        for (int pass = 0; pass < 2; pass++) {
            int start = pass == 0 ? 0 : 9;
            int end = pass == 0 ? 9 : player.inventory.mainInventory.size();

            for (int invSlotIdx = start; invSlotIdx < end; invSlotIdx++) {
                if (invSlotIdx == lockedSlot) {
                    continue;
                }

                ItemStack s = player.inventory.mainInventory.get(invSlotIdx);
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
                    return invSlotIdx;
                }
            }
        }

        return -1;
    }

    @Unique
    private void asg$detectAndSendChanges() {
        ((AEBaseContainer) (Object) this).detectAndSendChanges();
    }

}
