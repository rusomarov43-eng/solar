package dev.burmalda;

import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;

import java.util.Random;

/** Ambient particles around you (client-side only). */
public final class Aura {
    private static final Random R = new Random();

    public static void tick(MinecraftClient mc) {
        if (!Modules.AURA.enabled || mc.player == null || mc.world == null) return;
        if (mc.options.getPerspective().isFirstPerson() && mc.currentScreen != null) return;
        ParticleEffect pe = switch (Modules.AURA.m("Type")) {
            case 0 -> ParticleTypes.END_ROD;
            case 1 -> ParticleTypes.SNOWFLAKE;
            case 2 -> ParticleTypes.HEART;
            case 3 -> ParticleTypes.ENCHANT;
            default -> ParticleTypes.FLAME;
        };
        int n = (int) Modules.AURA.f("Amount");
        for (int i = 0; i < n; i++) {
            if (R.nextInt(3) != 0) continue;
            double a = R.nextDouble() * Math.PI * 2, r = 0.5 + R.nextDouble() * 0.7;
            mc.world.addParticle(pe, mc.player.getX() + Math.cos(a) * r, mc.player.getY() + R.nextDouble() * 2.0,
                    mc.player.getZ() + Math.sin(a) * r, 0, 0.01, 0);
        }
    }

    private Aura() { }
}
