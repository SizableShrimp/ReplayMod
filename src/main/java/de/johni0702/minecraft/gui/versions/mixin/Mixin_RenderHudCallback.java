//#if FABRIC>=1
package de.johni0702.minecraft.gui.versions.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import de.johni0702.minecraft.gui.versions.callbacks.RenderHudCallback;
import net.minecraft.client.gui.Gui;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class Mixin_RenderHudCallback {
    @Inject(
            method = "render",
            at = @At(
                    value = "FIELD",
                    opcode = Opcodes.GETFIELD,
                    target = "Lnet/minecraft/client/option/GameOptions;debugEnabled:Z"
            )
    )
    //#if MC>=11600
    private void renderOverlay(PoseStack stack, float partialTicks, CallbackInfo ci) {
    //#else
    //$$ private void renderOverlay(float partialTicks, CallbackInfo ci) {
    //$$     MatrixStack stack = new MatrixStack();
    //#endif
        RenderHudCallback.EVENT.invoker().renderHud(stack, partialTicks);
    }
}
//#endif
