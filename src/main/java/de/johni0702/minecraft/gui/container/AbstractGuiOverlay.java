/*
 * This file is part of jGui API, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016 johni0702 <https://github.com/johni0702>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.johni0702.minecraft.gui.container;

import de.johni0702.minecraft.gui.GuiRenderer;
import de.johni0702.minecraft.gui.MinecraftGuiRenderer;
import de.johni0702.minecraft.gui.OffsetGuiRenderer;
import de.johni0702.minecraft.gui.RenderInfo;
import de.johni0702.minecraft.gui.element.GuiElement;
import de.johni0702.minecraft.gui.function.Clickable;
import de.johni0702.minecraft.gui.function.Closeable;
import de.johni0702.minecraft.gui.function.Draggable;
import de.johni0702.minecraft.gui.function.Loadable;
import de.johni0702.minecraft.gui.function.Scrollable;
import de.johni0702.minecraft.gui.function.Tickable;
import de.johni0702.minecraft.gui.function.Typeable;
import de.johni0702.minecraft.gui.utils.EventRegistrations;
import de.johni0702.minecraft.gui.utils.MouseUtils;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.Point;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadablePoint;
import de.johni0702.minecraft.gui.versions.MCVer;
import de.johni0702.minecraft.gui.versions.callbacks.PreTickCallback;
import de.johni0702.minecraft.gui.versions.callbacks.RenderHudCallback;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;

import static de.johni0702.minecraft.gui.versions.MCVer.literalText;
//#else
//$$ import org.lwjgl.input.Mouse;
//$$ import net.minecraft.client.gui.ScaledResolution;
//#endif

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;

//#if MC>=10800 && MC<11400
//$$ import java.io.IOException;
//#endif

public abstract class AbstractGuiOverlay<T extends AbstractGuiOverlay<T>> extends AbstractGuiContainer<T> {

    private final UserInputGuiScreen userInputGuiScreen = new UserInputGuiScreen();
    private final EventHandler eventHandler = new EventHandler();
    private boolean visible;
    private Dimension screenSize;
    private boolean mouseVisible;
    private boolean closeable = true;

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        if (this.visible != visible) {
            if (visible) {
                invokeAll(Loadable.class, Loadable::load);
                eventHandler.register();
            } else {
                invokeAll(Closeable.class, Closeable::close);
                eventHandler.unregister();
            }
            updateUserInputGui();
        }
        this.visible = visible;
    }

    public boolean isMouseVisible() {
        return mouseVisible;
    }

    public void setMouseVisible(boolean mouseVisible) {
        this.mouseVisible = mouseVisible;
        updateUserInputGui();
    }

    public boolean isCloseable() {
        return closeable;
    }

    public void setCloseable(boolean closeable) {
        this.closeable = closeable;
    }

    /**
     * @see #setAllowUserInput(boolean)
     */
    public boolean isAllowUserInput() {
        return userInputGuiScreen.passEvents;
    }

    /**
     * Enable/Disable user input for this overlay while the mouse is visible.
     * User input are things like moving the player, attacking/interacting, key bindings but not input into the
     * GUI elements such as text fields.
     * Default for overlays is {@code true} whereas for normal GUI screens it is {@code false}.
     * @param allowUserInput {@code true} to allow user input, {@code false} to disallow it
     * @see net.minecraft.client.gui.screens.Screen#passEvents
     */
    public void setAllowUserInput(boolean allowUserInput) {
        userInputGuiScreen.passEvents = allowUserInput;
    }

    private void updateUserInputGui() {
        Minecraft mc = getMinecraft();
        if (visible) {
            if (mouseVisible) {
                if (mc.screen == null) {
                    mc.setScreen(userInputGuiScreen);
                }
            } else {
                if (mc.screen == userInputGuiScreen) {
                    mc.setScreen(null);
                }
            }
        }
    }

    @Override
    public void layout(ReadableDimension size, RenderInfo renderInfo) {
        if (size == null) {
            size = screenSize;
        }
        super.layout(size, renderInfo);
        if (mouseVisible && renderInfo.layer == getMaxLayer()) {
            final GuiElement tooltip = forEach(GuiElement.class, e -> e.getTooltip(renderInfo));
            if (tooltip != null) {
                tooltip.layout(tooltip.getMinSize(), renderInfo);
            }
        }
    }

    @Override
    public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
        super.draw(renderer, size, renderInfo);
        if (mouseVisible && renderInfo.layer == getMaxLayer()) {
            final GuiElement tooltip = forEach(GuiElement.class, e -> e.getTooltip(renderInfo));
            if (tooltip != null) {
                final ReadableDimension tooltipSize = tooltip.getMinSize();
                int x, y;
                if (renderInfo.mouseX + 8 + tooltipSize.getWidth() < screenSize.getWidth()) {
                    x = renderInfo.mouseX + 8;
                } else {
                    x = screenSize.getWidth() - tooltipSize.getWidth() - 1;
                }
                if (renderInfo.mouseY + 8 + tooltipSize.getHeight() < screenSize.getHeight()) {
                    y = renderInfo.mouseY + 8;
                } else {
                    y = screenSize.getHeight() - tooltipSize.getHeight() - 1;
                }
                final ReadablePoint position = new Point(x, y);
                try {
                    OffsetGuiRenderer eRenderer = new OffsetGuiRenderer(renderer, position, tooltipSize);
                    tooltip.draw(eRenderer, tooltipSize, renderInfo);
                } catch (Exception ex) {
                    CrashReport crashReport = CrashReport.forThrowable(ex, "Rendering Gui Tooltip");
                    renderInfo.addTo(crashReport);
                    CrashReportCategory category = crashReport.addCategory("Gui container details");
                    MCVer.addDetail(category, "Container", this::toString);
                    MCVer.addDetail(category, "Width", () -> "" + size.getWidth());
                    MCVer.addDetail(category, "Height", () -> "" + size.getHeight());
                    category = crashReport.addCategory("Tooltip details");
                    MCVer.addDetail(category, "Element", tooltip::toString);
                    MCVer.addDetail(category, "Position", position::toString);
                    MCVer.addDetail(category, "Size", tooltipSize::toString);
                    throw new ReportedException(crashReport);
                }
            }
        }
    }

    @Override
    public ReadableDimension getMinSize() {
        return screenSize;
    }

    @Override
    public ReadableDimension getMaxSize() {
        return screenSize;
    }

    private class EventHandler extends EventRegistrations {
        private EventHandler() {}

        { on(RenderHudCallback.EVENT, this::renderOverlay); }
        private void renderOverlay(PoseStack stack, float partialTicks) {
            updateUserInputGui();
            updateRenderer();
            int layers = getMaxLayer();
            int mouseX = -1, mouseY = -1;
            if (mouseVisible) {
                Point mouse = MouseUtils.getMousePos();
                mouseX = mouse.getX();
                mouseY = mouse.getY();
            }
            RenderInfo renderInfo = new RenderInfo(partialTicks, mouseX, mouseY, 0);
            for (int layer = 0; layer <= layers; layer++) {
                layout(screenSize, renderInfo.layer(layer));
            }
            MinecraftGuiRenderer renderer = new MinecraftGuiRenderer(stack);
            for (int layer = 0; layer <= layers; layer++) {
                draw(renderer, screenSize, renderInfo.layer(layer));
            }
        }

        { on(PreTickCallback.EVENT, () -> invokeAll(Tickable.class, Tickable::tick)); }

        private void updateRenderer() {
            Minecraft mc = getMinecraft();
            //#if MC>=11400
            Window
            //#else
            //$$ ScaledResolution
            //#endif
                    res = MCVer.newScaledResolution(mc);
            if (screenSize == null
                    || screenSize.getWidth() != res.getGuiScaledWidth()
                    || screenSize.getHeight() != res.getGuiScaledHeight()) {
                screenSize = new Dimension(res.getGuiScaledWidth(), res.getGuiScaledHeight());
            }
        }
    }

    protected class UserInputGuiScreen extends net.minecraft.client.gui.screens.Screen {

        //#if MC>=11400
        UserInputGuiScreen() {
            super(literalText(""));
        }
        //#endif

        {
            this.passEvents = true;
        }

        //#if MC>=11400
        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            Point mousePos = MouseUtils.getMousePos();
            boolean controlDown = hasControlDown();
            boolean shiftDown = hasShiftDown();
            if (!invokeHandlers(Typeable.class, e -> e.typeKey(mousePos, keyCode, '\0', controlDown, shiftDown))) {
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
            return true;
        }

        @Override
        public boolean charTyped(char keyChar, int modifiers) {
            Point mousePos = MouseUtils.getMousePos();
            boolean controlDown = hasControlDown();
            boolean shiftDown = hasShiftDown();
            if (!invokeHandlers(Typeable.class, e -> e.typeKey(mousePos, 0, keyChar, controlDown, shiftDown))) {
                return super.charTyped(keyChar, modifiers);
            }
            return true;
        }
        //#else
        //$$ @Override
        //$$ protected void keyTyped(char typedChar, int keyCode)
                //#if MC>=10800
                //$$ throws IOException
                //#endif
        //$$ {
        //$$     Point mousePos = MouseUtils.getMousePos();
        //$$     boolean controlDown = isCtrlKeyDown();
        //$$     boolean shiftDown = isShiftKeyDown();
        //$$     invokeHandlers(Typeable.class, e -> e.typeKey(mousePos, keyCode, typedChar, controlDown, shiftDown));
        //$$     if (closeable) {
        //$$         super.keyTyped(typedChar, keyCode);
        //$$     }
        //$$ }
        //#endif

        @Override
        //#if MC>=11400
        public boolean mouseClicked(double mouseXD, double mouseYD, int mouseButton) {
            int mouseX = (int) Math.round(mouseXD), mouseY = (int) Math.round(mouseYD);
            return
        //#else
        //$$ protected void mouseClicked(int mouseX, int mouseY, int mouseButton)
                //#if MC>=10800
                //$$ throws IOException
                //#endif
        //$$ {
        //#endif
            invokeHandlers(Clickable.class, e -> e.mouseClick(new Point(mouseX, mouseY), mouseButton));
        }

        @Override
        //#if MC>=11400
        public boolean mouseReleased(double mouseXD, double mouseYD, int mouseButton) {
            int mouseX = (int) Math.round(mouseXD), mouseY = (int) Math.round(mouseYD);
            return
        //#else
        //$$ protected void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        //#endif
            invokeHandlers(Draggable.class, e -> e.mouseRelease(new Point(mouseX, mouseY), mouseButton));
        }

        @Override
        //#if MC>=11400
        public boolean mouseDragged(double mouseXD, double mouseYD, int mouseButton, double deltaX, double deltaY) {
            int mouseX = (int) Math.round(mouseXD), mouseY = (int) Math.round(mouseYD);
            long timeSinceLastClick = 0;
            return
        //#else
        //$$ protected void mouseClickMove(int mouseX, int mouseY, int mouseButton, long timeSinceLastClick) {
        //#endif
            invokeHandlers(Draggable.class, e -> e.mouseDrag(new Point(mouseX, mouseY), mouseButton, timeSinceLastClick));
        }

        @Override
        //#if MC>=11400
        public void tick() {
        //#else
        //$$ public void updateScreen() {
        //#endif
            invokeAll(Tickable.class, Tickable::tick);
        }

        //#if MC>=11400
        @Override
        public boolean mouseScrolled(
                //#if MC>=11400
                double mouseX,
                double mouseY,
                //#endif
                double dWheel
        ) {
            //#if MC>=11400
            Point mouse = new Point((int) mouseX, (int) mouseY);
            //#else
            //$$ Point mouse = MouseUtils.getMousePos();
            //#endif
            int wheel = (int) (dWheel * 120);
            return invokeHandlers(Scrollable.class, e -> e.scroll(mouse, wheel));
        }
        //#else
        //$$ @Override
        //$$ public void handleMouseInput()
                //#if MC>=10800
                //$$ throws IOException
                //#endif
        //$$ {
        //$$     super.handleMouseInput();
        //$$     if (Mouse.hasWheel() && Mouse.getEventDWheel() != 0) {
        //$$         Point mouse = MouseUtils.getMousePos();
        //$$         int wheel = Mouse.getEventDWheel();
        //$$         invokeHandlers(Scrollable.class, e -> e.scroll(mouse, wheel));
        //$$     }
        //$$ }
        //#endif

        //#if MC>=11400
        @Override
        public void onClose() {
            if (closeable) {
                super.onClose();
            }
        }
        //#endif

        @Override
        //#if MC>=11400
        public void removed() {
        //#else
        //$$ public void onGuiClosed() {
        //#endif
            if (closeable) {
                mouseVisible = false;
            }
        }

        public AbstractGuiOverlay<T> getOverlay() {
            return AbstractGuiOverlay.this;
        }
    }
}
