package me.emvoh.ae2fluidterminalsrework.mixin.client;

import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketInventoryAction;
import appeng.fluids.client.gui.GuiMEPortableFluidCell;
import appeng.fluids.container.ContainerMEPortableFluidCell;
import appeng.client.me.SlotFluidME;
import appeng.helpers.InventoryAction;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiMEPortableFluidCell.class)
public abstract class MixinGuiMEPortableFluidCell {

    @Shadow @Final
    private ContainerMEPortableFluidCell container;

    @Inject(
            method = "handleMouseClick(Lnet/minecraft/inventory/Slot;IILnet/minecraft/inventory/ClickType;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void ae2fluidterminalsrework$shiftClickFluidFillsAll(Slot slot, int slotIdx, int mouseButton, ClickType clickType, CallbackInfo ci) {
        if (!(slot instanceof SlotFluidME meSlot)) return;
        if (clickType != ClickType.QUICK_MOVE) return;

        if (mouseButton != 0) return;
        if (!meSlot.getHasStack()) return;

        this.container.setTargetStack(meSlot.getAEFluidStack());
        NetworkHandler.instance().sendToServer(new PacketInventoryAction(InventoryAction.FILL_ITEM, slot.slotNumber, 1L));

        ci.cancel();
    }
}