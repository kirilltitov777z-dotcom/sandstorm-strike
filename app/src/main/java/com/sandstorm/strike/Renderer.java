package com.sandstorm.strike;

import android.graphics.Bitmap;
import android.graphics.Color;
import java.util.List;
import java.util.Random;

/** Software raycasting renderer into an int ARGB buffer. */
public final class Renderer {

    public int W, H;
    public int[] buf;
    public float[] zbuf;
    public Bitmap bitmap;

    private final int[][] wallTex = new int[6][];
    private final int[] floorSand, floorTile, floorBalcony, skyTex;
    private final Random RND = new Random();

    // camera plane derived from FOV
    private float dirX, dirY, planeX, planeY;
    private float posX, posY, posZ;
    private final World.Hit hit = new World.Hit();

    public Renderer(int w, int h) {
        resize(w, h);
        for (int i = 0; i < 6; i++) wallTex[i] = GameData.makeTexture(i);
        floorSand = GameData.makeTexture(5);
        floorTile = GameData.makeTexture(6);
        floorBalcony = GameData.makeTexture(7);
        skyTex = GameData.makeTexture(8);
    }

    public void resize(int w, int h) {
        this.W = w; this.H = h;
        buf = new int[w * h];
        zbuf = new float[w];
        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
    }

    private float lerpF(float a, float b, float t) { return a + (b - a) * t; }

    public void setSkyTexture(int[] tex) {
        if (tex == null) return;
        for (int i = 0; i < skyTex.length; i++) skyTex[i] = tex[i % tex.length];
    }

    /** Render one frame. */
    public void render(Entities.Player p, World world, List<Entities.Player> players,
                       Entities.Bomb bomb, List<Entities.Pickup> pickups,
                       List<Entities.Particle> particles, List<Entities.Corpse> corpses,
                       float time, int killEffect, float killEffectT, int quality) {

        posX = p.x; posY = p.y;
        // eye height: 0.5 baseline + elevation, lower if crouching
        posZ = (p.crouching ? 0.32f : 0.5f) + p.elevation;

        float fov = 0.66f; // ~66 deg
        dirX = (float)Math.cos(p.dir); dirY = (float)Math.sin(p.dir);
        planeX = -dirY * fov; planeY = dirX * fov;

        int horizon = H / 2 + (int)(p.pitch * H);
        if (horizon < 0) horizon = 0; if (horizon >= H) horizon = H - 1;

        drawFloorCeiling(world, horizon);
        drawWalls(world, horizon);
        drawSprites(p, players, bomb, pickups, corpses, horizon);
        drawParticles(p, particles, horizon);
        drawWeapon(p, time);
        if (killEffectT > 0) drawKillEffect(killEffect, killEffectT, time);

        bitmap.setPixels(buf, 0, W, 0, 0, W, H);
    }

