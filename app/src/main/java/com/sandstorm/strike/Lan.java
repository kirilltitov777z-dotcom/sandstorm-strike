package com.sandstorm.strike;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Простой LAN: host-authoritative deathmatch по TCP.
 * Хост симулирует всех, клиенты шлют ввод и рисуют полученное состояние.
 */
public class Lan {

    public static final int PORT = 27315;
    public static final int MAX_PLAYERS = 10;

    public volatile boolean host;
    public volatile boolean active;
    public volatile int myId = -1;
    public volatile String status = "не подключено";
    public volatile String myName = "Player";

    public final World world;
    public final Entities.Player local;
    public final List<Entities.Player> players = Collections.synchronizedList(new ArrayList<>());
    public final List<Entities.Particle> particles = Collections.synchronizedList(new ArrayList<>());
    public final List<Entities.Corpse> corpses = Collections.synchronizedList(new ArrayList<>());

    private final Random rnd = new Random();
    private volatile boolean run = true;
    private ServerSocket server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private final List<Conn> clients = new ArrayList<>();
    private Thread acceptThread, broadcastThread, clientReader;
    private float broadcastClock = 0;

    public static class Input {
        public float mx, my, dir, pitch;
        public boolean fire, jump, crouch, reload, plant;
        public int weapon;
    }

    private static class Conn {
        Socket socket; DataInputStream in; DataOutputStream out;
        volatile Input input = new Input();
        volatile boolean alive = true;
        int id; Entities.Player p;
    }

    public Lan(World world) {
        this.world = world;
        this.local = new Entities.Player(4.5f, 4.5f, Entities.TEAM_T, false);
        this.local.id = -1;
    }

    // ================= HOST =================
    public boolean startHost(String name) {
        try {
            host = true; active = true; run = true;
            myName = name;
            local.id = 0; local.name = name; local.team = Entities.TEAM_T;
            placeAtSpawn(local);
            synchronized (players) { players.clear(); players.add(local); }
            server = new ServerSocket(PORT);
            status = "IP: " + localIp() + ":" + PORT + "  •  1/" + MAX_PLAYERS;
            acceptThread = new Thread(this::acceptLoop, "lan-accept");
            acceptThread.start();
            broadcastThread = new Thread(this::broadcastLoop, "lan-broadcast");
            broadcastThread.start();
            return true;
        } catch (Exception e) {
            status = "Ошибка хоста: " + e.getMessage();
            active = false; host = false;
            return false;
        }
    }

