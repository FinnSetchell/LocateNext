package com.finndog.locatenext.client;

import com.finndog.locatenext.net.NavigatePayload;
// 26.1's GUI rework renamed GuiGraphics; every method used here keeps its signature.
//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Two-column picker: mods on the left, that mod's structures on the right.
 *
 * <p>Rows are drawn by hand rather than with {@code ObjectSelectionList} — the lists are long and
 * plain text, and hand-drawn rows keep the whole layout in one readable place.
 */
public final class LocateNextScreen extends Screen {

    private static final int ROW_HEIGHT = 12;
    private static final int COLOUR_TEXT = 0xFFE0E0E0;
    private static final int COLOUR_MUTED = 0xFF909090;
    private static final int COLOUR_HEADER = 0xFF55FFFF;
    private static final int COLOUR_CURRENT = 0xFF55FF55;
    private static final int COLOUR_ROW_HOVER = 0x50FFFFFF;
    private static final int COLOUR_ROW_SELECTED = 0x40FFFF55;
    private static final int COLOUR_PANEL = 0x60000000;

    private EditBox modFilter;
    private EditBox structureFilter;

    private int panelLeft;
    private int leftWidth;
    private int rightLeft;
    private int rightWidth;
    private int listTop;
    private int visibleRows;

    private int modScroll;
    private int structureScroll;

    /** Original indices into the selected mod's list, so filtering can't desync the goto index. */
    private List<Integer> visibleStructures = List.of();
    private List<String> visibleMods = List.of();

    /** Where closing returns to. Null when opened by the keybind, which closes to the world. */
    @Nullable private final Screen parent;

    public LocateNextScreen() {
        this(null);
    }

