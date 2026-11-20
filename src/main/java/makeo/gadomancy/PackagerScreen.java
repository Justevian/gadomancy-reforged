package makeo.gadomancy;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import thaumcraft.client.platform.GuiBlit;

@EventBusSubscriber(modid = Gadomancy.MOD_ID, value = Dist.CLIENT)
final class PackagerScreen extends AbstractContainerScreen<PackagerMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Gadomancy.MOD_ID, "textures/gui/gui_packager.png");
    private static final String[] OPTIONS = {"useEssentia", "autoStart", "disguisePackage"};
    private static final int FIRST_OPTION_Y = 97;
    private static final int OPTION_SPACING = 13;
    private static final int CHECK_X = 91;
    private static final int CHECK_SIZE = 8;
    private static final int LABEL_X = 100;
    private static final int LABEL_WIDTH = 83;

    private PackagerScreen(PackagerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 190; imageHeight = 234; inventoryLabelY = 140;
    }

    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ArcanePackagers.MENU.get(), PackagerScreen::new);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {

        sheet(graphics, 0, 0, imageWidth, imageHeight, 0, 0);
        if (menu.progress() > 0) {
            sheet(graphics, 89, 67, menu.progress(), 9, 210, 0);
        }
        for (int button = 0; button < OPTIONS.length; button++) {
            int y = FIRST_OPTION_Y + button * OPTION_SPACING;
            boolean checked = menu.enabled(button);
            if (checked) {
                sheet(graphics, CHECK_X, y, CHECK_SIZE, CHECK_SIZE, 249, 10);
            }
            Component label = Component.translatable("gadomancy.info.ArcanePackager." + OPTIONS[button]);
            float scale = Math.min(0.8F, (float) LABEL_WIDTH / Math.max(1, font.width(label)));
            if (mouseX >= leftPos + CHECK_X - 1 && mouseX < leftPos + LABEL_X + LABEL_WIDTH
                && mouseY >= topPos + y - 1 && mouseY < topPos + y + 10) {
                graphics.renderTooltip(font, label, mouseX, mouseY);
            }
            graphics.pose().pushPose();
            graphics.pose().translate(leftPos + LABEL_X, topPos + y + 1, 0);
            graphics.pose().scale(scale, scale, 1);
            graphics.drawString(font, label,
                0, 0, checked ? 0xFFFFFFFF : 0xFF808080, false);
            graphics.pose().popPose();
        }
    }

    private void sheet(GuiGraphics graphics, int x, int y, int width, int height, int u, int v) {
        GuiBlit.blit(graphics, TEXTURE, leftPos + x, topPos + y, width, height, u, v, width, height, 256, 256, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int buttonId) {
        double x = mouseX - leftPos;
        if (buttonId == InputConstants.MOUSE_BUTTON_LEFT && x >= CHECK_X - 1 && x < LABEL_X + LABEL_WIDTH) {
            for (int button = 0; button < OPTIONS.length; button++) {
                double y = mouseY - topPos - FIRST_OPTION_Y - button * OPTION_SPACING;
                if (y >= -1 && y < 10) {
                    if (menu.progress() == 0) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, buttonId);
    }
}
