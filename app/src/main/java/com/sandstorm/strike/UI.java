package com.sandstorm.strike;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

/** All 2D UI: menus, HUD, touch controls, click routing. */
public final class UI {

    static final int A_PLAY=1,A_ELO=2,A_CLAN=3,A_SKINS=4,A_SETTINGS=5,A_HELP=6,A_BACK=7,
            A_OFFLINE=8,A_ONLINE=9,A_LAN=10,A_STARTLAN=11,A_NEXTROUND=12,
            A_SENS_MINUS=20,A_SENS_PLUS=21,A_CROSS=22,A_CROSSCOLOR=23,
            A_CROSSSIZE_MINUS=24,A_CROSSSIZE_PLUS=25,A_BHOP=26,A_BHOPMULT=27,
            A_INVERT=28,A_QUALITY=29,A_EFFECT=30,A_FPS=31,A_SKY=32,A_DIFF=33,
            A_LAN_HOST=34,A_LAN_JOIN=35,A_SKIN_BASE=100,
            A_CLAN_CYCLE=200,A_SYNC=201;

    static final int G_FIRE=500,G_JUMP=501,G_CROUCH=502,G_RELOAD=503,
            G_PLANT=504,G_SWITCH=505,G_PAUSE=506;

    private static Paint p(int color,float size,boolean bold){
        Paint q=new Paint(Paint.ANTI_ALIAS_FLAG);
        q.setColor(color); q.setTextSize(size); q.setFakeBoldText(bold);
        return q;
    }
    private static final int GOLD=0xE8C24C, SAND=0xD8C39A, DARK=0x101820, PANEL=0x1C2630E0;

    private static void bg(Canvas c,int w,int h){
        c.drawColor(0x0B1016);
        Paint g=new Paint(Paint.ANTI_ALIAS_FLAG);
        for(int i=0;i<12;i++){
            g.setColor(Color.argb(10,232,194,76));
            c.drawRect(0,h*i/12,w,h*i/12+h/24,g);
        }
    }

    private static void btn(GameView v,Canvas c,float rx,float ry,float rw,float rh,String label,int action){
        float x=rx*v.viewW,y=ry*v.viewH,w=rw*v.viewW,h=rh*v.viewH;
        v.buttons.add(new GameView.Btn(x,y,w,h,action,label));
        Paint fill=new Paint(Paint.ANTI_ALIAS_FLAG); fill.setColor(0x22303C);
        c.drawRoundRect(new RectF((int)x,(int)y,(int)(x+w),(int)(y+h)),10,10,fill);
        Paint border=new Paint(Paint.ANTI_ALIAS_FLAG); border.setColor(GOLD); border.setStyle(Paint.Style.STROKE); border.setStrokeWidth(2);
        c.drawRoundRect(new RectF((int)x,(int)y,(int)(x+w),(int)(y+h)),10,10,border);
        Paint t=p(SAND,Math.max(15,h*0.42f),true);
        t.setTextAlign(Paint.Align.CENTER);
        c.drawText(label,x+w/2,y+h/2+h*0.15f,t);
    }

    private static void title(Canvas c,int w,int h){
        Paint t1=p(GOLD,Math.max(30,w*0.075f),true); t1.setTextAlign(Paint.Align.CENTER);
        c.drawText("SANDSTORM",w/2f,h*0.17f,t1);
        Paint t2=p(SAND,Math.max(18,w*0.045f),true); t2.setTextAlign(Paint.Align.CENTER);
        c.drawText("S T R I K E",w/2f,h*0.24f,t2);
        Paint t3=p(0x8FA0AE,Math.max(11,w*0.02f),false); t3.setTextAlign(Paint.Align.CENTER);
        c.drawText("оригинальная тактическая FPS",w/2f,h*0.30f,t3);
    }

    private static void info(GameView v,Canvas c,String s1,String s2,float y){
        Paint t=p(SAND,Math.max(13,v.viewW*0.026f),false); t.setTextAlign(Paint.Align.CENTER);
        c.drawText(s1,v.viewW/2f,y,t);
        Paint t2=p(GOLD,Math.max(15,v.viewW*0.03f),true); t2.setTextAlign(Paint.Align.CENTER);
        c.drawText(s2,v.viewW/2f,y+v.viewH*0.045f,t2);
    }

