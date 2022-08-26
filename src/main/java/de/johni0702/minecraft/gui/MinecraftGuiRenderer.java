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
package de.johni0702.minecraft.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import de.johni0702.minecraft.gui.utils.NonNull;
import de.johni0702.minecraft.gui.utils.lwjgl.Color;
import de.johni0702.minecraft.gui.utils.lwjgl.Point;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableColor;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadablePoint;
import de.johni0702.minecraft.gui.utils.lwjgl.WritableDimension;
import de.johni0702.minecraft.gui.versions.MCVer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import static com.mojang.blaze3d.systems.RenderSystem.*;
import static de.johni0702.minecraft.gui.versions.MCVer.getMinecraft;
import static de.johni0702.minecraft.gui.versions.MCVer.newScaledResolution;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;

public class MinecraftGuiRenderer implements GuiRenderer {

    private final DrawableHelper gui = new DrawableHelper(){};
    private final MinecraftClient mc = getMinecraft();

    private final MatrixStack matrixStack;

    @NonNull
    //#if MC>=11400
    private final int scaledWidth = newScaledResolution(mc).getScaledWidth();
    private final int scaledHeight = newScaledResolution(mc).getScaledHeight();
    private final double scaleFactor = newScaledResolution(mc).getScaleFactor();
    //#else
    //$$ private final int scaledWidth = newScaledResolution(mc).getScaledWidth();
    //$$ private final int scaledHeight = newScaledResolution(mc).getScaledHeight();
    //$$ private final double scaleFactor = newScaledResolution(mc).getScaleFactor();
    //#endif

    public MinecraftGuiRenderer(MatrixStack matrixStack) {
        this.matrixStack = matrixStack;
    }

    @Override
    public ReadablePoint getOpenGlOffset() {
        return new Point(0, 0);
    }

    @Override
    public MatrixStack getMatrixStack() {
        return matrixStack;
    }

    @Override
    public ReadableDimension getSize() {
        return new ReadableDimension() {
            @Override
            public int getWidth() {
                return scaledWidth;
            }

            @Override
            public int getHeight() {
                return scaledHeight;
            }

            @Override
            public void getSize(WritableDimension dest) {
                dest.setSize(getWidth(), getHeight());
            }
        };
    }

    @Override
    public void setDrawingArea(int x, int y, int width, int height) {
        // glScissor origin is bottom left corner whereas otherwise it's top left
        y = scaledHeight - y - height;

        int f = (int) scaleFactor;
        MCVer.setScissorBounds(x * f, y * f, width * f, height * f);
    }

    @Override
    public void bindTexture(Identifier location) {
        //#if MC>=11700
        RenderSystem.setShaderTexture(0, location);
        //#elseif MC>=11500
        //$$ MCVer.getMinecraft().getTextureManager().bindTexture(location);
        //#else
        //$$ MCVer.getMinecraft().getTextureManager().bindTexture(location);
        //#endif
    }

    @Override
    public void bindTexture(int glId) {
        //#if MC>=11700
        RenderSystem.setShaderTexture(0, glId);
        //#elseif MC>=10800
        //$$ GlStateManager.bindTexture(glId);
        //#else
        //$$ GL11.glBindTexture(GL_TEXTURE_2D, glId);
        //#endif
    }

    @Override
    public void drawTexturedRect(int x, int y, int u, int v, int width, int height) {
        //#if MC>=11600
        gui.drawTexture(matrixStack, x, y, u, v, width, height);
        //#else
        //#if MC>=11400
        //$$ gui.blit(x, y, u, v, width, height);
        //#else
        //$$ gui.drawTexturedModalRect(x, y, u, v, width, height);
        //#endif
        //#endif
    }

    @Override
    public void drawTexturedRect(int x, int y, int u, int v, int width, int height, int uWidth, int vHeight, int textureWidth, int textureHeight) {
        color(1, 1, 1);
        //#if MC>=11600
        DrawableHelper.drawTexture(matrixStack, x, y, width, height, u, v, uWidth, vHeight, textureWidth, textureHeight);
        //#else
        //#if MC>=11400
        //$$ DrawableHelper.blit(x, y, width, height, u, v, uWidth, vHeight, textureWidth, textureHeight);
        //#else
        //$$ Gui.drawScaledCustomSizeModalRect(x, y, u, v, uWidth, vHeight, width, height, textureWidth, textureHeight);
        //#endif
        //#endif
    }

