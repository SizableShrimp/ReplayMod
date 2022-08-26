package com.replaymod.render.mixin;

import com.replaymod.render.RenderSettings;
import com.replaymod.render.hooks.EntityRendererHandler;
import com.replaymod.replay.camera.CameraEntity;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.replaymod.core.versions.MCVer.*;

//#if MC>=11400
@Mixin(value = Camera.class)
//#else
//$$ @Mixin(value = EntityRenderer.class)
//#endif
public abstract class Mixin_StabilizeCamera {
    private EntityRendererHandler getHandler() {
        return ((EntityRendererHandler.IEntityRenderer) getMinecraft().gameRenderer).replayModRender_getHandler();
    }

    private float orgYaw;
    private float orgPitch;
    private float orgPrevYaw;
    private float orgPrevPitch;
    private float orgRoll;

    // Only relevant on 1.13+ (previously MC always used the non-head yaw) and only for LivingEntity view entities.
    private float orgHeadYaw;
    private float orgPrevHeadYaw;

    //#if MC>=11400
    @Inject(method = "update", at = @At("HEAD"))
    //#else
    //$$ @Inject(method = "setupCameraTransform", at = @At("HEAD"))
    //#endif
    private void replayModRender_beforeSetupCameraTransform(
            //#if MC>=11400
            BlockGetter blockView,
            Entity entity,
            boolean thirdPerson,
            boolean inverseView,
            //#endif
            float partialTicks,
            //#if MC<11400
            //$$ int renderPass,
            //#endif
            CallbackInfo ci
    ) {
        if (getHandler() != null) {
            //#if MC<11400
            //$$ Entity entity = getMinecraft().getRenderViewEntity();
            //#endif
            orgYaw = entity.getYRot();
            orgPitch = entity.getXRot();
            orgPrevYaw = entity.yRotO;
            orgPrevPitch = entity.xRotO;
            orgRoll = entity instanceof CameraEntity ? ((CameraEntity) entity).roll : 0;
            if (entity instanceof LivingEntity) {
                orgHeadYaw = ((LivingEntity) entity).yHeadRot;
                orgPrevHeadYaw = ((LivingEntity) entity).yHeadRotO;
            }
        }
    //#if MC<11400
    //$$ }
    //$$
    //$$ @Inject(method = "orientCamera", at = @At("HEAD"))
    //$$ private void replayModRender_resetRotationIfNeeded(float partialTicks, CallbackInfo ci) {
    //#endif
        if (getHandler() != null) {
            //#if MC<11400
            //$$ Entity entity = getMinecraft().getRenderViewEntity();
            //#endif
            RenderSettings settings = getHandler().getSettings();
            if (settings.isStabilizeYaw()) {
                entity.yRotO = 0;
                entity.setYRot(0);
                if (entity instanceof LivingEntity) {
                    ((LivingEntity) entity).yHeadRotO = ((LivingEntity) entity).yHeadRot = 0;
                }
            }
            if (settings.isStabilizePitch()) {
                entity.xRotO = 0;
                entity.setXRot(0);
            }
            if (settings.isStabilizeRoll() && entity instanceof CameraEntity) {
                ((CameraEntity) entity).roll = 0;
            }
        }
    }

    //#if MC>=11400
    @Inject(method = "update", at = @At("RETURN"))
    //#else
    //$$ @Inject(method = "setupCameraTransform", at = @At("RETURN"))
    //#endif
    private void replayModRender_afterSetupCameraTransform(
            //#if MC>=11400
            BlockGetter blockView,
            Entity entity,
            boolean thirdPerson,
            boolean inverseView,
            //#endif
            float partialTicks,
            //#if MC<11400
            //$$ int renderPass,
            //#endif
            CallbackInfo ci
    ) {
        if (getHandler() != null) {
            //#if MC<11400
            //$$ Entity entity = getMinecraft().getRenderViewEntity();
            //#endif
            entity.setYRot(orgYaw);
            entity.setXRot(orgPitch);
            entity.yRotO = orgPrevYaw;
            entity.xRotO = orgPrevPitch;
            if (entity instanceof CameraEntity) {
                ((CameraEntity) entity).roll = orgRoll;
            }
            if (entity instanceof LivingEntity) {
                ((LivingEntity) entity).yHeadRot = orgHeadYaw;
                ((LivingEntity) entity).yHeadRotO = orgPrevHeadYaw;
            }
        }
    }
}
