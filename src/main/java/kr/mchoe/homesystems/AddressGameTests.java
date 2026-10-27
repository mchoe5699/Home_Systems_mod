package kr.mchoe.homesystems;

import java.util.List;
import java.util.UUID;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(HomeSystems.MODID)
@PrefixGameTestTemplate(false)
public final class AddressGameTests {
    private static DeviceBlockEntity place(GameTestHelper h, BlockPos pos, Block block) {
        h.setBlock(pos, block);return (DeviceBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));
    }
    @GameTest(template = "empty", batch = "structured_addresses")
    public static void householdJurisdictionDoorAndGuardRouting(GameTestHelper h) {
        EstateGameTests.encloseFixtures(h);
        var lobby=place(h,new BlockPos(1,1,1),HomeSystems.LOBBY_PHONE.get());
        var video=place(h,new BlockPos(3,1,1),HomeSystems.VIDEO_PHONE.get());
        var pad=place(h,new BlockPos(5,1,1),HomeSystems.WALLPAD.get());
        var inter=place(h,new BlockPos(1,1,3),HomeSystems.INTERPHONE.get());
        var camera=place(h,new BlockPos(3,1,3),HomeSystems.DOOR_CAMERA.get());
        var phone=place(h,new BlockPos(5,1,3),HomeSystems.DOOR_PHONE.get());
        var guard=place(h,new BlockPos(1,1,5),HomeSystems.GUARD_STATION.get());
        var otherGuard=place(h,new BlockPos(3,1,5),HomeSystems.GUARD_STATION.get());
        h.assertTrue(lobby.configureAddress(0,0,0),"Zero must be accepted for every number");
        h.assertTrue(!lobby.configureAddress(-1,1,1)&&!lobby.configureAddress(1,10000,1)&&!lobby.configureAddress(1,1,10000),"Out-of-range address accepted");
        h.assertTrue(lobby.guardNumber()==0&&lobby.buildingNumber()==0&&lobby.unitNumber()==0,"Invalid save changed the address");
        h.assertTrue(lobby.configureAddress(9999,9999,9999),"9999 boundary rejected");
        lobby.configureAddress(321,101,0);
        for(var d:List.of(video,pad,inter,camera,phone))h.assertTrue(d.configureAddress(321,101,1203),"Matching household address could not be shared");
        for(var d:List.of(video,pad,inter)){h.assertTrue(camera.links().linkHome(d)&&phone.links().linkHome(d),"Explicit home pairing failed");}
        guard.configureAddress(321,0,0);otherGuard.configureAddress(654,0,0);
        var directory=DeviceDirectory.get(h.getLevel().getServer());
        var household=directory.household(lobby,101,1203);
        h.assertTrue(household.size()==3&&household.containsAll(List.of(video,pad,inter)),"Household dial did not find all three indoor devices");
        h.assertTrue(lobby.callMany(household),"Household call rejected");
        for(var d:List.of(video,pad,inter))h.assertTrue(d.state()==DeviceBlockEntity.RINGING,"Not all indoor devices rang");
        h.assertTrue(pad.answer()&&lobby.peer()==pad,"First household answer did not connect");
        h.assertTrue(video.state()==DeviceBlockEntity.IDLE&&inter.state()==DeviceBlockEntity.IDLE&&!video.answer(),"Other devices retained answered call");
        lobby.endCall();
        video.configureAddress(322,101,1203);
        h.assertTrue(!DeviceRouting.allowed(lobby,video),"Different jurisdiction indoor device linked");
        video.configureAddress(321,102,1203);
        h.assertTrue(!lobby.call(video)&&directory.household(lobby,102,1203).isEmpty(),"Building lobby reached a different building");
        lobby.configureAddress(321,9999,0);
        h.assertTrue(lobby.call(video)&&video.answer(),"9999 perimeter lobby could not reach another building");lobby.endCall();
        video.configureAddress(321,101,1203);
        h.assertTrue(camera.call(pad)&&DeviceVideo.canView(pad),"Door camera did not provide incoming video preview");
        h.assertTrue(pad.answer()&&pad.unlockPeer(),"Door camera call/unlock failed");camera.endCall();
        h.assertTrue(phone.call(pad)&&!DeviceVideo.canView(pad),"Audio-only door phone exposed video");
        h.assertTrue(pad.answer()&&!DeviceVideo.canView(pad)&&pad.unlockPeer(),"Door phone did not support audio-only call/unlock");phone.endCall();
        camera.configureAddress(321,101,1204);
        h.assertTrue(!camera.call(pad),"Door camera reached a different household");camera.configureAddress(321,101,1203);
        var preferred=directory.guards(lobby,321,true);
        h.assertTrue(preferred.equals(List.of(guard)),"Jurisdiction guard was not prioritized");
        h.assertTrue(directory.guards(lobby,654,false).isEmpty()&&!lobby.call(otherGuard),"Alternate guard reached while assigned guard was present");
        h.assertTrue(lobby.call(guard)&&guard.answer(),"Assigned guard call failed");
        h.assertTrue(directory.guards(camera,654,false).isEmpty(),"Busy guard was incorrectly treated as Away");lobby.endCall();
        guard.setGuardPresence(DeviceBlockEntity.GUARD_AWAY);
        h.assertTrue(directory.canBypass(lobby)&&directory.guards(lobby,321,true).equals(List.of(otherGuard)),"Away did not permit routing around assigned guard; bypass="+directory.canBypass(lobby)+" preferred="+directory.guards(lobby,321,true).stream().map(d->d.number()+":"+d.guardPresence()+":"+d.communicationType()).toList()+" index="+directory.save(new net.minecraft.nbt.CompoundTag()).getList("GuardPresence",10));
        h.assertTrue(lobby.callMany(directory.guards(lobby,654,false))&&otherGuard.answer(),"Explicit alternate guard call blocked while assigned guard was Away");
        guard.setGuardPresence(DeviceBlockEntity.GUARD_PRESENT);
        DeviceBlockEntity.serverTick(h.getLevel(),lobby.getBlockPos(),lobby.getBlockState(),lobby);
        h.assertTrue(lobby.state()==DeviceBlockEntity.IDLE&&otherGuard.state()==DeviceBlockEntity.IDLE,"Returning guard did not terminate an obsolete bypass call");
        h.assertTrue(!directory.canBypass(lobby)&&directory.guards(lobby,654,false).isEmpty(),"Returning guard did not disable bypass");
        guard.setGuardPresence(DeviceBlockEntity.GUARD_OUT);
        h.assertTrue(directory.canBypass(lobby),"Out status did not permit bypass");
        var backup=place(h,new BlockPos(5,1,5),HomeSystems.GUARD_STATION.get());backup.configureAddress(321,0,0);
        h.assertTrue(!directory.canBypass(lobby),"One present assigned terminal was ignored");backup.setGuardPresence(DeviceBlockEntity.GUARD_AWAY);
        h.assertTrue(directory.canBypass(lobby),"All assigned terminals Away/Out did not permit bypass");
        h.setBlock(new BlockPos(5,1,5),net.minecraft.world.level.block.Blocks.AIR);
        var savedPresence=new DeviceBlockEntity(guard.getBlockPos(),guard.getBlockState());savedPresence.load(guard.saveWithoutMetadata());
        h.assertTrue(savedPresence.guardPresence()==DeviceBlockEntity.GUARD_OUT,"Guard presence did not persist");
        h.assertTrue(DeviceDirectory.load(directory.save(new net.minecraft.nbt.CompoundTag())).canBypass(lobby),"Persisted guard-presence index was lost");
        h.setBlock(new BlockPos(1,1,5),net.minecraft.world.level.block.Blocks.AIR);
        h.assertTrue(!directory.canBypass(lobby)&&directory.guards(lobby,321,true).isEmpty(),"Missing assigned guard was incorrectly treated as explicitly Away");
        h.assertTrue(!DeviceRouting.videoSource(inter)&&!DeviceRouting.videoSource(phone)&&DeviceRouting.videoSource(camera),"Wrong audio/video role classification");
        var restored=new DeviceBlockEntity(pad.getBlockPos(),pad.getBlockState());restored.load(pad.saveWithoutMetadata());
        h.assertTrue(restored.addressConfigured()&&restored.guardNumber()==321&&restored.buildingNumber()==101&&restored.unitNumber()==1203,"Structured address did not persist");
        var reloadedDirectory=DeviceDirectory.load(directory.save(new net.minecraft.nbt.CompoundTag()));
        h.assertTrue(reloadedDirectory.household(lobby,101,1203).size()==3,"Directory lost duplicate household endpoints after save/load");
        h.assertTrue(lobby.callMany(List.of(video,pad,inter)),"Second group call failed");video.endCall();
        h.assertTrue(lobby.state()==DeviceBlockEntity.OUTGOING&&pad.state()==DeviceBlockEntity.RINGING&&inter.answer(),"One device declining cancelled the whole household");lobby.endCall();
        h.assertTrue(lobby.callMany(List.of(video,pad,inter)),"Removal test call failed");
        h.setBlock(new BlockPos(3,1,1),net.minecraft.world.level.block.Blocks.AIR);
        h.assertTrue(lobby.state()==DeviceBlockEntity.OUTGOING&&pad.answer(),"Removing the first ringing device lost the remaining household");lobby.endCall();
        var owner=new TestPlayer(h.getLevel(),"AddressOwner");var guest=new TestPlayer(h.getLevel(),"AddressGuest");
        var p=pad.getBlockPos();owner.setPos(p.getX()+.5,p.getY(),p.getZ()+.5);guest.setPos(owner.getX(),owner.getY(),owner.getZ());pad.setOwner(owner.getUUID());
        pad.saveAddress(guest,1,2,3);h.assertTrue(pad.guardNumber()==321,"Guest changed address");
        pad.saveAddress(owner,0,9999,0);h.assertTrue(pad.guardNumber()==0&&pad.buildingNumber()==9999&&pad.unitNumber()==0,"Owner could not save numeric boundary values");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void numericAddressPacketsRoundTrip(GameTestHelper h){
        for(int number:new int[]{0,9999}){
            var source=new DeviceNetwork.AddressRequest(42,number,number,number);
            var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try{DeviceNetwork.AddressRequest.encode(source,buffer);h.assertTrue(source.equals(DeviceNetwork.AddressRequest.decode(buffer)),"Address packet changed boundary values");}finally{buffer.release();}
        }
        h.succeed();
    }
    private static final class TestPlayer extends ServerPlayer{
        TestPlayer(ServerLevel level,String name){super(level.getServer(),level,new GameProfile(UUID.randomUUID(),name));}
        @Override public void sendSystemMessage(Component message){}
        @Override public boolean hasPermissions(int level){return false;}
    }
}