    // ---- Floor & ceiling casting ------------------------------------------
    private void drawFloorCeiling(World world, int horizon) {
        int floorTexId = 5;
        float rayX0 = dirX - planeX, rayY0 = dirY - planeY;
        float rayX1 = dirX + planeX, rayY1 = dirY + planeY;

        int yStart = horizon + 1;
        for (int y = yStart; y < H; y++) {
            int p = y - horizon;
            if (p <= 0) continue;
            float rowDist = posZ * H / p;
            if (rowDist > 60f) rowDist = 60f;
            float stepX = rowDist * (rayX1 - rayX0) / W;
            float stepY = rowDist * (rayY1 - rayY0) / W;
            float fx = posX + rowDist * rayX0;
            float fy = posY + rowDist * rayY0;
            int idx = y * W;
            for (int x = 0; x < W; x++) {
                int mx = (int)fx, my = (int)fYclamp(fy);
                char c = world.cellAt(mx, my);
                int[] tex = floorSand;
                if (c == '^' || c == '=' || c == '-') tex = floorBalcony;
                else if (c == 'A' || c == 'B' || c == '.' ) tex = floorSand;
                else tex = floorTile;
                int tx = (int)((fx - mx) * GameData.TEX) & (GameData.TEX - 1);
                int ty = (int)((fy - my) * GameData.TEX) & (GameData.TEX - 1);
                int col = tex[ty * GameData.TEX + tx];
                float fog = 1f - Math.min(1f, rowDist / 30f);
                col = GameData.shade(col, (int)(-40 * (1f - fog)));
                buf[idx + x] = col;
                fx += stepX; fy += stepY;
            }
        }

        // ceiling / sky
        for (int y = 0; y < horizon; y++) {
            int p = horizon - y;
            float rowDist = (1.0f - posZ) * H / Math.max(1, p);
            if (rowDist > 60f) rowDist = 60f;
            float stepX = rowDist * (rayX1 - rayX0) / W;
            float stepY = rowDist * (rayY1 - rayY0) / W;
            float fx = posX + rowDist * rayX0;
            float fy = posY + rowDist * rayY0;
            int idx = y * W;
            for (int x = 0; x < W; x++) {
                int mx = (int)fx, my = (int)fy;
                if (world.isSolid(mx, my)) {
                    int tx = (int)((fx - mx) * GameData.TEX) & (GameData.TEX - 1);
                    int ty = (int)((fy - my) * GameData.TEX) & (GameData.TEX - 1);
                    buf[idx + x] = GameData.shade(floorTile[ty * GameData.TEX + tx], -30);
                } else {
                    // sky gradient with sun glow, sampled by screen position
                    float u = x / (float)W;
                    float v = y / (float)Math.max(1, horizon);
                    int sx = (int)(u * GameData.TEX) & (GameData.TEX - 1);
                    int sy = (int)(v * GameData.TEX) & (GameData.TEX - 1);
                    int col = skyTex[sy * GameData.TEX + sx];
                    float sun = 1f - Math.min(1f, (float)Math.hypot(u - 0.7f, v - 0.25f) * 2.2f);
                    if (sun > 0) col = GameData.lerp(col, 0xFFF3C0, sun * 0.6f);
                    buf[idx + x] = col;
                }
                fx += stepX; fy += stepY;
            }
        }
    }

    private float fYclamp(float v) { return v; }

    // ---- Walls ------------------------------------------------------------
    private void drawWalls(World world, int horizon) {
        for (int x = 0; x < W; x++) {
            float camX = 2f * x / W - 1f;
            float rdx = dirX + planeX * camX;
            float rdy = dirY + planeY * camX;
            world.castRay(posX, posY, rdx, rdy, hit);
            float dist = hit.dist;
            zbuf[x] = dist;

            float wallHeight = world.isLowWall(hit.mx, hit.my) ? 0.5f : 1.0f;

            int lineH = (int)(H / dist * wallHeight);
            float drawStartF = horizon + (posZ - wallHeight) * (H / dist);
            int drawStart = (int)drawStartF;
            int drawEnd = drawStart + lineH;
            int ds = Math.max(0, drawStart), de = Math.min(H, drawEnd);

            int[] tex = wallTex[Math.max(0, Math.min(5, hit.wallType - 1))];
            int texX = (int)(hit.wallX * GameData.TEX);
            if ((hit.side == 0 && rdx > 0) || (hit.side == 1 && rdy < 0)) texX = GameData.TEX - texX - 1;

            float step = GameData.TEX / (float)Math.max(1, lineH);
            float texPos = (ds - drawStartF) * step;
            for (int y = ds; y < de; y++) {
                int ty = (int)texPos & (GameData.TEX - 1);
                int col = tex[ty * GameData.TEX + texX];
                if (hit.side == 1) col = GameData.shade(col, -22);
                float fog = 1f - Math.min(1f, dist / 26f);
                col = GameData.shade(col, (int)(-45 * (1f - fog)));
                buf[y * W + x] = col;
                texPos += step;
            }
        }
    }

    // ---- Sprites ----------------------------------------------------------
    private void drawSprites(Entities.Player viewer, List<Entities.Player> players,
                             Entities.Bomb bomb, List<Entities.Pickup> pickups,
                             List<Entities.Corpse> corpses, int horizon) {
        float invDet = 1f / (planeX * dirY - dirX * planeY);

        // corpses
        for (Entities.Corpse c : corpses) {
            drawBillboard(c.x, c.y, invDet, horizon, 0.35f, 0.5f, 0, 0, 1f);
        }
        // pickups
        for (Entities.Pickup pk : pickups) {
            if (!pk.active) continue;
            drawBillboard(pk.x, pk.y, invDet, horizon, 0.5f, 0.35f, 1, pk.type, 1f);
        }
        // bomb
        if (bomb != null && bomb.state == 1) {
            drawBillboard(bomb.x, bomb.y, invDet, horizon, 0.45f, 0.18f, 2, 0, 1f);
        }
        // players (skip viewer)
        for (Entities.Player p : players) {
            if (p == viewer || !p.alive) continue;
            float scale = p.crouching ? 0.78f : 1f;
            drawBillboard(p.x, p.y, invDet, horizon, 0.9f, 0.5f, 3, p.team, scale, p);
        }
    }

