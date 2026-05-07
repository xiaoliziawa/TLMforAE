package com.lirxowo.tlmforae.client.gui;

import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.task.MaidTaskConfigGui;
import com.lirxowo.tlmforae.init.ModTaskData;
import com.lirxowo.tlmforae.inventory.AEAutocraftConfigContainer;
import com.lirxowo.tlmforae.network.ModNetwork;
import com.lirxowo.tlmforae.network.message.SaveAEAutocraftConfigMessage;
import com.lirxowo.tlmforae.task.data.AEAutocraftConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class AEAutocraftConfigScreen extends MaidTaskConfigGui<AEAutocraftConfigContainer> {
    public static final int TARGET_SLOT_SIZE = 18;

    private static final int PANEL_X = 84;
    private static final int PANEL_Y = 32;
    private static final int PANEL_WIDTH = 164;
    private static final int PANEL_HEIGHT = 128;
    private static final int HEADER_HEIGHT = 16;
    private static final int ROW_HEIGHT = 21;
    private static final int VISIBLE_ROWS = 4;
    private static final int SLOT_X = PANEL_X + 11;
    private static final int AMOUNT_X = PANEL_X + 38;
    private static final int THRESHOLD_X = PANEL_X + 86;
    private static final int REMOVE_X = PANEL_X + 132;
    private static final int FIELD_WIDTH = 44;

    private final List<RequestRow> rows = new ArrayList<>();
    private int scrollOffset = 0;

    public AEAutocraftConfigScreen(AEAutocraftConfigContainer screenContainer, Inventory inv, Component titleIn) {
        super(screenContainer, inv, titleIn);
    }

    @Override
    protected void initAdditionData() {
        if (this.rows.isEmpty()) {
            AEAutocraftConfig config = this.getMaid().getOrCreateData(ModTaskData.AE_AUTOCRAFT_CONFIG, AEAutocraftConfig.empty());
            config.requests().forEach(request -> this.rows.add(new RequestRow(request.target().copy(), String.valueOf(request.craftAmount()), String.valueOf(request.threshold()))));
        }
        if (this.rows.isEmpty()) {
            this.rows.add(RequestRow.empty());
        }
        this.rows.forEach(RequestRow::clearWidgets);
        clampScroll();
    }

    @Override
    protected void initAdditionWidgets() {
        addVisibleRowWidgets();
        this.addRenderableWidget(Button.builder(Component.literal("+"), button -> addRequest())
                .pos(leftPos + PANEL_X + 112, topPos + PANEL_Y + PANEL_HEIGHT - 20)
                .size(20, 18)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.tlmforae.ae_autocraft_config.save"), button -> saveConfig())
                .pos(leftPos + PANEL_X + 134, topPos + PANEL_Y + PANEL_HEIGHT - 20)
                .size(28, 18)
                .build());
    }

    private void addVisibleRowWidgets() {
        for (int visible = 0; visible < VISIBLE_ROWS; visible++) {
            int index = scrollOffset + visible;
            if (index >= rows.size()) {
                break;
            }
            RequestRow row = rows.get(index);
            int y = topPos + rowY(visible) + 2;
            row.amountBox = new EditBox(this.font, leftPos + AMOUNT_X, y, FIELD_WIDTH, 16, Component.translatable("gui.tlmforae.ae_autocraft_config.amount"));
            row.amountBox.setMaxLength(9);
            row.amountBox.setFilter(AEAutocraftConfigScreen::isDigits);
            row.amountBox.setValue(row.amountText);
            this.addWidget(row.amountBox);

            row.thresholdBox = new EditBox(this.font, leftPos + THRESHOLD_X, y, FIELD_WIDTH + 4, 16, Component.translatable("gui.tlmforae.ae_autocraft_config.threshold"));
            row.thresholdBox.setMaxLength(18);
            row.thresholdBox.setFilter(AEAutocraftConfigScreen::isDigits);
            row.thresholdBox.setValue(row.thresholdText);
            this.addWidget(row.thresholdBox);

            this.addRenderableWidget(Button.builder(Component.literal("-"), button -> removeRequest(index))
                    .pos(leftPos + REMOVE_X, y - 1)
                    .size(18, 18)
                    .build());
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int x, int y) {
        super.renderBg(graphics, partialTicks, x, y);
        renderPanelBackground(graphics);
        renderScrollBar(graphics);
    }

    private void renderPanelBackground(GuiGraphics graphics) {
        int x = leftPos + PANEL_X;
        int y = topPos + PANEL_Y;
        graphics.fill(x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, 0xCCF4F0E7);
        graphics.fill(x, y, x + PANEL_WIDTH, y + 1, 0xFF6D6256);
        graphics.fill(x, y + PANEL_HEIGHT - 1, x + PANEL_WIDTH, y + PANEL_HEIGHT, 0xFF6D6256);
        graphics.fill(x, y, x + 1, y + PANEL_HEIGHT, 0xFF6D6256);
        graphics.fill(x + PANEL_WIDTH - 1, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, 0xFF6D6256);

        for (int visible = 0; visible < VISIBLE_ROWS; visible++) {
            int index = scrollOffset + visible;
            if (index >= rows.size()) {
                break;
            }
            int rowTop = topPos + rowY(visible);
            int rowLeft = leftPos + PANEL_X + 4;
            int rowRight = leftPos + PANEL_X + PANEL_WIDTH - 5;
            graphics.fill(rowLeft, rowTop, rowRight, rowTop + ROW_HEIGHT - 2, visible % 2 == 0 ? 0x33FFFFFF : 0x22000000);
        }
    }

    @Override
    protected void renderAddition(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int x = leftPos + PANEL_X;
        int y = topPos + PANEL_Y;
        graphics.drawString(font, Component.translatable("gui.tlmforae.ae_autocraft_config.target.short"), leftPos + SLOT_X, topPos + PANEL_Y + 6, 0x333333, false);
        graphics.drawString(font, Component.translatable("gui.tlmforae.ae_autocraft_config.amount.short"), leftPos + AMOUNT_X, topPos + PANEL_Y + 6, 0x333333, false);
        graphics.drawString(font, Component.translatable("gui.tlmforae.ae_autocraft_config.threshold.short"), leftPos + THRESHOLD_X, topPos + PANEL_Y + 6, 0x333333, false);

        for (int visible = 0; visible < VISIBLE_ROWS; visible++) {
            int index = scrollOffset + visible;
            if (index >= rows.size()) {
                break;
            }
            renderRow(graphics, rows.get(index), visible, mouseX, mouseY, partialTicks);
        }
        graphics.drawString(font, Component.literal((Math.min(rows.size(), scrollOffset + VISIBLE_ROWS)) + "/" + rows.size()), x + 8, y + PANEL_HEIGHT - 15, 0x555555, false);
    }

    private void renderRow(GuiGraphics graphics, RequestRow row, int visible, int mouseX, int mouseY, float partialTicks) {
        int rowTop = topPos + rowY(visible);
        int slotX = leftPos + SLOT_X;
        int slotY = rowTop + 1;
        renderItemSlot(graphics, row.target, slotX, slotY);
        row.amountBox.render(graphics, mouseX, mouseY, partialTicks);
        row.thresholdBox.render(graphics, mouseX, mouseY, partialTicks);
    }

    private void renderItemSlot(GuiGraphics graphics, ItemStack stack, int slotX, int slotY) {
        graphics.fill(slotX - 1, slotY - 1, slotX + 18, slotY + 18, 0xFF373737);
        graphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF8B8B8B);
        graphics.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFF202020);
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, slotX + 1, slotY + 1);
        } else {
            graphics.drawCenteredString(font, "-", slotX + 9, slotY + 5, ChatFormatting.GRAY.getColor());
        }
    }

    private void renderScrollBar(GuiGraphics graphics) {
        int barX = leftPos + PANEL_X + PANEL_WIDTH - 8;
        int barTop = topPos + PANEL_Y + HEADER_HEIGHT + 4;
        int barBottom = topPos + PANEL_Y + HEADER_HEIGHT + VISIBLE_ROWS * ROW_HEIGHT - 2;
        graphics.fill(barX, barTop, barX + 3, barBottom, 0x66000000);
        int maxScroll = maxScroll();
        int thumbHeight = maxScroll == 0 ? barBottom - barTop : Math.max(12, (barBottom - barTop) * VISIBLE_ROWS / rows.size());
        int thumbTop = maxScroll == 0 ? barTop : barTop + (barBottom - barTop - thumbHeight) * scrollOffset / maxScroll;
        graphics.fill(barX, thumbTop, barX + 3, thumbTop + thumbHeight, 0xFF000000);
    }

    @Override
    protected void renderAdditionTransTooltip(GuiGraphics graphics, int x, int y) {
        int index = getHoveredVisibleRow(x, y);
        if (index < 0) {
            return;
        }
        ItemStack target = rows.get(index).target;
        if (target.isEmpty()) {
            graphics.renderComponentTooltip(font, List.of(Component.translatable("gui.tlmforae.ae_autocraft_config.target.empty")), x, y);
        } else {
            graphics.renderTooltip(font, target, x, y);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        rows.forEach(RequestRow::tick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int index = getHoveredVisibleRow((int) mouseX, (int) mouseY);
        if (index >= 0) {
            setTargetStack(index, this.menu.getCarried());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (isInPanel((int) mouseX, (int) mouseY) && maxScroll() > 0) {
            syncVisibleRowText();
            this.scrollOffset = Mth.clamp(this.scrollOffset + (scrollY > 0 ? -1 : 1), 0, maxScroll());
            this.init();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (RequestRow row : rows) {
            if ((row.amountBox != null && (row.amountBox.keyPressed(keyCode, scanCode, modifiers) || row.amountBox.canConsumeInput()))
                    || (row.thresholdBox != null && (row.thresholdBox.keyPressed(keyCode, scanCode, modifiers) || row.thresholdBox.canConsumeInput()))) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        saveConfig();
        super.onClose();
    }

    private void addRequest() {
        syncVisibleRowText();
        if (this.rows.size() < AEAutocraftConfig.MAX_REQUESTS) {
            this.rows.add(RequestRow.empty());
            this.scrollOffset = maxScroll();
            this.init();
        }
    }

    private void removeRequest(int index) {
        syncVisibleRowText();
        if (index >= 0 && index < rows.size()) {
            this.rows.remove(index);
            if (this.rows.isEmpty()) {
                this.rows.add(RequestRow.empty());
            }
            clampScroll();
            saveConfig();
            this.init();
        }
    }

    private void saveConfig() {
        syncVisibleRowText();
        List<AEAutocraftConfig.Request> requests = rows.stream()
                .map(row -> new AEAutocraftConfig.Request(
                        row.target.copy(),
                        Mth.clamp(parseInt(row.amountText, 1), 1, Integer.MAX_VALUE),
                        Math.max(0, parseLong(row.thresholdText, 0))))
                .toList();
        ModNetwork.CHANNEL.sendToServer(new SaveAEAutocraftConfigMessage(
                this.getMaid().getId(),
                new AEAutocraftConfig(requests)));
    }

    public void setTargetStack(int index, ItemStack stack) {
        if (index < 0 || index >= rows.size()) {
            return;
        }
        rows.get(index).target = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        saveConfig();
    }

    public int addTargetStack(ItemStack stack) {
        syncVisibleRowText();
        int index = firstEmptyRow();
        if (index < 0 && rows.size() < AEAutocraftConfig.MAX_REQUESTS) {
            rows.add(RequestRow.empty());
            index = rows.size() - 1;
        }
        if (index >= 0) {
            rows.get(index).target = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
            this.scrollOffset = Mth.clamp(index, 0, maxScroll());
            saveConfig();
            this.init();
        }
        return index;
    }

    public List<TargetSlotArea> getVisibleTargetSlotAreas() {
        List<TargetSlotArea> areas = new ArrayList<>();
        for (int visible = 0; visible < VISIBLE_ROWS; visible++) {
            int index = scrollOffset + visible;
            if (index >= rows.size()) {
                break;
            }
            areas.add(new TargetSlotArea(index, getTargetSlotX(visible), getTargetSlotY(visible), TARGET_SLOT_SIZE, TARGET_SLOT_SIZE));
        }
        return areas;
    }

    public TargetSlotArea getAppendTargetArea() {
        return new TargetSlotArea(-1, leftPos + PANEL_X + 4, topPos + PANEL_Y + PANEL_HEIGHT - 22, 106, 18);
    }

    private void syncVisibleRowText() {
        rows.forEach(RequestRow::syncText);
    }

    private int firstEmptyRow() {
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).target.isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private int getHoveredVisibleRow(int mouseX, int mouseY) {
        for (int visible = 0; visible < VISIBLE_ROWS; visible++) {
            int index = scrollOffset + visible;
            if (index >= rows.size()) {
                return -1;
            }
            int slotX = getTargetSlotX(visible);
            int slotY = getTargetSlotY(visible);
            if (mouseX >= slotX && mouseX < slotX + TARGET_SLOT_SIZE && mouseY >= slotY && mouseY < slotY + TARGET_SLOT_SIZE) {
                return index;
            }
        }
        return -1;
    }

    private boolean isInPanel(int mouseX, int mouseY) {
        return mouseX >= leftPos + PANEL_X && mouseX < leftPos + PANEL_X + PANEL_WIDTH
                && mouseY >= topPos + PANEL_Y && mouseY < topPos + PANEL_Y + PANEL_HEIGHT;
    }

    private int getTargetSlotX(int visibleRow) {
        return leftPos + SLOT_X;
    }

    private int getTargetSlotY(int visibleRow) {
        return topPos + rowY(visibleRow) + 1;
    }

    private int rowY(int visibleRow) {
        return PANEL_Y + HEADER_HEIGHT + 4 + visibleRow * ROW_HEIGHT;
    }

    private void clampScroll() {
        this.scrollOffset = Mth.clamp(this.scrollOffset, 0, maxScroll());
    }

    private int maxScroll() {
        return Math.max(0, this.rows.size() - VISIBLE_ROWS);
    }

    private static boolean isDigits(String value) {
        return value.chars().allMatch(Character::isDigit);
    }

    private static int parseInt(String value, int fallback) {
        try {
            return value.isEmpty() ? fallback : Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static long parseLong(String value, long fallback) {
        try {
            return value.isEmpty() ? fallback : Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public record TargetSlotArea(int index, int x, int y, int width, int height) {
    }

    private static class RequestRow {
        private ItemStack target;
        private String amountText;
        private String thresholdText;
        private EditBox amountBox;
        private EditBox thresholdBox;

        private RequestRow(ItemStack target, String amountText, String thresholdText) {
            this.target = target;
            this.amountText = amountText;
            this.thresholdText = thresholdText;
        }

        private static RequestRow empty() {
            return new RequestRow(ItemStack.EMPTY, "1", "0");
        }

        private void tick() {
            if (this.amountBox != null) {
                this.amountBox.tick();
            }
            if (this.thresholdBox != null) {
                this.thresholdBox.tick();
            }
        }

        private void syncText() {
            if (this.amountBox != null) {
                this.amountText = this.amountBox.getValue();
            }
            if (this.thresholdBox != null) {
                this.thresholdText = this.thresholdBox.getValue();
            }
        }

        private void clearWidgets() {
            this.amountBox = null;
            this.thresholdBox = null;
        }
    }
}
