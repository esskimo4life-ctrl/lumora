package dev.aetheria.client;
import com.mojang.blaze3d.platform.InputConstants;
import java.io.*; import java.nio.file.*; import java.time.LocalTime; import java.util.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.*; import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier; import net.minecraft.world.entity.LivingEntity;
public final class AetheriaClient implements ClientModInitializer {
 public record Feature(String id,String title,String desc,String group,boolean def){}
 public static final Feature[] FEATURES={
 new Feature("infobar","Live status bar","FPS, latency and session clock","HUD",true),
 new Feature("coordinates","Coordinates","Position, direction and speed","HUD",true),
 new Feature("keystrokes","Keystrokes","Animated WASD and mouse buttons","HUD",false),
 new Feature("armor","Equipment status","Armor and held-item durability","HUD",true),
 new Feature("potions","Effect list","Active effects with timers","HUD",true),
 new Feature("target","Target card","Name, distance and health","HUD",true),
 new Feature("compass","Compass strip","Heading and degrees","HUD",false),
 new Feature("fpsgraph","Frame-time graph","Recent frame pacing","HUD",false),
 new Feature("particles","Ambient particles","Soft drifting motes","Visuals",false),
 new Feature("hitmarker","Hit feedback","Short marker on confirmed damage","Visuals",true),
 new Feature("reticle","Accent reticle","Minimal accent crosshair","Visuals",false),
 new Feature("damageTint","Damage tint","Subtle low-health screen edges","Visuals",true),
 new Feature("fullbright","Full brightness","Raise client brightness while enabled","Visuals",false),
 new Feature("zoom","Smooth zoom","Hold C for configurable zoom","Visuals",false),
 new Feature("eco","Low-effects mode","Reduce particle count for FPS/battery","Performance",false)};
 private static final Path CONFIG=FabricLoader.getInstance().getConfigDir().resolve("aetheria.properties");
 private static final int[] COLORS={0xFFBBA8FF,0xFF9AD9F2,0xFFFFB6A8,0xFF9ADAB8,0xFFF0CF8D,0xFFF1A7CC};
 private static final String[] COLOR_NAMES={"Lavender","Glacier","Peach","Sage","Honey","Rose"};
 private static final Map<String,Boolean> enabled=new LinkedHashMap<>();
 public static int palette=0,fps=0; public static String ping="—",clock="00:00";
 public static double density=18,opacity=76,rounding=12,zoomFov=38,speed=0;
 public static KeyMapping MENU,ZOOM;
 private static Double gamma=null; private static Integer fov=null;
 private static long lastFrame,lastHit; private static int targetId=-1,targetHurt,frameIndex;
 private static final float[] frames=new float[48],px=new float[64],py=new float[64],vx=new float[64],vy=new float[64],phase=new float[64];
 private static final Random random=new Random(431);
 private static double lx,lz; private static boolean hasPos,particlesReady; private static int ticks;
 static {for(Feature f:FEATURES)enabled.put(f.id(),f.def());}
 public void onInitializeClient(){
  load(); var category=KeyMapping.Category.register(Identifier.fromNamespaceAndPath("aetheria","main"));
  MENU=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.aetheria.menu",InputConstants.Type.KEYSYM,InputConstants.KEY_RIGHT_SHIFT,category));
  ZOOM=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.aetheria.zoom",InputConstants.Type.KEYSYM,InputConstants.KEY_C,category));
  ClientTickEvents.END_CLIENT_TICK.register(AetheriaClient::tick);
  HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("aetheria","hud"),(g,d)->render(g));
  ClientLifecycleEvents.CLIENT_STOPPING.register(mc->{restore(mc);save();});
 }
 public static boolean on(String id){return enabled.getOrDefault(id,false);}
 public static void toggle(String id){if(enabled.containsKey(id)){enabled.put(id,!on(id));save();}}
 public static int accent(){return COLORS[Math.floorMod(palette,COLORS.length)];}
 public static String paletteName(){return COLOR_NAMES[Math.floorMod(palette,COLOR_NAMES.length)];}
 public static void nextPalette(){palette=(palette+1)%COLORS.length;save();}
 public static double value(String k){return switch(k){case "density"->(density-6)/38;case "opacity"->(opacity-48)/46;case "rounding"->(rounding-5)/13;default->(zoomFov-25)/45;};}
 public static String valueText(String k){return switch(k){case "density"->Math.round(density)+" motes";case "opacity"->Math.round(opacity)+"%";case "rounding"->Math.round(rounding)+" px";default->Math.round(zoomFov)+"°";};}
 public static void slider(String k,double t){t=Math.max(0,Math.min(1,t));switch(k){case "density"->density=6+t*38;case "opacity"->opacity=48+t*46;case "rounding"->rounding=5+t*13;case "zoom"->zoomFov=25+t*45;default->{return;}}save();}
 private static void tick(Minecraft mc){
  while(MENU.consumeClick())if(mc.player!=null&&mc.screen==null)mc.setScreen(new AetheriaScreen());
  if(mc.player==null||mc.options==null){restore(mc);hasPos=false;return;}
  if(on("fullbright")){if(gamma==null)gamma=mc.options.gamma().get();mc.options.gamma().set(1.0);}else restoreGamma(mc);
  if(on("zoom")&&mc.screen==null&&ZOOM.isDown()){if(fov==null)fov=mc.options.fov().get();int old=mc.options.fov().get(),want=(int)Math.round(zoomFov);mc.options.fov().set(old+(int)Math.round((want-old)*.28));}else restoreFov(mc);
  double x=mc.player.getX(),z=mc.player.getZ();if(hasPos)speed+=(Math.hypot(x-lx,z-lz)*20-speed)*.35;lx=x;lz=z;hasPos=true;
  if(++ticks%10==0){var conn=mc.getConnection();var info=conn==null?null:conn.getPlayerInfo(mc.player.getUUID());ping=conn==null?"SP":info==null?"—":Math.max(0,info.getLatency())+" ms";LocalTime t=LocalTime.now();clock=String.format("%02d:%02d",t.getHour(),t.getMinute());}
  if(mc.crosshairPickEntity instanceof LivingEntity e&&e.getId()==targetId&&e.hurtTime>targetHurt)lastHit=System.currentTimeMillis();
  if(mc.crosshairPickEntity instanceof LivingEntity e){targetId=e.getId();targetHurt=e.hurtTime;}else{targetId=-1;targetHurt=0;}
 }
 private static void restore(Minecraft mc){restoreGamma(mc);restoreFov(mc);}
 private static void restoreGamma(Minecraft mc){if(gamma!=null&&mc.options!=null){mc.options.gamma().set(gamma);gamma=null;}}
 private static void restoreFov(Minecraft mc){if(fov!=null&&mc.options!=null){mc.options.fov().set(fov);fov=null;}}
 private static int panel(){return ((int)(opacity*2.55)<<24)|0x00131923;}
 private static void rect(GuiGraphicsExtractor g,int x,int y,int w,int h,int c){if(w>0&&h>0)g.fill(x,y,x+w,y+h,c);}
 private static void box(GuiGraphicsExtractor g,int x,int y,int w,int h){
  rect(g,x+2,y+2,w,h,0x18000000);rect(g,x,y,w,h,panel());rect(g,x+8,y,w-16,1,0x22FFFFFF);
 }
 private static void txt(GuiGraphicsExtractor g,Minecraft m,String s,int x,int y,int c){g.text(m.font,s,x,y,c,false);}
 private static String fit(Minecraft m,String s,int w){while(!s.isEmpty()&&m.font.width(s)>w)s=s.substring(0,s.length()-1);return s;}
 private static void render(GuiGraphicsExtractor g){
  Minecraft m=Minecraft.getInstance();if(m.player==null||m.level==null||m.screen!=null||m.options.hideGui)return;
  long now=System.nanoTime();float dt=lastFrame==0?.016f:Math.min(.06f,(now-lastFrame)/1e9f);lastFrame=now;fps=(int)(1/Math.max(.001f,dt));frames[frameIndex++%frames.length]=dt*1000;
  int sw=m.getWindow().getGuiScaledWidth(),sh=m.getWindow().getGuiScaledHeight();
  if(!particlesReady){for(int i=0;i<px.length;i++)spawn(i,sw,sh);particlesReady=true;}
  if(on("damageTint")&&m.player.getHealth()<9){int a=(int)((9-m.player.getHealth())/9*75);g.fillGradient(0,0,sw,Math.max(10,sh/9),(a<<24)|0x00E85D70,0x00E85D70);g.fillGradient(0,sh-Math.max(10,sh/9),sw,sh,0x00E85D70,(a<<24)|0x00E85D70);}
  if(on("particles"))particles(g,sw,sh,dt);
  if(on("reticle")){int cx=sw/2,cy=sh/2,c=accent();rect(g,cx-7,cy,4,1,c);rect(g,cx+4,cy,4,1,c);rect(g,cx,cy-7,1,4,c);rect(g,cx,cy+4,1,4,c);}
  if(on("hitmarker")&&lastHit>0&&now/1_000_000-lastHit<260){int d=5,c=accent(),cx=sw/2,cy=sh/2;rect(g,cx-d-2,cy-d-2,2,2,c);rect(g,cx+d,cy-d-2,2,2,c);rect(g,cx-d-2,cy+d,2,2,c);rect(g,cx+d,cy+d,2,2,c);}
  if(on("infobar")){int x=sw/2-106;box(g,x,7,212,21);txt(g,m,"FPS  "+fps,x+9,14,0xFFF7F4FF);txt(g,m,"PING  "+ping,x+65,14,0xFFADB5C6);txt(g,m,clock,x+165,14,accent());}
  if(on("coordinates")){String a=(int)Math.floor(m.player.getX())+" / "+(int)Math.floor(m.player.getY())+" / "+(int)Math.floor(m.player.getZ());String b=String.format("%.1f b/s",speed);box(g,8,sh-40,Math.max(m.font.width(a),m.font.width(b))+18,34);txt(g,m,a,17,sh-33,0xFFF7F4FF);txt(g,m,b,17,sh-20,0xFFADB5C6);}
  if(on("keystrokes")){int x=9,y=sh-72,k=18;key(g,m,x+k+2,y,k,"W",m.options.keyUp.isDown());key(g,m,x,y+k+2,k,"A",m.options.keyLeft.isDown());key(g,m,x+k+2,y+k+2,k,"S",m.options.keyDown.isDown());key(g,m,x+2*(k+2),y+k+2,k,"D",m.options.keyRight.isDown());}
  if(on("armor")){var stack=m.player.getMainHandItem();String s="ARMOR "+m.player.getArmorValue();if(stack.isDamageableItem())s+="  ·  "+Math.round(100.0*(stack.getMaxDamage()-stack.getDamageValue())/Math.max(1,stack.getMaxDamage()))+"%";int w=m.font.width(s)+18;box(g,(sw-w)/2,sh-27,w,19);txt(g,m,s,(sw-m.font.width(s))/2,sh-21,0xFFF1EDFA);}
  if(on("potions")){int row=0;for(var e:m.player.getActiveEffects()){if(row>=5)break;String n=e.getEffect().value().getDisplayName().getString();int sec=e.isInfiniteDuration()?-1:e.getDuration()/20;String tm=sec<0?"∞":sec/60+":"+String.format("%02d",sec%60);if(row==0)box(g,8,34,150,Math.min(75,m.player.getActiveEffects().size()*13+8));txt(g,m,fit(m,n,94),15,40+row*13,0xFFDDE2EF);txt(g,m,tm,119,40+row*13,accent());row++;}}
  if(on("target")&&m.crosshairPickEntity instanceof LivingEntity e&&e!=m.player){String n=e.getName().getString();int w=Math.max(130,m.font.width(n)+76),x=(sw-w)/2,y=sh/2+19;box(g,x,y,w,37);txt(g,m,fit(m,n,w-16),x+8,y+7,0xFFF7F4FF);txt(g,m,String.format("%.1f HP  ·  %.1f m",e.getHealth(),m.player.distanceTo(e)),x+8,y+20,accent());rect(g,x+8,y+31,w-16,2,0x44FFFFFF);rect(g,x+8,y+31,(int)((w-16)*Math.max(0,e.getHealth()/Math.max(1,e.getMaxHealth()))),2,accent());}
  if(on("compass")){String s="N  ·  "+Math.round((m.player.getYRot()%360+360)%360)+"°";box(g,sw/2-42,33,84,18);txt(g,m,s,sw/2-25,39,accent());}
  if(on("fpsgraph")){int x=sw-104,y=8;box(g,x,y,96,38);txt(g,m,"FRAME TIME",x+5,y+4,0xFFADB5C6);for(int i=0;i<40;i++){float v=frames[Math.floorMod(frameIndex-40+i,frames.length)];int h=Math.max(1,Math.min(17,(int)(v*.5)));rect(g,x+5+i*2,y+33-h,1,h,v<16.7?accent():0xFFF0CF8D);}}
 }
 private static void key(GuiGraphicsExtractor g,Minecraft m,int x,int y,int w,String s,boolean down){rect(g,x,y,w,18,down?accent():panel());txt(g,m,s,x+(w-m.font.width(s))/2,y+5,down?0xFFFFFFFF:0xFFADB5C6);}
 private static void spawn(int i,int w,int h){px[i]=random.nextFloat()*Math.max(1,w);py[i]=random.nextFloat()*Math.max(1,h);phase[i]=random.nextFloat()*6.28f;vx[i]=random.nextFloat()*10-5;vy[i]=8+random.nextFloat()*14;}
 private static void particles(GuiGraphicsExtractor g,int w,int h,float dt){int count=(int)(density*(on("eco")?.3:1));float t=System.nanoTime()/1e9f;for(int i=0;i<Math.min(count,px.length);i++){px[i]+=(vx[i]+(float)Math.sin(t+phase[i])*4)*dt;py[i]+=vy[i]*dt;if(py[i]>h||px[i]<0||px[i]>w)spawn(i,w,h);int a=(int)(80+55*Math.sin(t+phase[i]));rect(g,(int)px[i],(int)py[i],i%7==0?3:2,i%7==0?3:2,(a<<24)|(accent()&0xffffff));}}
 private static void load(){if(!Files.exists(CONFIG))return;Properties p=new Properties();try(InputStream in=Files.newInputStream(CONFIG)){p.load(in);for(Feature f:FEATURES)if(p.containsKey("f."+f.id()))enabled.put(f.id(),Boolean.parseBoolean(p.getProperty("f."+f.id())));palette=Integer.parseInt(p.getProperty("palette","0"));density=Double.parseDouble(p.getProperty("density","18"));opacity=Double.parseDouble(p.getProperty("opacity","76"));rounding=Double.parseDouble(p.getProperty("rounding","12"));zoomFov=Double.parseDouble(p.getProperty("zoom","38"));}catch(Exception ignored){}}
 public static void save(){Properties p=new Properties();for(Feature f:FEATURES)p.setProperty("f."+f.id(),Boolean.toString(on(f.id())));p.setProperty("palette",Integer.toString(palette));p.setProperty("density",Double.toString(density));p.setProperty("opacity",Double.toString(opacity));p.setProperty("rounding",Double.toString(rounding));p.setProperty("zoom",Double.toString(zoomFov));try{Files.createDirectories(CONFIG.getParent());try(OutputStream o=Files.newOutputStream(CONFIG)){p.store(o,"Aetheria Visuals");}}catch(IOException ignored){}}
}