package com.example.villagerremote.client;

import com.example.villagerremote.VillagerRemoteMod;
import com.example.villagerremote.network.NetworkHandler;
import com.example.villagerremote.network.packet.ArmMiningC2S;
import com.example.villagerremote.network.packet.OpenRemoteScreenS2C;
import com.example.villagerremote.network.packet.VillagerCommandC2S;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class VillagerRemoteScreen extends Screen {
    private static final ResourceLocation VILLAGER_ICON =
            new ResourceLocation(VillagerRemoteMod.MOD_ID, "textures/gui/villager_icon.png");
    private static final ResourceLocation FOLLOW_ICON =
            new ResourceLocation(VillagerRemoteMod.MOD_ID, "textures/gui/follow_icon.png");
    private static final ResourceLocation STOP_ICON =
            new ResourceLocation(VillagerRemoteMod.MOD_ID, "textures/gui/stop_icon.png");
    private static final ResourceLocation MINE_ICON =
            new ResourceLocation(VillagerRemoteMod.MOD_ID, "textures/gui/mine_icon.png");
    private static final ResourceLocation REMOTE_ICON =
            new ResourceLocation(VillagerRemoteMod.MOD_ID, "textures/gui/remote_icon.png");

    private static final int PANEL_WIDTH = 510;
    private static final int PANEL_HEIGHT = 300;
    private static final int ROW_HEIGHT = 42;
    private static final int MAX_VISIBLE_ROWS = 4;

    private final List<OpenRemoteScreenS2C.VillagerInfo> villagers;
    private int scrollOffset = 0;
    private int panelX;
    private int panelY;

    public VillagerRemoteScreen(List<OpenRemoteScreenS2C.VillagerInfo> villagers) {
        super(Component.literal("ريموت القرويين"));
        this.villagers = List.copyOf(villagers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        panelX = (this.width - PANEL_WIDTH) / 2;
        panelY = (this.height - PANEL_HEIGHT) / 2;

        // Main glass-like panel.
        graphics.fill(panelX - 2, panelY - 2, panelX + PANEL_WIDTH + 2, panelY + PANEL_HEIGHT + 2, 0xC9000000);
        graphics.fillGradient(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFF182333, 0xFF101722);
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + 3, 0xFF48B6D9);
        graphics.fill(panelX + 1, panelY + 52, panelX + PANEL_WIDTH - 1, panelY + 53, 0xFF33485D);

        graphics.blit(REMOTE_ICON, panelX + 16, panelY + 13, 0, 0, 28, 28, 32, 32);
        graphics.drawString(this.font, "ريموت القرويين", panelX + 54, panelY + 13, 0xFFF1F7FF, false);
        graphics.drawString(this.font, "مركز التحكم بالقرويين القريبين", panelX + 54, panelY + 29, 0xFF9AB1C7, false);

        String count = "القرويون المكتشفون: " + villagers.size();
        int countWidth = this.font.width(count);
        graphics.fill(panelX + PANEL_WIDTH - countWidth - 31, panelY + 17,
                panelX + PANEL_WIDTH - 14, panelY + 39, 0xFF263B4D);
        graphics.drawString(this.font, count, panelX + PANEL_WIDTH - countWidth - 22, panelY + 24, 0xFF8FE3C5, false);

        graphics.drawString(this.font, "اختر قرويًا ثم أعطه أمرًا مستقلًا:", panelX + 17, panelY + 61, 0xFFB5C8D8, false);

        if (villagers.isEmpty()) {
            graphics.drawString(this.font, "لم يتم العثور على قرويين. أغلق الريموت وأعد الفحص.",
                    panelX + 20, panelY + 132, 0xFFFFC27A, false);
        } else {
            int visible = Math.min(MAX_VISIBLE_ROWS, villagers.size() - scrollOffset);
            for (int row = 0; row < visible; row++) {
                int index = scrollOffset + row;
                OpenRemoteScreenS2C.VillagerInfo info = villagers.get(index);
                int rowY = panelY + 80 + row * ROW_HEIGHT;
                drawVillagerRow(graphics, info, rowY, mouseX, mouseY);
            }
        }

        int bottomY = panelY + PANEL_HEIGHT - 34;
        graphics.fill(panelX + 12, bottomY - 7, panelX + PANEL_WIDTH - 12, bottomY - 6, 0xFF33485D);
        graphics.drawString(this.font, "التكسير: اضغط «اكسر»، ثم أغلق القائمة وانقر على البلوك بالريموت.",
                panelX + 16, bottomY + 2, 0xFF9FB2C4, false);
        drawButton(graphics, panelX + PANEL_WIDTH - 86, panelY + PANEL_HEIGHT - 29,
                72, 19, "إغلاق", 0xFF35485B, 0xFF536B83,
                mouseX, mouseY, null);

        if (villagers.size() > MAX_VISIBLE_ROWS) {
            graphics.drawString(this.font,
                    (scrollOffset + 1) + "-" + Math.min(scrollOffset + MAX_VISIBLE_ROWS, villagers.size()) + " / " + villagers.size(),
                    panelX + PANEL_WIDTH - 91, panelY + 64, 0xFF8299AE, false);
        }
    }

    private void drawVillagerRow(GuiGraphics graphics,
                                 OpenRemoteScreenS2C.VillagerInfo info,
                                 int rowY,
                                 int mouseX,
                                 int mouseY) {
        int x = panelX + 12;
        graphics.fill(x, rowY, panelX + PANEL_WIDTH - 12, rowY + 37, 0xFF202D3B);
        graphics.fill(x, rowY, x + 2, rowY + 37, 0xFF3D718B);
        graphics.blit(VILLAGER_ICON, x + 9, rowY + 9, 0, 0, 20, 20, 16, 16);

        String name = this.font.plainSubstrByWidth(info.name(), 145);
        graphics.drawString(this.font, name, x + 38, rowY + 7, 0xFFE8F0F7, false);

        float healthRatio = info.maxHealth() <= 0.0F ? 0.0F : Math.max(0.0F, Math.min(1.0F, info.health() / info.maxHealth()));
        int barX = x + 38;
        int barY = rowY + 23;
        graphics.fill(barX, barY, barX + 132, barY + 5, 0xFF101820);
        graphics.fill(barX, barY, barX + Math.round(132 * healthRatio), barY + 5, 0xFF55C58A);
        String health = String.format(java.util.Locale.ROOT, "%.0f / %.0f", info.health(), info.maxHealth());
        graphics.drawString(this.font, health, barX + 138, rowY + 19, 0xFF9EB1C3, false);

        int followX = panelX + 226;
        int stopX = panelX + 307;
        int mineX = panelX + 382;
        drawButton(graphics, followX, rowY + 7, 74, 23, "اتبعني", 0xFF245C51, 0xFF32806E,
                mouseX, mouseY, FOLLOW_ICON);
        drawButton(graphics, stopX, rowY + 7, 68, 23, "توقف", 0xFF654044, 0xFF8D4C52,
                mouseX, mouseY, STOP_ICON);
        drawButton(graphics, mineX, rowY + 7, 103, 23, "اكسر بلوك", 0xFF62512D, 0xFF8B7139,
                mouseX, mouseY, MINE_ICON);
    }

    private void drawButton(GuiGraphics graphics,
                            int x,
                            int y,
                            int width,
                            int height,
                            String label,
                            int baseColor,
                            int hoverColor,
                            int mouseX,
                            int mouseY,
                            ResourceLocation icon) {
        boolean hovered = isInside(mouseX, mouseY, x, y, width, height);
        graphics.fill(x, y, x + width, y + height, hovered ? hoverColor : baseColor);
        graphics.fill(x, y, x + width, y + 1, hovered ? 0xFFE6F2FF : 0x55FFFFFF);
        int iconSpace = icon == null ? 0 : 15;
        if (icon != null) {
            graphics.blit(icon, x + 5, y + 4, 0, 0, 14, 14, 16, 16);
        }
        int textWidth = this.font.width(label);
        int textX = icon == null ? x + (width - textWidth) / 2 : x + iconSpace + (width - iconSpace - textWidth) / 2;
        graphics.drawString(this.font, label, textX, y + (height - 8) / 2, 0xFFFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        int closeX = panelX + PANEL_WIDTH - 86;
        int closeY = panelY + PANEL_HEIGHT - 29;
        if (isInside(mouseX, mouseY, closeX, closeY, 72, 19)) {
            onClose();
            return true;
        }

        for (int row = 0; row < MAX_VISIBLE_ROWS; row++) {
            int index = scrollOffset + row;
            if (index >= villagers.size()) {
                break;
            }
            int rowY = panelY + 80 + row * ROW_HEIGHT;
            if (mouseY < rowY + 7 || mouseY > rowY + 30) {
                continue;
            }

            int entityId = villagers.get(index).entityId();
            if (isInside(mouseX, mouseY, panelX + 226, rowY + 7, 74, 23)) {
                NetworkHandler.sendToServer(new VillagerCommandC2S(entityId, VillagerCommandC2S.FOLLOW));
                return true;
            }
            if (isInside(mouseX, mouseY, panelX + 307, rowY + 7, 68, 23)) {
                NetworkHandler.sendToServer(new VillagerCommandC2S(entityId, VillagerCommandC2S.STOP));
                return true;
            }
            if (isInside(mouseX, mouseY, panelX + 382, rowY + 7, 103, 23)) {
                NetworkHandler.sendToServer(new ArmMiningC2S(entityId));
                if (this.minecraft != null) {
                    this.minecraft.setScreen(null);
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (villagers.size() <= MAX_VISIBLE_ROWS) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        if (delta < 0.0D) {
            scrollOffset = Math.min(villagers.size() - MAX_VISIBLE_ROWS, scrollOffset + 1);
        } else if (delta > 0.0D) {
            scrollOffset = Math.max(0, scrollOffset - 1);
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(null);
        }
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
