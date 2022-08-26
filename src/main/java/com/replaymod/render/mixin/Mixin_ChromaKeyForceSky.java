package com.replaymod.render.mixin;

import com.replaymod.render.hooks.EntityRendererHandler;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Forces the sky to always render when chroma keying is active. Ordinarily it only renders when the render distance is
 * at 4 or greater.
 */
//#if MC>=11500
@Mixin(LevelRenderer.class)
//#else
//$$ @Mixin(GameRenderer.class)
//#endif
public abstract class Mixin_ChromaKeyForceSky {
    @Shadow @Final private Minecraft client;

    //#if MC>=11500
    @ModifyConstant(method = "render", constant = @Constant(intValue = 4))
    //#elseif MC>=11400
    //$$ @ModifyConstant(method = "renderCenter", constant = @Constant(intValue = 4))
    //#else
    //$$ @ModifyConstant(method = "renderWorldPass", constant = @Constant(intValue = 4))
    //#endif
    private int forceSkyWhenChromaKeying(int value) {
        EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) this.client.gameRenderer).replayModRender_getHandler();
        if (handler != null) {
            ReadableColor color = handler.getSettings().getChromaKeyingColor();
            if (color != null) {
                return 0;
            }
        }
        return value;
    }
}