    private String localIp() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (ni.isLoopback() || !ni.isUp()) continue;
                for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                    InetAddress a = ia.getAddress();
                    if (a instanceof Inet4Address && !a.isLoopbackAddress())
                        return a.getHostAddress();
                }
            }
        } catch (Exception ignored) {}
        return "192.168.x.x";
    }

    private void acceptLoop() {
        while (run && server != null && !server.isClosed()) {
            try {
                Socket s = server.accept();
                if (clients.size() >= MAX_PLAYERS - 1) { s.close(); continue; }
                Conn c = new Conn();
                c.socket = s;
                c.in = new DataInputStream(s.getInputStream());
                c.out = new DataOutputStream(s.getOutputStream());
                c.id = 1 + clients.size();
                synchronized (players) {
                    for (Entities.Player p : players) if (p.id == c.id) c.id++;
                    c.p = new Entities.Player(4.5f, 4.5f, c.id % 2 == 0 ? Entities.TEAM_T : Entities.TEAM_CT, false);
                    c.p.id = c.id;
                    c.p.name = "Player" + c.id;
                    placeAtSpawn(c.p);
                    players.add(c.p);
                }
                clients.add(c);
                status = "Игроков: " + (clients.size() + 1) + "/" + MAX_PLAYERS;
                c.out.writeByte(1); c.out.writeInt(c.id); c.out.flush();
                Thread t = new Thread(() -> readClient(c), "lan-client-" + c.id);
                t.start();
            } catch (Exception e) {
                if (run) status = "accept: " + e.getMessage();
            }
        }
    }

    private void readClient(Conn c) {
        try {
            while (run && c.alive) {
                int type = c.in.readByte();
                if (type == 3) {
                    Input i = new Input();
                    i.mx = c.in.readFloat(); i.my = c.in.readFloat();
                    i.dir = c.in.readFloat(); i.pitch = c.in.readFloat();
                    i.fire = c.in.readBoolean(); i.jump = c.in.readBoolean();
                    i.crouch = c.in.readBoolean(); i.reload = c.in.readBoolean();
                    i.plant = c.in.readBoolean(); i.weapon = c.in.readInt();
                    c.input = i;
                }
            }
        } catch (Exception e) {
            c.alive = false;
        }
        clients.remove(c);
        synchronized (players) { if (c.p != null) players.remove(c.p); }
        status = "Игроков: " + (clients.size() + 1) + "/" + MAX_PLAYERS;
    }

    private void broadcastLoop() {
        while (run && host) {
            try { Thread.sleep(45); } catch (InterruptedException e) { break; }
            if (!run) break;
            List<Entities.Player> snapshot;
            synchronized (players) { snapshot = new ArrayList<>(players); }
            for (Conn c : clients) {
                if (!c.alive) continue;
                try {
                    DataOutputStream o = c.out;
                    o.writeByte(2);
                    o.writeInt(snapshot.size());
                    for (Entities.Player p : snapshot) writePlayer(o, p);
                    o.flush();
                } catch (Exception e) { c.alive = false; }
            }
        }
    }

    private void writePlayer(DataOutputStream o, Entities.Player p) throws Exception {
        o.writeInt(p.id);
        o.writeUTF(p.name == null ? "Player" : p.name);
        o.writeFloat(p.x); o.writeFloat(p.y);
        o.writeFloat(p.dir); o.writeFloat(p.pitch);
        o.writeFloat(p.health);
        o.writeBoolean(p.alive);
        o.writeInt(p.team);
        o.writeInt(p.weapon);
        o.writeInt(p.kills);
        o.writeInt(p.skin);
    }

    // ================= CLIENT =================
    public boolean startClient(String ip, String name) {
        try {
            host = false; active = true; run = true;
            myName = name;
            status = "Подключение к " + ip + "...";
            socket = new Socket();
            socket.connect(new java.net.InetSocketAddress(ip, PORT), 4000);
            socket.setTcpNoDelay(true);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            int type = in.readByte();
            if (type != 1) throw new Exception("bad welcome");
            myId = in.readInt();
            local.id = myId; local.name = name;
            status = "Подключено, id " + myId;
            clientReader = new Thread(this::clientReadLoop, "lan-client-read");
            clientReader.start();
            return true;
        } catch (Exception e) {
            status = "Не подключиться: " + e.getMessage();
            active = false;
            return false;
        }
    }

    private void clientReadLoop() {
        List<Entities.Player> buf = new ArrayList<>();
        try {
            while (run) {
                byte type = in.readByte();
                if (type != 2) continue;
                int n = in.readInt();
                buf.clear();
                Entities.Player me = null;
                for (int i = 0; i < n; i++) {
                    Entities.Player p = readPlayer(in);
                    if (p.id == myId) { me = p; }
                    buf.add(p);
                }
                if (me != null) {
                    // переносим позицию/здоровье в свой локальный объект
                    if (local.alive && me.alive) {
                        local.x = me.x; local.y = me.y;
                    }
                    local.health = me.health;
                    local.alive = me.alive;
                    local.team = me.team;
                    local.kills = me.kills;
                    local.name = me.name;
                    local.skin = me.skin;
                }
                synchronized (players) {
                    players.clear();
                    players.addAll(buf);
                }
            }
        } catch (Exception e) {
            if (run) status = "Связь потеряна";
            active = false;
        }
    }

    private Entities.Player readPlayer(DataInputStream in) throws Exception {
        Entities.Player p = new Entities.Player(0, 0, Entities.TEAM_T, false);
        p.id = in.readInt();
        p.name = in.readUTF();
        p.x = in.readFloat(); p.y = in.readFloat();
        p.dir = in.readFloat(); p.pitch = in.readFloat();
        p.health = in.readFloat();
        p.alive = in.readBoolean();
        p.team = in.readInt();
        p.weapon = in.readInt();
        p.kills = in.readInt();
        p.skin = in.readInt();
        return p;
    }

    // ================= UPDATE =================
    /** Called every frame while LAN game is active. */
    public void update(GameView g, float dt) {
        if (!active) return;
        if (host) updateHost(g, dt);
        else updateClient(g, dt);
        updateParticles(dt);
    }

    private Input inputFromGame(GameView g) {
        Input i = new Input();
        i.mx = g.joyX; i.my = g.joyY;
        i.dir = local.dir; i.pitch = local.pitch;
        i.fire = g.firing;
        i.jump = g.wantJump; i.crouch = g.wantCrouch;
        i.reload = g.wantReload; i.plant = g.wantPlant;
        i.weapon = local.weapon;
        g.wantJump = false; g.wantReload = false; g.wantPlant = false;
        return i;
    }

    private void updateHost(GameView g, float dt) {
        // host local input
        applyInput(local, inputFromGame(g), dt);
        // remote inputs
        for (Conn c : new ArrayList<>(clients)) {
            if (c.alive && c.p != null) applyInput(c.p, c.input, dt);
        }
        // respawns + timers
        List<Entities.Player> all;
        synchronized (players) { all = new ArrayList<>(players); }
        for (Entities.Player p : all) {
            if (p.fireTimer > 0) p.fireTimer -= dt;
            if (p.reloadTimer > 0) p.reloadTimer -= dt;
            if (!p.alive) {
                p.respawnTimer -= dt;
                if (p.respawnTimer <= 0) {
                    p.alive = true; p.health = 100; p.armor = 0;
                    placeAtSpawn(p);
                }
            }
        }
    }

    private float inputClock = 0;

    private void updateClient(GameView g, float dt) {
        // локальный обзор — сразу, позиция — с сервера
        local.dir = g.player != null ? g.player.dir : local.dir;
        local.crouching = g.wantCrouch;
        inputClock += dt;
        if (out != null && inputClock >= 0.033f) {
            inputClock = 0;
            try {
                Input i = inputFromGame(g);
                out.writeByte(3);
                out.writeFloat(i.mx); out.writeFloat(i.my);
                out.writeFloat(local.dir); out.writeFloat(local.pitch);
                out.writeBoolean(i.fire); out.writeBoolean(i.jump);
                out.writeBoolean(i.crouch); out.writeBoolean(i.reload);
                out.writeBoolean(i.plant); out.writeInt(local.weapon);
                out.flush();
            } catch (Exception e) { active = false; }
        }
        // particles from shots locally for feedback
        if (g.firing && local.alive) {
            if (local.fireTimer > 0) local.fireTimer -= dt;
            if (local.fireTimer <= 0) {
                local.fireTimer = GameData.WEAPON_DELAY[local.weapon];
                addMuzzle(local);
            }
        }
        if (local.fireTimer > 0) local.fireTimer -= dt;
    }

    private void applyInput(Entities.Player p, Input in, float dt) {
        if (!p.alive) return;
        p.dir = in.dir; p.pitch = Math.max(-0.45f, Math.min(0.45f, in.pitch));
        p.crouching = in.crouch;
        p.weapon = Math.max(0, Math.min(4, in.weapon));

        float spd = p.crouching ? 1.6f : 3.0f;
        float mx = in.mx, my = in.my;
        float l = (float) Math.hypot(mx, my);
        if (l > 1) { mx /= l; my /= l; }
        float dx = (float) (Math.cos(p.dir) * -my + Math.cos(p.dir + Math.PI / 2) * mx) * spd * dt;
        float dy = (float) (Math.sin(p.dir) * -my + Math.sin(p.dir + Math.PI / 2) * mx) * spd * dt;
        float[] out = new float[2];
        world.moveWithCollision(p.x, p.y, p.x + dx, p.y + dy, 0.24f, out);
        p.x = out[0]; p.y = out[1];

        if (in.fire) shoot(p);
    }

    private void shoot(Entities.Player shooter) {
        int w = shooter.weapon;
        if (shooter.fireTimer > 0) return;
        shooter.fireTimer = GameData.WEAPON_DELAY[w];
        float spread = GameData.WEAPON_SPREAD[w] * 1.4f;
        float range = GameData.WEAPON_RANGE[w];
        List<Entities.Player> all;
        synchronized (players) { all = new ArrayList<>(players); }
        for (int pellet = 0; pellet < GameData.WEAPON_PELLETS[w]; pellet++) {
            float a = shooter.dir + (rnd.nextFloat() - 0.5f) * spread * 2f;
            float rdx = (float) Math.cos(a), rdy = (float) Math.sin(a);
            World.Hit wh = world.castRay(shooter.x, shooter.y, rdx, rdy, new World.Hit());
            float wall = Math.min(wh.dist, range);
            Entities.Player best = null; float bestT = wall;
            for (Entities.Player t : all) {
                if (t == shooter || !t.alive || t.team == shooter.team) continue;
                float dx = t.x - shooter.x, dy = t.y - shooter.y;
                float proj = dx * rdx + dy * rdy;
                if (proj <= 0 || proj > bestT) continue;
                float perp = Math.abs(dx * rdy - dy * rdx);
                if (perp < 0.38f) { best = t; bestT = proj; }
            }
            if (best != null) {
                best.health -= GameData.WEAPON_DAMAGE[w];
                if (best.health <= 0) {
                    best.health = 0; best.alive = false; best.respawnTimer = 2.5f;
                    shooter.kills++;
                    synchronized (corpses) { corpses.add(new Entities.Corpse(best.x, best.y, best.dir)); }
                    if (corpses.size() > 12) corpses.remove(0);
                }
            }
        }
        addMuzzle(shooter);
    }

    private void addMuzzle(Entities.Player p) {
        Entities.Particle pt = new Entities.Particle();
        pt.x = p.x + (float) Math.cos(p.dir) * 0.5f;
        pt.y = p.y + (float) Math.sin(p.dir) * 0.5f;
        pt.z = 0.5f; pt.color = 0xFFDD66; pt.size = 0.06f; pt.kind = 0;
        pt.life = 0.3f; pt.maxLife = 0.3f;
        pt.vx = 0; pt.vy = 0; pt.vz = 0;
        synchronized (particles) { particles.add(pt); }
    }

    private void updateParticles(float dt) {
        synchronized (particles) {
            for (int i = particles.size() - 1; i >= 0; i--) {
                Entities.Particle p = particles.get(i);
                p.life -= dt;
                if (p.life <= 0) particles.remove(i);
            }
        }
        synchronized (corpses) {
            for (int i = corpses.size() - 1; i >= 0; i--) {
                Entities.Corpse c = corpses.get(i);
                c.life -= dt;
                if (c.life <= 0) corpses.remove(i);
            }
        }
    }

    private void placeAtSpawn(Entities.Player p) {
        float x = 4.5f, y = 4.5f;
        List<World.Spawn> ts = world.tSpawns;
        List<World.Spawn> cs = world.cSpawns;
        List<World.Spawn> pool = (p.team == Entities.TEAM_CT) ? cs : ts;
        if (pool != null && !pool.isEmpty()) {
            World.Spawn s = pool.get(rnd.nextInt(pool.size()));
            x = s.x; y = s.y;
        }
        p.x = x; p.y = y;
        // small offset so players don't spawn inside each other
        p.x += (rnd.nextFloat() - 0.5f) * 0.8f;
        p.y += (rnd.nextFloat() - 0.5f) * 0.8f;
    }

    public Entities.Player myPlayer() {
        if (host) return local;
        return local;
    }

    public List<Entities.Player> others(Entities.Player me) {
        List<Entities.Player> r = new ArrayList<>();
        synchronized (players) {
            for (Entities.Player p : players) {
                if (p == me || p.id == me.id) continue;
                r.add(p);
            }
        }
        return r;
    }

    public int playerCount() {
        synchronized (players) { return players.size(); }
    }

    public void stop() {
        run = false;
        try { if (server != null) server.close(); } catch (Exception ignored) {}
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
        active = false;
        host = false;
    }
}
