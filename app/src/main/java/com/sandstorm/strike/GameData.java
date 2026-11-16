package com.sandstorm.strike;

import android.graphics.Bitmap;
import android.graphics.Color;
import java.util.Random;

/** Static game data: map, weapons, procedural textures, settings/save keys. */
public final class GameData {

    // ---- Map legend -------------------------------------------------------
    // # sandstone   b brick   w wood   m metal   r railing(low)
    // . floor      ^ balcony floor   = ramp up   - ramp down
    // A/B bomb sites   T/C spawns   + health   x armor   1-5 weapon pickups
    public static final String[] MAP = {
        "########################",
        "#..........#...........#",
        "#..AAAA....#...BBBB....#",
        "#..AAAA....#...BBBB....#",
        "#..........#...........#",
        "#..rrrr....#...rrrr....#",
        "#..r^^r....#...r^^r....#",
        "#..r^^r....#...r^^r....#",
        "#..==rr....#...rr==....#",
        "#..........#...........#",
        "#####..#####.#####..####",
        "#......................#",
        "#...####....####....#..#",
        "#...#..#....#..#....#..#",
        "#...####....####....#..#",
        "#......................#",
        "####..############..####",
        "#........+.....x.......#",
        "#..T................C..#",
        "#......................#",
        "#....1....2..3....4....#",
        "#......................#",
        "#......................#",
        "########################"
    };

    public static final int MAP_W = 24;
    public static final int MAP_H = 24;
    public static final int TEX = 64;

    // ---- Weapons ----------------------------------------------------------
    public static final String[] WEAPON_NAMES = {"Pistol", "SMG", "Rifle", "Shotgun", "Sniper"};
    public static final int[] WEAPON_DAMAGE = {24, 17, 31, 13, 95};
    public static final float[] WEAPON_DELAY = {0.26f, 0.085f, 0.13f, 0.72f, 1.05f};
    public static final int[] WEAPON_MAG = {12, 30, 30, 6, 5};
    public static final int[] WEAPON_RESERVE = {48, 90, 90, 24, 20};
    public static final float[] WEAPON_SPREAD = {0.008f, 0.035f, 0.016f, 0.10f, 0.0015f};
    public static final float[] WEAPON_RANGE = {28f, 24f, 42f, 13f, 70f};
    public static final int[] WEAPON_PELLETS = {1, 1, 1, 8, 1};
    public static final float[] WEAPON_RELOAD = {1.1f, 1.8f, 2.0f, 2.4f, 2.2f};

    // ---- Skins (color per weapon variant) ---------------------------------
    public static final int[][] SKIN_COLORS = {
        {0xC9A227, 0xB0B7C0, 0x3A6EA5, 0x8E44AD, 0xC0392B, 0x16A085, 0xE67E22, 0xECF0F1}, // pistol
        {0x2E8B57, 0xD4AC0D, 0xC0392B, 0x2980B9, 0x7F8C8D, 0x8E44AD, 0x1ABC9C, 0xE74C3C}, // smg
        {0x8B4513, 0x2C3E50, 0x27AE60, 0x8E44AD, 0xD35400, 0x2980B9, 0xD4AC0D, 0xECF0F1}, // rifle
        {0x6B4226, 0x34495E, 0xD4AC0D, 0xC0392B, 0x1ABC9C, 0x8E44AD, 0xE67E22, 0xBDC3C7}, // shotgun
        {0x2C3E50, 0xD4AC0D, 0xC0392B, 0x2980B9, 0x27AE60, 0x8E44AD, 0xE67E22, 0xECF0F1}  // sniper
    };
    public static final String[] SKIN_NAMES = {"Sand", "Chrome", "Neon", "Royal", "Ember", "Toxic", "Sunset", "Frost"};
    public static final int SKINS_PER_WEAPON = 8;

    // ---- Kill effects -----------------------------------------------------
    public static final String[] EFFECT_NAMES = {
        "Black Hole", "Explosion", "Blood", "Lightning", "Firestorm", "Ice Shards", "Neon Burst", "Void Implosion"
    };
    public static final int EFFECT_COUNT = 8;

