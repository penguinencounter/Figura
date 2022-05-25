package org.moon.figura.gui.widgets.lists;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Mth;
import org.moon.figura.avatars.Avatar;
import org.moon.figura.gui.widgets.AbstractContainerElement;
import org.moon.figura.gui.widgets.Label;
import org.moon.figura.gui.widgets.TexturedButton;
import org.moon.figura.lua.api.keybind.FiguraKeybind;
import org.moon.figura.utils.ColorUtils;
import org.moon.figura.utils.FiguraText;
import org.moon.figura.utils.TextUtils;
import org.moon.figura.utils.ui.UIHelper;

import java.util.ArrayList;
import java.util.List;

public class KeybindList extends AbstractList {

    private final List<KeybindElement> keybinds = new ArrayList<>();
    private final Avatar owner;

    public FiguraKeybind focusedKeybind;

    public KeybindList(int x, int y, int width, int height, Avatar owner) {
        super(x, y, width, height);
        this.owner = owner;
        updateList();

        Label noOwner, noKeys;
        this.children.add(noOwner = new Label(new FiguraText("gui.keybinds.no_avatar").withStyle(ChatFormatting.YELLOW), x + width / 2, y + height / 2, true, 0));
        this.children.add(noKeys = new Label(new FiguraText("gui.keybinds.no_keybinds").withStyle(ChatFormatting.YELLOW), x + width / 2, y + height / 2, true, 0));

        noOwner.setVisible(owner == null);
        noKeys.setVisible(!noOwner.isVisible() && keybinds.isEmpty());
    }

    @Override
    public void render(PoseStack stack, int mouseX, int mouseY, float delta) {
        //background and scissors
        UIHelper.renderSliced(stack, x, y, width, height, UIHelper.OUTLINE);
        UIHelper.setupScissor(x + scissorsX, y + scissorsY, width + scissorsWidth, height + scissorsHeight);

        if (!keybinds.isEmpty())
            updateEntries();

        //children
        super.render(stack, mouseX, mouseY, delta);

        //reset scissor
        RenderSystem.disableScissor();
    }

    private void updateEntries() {
        //scrollbar
        int totalHeight = -4;
        for (KeybindElement keybind : keybinds)
            totalHeight += keybind.height + 8;
        int entryHeight = keybinds.isEmpty() ? 0 : totalHeight / keybinds.size();

        scrollBar.visible = totalHeight > height;
        scrollBar.setScrollRatio(entryHeight, totalHeight - height);

        //render list
        int xOffset = scrollBar.visible ? 4 : 11;
        int yOffset = scrollBar.visible ? (int) -(Mth.lerp(scrollBar.getScrollProgress(), -4, totalHeight - height)) : 4;
        for (KeybindElement keybind : keybinds) {
            keybind.setPos(x + xOffset, y + yOffset);
            yOffset += keybind.height + 8;
        }
    }

    private void updateList() {
        //clear old widgets
        keybinds.forEach(children::remove);

        //add new keybinds
        if (owner == null || owner.luaState == null)
            return;

        for (FiguraKeybind keybind : owner.luaState.keybind.keyBindings) {
            KeybindElement element = new KeybindElement(width - 22, keybind, this);
            keybinds.add(element);
            children.add(element);
        }
    }

    private static class KeybindElement extends AbstractContainerElement {

        private static final Component HOVERED_ARROW = new TextComponent(">").setStyle(Style.EMPTY.withFont(TextUtils.FIGURA_FONT));

        private final FiguraKeybind keybind;
        private final KeybindList parent;
        private final TexturedButton resetButton;
        private final TexturedButton keybindButton;

        private boolean hovered = false;

        public KeybindElement(int width, FiguraKeybind keybind, KeybindList parent) {
            super(0, 0, width, 20);
            this.keybind = keybind;
            this.parent = parent;

            //toggle button
            children.add(0, keybindButton = new ParentedButton(0, 0, 90, 20, keybind.getTranslatedKeyMessage(), this, button -> parent.focusedKeybind = keybind));

            //reset button
            children.add(resetButton = new ParentedButton(x + width - 60, y, 60, 20, new TranslatableComponent("controls.reset"), this, button -> keybind.resetDefaultKey()));
        }

        @Override
        public void render(PoseStack stack, int mouseX, int mouseY, float delta) {
            if (!this.isVisible()) return;

            //reset enabled
            this.resetButton.active = !FiguraKeybind.isDefault(this.keybind);

            //button message
            this.keybindButton.setMessage(this.keybind.getTranslatedKeyMessage());

            //editing message
            if (parent.focusedKeybind == this.keybind) {
                keybindButton.setMessage(new TextComponent("> ").setStyle(ColorUtils.Colors.FRAN_PINK.style).append(keybindButton.getMessage()).append(" <"));
            }
            //conflict check
            else {
                boolean found = false;
                for (KeyMapping key : Minecraft.getInstance().options.keyMappings) {
                    if (key.saveString().equals(FiguraKeybind.getKey(this.keybind))) {
                        found = true;
                        keybindButton.setMessage(keybindButton.getMessage().copy().withStyle(ChatFormatting.RED));
                        break;
                    }
                }

                if (!found) {
                    for (KeybindElement keybindElement : this.parent.keybinds) {
                        if (keybindElement.keybind != this.keybind && FiguraKeybind.getKey(keybindElement.keybind).equals(FiguraKeybind.getKey(this.keybind))) {
                            keybindButton.setMessage(keybindButton.getMessage().copy().withStyle(ChatFormatting.YELLOW));
                            break;
                        }
                    }
                }
            }

            //vars
            Font font = Minecraft.getInstance().font;
            float textY = y + height / 2f - font.lineHeight / 2f;

            //hovered arrow
            this.hovered = isMouseOver(mouseX, mouseY);
            if (this.hovered) font.draw(stack, HOVERED_ARROW, x + 4, textY, 0xFFFFFF);

            //render name
            font.draw(stack, FiguraKeybind.getName(this.keybind), x + 16, textY, 0xFFFFFF);

            //render children
            super.render(stack, mouseX, mouseY, delta);
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return this.parent.isInsideScissors(mouseX, mouseY) && super.isMouseOver(mouseX, mouseY);
        }

        public void setPos(int x, int y) {
            this.x = x;
            this.y = y;

            resetButton.x = x + width - 60;
            resetButton.y = y;

            keybindButton.x = x + width - 154;
            keybindButton.y = y;
        }
    }

    private static class ParentedButton extends TexturedButton {

        private final KeybindElement parent;

        public ParentedButton(int x, int y, int width, int height, Component text, KeybindElement parent, OnPress pressAction) {
            super(x, y, width, height, text, null, pressAction);
            this.parent = parent;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return this.parent.hovered && super.isMouseOver(mouseX, mouseY);
        }
    }
}
