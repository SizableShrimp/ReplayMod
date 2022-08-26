//#if FABRIC>=1
package de.johni0702.minecraft.gui.versions.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import de.johni0702.minecraft.gui.versions.callbacks.PostRenderScreenCallback;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    //#if MC>=11600
                    target = "Lnet/minecraft/client/gui/screen/Screen;render(Lnet/minecraft/client/util/math/MatrixStack;IIF)V",
                    //#else
                    //$$ target = "Lnet/minecraft/client/gui/screen/Screen;render(IIF)V",
                    //#endif
                    shift = At.Shift.AFTER
            )
    )
    private void postRenderScreen(float partialTicks, long nanoTime, boolean renderWorld, CallbackInfo ci) {
        PoseStack stack = new PoseStack();
        PostRenderScreenCallback.EVENT.invoker().postRenderScreen(stack, partialTicks);
    }
}
//#endif
