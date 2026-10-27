package kr.mchoe.homesystems;

import java.util.ArrayList;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(HomeSystems.MODID)
@PrefixGameTestTemplate(false)
public final class HomeNetworkGameTests {
    private static final BlockPos PAD = new BlockPos(1, 1, 1);
    private static final BlockPos[] OUTPUTS = {new BlockPos(1, 1, 3), new BlockPos(3, 1, 3), new BlockPos(5, 1, 3), new BlockPos(7, 1, 3)};
    private static DeviceBlockEntity pad(GameTestHelper helper) {
        EstateGameTests.encloseFixtures(helper);
        helper.setBlock(PAD, HomeSystems.WALLPAD.get());
        return (DeviceBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(PAD));
    }
    private static HomeRelayEntity relay(GameTestHelper helper, int channel) {
        Block[] blocks = {HomeSystems.LIGHT_RELAY.get(), HomeSystems.DOOR_RELAY.get(), HomeSystems.GAS_RELAY.get(), HomeSystems.UTILITY_RELAY.get()};
        helper.setBlock(OUTPUTS[channel], blocks[channel]);
        return (HomeRelayEntity) helper.getLevel().getBlockEntity(helper.absolutePos(OUTPUTS[channel]));
    }

    @GameTest(template = "empty")
    public static void independentChannelsDriveRealRedstone(GameTestHelper helper) {
        var pad = pad(helper);
        for (int i = 0; i < 4; i++) helper.assertTrue(pad.homeNetwork().add(relay(helper, i)), "Relay pairing failed");
        BlockPos lamp = new BlockPos(1, 1, 4);
        helper.setBlock(lamp, Blocks.REDSTONE_LAMP);
        helper.assertTrue(!pad.homeNetwork().enabled(2), "Gas must start closed");
        pad.homeNetwork().toggle(0);
        helper.assertBlockProperty(OUTPUTS[0], HomeRelayBlock.POWERED, true);
        helper.assertBlockProperty(lamp, RedstoneLampBlock.LIT, true);
        for (int i = 1; i < 4; i++) helper.assertBlockProperty(OUTPUTS[i], HomeRelayBlock.POWERED, false);
        pad.homeNetwork().toggle(2);
        helper.assertBlockProperty(OUTPUTS[2], HomeRelayBlock.POWERED, true);
        helper.assertBlockProperty(OUTPUTS[1], HomeRelayBlock.POWERED, false);
        pad.homeNetwork().toggle(2);
        helper.assertBlockProperty(OUTPUTS[2], HomeRelayBlock.POWERED, false);
        pad.homeNetwork().toggle(0);
        helper.runAfterDelay(6, () -> {
            helper.assertBlockProperty(lamp, RedstoneLampBlock.LIT, false);
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void networkPersistsAndRebinds(GameTestHelper helper) {
        var pad = pad(helper); var relay = relay(helper, 2);
        pad.homeNetwork().add(relay); pad.homeNetwork().toggle(2);
        var restored = new DeviceBlockEntity(pad.getBlockPos(), pad.getBlockState());
        restored.load(pad.saveWithoutMetadata()); restored.setLevel(helper.getLevel());
        var restoredRelay = new HomeRelayEntity(relay.getBlockPos(), relay.getBlockState());
        restoredRelay.load(relay.saveWithoutMetadata()); restoredRelay.setLevel(helper.getLevel());
        helper.assertTrue(restored.homeNetwork().id().equals(pad.homeNetwork().id()), "Network ID not persisted");
        helper.assertTrue(restored.homeNetwork().count(2) == 1 && restored.homeNetwork().enabled(2), "Channel state/binding not persisted");
        helper.assertTrue(restoredRelay.matches(restored), "Relay could not restore its controller");
        helper.assertTrue(!new DeviceBlockEntity(pad.getBlockPos(), pad.getBlockState()).homeNetwork().id().equals(pad.homeNetwork().id()), "Replacement reused network ID");
        var another = new BlockPos(2, 1, 1);
        helper.setBlock(another, HomeSystems.WALLPAD.get());
        var next = (DeviceBlockEntity)helper.getLevel().getBlockEntity(helper.absolutePos(another));
        helper.assertTrue(next.homeNetwork().add(relay), "Rebinding failed");
        helper.assertTrue(pad.homeNetwork().count(2) == 0 && next.homeNetwork().count(2) == 1, "Old controller kept relay");
        helper.assertBlockProperty(OUTPUTS[2], HomeRelayBlock.POWERED, false);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void removingControllerAndRelayDisconnects(GameTestHelper helper) {
        var pad = pad(helper); var first = relay(helper, 0); var gas = relay(helper, 2);
        pad.homeNetwork().add(first); pad.homeNetwork().add(gas);
        pad.homeNetwork().toggle(0); pad.homeNetwork().toggle(2);
        helper.setBlock(OUTPUTS[0], Blocks.AIR);
        helper.assertTrue(pad.homeNetwork().count(0) == 0, "Broken relay remained registered");
        helper.setBlock(PAD, Blocks.AIR);
        helper.assertTrue(!gas.linked(), "Relay kept broken controller");
        helper.assertBlockProperty(OUTPUTS[2], HomeRelayBlock.POWERED, false);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void networkControlsRequireOwner(GameTestHelper helper) {
        var pad = pad(helper); var relay = relay(helper, 2);
        var owner = new RecordingPlayer(helper.getLevel(), "Owner");
        var guest = new RecordingPlayer(helper.getLevel(), "Guest");
        var pos = pad.getBlockPos();
        owner.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        guest.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        pad.setOwner(owner.getUUID()); relay.setOwner(owner.getUUID());
        pad.homeNetwork().add(relay);
        helper.assertTrue(relay.canConfigure(owner) && !relay.canConfigure(guest), "Relay ownership check failed");
        pad.handle(guest, new DeviceNetwork.Request(0, DeviceNetwork.Action.HOME_GAS, 0, 0, 0));
        helper.assertBlockProperty(OUTPUTS[2], HomeRelayBlock.POWERED, false);
        helper.runAfterDelay(5, () -> {
            pad.handle(owner, new DeviceNetwork.Request(0, DeviceNetwork.Action.HOME_GAS, 0, 0, 0));
            helper.assertBlockProperty(OUTPUTS[2], HomeRelayBlock.POWERED, true);
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void allFiveDevicesRouteNormalChatAndHaveMenus(GameTestHelper helper) {
        EstateGameTests.encloseFixtures(helper);
        BlockPos far = new BlockPos(4, 1, 1);
        helper.setBlock(far, HomeSystems.INTERPHONE.get());
        var receiver = (DeviceBlockEntity)helper.getLevel().getBlockEntity(helper.absolutePos(far));
        String prefix = "8" + Integer.toUnsignedString(helper.absolutePos(PAD).hashCode());
        helper.assertTrue(receiver.configure(prefix, ""), "Receiver registration failed");
        var sender = new RecordingPlayer(helper.getLevel(), "Caller");
        var listener = new RecordingPlayer(helper.getLevel(), "Listener");
        var outsider = new RecordingPlayer(helper.getLevel(), "Outside");
        var absolute = helper.absolutePos(PAD);
        sender.setPos(absolute.getX() + .5, absolute.getY() + .5, absolute.getZ() + .5);
        listener.setPos(sender.getX() + 2, sender.getY(), sender.getZ());
        outsider.setPos(sender.getX() + 100, sender.getY(), sender.getZ());
        Block[] types = {HomeSystems.LOBBY_PHONE.get(), HomeSystems.WALLPAD.get(), HomeSystems.VIDEO_PHONE.get(), HomeSystems.INTERPHONE.get(), HomeSystems.GUARD_STATION.get()};
        for (Block type : types) {
            helper.setBlock(PAD, type);
            var caller = (DeviceBlockEntity)helper.getLevel().getBlockEntity(absolute);
            String number = "7" + Integer.toUnsignedString(absolute.hashCode());
            helper.assertTrue(caller.configure(number, prefix) && caller.call(receiver) && receiver.answer(), "Call failed for " + type);
            helper.assertTrue(caller.createMenu(12, sender.getInventory(), sender) instanceof DeviceMenu menu && menu.stillValid(sender), "Missing usable control menu for " + type);
            var event = new ServerChatEvent(sender, "안녕하세요", Component.literal("안녕하세요"));
            MinecraftForge.EVENT_BUS.post(event);
            helper.assertTrue(event.isCanceled(), "Normal chat was not routed for " + type);
            sender.inbox.clear(); listener.inbox.clear(); outsider.inbox.clear();
            int delivered = DeviceDirectory.deliver(caller, Component.literal("private intercom"), List.of(sender, listener, outsider));
            helper.assertTrue(delivered == 2 && sender.inbox.size() == 1 && listener.inbox.size() == 1 && outsider.inbox.isEmpty(), "Chat leaked or duplicated");
            caller.endCall();
            event = new ServerChatEvent(sender, "public again", Component.literal("public again"));
            MinecraftForge.EVENT_BUS.post(event);
            helper.assertTrue(!event.isCanceled(), "Ordinary chat remained intercepted after hanging up");
        }
        helper.succeed();
    }

    private static final class RecordingPlayer extends ServerPlayer {
        final List<Component> inbox = new ArrayList<>();
        RecordingPlayer(ServerLevel level, String name) { super(level.getServer(), level, new GameProfile(UUID.randomUUID(), name)); }
        @Override public void sendSystemMessage(Component text) { inbox.add(text); }
        @Override public boolean hasPermissions(int level) { return false; }
    }

    @GameTest(template = "empty")
    public static void staleBindingsAreRemovedAfterReload(GameTestHelper helper) {
        var first = pad(helper); var relay = relay(helper, 2);
        first.homeNetwork().add(relay);
        var saved = first.saveWithoutMetadata();
        BlockPos other = new BlockPos(2, 1, 1);
        helper.setBlock(other, HomeSystems.WALLPAD.get());
        var second = (DeviceBlockEntity)helper.getLevel().getBlockEntity(helper.absolutePos(other));
        second.homeNetwork().add(relay);
        first.load(saved);
        first.homeNetwork().tick();
        helper.assertTrue(first.homeNetwork().count(2) == 0 && second.homeNetwork().count(2) == 1, "Stale controller binding survived reload");
        helper.succeed();
    }
}
