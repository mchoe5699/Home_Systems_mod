package kr.mchoe.homesystems.client;

import java.util.*;
import kr.mchoe.homesystems.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.util.Mth;

public final class DashboardScreen extends AbstractContainerScreen<DashboardMenu> {
    private enum Mode { SELECT, CREATE, MERGE, SPLIT }
    private Mode mode = Mode.SELECT;
    private DashboardNetwork.State state;
    private final Map<UUID, DashboardNetwork.Group> groups = new HashMap<>();
    private final Map<Long, EstateData.Parcel> cells = new HashMap<>();
    private int cacheMinX = Integer.MIN_VALUE, cacheMinZ, cacheMaxX, cacheMaxZ;
    private UUID selected = EstateData.NONE;
    private EditBox name;
    private Button rename, create, merge, split, delete;
    private double centerX, centerZ, zoom = 12;
    private int mapLeft, mapTop, mapRight, mapBottom, startX, startZ, dragX, dragZ;
    private boolean initialized, drawing, panning, confirmDelete;
    private String status = "loading";
    private long lastRequest;

    public DashboardScreen(DashboardMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }
    public DashboardNetwork.State snapshot() { return state; }
    public static void receive(DashboardNetwork.State state) {
        if (Minecraft.getInstance().screen instanceof DashboardScreen screen && screen.menu.containerId == state.container()) screen.accept(state);
    }
    private static Component tr(String key) { return Component.translatable("dashboard.homesystems." + key); }
    private void accept(DashboardNetwork.State value) {
        state = value; groups.clear(); value.groups().forEach(g -> groups.put(g.id(), g)); cacheMinX = Integer.MIN_VALUE;
        if (!value.status().isEmpty()) status = value.status().equals("split") ? "split_done" : value.status();
        if (value.status().equals("merged") || value.status().equals("split") || value.status().equals("created")) mode = Mode.SELECT;
        if (value.parcels().stream().noneMatch(p -> p.id().equals(selected))) selected = EstateData.NONE;
        if (name != null && (value.status().equals("split") || value.status().equals("renamed") || value.status().equals("merged"))) {
            var part = selectedParcel();
            if (part != null) name.setValue(groups.get(part.estate()).name());
        }
        updateButtons();
    }
    @Override protected void init() {
        imageWidth = Math.min(760, width - 16); imageHeight = Math.min(430, height - 16);
        super.init();
        mapLeft = leftPos + 12; mapTop = topPos + 40; mapRight = leftPos + imageWidth - 136; mapBottom = topPos + imageHeight - 32;
        if (!initialized) {
            centerX = minecraft.player.getX() / EstateData.UNIT; centerZ = minecraft.player.getZ() / EstateData.UNIT; initialized = true;
        }
        int side = leftPos + imageWidth - 124;
        String existing = name == null ? tr("new_name").getString() : name.getValue();
        name = new EditBox(font, side, topPos + 44, 112, 18, tr("name")); name.setMaxLength(32); name.setValue(existing); addRenderableWidget(name);
        rename = addRenderableWidget(Button.builder(tr("rename"), b -> send(DashboardNetwork.Action.RENAME, selected, EstateData.NONE)).bounds(side, topPos + 66, 112, 18).build());
        create = addRenderableWidget(Button.builder(tr("create"), b -> setMode(Mode.CREATE)).bounds(side, topPos + 88, 112, 18).build());
        merge = addRenderableWidget(Button.builder(tr("merge"), b -> setMode(Mode.MERGE)).bounds(side, topPos + 110, 112, 18).build());
        split = addRenderableWidget(Button.builder(tr("split"), b -> setMode(Mode.SPLIT)).bounds(side, topPos + 132, 112, 18).build());
        delete = addRenderableWidget(Button.builder(tr("delete"), b -> {
            if (confirmDelete) { send(DashboardNetwork.Action.DELETE, selected, EstateData.NONE); confirmDelete = false; }
            else { confirmDelete = true; status = "delete_confirm"; updateButtons(); }
        }).bounds(side, topPos + 154, 112, 18).build());
        addRenderableWidget(Button.builder(tr("center"), b -> {
            centerX = minecraft.player.getX() / EstateData.UNIT; centerZ = minecraft.player.getZ() / EstateData.UNIT; refresh();
        }).bounds(side, topPos + 176, 112, 18).build());
        updateButtons(); refresh();
    }
    private void setMode(Mode value) { mode = mode == value ? Mode.SELECT : value; confirmDelete = false; status = "mode_" + mode.name().toLowerCase(Locale.ROOT); updateButtons(); }
    private EstateData.Parcel selectedParcel() { return state == null ? null : state.parcels().stream().filter(p -> p.id().equals(selected)).findFirst().orElse(null); }
    private void updateButtons() {
        if (rename == null) return;
        EstateData.Parcel p = selectedParcel(); var g = p == null ? null : groups.get(p.estate()); boolean editable = g != null && g.editable();
        rename.active = delete.active = editable;
        create.active = state != null;
        merge.active = state != null && groups.values().stream().filter(DashboardNetwork.Group::editable).count() > 1;
        split.active = state != null && state.parcels().stream().anyMatch(part -> groups.get(part.estate()).editable() && state.parcels().stream().filter(q -> q.estate().equals(part.estate())).count() > 1);
        create.setMessage(tr(mode == Mode.CREATE ? "creating" : "create"));
        merge.setMessage(tr(mode == Mode.MERGE ? "merging" : "merge"));
        split.setMessage(tr(mode == Mode.SPLIT ? "splitting" : "split"));
        delete.setMessage(tr(confirmDelete ? "confirm" : "delete"));
    }
    private void refresh() { send(DashboardNetwork.Action.REFRESH, EstateData.NONE, EstateData.NONE); }
    private void send(DashboardNetwork.Action action, UUID first, UUID second) {
        long now = System.currentTimeMillis(); if (now - lastRequest < 250) { status = "wait"; return; } lastRequest = now;
        DeviceNetwork.CHANNEL.sendToServer(new DashboardNetwork.Request(menu.containerId, action, (int)Math.floor(centerX), (int)Math.floor(centerZ),
            startX, startZ, dragX, dragZ, first, second, name.getValue()));
    }
    private double worldX(double mouse) { return centerX + (mouse - (mapLeft + mapRight) / 2.0) / zoom; }
    private double worldZ(double mouse) { return centerZ + (mouse - (mapTop + mapBottom) / 2.0) / zoom; }
    private int screenX(int x) { return (int)Math.floor((mapLeft + mapRight) / 2.0 + (x - centerX) * zoom); }
    private int screenZ(int z) { return (int)Math.floor((mapTop + mapBottom) / 2.0 + (z - centerZ) * zoom); }
    private boolean onMap(double x, double y) { return x >= mapLeft && x < mapRight && y >= mapTop && y < mapBottom; }
    private static long key(int x, int z) { return ((long)x << 32) ^ (z & 0xffffffffL); }
    private void rebuild(int minX, int minZ, int maxX, int maxZ) {
        if (cacheMinX == minX && cacheMinZ == minZ && cacheMaxX == maxX && cacheMaxZ == maxZ) return;
        cacheMinX = minX; cacheMinZ = minZ; cacheMaxX = maxX; cacheMaxZ = maxZ; cells.clear();
        if (state == null) return;
        for (var p : state.parcels()) for (int x = Math.max(minX, p.minX()); x <= Math.min(maxX, p.maxX()); x++)
            for (int z = Math.max(minZ, p.minZ()); z <= Math.min(maxZ, p.maxZ()); z++) cells.put(key(x, z), p);
    }
    private EstateData.Parcel parcelAt(int x, int z) {
        return state == null ? null : state.parcels().stream().filter(p -> p.contains(x, z)).findFirst().orElse(null);
    }
    private void select(EstateData.Parcel p) {
        selected = p == null ? EstateData.NONE : p.id(); confirmDelete = false;
        if (p != null) { name.setValue(groups.get(p.estate()).name()); status = "selected"; } else status = "mode_select";
        updateButtons();
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (onMap(x, y)) {
            name.setFocused(false);
            if (button == 1 || button == 2) { panning = true; return true; }
            if (button == 0) {
                int gridX = (int)Math.floor(worldX(x)), gridZ = (int)Math.floor(worldZ(y));
                if (mode == Mode.CREATE) { drawing = true; startX = dragX = gridX; startZ = dragZ = gridZ; return true; }
                var clicked = parcelAt(gridX, gridZ);
                var current = selectedParcel();
                if (mode == Mode.MERGE && current != null && clicked != null && !current.estate().equals(clicked.estate())) send(DashboardNetwork.Action.MERGE, current.id(), clicked.id());
                else if (mode == Mode.SPLIT && clicked != null) { selected = clicked.id(); send(DashboardNetwork.Action.SPLIT, clicked.id(), EstateData.NONE); }
                else select(clicked);
                return true;
            }
        }
        return super.mouseClicked(x, y, button);
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (panning) { centerX -= dx / zoom; centerZ -= dy / zoom; centerX = Mth.clamp(centerX, -1800000, 1800000); centerZ = Mth.clamp(centerZ, -1800000, 1800000); return true; }
        if (drawing) { dragX = (int)Math.floor(worldX(Mth.clamp(x, mapLeft, mapRight - 1))); dragZ = (int)Math.floor(worldZ(Mth.clamp(y, mapTop, mapBottom - 1))); return true; }
        return super.mouseDragged(x, y, button, dx, dy);
    }
    @Override public boolean mouseReleased(double x, double y, int button) {
        if (drawing && button == 0) { drawing = false; send(DashboardNetwork.Action.CREATE, EstateData.NONE, EstateData.NONE); return true; }
        if (panning) { panning = false; refresh(); return true; }
        return super.mouseReleased(x, y, button);
    }
    @Override public boolean mouseScrolled(double x, double y, double delta) {
        if (onMap(x, y)) {
            double beforeX = worldX(x), beforeZ = worldZ(y); zoom = Mth.clamp(zoom * (delta > 0 ? 1.25 : .8), 4, 32);
            centerX += beforeX - worldX(x); centerZ += beforeZ - worldZ(y); return true;
        }
        return super.mouseScrolled(x, y, delta);
    }
    private int terrain(int x, int z) {
        if (state == null) return 0x192833;
        int sx = x - state.centerX() + DashboardNetwork.WIDTH / 2, sz = z - state.centerZ() + DashboardNetwork.HEIGHT / 2;
        return sx < 0 || sx >= DashboardNetwork.WIDTH || sz < 0 || sz >= DashboardNetwork.HEIGHT ? 0x192833 : state.colors()[sz * DashboardNetwork.WIDTH + sx];
    }
    private static int color(UUID id) { return 0xFF000000 | (0x608080 ^ (id.hashCode() & 0x7f7f7f)); }
    private static boolean same(EstateData.Parcel a, EstateData.Parcel b) { return a != null && b != null && a.estate().equals(b.estate()); }
    @Override protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        g.fill(leftPos - 2, topPos - 2, leftPos + imageWidth + 2, topPos + imageHeight + 2, 0xFF4F697A);
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF152330);
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + 28, 0xFF223B4A);
        g.fill(mapLeft - 1, mapTop - 1, mapRight + 1, mapBottom + 1, 0xFF688391);
        int minX = (int)Math.floor(worldX(mapLeft)) - 1, maxX = (int)Math.ceil(worldX(mapRight)) + 1;
        int minZ = (int)Math.floor(worldZ(mapTop)) - 1, maxZ = (int)Math.ceil(worldZ(mapBottom)) + 1;
        rebuild(minX, minZ, maxX, maxZ);
        var chosen = selectedParcel();
        g.enableScissor(mapLeft, mapTop, mapRight, mapBottom);
        for (int z = minZ; z <= maxZ; z++) for (int x = minX; x <= maxX; x++) {
            int x1 = screenX(x), z1 = screenZ(z), x2 = screenX(x + 1), z2 = screenZ(z + 1);
            g.fill(x1, z1, x2, z2, 0xFF000000 | terrain(x, z));
            var p = cells.get(key(x, z));
            if (p != null) {
                int c = color(p.estate()); boolean highlight = chosen != null && chosen.estate().equals(p.estate());
                g.fill(x1, z1, x2, z2, (highlight ? 0x77000000 : 0x44000000) | (c & 0xffffff));
                int border = highlight ? 0xFFFFE18A : c;
                if (!same(p, cells.get(key(x - 1, z)))) g.fill(x1, z1, x1 + 2, z2, border);
                if (!same(p, cells.get(key(x + 1, z)))) g.fill(x2 - 2, z1, x2, z2, border);
                if (!same(p, cells.get(key(x, z - 1)))) g.fill(x1, z1, x2, z1 + 2, border);
                if (!same(p, cells.get(key(x, z + 1)))) g.fill(x1, z2 - 2, x2, z2, border);
                if (mode == Mode.SPLIT) {
                    var right = cells.get(key(x + 1, z)); var down = cells.get(key(x, z + 1));
                    if (same(p, right) && !p.id().equals(right.id())) for (int yy = z1; yy < z2; yy += 4) g.fill(x2 - 1, yy, x2, Math.min(yy + 2, z2), 0xFFE0E5E8);
                    if (same(p, down) && !p.id().equals(down.id())) for (int xx = x1; xx < x2; xx += 4) g.fill(xx, z2 - 1, Math.min(xx + 2, x2), z2, 0xFFE0E5E8);
                }
            } else if (zoom >= 10) { g.fill(x1, z1, x1 + 1, z2, 0x33405060); g.fill(x1, z1, x2, z1 + 1, 0x33405060); }
        }
        if (state != null) for (var group : state.groups()) {
            var p = state.parcels().stream().filter(part -> part.estate().equals(group.id()) && part.maxX() >= minX && part.minX() <= maxX && part.maxZ() >= minZ && part.minZ() <= maxZ).findFirst().orElse(null);
            if (p != null && screenX(p.maxX() + 1) - screenX(p.minX()) >= 40 && screenZ(p.maxZ() + 1) - screenZ(p.minZ()) >= 14) {
                int x = screenX(Math.max(p.minX(), minX + 1)) + 4, z = screenZ(Math.max(p.minZ(), minZ + 1)) + 3;
                int available = Math.min(screenX(p.maxX() + 1), mapRight) - x - 4;
                if (available >= 16) g.drawString(font, font.plainSubstrByWidth(group.name(), available), x, z, 0xFFFFFFFF, true);
            }
        }
        if (drawing) {
            int x1 = screenX(Math.min(startX, dragX)), x2 = screenX(Math.max(startX, dragX) + 1), z1 = screenZ(Math.min(startZ, dragZ)), z2 = screenZ(Math.max(startZ, dragZ) + 1);
            g.fill(x1, z1, x2, z2, 0x6656D7BE); g.renderOutline(x1, z1, x2 - x1, z2 - z1, 0xFF56D7BE);
        }
        int px = (int)((mapLeft + mapRight) / 2.0 + (minecraft.player.getX() / EstateData.UNIT - centerX) * zoom);
        int pz = (int)((mapTop + mapBottom) / 2.0 + (minecraft.player.getZ() / EstateData.UNIT - centerZ) * zoom);
        g.fill(px - 3, pz - 1, px + 4, pz + 2, 0xFFFFFFFF); g.fill(px - 1, pz - 3, px + 2, pz + 4, 0xFFFFFFFF);
        g.disableScissor();
    }
    @Override protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, 12, 10, 0xFFEAF2F8, false);
        g.drawString(font, tr("name"), imageWidth - 124, 33, 0xFF99B0C0, false);
        g.drawString(font, font.plainSubstrByWidth(tr(status).getString(), imageWidth - 24), 12, imageHeight - 17, 0xFF56D7BE, false);
        g.drawString(font, font.plainSubstrByWidth(tr("map_hint").getString(), mapRight - mapLeft), 12, imageHeight - 29, 0xFF99B0C0, false);
    }
    @Override public void containerTick() { super.containerTick(); name.tick(); }
    @Override public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g); super.render(g, mx, my, partial);
        if (onMap(mx, my) && !drawing && !panning) {
            var parcel = parcelAt((int)Math.floor(worldX(mx)), (int)Math.floor(worldZ(my)));
            if (parcel != null) g.renderTooltip(font, Component.literal(groups.get(parcel.estate()).name()), mx, my);
        }
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == 256) { onClose(); return true; }
        if (name.isFocused() && key == minecraft.options.keyInventory.getKey().getValue()) return true;
        return super.keyPressed(key, scan, modifiers);
    }
}
