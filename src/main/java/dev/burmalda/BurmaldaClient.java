package dev.burmalda;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.ActionResult;
import org.lwjgl.glfw.GLFW;

public class BurmaldaClient implements ClientModInitializer {
    public static KeyBinding openKey, zoomKey;

    @Override
    public void onInitializeClient() {
        Config.load();
        Backgrounds.init();

        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key." + Brand.ID + ".menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category." + Brand.ID));
        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key." + Brand.ID + ".zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, "category." + Brand.ID));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openKey.wasPressed()) {
                if (mc.currentScreen == null && mc.player != null) mc.setScreen(new MenuScreen());
            }
            pollBinds(mc);
            TargetTracker.tick(mc);
            HitEffects.tick(mc);
            Rings.tick(mc);
            Trail.tick(mc);
            Aura.tick(mc);
            timeChanger(mc);

            boolean music = Modules.ISLAND.enabled && Modules.ISLAND.b("Music");
            boolean lyrics = Modules.LYRICS.enabled;
            MediaMonitor.update(music || lyrics, Modules.ISLAND.m("Source"));
            Lyrics.update(lyrics);
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient && player == MinecraftClient.getInstance().player) TargetTracker.onAttack(entity);
            return ActionResult.PASS;
        });

        HudRenderCallback.EVENT.register(HudRenderer::render);
        WorldRenderEvents.BLOCK_OUTLINE.register(BlockOverlay::onOutline);
        WorldRenderEvents.AFTER_ENTITIES.register(DamageParticles::render);
        WorldRenderEvents.LAST.register(TargetRender::render);
        WorldRenderEvents.LAST.register(Rings::render);
        WorldRenderEvents.LAST.register(Trail::render);

        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, ctx) -> {
            if (renderer instanceof PlayerEntityRenderer pr) helper.register(new HatFeature(pr));
        });
    }

    /** Client-side time of day (purely visual, the server time is untouched). */
    private static void timeChanger(MinecraftClient mc) {
        if (!Modules.TIME.enabled || mc.world == null) return;
        long ticks = (long) (((Modules.TIME.f("Hour") + 18f) % 24f) * 1000f);
        mc.world.setTimeOfDay(ticks);
    }

    /** Module hotkeys (set in the menu by clicking the key label of a card). */
    private static void pollBinds(MinecraftClient mc) {
        long win = mc.getWindow().getHandle();
        for (Feature f : Modules.ALL) {
            if (f.bind < 0) continue;
            boolean down = InputUtil.isKeyPressed(win, f.bind);
            if (down && !f.bindDown && mc.currentScreen == null) f.toggle();
            f.bindDown = down;
        }
    }
}
