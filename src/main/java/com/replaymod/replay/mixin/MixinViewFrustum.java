//#if MC>=10800
package com.replaymod.replay.mixin;

import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher.RenderChunk;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ViewArea.class)
public abstract class MixinViewFrustum {
    @Redirect(
            method = "updateCameraPosition",
            at = @At(
                    value = "INVOKE",
                    //#if MC>=10904
                    //#if MC>=11500
                    target = "Lnet/minecraft/client/render/chunk/ChunkBuilder$BuiltChunk;setOrigin(III)V"
                    //#else
                    //$$ target = "Lnet/minecraft/client/render/chunk/ChunkRenderer;setOrigin(III)V"
                    //#endif
                    //#else
                    //$$ target = "Lnet/minecraft/client/renderer/chunk/RenderChunk;setPosition(Lnet/minecraft/util/BlockPos;)V"
                    //#endif
            )
    )
    private void replayModReplay_updatePositionAndMarkForUpdate(
            //#if MC>=11500
            RenderChunk renderChunk,
            //#else
            //$$ ChunkRenderer renderChunk,
            //#endif
            //#if MC>=10904
            int x, int y, int z
            //#else
            //$$ BlockPos pos
            //#endif
    ) {
        //#if MC>=10904
        BlockPos pos = new BlockPos(x, y, z);
        //#endif
        if (!pos.equals(renderChunk.getOrigin())) {
            //#if MC>=10904
            renderChunk.setOrigin(x, y, z);
            renderChunk.setDirty(false);
            //#else
            //$$ renderChunk.setPosition(pos);
            //$$ renderChunk.setNeedsUpdate(true);
            //#endif
        }
    }
}
//#endif
