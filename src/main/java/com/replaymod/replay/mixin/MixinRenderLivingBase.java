package com.replaymod.replay.mixin;

import com.replaymod.replay.camera.CameraEntity;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.replaymod.core.versions.MCVer.*;

//#if MC>=10904
@Mixin(LivingEntityRenderer.class)
//#else
//$$ @Mixin(RendererLivingEntity.class)
//#endif
public abstract class MixinRenderLivingBase {
    //#if FABRIC>=1
    @Inject(method = "hasLabel", at = @At("HEAD"), cancellable = true)
    //#else
    //$$ @Inject(method = "canRenderName(Lnet/minecraft/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    //#endif
    private void replayModReplay_canRenderInvisibleName(LivingEntity entity, CallbackInfoReturnable<Boolean> ci) {
        Player thePlayer = getMinecraft().player;
        if (thePlayer instanceof CameraEntity && entity.isInvisible()) {
            ci.setReturnValue(false);
        }
    }

    @Redirect(
            //#if MC>=11500
            method = "render",
            //#else
            //$$ method = "render(Lnet/minecraft/entity/LivingEntity;FFFFFF)V",
            //#endif
            at = @At(
                    value = "INVOKE",
                    //#if MC>=11400
                    target = "Lnet/minecraft/entity/LivingEntity;isInvisibleTo(Lnet/minecraft/entity/player/PlayerEntity;)Z"
                    //#else
                    //$$ target = "Lnet/minecraft/entity/EntityLivingBase;isInvisibleToPlayer(Lnet/minecraft/entity/player/EntityPlayer;)Z"
                    //#endif
            )
    )
    private boolean replayModReplay_shouldInvisibleNotBeRendered(LivingEntity entity, Player thePlayer) {
        return thePlayer instanceof CameraEntity || entity.isInvisibleTo(thePlayer);
    }
}
