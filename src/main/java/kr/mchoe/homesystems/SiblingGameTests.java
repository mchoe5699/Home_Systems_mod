package kr.mchoe.homesystems;

import java.util.*;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;

@GameTestHolder(HomeSystems.MODID)
@PrefixGameTestTemplate(false)
public final class SiblingGameTests {
    static final String FAMILY="{\"models\":[{\"slot\":0,\"name\":\"Original fixture\"},{\"slot\":1,\"name\":\"Sibling fixture\"}]}";
    private static void use(TestPlayer player,BlockPos pos){var stack=player.getMainHandItem();stack.getItem().onItemUseFirst(stack,new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.NORTH,pos,false)));}
    @GameTest(template="empty",batch="sibling_models")
    public static void selectorChangesModelAndPreservesConfiguration(GameTestHelper h){
        var catalog=new SiblingModels();
        try {
            catalog.apply(Map.of(new ResourceLocation(HomeSystems.MODID,"wallpad"),JsonParser.parseString(FAMILY)),null,null);
            EstateGameTests.encloseFixtures(h);var local=new BlockPos(1,1,1);h.setBlock(local,HomeSystems.WALLPAD.get());
            var pos=h.absolutePos(local);var device=(DeviceBlockEntity)h.getLevel().getBlockEntity(pos);
            var owner=new TestPlayer(h.getLevel());device.setOwner(owner.getUUID());owner.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
            owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(HomeSystems.SIBLING_SELECTOR.get()));
            h.assertTrue(device.configureAddress(0,9999,9999),"Fixture address failed");
            var relayPos=new BlockPos(3,1,1);h.setBlock(relayPos,HomeSystems.LIGHT_RELAY.get());
            h.assertTrue(device.homeNetwork().add((HomeRelayEntity)h.getLevel().getBlockEntity(h.absolutePos(relayPos))),"Fixture relay failed");
            var cameraPos=new BlockPos(3,1,3);h.setBlock(cameraPos,HomeSystems.DOOR_CAMERA.get());
            var camera=(DeviceBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(cameraPos));h.assertTrue(camera.links().linkHome(device),"Fixture home link failed");
            var doorPos=new BlockPos(5,1,1);h.setBlock(doorPos.below(),Blocks.STONE);h.setBlock(doorPos,Blocks.IRON_DOOR.defaultBlockState());h.setBlock(doorPos.above(),Blocks.IRON_DOOR.defaultBlockState().setValue(net.minecraft.world.level.block.DoorBlock.HALF,net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
            h.assertTrue(device.links().linkDoor(h.absolutePos(doorPos)),"Fixture door link failed");
            var before=device.saveWithoutMetadata();var identity=device.homeNetwork().id();
            use(owner,pos);
            h.assertTrue(owner.containerMenu instanceof SiblingMenu,"Selector item did not open sibling menu");
            var menu=(SiblingMenu)owner.containerMenu;
            h.assertTrue(menu.options().size()==2&&menu.selected()==0,"Incorrect initial choices");
            h.assertTrue(!menu.clickMenuButton(owner,-1)&&!menu.clickMenuButton(owner,16)&&!menu.clickMenuButton(owner,2),"Unregistered slot accepted");
            h.assertTrue(menu.clickMenuButton(owner,1)&&menu.selected()==1,"Sibling not applied");
            h.assertTrue(h.getLevel().getBlockEntity(pos)==device&&device.saveWithoutMetadata().equals(before)&&device.homeNetwork().id().equals(identity),"Model change replaced device or lost settings/relay links");
            h.assertTrue(device.getBlockState().getValue(DeviceBlock.MODEL)==1&&device.getBlockState().getValue(DeviceBlock.FACING)==Direction.NORTH,"World model/orientation incorrect");
            h.assertTrue(camera.links().isLinked(device)&&device.links().doorCount()==1,"Physical links changed");
            var savedState=net.minecraft.nbt.NbtUtils.writeBlockState(device.getBlockState());
            h.assertTrue(net.minecraft.nbt.NbtUtils.readBlockState(h.getLevel().holderLookup(net.minecraft.core.registries.Registries.BLOCK),savedState).getValue(DeviceBlock.MODEL)==1,"Model slot did not survive block-state serialization");
            h.assertTrue(device.getDisplayName().getString().equals("Sibling fixture"),"Product title did not follow selected model");
            h.assertTrue(menu.clickMenuButton(owner,0)&&menu.selected()==0,"Could not restore original model");
            var guest=new TestPlayer(h.getLevel());guest.setPos(pos.getX(),pos.getY(),pos.getZ());guest.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(HomeSystems.SIBLING_SELECTOR.get()));
            h.assertTrue(!menu.clickMenuButton(guest,1),"Guest changed another player's product");
            owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);h.assertTrue(!menu.clickMenuButton(owner,1),"Selector not required");
            owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(HomeSystems.SIBLING_SELECTOR.get()));
            owner.setPos(pos.getX()+20,pos.getY(),pos.getZ());h.assertTrue(!menu.clickMenuButton(owner,1),"Out-of-reach change accepted");owner.setPos(pos.getX(),pos.getY(),pos.getZ());
            var peerPos=new BlockPos(1,1,3);h.setBlock(peerPos,HomeSystems.VIDEO_PHONE.get());var peer=(DeviceBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(peerPos));peer.configureAddress(0,9999,9999);
            h.assertTrue(device.call(peer)&&!menu.clickMenuButton(owner,1),"Active call did not block model change");device.endCall();
            catalog.apply(Map.of(),null,null);h.assertTrue(!menu.clickMenuButton(owner,1),"Stale catalog selection accepted after reload");
            h.assertTrue(SiblingModels.options(HomeSystems.WALLPAD.get()).size()==1,"No-family fallback incorrect");
            owner.closeContainer();use(owner,pos);h.assertTrue(!(owner.containerMenu instanceof SiblingMenu),"No-family device opened selector");
            catalog.apply(Map.of(new ResourceLocation(HomeSystems.MODID,"wallpad"),JsonParser.parseString(FAMILY)),null,null);
            var stale=new SiblingMenu(44,owner.getInventory(),pos,SiblingModels.options(HomeSystems.WALLPAD.get()));
            h.setBlock(local,Blocks.AIR);h.setBlock(local,HomeSystems.WALLPAD.get());h.assertTrue(!stale.clickMenuButton(owner,1),"Replaced device accepted stale menu");
            h.succeed();
        } finally {catalog.apply(Map.of(),null,null);}
    }
    @GameTest(template="empty",batch="sibling_models")
    public static void malformedCatalogsAreRejected(GameTestHelper h){
        for(String json:List.of("{\"models\":[]}","{\"models\":[{\"slot\":0,\"name\":\"a\"},{\"slot\":0,\"name\":\"b\"}]}","{\"models\":[{\"slot\":0,\"name\":\"a\"},{\"slot\":16,\"name\":\"b\"}]}","{\"models\":[{\"slot\":1,\"name\":\"a\"},{\"slot\":2,\"name\":\"b\"}]}","{\"models\":[{\"slot\":0,\"name\":\"a\"},{\"slot\":1.5,\"name\":\"b\"}]}")){
            boolean rejected=false;try{SiblingModels.parse(JsonParser.parseString(json));}catch(RuntimeException expected){rejected=true;}h.assertTrue(rejected,"Malformed catalog accepted: "+json);
        }
        h.succeed();
    }
    private static final class TestPlayer extends ServerPlayer {
        TestPlayer(ServerLevel level){super(level.getServer(),level,new GameProfile(UUID.randomUUID(),"SiblingTest"));connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),this);}
        @Override public boolean hasPermissions(int level){return false;}
        @Override public void displayClientMessage(Component text,boolean overlay){}
    }
}
