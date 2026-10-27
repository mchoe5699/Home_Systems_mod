package kr.mchoe.homesystems;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.saveddata.SavedData;

/** Opens real vanilla doors and retains auto-close deadlines across server restarts. */
public final class DoorControlData extends SavedData {
    private record Pulse(long until,String block){}
    private final Map<GlobalPos,Pulse> pulses=new HashMap<>();
    public static DoorControlData get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(DoorControlData::load,DoorControlData::new,"homesystems_door_pulses");}
    public static BlockPos lower(ServerLevel level,BlockPos clicked){
        if(!level.hasChunkAt(clicked)||!(level.getBlockState(clicked).getBlock() instanceof DoorBlock))return null;
        BlockPos pos=level.getBlockState(clicked).getValue(DoorBlock.HALF)==DoubleBlockHalf.UPPER?clicked.below():clicked;
        var state=level.getBlockState(pos);
        return state.getBlock() instanceof DoorBlock&&state.getValue(DoorBlock.HALF)==DoubleBlockHalf.LOWER?pos:null;
    }
    public boolean open(ServerLevel level,BlockPos clicked){
        BlockPos pos=lower(level,clicked);if(pos==null)return false;
        var state=level.getBlockState(pos);if(!(state.getBlock() instanceof DoorBlock door))return false;
        var location=GlobalPos.of(level.dimension(),pos.immutable());
        if(state.getValue(DoorBlock.OPEN)&&!pulses.containsKey(location))return true;
        door.setOpen(null,level,state,pos,true);
        pulses.put(location,new Pulse(level.getGameTime()+100,BuiltInRegistries.BLOCK.getKey(door).toString()));setDirty();return true;
    }
    public void tick(MinecraftServer server){
        var iterator=pulses.entrySet().iterator();
        while(iterator.hasNext()){
            var entry=iterator.next();var location=entry.getKey();var level=server.getLevel(location.dimension());
            if(level==null||level.getGameTime()<entry.getValue().until||!level.hasChunkAt(location.pos()))continue;
            var state=level.getBlockState(location.pos());
            if(state.getBlock() instanceof DoorBlock door&&BuiltInRegistries.BLOCK.getKey(door).toString().equals(entry.getValue().block)&&!state.getValue(DoorBlock.POWERED))door.setOpen(null,level,state,location.pos(),false);
            iterator.remove();setDirty();
        }
    }
    @Override public CompoundTag save(CompoundTag tag){
        ListTag list=new ListTag();pulses.forEach((pos,pulse)->{var t=new CompoundTag();t.putString("Dimension",pos.dimension().location().toString());t.putLong("Pos",pos.pos().asLong());t.putLong("Until",pulse.until);t.putString("Block",pulse.block);list.add(t);});tag.put("Pulses",list);return tag;
    }
    public static DoorControlData load(CompoundTag tag){
        var data=new DoorControlData();for(Tag value:tag.getList("Pulses",Tag.TAG_COMPOUND)){
            var t=(CompoundTag)value;var dimension=ResourceLocation.tryParse(t.getString("Dimension"));
            if(dimension!=null)data.pulses.put(GlobalPos.of(ResourceKey.create(Registries.DIMENSION,dimension),BlockPos.of(t.getLong("Pos"))),new Pulse(t.getLong("Until"),t.getString("Block")));
        }return data;
    }
}
