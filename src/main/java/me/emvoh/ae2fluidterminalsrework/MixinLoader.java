package me.emvoh.ae2fluidterminalsrework;

import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.jetbrains.annotations.Nullable;
import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@IFMLLoadingPlugin.MCVersion(ForgeVersion.mcVersion)
@IFMLLoadingPlugin.Name("AE2-FluidTerminals-Rework")
@Optional.Interface(iface = "zone.rong.mixinbooter.ILateMixinLoader", modid = "mixinbooter")
public class MixinLoader implements IFMLLoadingPlugin, ILateMixinLoader {

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public @Nullable String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {

    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    @Override
    @Optional.Method(modid = "mixinbooter")
    public List<String> getMixinConfigs() {
        return Collections.singletonList("mixins.ae2fluidterminalsrework.json");
    }
}
