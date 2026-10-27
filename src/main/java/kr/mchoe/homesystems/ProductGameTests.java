package kr.mchoe.homesystems;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.gametest.*;

@GameTestHolder(HomeSystems.MODID)
@PrefixGameTestTemplate(false)
public final class ProductGameTests {
    private static DeviceBlockEntity device(GameTestHelper h,int x,int z,Block block){EstateGameTests.encloseFixtures(h);var pos=new BlockPos(x,1,z);h.setBlock(pos,block);var d=(DeviceBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));h.assertTrue(d.configureAddress(0,101,101),"Fixture address failed");return d;}
    @GameTest(template="empty",batch="product_controls")
    public static void receiversLiftAnswerHangUpAndPersist(GameTestHelper h){
        var lobby=device(h,1,1,HomeSystems.LOBBY_PHONE.get());var video=device(h,3,1,HomeSystems.VIDEO_PHONE.get());var inter=device(h,1,3,HomeSystems.INTERPHONE.get());var guard=device(h,3,3,HomeSystems.GUARD_STATION.get());var pad=device(h,5,1,HomeSystems.WALLPAD.get());
        for(var d:List.of(video,inter,guard)){
            h.assertTrue(d.hasHandset()&&!d.offHook(),"Receiver not initially on cradle");
            h.assertTrue(lobby.call(d)&&d.setHandset(true)&&d.state()==DeviceBlockEntity.CONNECTED&&lobby.state()==DeviceBlockEntity.CONNECTED,"Lifting did not answer incoming call");
            h.assertTrue(d.setHandset(false)&&d.state()==0&&lobby.state()==0&&!d.offHook(),"Replacing receiver did not end both endpoints");
            d.setHandset(true);var nbt=net.minecraft.nbt.NbtUtils.writeBlockState(d.getBlockState());
            h.assertTrue(net.minecraft.nbt.NbtUtils.readBlockState(h.getLevel().holderLookup(net.minecraft.core.registries.Registries.BLOCK),nbt).getValue(DeviceBlock.OFFHOOK),"Raised receiver state did not serialize");d.setHandset(false);
        }
        h.assertTrue(!pad.hasHandset()&&!pad.setHandset(true),"Touch wallpad gained a receiver");h.setBlock(new BlockPos(3,1,3),net.minecraft.world.level.block.Blocks.AIR);h.succeed();
    }
    @GameTest(template="empty",batch="product_controls")
    public static void communicationTypesGateCallsHomeLinksAndGuardBypass(GameTestHelper h){
        var pad=device(h,1,1,HomeSystems.WALLPAD.get());var lobby=device(h,3,1,HomeSystems.LOBBY_PHONE.get());var camera=device(h,1,3,HomeSystems.DOOR_CAMERA.get());var guard=device(h,3,3,HomeSystems.GUARD_STATION.get());
        h.assertTrue(pad.communicationType()==0&&lobby.setCommunicationType(1)&&!lobby.call(pad),"STAR and BUS communicated");
        h.assertTrue(camera.setCommunicationType(1)&&!camera.links().linkHome(pad),"Mismatched home types linked");
        h.assertTrue(pad.setCommunicationType(1)&&camera.links().linkHome(pad)&&lobby.call(pad)&&pad.answer(),"Matching BUS devices could not link/call");
        h.assertTrue(!pad.setCommunicationType(0),"Type changed during call");lobby.endCall();
        var saved=pad.saveWithoutMetadata();var restored=new DeviceBlockEntity(pad.getBlockPos(),pad.getBlockState());restored.load(saved);h.assertTrue(restored.communicationType()==1,"Communication type did not persist");
        h.assertTrue(guard.setGuardPresence(DeviceBlockEntity.GUARD_AWAY),"Fixture guard Away failed");
        h.assertTrue(!DeviceDirectory.get(h.getLevel().getServer()).canBypass(pad),"Different-type guard enabled bypass");
        h.assertTrue(guard.setCommunicationType(1)&&DeviceDirectory.get(h.getLevel().getServer()).canBypass(pad),"Matching-type Away guard failed bypass");
        h.assertTrue(pad.setCommunicationType(0)&&!camera.links().isLinked(pad)&&!camera.call(pad),"Changing types left a usable cross-type link");h.setBlock(new BlockPos(3,1,3),net.minecraft.world.level.block.Blocks.AIR);h.succeed();
    }
    @GameTest(template="empty",batch="product_controls",timeoutTicks=50)
    public static void entranceMonitoringDoesNotStartChatCallAndRevokesUnlink(GameTestHelper h){
        var pad=device(h,1,1,HomeSystems.WALLPAD.get());var camera=device(h,3,1,HomeSystems.DOOR_CAMERA.get());var phone=device(h,1,3,HomeSystems.DOOR_PHONE.get());
        h.assertTrue(!pad.startMonitor(),"Unlinked camera was viewable");phone.links().linkHome(pad);h.assertTrue(!pad.startMonitor(),"Audio-only entrance produced video");
        camera.links().linkHome(pad);h.assertTrue(pad.startMonitor()&&pad.monitorSource()==camera&&DeviceVideo.canView(pad)&&pad.state()==0&&camera.state()==0,"Monitor started an audio/chat call or no frame");
        var tag=pad.getUpdateTag();var copy=new DeviceBlockEntity(pad.getBlockPos(),pad.getBlockState());copy.setLevel(h.getLevel());copy.handleUpdateTag(tag);h.assertTrue(copy.monitoring(),"Monitoring did not sync to client");
        var disk=new DeviceBlockEntity(pad.getBlockPos(),pad.getBlockState());disk.setLevel(h.getLevel());disk.load(pad.saveWithoutMetadata());h.assertTrue(!disk.monitoring(),"Transient monitor session restored after restart");
        camera.links().unlinkHome(pad);h.assertTrue(pad.monitorSource()==null,"Unlinked source remained viewable");
        h.runAfterDelay(22,()->{h.assertTrue(!pad.monitoring()&&!DeviceVideo.canView(pad),"Monitor not revoked after unlink");h.succeed();});
    }
}
