package makeo.gadomancy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import thaumcraft.client.screen.GolemScreen;

@Mixin(GolemScreen.class)
abstract class GolemScreenLayoutMixin {
    @WrapOperation(method = "renderLabels", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/GuiGraphics;drawWordWrap(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/FormattedText;IIII)V"))
    private void gadomancy$drawDescriptionAbovePanel(GuiGraphics graphics, Font font, FormattedText text,
        int x, int y, int width, int color, Operation<Void> original) {
        var lines = font.split(text, 204);
        graphics.pose().pushPose();
        graphics.pose().translate(x + 8, y - 32 - lines.size() * 8, 0);
        graphics.pose().scale(0.8F, 0.8F, 1);
        for (int line = 0; line < lines.size(); line++) {
            var content = lines.get(line);
            graphics.drawString(font, content, -font.width(content) / 2, line * 10, color, true);
        }
        graphics.pose().popPose();

    }

    @com.llamalad7.mixinextras.injector.ModifyExpressionValue(method = "renderBg",
        at = @At(value = "CONSTANT", args = "intValue=66", ordinal = 1))
    private int gadomancy$moveSortingControls(int x) {
        return 97;
    }

    @WrapOperation(method = "renderBg", at = @At(value = "INVOKE",
        target = "Lthaumcraft/client/screen/GolemScreen;scaled(Lnet/minecraft/client/gui/GuiGraphics;Ljava/lang/String;IIIF)V",
        ordinal = 2))
    private void gadomancy$fitSortingLabels(GolemScreen screen, GuiGraphics graphics,
        String label, int x, int y, int color, float scale, Operation<Void> original) {
        if (x < 176) {
            int width = net.minecraft.client.Minecraft.getInstance().font.width(label);
            scale = Math.min(scale, 65F / Math.max(1, width));
        }
        original.call(screen, graphics, label, x, y, color, scale);
    }

    @com.llamalad7.mixinextras.injector.ModifyExpressionValue(method = "mouseClicked",
        at = @At(value = "CONSTANT", args = "intValue=66", ordinal = 1))
    private int gadomancy$moveSortingHitboxes(int x) {
        return 97;
    }
}
