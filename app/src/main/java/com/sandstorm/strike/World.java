package com.sandstorm.strike;

import java.util.ArrayList;
import java.util.List;

/** Map, collision and DDA raycasting. */
public final class World {

    public static final int WALL_NONE = 0;
    public static final int WALL_SAND = 1, WALL_BRICK = 2, WALL_WOOD = 3, WALL_METAL = 4, WALL_RAIL = 5;

    public final char[][] cells = new char[GameData.MAP_H][GameData.MAP_W];
    public final List<Spawn> tSpawns = new ArrayList<>();
    public final List<Spawn> cSpawns = new ArrayList<>();
    public final List<Site> siteA = new ArrayList<>();
    public final List<Site> siteB = new ArrayList<>();

    public World() {
        for (int y = 0; y < GameData.MAP_H; y++) {
            String row = GameData.MAP[y];
            for (int x = 0; x < GameData.MAP_W; x++) {
                char ch = x < row.length() ? row.charAt(x) : '#';
                cells[y][x] = ch;
                if (ch == 'T') tSpawns.add(new Spawn(x + 0.5f, y + 0.5f));
                if (ch == 'C') cSpawns.add(new Spawn(x + 0.5f, y + 0.5f));
                if (ch == 'A') siteA.add(new Site(x + 0.5f, y + 0.5f));
                if (ch == 'B') siteB.add(new Site(x + 0.5f, y + 0.5f));
            }
        }
    }

    public static class Spawn {
        public final float x, y;
        public Spawn(float x, float y) { this.x = x; this.y = y; }
    }

    public static class Site {
        public final float x, y;
        public Site(float x, float y) { this.x = x; this.y = y; }
    }

    public char cellAt(int mx, int my) {
        if (mx < 0 || my < 0 || mx >= GameData.MAP_W || my >= GameData.MAP_H) return '#';
        return cells[my][mx];
    }

    /** Returns wall type, or 0 if passable. */
    public int wallType(int mx, int my) {
        char c = cellAt(mx, my);
        switch (c) {
            case '#': return WALL_SAND;
            case 'b': return WALL_BRICK;
            case 'w': return WALL_WOOD;
            case 'm': return WALL_METAL;
            case 'r': return WALL_RAIL;
            default: return WALL_NONE;
        }
    }

    /** Railings are half-height walls; everything else full height. */
    public boolean isLowWall(int mx, int my) { return cellAt(mx, my) == 'r'; }

    public boolean isSolid(int mx, int my) { return wallType(mx, my) != WALL_NONE; }

    /** Circle-vs-grid collision: try move, slide on axes. */
    public void moveWithCollision(float px, float py, float nx, float ny, float radius, float[] out) {
        out[0] = px; out[1] = py;
        if (!blocked(nx, py, radius)) out[0] = nx;
        if (!blocked(out[0], ny, radius)) out[1] = ny;
    }

    private boolean blocked(float x, float y, float r) {
        int x0 = (int)(x - r), x1 = (int)(x + r);
        int y0 = (int)(y - r), y1 = (int)(y + r);
        for (int my = y0; my <= y1; my++) {
            for (int mx = x0; mx <= x1; mx++) {
                if (!isSolid(mx, my)) continue;
                // closest point on cell rect to circle center
                float cx = Math.max(mx, Math.min(x, mx + 1));
                float cy = Math.max(my, Math.min(y, my + 1));
                float dx = x - cx, dy = y - cy;
                if (dx * dx + dy * dy < r * r) return true;
            }
        }
        return false;
    }

    /** True if a ray from (x0,y0) to (x1,y1) hits a solid wall. */
    public boolean lineBlocked(float x0, float y0, float x1, float y1) {
        float dx = x1 - x0, dy = y1 - y0;
        float dist = (float)Math.sqrt(dx * dx + dy * dy);
        if (dist < 0.001f) return false;
        int steps = (int)(dist * 8);
        for (int i = 1; i < steps; i++) {
            float t = i / (float)steps;
            int mx = (int)(x0 + dx * t), my = (int)(y0 + dy * t);
            if (isSolid(mx, my)) return true;
        }
        return false;
    }

    /** DDA result. */
    public static class Hit {
        public float dist;
        public int wallType;
        public float wallX;   // 0..1 along wall for texture
        public int side;      // 0 = x-side, 1 = y-side
        public int mx, my;
    }

    /** Cast one ray using DDA. Reuses a Hit object passed in. */
    public Hit castRay(float px, float py, float rdx, float rdy, Hit hit) {
        int mx = (int)px, my = (int)py;
        float deltaX = rdx == 0 ? 1e30f : Math.abs(1f / rdx);
        float deltaY = rdy == 0 ? 1e30f : Math.abs(1f / rdy);
        int stepX, stepY;
        float sideX, sideY;
        if (rdx < 0) { stepX = -1; sideX = (px - mx) * deltaX; }
        else { stepX = 1; sideX = (mx + 1 - px) * deltaX; }
        if (rdy < 0) { stepY = -1; sideY = (py - my) * deltaY; }
        else { stepY = 1; sideY = (my + 1 - py) * deltaY; }

        int side = 0, type = 0;
        for (int i = 0; i < 128; i++) {
            if (sideX < sideY) { sideX += deltaX; mx += stepX; side = 0; }
            else { sideY += deltaY; my += stepY; side = 1; }
            type = wallType(mx, my);
            if (type != WALL_NONE) break;
        }

        float dist = side == 0 ? (sideX - deltaX) : (sideY - deltaY);
        if (dist < 0.0001f) dist = 0.0001f;
        float wallX = side == 0 ? py + dist * rdy : px + dist * rdx;
        wallX -= (int)wallX;

        hit.dist = dist;
        hit.wallType = type;
        hit.wallX = wallX;
        hit.side = side;
        hit.mx = mx; hit.my = my;
        return hit;
    }
}