    @Override
    public void drawRect(int x, int y, int width, int height, int color) {
        DrawableHelper.fill(
                //#if MC>=11600
                matrixStack,
                //#endif
                x, y, x + width, y + height, color);
        color(1, 1, 1);
        enableBlend();
    }

    @Override
    public void drawRect(int x, int y, int width, int height, ReadableColor color) {
        drawRect(x, y, width, height, color(color));
    }

    @Override
    public void drawRect(int x, int y, int width, int height, int topLeftColor, int topRightColor, int bottomLeftColor, int bottomRightColor) {
        drawRect(x, y, width, height, color(topLeftColor), color(topRightColor), color(bottomLeftColor), color(bottomRightColor));
    }

    @Override
    public void drawRect(int x, int y, int width, int height, ReadableColor tl, ReadableColor tr, ReadableColor bl, ReadableColor br) {
        disableTexture();
        enableBlend();
        blendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        //#if MC>=11700
        setShader(GameRenderer::getPositionColorShader);
        //#else
        //$$ disableAlphaTest();
        //$$ shadeModel(GL_SMOOTH);
        //#endif
        MCVer.drawRect(x, y, width, height, tl, tr, bl, br);
        //#if MC>=11700
        //#else
        //$$ shadeModel(GL_FLAT);
        //$$ enableAlphaTest();
        //#endif
        enableTexture();
    }

    @Override
    public int drawString(int x, int y, int color, String text) {
        return drawString(x, y, color, text, false);
    }

    @Override
    public int drawString(int x, int y, ReadableColor color, String text) {
        return drawString(x, y, color(color), text);
    }

    @Override
    public int drawCenteredString(int x, int y, int color, String text) {
        return drawCenteredString(x, y, color, text, false);
    }

    @Override
    public int drawCenteredString(int x, int y, ReadableColor color, String text) {
        return drawCenteredString(x, y, color(color), text);
    }

    @Override
    public int drawString(int x, int y, int color, String text, boolean shadow) {
        TextRenderer fontRenderer = MCVer.getFontRenderer();
        try {
            if (shadow) {
                return fontRenderer.drawWithShadow(
                        //#if MC>=11600
                        matrixStack,
                        //#endif
                        text, x, y, color);
            } else {
                return fontRenderer.draw(
                        //#if MC>=11600
                        matrixStack,
                        //#endif
                        text, x, y, color);
            }
        } finally {
            color(1, 1, 1);
        }
    }

    @Override
    public int drawString(int x, int y, ReadableColor color, String text, boolean shadow) {
        return drawString(x, y, color(color), text, shadow);
    }

    @Override
    public int drawCenteredString(int x, int y, int color, String text, boolean shadow) {
        TextRenderer fontRenderer = MCVer.getFontRenderer();
        x-=fontRenderer.getWidth(text) / 2;
        return drawString(x, y, color, text, shadow);
    }

    @Override
    public int drawCenteredString(int x, int y, ReadableColor color, String text, boolean shadow) {
        return drawCenteredString(x, y, color(color), text, shadow);
    }

    private int color(ReadableColor color) {
        return color.getAlpha() << 24
                | color.getRed() << 16
                | color.getGreen() << 8
                | color.getBlue();
    }

    private ReadableColor color(int color) {
        return new Color((color >> 16) & 0xff, (color >> 8) & 0xff, color & 0xff, (color >> 24) & 0xff);
    }

    private void color(float r, float g, float b) {
        //#if MC>=11700
        RenderSystem.setShaderColor(r, g, b, 1);
        //#else
        //#if MC>=10800
        //#if MC>=11400
        //$$ GlStateManager.color4f(r, g, b, 1);
        //#else
        //$$ GlStateManager.color(r, g, b);
        //#endif
        //#else
        //$$ MCVer.color(r, g, b);
        //#endif
        //#endif
    }

    @Override
    public void invertColors(int right, int bottom, int left, int top) {
        if (left >= right || top >= bottom) return;

        color(0, 0, 1);
        disableTexture();
        enableColorLogicOp();
        //#if MC>=11700
        logicOp(GlStateManager.LogicOp.OR_REVERSE);
        //#else
        //$$ logicOp(GL11.GL_OR_REVERSE);
        //#endif

        MCVer.drawRect(right, bottom, left, top);

        disableColorLogicOp();
        enableTexture();
        color(1, 1, 1);
    }
}