    // ---- Save keys --------------------------------------------------------
    public static final String PREFS = "sandstorm_prefs";
    public static final String K_ELO = "elo";
    public static final String K_PENDING_ELO = "pending_elo";
    public static final String K_WINS = "wins";
    public static final String K_LOSSES = "losses";
    public static final String K_KILLS = "kills";
    public static final String K_CLAN = "clan";
    public static final String K_SKIN = "skin_";
    public static final String K_SETTINGS = "settings";

    private static final Random RND = new Random();

    /** Generate a 64x64 procedural texture as an int ARGB array. */
    public static int[] makeTexture(int type) {
        int[] t = new int[TEX * TEX];
        for (int y = 0; y < TEX; y++) {
            for (int x = 0; x < TEX; x++) {
                int c;
                int n = RND.nextInt(18) - 9;
                switch (type) {
                    case 0: // sandstone
                        c = shade(0xC8A870, n);
                        if (y % 16 == 0 || (x + (y / 16 % 2) * 16) % 32 == 0) c = shade(0xA8844F, n);
                        break;
                    case 1: // brick
                        c = shade(0xA0503C, n);
                        if (y % 12 == 0 || (x + (y / 12 % 2) * 16) % 32 == 0) c = shade(0x6B3B2A, n);
                        break;
                    case 2: // wood
                        c = shade(0x7A5230, n);
                        if ((x + (int)(Math.sin(y * 0.3f) * 4)) % 14 == 0) c = shade(0x5C3D22, n);
                        break;
                    case 3: // metal
                        c = shade(0x7C8894, n);
                        if (x % 16 == 0 && y % 16 == 0) c = 0x4A5560;
                        if (y % 32 < 2) c = shade(0x64707C, n);
                        break;
                    case 4: // railing (bars)
                        c = ((x % 12 < 3) || (y % 20 < 3)) ? shade(0x3A4650, n) : 0x1A2228;
                        break;
                    case 5: // floor sand
                        c = shade(0xB9A073, n);
                        break;
                    case 6: // floor tile
                        c = shade(0x9AA0A4, n);
                        if (x % 16 < 2 || y % 16 < 2) c = shade(0x6E7478, n);
                        break;
                    case 7: // balcony tile
                        c = shade(0x8A8068, n);
                        if (x % 8 < 1 || y % 8 < 1) c = shade(0x6A6250, n);
                        break;
                    case 8: // sky (default desert)
                        float fy = y / (float)TEX;
                        c = skyColor(fy);
                        break;
                    default:
                        c = Color.MAGENTA;
                }
                t[y * TEX + x] = c;
            }
        }
        return t;
    }

    public static int skyColor(float fy) {
        int top = 0x2E6FA7, mid = 0x87B6D6, bot = 0xE8C99A;
        if (fy < 0.5f) return lerp(top, mid, fy * 2f);
        return lerp(mid, bot, (fy - 0.5f) * 2f);
    }

    public static int shade(int c, int n) {
        int r = Math.max(0, Math.min(255, Color.red(c) + n));
        int g = Math.max(0, Math.min(255, Color.green(c) + n));
        int b = Math.max(0, Math.min(255, Color.blue(c) + n));
        return Color.rgb(r, g, b);
    }

    public static int lerp(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int)(Color.red(a) + (Color.red(b) - Color.red(a)) * t);
        int g = (int)(Color.green(a) + (Color.green(b) - Color.green(a)) * t);
        int bl = (int)(Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t);
        return Color.rgb(r, g, bl);
    }

    /** Sample texture with nearest-neighbour wrap. */
    public static int texel(int[] tex, int tx, int ty) {
        tx &= (TEX - 1); ty &= (TEX - 1);
        return tex[ty * TEX + tx];
    }

    public static int randomEffect() { return RND.nextInt(EFFECT_COUNT); }
}
