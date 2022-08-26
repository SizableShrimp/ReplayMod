package com.replaymod.replay.mixin;

import com.replaymod.extras.advancedscreenshots.AdvancedScreenshots;
import com.replaymod.replay.ReplayModReplay;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=11400
@Mixin(KeyboardHandler.class)
//#else
//$$ @Mixin(Minecraft.class)
//#endif
public abstract class MixinKeyboardListener {
    @Inject(
            //#if MC>=11400
            method = "onKey",
            //#else
            //$$ method = "dispatchKeypresses",
            //#endif
            at = @At(
                    value = "INVOKE",
                    //#if MC>=11701
                    target = "Lnet/minecraft/client/util/ScreenshotRecorder;saveScreenshot(Ljava/io/File;Lnet/minecraft/client/gl/Framebuffer;Ljava/util/function/Consumer;)V"
                    //#elseif MC>=11400
                    //$$ target = "Lnet/minecraft/client/util/ScreenshotRecorder;saveScreenshot(Ljava/io/File;IILnet/minecraft/client/gl/Framebuffer;Ljava/util/function/Consumer;)V"
                    //#else
                    //#if MC>=11400
                    //$$ target = "Lnet/minecraft/util/ScreenShotHelper;saveScreenshot(Ljava/io/File;IILnet/minecraft/client/shader/Framebuffer;Ljava/util/function/Consumer;)V"
                    //#else
                    //$$ target = "Lnet/minecraft/util/ScreenShotHelper;saveScreenshot(Ljava/io/File;IILnet/minecraft/client/shader/Framebuffer;)Lnet/minecraft/util/text/ITextComponent;"
                    //#endif
                    //#endif
            ),
            cancellable = true
    )
    private void takeScreenshot(CallbackInfo ci) {
        if (ReplayModReplay.instance.getReplayHandler() != null) {
            AdvancedScreenshots.take();
            ci.cancel();
        }
    }
}
