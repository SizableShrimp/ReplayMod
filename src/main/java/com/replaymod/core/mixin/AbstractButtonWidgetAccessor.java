//#if MC>=11400
package com.replaymod.core.mixin;

import net.minecraft.client.gui.widget.ClickableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClickableWidget.class)
public interface AbstractButtonWidgetAccessor {
    @Accessor
    int getHeight();
}
//#endif
