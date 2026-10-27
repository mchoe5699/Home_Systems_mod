package kr.mchoe.homesystems;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;

/** Behavior read from the user-supplied SHT-308 manual, separate from generic terminals. */
public final class Sht308 {
    private final DeviceBlockEntity owner;
    private boolean powered=true,away,emergency;
    private int volume=1,brightness=1,contrast=1;
    private long alarmStarted;
    private final Set<UUID> notifiedGuards=new HashSet<>();
    public Sht308(DeviceBlockEntity owner){this.owner=owner;}
    public boolean powered(){return powered;}public boolean away(){return away;}public boolean emergency(){return emergency;}
    public int volume(){return volume;}public int brightness(){return brightness;}public int contrast(){return contrast;}
    public int lampColor(int index){
        if(!powered)return 0;boolean blink=owner.getLevel()!=null&&owner.getLevel().getGameTime()%20<10;
        return switch(index){case 0->emergency&&blink?0xff433c:0;case 1->owner.state()!=0&&(owner.peerKind()==2||owner.peerKind()==3)&&(owner.state()!=DeviceBlockEntity.RINGING||blink)?0x63df9b:0;case 2->away?0xe2b959:0;default->0;};
    }
    public boolean setAway(boolean value){if(!owner.isSht308()||!powered||!EstateData.inside(owner))return false;away=value;owner.changed();return true;}
    public boolean setEmergency(boolean value){
        if(!owner.isSht308()||!powered||!EstateData.inside(owner)||!(owner.getLevel() instanceof ServerLevel))return false;
        emergency=value;alarmStarted=owner.getLevel().getGameTime();notifiedGuards.clear();owner.changed();return true;
    }
    public boolean togglePower(){
        if(!owner.isSht308()||!(owner.getLevel() instanceof ServerLevel))return false;
        powered=!powered;if(!powered){owner.stopMonitor();owner.endCall();emergency=false;notifiedGuards.clear();}owner.changed();return true;
    }
    /** Manual p6: volume top=high, brightness/contrast right=high. */
    public boolean setSwitch(int control, int position) {
        if (!owner.isSht308() || !powered || !EstateData.inside(owner)
            || control < 0 || control > 2 || position < 0 || position > 2) return false;
        switch (control) {
            case 0 -> volume = position;
            case 1 -> brightness = position;
            case 2 -> contrast = position;
        }
        owner.changed();
        return true;
    }
    public void adjust(DeviceNetwork.Action action) {
        switch (action) {
            case SHT_VOLUME -> setSwitch(0, (volume + 1) % 3);
            case SHT_BRIGHTNESS -> setSwitch(1, (brightness + 1) % 3);
            case SHT_CONTRAST -> setSwitch(2, (contrast + 1) % 3);
            default -> { }
        }
    }
    public boolean entranceButton(){
        if(!owner.isSht308()||!powered)return false;
        if(owner.state()==DeviceBlockEntity.CONNECTED){
            var peer=owner.peer();if(peer!=null&&DeviceRouting.lobby(peer))return owner.openLinkedDoors();
            if(peer!=null&&DeviceRouting.door(peer)){owner.renewCall();return true;}return false;
        }
        if(owner.state()!=DeviceBlockEntity.IDLE)return false;
        if(owner.monitoring()){owner.stopMonitor();return true;}
        return owner.startMonitor();
    }
    public boolean interiorAlarm(){long age=owner.getLevel().getGameTime()-alarmStarted;return emergency&&powered&&(age<600||age>=1800);}
    public boolean entranceAlarm(){long age=owner.getLevel().getGameTime()-alarmStarted;return emergency&&powered&&age>=600&&age<1800;}
    public boolean alertsReady(){return emergency&&powered&&owner.getLevel()!=null&&owner.getLevel().getGameTime()-alarmStarted>=600;}
    public float ringVolume(){return new float[]{.25f,.6f,1f}[volume];}
    public void tick(){
        if(!owner.isSht308()||!powered||!emergency||!(owner.getLevel() instanceof ServerLevel level)||!EstateData.inside(owner)||level.getGameTime()%20!=0)return;
        if(interiorAlarm())beep(owner,.8f);
        var directory=DeviceDirectory.get(level.getServer());
        if(alertsReady())for(var guard:directory.assignedGuardDevices(owner)){
            if(notifiedGuards.add(guard.homeNetwork().id()))guard.notifyNearby(Component.translatable("message.homesystems.sht_emergency_report",owner.buildingNumber(),owner.unitNumber()));
            beep(guard,.65f);
        }
        if(entranceAlarm())for(var entrance:directory.linkedEntrances(owner))beep(entrance,.8f);
    }
    private static void beep(DeviceBlockEntity d,float volume){d.getLevel().playSound(null,d.getBlockPos(),SoundEvents.NOTE_BLOCK_BIT.value(),SoundSource.BLOCKS,volume,1.7f);}
    public void save(CompoundTag tag){var t=new CompoundTag();t.putBoolean("Powered",powered);t.putBoolean("Away",away);t.putBoolean("Emergency",emergency);t.putLong("AlarmStarted",alarmStarted);t.putInt("Volume",volume);t.putInt("Brightness",brightness);t.putInt("Contrast",contrast);tag.put("Sht308",t);}
    public void load(CompoundTag tag){var t=tag.getCompound("Sht308");powered=!t.contains("Powered")||t.getBoolean("Powered");away=t.getBoolean("Away");emergency=powered&&t.getBoolean("Emergency");alarmStarted=t.getLong("AlarmStarted");volume=clamp(t,"Volume");brightness=clamp(t,"Brightness");contrast=clamp(t,"Contrast");notifiedGuards.clear();}
    private static int clamp(CompoundTag t,String key){return t.contains(key)?Math.max(0,Math.min(2,t.getInt(key))):1;}
}
