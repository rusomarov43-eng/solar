package dev.burmalda;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Who you are aiming at / just hit. Only entities inside normal reach and in line of sight. */
public final class TargetTracker {
    public static LivingEntity target;
    public static long lastSeen;
    public static final List<Dmg> DMG = new ArrayList<>();

    private static LivingEntity tracked;
    private static float lastHp;
    private static long lastAttack;
    private static boolean killFired;
    private static final Random RNG = new Random();

    public static final class Dmg {
        public final Vec3d pos;
        public final String text;
        public final long born;
        Dmg(Vec3d pos, String text, long born) { this.pos = pos; this.text = text; this.born = born; }
    }

    public static void onAttack(Entity e) {
        if (e instanceof LivingEntity le) {
            tracked = le;
            lastHp = le.getHealth();
            lastAttack = System.currentTimeMillis();
            target = le;
            lastSeen = lastAttack;
            killFired = false;
            HitEffects.onHit(MinecraftClient.getInstance(), le);
        }
    }

    public static void tick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) {
            target = null; tracked = null; DMG.clear();
            return;
        }
        long now = System.currentTimeMillis();

        if (mc.targetedEntity instanceof LivingEntity le && le.isAlive()) {
            target = le;
            lastSeen = now;
        }
        if (target != null && (target.isRemoved() || now - lastSeen > 3500)) target = null;

        if (tracked != null) {
            if (tracked.isRemoved() || now - lastAttack > 3000) {
                tracked = null;
            } else {
                float hp = tracked.getHealth();
                if (hp < lastHp - 0.01f && Modules.DAMAGE.enabled) spawn(tracked, lastHp - hp);
                if (hp <= 0f && !killFired) { killFired = true; HitEffects.onKill(mc, tracked); }
                lastHp = hp;
            }
        }
    }

    private static void spawn(LivingEntity e, float dmg) {
        Vec3d p = new Vec3d(
                e.getX() + (RNG.nextDouble() - 0.5) * 0.8,
                e.getY() + e.getHeight() * 0.85,
                e.getZ() + (RNG.nextDouble() - 0.5) * 0.8);
        DMG.add(new Dmg(p, String.format(Locale.ROOT, "-%.2f", dmg), System.currentTimeMillis()));
    }

    private TargetTracker() { }
}
