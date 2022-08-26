package com.replaymod.core.utils;
import com.replaymod.replaystudio.data.ModInfo;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

class ModInfoGetter {
    static Collection<ModInfo> getInstalledNetworkMods() {
        Map<String, ModInfo> modInfoMap = FabricLoader.getInstance().getAllMods().stream()
                .map(ModContainer::getMetadata)
                .map(m -> new ModInfo(m.getId(), m.getName(), m.getVersion().toString()))
                .collect(Collectors.toMap(ModInfo::getId, Function.identity()));
        return Registry.REGISTRY.stream()
                .map(Registry::keySet).flatMap(Set::stream)
                .map(ResourceLocation::getNamespace).filter(s -> !s.equals("minecraft")).distinct()
                .map(modInfoMap::get).filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