    // ============ MENUS ============
    public static void drawMenu(GameView v,Canvas c,int w,int h){
        v.buttons.clear();
        bg(c,w,h);
        switch(v.screen){
            case GameView.S_MENU: {
                title(c,w,h);
                btn(v,c,0.34f,0.38f,0.32f,0.085f,"ИГРАТЬ",A_PLAY);
                btn(v,c,0.34f,0.49f,0.32f,0.085f,"ELO / ЛИДЕРЫ",A_ELO);
                btn(v,c,0.34f,0.60f,0.32f,0.085f,"КЛАНЫ",A_CLAN);
                btn(v,c,0.34f,0.71f,0.32f,0.085f,"СКИНЫ",A_SKINS);
                btn(v,c,0.10f,0.82f,0.20f,0.08f,"НАСТРОЙКИ",A_SETTINGS);
                btn(v,c,0.70f,0.82f,0.20f,0.08f,"УПРАВЛЕНИЕ",A_HELP);
                Paint t=p(0x7C8C99,Math.max(11,w*0.019f),false); t.setTextAlign(Paint.Align.CENTER);
                int lvl=1+Math.max(0,v.elo)/150;
                c.drawText("Ур."+lvl+"   •   ELO "+v.elo+(v.pendingElo!=0?"  (+"+v.pendingElo+" офлайн)":"")+"   •   "+v.wins+"W / "+v.losses+"L",w/2f,h*0.95f,t);
                break;
            }
            case GameView.S_PLAY: {
                Paint t=p(GOLD,Math.max(22,w*0.05f),true);t.setTextAlign(Paint.Align.CENTER);
                c.drawText("ВЫБЕРИ РЕЖИМ",w/2f,h*0.18f,t);
                btn(v,c,0.30f,0.32f,0.40f,0.10f,"ОФЛАЙН  (ELO копится)",A_OFFLINE);
                btn(v,c,0.30f,0.45f,0.40f,0.10f,"ОНЛАЙН  (ELO сразу)",A_ONLINE);
                btn(v,c,0.30f,0.58f,0.40f,0.10f,"LAN  (с друзьями)",A_LAN);
                btn(v,c,0.30f,0.78f,0.40f,0.08f,"НАЗАД",A_BACK);
                break;
            }
            case GameView.S_LAN: {
                Paint t=p(GOLD,Math.max(22,w*0.05f),true);t.setTextAlign(Paint.Align.CENTER);
                c.drawText("LAN — ЛОКАЛЬНАЯ СЕТЬ",w/2f,h*0.16f,t);
                Paint b=p(SAND,Math.max(13,w*0.025f),false);
                String[] lines={
                    "До 10 игроков. Один хост, остальные входят по IP.",
                    "Все устройства в одной Wi-Fi сети.",
                    "Хост создаёт комнату и видит свой IP.",
                    "Формат: deathmatch с респавном."
                };
                for(int i=0;i<lines.length;i++){
                    b.setTextAlign(Paint.Align.CENTER);
                    c.drawText(lines[i],w/2f,h*0.27f+i*h*0.05f,b);
                }
                if(v.lan!=null){
                    Paint st=p(0x7FD8A0,Math.max(12,w*0.024f),false);st.setTextAlign(Paint.Align.CENTER);
                    c.drawText(v.lan.status,w/2f,h*0.50f,st);
                }
                btn(v,c,0.30f,0.56f,0.40f,0.09f,"СОЗДАТЬ КОМНАТУ (ХОСТ)",A_LAN_HOST);
                btn(v,c,0.30f,0.68f,0.40f,0.09f,"ВОЙТИ ПО IP",A_LAN_JOIN);
                btn(v,c,0.32f,0.82f,0.36f,0.07f,"НАЗАД",A_BACK);
                break;
            }
            case GameView.S_SETTINGS: {
                Paint t=p(GOLD,Math.max(22,w*0.05f),true);t.setTextAlign(Paint.Align.CENTER);
                c.drawText("НАСТРОЙКИ",w/2f,h*0.09f,t);
                int[] rows={A_SENS_MINUS,A_CROSS,A_CROSSCOLOR,A_CROSSSIZE_MINUS,A_BHOP,
                            A_BHOPMULT,A_INVERT,A_QUALITY,A_EFFECT,A_FPS,A_DIFF};
                String[] names={
                    "Чувствительность","Прицел (стиль)","Цвет прицела","Размер прицела",
                    "Баннихоп","Множитель БХ","Инверт Y","Графика","Эффект убийства","Показ FPS","Сложность ботов"
                };
                String[] vals={
                    String.format("%.2f",v.sensitivity), String.valueOf(v.crosshairStyle),
                    String.format("#%06X",v.crosshairColor&0xFFFFFF), String.valueOf(v.crosshairSize),
                    v.bhopEnabled?"ВКЛ":"ВЫКЛ", "x"+String.format("%.1f",v.bhopMult),
                    v.invertY?"ДА":"НЕТ", new String[]{"НИЗК","СРЕД","ВЫСОК"}[v.quality],
                    GameData.EFFECT_NAMES[v.effect%GameData.EFFECT_COUNT], v.showFps?"ДА":"НЕТ",
                    new String[]{"ЛЁГКИЕ","СРЕДНИЕ+","ХАРД"}[v.difficulty]
                };
                float sy=h*0.14f, sh=h*0.062f;
                for(int i=0;i<rows.length;i++){
                    float y=sy+i*sh;
                    Paint nm=p(SAND,Math.max(12,w*0.024f),false);
                    c.drawText(names[i],w*0.08f,y+sh*0.45f,nm);
                    Paint vl=p(GOLD,Math.max(12,w*0.024f),true); vl.setTextAlign(Paint.Align.RIGHT);
                    c.drawText(vals[i],w*0.70f,y+sh*0.45f,vl);
                    btn(v,c,0.72f,y/h,0.11f,sh/h*0.8f,"<",rows[i]+1000);
                    btn(v,c,0.85f,y/h,0.11f,sh/h*0.8f,">",rows[i]);
                }
                btn(v,c,0.30f,0.835f,0.40f,0.05f,"НЕБО СВОЁ ФОТО",A_SKY);
                btn(v,c,0.36f,0.90f,0.28f,0.055f,"НАЗАД",A_BACK);
                break;
            }
            case GameView.S_ELO: {
                Paint t=p(GOLD,Math.max(22,w*0.05f),true);t.setTextAlign(Paint.Align.CENTER);
                c.drawText("ТАБЛИЦА ELO",w/2f,h*0.10f,t);
                info(v,c,"Твой ELO",String.valueOf(v.elo),h*0.17f);
                String[] names={"Ghost_Reaper","SandKing","NoScopePro","AlfaTiger","DesertFox",
                                "Vortex","NightWolf","Ты","SniperX","Titan"};
                int[] elo={1650,1520,1410,1320,1210,1150,1080,v.elo,940,880};
                boolean[] me={false,false,false,false,false,false,false,true,false,false};
                Paint r=p(SAND,Math.max(13,w*0.026f),false);
                for(int i=0;i<names.length;i++){
                    float y=h*0.30f+i*h*0.05f;
                    r.setColor(me[i]?GOLD:(i<3?0x7FD8A0:SAND));
                    c.drawText((i+1)+".  "+names[i],w*0.16f,y,r);
                    Paint e=p(me[i]?GOLD:0xB0BEC8,Math.max(13,w*0.026f),me[i]);e.setTextAlign(Paint.Align.RIGHT);
                    c.drawText(String.valueOf(elo[i]),w*0.84f,y,e);
                }
                if(v.pendingElo!=0){
                    btn(v,c,0.30f,0.84f,0.40f,0.08f,"СИНХРОНИЗИРОВАТЬ +"+v.pendingElo,A_SYNC);
                }
                btn(v,c,0.36f,0.93f,0.28f,0.06f,"НАЗАД",A_BACK);
                break;
            }
            case GameView.S_CLAN: {
                Paint t=p(GOLD,Math.max(22,w*0.05f),true);t.setTextAlign(Paint.Align.CENTER);
                c.drawText("КЛАНЫ",w/2f,h*0.12f,t);
                String cl=v.clanName.isEmpty()?"— нет —":v.clanName;
                info(v,c,"Твой клан",cl,h*0.22f);
                Paint b=p(SAND,Math.max(13,w*0.025f),false);b.setTextAlign(Paint.Align.CENTER);
                c.drawText("Кланы фармят ELO вместе с союзниками.",w/2f,h*0.38f,b);
                c.drawText("Меняй название клана кнопкой ниже.",w/2f,h*0.44f,b);
                btn(v,c,0.30f,0.55f,0.40f,0.09f,"СМЕНИТЬ КЛАН",A_CLAN_CYCLE);
                btn(v,c,0.36f,0.88f,0.28f,0.07f,"НАЗАД",A_BACK);
                break;
            }
            case GameView.S_SKINS: {
                Paint t=p(GOLD,Math.max(22,w*0.05f),true);t.setTextAlign(Paint.Align.CENTER);
                c.drawText("СКИНЫ ОРУЖИЯ",w/2f,h*0.08f,t);
                float sy=h*0.16f,sh=h*0.14f;
                for(int wI=0;wI<5;wI++){
                    float y=sy+wI*sh;
                    Paint nm=p(SAND,Math.max(13,w*0.026f),false);
                    c.drawText(GameData.WEAPON_NAMES[wI],w*0.06f,y+sh*0.3f,nm);
                    for(int s=0;s<GameData.SKINS_PER_WEAPON;s++){
                        int col=GameData.SKIN_COLORS[wI][s];
                        Paint sw=new Paint(Paint.ANTI_ALIAS_FLAG);sw.setColor(col);
                        float bx=w*(0.30f+s*0.085f),by=y;
                        c.drawRoundRect(new RectF((int)bx,(int)by,(int)(bx+w*0.065f),(int)(by+sh*0.6f)),8,8,sw);
                        if(v.skinPerWeapon[wI]==s){
                            Paint sel=new Paint();sel.setColor(Color.WHITE);sel.setStyle(Paint.Style.STROKE);sel.setStrokeWidth(4);
                            c.drawRoundRect(new RectF((int)bx,(int)by,(int)(bx+w*0.065f),(int)(by+sh*0.6f)),8,8,sel);
                        }
                        v.buttons.add(new GameView.Btn(bx,by,w*0.065f,sh*0.6f,A_SKIN_BASE+wI*GameData.SKINS_PER_WEAPON+s,""));
                    }
                }
                btn(v,c,0.36f,0.93f,0.28f,0.06f,"НАЗАД",A_BACK);
                break;
            }
            case GameView.S_HELP: {
                Paint t=p(GOLD,Math.max(22,w*0.05f),true);t.setTextAlign(Paint.Align.CENTER);
                c.drawText("УПРАВЛЕНИЕ",w/2f,h*0.10f,t);
                String[] lines={
                    "ЛЕВАЯ ПОЛОВИНА — джойстик движения",
                    "ПРАВАЯ ПОЛОВИНА — поворот/прицел (плавно)",
                    "ОГОНЬ — стрелять (удержание)",
                    "ПРЫГ — баннихоп x"+String.format("%.1f",v.bhopMult),
                    "ПРИСЕД — тише, точнее, обезвреживание бомбы",
                    "ПЕРЕЗ — перезарядка",
                    "ЗАКЛАДКА — заложить/обезвредить бомбу",
                    "E — закладка (клавиатура), R — перезарядка",
                    "WASD — движение, стрелки — поворот"
                };
                Paint b=p(SAND,Math.max(12,w*0.023f),false);
                for(int i=0;i<lines.length;i++){
                    b.setTextAlign(Paint.Align.CENTER);
                    c.drawText(lines[i],w/2f,h*0.20f+i*h*0.065f,b);
                }
                btn(v,c,0.36f,0.90f,0.28f,0.07f,"НАЗАД",A_BACK);
                break;
            }
            case GameView.S_END: {
                // handled as overlay in HUD
                break;
            }
        }
    }

