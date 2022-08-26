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
package de.johni0702.minecraft.gui.layout;

import de.johni0702.minecraft.gui.container.GuiContainer;
import de.johni0702.minecraft.gui.element.GuiElement;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.Point;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadablePoint;
import org.apache.commons.lang3.tuple.Pair;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class HorizontalLayout implements Layout {
    private static final Data DEFAULT_DATA = new Data(0);

    private final Alignment alignment;

    private int spacing;

    public HorizontalLayout() {
        this(Alignment.LEFT);
    }

    public HorizontalLayout(Alignment alignment) {
        this.alignment = alignment;
    }

    @Override
    public Map<GuiElement, Pair<ReadablePoint, ReadableDimension>> layOut(GuiContainer<?> container, ReadableDimension size) {
        int x = 0;
        int spacing = 0;
        Map<GuiElement, Pair<ReadablePoint, ReadableDimension>> map = new LinkedHashMap<>();
        for (Map.Entry<GuiElement, LayoutData> entry : container.getElements().entrySet()) {
            x += spacing;
            spacing = this.spacing;

            GuiElement element  = entry.getKey();
            Data data = entry.getValue() instanceof Data ? (Data) entry.getValue() : DEFAULT_DATA;
            Dimension elementSize = new Dimension(element.getMinSize());
            ReadableDimension elementMaxSize = element.getMaxSize();
            elementSize.setWidth(Math.min(size.getWidth() - x, Math.min(elementSize.getWidth(), elementMaxSize.getWidth())));
            elementSize.setHeight(Math.min(size.getHeight(), elementMaxSize.getHeight()));
            int remainingHeight = size.getHeight() - elementSize.getHeight();
            int y = (int) (data.alignment * remainingHeight);
            map.put(element, Pair.<ReadablePoint, ReadableDimension>of(new Point(x, y), elementSize));
            x += elementSize.getWidth();
        }
        if (alignment != Alignment.LEFT) {
            int remaining = size.getWidth() - x;
            if (alignment == Alignment.CENTER) {
                remaining /= 2;
            }
            for (Pair<ReadablePoint, ReadableDimension> pair : map.values()) {
                ((Point) pair.getLeft()).translate(remaining, 0);
            }
        }
        return map;
    }

    @Override
    public ReadableDimension calcMinSize(GuiContainer<?> container) {
        int maxHeight = 0;
        int width = 0;
        int spacing = 0;
        for (Map.Entry<GuiElement, LayoutData> entry : container.getElements().entrySet()) {
            width += spacing;
            spacing = this.spacing;

            GuiElement element = entry.getKey();
            ReadableDimension minSize = element.getMinSize();
            int height = minSize.getHeight();
            if (height > maxHeight) {
                maxHeight = height;
            }
            width += minSize.getWidth();
        }
        return new Dimension(width, maxHeight);
    }

    public int getSpacing() {
        return this.spacing;
    }

    public HorizontalLayout setSpacing(int spacing) {
        this.spacing = spacing;
        return this;
    }

    public static class Data implements LayoutData {
        private double alignment;

        public Data() {
            this(0);
        }

        public Data(double alignment) {
            this.alignment = alignment;
        }

        public double getAlignment() {
            return this.alignment;
        }

        public void setAlignment(double alignment) {
            this.alignment = alignment;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Data data = (Data) o;
            return Double.compare(data.alignment, alignment) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(alignment);
        }

        @Override
        public String toString() {
            return "Data{" +
                    "alignment=" + alignment +
                    '}';
        }
    }

    public enum Alignment {
        LEFT, RIGHT, CENTER
    }
}
