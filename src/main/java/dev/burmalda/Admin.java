package dev.burmalda;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

/**
 * Admin panel lock. The first passphrase you enter is stored (hashed) next to your config; afterwards it
 * must match. This is a door lock for your own install, not a vault: every button only sends an ordinary
 * chat command, so the server / world still decides what you are allowed to do.
 */
public final class Admin {
    private static boolean unlocked;

    private static Path keyFile() { return FabricLoader.getInstance().getConfigDir().resolve(Brand.ID).resolve("admin.key"); }

    public static boolean hasKey() { return Files.exists(keyFile()); }
    public static boolean unlocked() { return unlocked; }
    public static void lock() { unlocked = false; }

    /** Creates the passphrase on first use, otherwise verifies it. */
    public static boolean submit(String pass) {
        if (pass == null || pass.length() < 4) return false;
        String h = sha(pass);
        try {
            if (!hasKey()) {
                Files.createDirectories(keyFile().getParent());
                Files.writeString(keyFile(), h);
                unlocked = true;
                return true;
            }
            if (Files.readString(keyFile()).trim().equals(h)) { unlocked = true; return true; }
        } catch (IOException ignored) { }
        return false;
    }

    private static String sha(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(("nebula-admin:" + s).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static void cmd(String c) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.player.networkHandler == null) { Notifications.push("Join a world first"); return; }
        mc.player.networkHandler.sendChatCommand(c);
        Notifications.push("/" + c);
    }

    private Admin() { }
}
