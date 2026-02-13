package me.emvoh.ae2fluidterminalsrework.helper;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.items.tools.powered.ToolWirelessTerminal;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.Optional;

public final class WirelessFluidDrainHelper {

    private WirelessFluidDrainHelper() {}

    public enum Outcome { NO_TARGET, STORED, FAILED }

    private static final int MB_PER_USE = 1000;

    public static Outcome tryDrainSourceIntoNetwork(
            ToolWirelessTerminal item,
            World world,
            EntityPlayer player,
            EnumHand hand,
            ItemStack terminalStack,
            int slot,
            RayTraceResult hit
    ) {
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK) {
            return Outcome.NO_TARGET;
        }

        final IFluidHandler fh = FluidUtil.getFluidHandler(world, hit.getBlockPos(), hit.sideHit);
        if (fh == null) {
            return Outcome.NO_TARGET;
        }

        final FluidStack preview = fh.drain(MB_PER_USE, false);
        if (preview == null || preview.amount != MB_PER_USE) {
            return Outcome.NO_TARGET;
        }

        final WirelessTerminalGuiObject wto = new WirelessTerminalGuiObject(item, terminalStack, player, world, slot, 0, 0);
        if (!wto.rangeCheck() || wto.getActionableNode() == null) {
            player.sendStatusMessage(new TextComponentString(TextFormatting.RED + "Out of range / not linked"), true);
            return Outcome.FAILED;
        }

        final IFluidStorageChannel chan = AEApi.instance().storage().getStorageChannel(IFluidStorageChannel.class);
        final IMEMonitor<IAEFluidStack> fluids = wto.getInventory(chan);
        if (fluids == null) {
            player.sendStatusMessage(new TextComponentString(TextFormatting.RED + "No fluid storage access"), true);
            return Outcome.FAILED;
        }

        final IAEFluidStack sim = chan.createStack(preview);
        if (sim == null) return Outcome.FAILED;

        final IActionSource src = new SimpleActionSource(player, wto);

        final IAEFluidStack remSim = fluids.injectItems(sim, Actionable.SIMULATE, src);
        if (remSim != null && remSim.getStackSize() > 0) {
            player.sendStatusMessage(new TextComponentString(TextFormatting.RED + "Network can't accept that fluid"), true);
            return Outcome.FAILED;
        }

        final FluidStack drained = fh.drain(MB_PER_USE, true);
        if (drained == null || drained.amount != MB_PER_USE) {
            player.sendStatusMessage(new TextComponentString(TextFormatting.RED + "Drain failed"), true);
            return Outcome.FAILED;
        }

        final IAEFluidStack ins = chan.createStack(drained);
        final IAEFluidStack rem = fluids.injectItems(ins, Actionable.MODULATE, src);

        if (rem != null && rem.getStackSize() > 0) {
            player.sendStatusMessage(new TextComponentString(TextFormatting.RED + "Insert failed"), true);
            return Outcome.FAILED;
        }

        SoundEvent fill = drained.getFluid().getFillSound(drained);
        if (fill == null) fill = SoundEvents.ITEM_BUCKET_FILL;

        world.playSound(null, hit.getBlockPos(), fill, SoundCategory.PLAYERS, 0.8F, 1.0F);

        player.sendStatusMessage(
                new TextComponentString(TextFormatting.GREEN + "Stored " + MB_PER_USE + " mB of " + drained.getLocalizedName()),
                true
        );
        player.swingArm(hand);
        return Outcome.STORED;
    }


    private static final class SimpleActionSource implements IActionSource {
        private final EntityPlayer player;
        private final IActionHost host;

        private SimpleActionSource(EntityPlayer player, IActionHost host) {
            this.player = player;
            this.host = host;
        }

        @Override
        public Optional<EntityPlayer> player() {
            return Optional.ofNullable(player);
        }

        @Override
        public Optional<IActionHost> machine() {
            return Optional.ofNullable(host);
        }

        @Override
        public Optional<Object> context(final Class key) {
            return Optional.empty();
        }
    }
}
