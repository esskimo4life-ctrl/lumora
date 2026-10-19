package dev.aetheria.client;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
public final class AetheriaScreen extends Screen {
 private record Hit(int x,int y,int w,int h,int type,String id,int page,String slider){}
 private final List<Hit> hits=new ArrayList<>();private int page;private Hit drag;private double scroll;
 private static final String[] PAGES={"Overview","Visuals","HUD","Performance","Style"};
 private AetheriaScreen(){super(Component.literal("Aetheria Visuals"));}
 private void hit(int x,int y,int w,int h,int type,String id,int p,String s){hits.add(new Hit(x,y,w,h,type,id,p,s));}
 private boolean inside(double mx,double my,int x,int y,int w,int h){return mx>=x&&my>=y&&mx<x+w&&my<y+h;}
 private void rect(GuiGraphicsExtractor g,int x,int y,int w,int h,int c){g.fill(x,y,x+w,y+h,c);}
 private void box(GuiGraphicsExtractor g,int x,int y,int w,int h,int c){rect(g,x+2,y+2,w,h,0x18000000);rect(g,x,y,w,h,c);rect(g,x+8,y,w-16,1,0x25FFFFFF);}
 private void txt(GuiGraphicsExtractor g,String s,int x,int y,int c){g.text(font,s,x,y,c,false);}
 private String fit(String s,int w){while(!s.isEmpty()&&font.width(s)>w)s=s.substring(0,s.length()-1);return s;}
 @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
 super.extractRenderState(g,mx,my,delta);hits.clear();g.fillGradient(0,0,width,height,0xE80D1018,0xF50B0D13);rect(g,0,0,width,2,AetheriaClient.accent());
 int pw=Math.max(205,Math.min(width-12,780)),ph=Math.max(170,Math.min(height-12,540)),x=(width-pw)/2,y=(height-ph)/2;boolean compact=width<540;rect(g,x,y,pw,ph,0xF0181D28);rect(g,x+1,y+1,pw-2,1,0x283B4355);
 rect(g,x+14,y+15,27,27,0xFF303247);rect(g,x+20,y+21,15,15,AetheriaClient.accent());txt(g,"Aetheria",x+50,y+17,0xFFF7F5FC);txt(g,"A VISUAL EXPERIENCE",x+50,y+33,0xFF929BAF);txt(g,"AETHERIA",x+pw-82,y+23,AetheriaClient.accent());
 int left=compact?0:155;
 if(compact){int nw=(pw-16)/PAGES.length;for(int i=0;i<PAGES.length;i++){int nx=x+8+i*nw;if(page==i)rect(g,nx,y+60,nw-2,20,0xFF303044);txt(g,PAGES[i],nx+1,y+67,page==i?0xFFFFFFFF:0xFF929BAF);hit(nx,y+58,nw-2,24,0,"",i,"");}}
 else {rect(g,x+left,y+55,1,ph-56,0x203F4658);for(int i=0;i<PAGES.length;i++){int ny=y+80+i*39;if(page==i)rect(g,x+10,ny,left-20,31,0xFF2B2D40);txt(g,(page==i?"◆ ":"◇ ")+PAGES[i],x+22,ny+10,page==i?AetheriaClient.accent():0xFF929BAF);hit(x+9,ny,left-18,32,0,"",i,"");}txt(g,"CLIENT-SIDE",x+17,y+ph-35,0xFFA7B1C4);}
 int cx=x+left+(compact?9:17),right=x+pw-13,cy=y+(compact?92:65),bottom=y+ph-12;
 if(page==0){box(g,cx,cy,right-cx,70,0xFF222637);txt(g,"Make the game feel like yours.",cx+13,cy+14,0xFFF7F4FE);txt(g,"Precise visuals, tuned to your screen.",cx+13,cy+32,0xFFADB4C5);txt(g,"15 MODULES  ·  CLIENT SIDE  ·  LIGHTWEIGHT",cx+13,cy+51,AetheriaClient.accent());txt(g,"LIVE",cx,cy+85,0xFF929BAF);txt(g,AetheriaClient.fps+" FPS     "+AetheriaClient.ping+"     "+AetheriaClient.paletteName(),cx,cy+104,0xFFF7F4FE);int yy=cy+126;for(AetheriaClient.Feature f:AetheriaClient.FEATURES){if(yy>bottom-20)break;txt(g,(AetheriaClient.on(f.id())?"● ":"○ ")+f.title(),cx+2,yy,AetheriaClient.on(f.id())?AetheriaClient.accent():0xFFD6D9E4);hit(cx,yy-3,right-cx,17,1,f.id(),0,"");yy+=19;}}
 else if(page==4){txt(g,"PERSONALISE",cx,cy,0xFFF5F2FC);txt(g,"Accent palette",cx,cy+23,0xFFF1EEF8);txt(g,AetheriaClient.paletteName()+"  ›",right-95,cy+23,AetheriaClient.accent());hit(cx,cy+15,right-cx,24,2,"",0,"");String[] labels={"Particle density","Panel opacity","Corner softness","Zoom field of view"};String[] ids={"density","opacity","rounding","zoom"};for(int i=0;i<4;i++){int sy=cy+52+i*66;box(g,cx,sy,right-cx,56,0xFF202633);txt(g,labels[i],cx+10,sy+7,0xFFF1EEF8);txt(g,AetheriaClient.valueText(ids[i]),right-80,sy+7,AetheriaClient.accent());int tx=cx+10,tw=right-cx-20,ty=sy+39;rect(g,tx,ty,tw,4,0xFF3B4352);int fw=(int)(tw*AetheriaClient.value(ids[i]));rect(g,tx,ty,Math.max(4,fw),4,AetheriaClient.accent());hit(tx,ty-6,tw,16,3,"",0,ids[i]);}}
 else {String group=PAGES[page];txt(g,group.toUpperCase(),cx,cy,0xFFF5F2FC);txt(g,"Tap a card to toggle. Settings save automatically.",cx,cy+17,0xFF858FA4);int sy=cy+36,cardH=52,gap=6,w=right-cx,cols=w>460?2:1,cw=(w-gap*(cols-1))/cols;List<AetheriaClient.Feature> list=new ArrayList<>();for(var f:AetheriaClient.FEATURES)if(f.group().equals(group))list.add(f);int total=(list.size()+cols-1)/cols*(cardH+gap),view=bottom-sy;scroll=Math.max(0,Math.min(scroll,Math.max(0,total-view)));g.enableScissor(cx,sy,right,bottom);for(int i=0;i<list.size();i++){var f=list.get(i);int xx=cx+(i%cols)*(cw+gap),yy=sy+(i/cols)*(cardH+gap)-(int)scroll;if(yy+cardH<sy||yy>bottom)continue;boolean on=AetheriaClient.on(f.id());rect(g,xx,yy,cw,cardH,on?0xFF252A3A:0xFF1B212C);rect(g,xx+9,yy+10,5,5,on?AetheriaClient.accent():0xFF525C6F);txt(g,fit(f.title(),cw-25),xx+20,yy+8,on?0xFFF8F5FD:0xFFD6D9E4);txt(g,fit(f.desc(),cw-18),xx+10,yy+25,0xFF8F99AD);txt(g,on?"ON":"OFF",xx+10,yy+39,on?AetheriaClient.accent():0xFF7F899B);hit(xx,yy,cw,cardH,1,f.id(),0,"");}g.disableScissor();}
 txt(g,"RIGHT SHIFT  /  CLOSE",x+pw-143,y+ph-11,0xFF697488);
 }
 @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubled){for(int i=hits.size()-1;i>=0;i--){Hit h=hits.get(i);if(!inside(e.x(),e.y(),h.x(),h.y(),h.w(),h.h()))continue;if(h.type()==0){page=h.page();scroll=0;return true;}if(h.type()==1){AetheriaClient.toggle(h.id());return true;}if(h.type()==2){AetheriaClient.nextPalette();return true;}if(h.type()==3){drag=h;AetheriaClient.slider(h.slider(),(e.x()-h.x())/Math.max(1,h.w()));return true;}}return super.mouseClicked(e,doubled);}
 @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){if(drag!=null){AetheriaClient.slider(drag.slider(),(e.x()-drag.x())/Math.max(1,drag.w()));return true;}return super.mouseDragged(e,dx,dy);}
 @Override public boolean mouseReleased(MouseButtonEvent e){boolean b=drag!=null;drag=null;if(b){AetheriaClient.save();return true;}return super.mouseReleased(e);}
 @Override public boolean mouseScrolled(double x,double y,double sx,double sy){scroll=Math.max(0,scroll-sy*24);return true;}
 @Override public boolean keyPressed(KeyEvent e){if(e.key()==InputConstants.KEY_ESCAPE||e.key()==InputConstants.KEY_RIGHT_SHIFT){onClose();return true;}return super.keyPressed(e);}
 @Override public void onClose(){AetheriaClient.save();super.onClose();}
}