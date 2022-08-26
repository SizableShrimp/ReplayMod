package com.replaymod.render.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.replaymod.render.hooks.ForceChunkLoadingHook;
import com.replaymod.render.hooks.IForceChunkLoading;
import com.replaymod.render.utils.FlawlessFrames;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.culling.Frustum;

@Mixin(LevelRenderer.class)
public abstract class Mixin_ForceChunkLoading implements IForceChunkLoading {
    private ForceChunkLoadingHook replayModRender_hook;

    @Override
    public void replayModRender_setHook(ForceChunkLoadingHook hook) {
        this.replayModRender_hook = hook;
    }

    @Shadow private ChunkRenderDispatcher chunkBuilder;

    @Shadow protected abstract void setupTerrain(Camera par1, Frustum par2, boolean par3, boolean par4);

    @Shadow private Frustum frustum;

    @Shadow private Frustum capturedFrustum;

    @Shadow @Final private Minecraft client;

    @Shadow @Final private ObjectArrayList<ChunkInfoAccessor> chunkInfos;

    @Shadow private boolean shouldUpdate;

    @Shadow @Final private BlockingQueue<ChunkRenderDispatcher.RenderChunk> builtChunks;

    @Shadow private Future<?> fullUpdateFuture;

    @Shadow @Final private AtomicBoolean updateFinished;

    @Shadow protected abstract void applyFrustum(Frustum par1);

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;setupTerrain(Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/Frustum;ZZ)V"))
    private void forceAllChunks(PoseStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightmapTextureManager, Matrix4f matrix4f, CallbackInfo ci) {
        if (replayModRender_hook == null) {
            return;
        }
        if (FlawlessFrames.hasSodium()) {
            return;
        }

        assert this.client.player != null;

        RenderRegionCache chunkRendererRegionBuilder = new RenderRegionCache();

        do {
            // Determine which chunks shall be visible
            setupTerrain(camera, this.frustum, this.capturedFrustum != null, this.client.player.isSpectator());

            // Wait for async processing to be complete
            if (this.fullUpdateFuture != null) {
                try {
                    this.fullUpdateFuture.get(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                } catch (TimeoutException e) {
                    e.printStackTrace();
                }
            }

            // If that async processing did change the chunk graph, we need to re-apply the frustum (otherwise this is
            // only done in the next setupTerrain call, which not happen this frame)
            if (this.updateFinished.compareAndSet(true, false)) {
                this.applyFrustum((new Frustum(frustum)).offsetToFullyIncludeCameraCube(8)); // call based on the one in setupTerrain
            }

            // Schedule all chunks which need rebuilding (we schedule even important rebuilds because we wait for
            // all of them anyway and this way we can take advantage of threading)
            for (ChunkInfoAccessor chunkInfo : this.chunkInfos) {
                ChunkRenderDispatcher.RenderChunk builtChunk = chunkInfo.getChunk();
                if (!builtChunk.isDirty()) {
                    continue;
                }
                // MC sometimes schedules invalid chunks when you're outside of loaded chunks (e.g. y > 256)
                if (builtChunk.hasAllNeighbors()) {
                    builtChunk.rebuildChunkAsync(this.chunkBuilder, chunkRendererRegionBuilder);
                }
                builtChunk.setNotDirty();
            }

            // Upload all chunks
            this.shouldUpdate |= ((ForceChunkLoadingHook.IBlockOnChunkRebuilds) this.chunkBuilder).uploadEverythingBlocking();

            // Repeat until no more updates are needed
        } while (this.shouldUpdate || !this.builtChunks.isEmpty());
    }
}
