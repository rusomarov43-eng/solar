package dev.burmalda;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.AddServerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.MultiplayerServerListPinger;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.Collections;

/** Glass server list with live ping, MOTD and player count. */
public class ServerSelectScreen extends Screen {
    private static final int ROW = 54;
    private final Screen parent;
    private ServerList servers;
    private final MultiplayerServerListPinger pinger = new MultiplayerServerListPinger();
    private int selected = -1, lastClickIdx = -1;
    private long lastClick;
    private float scroll, target;
    private boolean confirmDelete;
    private final GBtn join, add, edit, delete, refresh, classic, back;
    private final GBtn[] btns;
    private final long opened = System.currentTimeMillis();

    public ServerSelectScreen(Screen parent) {
        super(Text.literal(Brand.NAME));
        this.parent = parent;
        join = new GBtn("Join", this::joinSelected).primary().icon("play");
        add = new GBtn("Add", this::addServer);
        edit = new GBtn("Edit", this::editServer);
        delete = new GBtn("Delete", this::deleteServer);
        refresh = new GBtn("Refresh", this::reload);
        classic = new GBtn("Classic", () -> MinecraftClient.getInstance().setScreen(new MultiplayerScreen(this)));
        back = new GBtn("Back", this::close);
        btns = new GBtn[]{join, add, edit, delete, refresh, classic, back};
    }

    @Override protected void init() { reload(); }

    private void reload() {
        MinecraftClient mc = MinecraftClient.getInstance();
        pinger.cancel();
        servers = new ServerList(mc);
        servers.loadFile();
        for (int i = 0; i < servers.size(); i++) {
            ServerInfo info = servers.get(i);
            info.label = Text.translatable("multiplayer.status.pinging");
            info.ping = -2L;
            info.playerListSummary = Collections.emptyList();
            try { pinger.add(info, servers::saveFile); }
            catch (Exception e) { info.label = Text.literal("Can't resolve hostname"); info.ping = -1L; }
        }
        if (selected >= servers.size()) selected = servers.size() - 1;
        if (selected < 0 && servers.size() > 0) selected = 0;
        confirmDelete = false;
    }

    private ServerInfo sel() { return servers != null && selected >= 0 && selected < servers.size() ? servers.get(selected) : null; }