    // ============ HUD ============
    public static void drawHUD(GameView v,Canvas c,int w,int h){
        v.buttons.clear();
        if(v.screen==GameView.S_END){
            Paint sh=new Paint();sh.setColor(0xAA000000);c.drawRect(0,0,w,h,sh);
            Paint t=p(v.roundWinner==v.player.team?0x7FD8A0:0xE86A5A,Math.max(28,w*0.06f),true);t.setTextAlign(Paint.Align.CENTER);
            c.drawText(v.roundWinner==v.player.team?"ПОБЕДА":"ПОРАЖЕНИЕ",w/2f,h*0.35f,t);
            Paint t2=p(SAND,Math.max(16,w*0.032f),false);t2.setTextAlign(Paint.Align.CENTER);
            c.drawText(v.endMessage,w/2f,h*0.45f,t2);
            c.drawText("Счёт  "+v.scoreT+" : "+v.scoreCT,w/2f,h*0.52f,t2);
            btn(v,c,0.36f,0.62f,0.28f,0.10f,"СЛЕДУЮЩИЙ РАУНД",A_NEXTROUND);
            return;
        }
        Entities.Player p=v.player;
        if(p==null) return;

        // top bar
        Paint bar=new Paint();bar.setColor(0x900B1016);c.drawRect(0,0,w,h*0.075f,bar);
        Paint tm=p(Color.WHITE,Math.max(18,w*0.035f),true);tm.setTextAlign(Paint.Align.CENTER);
        String tt=v.roundState==1?String.format("%.0f",v.bomb.timer):String.format("%.0f",v.roundTimer);
        c.drawText(tt,w/2f,h*0.05f,tm);
        Paint sc=p(v.scoreT>v.scoreCT?0xE8C24C:0xE86A5A,Math.max(16,w*0.03f),true);sc.setTextAlign(Paint.Align.RIGHT);
        c.drawText("T "+v.scoreT,w/2f-w*0.07f,h*0.05f,sc);
        Paint sc2=p(v.scoreCT>v.scoreT?0x5AA8E8:0xE8E8E8,Math.max(16,w*0.03f),true);
        c.drawText("КТ "+v.scoreCT,w/2f+w*0.03f,h*0.05f,sc2);

        // ELO / mode
        Paint el=p(GOLD,Math.max(12,w*0.022f),false);
        c.drawText("ELO "+v.elo+(v.mode==GameView.MODE_OFFLINE?" (офф)":""),w*0.02f,h*0.05f,el);

        // crosshair
        drawCrosshair(v,c,w/2f,h/2f);

        // health / armor
        Paint hp=p(p.health>30?0x7FD8A0:0xE86A5A,Math.max(18,w*0.035f),true);
        c.drawText("♥ "+Math.round(p.health),w*0.02f,h*0.96f,hp);
        if(p.armor>0){ Paint ar=p(0x5AA8E8,Math.max(15,w*0.028f),true); c.drawText("◈ "+Math.round(p.armor),w*0.02f,h*0.91f,ar); }

        // ammo
        Paint am=p(Color.WHITE,Math.max(22,w*0.042f),true);am.setTextAlign(Paint.Align.RIGHT);
        c.drawText(p.ammo[p.weapon]+" / "+p.reserve[p.weapon],w*0.98f,h*0.96f,am);
        Paint wn=p(GOLD,Math.max(13,w*0.025f),false);wn.setTextAlign(Paint.Align.RIGHT);
        c.drawText(GameData.WEAPON_NAMES[p.weapon]+"  •  "+GameData.SKIN_NAMES[p.skin%GameData.SKINS_PER_WEAPON],w*0.98f,h*0.915f,wn);

        // minimap
        drawMinimap(v,c,w,h);

        // kill feed
        Paint kf=p(0xD0DAE2,Math.max(11,w*0.02f),false);kf.setTextAlign(Paint.Align.RIGHT);
        for(int i=0;i<v.killFeed.size();i++) c.drawText(v.killFeed.get(i),w*0.98f,h*0.12f+i*h*0.035f,kf);

        // round message
        if(v.roundMessageT>0){
            Paint rm=p(GOLD,Math.max(20,w*0.04f),true);rm.setTextAlign(Paint.Align.CENTER);
            c.drawText(v.roundMessage,w/2f,h*0.22f,rm);
        }

        // touch controls
        drawTouchControls(v,c,w,h);

        // fps
        if(v.showFps){ Paint f=p(0x88FF88,Math.max(11,w*0.02f),false); c.drawText("FPS "+Math.round(v.fps),w*0.02f,h*0.12f,f); }
    }

