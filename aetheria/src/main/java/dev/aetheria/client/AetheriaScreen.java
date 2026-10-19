package dev.aetheria.client;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Compact two-column visual menu, inspired by the clean toggle grid used by modern PvP visual clients. */
public final class AetheriaScreen extends Screen {
    private record Hit(int x, int y, int w, int h, int type, String id, int tab, String slider) {}
    private static final String[] TABS = {"Visuals", "HUD", "Utility", "Style"};
    private static final int NAV = 0, FEATURE = 1, PALETTE = 2, SLIDER = 3;
    private final List<Hit> hits = new ArrayList<>();
    private int tab;
    private Hit dragging;
    private double scroll;

    public AetheriaScreen() { super(Component.literal("Aetheria Visuals")); }

    private void hit(int x,int y,int w,int h,int type,String id,int toTab,String slider) {
        hits.add(new Hit(x,y,w,h,type,id,toTab,slider));
    }
    private static boolean inside(double mx,double my,int x,int y,int w,int h) {
        return mx>=x && my>=y && mx<x+w && my<y+h;
    }
    private void rect(GuiGraphicsExtractor g,int x,int y,int w,int h,int c) {
        if(w>0&&h>0)g.fill(x,y,x+w,y+h,c);
    }
    private void rounded(GuiGraphicsExtractor g,int x,int y,int w,int h,int radius,int color) {
        if(w<=0||h<=0)return;
        int r=Math.max(0,Math.min(radius,Math.min(w,h)/2));
        if(r==0){rect(g,x,y,w,h,color);return;}
        for(int i=0;i<r;i++){
            double dy=r-i-0.5;
            int inset=Math.max(0,r-(int)Math.round(Math.sqrt(r*r-dy*dy)));
            rect(g,x+inset,y+i,w-2*inset,1,color);
            rect(g,x+inset,y+h-1-i,w-2*inset,1,color);
        }
        rect(g,x,y+r,w,h-2*r,color);
    }
    private void text(GuiGraphicsExtractor g,String value,int x,int y,int color) {
        g.text(font,value,x,y,color,false);
    }
    private String fit(String value,int maxWidth) {
        if(font.width(value)<=maxWidth)return value;
        while(!value.isEmpty()&&font.width(value+"…")>maxWidth)value=value.substring(0,value.length()-1);
        return value.isEmpty()?"":value+"…";
    }
    private void toggle(GuiGraphicsExtractor g,int x,int y,boolean on) {
        int w=31,h=17;
        rounded(g,x,y,w,h,9,on?0xFF5D9CF6:0xFF484A4F);
        rounded(g,x+(on?16:2),y+2,13,13,7,0xFFF4F4F4);
    }
    private void panel(GuiGraphicsExtractor g,int x,int y,int w,int h) {
        rounded(g,x,y,w,h,13,0xF01D1D1F);
        rounded(g,x+1,y+1,w-2,h-2,12,0xFF242426);
        rect(g,x+15,y,w-30,1,0x13FFFFFF);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta) {
        super.extractRenderState(g,mouseX,mouseY,delta);
        hits.clear();
        g.fillGradient(0,0,width,height,0x9A08090B,0xD608090B);

        int pw=Math.max(220,Math.min(width-12,760));
        int ph=Math.max(185,Math.min(height-12,510));
        int x=(width-pw)/2,y=(height-ph)/2;
        boolean compact=width<520;
        panel(g,x,y,pw,ph);

        rounded(g,x+16,y+13,26,26,8,0xFF38383B);
        text(g,"A",x+25,y+20,AetheriaClient.accent());
        text(g,"Aetheria",x+51,y+15,0xFFF2F2F2);
        text(g,"VISUALS",x+51,y+29,0xFF89898E);
        text(g,"26.1.2",x+pw-51,y+22,0xFF88888D);

        int navY=y+49;
        int navX=x+14;
        int navW=(pw-28)/TABS.length;
        for(int i=0;i<TABS.length;i++){
            int nx=navX+i*navW;
            boolean active=tab==i;
            if(active)rounded(g,nx,navY,navW-4,25,7,0xFF3A3A3D);
            text(g,TABS[i],nx+(navW-4-font.width(TABS[i]))/2,navY+8,
                active?0xFFF4F4F5:0xFF929297);
            hit(nx,navY,navW-4,25,NAV,"",i,"");
        }
        rect(g,x+14,navY+33,pw-28,1,0x17FFFFFF);

        int cx=x+16, right=x+pw-16, contentY=navY+43, bottom=y+ph-24;
        if(tab==3)drawStyle(g,cx,contentY,right,bottom,mouseX,mouseY);
        else drawFeatureGrid(g,cx,contentY,right,bottom,mouseX,mouseY,compact);
        text(g,"ESC  ·  CLOSE",x+pw-131,y+ph-14,0xFF77777C);
    }

