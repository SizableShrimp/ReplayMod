package com.replaymod.replay.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.replaymod.replay.camera.CameraEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinCamera {
    @Shadow @Final private Minecraft client;
    @Inject(
            method = "renderWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/Camera;getPitch()F"
            )
    )
    private void applyRoll(float float_1, long long_1, PoseStack matrixStack, CallbackInfo ci) {
        Entity entity = this.client.getCameraEntity() == null ? this.client.player : this.client.getCameraEntity();
        if (entity instanceof CameraEntity) {
            matrixStack.mulPose(Vector3f.ZP.rotationDegrees(((CameraEntity) entity).roll));
        }
    }
}