    public static void drawLanHUD(GameView v,Canvas c,int w,int h){
        v.buttons.clear();
        Entities.Player p=v.player;
        if(p==null) return;
        // top bar
        Paint bar=new Paint();bar.setColor(0x900B1016);c.drawRect(0,0,w,h*0.075f,bar);
        Paint lanT=p(0x7FD8A0,Math.max(14,w*0.027f),true);
        c.drawText("LAN  "+(v.lan!=null?v.lan.status:""),w*0.02f,h*0.048f,lanT);
        Paint cnt=p(GOLD,Math.max(14,w*0.027f),true);cnt.setTextAlign(Paint.Align.RIGHT);
        c.drawText("Игроков: "+(v.lan!=null?v.lan.playerCount():0),w*0.98f,h*0.048f,cnt);
        // crosshair
        drawCrosshair(v,c,w/2f,h/2f);
        // health
        Paint hp=p(p.health>30?0x7FD8A0:0xE86A5A,Math.max(20,w*0.038f),true);
        c.drawText("♥ "+Math.round(p.health),w*0.02f,h*0.96f,hp);
        // weapon
        Paint wn=p(GOLD,Math.max(13,w*0.025f),false);wn.setTextAlign(Paint.Align.RIGHT);
        c.drawText(GameData.WEAPON_NAMES[p.weapon],w*0.98f,h*0.93f,wn);
        // scoreboard
        java.util.List<Entities.Player> all=new java.util.ArrayList<>();
        if(v.lan!=null){ synchronized(v.lan.players){ all.addAll(v.lan.players);} }
        all.sort((a,b)->Integer.compare(b.kills,a.kills));
        Paint sb=p(0xD0DAE2,Math.max(11,w*0.019f),false);sb.setTextAlign(Paint.Align.RIGHT);
        for(int i=0;i<Math.min(6,all.size());i++){
            Entities.Player q=all.get(i);
            sb.setColor(q.id==p.id?GOLD:0xD0DAE2);
            c.drawText((i+1)+". "+q.name+"  "+q.kills,w*0.98f,h*0.13f+i*h*0.035f,sb);
        }
        drawTouchControls(v,c,w,h);
    }