    private void joinSelected() {
        ServerInfo info = sel();
        if (info == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        ConnectScreen.connect(this, mc, ServerAddress.parse(info.address), info, false);
    }

    private void addServer() {
        ServerInfo info = new ServerInfo(I18n.translate("selectServer.defaultName"), "", false);
        MinecraftClient.getInstance().setScreen(new AddServerScreen(this, ok -> {
            if (ok) { servers.add(info, false); servers.saveFile(); }
            MinecraftClient.getInstance().setScreen(this);
            reload();
        }, info));
    }

    private void editServer() {
        ServerInfo info = sel();
        if (info == null) return;
        MinecraftClient.getInstance().setScreen(new AddServerScreen(this, ok -> {
            if (ok) servers.saveFile();
            MinecraftClient.getInstance().setScreen(this);
            reload();
        }, info));
    }

    private void deleteServer() {
        ServerInfo info = sel();
        if (info == null) return;
        if (!confirmDelete) { confirmDelete = true; return; }
        servers.remove(info);
        servers.saveFile();
        reload();
    }

    @Override public void tick() { pinger.tick(); }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        float t = (System.currentTimeMillis() - opened) / 1000f;
        int accent = Colors.client();
        Backgrounds.draw(c, width, height, accent, t);
        c.fillGradient(0, 0, width, height, Colors.a(0x000000, 40), Colors.a(0x000000, 110));

        UI.drawBold(c, textRenderer, "Multiplayer", 24, 20, 0xFFFFFFFF);
        UI.draw(c, textRenderer, (servers == null ? 0 : servers.size()) + " servers", 24, 33, 0x99FFFFFF);

        int pw = Math.min(560, width - 40), px = (width - pw) / 2, py = 56, ph = height - 56 - 70;
        Draw.glass(c, px - 8, py - 8, pw + 16, ph + 16, 14, accent, 1f);
        int n = servers == null ? 0 : servers.size();
        target = Math.max(0, Math.min(Math.max(0, n * ROW - ph), target));
        scroll += (target - scroll) * 0.25f;
        if (n == 0) UI.centered(c, textRenderer, "No servers yet - press \"Add\"", width / 2, py + 30, 0xAAFFFFFF);

        c.enableScissor(px, py, px + pw, py + ph);
        for (int i = 0; i < n; i++) {
            int y = py + i * ROW - (int) scroll;
            if (y + ROW < py || y > py + ph) continue;
            ServerInfo s = servers.get(i);
            boolean hov = mx >= px && mx < px + pw && my >= y && my < y + ROW - 4 && my >= py && my < py + ph;
            boolean on = i == selected;
            Draw.rrect(c, px, y, pw, ROW - 6, 10,
                    on ? Colors.a(Colors.lerp(0x10121E, accent, 0.30f), 210) : Colors.a(0xFFFFFF, hov ? 22 : 10));
            if (on) Draw.outline(c, px, y, pw, ROW - 6, 10, Colors.a(accent, 200));

            UI.drawBold(c, textRenderer, UI.trim(textRenderer, s.name, pw - 170), px + 14, y + 8, 0xFFFFFFFF);
            String motd = s.label == null ? "" : s.label.getString().replace('\n', ' ');
            UI.draw(c, textRenderer, UI.trim(textRenderer, motd, pw - 170), px + 14, y + 22, 0xAAFFFFFF);
            String addr = s.address == null ? "" : s.address;
            UI.draw(c, textRenderer, UI.trim(textRenderer, addr, pw - 170), px + 14, y + 34, 0x66FFFFFF);

            String players = s.playerCountLabel == null ? "" : s.playerCountLabel.getString();
            UI.draw(c, textRenderer, players, px + pw - 14 - UI.w(textRenderer, players), y + 22, 0xCCFFFFFF);
            String ping;
            int pcol;
            if (s.ping == -2L) { ping = "..."; pcol = 0xAAFFFFFF; }
            else if (s.ping < 0 && !s.online) { ping = "offline"; pcol = 0xFFFF7777; }
            else { ping = s.ping + " ms"; pcol = s.ping < 80 ? 0xFF6BFF8F : s.ping < 200 ? 0xFFFFD166 : 0xFFFF7777; }
            Icons.draw(c, "ping", px + pw - 24 - UI.w(textRenderer, ping) - 8, y + 9, 1, pcol);
            UI.draw(c, textRenderer, ping, px + pw - 14 - UI.w(textRenderer, ping), y + 9, pcol);
        }
        c.disableScissor();

        delete.label = confirmDelete ? "Sure?" : "Delete";
        delete.danger = confirmDelete;
        join.enabled = sel() != null;
        edit.enabled = sel() != null;
        delete.enabled = sel() != null;
        int bw = 96, gap = 8, total = btns.length * bw + (btns.length - 1) * gap;
        int bx = (width - total) / 2, by = height - 52;
        for (int i = 0; i < btns.length; i++) btns[i].at(bx + i * (bw + gap), by, bw, 30);
        for (GBtn b : btns) b.render(c, textRenderer, mx, my, accent);

        MouseTrail.update(mx, my);
        MouseTrail.render(c, accent, 1f);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        MouseTrail.click(mx, my);
        if (button != 0) return super.mouseClicked(mx, my, button);
        for (GBtn b : btns) if (b.click(mx, my)) return true;
        int pw = Math.min(560, width - 40), px = (width - pw) / 2, py = 56, ph = height - 56 - 70;
        if (mx >= px && mx < px + pw && my >= py && my < py + ph && servers != null) {
            int i = (int) ((my - py + scroll) / ROW);
            if (i >= 0 && i < servers.size()) {
                long now = System.currentTimeMillis();
                if (i == lastClickIdx && now - lastClick < 400) joinSelected();
                selected = i; lastClickIdx = i; lastClick = now; confirmDelete = false;
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        target -= (float) amount * 40f;
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ENTER) { joinSelected(); return true; }
        if (key == GLFW.GLFW_KEY_DOWN && servers != null && selected < servers.size() - 1) { selected++; return true; }
        if (key == GLFW.GLFW_KEY_UP && selected > 0) { selected--; return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override public void removed() { pinger.cancel(); }
    @Override public void close() { MinecraftClient.getInstance().setScreen(parent); }
}