    private void drawBillboard(float wx, float wy, float invDet, int horizon,
                               float heightUnits, float widthUnits, int kind, int meta, float scale) {
        drawBillboard(wx, wy, invDet, horizon, heightUnits, widthUnits, kind, meta, scale, null);
    }

    private void drawBillboard(float wx, float wy, float invDet, int horizon,
                               float heightUnits, float widthUnits, int kind, int meta,
                               float scale, Entities.Player player) {
        float sx = wx - posX, sy = wy - posY;
        float tx = invDet * (dirY * sx - dirX * sy);
        float ty = invDet * (-planeY * sx + planeX * sy);
        if (ty <= 0.08f) return;

        int screenX = (int)((W / 2f) * (1f + tx / ty));
        int spriteH = (int)Math.abs((H / ty) * heightUnits * scale);
        int spriteW = (int)Math.abs((H / ty) * widthUnits * scale);
        if (spriteH <= 0 || spriteW <= 0) return;

        // vertical placement: feet on floor
        float floorOffset = posZ * (H / ty);
        int centerY = horizon + (int)floorOffset - spriteH / 2;
        if (player != null) centerY -= (int)(player.elevation * (H / ty));

        int startX = Math.max(0, screenX - spriteW / 2);
        int endX = Math.min(W, screenX + spriteW / 2);
        int startY = Math.max(0, centerY);
        int endY = Math.min(H, centerY + spriteH);

        for (int x = startX; x < endX; x++) {
            if (ty >= zbuf[x]) continue;
            float u = (x - (screenX - spriteW / 2f)) / spriteW;
            for (int y = startY; y < endY; y++) {
                float v = (y - centerY) / (float)spriteH;
                int col = spritePixel(kind, meta, u, v, ty);
                if (col == 0) continue;
                float fog = 1f - Math.min(1f, ty / 28f);
                buf[y * W + x] = GameData.shade(col, (int)(-40 * (1f - fog)));
            }
        }
    }

    /** Procedural sprite pixel. u,v in 0..1. Returns ARGB (0 = transparent). */
    private int spritePixel(int kind, int meta, float u, float v, float dist) {
        if (kind == 0) { // corpse
            if (v > 0.45f && v < 0.8f && u > 0.2f && u < 0.8f) return 0x5A4030;
            if (v > 0.75f && u > 0.3f && u < 0.7f) return 0x3A2E28;
            return 0;
        }
        if (kind == 1) { // pickup
            int[] colors = {0xE74C3C, 0x3498DB, 0xF1C40F,
                            0xB0B7C0, 0x2E8B57, 0xC0392B, 0x8E44AD, 0xD35400};
            int c = colors[Math.max(0, Math.min(colors.length - 1, meta))];
            float dx = u - 0.5f, dy = v - 0.5f;
            float r = dx * dx + dy * dy;
            if (r < 0.16f) {
                if (r > 0.12f) return 0x101820;
                return c;
            }
            return 0;
        }
        if (kind == 2) { // bomb
            if (u > 0.15f && u < 0.85f && v > 0.25f && v < 0.9f) {
                float pulse = (System.currentTimeMillis() / 250) % 2;
                if (u > 0.6f && u < 0.75f && v > 0.4f && v < 0.55f)
                    return pulse < 1 ? 0xFF2020 : 0x802020;
                return 0x1C2024;
            }
            return 0;
        }
        if (kind == 3) { // soldier
            boolean ct = meta == Entities.TEAM_CT;
            int uniform = ct ? 0x2C4A73 : 0x8A6A2F;
            int vest = ct ? 0x1E3552 : 0x5E4A20;
            int skin = 0xC99B72;
            // head
            if (v < 0.18f && u > 0.36f && u < 0.64f) {
                if (v < 0.06f) return ct ? 0x243B5A : 0x6B5226; // helmet
                return skin;
            }
            // body / vest
            if (v >= 0.18f && v < 0.62f && u > 0.24f && u < 0.76f) {
                if (u > 0.3f && u < 0.7f) return vest;
                return uniform;
            }
            // arms
            if (v >= 0.22f && v < 0.55f && ((u > 0.12f && u < 0.24f) || (u > 0.76f && u < 0.88f)))
                return uniform;
            // legs
            if (v >= 0.62f && v < 0.98f && ((u > 0.3f && u < 0.46f) || (u > 0.54f && u < 0.7f)))
                return uniform;
            // gun
            if (v > 0.4f && v < 0.52f && u > 0.55f && u < 0.95f) return 0x181C20;
            return 0;
        }
        return 0;
    }

