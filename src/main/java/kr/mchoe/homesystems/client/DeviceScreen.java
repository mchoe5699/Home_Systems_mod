package kr.mchoe.homesystems.client;

import kr.mchoe.homesystems.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class DeviceScreen extends AbstractContainerScreen<DeviceMenu> {
    private EditBox targetBuilding, targetUnit, targetGuard, settingGuard, settingBuilding, settingUnit;
    private Button settings, saveSettings, back, call, priorityGuard, otherGuard, answer, end, unlock, chat, videoTab, homeTab;
    private final Button[] homeButtons = new Button[4];
    private final Button[] presenceButtons = new Button[3];
    private final CameraView camera = new CameraView();
    private boolean videoMode, homeMode, settingsMode;
    private String loadedAddress = "";
    private static final int WHITE = 0xEAF2F8, MUTED = 0x99B0C0, ACCENT = 0x56D7BE;
    public DeviceScreen(DeviceMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); imageWidth = 320; imageHeight = 234; }
    private static Component tr(String key) { return Component.translatable("screen.homesystems." + key); }
    private EditBox numeric(String label, int x, int y, int width) {
        var box = new EditBox(font, leftPos + x, topPos + y, width, 18, tr(label));box.setMaxLength(4);box.setFilter(v -> v.matches("[0-9]{0,4}"));addRenderableWidget(box);return box;
    }
    private static int value(EditBox box) { try { return Integer.parseInt(box.getValue()); } catch (NumberFormatException ignored) { return -1; } }
    private Button action(String label, int x, int y, int width, DeviceNetwork.Action action) {
        return addRenderableWidget(Button.builder(tr(label), b -> DeviceNetwork.CHANNEL.sendToServer(new DeviceNetwork.Request(menu.containerId, action, value(targetBuilding), value(targetUnit), value(targetGuard))))
            .bounds(leftPos + x, topPos + y, width, 20).build());
    }
    @Override protected void init() {
        super.init();
        targetBuilding = numeric("dial_building", 20, 121, 80);targetUnit = numeric("dial_unit", 120, 121, 80);targetGuard = numeric("dial_guard", 220, 121, 80);
        settingGuard = numeric("assigned_guard", 20, 112, 80);settingBuilding = numeric("building", 120, 112, 80);settingUnit = numeric("unit", 220, 112, 80);
        settings = addRenderableWidget(Button.builder(tr("address_settings"), b -> {
            settingsMode = true;homeMode = videoMode = false;loadSettings();updateButtons();
        }).bounds(leftPos + 20, topPos + 84, 280, 20).build());
        saveSettings = addRenderableWidget(Button.builder(tr("save_address"), b -> {
            var d = menu.device();boolean guard = d != null && DeviceRouting.guard(d);
            DeviceNetwork.CHANNEL.sendToServer(new DeviceNetwork.AddressRequest(menu.containerId, value(settingGuard), guard ? 0 : value(settingBuilding), guard ? 0 : value(settingUnit)));
        }).bounds(leftPos + 20, topPos + 146, 280, 20).build());
        back = addRenderableWidget(Button.builder(tr("back"), b -> { settingsMode = false;updateButtons(); }).bounds(leftPos + 20, topPos + 174, 280, 20).build());
        call = action("call_home",20,145,134,DeviceNetwork.Action.CALL);
        priorityGuard = action("priority_guard",164,145,136,DeviceNetwork.Action.CALL_GUARD);
        otherGuard = action("other_guard",20,170,280,DeviceNetwork.Action.CALL_OTHER_GUARD);
        answer = action("answer",20,205,68,DeviceNetwork.Action.ANSWER);end = action("end",94,205,68,DeviceNetwork.Action.END);unlock = action("unlock",168,205,68,DeviceNetwork.Action.UNLOCK);
        chat = addRenderableWidget(Button.builder(tr("chat"), b -> { onClose();minecraft.setScreen(new ChatScreen("")); }).bounds(leftPos + 242, topPos + 205, 58, 20).build());
        videoTab = addRenderableWidget(Button.builder(tr("video_tab"), b -> { videoMode = !videoMode;homeMode = settingsMode = false;updateButtons(); }).bounds(leftPos + 200, topPos + 12, 100, 20).build());
        homeTab = addRenderableWidget(Button.builder(tr("home_tab"), b -> { homeMode = !homeMode;videoMode = settingsMode = false;updateButtons(); }).bounds(leftPos + 104, topPos + 12, 90, 20).build());
        var actions = new DeviceNetwork.Action[]{DeviceNetwork.Action.HOME_LIGHTS,DeviceNetwork.Action.HOME_DOOR,DeviceNetwork.Action.HOME_GAS,DeviceNetwork.Action.HOME_OTHER};
        for(int i=0;i<4;i++)homeButtons[i]=action("home_on",199,72+i*30,91,actions[i]);
        var presenceActions=new DeviceNetwork.Action[]{DeviceNetwork.Action.GUARD_PRESENT,DeviceNetwork.Action.GUARD_AWAY,DeviceNetwork.Action.GUARD_OUT};
        for(int i=0;i<3;i++)presenceButtons[i]=action("presence_"+i,20+i*96,170,88,presenceActions[i]);
        for(var box:new EditBox[]{targetBuilding,targetUnit,targetGuard,settingGuard,settingBuilding,settingUnit})box.setResponder(ignored->updateButtons());
        loadedAddress = "";loadSettings();updateButtons();
    }
    private void loadSettings() {
        var d = menu.device();
        settingGuard.setValue(d != null && d.addressConfigured() ? Integer.toString(d.guardNumber()) : "");
        settingBuilding.setValue(d != null && d.addressConfigured() ? Integer.toString(d.buildingNumber()) : "");
        settingUnit.setValue(d != null && d.addressConfigured() ? Integer.toString(d.unitNumber()) : "");
    }
    public CameraView camera() { return camera; }
    private void updateButtons() {
        var d = menu.device();boolean present = d != null && minecraft.player != null;
        boolean guard = present && DeviceRouting.guard(d), door = present && DeviceRouting.door(d);
        boolean owner = present && d.canConfigure(minecraft.player), idle = present && d.state()==DeviceBlockEntity.IDLE;
        boolean main = !homeMode && !videoMode && !settingsMode;
        videoTab.visible = present && DeviceVideo.hasDisplay(d);homeTab.visible = present && d.homeNetwork().isWallpad();
        videoTab.setMessage(tr(videoMode?"controls_tab":"video_tab"));homeTab.setMessage(tr(homeMode?"controls_tab":"home_tab"));
        settings.visible = call.visible = priorityGuard.visible = otherGuard.visible = targetBuilding.visible = targetUnit.visible = targetGuard.visible = main;
        otherGuard.visible=main&&!guard;targetGuard.visible=main&&!guard;
        for(int i=0;i<3;i++){presenceButtons[i].visible=main&&guard;presenceButtons[i].active=owner&&d.addressConfigured()&&d.guardPresence()!=i;}
        settings.setMessage(tr(guard?"guard_settings":"address_settings"));settings.active = owner && idle;
        targetBuilding.setEditable(!door);targetUnit.setEditable(!door);
        saveSettings.visible = back.visible = settingGuard.visible = settingsMode;
        settingBuilding.visible = settingUnit.visible = settingsMode && !guard;
        settingGuard.setEditable(owner && idle);settingBuilding.setEditable(owner && idle);settingUnit.setEditable(owner && idle);
        answer.visible = end.visible = unlock.visible = chat.visible = !homeMode && !settingsMode;
        for(int i=0;i<4;i++){
            homeButtons[i].visible=homeMode;homeButtons[i].active=present&&owner&&d.homeNetwork().count(i)>0;
            boolean on=present&&d.homeNetwork().enabled(i);homeButtons[i].setMessage(tr(i==2?(on?"gas_off":"gas_on"):(on?"home_off":"home_on")));
        }
        if(!present)return;
        String address=d.addressConfigured()+":"+d.guardNumber()+":"+d.buildingNumber()+":"+d.unitNumber();
        if(!address.equals(loadedAddress)){
            loadedAddress=address;
            if(d.addressConfigured()){
                targetBuilding.setValue(d.buildingNumber()==9999&&!door?"":Integer.toString(d.buildingNumber()));
                targetUnit.setValue(Integer.toString(d.unitNumber()));targetGuard.setValue("");
            }
        }
        boolean valid = DeviceRouting.valid(value(settingGuard)) && (guard || DeviceRouting.valid(value(settingBuilding))&&DeviceRouting.valid(value(settingUnit)));
        boolean saved = d.addressConfigured() && value(settingGuard)==d.guardNumber() && (guard||value(settingBuilding)==d.buildingNumber()&&value(settingUnit)==d.unitNumber());
        saveSettings.active=owner&&idle&&valid&&!saved;saveSettings.setMessage(tr(saved?"address_saved_button":"save_address"));
        call.active=idle&&d.addressConfigured()&&(!guard||d.guardPresence()==DeviceBlockEntity.GUARD_PRESENT)&&(door||DeviceRouting.valid(value(targetBuilding))&&DeviceRouting.valid(value(targetUnit)));
        call.setMessage(tr(door?"ring_doorbell":"call_home"));
        priorityGuard.active=idle&&d.addressConfigured()&&!guard;otherGuard.active=idle&&d.addressConfigured()&&DeviceRouting.valid(value(targetGuard))&&value(targetGuard)!=d.guardNumber()&&d.canCallAlternateGuard();
        answer.active=d.state()==DeviceBlockEntity.RINGING;end.active=!idle;unlock.active=owner&&(d.links().doorCount()>0||d.state()==DeviceBlockEntity.CONNECTED);chat.active=d.state()==DeviceBlockEntity.CONNECTED;
        if(!DeviceVideo.canView(d))camera.clear();
    }
    @Override public void containerTick(){super.containerTick();for(var box:new EditBox[]{targetBuilding,targetUnit,targetGuard,settingGuard,settingBuilding,settingUnit})box.tick();updateButtons();}
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        int x=leftPos,y=topPos;g.fill(x-2,y-2,x+322,y+236,0xFF4F697A);g.fill(x,y,x+320,y+234,0xFF152330);g.fill(x,y,x+320,y+44,0xFF223B4A);g.fill(x,y,x+4,y+44,0xFF56D7BE);
        if(homeMode){for(int i=0;i<4;i++)g.fill(x+20,y+68+i*30,x+300,y+96+i*30,0xFF0B1720);}
        else if(videoMode){g.fill(x+18,y+48,x+302,y+200,0xFF050B10);camera.render(g,x+20,y+50,280,148);}
        else g.fill(x+20,y+50,x+300,y+78,0xFF0B1720);
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){
        g.drawString(font,title,20,12,WHITE,false);g.drawString(font,tr(homeMode?"home_subtitle":settingsMode?"settings_subtitle":"subtitle"),20,33,MUTED,false);
        var d=menu.device();if(d==null)return;
        if(homeMode){
            g.drawString(font,tr("home_title"),20,53,ACCENT,false);String[] channels={"lights","door","gas","other"};
            for(int i=0;i<4;i++){int y=73+i*30;g.drawString(font,tr("channel_"+channels[i]),30,y,WHITE,false);g.drawString(font,Component.translatable("screen.homesystems.relay_count",d.homeNetwork().count(i)),30,y+11,MUTED,false);g.drawString(font,tr(d.homeNetwork().enabled(i)?"state_on":"state_off"),149,y+5,d.homeNetwork().enabled(i)?ACCENT:MUTED,false);}
            g.drawString(font,tr("home_hint"),20,205,MUTED,false);g.drawString(font,tr("home_hint2"),20,219,MUTED,false);return;
        }
        if(videoMode){
            g.drawString(font,tr(camera.hasFrame()?"live":d.state()==DeviceBlockEntity.CONNECTED&&!d.peerHasVideo()?"audio_only":"no_video"),26,57,ACCENT,true);
            if(!d.peerNumber().isEmpty())g.drawString(font,Component.translatable("screen.homesystems.peer",d.peerNumber()),26,183,WHITE,true);return;
        }
        g.drawString(font,font.plainSubstrByWidth(d.addressLabel().getString(),260),28,66,WHITE,false);
        if(settingsMode){
            g.drawString(font,tr(DeviceRouting.guard(d)?"guard_settings":"address_settings"),28,53,ACCENT,false);
            g.drawString(font,tr(DeviceRouting.guard(d)?"guard_number":"assigned_guard"),20,98,MUTED,false);
            if(!DeviceRouting.guard(d)){g.drawString(font,tr("building"),120,98,MUTED,false);g.drawString(font,tr("unit"),220,98,MUTED,false);}
            g.drawString(font,tr("address_range"),20,207,MUTED,false);g.drawString(font,tr("outside_lobby_hint"),20,220,MUTED,false);return;
        }
        String status=switch(d.state()){case DeviceBlockEntity.OUTGOING->"outgoing";case DeviceBlockEntity.RINGING->"ringing";case DeviceBlockEntity.CONNECTED->"connected";default->"idle";};
        g.drawString(font,tr(status),28,53,ACCENT,false);
        if(!d.peerNumber().isEmpty()){var peer=Component.translatable("screen.homesystems.peer",d.peerNumber());g.drawString(font,peer,292-font.width(peer),53,WHITE,false);}
        g.drawString(font,tr("dial_building"),20,109,MUTED,false);g.drawString(font,tr("dial_unit"),120,109,MUTED,false);if(!DeviceRouting.guard(d))g.drawString(font,tr("dial_guard"),220,109,MUTED,false);
        g.drawString(font,tr(d.state()==DeviceBlockEntity.IDLE&&!DeviceRouting.guard(d)&&d.addressConfigured()&&!d.canCallAlternateGuard()?"no_bypass_hint":"text_hint"),20,194,MUTED,false);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){renderBackground(g);super.render(g,mx,my,partial);renderTooltip(g,mx,my);}
    @Override public boolean keyPressed(int key,int scan,int modifiers){if(key==256){onClose();return true;}for(var b:new EditBox[]{targetBuilding,targetUnit,targetGuard,settingGuard,settingBuilding,settingUnit})if(b.visible&&b.isFocused()&&key==minecraft.options.keyInventory.getKey().getValue())return true;return super.keyPressed(key,scan,modifiers);}
    @Override public void removed(){camera.close();super.removed();}
}
