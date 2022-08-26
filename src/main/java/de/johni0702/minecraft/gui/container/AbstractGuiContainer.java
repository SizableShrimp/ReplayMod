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
import de.johni0702.minecraft.gui.OffsetGuiRenderer;
import de.johni0702.minecraft.gui.RenderInfo;
import de.johni0702.minecraft.gui.element.AbstractComposedGuiElement;
import de.johni0702.minecraft.gui.element.ComposedGuiElement;
import de.johni0702.minecraft.gui.element.GuiElement;
import de.johni0702.minecraft.gui.layout.HorizontalLayout;
import de.johni0702.minecraft.gui.layout.Layout;
import de.johni0702.minecraft.gui.layout.LayoutData;
import de.johni0702.minecraft.gui.utils.lwjgl.Point;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableColor;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadablePoint;
import de.johni0702.minecraft.gui.versions.MCVer;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;

public abstract class AbstractGuiContainer<T extends AbstractGuiContainer<T>>
        extends AbstractComposedGuiElement<T> implements GuiContainer<T> {

    private static final Layout DEFAULT_LAYOUT = new HorizontalLayout();

    private Map<GuiElement, LayoutData> elements = new LinkedHashMap<>();

    private Map<GuiElement, Pair<ReadablePoint, ReadableDimension>> layedOutElements;

    private Layout layout = DEFAULT_LAYOUT;

    private ReadableColor backgroundColor;

    public AbstractGuiContainer() {
    }

    public AbstractGuiContainer(GuiContainer container) {
        super(container);
    }

    @Override
    public T setLayout(Layout layout) {
        this.layout = layout;
        return getThis();
    }

    @Override
    public Layout getLayout() {
        return layout;
    }

    @Override
    public void convertFor(GuiElement element, Point point) {
        convertFor(element, point, element.getLayer());
    }

    @Override
    public void convertFor(GuiElement element, Point point, int relativeLayer) {
        if (layedOutElements == null || !layedOutElements.containsKey(element)) {
            layout(null, new RenderInfo(0, 0, 0, relativeLayer));
        }
        checkState(layedOutElements != null, "Cannot convert position unless rendered at least once.");
        Pair<ReadablePoint, ReadableDimension> pair = layedOutElements.get(element);
        checkState(pair != null, "Element " + element + " not part of " + this);
        ReadablePoint pos = pair.getKey();
        if (getContainer() != null) {
            getContainer().convertFor(this, point, relativeLayer + getLayer());
        }
        point.translate(-pos.getX(), -pos.getY());
    }

    @Override
    public Collection<GuiElement> getChildren() {
        return Collections.unmodifiableCollection(elements.keySet());
    }

    @Override
    public Map<GuiElement, LayoutData> getElements() {
        return Collections.unmodifiableMap(elements);
    }

    @Override
    public T addElements(LayoutData layoutData, GuiElement... elements) {
        if (layoutData == null) {
            layoutData = LayoutData.NONE;
        }
        for (GuiElement element : elements) {
            this.elements.put(element, layoutData);
            element.setContainer(this);
        }
        return getThis();
    }

    @Override
    public T removeElement(GuiElement element) {
        if (elements.remove(element) != null) {
            element.setContainer(null);
            if (layedOutElements != null) {
                layedOutElements.remove(element);
            }
        }
        return getThis();
    }

    @Override
    public void layout(ReadableDimension size, RenderInfo renderInfo) {
        super.layout(size, renderInfo);
        if (size == null) return;
        try {
            layedOutElements = layout.layOut(this, size);
        } catch (Exception ex) {
            CrashReport crashReport = CrashReport.create(ex, "Gui Layout");
            renderInfo.addTo(crashReport);
            CrashReportSection category = crashReport.addElement("Gui container details");
            MCVer.addDetail(category, "Container", this::toString);
            MCVer.addDetail(category, "Layout", layout::toString);
            throw new CrashException(crashReport);
        }
        for (final Map.Entry<GuiElement, Pair<ReadablePoint, ReadableDimension>> e : layedOutElements.entrySet()) {
            GuiElement element = e.getKey();
            if (element instanceof ComposedGuiElement) {
                if (((ComposedGuiElement) element).getMaxLayer() < renderInfo.layer) {
                    continue;
                }
            } else {
                if (element.getLayer() != renderInfo.layer) {
                    continue;
                }
            }
            ReadablePoint ePosition = e.getValue().getLeft();
            ReadableDimension eSize = e.getValue().getRight();
            element.layout(eSize, renderInfo.offsetMouse(ePosition.getX(), ePosition.getY())
                    .layer(renderInfo.getLayer() - element.getLayer()));
        }
    }

    @Override
    public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
        super.draw(renderer, size, renderInfo);
        if (backgroundColor != null && renderInfo.getLayer() == 0) {
            renderer.drawRect(0, 0, size.getWidth(), size.getHeight(), backgroundColor);
        }
        for (final Map.Entry<GuiElement, Pair<ReadablePoint, ReadableDimension>> e : layedOutElements.entrySet()) {
            GuiElement element = e.getKey();
            boolean strict;
            if (element instanceof ComposedGuiElement) {
                if (((ComposedGuiElement) element).getMaxLayer() < renderInfo.layer) {
                    continue;
                }
                strict = renderInfo.layer == 0;
            } else {
                if (element.getLayer() != renderInfo.layer) {
                    continue;
                }
                strict = true;
            }
            final ReadablePoint ePosition = e.getValue().getLeft();
            final ReadableDimension eSize = e.getValue().getRight();
            try {
                OffsetGuiRenderer eRenderer = new OffsetGuiRenderer(renderer, ePosition, eSize, strict);
                eRenderer.startUsing();
                e.getKey().draw(eRenderer, eSize, renderInfo.offsetMouse(ePosition.getX(), ePosition.getY())
                        .layer(renderInfo.getLayer() - e.getKey().getLayer()));
                eRenderer.stopUsing();
            } catch (Exception ex) {
                CrashReport crashReport = CrashReport.create(ex, "Rendering Gui");
                renderInfo.addTo(crashReport);
                CrashReportSection category = crashReport.addElement("Gui container details");
                MCVer.addDetail(category, "Container", this::toString);
                MCVer.addDetail(category, "Width", () -> "" + size.getWidth());
                MCVer.addDetail(category, "Height", () -> "" + size.getHeight());
                MCVer.addDetail(category, "Layout", layout::toString);
                category = crashReport.addElement("Gui element details");
                MCVer.addDetail(category, "Element", () -> e.getKey().toString());
                MCVer.addDetail(category, "Position", ePosition::toString);
                MCVer.addDetail(category, "Size", eSize::toString);
                if (e.getKey() instanceof GuiContainer) {
                    MCVer.addDetail(category, "Layout", () -> ((GuiContainer) e.getKey()).getLayout().toString());
                }
                throw new CrashException(crashReport);
            }
        }
    }

    @Override
    public ReadableDimension calcMinSize() {
        return layout.calcMinSize(this);
    }

    @Override
    public T sortElements() {
        sortElements(new Comparator<GuiElement>() {
            @SuppressWarnings("unchecked")
            @Override
            public int compare(GuiElement o1, GuiElement o2) {
                if (o1 instanceof Comparable && o2 instanceof Comparable) {
                    return ((Comparable) o1).compareTo(o2);
                }
                return o1.hashCode() - o2.hashCode();
            }
        });
        return getThis();
    }

    @Override
    public T sortElements(final Comparator<GuiElement> comparator) {
        elements = elements.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey(comparator))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> y, LinkedHashMap::new));
        return getThis();
    }

    @Override
    public ReadableColor getBackgroundColor() {
        return backgroundColor;
    }

    @Override
    public T setBackgroundColor(ReadableColor backgroundColor) {
        this.backgroundColor = backgroundColor;
        return getThis();
    }
}
