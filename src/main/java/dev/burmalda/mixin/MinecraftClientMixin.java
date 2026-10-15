package dev.burmalda.mixin;

import dev.burmalda.BrandTitleScreen;
import dev.burmalda.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    /** Swap the vanilla title screen for ours. */
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen burmalda$title(Screen screen) {
        if (screen != null && screen.getClass() == TitleScreen.class && Modules.MAINMENU.enabled) {
            return new BrandTitleScreen();
        }
        return screen;
    }
}