    private void drawFeatureGrid(GuiGraphicsExtractor g,int x1,int y1,int x2,int y2,int mx,int my,boolean compact) {
        String group=TABS[tab];
        String subtitle=switch(group){
            case "Visuals" -> "Fine-tune the look and feel";
            case "HUD" -> "Choose the information shown in game";
            default -> "Keep visuals smooth on lighter devices";
        };
        text(g,group.toUpperCase(),x1+2,y1+2,0xFFF0F0F2);
        text(g,subtitle,x1+2,y1+16,0xFF8B8B91);

        int contentY=y1+32;
        List<AetheriaClient.Feature> items=new ArrayList<>();
        for(AetheriaClient.Feature f:AetheriaClient.FEATURES){
            boolean match=tab==0?f.group().equals("Visuals"):tab==1?f.group().equals("HUD"):f.group().equals("Utility");
            if(match)items.add(f);
        }
        if(items.isEmpty()){
            text(g,"No options in this section.",x1+4,contentY+10,0xFF88888E);
            return;
        }
        int gap=7,cardH=39,avail=x2-x1;
        int cols=avail>=390?2:1;
        int cardW=(avail-gap*(cols-1))/cols;
        int rows=(items.size()+cols-1)/cols;
        int total=rows*(cardH+gap)-gap;
        int view=Math.max(1,y2-contentY);
        double maxScroll=Math.max(0,total-view);
        scroll=Math.max(0,Math.min(scroll,maxScroll));
        g.enableScissor(x1,contentY,x2,y2);
        for(int i=0;i<items.size();i++){
            AetheriaClient.Feature f=items.get(i);
            int cx=x1+(i%cols)*(cardW+gap),cy=contentY+(i/cols)*(cardH+gap)-(int)scroll;
            if(cy+cardH<contentY||cy>y2)continue;
            boolean on=AetheriaClient.on(f.id());
            boolean hover=inside(mx,my,cx,cy,cardW,cardH);
            int bg=hover?0xFF363638:0xFF2E2E30;
            rounded(g,cx,cy,cardW,cardH,8,bg);
            rounded(g,cx+1,cy+1,cardW-2,cardH-2,7,0xFF303032);
            text(g,fit(f.title(),Math.max(60,cardW-64)),cx+11,cy+7,on?0xFFF3F3F4:0xFFD0D0D2);
            text(g,fit(f.desc(),Math.max(55,cardW-64)),cx+11,cy+21,0xFF8C8C91);
            toggle(g,cx+cardW-43,cy+11,on);
            hit(cx,cy,cardW,cardH,FEATURE,f.id(),0,"");
        }
        g.disableScissor();
        if(maxScroll>0){
            int th=Math.max(18,view*view/Math.max(1,total));
            int ty=contentY+(int)((view-th)*scroll/maxScroll);
            rounded(g,x2-2,contentY,2,view,1,0x18FFFFFF);
            rounded(g,x2-2,ty,2,th,1,0xFF737378);
        }
    }

    private void drawStyle(GuiGraphicsExtractor g,int x1,int y1,int x2,int y2,int mx,int my) {
        text(g,"STYLE",x1+2,y1+2,0xFFF0F0F2);
        text(g,"Subtle accents, clean surfaces, your choice.",x1+2,y1+16,0xFF8B8B91);
        int sy=y1+34,w=x2-x1;
        rounded(g,x1,sy,w,40,8,0xFF303032);
        text(g,"Accent palette",x1+11,sy+8,0xFFF1F1F2);
        text(g,AetheriaClient.paletteName()+"  ›",x2-font.width(AetheriaClient.paletteName()+"  ›")-11,sy+15,AetheriaClient.accent());
        hit(x1,sy,w,40,PALETTE,"",0,"");
        String[] labels={"Particle amount","Panel opacity","Corner radius","Zoom field of view"};
        String[] ids={"density","opacity","rounding","zoom"};
        for(int i=0;i<ids.length;i++){
            int rowY=sy+48+i*55;
            if(rowY>y2)continue;
            rounded(g,x1,rowY,w,49,8,0xFF303032);
            text(g,labels[i],x1+11,rowY+7,0xFFF0F0F2);
            String v=AetheriaClient.valueText(ids[i]);
            text(g,v,x2-font.width(v)-11,rowY+7,AetheriaClient.accent());
            int tx=x1+11,tw=w-22,ty=rowY+34;
            rounded(g,tx,ty,tw,4,2,0xFF4A4A4D);
            int fill=Math.max(4,(int)(tw*AetheriaClient.value(ids[i])));
            rounded(g,tx,ty,fill,4,2,0xFF5D9CF6);
            rounded(g,tx+fill-4,ty-2,8,8,4,0xFFF4F4F4);
            hit(tx,ty-6,tw,16,SLIDER,"",0,ids[i]);
        }
    }

    private void setSlider(Hit h,double mouseX){AetheriaClient.slider(h.slider(),(mouseX-h.x())/Math.max(1,h.w()));}
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubled){
        for(int i=hits.size()-1;i>=0;i--){Hit h=hits.get(i);if(!inside(event.x(),event.y(),h.x(),h.y(),h.w(),h.h()))continue;
            switch(h.type()){
                case NAV->{tab=h.tab();scroll=0;return true;}
                case FEATURE->{AetheriaClient.toggle(h.id());return true;}
                case PALETTE->{AetheriaClient.nextPalette();return true;}
                case SLIDER->{dragging=h;setSlider(h,event.x());return true;}
                default->{}
            }
        }
        return super.mouseClicked(event,doubled);
    }
    @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy){
        if(dragging!=null){setSlider(dragging,event.x());return true;}return super.mouseDragged(event,dx,dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent event){
        boolean used=dragging!=null;dragging=null;
        if(used){AetheriaClient.save();return true;}return super.mouseReleased(event);
    }
    @Override public boolean mouseScrolled(double x,double y,double sx,double sy){scroll=Math.max(0,scroll-sy*25);return true;}
    @Override public boolean keyPressed(KeyEvent event){
        if(event.key()==InputConstants.KEY_ESCAPE){onClose();return true;}
        return super.keyPressed(event);
    }
    @Override public void onClose(){AetheriaClient.save();super.onClose();}
}