    public LocateNextScreen(@Nullable Screen parent) {
        super(Component.translatable("locatenext.screen.title"));
        this.parent = parent;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            //? if >=26.1 {
            /*this.minecraft.setScreenAndShow(this.parent);
            *///?} else {
            this.minecraft.setScreen(this.parent);
            //?}
        }
    }

    // 26.1 renamed the drawing calls (drawString -> text, drawCenteredString -> centeredText)
    // and nothing else about them. Routing every call site through these three helpers keeps the
    // rename in one place instead of a conditional around each of the ten draws below.
    //? if >=26.1 {
    /*private void text(GuiGraphicsExtractor graphics, Component text, int x, int y, int colour) {
        graphics.text(this.font, text, x, y, colour);
    }

    private void text(GuiGraphicsExtractor graphics, String text, int x, int y, int colour, boolean shadow) {
        graphics.text(this.font, text, x, y, colour, shadow);
    }

    private void centeredText(GuiGraphicsExtractor graphics, Component text, int x, int y, int colour) {
        graphics.centeredText(this.font, text, x, y, colour);
    }
    *///?} else {
    private void text(GuiGraphics graphics, Component text, int x, int y, int colour) {
        graphics.drawString(this.font, text, x, y, colour);
    }

    private void text(GuiGraphics graphics, String text, int x, int y, int colour, boolean shadow) {
        graphics.drawString(this.font, text, x, y, colour, shadow);
    }

    private void centeredText(GuiGraphics graphics, Component text, int x, int y, int colour) {
        graphics.drawCenteredString(this.font, text, x, y, colour);
    }
    //?}

    @Override
    protected void init() {
        int contentWidth = Math.min(this.width - 40, 460);
        this.panelLeft = (this.width - contentWidth) / 2;
        this.leftWidth = Math.min(150, contentWidth / 2 - 5);
        this.rightLeft = this.panelLeft + this.leftWidth + 10;
        this.rightWidth = contentWidth - this.leftWidth - 10;
        this.listTop = 66;
        this.visibleRows = Math.max(3, (this.height - this.listTop - 42) / ROW_HEIGHT);

        this.modFilter = new EditBox(this.font, this.panelLeft, 44, this.leftWidth, 16,
                Component.translatable("locatenext.screen.search"));
        this.modFilter.setHint(Component.translatable("locatenext.screen.search"));
        this.modFilter.setResponder(text -> this.modScroll = 0);
        this.addRenderableWidget(this.modFilter);

        this.structureFilter = new EditBox(this.font, this.rightLeft, 44, this.rightWidth, 16,
                Component.translatable("locatenext.screen.search"));
        this.structureFilter.setHint(Component.translatable("locatenext.screen.search"));
        this.structureFilter.setResponder(text -> this.structureScroll = 0);
        this.addRenderableWidget(this.structureFilter);

        int buttonY = this.height - 28;
        int buttonWidth = (contentWidth - 16) / 5;
        int x = this.panelLeft;
        x = addButton(Component.literal("◀ Prev"), x, buttonY, buttonWidth,
                () -> jump(NavigatePayload.OP_PREV));
        x = addButton(Component.literal("Next ▶"), x, buttonY, buttonWidth,
                () -> jump(NavigatePayload.OP_NEXT));
        x = addButton(Component.literal("↑ New"), x, buttonY, buttonWidth,
                () -> jump(NavigatePayload.OP_VARIANT_NEXT));
        x = addButton(Component.literal("Home"), x, buttonY, buttonWidth,
                () -> jump(NavigatePayload.OP_HOME));
        addButton(CommonComponents.GUI_DONE, x, buttonY, buttonWidth, this::onClose);
    }

    private int addButton(Component label, int x, int y, int width, Runnable action) {
        this.addRenderableWidget(Button.builder(label, button -> action.run())
                .bounds(x, y, width, 20).build());
        return x + width + 4;
    }

    /** Buttons close the screen so the teleport lands with the world visible. */
    private void jump(int op) {
        ClientStructureIndex.navigate(op);
        this.onClose();
    }

    // 26.1 moved screens to a build-a-render-state model: the per-frame entry point is
    // extractRenderState rather than render. The body is unchanged — the extractor still takes
    // the same immediate-style draw calls.
    @Override
    //? if >=26.1 {
    /*public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    *///?} else {
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
    //?}

        centeredText(graphics, this.title, this.width / 2, 16, COLOUR_HEADER);

        if (ClientStructureIndex.isEmpty()) {
            centeredText(graphics, Component.translatable("locatenext.screen.empty"),
                    this.width / 2, this.height / 2, COLOUR_MUTED);
            return;
        }

        renderMods(graphics, mouseX, mouseY);
        renderStructures(graphics, mouseX, mouseY);
    }

    // ------------------------------------------------------------------ panels

    //? if >=26.1 {
    /*private void renderMods(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    *///?} else {
    private void renderMods(GuiGraphics graphics, int mouseX, int mouseY) {
    //?}
        this.visibleMods = filterMods();
        this.modScroll = clampScroll(this.modScroll, this.visibleMods.size());

        text(graphics, Component.translatable("locatenext.screen.mods")
                        .append(Component.literal(" (" + this.visibleMods.size() + ")")),
                this.panelLeft, 32, COLOUR_HEADER);
        drawPanel(graphics, this.panelLeft, this.leftWidth);

        String selected = ClientStructureIndex.selectedNamespace();
        for (int row = 0; row < this.visibleRows; row++) {
            int index = row + this.modScroll;
            if (index >= this.visibleMods.size()) {
                break;
            }
            String namespace = this.visibleMods.get(index);
            int y = this.listTop + row * ROW_HEIGHT;
            boolean hovered = isOver(mouseX, mouseY, this.panelLeft, this.leftWidth, y);
            highlight(graphics, this.panelLeft, this.leftWidth, y, hovered, namespace.equals(selected));

            int count = ClientStructureIndex.structures(namespace).size();
            String suffix = " (" + count + ")";
            String label = this.font.plainSubstrByWidth(namespace, this.leftWidth - 8 - this.font.width(suffix));
            text(graphics, label, this.panelLeft + 4, y + 2,
                    namespace.equals(selected) ? COLOUR_CURRENT : COLOUR_TEXT, false);
            text(graphics, suffix,
                    this.panelLeft + this.leftWidth - 4 - this.font.width(suffix), y + 2, COLOUR_MUTED, false);
        }
    }

    //? if >=26.1 {
    /*private void renderStructures(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    *///?} else {
    private void renderStructures(GuiGraphics graphics, int mouseX, int mouseY) {
    //?}
        String namespace = ClientStructureIndex.selectedNamespace();
        List<ResourceLocation> all = ClientStructureIndex.structures(namespace);

        if (all.isEmpty()) {
            text(graphics, Component.translatable("locatenext.screen.no_mod"),
                    this.rightLeft, 32, COLOUR_MUTED);
            drawPanel(graphics, this.rightLeft, this.rightWidth);
            this.visibleStructures = List.of();
            return;
        }

        this.visibleStructures = filterStructures(all);
        this.structureScroll = clampScroll(this.structureScroll, this.visibleStructures.size());

        int current = ClientStructureIndex.selectedIndex();
        text(graphics, Component.literal(namespace + "  ")
                        .append(Component.literal((current + 1) + "/" + all.size())),
                this.rightLeft, 32, COLOUR_HEADER);
        drawPanel(graphics, this.rightLeft, this.rightWidth);

        for (int row = 0; row < this.visibleRows; row++) {
            int slot = row + this.structureScroll;
            if (slot >= this.visibleStructures.size()) {
                break;
            }
            int index = this.visibleStructures.get(slot);
            int y = this.listTop + row * ROW_HEIGHT;
            boolean hovered = isOver(mouseX, mouseY, this.rightLeft, this.rightWidth, y);
            highlight(graphics, this.rightLeft, this.rightWidth, y, hovered, index == current);

            String number = (index + 1) + ".";
            text(graphics, number, this.rightLeft + 4, y + 2, COLOUR_MUTED, false);
            int textX = this.rightLeft + 8 + this.font.width("999.");
            String label = this.font.plainSubstrByWidth(
                    all.get(index).getPath(), this.rightLeft + this.rightWidth - 4 - textX);
            text(graphics, label, textX, y + 2,
                    index == current ? COLOUR_CURRENT : COLOUR_TEXT, false);
        }
    }

    //? if >=26.1 {
    /*private void drawPanel(GuiGraphicsExtractor graphics, int x, int width) {
    *///?} else {
    private void drawPanel(GuiGraphics graphics, int x, int width) {
    //?}
        graphics.fill(x, this.listTop - 2, x + width, this.listTop + this.visibleRows * ROW_HEIGHT, COLOUR_PANEL);
    }

    //? if >=26.1 {
    /*private void highlight(GuiGraphicsExtractor graphics, int x, int width, int y, boolean hovered, boolean selected) {
    *///?} else {
    private void highlight(GuiGraphics graphics, int x, int width, int y, boolean hovered, boolean selected) {
    //?}
        if (selected) {
            graphics.fill(x, y, x + width, y + ROW_HEIGHT, COLOUR_ROW_SELECTED);
        }
        if (hovered) {
            graphics.fill(x, y, x + width, y + ROW_HEIGHT, COLOUR_ROW_HOVER);
        }
    }

    // ------------------------------------------------------------------ input

    // 26.1 replaced the (x, y, button) triple with a MouseButtonEvent carrying the same values
    // plus a "double click" flag. Only the signature and the accessors differ; the body below is
    // shared, so it reads the three values into locals and continues unchanged.
    //? if >=26.1 {
    /*@Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        double mouseX = event.x();
        double mouseY = event.y();
        if (event.button() != 0) {
            return false;
        }
    *///?} else {
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button != 0) {
            return false;
        }
    //?}

        int modRow = rowAt(mouseX, mouseY, this.panelLeft, this.leftWidth, this.modScroll, this.visibleMods.size());
        if (modRow >= 0) {
            ClientStructureIndex.selectMod(this.visibleMods.get(modRow));
            this.structureScroll = 0;
            this.structureFilter.setValue("");
            return true;
        }

        int structureRow = rowAt(mouseX, mouseY, this.rightLeft, this.rightWidth,
                this.structureScroll, this.visibleStructures.size());
        if (structureRow >= 0) {
            ClientStructureIndex.goTo(this.visibleStructures.get(structureRow));
            this.onClose();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int delta = (int) -Math.signum(scrollY) * 3;
        if (mouseX >= this.panelLeft && mouseX < this.panelLeft + this.leftWidth) {
            this.modScroll = clampScroll(this.modScroll + delta, this.visibleMods.size());
            return true;
        }
        if (mouseX >= this.rightLeft && mouseX < this.rightLeft + this.rightWidth) {
            this.structureScroll = clampScroll(this.structureScroll + delta, this.visibleStructures.size());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        // Keeps the world ticking, so the teleport that a click triggers isn't queued behind a
        // pause on the integrated server.
        return false;
    }

    // ------------------------------------------------------------------ helpers

    private List<String> filterMods() {
        String query = this.modFilter == null ? "" : this.modFilter.getValue().toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String namespace : ClientStructureIndex.byNamespace().keySet()) {
            if (query.isEmpty() || namespace.toLowerCase(Locale.ROOT).contains(query)) {
                result.add(namespace);
            }
        }
        return result;
    }

    private List<Integer> filterStructures(List<ResourceLocation> all) {
        String query = this.structureFilter == null ? "" : this.structureFilter.getValue().toLowerCase(Locale.ROOT);
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) {
            if (query.isEmpty() || all.get(i).getPath().toLowerCase(Locale.ROOT).contains(query)) {
                result.add(i);
            }
        }
        return result;
    }

    private boolean isOver(double mouseX, double mouseY, int x, int width, int y) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + ROW_HEIGHT;
    }

    /** @return index into the filtered list, or -1 when the click missed every row. */
    private int rowAt(double mouseX, double mouseY, int x, int width, int scroll, int size) {
        if (mouseX < x || mouseX >= x + width || mouseY < this.listTop) {
            return -1;
        }
        int row = (int) ((mouseY - this.listTop) / ROW_HEIGHT);
        if (row < 0 || row >= this.visibleRows) {
            return -1;
        }
        int index = row + scroll;
        return index < size ? index : -1;
    }

    private int clampScroll(int scroll, int size) {
        return Mth.clamp(scroll, 0, Math.max(0, size - this.visibleRows));
    }
}
