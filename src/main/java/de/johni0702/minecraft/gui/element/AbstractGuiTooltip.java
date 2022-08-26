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
package de.johni0702.minecraft.gui.element;

import de.johni0702.minecraft.gui.GuiRenderer;
import de.johni0702.minecraft.gui.RenderInfo;
import de.johni0702.minecraft.gui.utils.StringUtils;
import de.johni0702.minecraft.gui.utils.lwjgl.Color;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableColor;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import de.johni0702.minecraft.gui.versions.MCVer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.resource.language.I18n;

public abstract class AbstractGuiTooltip<T extends AbstractGuiTooltip<T>> extends AbstractGuiElement<T> {
    private static final int LINE_SPACING = 3;
    private static final ReadableColor BACKGROUND_COLOR = new Color(16, 0, 16, 240);
    private static final ReadableColor BORDER_LIGHT = new Color(80, 0, 255, 80);
    private static final ReadableColor BORDER_DARK = new Color(40, 0, 127, 80);

    private String[] text = {};

    private ReadableColor color = ReadableColor.WHITE;

    @Override
    public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
        super.draw(renderer, size, renderInfo);

        int width = size.getWidth();
        int height = size.getHeight();

        // Draw background
        renderer.drawRect(1, 0, width - 2, height, BACKGROUND_COLOR); // Top to bottom
        renderer.drawRect(0, 1, 1, height - 2, BACKGROUND_COLOR); // Left pixel row
        renderer.drawRect(width - 1, 1, 1, height - 2, BACKGROUND_COLOR); // Right pixel row

        // Draw the border, it gets darker from top to bottom
        renderer.drawRect(1, 1, width - 2, 1, BORDER_LIGHT); // Top border
        renderer.drawRect(1, height - 2, width - 2, 1, BORDER_DARK); // Bottom border
        renderer.drawRect(1, 2, 1, height - 4, BORDER_LIGHT, BORDER_LIGHT, BORDER_DARK, BORDER_DARK); // Left border
        renderer.drawRect(width - 2, 2, 1, height - 4, BORDER_LIGHT, BORDER_LIGHT, BORDER_DARK, BORDER_DARK); // Right border

        TextRenderer fontRenderer = MCVer.getFontRenderer();
        int y = LINE_SPACING + 1;
        for (String line : text) {
            renderer.drawString(LINE_SPACING + 1, y, color, line, true);
            y += fontRenderer.fontHeight + LINE_SPACING;
        }
    }

    @Override
    public ReadableDimension calcMinSize() {
        TextRenderer fontRenderer = MCVer.getFontRenderer();
        int height = 1 + LINE_SPACING + text.length * (fontRenderer.fontHeight + LINE_SPACING);
        int width = 0;
        for (String line : text) {
            int w = fontRenderer.getWidth(line);
            if (w > width) {
                width = w;
            }
        }
        width+=4 * 2;
        return new Dimension(width, height);
    }

    @Override
    public ReadableDimension getMaxSize() {
        return getMinSize();
    }

    public T setText(String[]text) {
        this.text = text;
        return getThis();
    }

    public T setText(String text) {
        return setText(StringUtils.splitStringInMultipleRows(text, 250));
    }

    public T setI18nText(String text, Object... args) {
        return setText(I18n.translate(text, args));
    }

    public T setColor(ReadableColor color) {
        this.color = color;
        return getThis();
    }

    public String[] getText() {
        return this.text;
    }

    public ReadableColor getColor() {
        return this.color;
    }
}
