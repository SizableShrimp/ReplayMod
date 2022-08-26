//#if MC>=10904
package com.replaymod.render.blend.mixin;

import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.renderer.entity.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemRenderer.class)
public interface ItemRendererAccessor {
    @Accessor("colors")
    ItemColors getItemColors();
}
//#endif
