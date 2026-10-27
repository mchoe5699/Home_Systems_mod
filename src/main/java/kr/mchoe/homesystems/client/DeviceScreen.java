package kr.mchoe.homesystems.client;

import java.util.*;
import kr.mchoe.homesystems.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** The product's own face is the control surface; administrative settings are a separate page. */
public final class DeviceScreen extends AbstractContainerScreen<DeviceMenu> {
    private final CameraView camera=new CameraView();
    private final List<AbstractWidget> faceControls=new ArrayList<>(),settingsControls=new ArrayList<>();
    private final List<EditBox> fields=new ArrayList<>();
    private final Map<DeviceNetwork.Action,List<AbstractWidget>> actions=new EnumMap<>(DeviceNetwork.Action.class);
    private EditBox building,unit,guard,ownGuard,ownBuilding,ownUnit;
    private Button settings,back,save,star,bus,chat;
    private AbstractWidget draggedShtSlider;
    private String loadedAddress="";
    private ProductDesign design;private boolean settingsMode;private int px,py,pw,ph;
    public DeviceScreen(DeviceMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=400;imageHeight=234;}
    private static Component tr(String key){return Component.translatable("screen.homesystems."+key);}
    public CameraView camera(){return camera;}
    private int x(int value){return leftPos+px+value*pw/256;}private int y(int value){return topPos+py+value*ph/256;}
    private int w(int value){return Math.max(9,value*pw/256);}private int h(int value){return Math.max(10,value*ph/256);}
    private void send(DeviceNetwork.Action action){DeviceNetwork.CHANNEL.sendToServer(new DeviceNetwork.Request(menu.containerId,action,value(building),value(unit),value(guard)));}
    private static int value(EditBox field){try{return Integer.parseInt(field.getValue());}catch(Exception e){return -1;}}
    private EditBox numeric(String label,int x,int y,int width,List<AbstractWidget> group){
        var field=new EditBox(font,x,y,width,18,tr(label));field.setMaxLength(4);field.setFilter(v->v.matches("[0-9]{0,4}"));addRenderableWidget(field);fields.add(field);group.add(field);return field;
    }
    private void hotspot(String label,int x,int y,int width,int height,Runnable run,DeviceNetwork.Action action){
        var button=new AbstractButton(x(x),y(y),w(width),h(height),tr(label)){
            @Override public void onPress(){run.run();}
            @Override protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output){defaultButtonNarrationText(output);}
            @Override protected void renderWidget(GuiGraphics g,int mx,int my,float partial){
                if(isHoveredOrFocused()){g.fill(getX(),getY(),getX()+getWidth(),getY()+getHeight(),0x6686c8ef);g.renderOutline(getX(),getY(),getWidth(),getHeight(),0xffb9efff);}
            }
        };
        button.setTooltip(Tooltip.create(tr(label)));addRenderableWidget(button);faceControls.add(button);if(action!=null)actions.computeIfAbsent(action,a->new ArrayList<>()).add(button);
    }
    private void hot(String label,int x,int y,int width,int height,DeviceNetwork.Action action){hotspot(label,x,y,width,height,()->send(action),action);}
    /** Click or drag to a real switch position, rather than cycling a fake button. */
    private void shtSlider(String label,int x,int y,int width,int height,int control,boolean vertical,DeviceNetwork.Action action){
        var slider=new AbstractButton(x(x),y(y),w(width),h(height),tr(label)){
            private int sent=-1;
            private void position(double mx,double my){
                double fraction=vertical?(my-getY())/getHeight():(mx-getX())/getWidth();
                int value=Math.max(0,Math.min(2,(int)Math.floor(fraction*3)));
                if(vertical)value=2-value;
                if(value!=sent){sent=value;DeviceNetwork.CHANNEL.sendToServer(new DeviceNetwork.ShtSwitchRequest(menu.containerId,control,value));}
            }
            @Override public void onPress(){send(action);}
            @Override public boolean mouseClicked(double mx,double my,int button){
                if(button!=0||!active||!visible||!isMouseOver(mx,my))return false;
                draggedShtSlider=this;sent=-1;position(mx,my);playDownSound(minecraft.getSoundManager());return true;
            }
            @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy){
                if(button!=0||!active||!visible)return false;position(mx,my);return true;
            }
            @Override protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output){defaultButtonNarrationText(output);}
            @Override protected void renderWidget(GuiGraphics g,int mx,int my,float partial){
                if(isHoveredOrFocused())g.renderOutline(getX(),getY(),getWidth(),getHeight(),0xffb9efff);
            }
        };
        slider.setTooltip(Tooltip.create(tr(label)));addRenderableWidget(slider);faceControls.add(slider);
        actions.computeIfAbsent(action,a->new ArrayList<>()).add(slider);
    }
    private void screenButton(String label,int x,int y,int width,int height,DeviceNetwork.Action action){
        var button=new AbstractButton(x(x),y(y),w(width),h(height),tr(label)){
            @Override public void onPress(){send(action);}
            @Override protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output){defaultButtonNarrationText(output);}
            @Override protected void renderWidget(GuiGraphics g,int mx,int my,float partial){
                var device=menu.device();boolean network=action.ordinal()>=DeviceNetwork.Action.HOME_LIGHTS.ordinal()&&action.ordinal()<=DeviceNetwork.Action.HOME_OTHER.ordinal();
                boolean on=network&&device!=null&&device.homeNetwork().enabled(action.ordinal()-DeviceNetwork.Action.HOME_LIGHTS.ordinal());
                int color=!active?0xff25323b:isHoveredOrFocused()?0xff397b85:on?0xff327e72:0xff204c5e;
                g.fill(getX()+1,getY(),getX()+getWidth()-1,getY()+getHeight(),color);g.fill(getX(),getY()+1,getX()+getWidth(),getY()+getHeight()-1,color);
                String shortKey=switch(label){case "priority_guard"->"panel_guard";case "home_tab"->"panel_home";default->label;};
                var text=tr(shortKey);if(network)text=text.copy().append(on?" ON":" OFF");float scale=Math.min(.85f,(getWidth()-4f)/Math.max(1,font.width(text)));
                g.pose().pushPose();g.pose().translate(getX()+getWidth()/2f,getY()+getHeight()/2f-4*scale,0);g.pose().scale(scale,scale,1);g.drawString(font,text,-font.width(text)/2,0,active?0xe0f0f5:0x788c98,false);g.pose().popPose();
            }
        };
        button.setTooltip(Tooltip.create(tr(label)));addRenderableWidget(button);faceControls.add(button);actions.computeIfAbsent(action,a->new ArrayList<>()).add(button);
    }
    @Override protected void init(){
        super.init();faceControls.clear();settingsControls.clear();fields.clear();actions.clear();var d=menu.device();design=d==null?ProductDesign.WALLPAD:ProductDesign.of(d.getBlockState());
        ph=178;pw=Math.min(350,Math.round(ph*design.width()/design.height()));px=(400-pw)/2;py=23;
        settings=addRenderableWidget(Button.builder(tr(design==ProductDesign.GUARD?"guard_settings":"address_settings"),b->{settingsMode=true;loadSettings();updateControls();}).bounds(leftPos+8,topPos+212,280,20).build());
        chat=addRenderableWidget(Button.builder(tr("chat"),b->{onClose();minecraft.setScreen(new ChatScreen(""));}).bounds(leftPos+294,topPos+212,98,20).build());
        ownGuard=numeric("assigned_guard",leftPos+24,topPos+72,100,settingsControls);ownBuilding=numeric("building",leftPos+150,topPos+72,100,settingsControls);ownUnit=numeric("unit",leftPos+276,topPos+72,100,settingsControls);
        save=Button.builder(tr("save_address"),b->DeviceNetwork.CHANNEL.sendToServer(new DeviceNetwork.AddressRequest(menu.containerId,value(ownGuard),design==ProductDesign.GUARD?0:value(ownBuilding),design==ProductDesign.GUARD?0:value(ownUnit)))).bounds(leftPos+24,topPos+102,352,20).build();settingsControls.add(addRenderableWidget(save));
        star=Button.builder(Component.literal("STAR"),b->send(DeviceNetwork.Action.TYPE_STAR)).bounds(leftPos+24,topPos+146,100,20).build();bus=Button.builder(Component.literal("BUS"),b->send(DeviceNetwork.Action.TYPE_BUS)).bounds(leftPos+136,topPos+146,100,20).build();settingsControls.add(addRenderableWidget(star));settingsControls.add(addRenderableWidget(bus));
        guard=numeric("dial_guard",leftPos+24,topPos+178,100,settingsControls);
        var other=Button.builder(tr("other_guard"),b->send(DeviceNetwork.Action.CALL_OTHER_GUARD)).bounds(leftPos+136,topPos+178,240,20).build();settingsControls.add(addRenderableWidget(other));actions.put(DeviceNetwork.Action.CALL_OTHER_GUARD,List.of(other));
        back=addRenderableWidget(Button.builder(tr("back"),b->{settingsMode=false;updateControls();}).bounds(leftPos+8,topPos+212,280,20).build());
        building=numeric("dial_building",x(design==ProductDesign.LOBBY?31:84),y(design==ProductDesign.LOBBY?58:62),w(design==ProductDesign.LOBBY?65:61),faceControls);
        unit=numeric("dial_unit",x(design==ProductDesign.LOBBY?31:154),y(design==ProductDesign.LOBBY?98:62),w(design==ProductDesign.LOBBY?65:65),faceControls);
        if(design==ProductDesign.LOBBY||design==ProductDesign.GUARD){
            for(int row=0;row<4;row++)for(int col=0;col<3;col++){
                String key=row==3?new String[]{"*","0","#"}[col]:Integer.toString(row*3+col+1);
                int kx=design==ProductDesign.LOBBY?151+col*24:77+col*24,ky=design==ProductDesign.LOBBY?88+row*24:143+row*20;
                hotspot("keypad",kx,ky,22,20,()->{EditBox target=building.isFocused()?building:unit;if(key.equals("*")){String v=target.getValue();target.setValue(v.isEmpty()?v:v.substring(0,v.length()-1));}else if(key.equals("#")){if(building.isFocused()){building.setFocused(false);unit.setFocused(true);}else send(DeviceNetwork.Action.CALL);}else if(target.getValue().length()<4)target.setValue(target.getValue()+key);},null);
            }
            if(design==ProductDesign.LOBBY)hot("call_home",181,187,47,54,DeviceNetwork.Action.CALL);
            else hot("unlock",171,166,50,50,DeviceNetwork.Action.UNLOCK);
            if(design==ProductDesign.LOBBY){screenButton("priority_guard",26,168,87,24,DeviceNetwork.Action.CALL_GUARD);screenButton("end",26,198,87,24,DeviceNetwork.Action.END);}
            else {screenButton("presence_0",79,96,49,23,DeviceNetwork.Action.GUARD_PRESENT);screenButton("presence_1",132,96,49,23,DeviceNetwork.Action.GUARD_AWAY);screenButton("presence_2",185,96,49,23,DeviceNetwork.Action.GUARD_OUT);}
        }
        if(design==ProductDesign.SHT308){hot("handset",0,0,60,250,DeviceNetwork.Action.HANDSET);hot("handset",-65,0,60,250,DeviceNetwork.Action.HANDSET);}
        else if(design.handset){hot("handset",design==ProductDesign.INTERPHONE?50:10,8,design==ProductDesign.INTERPHONE?151:48,design==ProductDesign.INTERPHONE?175:227,DeviceNetwork.Action.HANDSET);}
        if(design==ProductDesign.INTERPHONE){hot("priority_guard",79,184,100,24,DeviceNetwork.Action.CALL_GUARD);hot("unlock",79,211,100,28,DeviceNetwork.Action.UNLOCK);}
        if(design==ProductDesign.VIDEO){hot("monitor",85,187,27,36,DeviceNetwork.Action.MONITOR);hot("priority_guard",126,187,27,36,DeviceNetwork.Action.CALL_GUARD);hot("unlock",167,187,27,36,DeviceNetwork.Action.UNLOCK);hotspot("address_settings",208,187,27,36,()->{settingsMode=true;loadSettings();updateControls();},null);}
        if(design==ProductDesign.SHT308){
            hot("sht_emergency",224,134,20,21,DeviceNetwork.Action.SHT_EMERGENCY);
            hot("priority_guard",224,156,20,21,DeviceNetwork.Action.CALL_GUARD);
            hot("sht_away",224,178,20,21,DeviceNetwork.Action.SHT_AWAY);
            hot("sht_entrance",222,203,24,26,DeviceNetwork.Action.SHT_ENTRANCE);
            hot("sht_power",65,195,12,34,DeviceNetwork.Action.SHT_POWER);
            shtSlider("sht_volume",246,58,10,35,0,true,DeviceNetwork.Action.SHT_VOLUME);
            shtSlider("sht_brightness",140,247,25,7,1,false,DeviceNetwork.Action.SHT_BRIGHTNESS);
            shtSlider("sht_contrast",191,247,25,7,2,false,DeviceNetwork.Action.SHT_CONTRAST);
        }
        if(design==ProductDesign.WALLPAD){
            screenButton("monitor",30,135,63,31,DeviceNetwork.Action.MONITOR);screenButton("priority_guard",98,135,63,31,DeviceNetwork.Action.CALL_GUARD);screenButton("home_tab",166,135,63,31,DeviceNetwork.Action.PANEL_HOME);
            screenButton("answer",30,174,63,31,DeviceNetwork.Action.ANSWER);screenButton("unlock",98,174,63,31,DeviceNetwork.Action.UNLOCK);screenButton("end",166,174,63,31,DeviceNetwork.Action.END);
            var aa=new DeviceNetwork.Action[]{DeviceNetwork.Action.HOME_LIGHTS,DeviceNetwork.Action.HOME_DOOR,DeviceNetwork.Action.HOME_GAS,DeviceNetwork.Action.HOME_OTHER};
            for(int i=0;i<4;i++)screenButton("channel_"+new String[]{"lights","door","gas","other"}[i],30+(i%2)*103,55+(i/2)*44,92,34,aa[i]);
        }
        if(design==ProductDesign.CAMERA||design==ProductDesign.DOOR){screenButton("ring_doorbell",50,180,156,35,DeviceNetwork.Action.CALL);}
        loadSettings();if(d!=null&&d.addressConfigured()){building.setValue(d.buildingNumber()==9999?"":Integer.toString(d.buildingNumber()));unit.setValue(Integer.toString(d.unitNumber()));}
        for(var field:fields)field.setResponder(v->updateControls());updateControls();
    }
    private void loadSettings(){var d=menu.device();ownGuard.setValue(d!=null&&d.addressConfigured()?""+d.guardNumber():"");ownBuilding.setValue(d!=null&&d.addressConfigured()?""+d.buildingNumber():"");ownUnit.setValue(d!=null&&d.addressConfigured()?""+d.unitNumber():"");}
    private void updateControls(){
        if(settings==null)return;var d=menu.device();boolean present=d!=null&&minecraft.player!=null;if(!present)return;boolean owner=d.canConfigure(minecraft.player),idle=d.state()==0;
        String address=d.addressConfigured()+":"+d.buildingNumber()+":"+d.unitNumber();
        if(!address.equals(loadedAddress)){loadedAddress=address;if(d.addressConfigured()){building.setValue(d.buildingNumber()==9999?"":Integer.toString(d.buildingNumber()));unit.setValue(Integer.toString(d.unitNumber()));}}
        for(var a:faceControls)a.visible=!settingsMode;for(var a:settingsControls)a.visible=settingsMode;
        chat.active=d.state()==DeviceBlockEntity.CONNECTED;settings.visible=!settingsMode;settings.active=owner&&idle;back.visible=settingsMode;ownBuilding.visible=ownUnit.visible=settingsMode&&design!=ProductDesign.GUARD;
        boolean dialing=(design==ProductDesign.LOBBY||design==ProductDesign.GUARD)&&idle;
        building.visible=unit.visible=!settingsMode&&dialing;save.active=owner&&idle&&DeviceRouting.valid(value(ownGuard))&&(design==ProductDesign.GUARD||DeviceRouting.valid(value(ownBuilding))&&DeviceRouting.valid(value(ownUnit)));
        star.active=owner&&idle&&!d.isSht308()&&d.communicationType()!=0;bus.active=owner&&idle&&d.communicationType()!=1;guard.visible=settingsMode&&design!=ProductDesign.GUARD;
        for(var a:actions.entrySet())for(var widget:a.getValue()){
            boolean active=switch(a.getKey()){
                case CALL->idle&&d.addressConfigured()&&(design!=ProductDesign.GUARD||d.guardPresence()==DeviceBlockEntity.GUARD_PRESENT)&&(DeviceRouting.door(d)||DeviceRouting.valid(value(building))&&DeviceRouting.valid(value(unit)));
                case CALL_GUARD->idle&&d.addressConfigured()&&design!=ProductDesign.GUARD;
                case CALL_OTHER_GUARD->idle&&d.addressConfigured()&&DeviceRouting.valid(value(guard))&&value(guard)!=d.guardNumber()&&d.canCallAlternateGuard();
                case ANSWER->d.state()==DeviceBlockEntity.RINGING;
                case END->!idle||d.monitoring();
                case UNLOCK->owner&&(d.links().doorCount()>0||d.state()==DeviceBlockEntity.CONNECTED);
                case MONITOR->owner&&idle;
                case HOME_LIGHTS,HOME_DOOR,HOME_GAS,HOME_OTHER->owner&&d.homeNetwork().count(a.getKey().ordinal()-DeviceNetwork.Action.HOME_LIGHTS.ordinal())>0;
                case GUARD_PRESENT,GUARD_AWAY,GUARD_OUT->owner&&d.addressConfigured()&&d.guardPresence()!=a.getKey().ordinal()-DeviceNetwork.Action.GUARD_PRESENT.ordinal();
                default->true;
            };widget.active=active;
            if(a.getKey()==DeviceNetwork.Action.CALL_OTHER_GUARD)widget.visible=settingsMode&&design!=ProductDesign.GUARD;
            if(a.getKey().ordinal()>=DeviceNetwork.Action.HOME_LIGHTS.ordinal()&&a.getKey().ordinal()<=DeviceNetwork.Action.HOME_OTHER.ordinal())widget.visible=!settingsMode&&d.panelPage()==1;
        }
        if(d.isSht308()){
            var receivers=actions.get(DeviceNetwork.Action.HANDSET);if(receivers!=null&&receivers.size()>1)receivers.get(1).visible&=d.offHook();
            for(var entry:actions.entrySet())for(var widget:entry.getValue()){
                if(entry.getKey()==DeviceNetwork.Action.CALL_GUARD)widget.active&=d.offHook()&&d.operational();
                if(!d.operational()&&entry.getKey()!=DeviceNetwork.Action.HANDSET&&entry.getKey()!=DeviceNetwork.Action.SHT_POWER&&entry.getKey()!=DeviceNetwork.Action.CALL_OTHER_GUARD)widget.active=false;
            }
        }
        camera.tone(d.monochrome(),d.isSht308()?d.sht308().brightness():1,d.isSht308()?d.sht308().contrast():1);
        if(!DeviceVideo.canView(d))camera.clear();
    }
    @Override protected void containerTick(){super.containerTick();fields.forEach(EditBox::tick);updateControls();}
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        g.fill(leftPos,topPos,leftPos+400,topPos+234,0xee101922);var d=menu.device();if(d==null)return;
        if(settingsMode){g.fill(leftPos+12,topPos+28,leftPos+388,topPos+205,0xff243747);return;}
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        var texture=new ResourceLocation(HomeSystems.MODID,"textures/block/"+design.id+(d.offHook()?"_offhook":"")+".png");g.blit(texture,leftPos+px,topPos+py,pw,ph,0,0,256,256,256,256);
        if(design.sx!=design.ex){
            int sx=x(design.sx),sy=y(design.sy),sw=w(design.ex-design.sx),sh=h(design.ey-design.sy);
            g.fill(sx,sy,sx+sw,sy+sh,d.isSht308()?0xff302f35:0xff0a1821);
            if(DeviceVideo.canView(d)&&camera.hasFrame())camera.render(g,sx,sy,sw,sh);
            else if(design==ProductDesign.WALLPAD){g.drawString(font,Component.literal(String.format("%02d:%02d",(d.getLevel().getDayTime()/1000+6)%24,d.getLevel().getDayTime()%1000*60/1000)),sx+8,sy+10,0xffffff,false);}
            if(!d.isSht308()&&(d.state()!=0||d.monitoring()))g.drawString(font,tr(d.monitoring()?"monitor":d.state()==1?"outgoing":d.state()==2?"ringing":"connected"),sx+3,sy+3,0x83e0ca,true);
        }
        if(d.isSht308()){
            if(d.offHook())g.blit(new ResourceLocation(HomeSystems.MODID,"textures/block/sht_308_receiver.png"),x(-65),y(0),w(60),h(250),0,0,128,512,128,512);
            for(int i=0;i<3;i++){int color=d.sht308().lampColor(i);if(color!=0){g.setColor((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,1);g.blit(new ResourceLocation(HomeSystems.MODID,"textures/block/panel_lamp.png"),x(228),y(137+i*22),w(11),h(11),0,0,32,32,32,32);g.setColor(1,1,1,1);}}
            g.fill(x(249),y(62+(2-d.sht308().volume())*8),x(252),y(70+(2-d.sht308().volume())*8),0xff50584d);
            g.fill(x(69),y(d.operational()?200:214),x(72),y(d.operational()?209:223),0xff50584d);
            if(d.operational())g.fill(x(223),y(32),x(226),y(35),0xffe75250);
            g.fill(x(142+d.sht308().brightness()*9),y(249),x(149+d.sht308().brightness()*9),y(252),0xff50584d);
            g.fill(x(192+d.sht308().contrast()*9),y(249),x(199+d.sht308().contrast()*9),y(252),0xff50584d);
        }
        if(d.emergencyAlerts()>0)g.drawString(font,Component.translatable("screen.homesystems.sht_alarm_count",d.emergencyAlerts()),leftPos+20,topPos+32,0xff6559,true);
        // Camera rendering restores its own blend state; transparent logo edges need blending.
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        // Replaceable logo texture, separate from the product's printed face.
        g.blit(new ResourceLocation(HomeSystems.MODID,"textures/logo/"+design.id+".png"),x(design.logoX()),y(design.logoY()),w(design.logoWidth()),h(design.logoHeight()),0,0,128,16,128,16);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){
        g.drawString(font,title,8,7,0xe3edf5,false);var d=menu.device();if(d==null)return;
        g.drawString(font,Component.literal(d.communicationType()==0?"STAR":"BUS"),350,7,0x84cbe3,false);
        if(settingsMode){g.drawString(font,tr(design==ProductDesign.GUARD?"guard_number":"assigned_guard"),24,58,0xffffff,false);if(design!=ProductDesign.GUARD){g.drawString(font,tr("building"),150,58,0xffffff,false);g.drawString(font,tr("unit"),276,58,0xffffff,false);}g.drawString(font,tr("communication_type"),24,132,0xffffff,false);g.drawString(font,tr("address_range"),24,34,0xabc2d1,false);return;}
        g.drawString(font,font.plainSubstrByWidth(d.addressLabel().getString(),380),8,201,0xb3c8d6,false);
        if(building.visible){g.drawString(font,tr("panel_building"),building.getX()-leftPos,building.getY()-topPos-10,0xc3d7e5,false);g.drawString(font,tr("panel_unit"),unit.getX()-leftPos,unit.getY()-topPos-10,0xc3d7e5,false);}
        if(design==ProductDesign.INTERPHONE)g.drawString(font,tr(d.offHook()?"handset_down":"handset_up"),8,32,0xabc2d1,false);
    }
    @Override public void render(GuiGraphics g,int x,int y,float partial){renderBackground(g);super.render(g,x,y,partial);renderTooltip(g,x,y);}
    @Override public boolean mouseClicked(double mx,double my,int button){
        draggedShtSlider=null;return super.mouseClicked(mx,my,button);
    }
    @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy){
        // AbstractContainerScreen consumes inventory dragging instead of forwarding to children.
        if(draggedShtSlider!=null&&button==0)return draggedShtSlider.mouseDragged(mx,my,button,dx,dy);
        return super.mouseDragged(mx,my,button,dx,dy);
    }
    @Override public boolean mouseReleased(double mx,double my,int button){
        if(draggedShtSlider!=null){draggedShtSlider=null;return true;}
        return super.mouseReleased(mx,my,button);
    }
    @Override public boolean keyPressed(int key,int scan,int mods){if(key==256){onClose();return true;}for(var field:fields)if(field.visible&&field.isFocused()&&key==minecraft.options.keyInventory.getKey().getValue())return true;return super.keyPressed(key,scan,mods);}
    @Override public void removed(){camera.close();super.removed();}
}