    private static void drawCrosshair(GameView v,Canvas c,float cx,float cy){
        Paint q=new Paint(Paint.ANTI_ALIAS_FLAG);q.setColor(v.crosshairColor);q.setStrokeWidth(2);
        int s=v.crosshairSize;
        switch(v.crosshairStyle){
            case 0: c.drawCircle(cx,cy,2,q);break;
            case 1: c.drawLine(cx-s,cy,cx+s,cy,q);c.drawLine(cx,cy-s,cx,cy+s,q);break;
            case 2: q.setStyle(Paint.Style.STROKE);c.drawCircle(cx,cy,s,q);break;
            case 3: c.drawLine(cx-s,cy,cx-s/3,cy,q);c.drawLine(cx+s/3,cy,cx+s,cy,q);c.drawLine(cx,cy-s,cx,cy-s/3,q);break;
            default: c.drawCircle(cx,cy,2,q);q.setStyle(Paint.Style.STROKE);c.drawCircle(cx,cy,s,q);break;
        }
    }

    private static void drawMinimap(GameView v,Canvas c,int w,int h){
        int size=Math.round(w*0.16f);
        float ox=w-size-w*0.015f, oy=h*0.09f;
        float cs=size/(float)GameData.MAP_W;
        Paint floorP=new Paint();floorP.setColor(0x55483A);
        Paint wallP=new Paint();wallP.setColor(0x2A2018);
        c.drawRect(ox,oy,ox+size,oy+size,floorP);
        for(int y=0;y<GameData.MAP_H;y++)for(int x=0;x<GameData.MAP_W;x++){
            char ch=v.world.cellAt(x,y);
            if(ch=='#'){c.drawRect(ox+x*cs,oy+y*cs,ox+(x+1)*cs,oy+(y+1)*cs,wallP);}
        }
        Paint siteA=new Paint();siteA.setColor(0xE8C24C);
        for(World.Site s:v.world.siteA) c.drawCircle(ox+s.x*cs,oy+s.y*cs,cs,siteA);
        Paint siteB=new Paint();siteB.setColor(0x5AA8E8);
        for(World.Site s:v.world.siteB) c.drawCircle(ox+s.x*cs,oy+s.y*cs,cs,siteB);
        // teammates
        Paint mate=new Paint();mate.setColor(0x7FD8A0);
        for(Entities.Player b:v.bots) if(b.alive&&b.team==v.player.team)
            c.drawCircle(ox+b.x*cs,oy+b.y*cs,cs*0.7f,mate);
        // player
        Paint me=new Paint();me.setColor(Color.WHITE);
        c.drawCircle(ox+v.player.x*cs,oy+v.player.y*cs,cs*0.9f,me);
    }

