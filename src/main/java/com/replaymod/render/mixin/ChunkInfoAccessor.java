// 1.18+
package com.replaymod.render.mixin;

import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.render.WorldRenderer$ChunkInfo")
public interface ChunkInfoAccessor {
    @Accessor
    ChunkRenderDispatcher.RenderChunk getChunk();
}