    // ---- Particles --------------------------------------------------------
    private void drawParticles(Entities.Player p, List<Entities.Particle> particles, int horizon) {
        float invDet = 1f / (planeX * dirY - dirX * planeY);
        for (Entities.Particle pt : particles) {
            float sx = pt.x - posX, sy = pt.y - posY;
            float tx = invDet * (dirY * sx - dirX * sy);
            float ty = invDet * (-planeY * sx + planeX * sy);
            if (ty <= 0.1f) continue;
            float floorOffset = posZ * (H / ty);
            int syScreen = horizon + (int)floorOffset - (int)(pt.z * H / ty);
            int sxScreen = (int)((W / 2f) * (1f + tx / ty));
            float sz = Math.max(1f, pt.size * H / ty);
            float alpha = Math.max(0, Math.min(1, pt.life / pt.maxLife));
            int col = GameData.shade(pt.color, (int)(-60 * (1 - alpha)));
            if (pt.kind == 1) { // expanding ring
                float rr = pt.radius * H / ty;
                int ir = (int)sz, or = (int)(sz + rr);
                drawRing(sxScreen, syScreen, ir, or, col);
            } else if (pt.kind == 2) { // glyph / square
                int r = (int)(sz * 0.5f);
                fillRect(sxScreen - r, syScreen - r, sxScreen + r, syScreen + r, col);
            } else {
                int r = Math.max(1, (int)sz);
                fillRect(sxScreen - r, syScreen - r, sxScreen + r, syScreen + r, col);
            }
        }
    }

    private void fillRect(int x0, int y0, int x1, int y1, int col) {
        for (int y = Math.max(0, y0); y < Math.min(H, y1); y++) {
            int idx = y * W;
            for (int x = Math.max(0, x0); x < Math.min(W, x1); x++) buf[idx + x] = col;
        }
    }

    private void drawRing(int cx, int cy, int inner, int outer, int col) {
        for (int y = Math.max(0, cy - outer); y < Math.min(H, cy + outer); y++) {
            for (int x = Math.max(0, cx - outer); x < Math.min(W, cx + outer); x++) {
                float d = (float)Math.hypot(x - cx, y - cy);
                if (d >= inner && d <= outer) buf[y * W + x] = col;
            }
        }
    }

    // ---- Viewmodel weapon -------------------------------------------------
    private void drawWeapon(Entities.Player p, float time) {
        int w = p.weapon;
        int baseColor = GameData.SKIN_COLORS[w][p.skin % 5];
        float recoil = p.fireTimer > 0 ? p.fireTimer * 30f : 0f;
        float bobX = (float)Math.sin(p.bobPhase) * 6f * (p.bob / 100f);
        float bobY = (float)Math.abs(Math.cos(p.bobPhase)) * 5f * (p.bob / 100f);
        int cx = W / 2 + (int)bobX;
        int cy = H - (int)(H * 0.06f) + (int)bobY + (int)recoil;
        int scale = Math.max(1, H / 180);

        // barrel
        fillRect(cx - 4 * scale, cy - 70 * scale, cx + 4 * scale, cy - 10 * scale, 0x20252A);
        fillRect(cx - 2 * scale, cy - 74 * scale, cx + 2 * scale, cy - 68 * scale, 0x4A525A);
        // body
        fillRect(cx - 14 * scale, cy - 12 * scale, cx + 14 * scale, cy + 16 * scale, baseColor);
        fillRect(cx - 10 * scale, cy - 8 * scale, cx + 10 * scale, cy + 8 * scale, GameData.shade(baseColor, 30));
        // magazine
        fillRect(cx - 6 * scale, cy + 10 * scale, cx + 6 * scale, cy + 34 * scale, 0x2B3035);
        // grip
        fillRect(cx - 5 * scale, cy + 8 * scale, cx + 3 * scale, cy + 30 * scale, 0x1C2024);
        // hands
        fillRect(cx + 10 * scale, cy + 2 * scale, cx + 22 * scale, cy + 22 * scale, 0xC99B72);
        fillRect(cx - 20 * scale, cy - 4 * scale, cx - 8 * scale, cy + 14 * scale, 0xC99B72);
        // muzzle flash
        if (p.fireTimer > GameData.WEAPON_DELAY[w] - 0.04f) {
            int fr = 10 * scale + RND.nextInt(6 * scale);
            drawRing(cx, cy - 74 * scale, 2 * scale, fr, 0xFFDD55);
            fillRect(cx - fr / 2, cy - 74 * scale - fr / 2, cx + fr / 2, cy - 74 * scale + fr / 2, 0xFFF0A0);
        }
    }

