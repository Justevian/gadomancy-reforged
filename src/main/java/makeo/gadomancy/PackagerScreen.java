package makeo.gadomancy;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import thaumcraft.client.platform.GuiBlit;

@EventBusSubscriber(modid = Gadomancy.MOD_ID, value = Dist.CLIENT)
final class PackagerScreen extends AbstractContainerScreen<PackagerMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Gadomancy.MOD_ID, "textures/gui/gui_packager.png");
    private static final String[] OPTIONS = {"useEssentia", "autoStart", "disguisePackage"};
    private static final int FIRST_OPTION_Y = 97;
    private static final int OPTION_SPACING = 13;
    private static final int CHECK_X = 91;
    private static final int CHECK_SIZE = 8;
    private static final int LABEL_X = 100;
    private static final int LABEL_WIDTH = 83;

    private PackagerScreen(PackagerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 190, 234);
    }

    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ArcanePackagers.MENU.get(), PackagerScreen::new);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
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
                graphics.setTooltipForNextFrame(font, label, mouseX, mouseY);
            }
            graphics.pose().pushMatrix();
            graphics.pose().translate(leftPos + LABEL_X, topPos + y + 1);
            graphics.pose().scale(scale, scale);
            graphics.text(font, label,
                0, 0, checked ? 0xFFFFFFFF : 0xFF808080, false);
            graphics.pose().popMatrix();
        }
    }

    private void sheet(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int u, int v) {
        GuiBlit.blit(graphics, TEXTURE, leftPos + x, topPos + y, width, height, u, v, width, height, 256, 256, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double x = event.x() - leftPos;
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && x >= CHECK_X - 1 && x < LABEL_X + LABEL_WIDTH) {
            for (int button = 0; button < OPTIONS.length; button++) {
                double y = event.y() - topPos - FIRST_OPTION_Y - button * OPTION_SPACING;
                if (y >= -1 && y < 10) {
                    if (menu.progress() == 0) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}
