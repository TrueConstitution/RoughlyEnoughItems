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
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.impl.client.gui.widget.entrylist.EntryListSearchManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import java.lang.RuntimeException;
import java.util.HashMap;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.world.item.ItemStack;

public class EntryHighlighter {
    public static void render(GuiGraphics graphics) {
        RenderSystem.disableDepthTest();
        RenderSystem.colorMask(true, true, true, false);
        if (Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> containerScreen) {
            int x = containerScreen.leftPos, y = containerScreen.topPos;
            for (Slot slot : containerScreen.getMenu().slots) {
                boolean shulkerMatches = false;
                if (slot.getItem().getDescriptionId().contains("shulker_box"))
                    shulkerMatches = helper.shulkerMatches(slot.getItem());
                if (slot.hasItem() && (EntryListSearchManager.INSTANCE.matches(EntryStacks.of(slot.getItem())) || shulkerMatches)) {
                    graphics.pose().pushPose();
                    graphics.pose().translate(0, 0, 200f);
                    graphics.fillGradient(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, 0x345fff3b, 0x345fff3b);
                    graphics.fillGradient(x + slot.x - 1, y + slot.y - 1, x + slot.x, y + slot.y + 16 + 1, 0xff5fff3b, 0xff5fff3b);
                    graphics.fillGradient(x + slot.x + 16, y + slot.y - 1, x + slot.x + 16 + 1, y + slot.y + 16 + 1, 0xff5fff3b, 0xff5fff3b);
                    graphics.fillGradient(x + slot.x - 1, y + slot.y - 1, x + slot.x + 16, y + slot.y, 0xff5fff3b, 0xff5fff3b);
                    graphics.fillGradient(x + slot.x - 1, y + slot.y + 16, x + slot.x + 16, y + slot.y + 16 + 1, 0xff5fff3b, 0xff5fff3b);

                    graphics.pose().popPose();
                } else {
                    graphics.pose().pushPose();
                    graphics.pose().translate(0, 0, 500f);
                    graphics.fillGradient(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, 0xdc202020, 0xdc202020);
                    graphics.pose().popPose();
                }
            }
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.enableDepthTest();
        }
    }
    public static void clearCache() {
        helper.clear();
    }
    private static class helper {
    private static HashMap<CompoundTag, Boolean> shulkermap = new HashMap<>();
    public static void clear() {
        shulkermap.clear();
    }
    public static boolean shulkerMatches(ItemStack stack) {
        CompoundTag shulker = stack.getTag();
        if (shulkermap.containsKey(shulker)) return shulkermap.get(shulker);
        try {
            CompoundTag blockEntityTag = shulker.getCompound("BlockEntityTag");
            ListTag itemsInShulker = (ListTag) blockEntityTag.get("Items");
            assert itemsInShulker != null;
            shulkermap.put(shulker, itemsInShulker.stream().anyMatch(item -> {
                    CompoundTag itemTag;
                    try {
                        itemTag = TagParser.parseTag(item.getAsString());
                    } catch (CommandSyntaxException e) {
                        return EntryListSearchManager.INSTANCE.matches(EntryStacks.of(
                                ItemStack.EMPTY));
                    }
                return EntryListSearchManager.INSTANCE.matches(EntryStacks.of(
                        ItemStack.of(itemTag)));
            }));
        }
        catch (RuntimeException ignored) {}
        return shulkermap.getOrDefault(shulker, false);
    }
    }
}
