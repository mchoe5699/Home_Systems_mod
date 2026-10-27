package kr.mchoe.homesystems;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.gametest.*;

@GameTestHolder(HomeSystems.MODID)
@PrefixGameTestTemplate(false)
public final class Sht308GameTests {
    private static DeviceBlockEntity place(GameTestHelper h,int x,int z,Block block){EstateGameTests.encloseFixtures(h);var p=new BlockPos(x,1,z);h.setBlock(p,block);var d=(DeviceBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(p));d.setCommunicationType(1);h.assertTrue(d.configureAddress(7000+Math.floorMod(h.absolutePos(BlockPos.ZERO).hashCode(),2000),101,308),"Fixture address failed");return d;}
    private static void cleanup(DeviceBlockEntity... devices){for(var d:devices)d.getLevel().setBlock(d.getBlockPos(),Blocks.AIR.defaultBlockState(),3);}
    @GameTest(template="empty",batch="sht308")
    public static void fixedBusAndBlackWhiteProfileCannotBeOverriddenBySavedData(GameTestHelper h){
        var d=place(h,1,1,HomeSystems.SHT_308.get());var camera=place(h,3,1,HomeSystems.DOOR_CAMERA.get());
        h.assertTrue(d.hasHandset()&&d.monochrome()&&d.communicationType()==1&&DeviceRouting.indoor(d)&&DeviceVideo.hasDisplay(d)&&!DeviceRouting.videoSource(d),"Wrong product roles");
        h.assertTrue(!d.setCommunicationType(0)&&camera.links().linkHome(d),"Fixed BUS was changeable or home link failed");
        var tag=d.saveWithoutMetadata();tag.putInt("CommunicationType",0);var copy=new DeviceBlockEntity(d.getBlockPos(),d.getBlockState());copy.load(tag);h.assertTrue(copy.communicationType()==1,"NBT bypassed fixed BUS");
        camera.setCommunicationType(0);h.assertTrue(!camera.call(d)&&!camera.links().isLinked(d),"STAR device reached SHT-308");
        int sample=ScreenTone.monochrome(0xff1767f1,1,1);h.assertTrue((sample&255)==(sample>>>8&255)&&(sample&255)==(sample>>>16&255),"Monochrome contains color");
        h.assertTrue((ScreenTone.monochrome(0xff808080,2,1)&255)>(ScreenTone.monochrome(0xff808080,0,1)&255),"Brightness controls do not affect pixels");
        cleanup(d,camera);h.succeed();
    }
    @GameTest(template="empty",batch="sht308",timeoutTicks=340)
    public static void manualMonitorExpiresAt15SecondsAndReceiverStartsEntranceChat(GameTestHelper h){
        var d=place(h,1,1,HomeSystems.SHT_308.get());var camera=place(h,3,1,HomeSystems.DOOR_CAMERA.get());camera.links().linkHome(d);
        h.assertTrue(d.sht308().entranceButton()&&d.monitoring()&&d.state()==0,"Entrance button did not monitor silently");
        h.assertTrue(d.setHandset(true)&&d.state()==3&&camera.state()==3&&DeviceVideo.canView(d),"Lifting receiver while monitoring failed to connect");
        d.setHandset(false);h.assertTrue(d.state()==0&&camera.state()==0&&!d.monitoring(),"Handset replacement did not blank the CRT");
        d.sht308().entranceButton();h.runAfterDelay(322,()->{h.assertTrue(!d.monitoring()&&!DeviceVideo.canView(d),"Monitor exceeded 15 seconds");cleanup(d,camera);h.succeed();});
    }
    @GameTest(template="empty",batch="sht308",timeoutTicks=1360)
    public static void entranceTalkExtendsAndTimesOutAtOneMinute(GameTestHelper h){
        var d=place(h,1,1,HomeSystems.SHT_308.get());var camera=place(h,3,1,HomeSystems.DOOR_CAMERA.get());camera.links().linkHome(d);
        h.assertTrue(camera.call(d)&&d.setHandset(true)&&d.state()==3,"Fixture conversation failed");
        h.runAfterDelay(100,()->h.assertTrue(d.sht308().entranceButton(),"Entrance button did not extend conversation"));
        h.runAfterDelay(1210,()->h.assertTrue(d.state()==3,"Renewed conversation ended too early"));
        h.runAfterDelay(1340,()->{h.assertTrue(d.state()==0&&camera.state()==0&&!DeviceVideo.canView(d),"Conversation exceeded renewed minute");cleanup(d,camera);h.succeed();});
    }
    @GameTest(template="empty",batch="sht308")
    public static void awayForwardsOnlyToEligibleAssignedOrAwayBypassOffice(GameTestHelper h){
        var d=place(h,1,1,HomeSystems.SHT_308.get());var camera=place(h,3,1,HomeSystems.DOOR_CAMERA.get());var guard=place(h,1,3,HomeSystems.GUARD_STATION.get());var other=place(h,3,3,HomeSystems.GUARD_STATION.get());other.configureAddress(d.guardNumber()==9999?9998:d.guardNumber()+1,0,0);camera.links().linkHome(d);
        d.sht308().setAway(true);h.assertTrue(camera.call(d)&&d.state()==2&&guard.state()==2&&other.state()==0,"Away did not ring resident and assigned guard exclusively");
        h.assertTrue(guard.answer()&&d.state()==0&&camera.peer()==guard,"First guard answer failed to clear the resident bell");camera.endCall();
        guard.setGuardPresence(DeviceBlockEntity.GUARD_AWAY);var eligible=DeviceDirectory.get(h.getLevel().getServer()).guards(d,d.guardNumber(),true);
        h.assertTrue(!eligible.isEmpty()&&camera.call(d)&&eligible.stream().allMatch(g->g.state()==2&&g.guardNumber()!=d.guardNumber()),"Explicit Away office did not allow eligible alternate guard");
        var alternate=eligible.get(0);alternate.answer();d.sht308().setAway(false);DeviceBlockEntity.serverTick(h.getLevel(),camera.getBlockPos(),camera.getBlockState(),camera);h.assertTrue(camera.state()==0&&alternate.state()==0,"Cancel Away left forwarded conversation connected");
        cleanup(d,camera,guard,other);h.succeed();
    }
    @GameTest(template="empty",batch="sht308",timeoutTicks=1840)
    public static void alarmStagesPersistenceAndPowerSwitch(GameTestHelper h){
        var d=place(h,1,1,HomeSystems.SHT_308.get());var guard=place(h,3,1,HomeSystems.GUARD_STATION.get());d.sht308().setEmergency(true);d.sht308().setAway(true);
        h.assertTrue(d.sht308().interiorAlarm()&&!d.sht308().entranceAlarm(),"Initial interior alarm stage incorrect");
        var copy=new DeviceBlockEntity(d.getBlockPos(),d.getBlockState());copy.setLevel(h.getLevel());copy.load(d.saveWithoutMetadata());h.assertTrue(copy.sht308().emergency()&&copy.sht308().away(),"Alarm/Away did not persist");
        h.runAfterDelay(625,()->{h.assertTrue(d.sht308().entranceAlarm()&&!d.sht308().interiorAlarm()&&guard.emergencyAlerts()>0,"Guard notice/entrance alarm stage missing");});
        h.runAfterDelay(1822,()->{h.assertTrue(d.sht308().interiorAlarm()&&!d.sht308().entranceAlarm(),"Continuous interior alarm did not resume");d.sht308().togglePower();h.assertTrue(!d.operational()&&!d.sht308().emergency()&&!guard.call(d),"Power off did not cancel alarm/block calls");cleanup(d,guard);h.succeed();});
    }
    @GameTest(template="empty",batch="sht308")
    public static void entranceButtonUnlocksLobbyOnlyDuringConnectedLobbyCall(GameTestHelper h){
        var d=place(h,1,1,HomeSystems.SHT_308.get());var lobby=place(h,3,1,HomeSystems.LOBBY_PHONE.get());var p=new BlockPos(5,1,3);h.setBlock(p.below(),Blocks.STONE);h.setBlock(p,Blocks.IRON_DOOR.defaultBlockState());h.setBlock(p.above(),Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER));lobby.links().linkDoor(h.absolutePos(p));
        h.assertTrue(!d.openLinkedDoors()&&lobby.call(d)&&d.setHandset(true)&&d.sht308().entranceButton(),"Connected lobby unlock failed");h.assertBlockProperty(p,DoorBlock.OPEN,true);d.setHandset(false);h.assertTrue(!d.sht308().entranceButton(),"Disconnected entrance button bypassed live call");cleanup(d,lobby);h.succeed();
    }
}