    private static void touchBtn(GameView v,Canvas c,float rx,float ry,float rw,float rh,String label,int action,int color){
        float x=rx*v.viewW,y=ry*v.viewH,w=rw*v.viewW,h=rh*v.viewH;
        v.buttons.add(new GameView.Btn(x,y,w,h,action,label));
        Paint q=new Paint(Paint.ANTI_ALIAS_FLAG);q.setColor(color);
        c.drawRoundRect(new RectF((int)x,(int)y,(int)(x+w),(int)(y+h)),12,12,q);
        Paint b=new Paint(Paint.ANTI_ALIAS_FLAG);b.setColor(0x88FFFFFF);b.setStyle(Paint.Style.STROKE);b.setStrokeWidth(2);
        c.drawRoundRect(new RectF((int)x,(int)y,(int)(x+w),(int)(y+h)),12,12,b);
        Paint t=p(Color.WHITE,Math.max(11,h*0.30f),true);t.setTextAlign(Paint.Align.CENTER);
        c.drawText(label,x+w/2,y+h/2+h*0.12f,t);
    }

    private static void drawTouchControls(GameView v,Canvas c,int w,int h){
        // fire big button bottom-right
        touchBtn(v,c,0.78f,0.55f,0.20f,0.40f,"ОГОНЬ",G_FIRE,0x99C0392B);
        touchBtn(v,c,0.66f,0.80f,0.11f,0.15f,"ПРЫГ",G_JUMP,0x884A6A8A);
        touchBtn(v,c,0.66f,0.64f,0.11f,0.15f,"ПРИСЕД",G_CROUCH,0x884A6A8A);
        touchBtn(v,c,0.55f,0.88f,0.10f,0.10f,"ПЕРЕЗ",G_RELOAD,0x887A6A4A);
        touchBtn(v,c,0.55f,0.76f,0.10f,0.10f,"ЗАКЛ",G_PLANT,0x886A4A7A);
        touchBtn(v,c,0.40f,0.90f,0.12f,0.08f,"ОРУЖ",G_SWITCH,0x884A6A8A);
        // pause
        touchBtn(v,c,0.90f,0.02f,0.09f,0.05f,"II",G_PAUSE,0x66000000);
        // joystick zone hint
        Paint j=new Paint(Paint.ANTI_ALIAS_FLAG);j.setColor(0x33FFFFFF);j.setStyle(Paint.Style.STROKE);j.setStrokeWidth(2);
        c.drawCircle(w*0.14f,h*0.78f,w*0.09f,j);
        Paint t=p(0x66FFFFFF,Math.max(10,w*0.02f),false);t.setTextAlign(Paint.Align.CENTER);
        c.drawText("ДВИЖЕНИЕ",w*0.14f,h*0.90f,t);
    }