    // ---- Kill effects (full-screen overlays) ------------------------------
    private void drawKillEffect(int effect, float t, float time) {
        float k = 1f - t; // 0..1 progress
        switch (effect % GameData.EFFECT_COUNT) {
            case 0: // black hole
                int cx = W / 2, cy = H / 2;
                int rMax = (int)(Math.max(W, H) * 0.55f);
                int r = (int)(rMax * k);
                drawRing(cx, cy, 0, Math.max(2, r), 0x000000);
                drawRing(cx, cy, Math.max(2, r - 3), Math.max(4, r + 6), 0x4B2E83);
                break;
            case 1: // explosion
                int er = (int)(Math.max(W, H) * 0.5f * k);
                drawRing(cx, cy, Math.max(0, er - 30), er, 0xFF8525);
                drawRing(cx, cy, Math.max(0, er - 12), Math.max(1, er), 0xFFE17A);
                break;
            case 2: // blood splatter
                for (int i = 0; i < 26; i++) {
                    double a = i * 2.399;
                    int rr = (int)(k * W * (0.2f + (i % 5) * 0.08f));
                    int x = (int)(cx + Math.cos(a) * rr), y = (int)(cy + Math.sin(a) * rr);
                    fillRect(x - 3, y - 3, x + 3, y + 3, 0xB31212);
                }
                break;
            case 3: // lightning
                int lx = cx;
                for (int y = 0; y < H; y += 12) {
                    int nx = lx + RND.nextInt(40) - 20;
                    fillRect(Math.min(lx, nx), y, Math.max(lx, nx) + 3, y + 4, 0xBBEAFF);
                    lx = nx;
                }
                break;
            case 4: // firestorm
                for (int i = 0; i < 40; i++) {
                    int x = RND.nextInt(W);
                    int y = H - (int)((k * H + RND.nextInt(H / 2)) % H);
                    int s = 2 + RND.nextInt(5);
                    fillRect(x, y, x + s, y + s * 2, RND.nextBoolean() ? 0xFF6A00 : 0xFFC300);
                }
                break;
            case 5: // ice shards
                for (int i = 0; i < 20; i++) {
                    double a = i * 0.31 + time;
                    int rr = (int)(k * W * 0.5f * ((i % 4) + 1) / 4f);
                    int x = (int)(cx + Math.cos(a) * rr), y = (int)(cy + Math.sin(a) * rr);
                    fillRect(x - 4, y - 8, x + 4, y + 8, 0x9FE8FF);
                }
                break;
            case 6: // neon burst
                drawRing(cx, cy, Math.max(0, (int)(k * W * 0.3f) - 8), (int)(k * W * 0.3f), 0xFF30D0);
                drawRing(cx, cy, Math.max(0, (int)(k * W * 0.2f) - 4), (int)(k * W * 0.2f), 0x30FFD0);
                break;
            case 7: // void implosion
                drawRing(cx, cy, (int)(k * W * 0.5f), (int)(k * W * 0.5f) + 4, 0x8A2BE2);
                drawRing(cx, cy, (int)(k * W * 0.35f), (int)(k * W * 0.35f) + 3, 0x000000);
                break;
        }
    }
}
