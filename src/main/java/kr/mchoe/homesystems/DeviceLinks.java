package kr.mchoe.homesystems;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.DoorBlock;

/** Persistent physical door links and explicitly selected indoor call endpoints. */
public final class DeviceLinks {
    public static final int RANGE=64,MAX_LINKS=16;
    private final DeviceBlockEntity owner;
    private final Map<BlockPos,String> doors=new LinkedHashMap<>();
    private final Map<BlockPos,UUID> homes=new LinkedHashMap<>();
    public DeviceLinks(DeviceBlockEntity owner){this.owner=owner;}
    public int doorCount(){return doors.size();}
    public int homeCount(){return homes.size();}
    public boolean doorController(){return DeviceRouting.indoor(owner)||DeviceRouting.lobby(owner);}
    private boolean sameEstate(BlockPos pos){return owner.getLevel() instanceof ServerLevel level&&EstateData.get(level.getServer()).same(level.dimension().location().toString(),owner.getBlockPos(),level.dimension().location().toString(),pos);}
    public boolean linkDoor(BlockPos clicked){
        if(!(owner.getLevel() instanceof ServerLevel level)||!doorController())return false;
        BlockPos pos=DoorControlData.lower(level,clicked);
        if(pos==null||!sameEstate(pos)||owner.getBlockPos().distSqr(pos)>RANGE*RANGE||!doors.containsKey(pos)&&doors.size()>=MAX_LINKS)return false;
        doors.put(pos.immutable(),BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString());owner.changed();return true;
    }
    public boolean unlinkDoor(BlockPos clicked){
        if(!(owner.getLevel() instanceof ServerLevel level))return false;
        BlockPos pos=DoorControlData.lower(level,clicked);if(pos==null)return false;
        if(doors.remove(pos)!=null){owner.changed();return true;}return false;
    }
    public int openDoors(){
        if(!(owner.getLevel() instanceof ServerLevel level)||!doorController()||!EstateData.inside(owner))return 0;
        int count=0;
        for(var entry:doors.entrySet()){
            BlockPos pos=entry.getKey();if(!level.hasChunkAt(pos)||!sameEstate(pos))continue;
            var state=level.getBlockState(pos);
            if(state.getBlock() instanceof DoorBlock&&BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals(entry.getValue())
                &&DoorControlData.get(level.getServer()).open(level,pos))count++;
        }
        return count;
    }
    public boolean linkHome(DeviceBlockEntity indoor){
        if(!(owner.getLevel() instanceof ServerLevel)||!DeviceRouting.door(owner)||!DeviceRouting.indoor(indoor)||!indoor.addressConfigured()
            ||owner.communicationType()!=indoor.communicationType()||owner.state()!=DeviceBlockEntity.IDLE||indoor.state()!=DeviceBlockEntity.IDLE||!EstateData.same(owner,indoor)
            ||owner.getBlockPos().distSqr(indoor.getBlockPos())>RANGE*RANGE||!homes.containsKey(indoor.getBlockPos())&&homes.size()>=MAX_LINKS)return false;
        boolean changedHome=owner.addressConfigured()&&(owner.guardNumber()!=indoor.guardNumber()||!DeviceRouting.sameHome(owner,indoor));
        if(!owner.configureAddress(indoor.guardNumber(),indoor.buildingNumber(),indoor.unitNumber()))return false;
        if(changedHome)homes.clear();
        homes.put(indoor.getBlockPos().immutable(),indoor.homeNetwork().id());owner.changed();return true;
    }
    public boolean isLinked(DeviceBlockEntity indoor){
        return indoor!=null&&owner.communicationType()==indoor.communicationType()&&DeviceRouting.indoor(indoor)&&owner.getLevel()==indoor.getLevel()&&indoor.homeNetwork().id().equals(homes.get(indoor.getBlockPos()))
            &&owner.addressConfigured()&&indoor.addressConfigured()&&owner.guardNumber()==indoor.guardNumber()&&DeviceRouting.sameHome(owner,indoor)&&EstateData.same(owner,indoor);
    }
    public boolean unlinkHome(DeviceBlockEntity indoor){
        if(!indoor.homeNetwork().id().equals(homes.get(indoor.getBlockPos())))return false;
        homes.remove(indoor.getBlockPos());owner.changed();
        if(owner.state()==DeviceBlockEntity.CONNECTED&&owner.peer()==indoor)owner.endCall();return true;
    }
    public void tick(){
        if(!(owner.getLevel() instanceof ServerLevel level))return;
        boolean changed=homes.entrySet().removeIf(entry->{
            if(!level.hasChunkAt(entry.getKey()))return false;
            return !(level.getBlockEntity(entry.getKey()) instanceof DeviceBlockEntity d)||!DeviceRouting.indoor(d)||!d.homeNetwork().id().equals(entry.getValue());
        });
        changed|=doors.entrySet().removeIf(entry->level.hasChunkAt(entry.getKey())&&(!(level.getBlockState(entry.getKey()).getBlock() instanceof DoorBlock)
            ||!BuiltInRegistries.BLOCK.getKey(level.getBlockState(entry.getKey()).getBlock()).toString().equals(entry.getValue())));
        if(changed)owner.changed();
    }
    public void save(CompoundTag tag){
        ListTag doorList=new ListTag(),homeList=new ListTag();
        doors.forEach((pos,block)->{var t=new CompoundTag();t.putLong("Pos",pos.asLong());t.putString("Block",block);doorList.add(t);});
        homes.forEach((pos,id)->{var t=new CompoundTag();t.putLong("Pos",pos.asLong());t.putUUID("DeviceId",id);homeList.add(t);});
        tag.put("DoorLinks",doorList);tag.put("HomeLinks",homeList);
    }
    public void load(CompoundTag tag){
        doors.clear();homes.clear();
        for(Tag value:tag.getList("DoorLinks",Tag.TAG_COMPOUND)){
            var t=(CompoundTag)value;BlockPos pos=BlockPos.of(t.getLong("Pos"));
            if(doors.size()<MAX_LINKS&&owner.getBlockPos().distSqr(pos)<=RANGE*RANGE&&!t.getString("Block").isEmpty())doors.put(pos,t.getString("Block"));
        }
        for(Tag value:tag.getList("HomeLinks",Tag.TAG_COMPOUND)){
            var t=(CompoundTag)value;BlockPos pos=BlockPos.of(t.getLong("Pos"));
            if(homes.size()<MAX_LINKS&&owner.getBlockPos().distSqr(pos)<=RANGE*RANGE&&t.hasUUID("DeviceId"))homes.put(pos,t.getUUID("DeviceId"));
        }
    }
}
