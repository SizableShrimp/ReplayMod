package com.replaymod.replay.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ClientLevel.class)
public abstract class MixinWorldClient {
    // Looks like this has finally been fixed in 1.14 (or been moved somewhere else entirely, guess we'll find out)
    // 1.12.2 and below
}
