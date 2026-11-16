package com.sandstorm.strike;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    // screens
    static final int S_MENU=0, S_PLAY=1, S_SETTINGS=2, S_ELO=3, S_CLAN=4, S_SKINS=5, S_GAME=6, S_END=7, S_LAN=8, S_HELP=9;
    static final int MODE_OFFLINE=0, MODE_ONLINE=1, MODE_LAN=2;

    SurfaceHolder holder;
    Thread thread;
    volatile boolean running = true;
    Renderer renderer;
    World world;
    SharedPreferences prefs;
    final Random RND = new Random();

    // ---- state (package-visible for UI) ----
    int screen = S_MENU, mode = MODE_OFFLINE, difficulty = 1;
    int viewW = 960, viewH = 540;

    float sensitivity = 1.0f;
    int crosshairStyle = 0, crosshairColor = 0x00FF66, crosshairSize = 9;
    boolean bhopEnabled = true; float bhopMult = 1.5f;
    boolean invertY = false; int quality = 1, effect = 0;
    boolean showFps = false; float masterVolume = 1f;

    int elo = 1000, pendingElo = 0, wins = 0, losses = 0, totalKills = 0;
    String clanName = "";
    int[] skinPerWeapon = new int[5];

    Entities.Player player;
    final List<Entities.Player> bots = new ArrayList<>();
    Entities.Bomb bomb;
    final List<Entities.Pickup> pickups = new ArrayList<>();
    final List<Entities.Particle> particles = new ArrayList<>();
    final List<Entities.Corpse> corpses = new ArrayList<>();

    int roundNo = 1, scoreT = 0, scoreCT = 0, roundState = 0; // 0 normal 1 planted 2 ended
    float roundTimer = 105, plantTimer = 40, bombTimer = 40;
    String roundMessage = "", endMessage = "";
    float roundMessageT = 0, endTimer = 0;
    int roundWinner = -1;
    float matchTime = 0;

    int killEffect = 0; float killEffectT = 0;
    final List<String> killFeed = new ArrayList<>();
    final List<Btn> buttons = new ArrayList<>();

    // input
    float joyX = 0, joyY = 0;
    boolean firing = false, wantJump = false, wantCrouch = false, wantReload = false, wantPlant = false;
    int lookPointer = -1, movePointer = -1;
    float lookLastX, lookLastY, moveStartX, moveStartY;
    final Set<Integer> keys = new HashSet<>();

    static class Btn {
        final float x, y, w, h; final int action; final String label;
        Btn(float x,float y,float w,float h,int action,String label){
            this.x=x;this.y=y;this.w=w;this.h=h;this.action=action;this.label=label;
        }
        boolean contains(float px,float py){ return px>=x&&px<=x+w&&py>=y&&py<=y+h; }
    }

    final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    Bitmap skyPhoto = null;
    long lastFrame = 0;
    float fps = 60;

    public GameView(Context c, AttributeSet a) {
        super(c, a);
        holder = getHolder(); holder.addCallback(this);
        prefs = c.getSharedPreferences(GameData.PREFS, Context.MODE_PRIVATE);
        renderer = new Renderer(320, 180);
        loadPrefs();
        world = new World();
        setFocusable(true);
        setOnTouchListener((v, e) -> handleTouch(e));
    }

    void loadPrefs() {
        elo = prefs.getInt(GameData.K_ELO, 1000);
        pendingElo = prefs.getInt(GameData.K_PENDING_ELO, 0);
        wins = prefs.getInt(GameData.K_WINS, 0);
        losses = prefs.getInt(GameData.K_LOSSES, 0);
        totalKills = prefs.getInt(GameData.K_KILLS, 0);
        clanName = prefs.getString(GameData.K_CLAN, "");
        String s = prefs.getString(GameData.K_SETTINGS, "");
        if (s != null && !s.isEmpty()) {
            String[] p = s.split(";");
            try {
                sensitivity = Float.parseFloat(p[0]); crosshairStyle = Integer.parseInt(p[1]);
                crosshairColor = Integer.parseInt(p[2]); crosshairSize = Integer.parseInt(p[3]);
                bhopEnabled = Boolean.parseBoolean(p[4]); bhopMult = Float.parseFloat(p[5]);
                invertY = Boolean.parseBoolean(p[6]); quality = Integer.parseInt(p[7]);
                effect = Integer.parseInt(p[8]); showFps = Boolean.parseBoolean(p[9]);
                masterVolume = Float.parseFloat(p[10]);
            } catch (Exception ignored) {}
        }
        for (int i = 0; i < 5; i++) skinPerWeapon[i] = prefs.getInt(GameData.K_SKIN + i, 0);
    }

    void savePrefs() {
        SharedPreferences.Editor e = prefs.edit();
        e.putInt(GameData.K_ELO, elo); e.putInt(GameData.K_PENDING_ELO, pendingElo);
        e.putInt(GameData.K_WINS, wins); e.putInt(GameData.K_LOSSES, losses);
        e.putInt(GameData.K_KILLS, totalKills); e.putString(GameData.K_CLAN, clanName);
        String s = sensitivity + ";" + crosshairStyle + ";" + crosshairColor + ";" + crosshairSize + ";"
                + bhopEnabled + ";" + bhopMult + ";" + invertY + ";" + quality + ";"
                + effect + ";" + showFps + ";" + masterVolume;
        e.putString(GameData.K_SETTINGS, s);
        for (int i = 0; i < 5; i++) e.putInt(GameData.K_SKIN + i, skinPerWeapon[i]);
        e.apply();
    }

    public void setSkyPhoto(Bitmap b) {
        skyPhoto = b;
        if (b != null) {
            Bitmap sq = Bitmap.createScaledBitmap(b, GameData.TEX, GameData.TEX, true);
            int[] px = new int[GameData.TEX * GameData.TEX];
            sq.getPixels(px, 0, GameData.TEX, 0, 0, GameData.TEX, GameData.TEX);
            renderer.setSkyTexture(px);
        }
    }

    // ---- surface ----
    @Override public void surfaceCreated(SurfaceHolder h) {
        running = true;
        thread = new Thread(this::loop);
        thread.start();
    }
    @Override public void surfaceChanged(SurfaceHolder h, int f, int w, int ht) {
        viewW = w; viewH = ht; applyQuality();
    }
    @Override public void surfaceDestroyed(SurfaceHolder h) {
        running = false;
        try { thread.join(1000); } catch (Exception ignored) {}
    }

    void applyQuality() {
        int[][] res = {{240,135},{360,203},{480,270}};
        int q = Math.max(0, Math.min(2, quality));
        renderer.resize(res[q][0], res[q][1]);
    }

    void loop() {
        while (running) {
            long now = System.nanoTime();
            float dt = lastFrame == 0 ? 0.016f : (now - lastFrame) / 1e9f;
            lastFrame = now;
            if (dt > 0.1f) dt = 0.1f;
            fps = fps * 0.92f + (1f / Math.max(0.001f, dt)) * 0.08f;
            try {
                update(dt);
                draw();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // ================= GAME LOGIC =================
    void update(float dt) {
        matchTime += dt;
        if (killEffectT > 0) killEffectT -= dt * 1.6f;
        if (roundMessageT > 0) roundMessageT -= dt;
        if (endTimer > 0) { endTimer -= dt; if (endTimer <= 0) screen = S_MENU; }

        if (screen != S_GAME) return;
        if (roundState == 2) return;

        updatePlayer(dt);
        for (Entities.Player b : bots) updateBot(b, dt);
        updateBomb(dt);
        updateParticles(dt);
        updatePickups(dt);

        roundTimer -= dt;
        if (roundTimer <= 0 && roundState == 0) {
            if (bomb.state == 1) { /* bomb keeps ticking */ }
            else endRound(Entities.TEAM_CT, "Время вышло");
        }
        checkWin();
    }

    void updatePlayer(float dt) {
        Entities.Player p = player;
        if (!p.alive) return;

        // rotation from look input already applied in onTouch; keyboard fallback
        float turn = 0, look = 0;
        if (keys.contains(KeyEvent.KEYCODE_DPAD_LEFT)) turn -= 1;
        if (keys.contains(KeyEvent.KEYCODE_DPAD_RIGHT)) turn += 1;
        if (keys.contains(KeyEvent.KEYCODE_DPAD_UP)) look -= 1;
        if (keys.contains(KeyEvent.KEYCODE_DPAD_DOWN)) look += 1;
        p.dir += turn * 2.2f * dt;
        p.pitch += look * 1.5f * dt * (invertY ? -1 : 1);
        p.pitch = Math.max(-0.45f, Math.min(0.45f, p.pitch));

        float mx = joyX, my = joyY;
        if (keys.contains(KeyEvent.KEYCODE_W)) my -= 1;
        if (keys.contains(KeyEvent.KEYCODE_S)) my += 1;
        if (keys.contains(KeyEvent.KEYCODE_A)) mx -= 1;
        if (keys.contains(KeyEvent.KEYCODE_D)) mx += 1;
        float len = (float)Math.hypot(mx, my);
        if (len > 1) { mx /= len; my /= len; }

        p.crouching = wantCrouch;
        float baseSpeed = p.crouching ? 1.6f : 3.0f;
        if (bhopEnabled) baseSpeed *= p.speedBoost;
        float dx = (float)(Math.cos(p.dir) * -my + Math.cos(p.dir + Math.PI/2) * mx) * baseSpeed * dt;
        float dy = (float)(Math.sin(p.dir) * -my + Math.sin(p.dir + Math.PI/2) * mx) * baseSpeed * dt;
        float[] out = new float[2];
        world.moveWithCollision(p.x, p.y, p.x + dx, p.y + dy, 0.24f, out);
        p.x = out[0]; p.y = out[1];

        // elevation from cell
        char c = world.cellAt((int)p.x, (int)p.y);
        if (c == '^') p.targetElevation = 0.4f;
        else if (c == '=') p.targetElevation = 0.2f;
        else if (c == '-') p.targetElevation = 0.1f;
        else p.targetElevation = 0f;
        p.elevation += (p.targetElevation - p.elevation) * Math.min(1, dt * 10);

        float moving = len * baseSpeed;
        p.bob += (moving - p.bob) * Math.min(1, dt * 6);
        p.bobPhase += dt * (4f + moving * 1.5f);

        // bunnyhop: speed boost decays
        if (wantJump && !p.crouching) {
            p.speedBoost = bhopEnabled ? bhopMult : 1f;
            wantJump = false;
        }
        p.speedBoost += (1f - p.speedBoost) * Math.min(1, dt * 1.3f);

        if (p.fireTimer > 0) p.fireTimer -= dt;
        if (p.reloadTimer > 0) {
            p.reloadTimer -= dt;
            if (p.reloadTimer <= 0) finishReload(p);
        }
        if (wantReload) { startReload(p); wantReload = false; }
        if (firing) fire(p);

        if (wantPlant) { tryBombAction(p); wantPlant = false; }
    }

    void startReload(Entities.Player p) {
        int w = p.weapon;
        if (p.reloadTimer > 0) return;
        if (p.ammo[w] >= GameData.WEAPON_MAG[w]) return;
        if (p.reserve[w] <= 0) return;
        p.reloadTimer = GameData.WEAPON_RELOAD[w];
    }
    void finishReload(Entities.Player p) {
        int w = p.weapon;
        int need = GameData.WEAPON_MAG[w] - p.ammo[w];
        int take = Math.min(need, p.reserve[w]);
        p.ammo[w] += take; p.reserve[w] -= take;
    }

    void fire(Entities.Player shooter) {
        int w = shooter.weapon;
        if (shooter.fireTimer > 0 || shooter.reloadTimer > 0) return;
        if (shooter.ammo[w] <= 0) { startReload(shooter); return; }
        shooter.ammo[w]--;
        shooter.fireTimer = GameData.WEAPON_DELAY[w];
        int pellets = GameData.WEAPON_PELLETS[w];
        float range = GameData.WEAPON_RANGE[w];
        float spread = GameData.WEAPON_SPREAD[w];
        if (shooter.bob > 2) spread *= 1.5f;
        if (shooter.crouching) spread *= 0.7f;
        if (shooter.isBot) spread += shooter.aimJitter;

        for (int i = 0; i < pellets; i++) {
            float a = shooter.dir + (RND.nextFloat() - 0.5f) * spread * 2f;
            float rdx = (float)Math.cos(a), rdy = (float)Math.sin(a);
            World.Hit wh = world.castRay(shooter.x, shooter.y, rdx, rdy, new World.Hit());
            float wallDist = wh.dist;
            Entities.Player best = null; float bestT = wallDist;
            List<Entities.Player> all = new ArrayList<>(); all.add(player); all.addAll(bots);
            for (Entities.Player t : all) {
                if (t == shooter || !t.alive || t.team == shooter.team) continue;
                float dx = t.x - shooter.x, dy = t.y - shooter.y;
                float proj = dx * rdx + dy * rdy;
                if (proj <= 0 || proj > bestT) continue;
                float perp = Math.abs(dx * rdy - dy * rdx);
                float radius = t.crouching ? 0.26f : 0.36f;
                if (perp < radius) { best = t; bestT = proj; }
            }
            if (best != null) {
                damage(best, GameData.WEAPON_DAMAGE[w], shooter);
            }
        }
        // tracer particles from muzzle
        for (int i = 0; i < 3; i++) {
            addParticle(shooter.x + (float)Math.cos(shooter.dir) * 0.4f,
                        shooter.y + (float)Math.sin(shooter.dir) * 0.4f, 0.5f,
                        0xFFDD66, 0.04f, 0);
        }
    }

    void damage(Entities.Player target, int dmg, Entities.Player attacker) {
        if (!target.alive) return;
        int d = dmg;
        if (target.armor > 0) {
            int absorb = (int)Math.min(target.armor, d * 0.5f);
            target.armor -= absorb; d -= absorb;
        }
        target.health -= d;
        if (target.health <= 0) {
            target.health = 0; target.alive = false; target.deaths++;
            if (!target.isBot) { roundMessage = "Ты погиб"; roundMessageT = 2; }
            corpses.add(new Entities.Corpse(target.x, target.y, target.dir));
            if (attacker != null && !attacker.isBot) {
                attacker.kills++; totalKills++;
                triggerKillEffect();
                killFeed.add(0, attacker.name + " ➜ " + target.name);
                if (killFeed.size() > 5) killFeed.remove(killFeed.size() - 1);
            } else if (attacker != null) {
                killFeed.add(0, attacker.name + " ➜ " + target.name);
                if (killFeed.size() > 5) killFeed.remove(killFeed.size() - 1);
            }
            if (target == player) {
                // spectate: stay dead until round ends
            }
        }
    }

    void triggerKillEffect() {
        killEffect = effect >= 0 && effect < GameData.EFFECT_COUNT ? effect : RND.nextInt(GameData.EFFECT_COUNT);
        killEffectT = 1f;
    }

    void addParticle(float x, float y, float z, int color, float size, int kind) {
        Entities.Particle p = new Entities.Particle();
        p.x=x; p.y=y; p.z=z; p.color=color; p.size=size; p.kind=kind;
        p.life = 0.6f; p.maxLife = 0.6f;
        p.vx = (RND.nextFloat()-0.5f); p.vy=(RND.nextFloat()-0.5f); p.vz=RND.nextFloat();
        particles.add(p);
    }

    void burst(Entities.Player victim) {
        for (int i = 0; i < 30; i++) {
            Entities.Particle p = new Entities.Particle();
            p.x=victim.x; p.y=victim.y; p.z=0.3f+RND.nextFloat()*0.8f;
            p.color = RND.nextBoolean()?0xC0392B:0x8B0000;
            p.size=0.05f+RND.nextFloat()*0.08f; p.kind=0;
            p.vx=(RND.nextFloat()-0.5f)*2; p.vy=(RND.nextFloat()-0.5f)*2; p.vz=RND.nextFloat()*2;
            p.maxLife=0.8f+RND.nextFloat()*0.6f; p.life=p.maxLife;
            particles.add(p);
        }
    }

    void updateParticles(float dt) {
        for (int i = particles.size()-1; i>=0; i--) {
            Entities.Particle p = particles.get(i);
            p.life -= dt;
            if (p.life <= 0) { particles.remove(i); continue; }
            p.x += p.vx*dt; p.y += p.vy*dt; p.z += p.vz*dt;
            p.vz -= 3f*dt;
            if (p.z < 0) { p.z=0; p.vz*=-0.3f; }
        }
    }

    void updatePickups(float dt) {
        for (Entities.Pickup pk : pickups) {
            if (!pk.active) {
                pk.respawn -= dt;
                if (pk.respawn <= 0) pk.active = true;
                continue;
            }
            float d = (float)Math.hypot(pk.x-player.x, pk.y-player.y);
            if (d < 0.45f && player.alive) {
                if (pk.type==0) player.health=Math.min(100,player.health+35);
                else if (pk.type==1) player.armor=Math.min(100,player.armor+50);
                else if (pk.type==2) for(int i=0;i<5;i++) player.reserve[i]+=20;
                else {
                    int w=pk.type-3;
                    if (w>=0&&w<5) { player.weapon=w; player.ammo[w]=GameData.WEAPON_MAG[w]; }
                }
                pk.active=false; pk.respawn=18f;
            }
        }
    }

    // ---- bots ----
    void updateBot(Entities.Player b, float dt) {
        if (!b.alive) return;
        b.aiThink -= dt;
        if (b.fireTimer > 0) b.fireTimer -= dt;
        if (b.reloadTimer > 0) { b.reloadTimer -= dt; if (b.reloadTimer<=0) finishReload(b); }

        if (b.aiThink <= 0) {
            b.aiThink = 0.25f + RND.nextFloat()*0.2f;
            List<Entities.Player> all = new ArrayList<>(); all.add(player); all.addAll(bots);
            Entities.Player nearest=null; float nd=1e9f;
            for (Entities.Player e : all) {
                if (e==b || !e.alive || e.team==b.team) continue;
                float d=(float)Math.hypot(e.x-b.x,e.y-b.y);
                if (d<nd && !world.lineBlocked(b.x,b.y,e.x,e.y)) { nd=d; nearest=e; }
            }
            if (nearest!=null) { b.target=nearest; b.aiState=2; }
            else if (b.target!=null && b.target.alive) b.aiState=1;
            else {
                b.aiState=0;
                World.Site s = pickSite(b.team);
                b.patrolX=s.x; b.patrolY=s.y;
            }
            b.aiStrafe = RND.nextBoolean()?1:-1;
        }

        float tx=b.x, ty=b.y;
        boolean attack=false;
        if (b.aiState==2 && b.target!=null && b.target.alive) {
            if (!world.lineBlocked(b.x,b.y,b.target.x,b.target.y)) {
                float want=(float)Math.atan2(b.target.y-b.y,b.target.x-b.x);
                b.dir = angleLerp(b.dir,want,Math.min(1,dt*(difficulty==2?9:difficulty==1?6:3.5f)));
                tx=b.target.x; ty=b.target.y;
                float d=(float)Math.hypot(b.target.x-b.x,b.target.y-b.y);
                attack = d<12 && Math.abs(angleDiff(b.dir,want))<0.25f;
            } else { b.aiState=1; }
        } else if (b.aiState==1 && b.target!=null) {
            tx=b.target.x; ty=b.target.y;
            float want=(float)Math.atan2(ty-b.y,tx-b.x);
            b.dir=angleLerp(b.dir,want,Math.min(1,dt*4));
        } else {
            tx=b.patrolX; ty=b.patrolY;
            float want=(float)Math.atan2(ty-b.y,tx-b.x);
            b.dir=angleLerp(b.dir,want,Math.min(1,dt*2.5f));
        }

        // movement with simple obstacle avoidance
        float dx=tx-b.x, dy=ty-b.y;
        float dist=(float)Math.hypot(dx,dy);
        float spd=(difficulty==2?2.7f:difficulty==1?2.3f:1.8f);
        if (attack && dist<5) { spd*=0.5f; }
        float nx=0,ny=0;
        if (dist>0.6f) { nx=dx/dist; ny=dy/dist; }
        // strafe + avoid walls
        float sx=-ny*b.aiStrafe, sy=nx*b.aiStrafe;
        float mvx=(nx*0.85f+sx*0.35f)*spd*dt;
        float mvy=(ny*0.85f+sy*0.35f)*spd*dt;
        float[] out=new float[2];
        world.moveWithCollision(b.x,b.y,b.x+mvx,b.y+mvy,0.24f,out);
        if (Math.abs(out[0]-b.x)<0.001f && Math.abs(out[1]-b.y)<0.001f) {
            b.aiStrafe*=-1;
            world.moveWithCollision(b.x,b.y,b.x+sx*spd*dt,b.y+sy*spd*dt,0.24f,out);
        }
        b.x=out[0]; b.y=out[1];

        if (attack) {
            if (b.ammo[b.weapon]<=0) startReload(b);
            else fire(b);
        }
        // elevation
        char c=world.cellAt((int)b.x,(int)b.y);
        b.targetElevation = (c=='^')?0.4f:(c=='=')?0.2f:0f;
        b.elevation += (b.targetElevation-b.elevation)*Math.min(1,dt*8);
    }

    World.Site pickSite(int team) {
        if (!world.siteA.isEmpty() && !world.siteB.isEmpty())
            return RND.nextBoolean()?world.siteA.get(RND.nextInt(world.siteA.size())):world.siteB.get(RND.nextInt(world.siteB.size()));
        return new World.Site(12,12);
    }

    static float angleDiff(float a,float b){ float d=(a-b+3.14159f)%(6.28318f); if(d<0)d+=6.28318f; return d-3.14159f; }
    static float angleLerp(float a,float b,float t){ return a+angleDiff(b,a)*t; }

    // ---- bomb / rounds ----
    void tryBombAction(Entities.Player p) {
        if (bomb==null) return;
        if (p.team==Entities.TEAM_T && bomb.state==0 && nearSite(p.x,p.y)) {
            bomb.state=1; bomb.x=p.x; bomb.y=p.y; bomb.site=nearSiteIndex(p.x,p.y);
            bomb.timer=40; roundState=1; roundMessage="Бомба заложена!"; roundMessageT=2;
            plantTimer=40;
        } else if (p.team==Entities.TEAM_CT && bomb.state==1) {
            float d=(float)Math.hypot(bomb.x-p.x,bomb.y-p.y);
            if (d<1f) { /* defuse progress handled by holding */ }
        }
    }

    boolean nearSite(float x,float y){
        for(World.Site s:world.siteA) if(Math.hypot(s.x-x,s.y-y)<2.2f) return true;
        for(World.Site s:world.siteB) if(Math.hypot(s.x-x,s.y-y)<2.2f) return true;
        return false;
    }
    int nearSiteIndex(float x,float y){
        for(World.Site s:world.siteA) if(Math.hypot(s.x-x,s.y-y)<2.2f) return 0;
        return 1;
    }

    void updateBomb(float dt) {
        if (bomb==null) return;
        // CT defusing
        if (bomb.state==1 && player.alive && player.team==Entities.TEAM_CT) {
            float d=(float)Math.hypot(bomb.x-player.x,bomb.y-player.y);
            if (d<1f && wantCrouch) {
                bomb.defuseProgress+=dt;
                if (bomb.defuseProgress>=5f) { bomb.state=3; endRound(Entities.TEAM_CT,"Бомба обезврежена"); }
            } else bomb.defuseProgress=Math.max(0,bomb.defuseProgress-dt*2);
        }
        if (bomb.state==1) {
            bomb.timer-=dt;
            if (bomb.timer<=0) {
                bomb.state=2;
                for(int i=0;i<120;i++) addParticle(bomb.x,bomb.y,RND.nextFloat()*2,
                        RND.nextBoolean()?0xFF6A00:0xFFD300,0.1f,0);
                endRound(Entities.TEAM_T,"БОМБА ВЗОРВАНА");
            }
        }
    }

    void checkWin() {
        if (roundState==2) return;
        int tAlive=0,ctAlive=0;
        if (player.alive) { if(player.team==Entities.TEAM_T)tAlive++; else ctAlive++; }
        for(Entities.Player b:bots) if(b.alive){ if(b.team==Entities.TEAM_T)tAlive++; else ctAlive++; }
        if (bomb.state==1) {
            if (ctAlive==0) endRound(Entities.TEAM_T,"Все КТ убиты");
        } else {
            if (tAlive==0) endRound(Entities.TEAM_CT,"Все Т убиты");
        }
    }

    void endRound(int winner, String msg) {
        if (roundState==2) return;
        roundState=2; roundWinner=winner; endMessage=msg; endTimer=3.5f;
        if (winner==player.team) { scoreT++; applyElo(true); }
        else { scoreCT++; applyElo(false); }
        screen=S_END;
    }

    void applyElo(boolean won) {
        int base = won?25:-20;
        int perf = Math.max(-5,Math.min(10,player.kills*2));
        int delta = won?base+perf:base;
        if (mode==MODE_ONLINE) { elo=Math.max(0,elo+delta); }
        else pendingElo += delta; // offline accumulates
        if (won) wins++; else losses++;
        savePrefs();
    }

    void syncElo() {
        if (pendingElo!=0) { elo=Math.max(0,elo+pendingElo); pendingElo=0; savePrefs(); }
    }

    void nextRound() {
        roundNo++;
        roundState=0; roundTimer=105; bombTimer=40; plantTimer=40;
        roundMessage=""; roundMessageT=0;
        particles.clear(); corpses.clear(); killFeed.clear();
        bomb.state=0; bomb.carrier=player; bomb.defuseProgress=0;
        player.resetForRound(world.tSpawns.get(0).x,world.tSpawns.get(0).y,(float)Math.PI/4);
        player.team=Entities.TEAM_T;
        int ti=0,ci=0;
        for(Entities.Player b:bots){
            if(b.team==Entities.TEAM_T){
                World.Spawn s=world.tSpawns.get((ti++)%world.tSpawns.size());
                b.resetForRound(s.x,s.y,(float)Math.PI/4);
            } else {
                World.Spawn s=world.cSpawns.get((ci++)%world.cSpawns.size());
                b.resetForRound(s.x,s.y,(float)Math.PI);
            }
        }
        screen=S_GAME;
    }

    void startMatch(int m) {
        mode=m;
        if (m==MODE_ONLINE) syncElo();
        scoreT=0; scoreCT=0; roundNo=1;
        player=new Entities.Player(3.5f,18.5f,Entities.TEAM_T,false);
        player.name="Ты"; player.skin=skinPerWeapon[player.weapon];
        bots.clear();
        for(int i=0;i<4;i++) bots.add(new Entities.Player(3.5f+i*0.7f,18.5f,Entities.TEAM_T,true));
        for(int i=0;i<5;i++) bots.add(new Entities.Player(19.5f-i*0.7f,18.5f,Entities.TEAM_CT,true));
        bomb=new Entities.Bomb(0,0,0);
        bomb.carrier=player;
        pickups.clear();
        // seed pickups
        float[][] pp={{9.5f,17.5f,0},{14.5f,17.5f,1},{11.5f,17.5f,2},
                      {4.5f,20.5f,3},{6.5f,20.5f,4},{8.5f,20.5f,5},
                      {17.5f,20.5f,6},{19.5f,11.5f,0},{4.5f,11.5f,1}};
        for(float[] a:pp){ Entities.Pickup pk=new Entities.Pickup(); pk.x=a[0];pk.y=a[1];pk.type=(int)a[2];pickups.add(pk);}
        nextRound(); screen=S_GAME;
    }

    // ================= DRAW =================
    void draw() {
        SurfaceHolder h=holder;
        Canvas c=null;
        try { c=h.lockCanvas(); } catch(Exception e){ return; }
        if (c==null) return;
        try {
            if (screen==S_GAME || screen==S_END) {
                renderer.render(player,world,bots,bomb,pickups,particles,corpses,matchTime,killEffect,killEffectT,quality);
                c.drawBitmap(renderer.bitmap,null,new android.graphics.Rect(0,0,viewW,viewH),paint);
                UI.drawHUD(this,c,viewW,viewH);
            } else {
                UI.drawMenu(this,c,viewW,viewH);
            }
        } finally { h.unlockCanvasAndPost(c); }
    }

    // ================= INPUT =================
    boolean handleTouch(MotionEvent e) {
        int action=e.getActionMasked();
        int idx=e.getActionIndex();
        float x=e.getX(idx), y=e.getY(idx);
        if (screen!=S_GAME) {
            if (action==MotionEvent.ACTION_DOWN || action==MotionEvent.ACTION_POINTER_DOWN) {
                UI.handleClick(this,x,y,viewW,viewH);
            }
            return true;
        }
        // in game
        if (action==MotionEvent.ACTION_DOWN) {
            // check HUD buttons first
            if (UI.handleGameButton(this,x,y,viewW,viewH)) return true;
            if (x < viewW*0.45f) { movePointer=e.getPointerId(idx); moveStartX=x;moveStartY=y;joyX=0;joyY=0; }
            else { lookPointer=e.getPointerId(idx); lookLastX=x;lookLastY=y; }
        } else if (action==MotionEvent.ACTION_POINTER_DOWN) {
            if (x < viewW*0.45f && movePointer==-1) { movePointer=e.getPointerId(idx);moveStartX=x;moveStartY=y; }
            else { lookPointer=e.getPointerId(idx);lookLastX=x;lookLastY=y; }
        } else if (action==MotionEvent.ACTION_MOVE) {
            for(int i=0;i<e.getPointerCount();i++){
                int id=e.getPointerId(i);
                float px=e.getX(i),py=e.getY(i);
                if(id==movePointer){
                    joyX=(px-moveStartX)/(viewW*0.09f);
                    joyY=(py-moveStartY)/(viewW*0.09f);
                    float l=(float)Math.hypot(joyX,joyY); if(l>1){joyX/=l;joyY/=l;}
                } else if(id==lookPointer){
                    float ddx=px-lookLastX, ddy=py-lookLastY;
                    player.dir += ddx*0.004f*sensitivity;
                    player.pitch += ddy*0.003f*sensitivity*(invertY?-1:1);
                    player.pitch=Math.max(-0.45f,Math.min(0.45f,player.pitch));
                    lookLastX=px;lookLastY=py;
                }
            }
        } else if (action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_POINTER_UP) {
            int id=e.getPointerId(idx);
            if(id==movePointer){movePointer=-1;joyX=0;joyY=0;}
            if(id==lookPointer)lookPointer=-1;
            firing=false;
        }
        return true;
    }

    @Override public boolean onKeyDown(int code, KeyEvent e) {
        keys.add(code);
        if (code==KeyEvent.KEYCODE_SPACE) wantJump=true;
        if (code==KeyEvent.KEYCODE_CTRL_LEFT || code==KeyEvent.KEYCODE_SHIFT_LEFT) wantCrouch=true;
        if (code==KeyEvent.KEYCODE_R) wantReload=true;
        if (code==KeyEvent.KEYCODE_E) wantPlant=true;
        return true;
    }
    @Override public boolean onKeyUp(int code, KeyEvent e) {
        keys.remove(code);
        if (code==KeyEvent.KEYCODE_CTRL_LEFT || code==KeyEvent.KEYCODE_SHIFT_LEFT) wantCrouch=false;
        return true;
    }
}
