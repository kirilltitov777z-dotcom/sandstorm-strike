package com.sandstorm.strike;

import java.util.Random;

/** Player, bots, bomb, pickups, particles. */
public final class Entities {

    public static final int TEAM_T = 0, TEAM_CT = 1;

    public static class Player {
        public float x, y, dir, pitch;
        public float health = 100, armor = 0;
        public int team;
        public int weapon = 2;
        public int[] ammo = new int[5];
        public int[] reserve = new int[5];
        public int kills, deaths;
        public boolean alive = true;
        public boolean isBot;
        public float fireTimer, reloadTimer;
        public float elevation, targetElevation;
        public float bob;
        public float bobPhase;
        public int skin;
        public String name = "Player";
        public float speedBoost = 1f;
        public boolean crouching;
        // bot AI
        public int aiState; // 0 patrol 1 chase 2 attack
        public float aiThink, aiStrafe;
        public float patrolX, patrolY;
        public Player target;
        public float aimJitter;

        public Player(float x, float y, int team, boolean bot) {
            this.x = x; this.y = y; this.team = team; this.isBot = bot;
            this.dir = team == TEAM_T ? 0f : (float)Math.PI;
            for (int i = 0; i < 5; i++) {
                ammo[i] = GameData.WEAPON_MAG[i];
                reserve[i] = GameData.WEAPON_RESERVE[i];
            }
            if (bot) {
                this.aimJitter = 0.06f + new Random().nextFloat() * 0.12f;
                this.name = team == TEAM_T ? "T-Bot" : "CT-Bot";
            }
        }

        public void resetForRound(float sx, float sy, float angle) {
            x = sx; y = sy; dir = angle; pitch = 0;
            health = 100; armor = 0; alive = true;
            fireTimer = 0; reloadTimer = 0;
            elevation = 0; targetElevation = 0;
            for (int i = 0; i < 5; i++) { ammo[i] = GameData.WEAPON_MAG[i]; reserve[i] = GameData.WEAPON_RESERVE[i]; }
            aiState = 0; target = null;
        }
    }

    public static class Bomb {
        public float x, y;
        public int state; // 0 carried 1 planted 2 exploded 3 defused
        public float timer;
        public Player carrier;
        public int site; // 0 A 1 B
        public float defuseProgress;
        public Bomb() {}
        public Bomb(float x, float y, int state) { this.x=x; this.y=y; this.state=state; }
    }

    public static class Pickup {
        public float x, y;
        public int type; // 0 hp 1 armor 2 ammo 3..7 weapon index+3
        public boolean active = true;
        public float respawn;
    }

    public static class Particle {
        public float x, y, z, vx, vy, vz, life, maxLife;
        public int color;
        public float size;
        public int kind; // 0 particle 1 ring 2 glyph
        public float radius, maxRadius;
    }

    public static class Corpse {
        public float x, y, dir;
        public float life = 14f;
        public Corpse() {}
        public Corpse(float x, float y, float dir) { this.x=x; this.y=y; this.dir=dir; }
    }
}