    // ============ CLICK HANDLING ============
    public static void handleClick(GameView v,float x,float y,int w,int h){
        for(GameView.Btn b:v.buttons){
            if(!b.contains(x,y)) continue;
            int a=b.action;
            if(a>=A_SKIN_BASE && a<A_SKIN_BASE+5*GameData.SKINS_PER_WEAPON){
                int idx=a-A_SKIN_BASE;
                int wi=idx/GameData.SKINS_PER_WEAPON, sk=idx%GameData.SKINS_PER_WEAPON;
                v.skinPerWeapon[wi]=sk;
                if(v.player!=null&&v.player.weapon==wi)v.player.skin=sk;
                v.savePrefs();return;
            }
            switch(a){
                case A_PLAY: v.screen=GameView.S_PLAY;break;
                case A_ELO: v.screen=GameView.S_ELO;break;
                case A_CLAN: v.screen=GameView.S_CLAN;break;
                case A_SKINS: v.screen=GameView.S_SKINS;break;
                case A_SETTINGS: v.screen=GameView.S_SETTINGS;break;
                case A_HELP: v.screen=GameView.S_HELP;break;
                case A_BACK: v.screen=GameView.S_MENU;break;
                case A_OFFLINE: v.startMatch(GameView.MODE_OFFLINE);break;
                case A_ONLINE: v.startMatch(GameView.MODE_ONLINE);break;
                case A_LAN: v.screen=GameView.S_LAN;break;
                case A_LAN_HOST: v.startLanHost();break;
                case A_LAN_JOIN:
                    if(v.getContext() instanceof MainActivity) ((MainActivity)v.getContext()).promptIp();
                    break;
                case A_NEXTROUND: v.nextRound();break;
                case A_SYNC: v.syncElo();break;
                case A_SKY: if(v.getContext() instanceof MainActivity) ((MainActivity)v.getContext()).pickSky();break;
                case A_CLAN_CYCLE: {
                    String[] clans={"","WOLF PACK","DESERT EAGLE","NIGHT RAID","STORM UNIT","IRON CLAN"};
                    int i=0;for(int k=0;k<clans.length;k++)if(clans[k].equals(v.clanName))i=k;
                    v.clanName=clans[(i+1)%clans.length];v.savePrefs();break;
                }
                case A_SENS_MINUS+1000: v.sensitivity=Math.max(0.2f,v.sensitivity-0.1f);v.savePrefs();break;
                case A_SENS_MINUS: v.sensitivity=Math.min(3.0f,v.sensitivity+0.1f);v.savePrefs();break;
                case A_CROSS+1000: case A_CROSS: v.crosshairStyle=(v.crosshairStyle+(a==A_CROSS?1:4))%5;v.savePrefs();break;
                case A_CROSSCOLOR+1000: case A_CROSSCOLOR: {
                    int[] cols={0x00FF66,0xFF3333,0x33AAFF,0xFFFF00,0xFFFFFF,0xFF00FF};
                    int i=0;for(int k=0;k<cols.length;k++)if(cols[k]==v.crosshairColor)i=k;
                    v.crosshairColor=cols[(i+(a==A_CROSSCOLOR?1:cols.length-1))%cols.length];v.savePrefs();break;
                }
                case A_CROSSSIZE_MINUS+1000: v.crosshairSize=Math.max(3,v.crosshairSize-2);v.savePrefs();break;
                case A_CROSSSIZE_MINUS: v.crosshairSize=Math.min(24,v.crosshairSize+2);v.savePrefs();break;
                case A_BHOP+1000: case A_BHOP: v.bhopEnabled=!v.bhopEnabled;v.savePrefs();break;
                case A_BHOPMULT+1000: case A_BHOPMULT: {
                    float[] m={1.2f,1.3f,1.5f};int i=0;
                    for(int k=0;k<m.length;k++)if(m[k]==v.bhopMult)i=k;
                    v.bhopMult=m[(i+(a==A_BHOPMULT?1:m.length-1))%m.length];v.savePrefs();break;
                }
                case A_INVERT+1000: case A_INVERT: v.invertY=!v.invertY;v.savePrefs();break;
                case A_QUALITY+1000: case A_QUALITY: v.quality=(v.quality+(a==A_QUALITY?1:2))%3;v.applyQuality();v.savePrefs();break;
                case A_EFFECT+1000: case A_EFFECT: v.effect=(v.effect+(a==A_EFFECT?1:GameData.EFFECT_COUNT-1))%GameData.EFFECT_COUNT;v.savePrefs();break;
                case A_FPS+1000: case A_FPS: v.showFps=!v.showFps;v.savePrefs();break;
                case A_DIFF+1000: case A_DIFF: v.difficulty=(v.difficulty+(a==A_DIFF?1:2))%3;v.savePrefs();break;
            }
            return;
        }
    }

    public static boolean handleGameButton(GameView v,float x,float y,int w,int h){
        for(GameView.Btn b:v.buttons){
            if(!b.contains(x,y)) continue;
            switch(b.action){
                case G_FIRE: v.firing=true;return true;
                case G_JUMP: v.wantJump=true;return true;
                case G_CROUCH: v.wantCrouch=!v.wantCrouch;return true;
                case G_RELOAD: v.wantReload=true;return true;
                case G_PLANT: v.wantPlant=true;return true;
                case G_SWITCH: {
                    v.player.weapon=(v.player.weapon+1)%5;
                    v.player.skin=v.skinPerWeapon[v.player.weapon];
                    return true;
                }
                case G_PAUSE: v.leaveLan(); v.screen=GameView.S_MENU;v.firing=false;return true;
            }
        }
        return false;
    }
}
