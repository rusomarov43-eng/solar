package dev.burmalda;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/** Everything that happens (visually) when you hit something. Nothing is sent to the server. */
public final class HitEffects {
    public static int combo, kills, deaths;
    private static boolean wasDead;
    public static long lastHit;
    public static double reach;
    private static final Random R = new Random();

    public static void onHit(MinecraftClient mc, LivingEntity e) {
        if (mc.player == null || mc.world == null) return;
        long now = System.currentTimeMillis();
        combo = (now - lastHit < 3000) ? combo + 1 : 1;
        lastHit = now;

        Box b = e.getBoundingBox();
        Vec3d eye = mc.player.getEyePos();
        double dx = MathHelper.clamp(eye.x, b.minX, b.maxX) - eye.x;
        double dy = MathHelper.clamp(eye.y, b.minY, b.maxY) - eye.y;
        double dz = MathHelper.clamp(eye.z, b.minZ, b.maxZ) - eye.z;
        reach = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (Modules.HIT_SOUND.enabled) {
            SoundEvent s = switch (Modules.HIT_SOUND.m("Sound")) {
                case 0 -> SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
                case 1 -> SoundEvents.ENTITY_ARROW_HIT_PLAYER;
                case 2 -> SoundEvents.BLOCK_ANVIL_LAND;
                default -> SoundEvents.ENTITY_PLAYER_ATTACK_CRIT;
            };
            mc.getSoundManager().play(PositionedSoundInstance.master(s, 1.2f, Modules.HIT_SOUND.f("Volume")));
        }
        if (Modules.HIT_PARTICLES.enabled) {
            ParticleEffect pe = switch (Modules.HIT_PARTICLES.m("Type")) {
                case 0 -> ParticleTypes.CRIT;
                case 1 -> ParticleTypes.ENCHANTED_HIT;
                case 2 -> ParticleTypes.FLAME;
                case 3 -> ParticleTypes.HEART;
                case 4 -> ParticleTypes.TOTEM_OF_UNDYING;
                default -> ParticleTypes.SOUL_FIRE_FLAME;
            };
            burst(mc, e, pe, (int) Modules.HIT_PARTICLES.f("Amount"), 0.4);
        }
        if (Modules.HIT_WAVE.enabled) {
            Rings.add(e.getX(), e.getY(), e.getZ(), Modules.HIT_WAVE.m("Color"), Modules.HIT_WAVE.f("Size"), 700);
        }
    }

    public static void onKill(MinecraftClient mc, LivingEntity e) {
        kills++;
        if (!Modules.KILL.enabled || mc.world == null) return;
        ParticleEffect pe = switch (Modules.KILL.m("Effect")) {
            case 0 -> ParticleTypes.TOTEM_OF_UNDYING;
            case 1 -> ParticleTypes.SOUL_FIRE_FLAME;
            default -> ParticleTypes.HEART;
        };
        burst(mc, e, pe, 36, 0.7);
    }

    public static void tick(MinecraftClient mc) {
        if (mc.player != null && mc.player.hurtTime > 0) combo = 0;
        if (mc.player != null) {
            boolean dead = mc.player.getHealth() <= 0f;
            if (dead && !wasDead) deaths++;
            wasDead = dead;
        }
    }

    private static void burst(MinecraftClient mc, LivingEntity e, ParticleEffect pe, int n, double speed) {
        for (int i = 0; i < n; i++) {
            mc.world.addParticle(pe,
                    e.getX() + (R.nextDouble() - 0.5) * e.getWidth(),
                    e.getY() + R.nextDouble() * e.getHeight(),
                    e.getZ() + (R.nextDouble() - 0.5) * e.getWidth(),
                    (R.nextDouble() - 0.5) * speed, R.nextDouble() * speed * 0.8, (R.nextDouble() - 0.5) * speed);
        }
    }

    private HitEffects() { }
}
