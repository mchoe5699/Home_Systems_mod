package kr.mchoe.homesystems;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(HomeSystems.MODID)
@PrefixGameTestTemplate(false)
public final class DeviceGameTests {
    private static final BlockPos FIRST = new BlockPos(2, 1, 2), SECOND = new BlockPos(4, 1, 2);

    private static DeviceBlockEntity place(GameTestHelper helper, BlockPos pos, net.minecraft.world.level.block.Block block) {
        EstateGameTests.encloseFixtures(helper);
        helper.setBlock(pos, block);
        return (DeviceBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }
    private static String number(GameTestHelper helper, BlockPos pos) {
        return "9" + Integer.toUnsignedString(helper.absolutePos(pos).hashCode());
    }
    private static DeviceBlockEntity[] pair(GameTestHelper helper) {
        var entrance = place(helper, FIRST, HomeSystems.LOBBY_PHONE.get());
        var wallpad = place(helper, SECOND, HomeSystems.WALLPAD.get());
        helper.assertTrue(entrance.configure(number(helper, FIRST), number(helper, SECOND)), "Entrance registration failed");
        helper.assertTrue(wallpad.configure(number(helper, SECOND), number(helper, FIRST)), "Wallpad registration failed");
        return new DeviceBlockEntity[]{entrance, wallpad};
    }

    @GameTest(template = "empty")
    public static void callAnswerAndHangUp(GameTestHelper helper) {
        var devices = pair(helper);
        helper.assertTrue(devices[0].call(devices[1]), "Call rejected");
        helper.assertTrue(devices[0].state() == DeviceBlockEntity.OUTGOING && devices[1].state() == DeviceBlockEntity.RINGING, "Wrong ringing states");
        helper.assertTrue(devices[1].answer(), "Answer rejected");
        helper.assertTrue(devices[0].state() == DeviceBlockEntity.CONNECTED && devices[1].state() == DeviceBlockEntity.CONNECTED, "Both endpoints must connect");
        devices[1].endCall();
        helper.assertTrue(devices[0].state() == DeviceBlockEntity.IDLE && devices[1].state() == DeviceBlockEntity.IDLE, "Both endpoints must hang up");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void rejectDuplicateAndInvalidNumbers(GameTestHelper helper) {
        var devices = pair(helper);
        helper.assertTrue(!devices[1].configure(devices[0].number(), ""), "Duplicate number accepted");
        helper.assertTrue(!devices[1].configure("abc", ""), "Non-numeric number accepted");
        helper.assertTrue(!devices[1].configure("1234567890123", ""), "Oversized number accepted");
        helper.assertTrue(!devices[0].call(devices[0]), "Self call accepted");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void persistNumbersAndDirectory(GameTestHelper helper) {
        var devices = pair(helper);
        var restored = new DeviceBlockEntity(helper.absolutePos(FIRST), devices[0].getBlockState());
        restored.load(devices[0].saveWithoutMetadata());
        helper.assertTrue(restored.number().equals(devices[0].number()) && restored.target().equals(devices[0].target()), "Device configuration not persisted");
        var directory = DeviceDirectory.get(helper.getLevel().getServer());
        var reloaded = DeviceDirectory.load(directory.save(new CompoundTag()));
        helper.assertTrue(reloaded.find(helper.getLevel().getServer(), devices[0].number()) == devices[0], "Saved directory did not resolve the device");
        devices[0].call(devices[1]);
        var afterRestart = new DeviceBlockEntity(helper.absolutePos(FIRST), devices[0].getBlockState());
        afterRestart.load(devices[0].saveWithoutMetadata());
        helper.assertTrue(afterRestart.state() == DeviceBlockEntity.IDLE, "Live calls must not persist across restart");
        devices[0].endCall();
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 140)
    public static void doorRelayResets(GameTestHelper helper) {
        var devices = pair(helper);
        helper.assertTrue(!devices[1].unlockPeer(), "Unconnected unlock accepted");
        devices[0].call(devices[1]);
        devices[1].answer();
        helper.assertTrue(!devices[0].unlockPeer(), "Indoor wallpad must not expose an entrance relay");
        helper.assertTrue(devices[1].unlockPeer(), "Connected receiver could not unlock entrance");
        helper.assertBlockProperty(FIRST, DeviceBlock.POWERED, true);
        helper.runAfterDelay(110, () -> {
            helper.assertBlockProperty(FIRST, DeviceBlock.POWERED, false);
            devices[0].endCall();
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void breakingDeviceEndsCall(GameTestHelper helper) {
        var devices = pair(helper);
        devices[0].call(devices[1]);
        devices[1].answer();
        String oldNumber = devices[0].number();
        helper.setBlock(FIRST, net.minecraft.world.level.block.Blocks.AIR);
        helper.assertTrue(devices[1].state() == DeviceBlockEntity.IDLE, "Remote device still connected after removal");
        helper.assertTrue(DeviceDirectory.get(helper.getLevel().getServer()).find(helper.getLevel().getServer(), oldNumber) == null, "Removed device still registered");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 660)
    public static void unansweredCallTimesOut(GameTestHelper helper) {
        var devices = pair(helper);
        devices[0].call(devices[1]);
        helper.runAfterDelay(625, () -> {
            helper.assertTrue(devices[0].state() == DeviceBlockEntity.IDLE && devices[1].state() == DeviceBlockEntity.IDLE, "Unanswered call did not time out");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void videoCapturesRealWorldAndRoundTrips(GameTestHelper helper) {
        var devices = pair(helper);
        helper.assertTrue(DeviceVideo.hasDisplay(devices[1]) && !DeviceVideo.hasDisplay(devices[0]), "Wrong video-capable device types");
        BlockPos sample = new BlockPos(2, 1, 1);
        helper.setBlock(sample, net.minecraft.world.level.block.Blocks.GOLD_BLOCK);
        VideoFrame first = DeviceVideo.capture(devices[0], 42);
        int gold = net.minecraft.world.level.block.Block.getId(net.minecraft.world.level.block.Blocks.GOLD_BLOCK.defaultBlockState());
        helper.assertTrue(first.blocks().stream().anyMatch(cell -> cell.pos().equals(new BlockPos(0, 0, -1)) && cell.state() == gold), "Camera did not capture actual gold block");
        helper.setBlock(sample, net.minecraft.world.level.block.Blocks.DIAMOND_BLOCK);
        VideoFrame second = DeviceVideo.capture(devices[0], 42);
        int diamond = net.minecraft.world.level.block.Block.getId(net.minecraft.world.level.block.Blocks.DIAMOND_BLOCK.defaultBlockState());
        helper.assertTrue(second.blocks().stream().anyMatch(cell -> cell.pos().equals(new BlockPos(0, 0, -1)) && cell.state() == diamond), "Camera did not reflect the world change");
        var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            VideoFrame.encode(second, buffer);
            VideoFrame decoded = VideoFrame.decode(buffer);
            helper.assertTrue(decoded.equals(second), "Video packet round trip changed the scene");
            helper.assertTrue(decoded.blocks().size() <= VideoFrame.MAX_BLOCKS, "Camera scene exceeded packet bounds");
        } finally { buffer.release(); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void callerCannotWatchBeforeAnswer(GameTestHelper helper) {
        var first = place(helper, FIRST, HomeSystems.VIDEO_PHONE.get());
        var second = place(helper, SECOND, HomeSystems.GUARD_STATION.get());
        first.configure(number(helper, FIRST), number(helper, SECOND));
        second.configure(number(helper, SECOND), number(helper, FIRST));
        helper.assertTrue(!DeviceVideo.canView(first) && !DeviceVideo.canView(second), "Idle camera allowed");
        first.call(second);
        helper.assertTrue(!DeviceVideo.canView(first), "Caller may not watch an unanswered indoor device");
        helper.assertTrue(DeviceVideo.canView(second), "Receiver must be able to preview the visitor");
        second.answer();
        helper.assertTrue(DeviceVideo.canView(first) && DeviceVideo.canView(second), "Accepted call must enable both displays");
        first.endCall();
        helper.assertTrue(!DeviceVideo.canView(first) && !DeviceVideo.canView(second), "Ended camera allowed");
        helper.succeed();
    }
}
