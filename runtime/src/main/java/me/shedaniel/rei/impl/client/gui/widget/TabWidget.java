/*
 * This file is licensed under the MIT License, part of Roughly Enough Items.
 * Copyright (c) 2018, 2019, 2020, 2021, 2022, 2023 shedaniel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.shedaniel.rei.impl.client.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import me.shedaniel.clothconfig2.api.animator.NumberAnimator;
import me.shedaniel.clothconfig2.api.animator.ValueAnimator;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.ClientHelper;
import me.shedaniel.rei.api.client.REIRuntime;
import me.shedaniel.rei.api.client.config.ConfigObject;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.drag.DraggableStack;
import me.shedaniel.rei.api.client.gui.drag.DraggableStackProviderWidget;
import me.shedaniel.rei.api.client.gui.drag.DraggedAcceptorResult;
import me.shedaniel.rei.api.client.gui.drag.DraggingContext;
import me.shedaniel.rei.api.client.gui.widgets.CloseableScissors;
import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.WidgetWithBounds;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.impl.client.gui.InternalTextures;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

@ApiStatus.Internal
public class TabWidget extends WidgetWithBounds implements DraggableStackProviderWidget {
    public boolean selected = false;
    public Renderer renderer;
    public Component categoryName;
    public Rectangle bounds;
    public DisplayCategory<?> category;
    public int u, v;
    public float opacity = 1.0F;
    @Nullable
    private Predicate<TabWidget> onClick;
    private final NumberAnimator<Float> darkBackgroundAlpha = ValueAnimator.ofFloat()
            .withConvention(() -> REIRuntime.getInstance().isDarkThemeEnabled() ? 1.0F : 0.0F, ValueAnimator.typicalTransitionTime())
            .asFloat();
    
    private TabWidget(Rectangle bounds, int u, int v, @Nullable Predicate<TabWidget> onClick) {
        this.bounds = bounds;
        this.u = u;
        this.v = v;
        this.onClick = onClick;
    }
    
    @ApiStatus.Internal
    public static TabWidget create(int id, int tabSize, int leftX, int bottomY, int u, int v, @Nullable Predicate<TabWidget> onClick) {
        return new TabWidget(new Rectangle(leftX + id * tabSize, bottomY - tabSize, tabSize, tabSize), u, v, onClick);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return button == 0 && containsMouse(mouseX, mouseY) && onClick != null && onClick.test(this);
    }
    
    public void setRenderer(DisplayCategory<?> category, Renderer renderer, Component categoryName, boolean selected) {
        this.renderer = renderer;
        this.category = category;
        this.selected = selected;
        this.categoryName = categoryName;
    }
    
    public boolean isSelected() {
        return selected;
    }
    
    public boolean isShown() {
        return renderer != null;
    }
    
    @Override
    public List<Widget> children() {
        return Collections.emptyList();
    }
    
    @Override
    public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
        if (bounds.getMaxX() < 0 || opacity == 0) return;
        if (renderer != null) {
            try (CloseableScissors scissors = Widget.scissor(matrices, new Rectangle(bounds.x, bounds.y + 2, bounds.width, (selected ? bounds.height + 2 : bounds.height - 2)))) {
                darkBackgroundAlpha.update(delta);
                RenderSystem.enableBlend();
                RenderSystem.blendFuncSeparate(770, 771, 1, 0);
                RenderSystem.blendFunc(770, 771);
                RenderSystem.setShaderTexture(0, InternalTextures.CHEST_GUI_TEXTURE);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, opacity);
                this.blit(matrices, bounds.x, bounds.y + 2, u + (selected ? bounds.width : 0), v, bounds.width, (selected ? bounds.height + 2 : bounds.height - 2));
                RenderSystem.setShaderTexture(0, InternalTextures.CHEST_GUI_TEXTURE_DARK);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, darkBackgroundAlpha.value() * opacity);
                this.blit(matrices, bounds.x, bounds.y + 2, u + (selected ? bounds.width : 0), v, bounds.width, (selected ? bounds.height + 2 : bounds.height - 2));
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, opacity);
                renderer.setZ(100);
                renderer.render(matrices, new Rectangle(bounds.getCenterX() - 8, bounds.getCenterY() - 5, 16, 16), mouseX, mouseY, delta);
            }
            if (containsMouse(mouseX, mouseY) && category != null) {
                drawTooltip();
            }
        }
    }
    
    private void drawTooltip() {
        Tooltip tooltip = Tooltip.create(categoryName);
        if (this.minecraft.options.advancedItemTooltips) {
            tooltip.add(Component.literal(category.getIdentifier().toString()).withStyle(ChatFormatting.DARK_GRAY));
        }
        if (ConfigObject.getInstance().shouldAppendModNames()) {
            tooltip.add(ClientHelper.getInstance().getFormattedModFromIdentifier(category.getIdentifier()));
        }
        tooltip.queue();
    }
    
    @Override
    public Rectangle getBounds() {
        return bounds;
    }
    
    @Override
    public double getZRenderingPriority() {
        return selected ? 10 : -10;
    }
    
    @Override
    @Nullable
    public DraggableStack getHoveredStack(DraggingContext<Screen> context, double mouseX, double mouseY) {
        if (isShown() && renderer instanceof EntryStack<?> entryStack && containsMouse(mouseX, mouseY)) {
            return new DraggableStack() {
                EntryStack<?> stack = entryStack.copy();
                
                @Override
                public EntryStack<?> getStack() {
                    return stack;
                }
                
                @Override
                public void drag() {
                }
                
                @Override
                public void release(DraggedAcceptorResult result) {
                    if (result == DraggedAcceptorResult.PASS) {
                        context.renderBackToPosition(this, DraggingContext.getInstance().getCurrentPosition(), () -> new Point(getBounds().getCenterX() - 8, getBounds().getCenterY() - 8));
                    }
                }
            };
        }
        return null;
    }
}
