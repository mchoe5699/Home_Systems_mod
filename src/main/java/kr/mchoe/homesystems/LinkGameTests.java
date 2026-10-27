package kr.mchoe.homesystems;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(HomeSystems.MODID)
@PrefixGameTestTemplate(false)
public final class LinkGameTests {
    private static DeviceBlockEntity device(GameTestHelper h,BlockPos pos,Block block){EstateGameTests.encloseFixtures(h);h.setBlock(pos,block);return (DeviceBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));}
    private static void door(GameTestHelper h,BlockPos pos){h.setBlock(pos.below(),Blocks.STONE);h.setBlock(pos,Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.HALF,DoubleBlockHalf.LOWER));h.setBlock(pos.above(),Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER));}
    private static void use(GameTestHelper h,TestPlayer player,BlockPos pos){
        BlockPos absolute=h.absolutePos(pos);player.setPos(absolute.getX()+.5,absolute.getY(),absolute.getZ()+.5);
        var context=new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(absolute),Direction.NORTH,absolute,false));
        var stack=player.getMainHandItem();stack.getItem().onItemUseFirst(stack,context);
    }
    @GameTest(template="empty",batch="physical_links",timeoutTicks=140)
    public static void doorLinkToolOpensBothHalvesAndCloses(GameTestHelper h){
        var p=new BlockPos(1,1,1);var d=new BlockPos(3,1,3);var controller=device(h,p,HomeSystems.WALLPAD.get());door(h,d);
        var owner=new TestPlayer(h.getLevel(),"DoorOwner");controller.setOwner(owner.getUUID());owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(HomeSystems.DOOR_LINK.get()));
        use(h,owner,p);use(h,owner,d.above());
        h.assertTrue(controller.links().doorCount()==1,"Upper-half door selection did not bind: "+owner.messages);
        h.assertTrue(controller.openLinkedDoors(),"Idle indoor device could not open its linked door");
        h.assertBlockProperty(d,DoorBlock.OPEN,true);h.assertBlockProperty(d.above(),DoorBlock.OPEN,true);
        var restored=new DeviceBlockEntity(controller.getBlockPos(),controller.getBlockState());restored.load(controller.saveWithoutMetadata());h.assertTrue(restored.links().doorCount()==1,"Door link did not persist");
        var saved=DoorControlData.get(h.getLevel().getServer()).save(new net.minecraft.nbt.CompoundTag());
        h.assertTrue(DoorControlData.load(saved).save(new net.minecraft.nbt.CompoundTag()).equals(saved),"Door close deadlines did not persist");
        h.runAfterDelay(110,()->{h.assertBlockProperty(d,DoorBlock.OPEN,false);h.assertBlockProperty(d.above(),DoorBlock.OPEN,false);h.succeed();});
    }
    @GameTest(template="empty",batch="physical_links")
    public static void homeLinkCopiesAddressAndOnlyCallsSelectedIndoors(GameTestHelper h){
        var a=new BlockPos(1,1,1);var b=new BlockPos(3,1,1);var c=new BlockPos(1,1,3);var f=new BlockPos(3,1,3);
        var video=device(h,a,HomeSystems.VIDEO_PHONE.get());var pad=device(h,b,HomeSystems.WALLPAD.get());var camera=device(h,c,HomeSystems.DOOR_CAMERA.get());var phone=device(h,f,HomeSystems.DOOR_PHONE.get());
        video.configureAddress(0,9999,0);pad.configureAddress(0,9999,0);var owner=new TestPlayer(h.getLevel(),"HomeOwner");
        for(var d:List.of(video,pad,camera,phone))d.setOwner(owner.getUUID());
        owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(HomeSystems.HOME_LINK.get()));
        use(h,owner,a);use(h,owner,c);use(h,owner,f);
        h.assertTrue(camera.addressConfigured()&&camera.guardNumber()==0&&camera.buildingNumber()==9999&&camera.unitNumber()==0,"Home Link did not copy saved household address");
        h.assertTrue(camera.links().isLinked(video)&&phone.links().isLinked(video)&&!camera.links().isLinked(pad),"Home Link selected wrong indoor endpoint");
        var directory=DeviceDirectory.get(h.getLevel().getServer());
        h.assertTrue(directory.household(camera,9999,0).equals(List.of(video))&&!camera.call(pad),"Unlinked same-address device was called");
        h.assertTrue(camera.call(video)&&video.answer(),"Linked camera call failed");camera.endCall();
        use(h,owner,b);use(h,owner,c);
        h.assertTrue(camera.callMany(directory.household(camera,9999,0))&&video.state()==DeviceBlockEntity.RINGING&&pad.state()==DeviceBlockEntity.RINGING&&pad.answer(),"Multiple selected indoor endpoints did not ring");
        owner.setShiftKeyDown(true);use(h,owner,c);owner.setShiftKeyDown(false);
        h.assertTrue(camera.state()==0&&pad.state()==0&&camera.links().homeCount()==1&&camera.links().isLinked(video),"Home unlink did not end selected active connection");
        h.succeed();
    }
    @GameTest(template="empty",batch="physical_links")
    public static void permissionsAndReplacedSourceCannotHijackDoor(GameTestHelper h){
        var p=new BlockPos(1,1,1);var d=new BlockPos(3,1,3);var controller=device(h,p,HomeSystems.INTERPHONE.get());door(h,d);
        var owner=new TestPlayer(h.getLevel(),"Owner");var guest=new TestPlayer(h.getLevel(),"Guest");controller.setOwner(owner.getUUID());
        guest.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(HomeSystems.DOOR_LINK.get()));use(h,guest,p);use(h,guest,d);h.assertTrue(controller.links().doorCount()==0,"Guest linked another player's controller");
        owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(HomeSystems.DOOR_LINK.get()));use(h,owner,p);
        h.setBlock(p,Blocks.AIR);var replacement=device(h,p,HomeSystems.INTERPHONE.get());replacement.setOwner(owner.getUUID());use(h,owner,d);
        h.assertTrue(replacement.links().doorCount()==0,"Selection token accepted a replacement controller");h.succeed();
    }
    @GameTest(template="empty",batch="physical_links")
    public static void lobbyUnlockTargetsEntranceInsteadOfLocalHomeDoor(GameTestHelper h){
        var lobby=device(h,new BlockPos(1,1,1),HomeSystems.LOBBY_PHONE.get());var pad=device(h,new BlockPos(3,1,1),HomeSystems.WALLPAD.get());
        var entrance=new BlockPos(1,1,4);var privateDoor=new BlockPos(5,1,4);door(h,entrance);door(h,privateDoor);
        lobby.configureAddress(1,101,0);pad.configureAddress(1,101,101);
        h.assertTrue(lobby.links().linkDoor(h.absolutePos(entrance))&&pad.links().linkDoor(h.absolutePos(privateDoor)),"Door pairing failed");
        h.assertTrue(lobby.call(pad)&&pad.answer()&&pad.openLinkedDoors(),"Connected household could not open lobby door");
        h.assertBlockProperty(entrance,DoorBlock.OPEN,true);h.assertBlockProperty(privateDoor,DoorBlock.OPEN,false);
        lobby.endCall();h.assertTrue(pad.openLinkedDoors(),"Idle household could not open private linked door");h.assertBlockProperty(privateDoor,DoorBlock.OPEN,true);h.succeed();
    }
    @GameTest(template="empty",batch="physical_links")
    public static void homeIdentityAndDoorLinkRemovalArePersistent(GameTestHelper h){
        var p=new BlockPos(1,1,1);var c=new BlockPos(3,1,1);var d=new BlockPos(1,1,4);
        var indoor=device(h,p,HomeSystems.VIDEO_PHONE.get());var camera=device(h,c,HomeSystems.DOOR_CAMERA.get());indoor.configureAddress(3,101,1);camera.links().linkHome(indoor);door(h,d);indoor.links().linkDoor(h.absolutePos(d));
        var saved=camera.saveWithoutMetadata();var restored=new DeviceBlockEntity(camera.getBlockPos(),camera.getBlockState());restored.setLevel(h.getLevel());restored.load(saved);h.assertTrue(restored.links().isLinked(indoor),"Home pairing did not persist");
        h.assertTrue(indoor.links().unlinkDoor(h.absolutePos(d.above()))&&indoor.links().doorCount()==0&&!indoor.openLinkedDoors(),"Unlinked physical door remained controllable");
        h.setBlock(p,Blocks.AIR);var replacement=device(h,p,HomeSystems.VIDEO_PHONE.get());replacement.configureAddress(3,101,1);
        h.assertTrue(!camera.links().isLinked(replacement)&&!camera.call(replacement),"Replacement indoor device inherited old physical link");h.succeed();
    }
    private static final class TestPlayer extends ServerPlayer{
        final List<String> messages=new ArrayList<>();
        TestPlayer(ServerLevel level,String name){super(level.getServer(),level,new GameProfile(UUID.randomUUID(),name));}
        @Override public void sendSystemMessage(Component message){messages.add(message.getString());}
        @Override public void displayClientMessage(Component message,boolean overlay){messages.add(message.getString());}
        @Override public boolean hasPermissions(int level){return false;}
    }
}
