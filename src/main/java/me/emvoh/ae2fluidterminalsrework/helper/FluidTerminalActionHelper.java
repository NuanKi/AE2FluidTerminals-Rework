package me.emvoh.ae2fluidterminalsrework.helper;

import appeng.api.config.Actionable;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEFluidStack;
import appeng.core.AELog;
import appeng.util.Platform;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

public final class FluidTerminalActionHelper {

    private FluidTerminalActionHelper() {
    }

    /**
     * Finds the first slot in the player's main inventory (hotbar first) that can accept the given fluid.
     *
     * @param lockedSlot slot index to skip, or -1 for none
     */
    public static int findFirstFillableContainerSlot(EntityPlayerMP player, FluidStack fluid, int lockedSlot) {
        if (player == null || fluid == null) {
            return -1;
        }

        // Hotbar first (0-8), then rest (9-35)
        for (int pass = 0; pass < 2; pass++) {
            final int start = (pass == 0) ? 0 : 9;
            final int end = (pass == 0) ? 9 : player.inventory.mainInventory.size();

            for (int idx = start; idx < end; idx++) {
                if (idx == lockedSlot) {
                    continue;
                }

                final ItemStack s = player.inventory.mainInventory.get(idx);
                if (s.isEmpty()) {
                    continue;
                }

                final ItemStack one = s.copy();
                one.setCount(1);

                final IFluidHandlerItem fh = FluidUtil.getFluidHandler(one);
                if (fh == null) {
                    continue;
                }

                final int canAccept = fh.fill(fluid.copy(), false);
                if (canAccept > 0) {
                    return idx;
                }
            }
        }

        return -1;
    }

    /**
     * Fills all fillable containers found in the player's inventory (hotbar first).
     *
     * @return true if at least one container was filled
     */
    public static boolean fillAllFillableContainers(
            EntityPlayerMP player,
            IMEMonitor<IAEFluidStack> monitor,
            IEnergySource power,
            IActionSource actionSource,
            IAEFluidStack request,
            int lockedSlot
    ) {
        if (player == null || monitor == null || power == null || actionSource == null || request == null) {
            return false;
        }

        boolean changed = false;

        for (int pass = 0; pass < 2; pass++) {
            final int start = (pass == 0) ? 0 : 9;
            final int end = (pass == 0) ? 9 : player.inventory.mainInventory.size();

            for (int idx = start; idx < end; idx++) {
                if (idx == lockedSlot) {
                    continue;
                }

                final ItemStack s = player.inventory.mainInventory.get(idx);
                if (s.isEmpty()) {
                    continue;
                }

                if (s.getCount() == 1) {
                    while (true) {
                        final int res = fillOnceFromInvSlot(player, idx, monitor, power, actionSource, request, lockedSlot);
                        if (res > 0) {
                            changed = true;
                            continue;
                        }
                        if (res == 0) {
                            break;
                        }
                        // res < 0: ME can't provide fluid or power, stop early
                        return changed;
                    }
                } else {
                    final int originalCount = s.getCount();
                    for (int i = 0; i < originalCount; i++) {
                        final int res = fillOnceFromInvSlot(player, idx, monitor, power, actionSource, request, lockedSlot);
                        if (res > 0) {
                            changed = true;
                            continue;
                        }
                        if (res == 0) {
                            break;
                        }
                        return changed;
                    }
                }
            }
        }

        return changed;
    }

    /**
     * Attempts to fill exactly one container from the given inventory slot.
     *
     * @return >0 = amount filled, 0 = this slot can't be filled further, <0 = ME can't provide fluid or power
     */
    public static int fillOnceFromInvSlot(
            EntityPlayerMP player,
            int invSlot,
            IMEMonitor<IAEFluidStack> monitor,
            IEnergySource power,
            IActionSource actionSource,
            IAEFluidStack request,
            int lockedSlot
    ) {
        if (player == null || monitor == null || power == null || actionSource == null || request == null) {
            return 0;
        }

        if (invSlot == lockedSlot) {
            return 0;
        }

        ItemStack inSlot = player.inventory.mainInventory.get(invSlot);
        if (inSlot.isEmpty()) {
            return 0;
        }

        final FluidStack requestFluid = request.getFluidStack();
        if (requestFluid == null) {
            return 0;
        }

        final ItemStack one = inSlot.copy();
        one.setCount(1);

        IFluidHandlerItem fh = FluidUtil.getFluidHandler(one);
        if (fh == null) {
            return 0;
        }

        // How much could the container accept (for this fluid)
        final int amountAllowed = fh.fill(requestFluid.copy(), false);
        if (amountAllowed <= 0) {
            return 0;
        }

        // Simulate extraction from ME
        final IAEFluidStack simReq = request.copy().setStackSize(amountAllowed);
        final IAEFluidStack canPull = Platform.poweredExtraction(
                power,
                monitor,
                simReq,
                actionSource,
                Actionable.SIMULATE
        );

        if (canPull == null || canPull.getStackSize() < 1) {
            return -1;
        }

        final FluidStack canPullFluid = canPull.getFluidStack();
        if (canPullFluid == null) {
            return -1;
        }

        // Re-check how much would actually fit with what ME can provide
        final int canFill = fh.fill(canPullFluid.copy(), false);
        if (canFill <= 0) {
            return 0;
        }

        // Actually extract from ME
        final IAEFluidStack modReq = request.copy().setStackSize(canFill);
        final IAEFluidStack pulled = Platform.poweredExtraction(
                power,
                monitor,
                modReq,
                actionSource
        );

        if (pulled == null || pulled.getStackSize() < 1) {
            AELog.error("Unable to pull fluid out of the ME system even though the simulation said yes");
            return -1;
        }

        final FluidStack pulledFluid = pulled.getFluidStack();
        if (pulledFluid == null) {
            AELog.error("Pulled fluid stack was null even though extraction returned a valid AE stack");
            return -1;
        }

        // Fill the container for real
        final int used = fh.fill(pulledFluid.copy(), true);
        if (used != canFill) {
            AELog.error("Fluid item [%s] reported a different possible amount than it actually accepted.", inSlot.getDisplayName());
        }

        final ItemStack result = fh.getContainer();

        // Re-read slot (defensive)
        inSlot = player.inventory.mainInventory.get(invSlot);
        if (inSlot.isEmpty()) {
            tryAddOrDrop(player, result);
            return used;
        }

        if (inSlot.getCount() == 1) {
            player.inventory.mainInventory.set(invSlot, result);
        } else {
            inSlot.shrink(1);
            player.inventory.mainInventory.set(invSlot, inSlot);
            tryAddOrDrop(player, result);
        }

        return used;
    }

    private static void tryAddOrDrop(EntityPlayerMP player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (!player.inventory.addItemStackToInventory(stack)) {
            player.dropItem(stack, false);
        }
    }
}