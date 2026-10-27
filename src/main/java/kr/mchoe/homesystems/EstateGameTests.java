package kr.mchoe.homesystems;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(HomeSystems.MODID)
@PrefixGameTestTemplate(false)
public final class EstateGameTests {
    private static final UUID OWNER = new UUID(10, 20);
    private static final String DIM = "minecraft:overworld";
    /** One common test estate contains the existing device fixtures; tests below use independent data. */
    static void encloseFixtures(GameTestHelper helper) {
        var data = EstateData.get(helper.getLevel().getServer());
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        if (data.atBlock(DIM, pos) != null) return;
        int x = Math.floorDiv(pos.getX(), EstateData.UNIT), z = Math.floorDiv(pos.getZ(), EstateData.UNIT);
        helper.assertTrue(data.create(OWNER, DIM, "GameTest fixtures", x - 16, z - 16, x + 16, z + 16).equals("created"), "Could not create fixture estate");
    }

    @GameTest(template = "empty")
    public static void mergingRemovesOnlySharedEdgesAndSplitRestoresThem(GameTestHelper helper) {
        EstateData d = new EstateData();
        d.create(OWNER, DIM, "A", 0, 0, 1, 1); d.create(OWNER, DIM, "B", 2, 0, 3, 1);
        var a = d.at(DIM, 0, 0); var b = d.at(DIM, 2, 0);
        helper.assertTrue(d.boundary(DIM, 1, 0, 1, 0), "Separate estates lost their shared border");
        helper.assertTrue(d.merge(OWNER, false, a.id(), b.id()).equals("merged"), "Merge failed");
        helper.assertTrue(!d.boundary(DIM, 1, 0, 1, 0), "Internal shared edge remained after merge");
        helper.assertTrue(d.boundary(DIM, 0, 0, -1, 0) && d.boundary(DIM, 3, 1, 1, 0), "External perimeter disappeared");
        helper.assertTrue(d.at(DIM, 2, 2) == null, "Merge filled unclaimed land");
        helper.assertTrue(d.split(OWNER, false, b.id()).equals("split") && d.boundary(DIM, 1, 0, 1, 0), "Split did not restore exact shared border");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disconnectedMergeKeepsGapsAndLShape(GameTestHelper helper) {
        var d = new EstateData();
        d.create(OWNER, DIM, "West", -2, -2, -1, 1); d.create(OWNER, DIM, "East", 0, 0, 2, 1);
        var a = d.at(DIM, -1, 0); var b = d.at(DIM, 0, 0);
        d.merge(OWNER, false, a.id(), b.id());
        helper.assertTrue(!d.boundary(DIM, -1, 0, 1, 0), "Touching side of L shape retained border");
        helper.assertTrue(d.boundary(DIM, -1, -1, 1, 0) && d.at(DIM, 0, -1) == null, "L shape became a bounding rectangle");
        d.create(OWNER, DIM, "Island", 5, 0, 5, 0);
        d.merge(OWNER, false, a.id(), d.at(DIM, 5, 0).id());
        helper.assertTrue(d.at(DIM, 4, 0) == null && d.boundary(DIM, 5, 0, -1, 0), "Disconnected merge filled gap or removed island perimeter");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void ownershipOverlapAndSaveAreEnforced(GameTestHelper helper) {
        var d = new EstateData();
        helper.assertTrue(d.create(OWNER, DIM, "우리 단지", -1, -1, 1, 1).equals("created"), "Creation failed");
        helper.assertTrue(d.create(OWNER, DIM, "겹침", 1, 1, 2, 2).equals("overlap"), "Overlap accepted");
        helper.assertTrue(d.create(OWNER, DIM, " ", 4, 4, 4, 4).equals("bad_name"), "Blank name accepted");
        var part = d.at(DIM, -1, -1); var stranger = new UUID(30, 40);
        helper.assertTrue(d.rename(stranger, false, part.id(), "steal").equals("not_owner"), "Non-owner renamed estate");
        helper.assertTrue(d.delete(stranger, false, part.id()).equals("not_owner"), "Non-owner deleted parcel");
        d.rename(OWNER, false, part.id(), "새 이름");
        var saved = EstateData.load(d.save(new net.minecraft.nbt.CompoundTag()));
        helper.assertTrue(saved.estate(part.estate()).name().equals("새 이름") && saved.at(DIM, 1, 1).id().equals(part.id()), "Estate/name/parcel not persisted");
        helper.assertTrue(saved.atBlock(DIM, new BlockPos(-1, 5, -1)) != null && saved.atBlock(DIM, new BlockPos(32, 5, 0)) == null, "Negative chunk or boundary conversion wrong");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void deviceOutsideEstateCannotConfigureOrCall(GameTestHelper helper) {
        encloseFixtures(helper);
        BlockPos p = new BlockPos(2, 1, 2); helper.setBlock(p, HomeSystems.WALLPAD.get());
        var device = (DeviceBlockEntity)helper.getLevel().getBlockEntity(helper.absolutePos(p));
        String number = "6" + Integer.toUnsignedString(device.getBlockPos().hashCode());
        helper.assertTrue(device.configure(number, ""), "Device inside estate could not configure");
        var outside = new DeviceBlockEntity(new BlockPos(20000000, 5, 20000000), HomeSystems.LOBBY_PHONE.get().defaultBlockState());
        outside.setLevel(helper.getLevel()); outside.load(device.saveWithoutMetadata());
        helper.assertTrue(!outside.configure("999999999999", "") && !device.call(outside), "Outside device became operational");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void outsideRelayCannotJoinNetwork(GameTestHelper helper) {
        encloseFixtures(helper);
        BlockPos p = new BlockPos(2, 1, 2); helper.setBlock(p, HomeSystems.WALLPAD.get());
        var controller = (DeviceBlockEntity)helper.getLevel().getBlockEntity(helper.absolutePos(p));
        var outside = new HomeRelayEntity(new BlockPos(20000000, 5, 20000000), HomeSystems.GAS_RELAY.get().defaultBlockState());
        outside.setLevel(helper.getLevel());
        helper.assertTrue(!controller.homeNetwork().add(outside) && !outside.linked(), "Relay outside estate joined home network");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void estateMembershipChangesOnMergeAndSplit(GameTestHelper helper) {
        var d = new EstateData();
        d.create(OWNER, DIM, "A", 0, 0, 0, 0); d.create(OWNER, DIM, "B", 1, 0, 1, 0);
        var a = d.at(DIM, 0, 0); var b = d.at(DIM, 1, 0);
        var first = new BlockPos(15, 8, 5); var second = new BlockPos(16, 8, 5);
        helper.assertTrue(!d.same(DIM, first, DIM, second), "Separate adjacent estates could communicate");
        d.merge(OWNER, false, a.id(), b.id());
        helper.assertTrue(d.same(DIM, first, DIM, second), "Merged estates could not communicate");
        helper.assertTrue(!d.same(DIM, first, "minecraft:the_nether", second), "Estate leaked across dimensions");
        d.split(OWNER, false, b.id());
        helper.assertTrue(!d.same(DIM, first, DIM, second), "Split estates remained connected");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dashboardPacketPreservesMapAndParcels(GameTestHelper helper) {
        var d = new EstateData();d.create(OWNER, DIM, "테스트 단지", -2, -3, 1, 0);
        var e = d.estates().iterator().next();
        int[] colors = new int[DashboardNetwork.WIDTH * DashboardNetwork.HEIGHT]; java.util.Arrays.fill(colors, 0x72a44b);
        var source = new DashboardNetwork.State(12, DIM, -1, -2, colors, java.util.List.of(new DashboardNetwork.Group(e.id(), e.name(), true)), java.util.List.copyOf(d.parcels()), "created");
        var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            DashboardNetwork.State.encode(source, buffer); var restored = DashboardNetwork.State.decode(buffer);
            helper.assertTrue(restored.container() == 12 && java.util.Arrays.equals(colors, restored.colors()) && restored.parcels().equals(source.parcels())
                && restored.groups().equals(source.groups()), "Dashboard map packet did not round trip");
        } finally { buffer.release(); }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "estate_transition")
    public static void splittingStopsRealCallsAndRelayOutput(GameTestHelper helper) {
        var level = helper.getLevel(); var data = EstateData.get(level.getServer());
        // Batches run sequentially. Remove only the explicitly owned fixture estate, never user claims.
        for (var p : data.parcels()) {
            var e = data.estate(p.estate());
            if (e != null && e.owner().equals(OWNER) && e.name().equals("GameTest fixtures")) data.delete(OWNER, false, p.id());
        }
        BlockPos base = helper.absolutePos(BlockPos.ZERO);
        int boundaryX = (Math.floorDiv(base.getX(), EstateData.UNIT) + 1) * EstateData.UNIT;
        BlockPos firstPos = new BlockPos(boundaryX - 1, base.getY() + 1, base.getZ() + 1);
        BlockPos secondPos = firstPos.east(), relayPos = secondPos.above();
        int x = Math.floorDiv(firstPos.getX(), EstateData.UNIT), z = Math.floorDiv(firstPos.getZ(), EstateData.UNIT);
        UUID firstParcel = null, secondParcel = null;
        try {
            helper.assertTrue(data.create(OWNER, DIM, "Test west", x, z, x, z).equals("created"), "West parcel failed");
            firstParcel = data.at(DIM, x, z).id();
            helper.assertTrue(data.create(OWNER, DIM, "Test east", x + 1, z, x + 1, z).equals("created"), "East parcel failed");
            secondParcel = data.at(DIM, x + 1, z).id();
            level.setBlock(firstPos, HomeSystems.WALLPAD.get().defaultBlockState(), 3);
            level.setBlock(secondPos, HomeSystems.LOBBY_PHONE.get().defaultBlockState(), 3);
            level.setBlock(relayPos, HomeSystems.GAS_RELAY.get().defaultBlockState(), 3);
            var first = (DeviceBlockEntity)level.getBlockEntity(firstPos);
            var second = (DeviceBlockEntity)level.getBlockEntity(secondPos);
            var relay = (HomeRelayEntity)level.getBlockEntity(relayPos);
            String n = Integer.toUnsignedString(firstPos.hashCode());
            helper.assertTrue(first.configure("1" + n, "") && second.configure("2" + n, ""), "Device registration failed");
            helper.assertTrue(!first.call(second) && !first.homeNetwork().add(relay), "Separate estates communicated");
            data.merge(OWNER, false, firstParcel, secondParcel);
            helper.assertTrue(first.call(second) && second.answer() && first.homeNetwork().add(relay), "Merged estate failed to connect");
            first.homeNetwork().toggle(2);
            helper.assertTrue(level.getBlockState(relayPos).getValue(HomeRelayBlock.POWERED), "Gas relay did not turn on");
            data.split(OWNER, false, secondParcel);
            DeviceBlockEntity.serverTick(level, firstPos, first.getBlockState(), first);
            HomeRelayEntity.serverTick(level, relayPos, relay.getBlockState(), relay);
            helper.assertTrue(first.state() == DeviceBlockEntity.IDLE && second.state() == DeviceBlockEntity.IDLE, "Split did not stop active call");
            helper.assertTrue(!level.getBlockState(relayPos).getValue(HomeRelayBlock.POWERED), "Split left remote gas relay powered");
            helper.assertTrue(!first.call(second), "Cross-estate call restarted after split");
            helper.succeed();
        } finally {
            level.setBlock(firstPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(secondPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(relayPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            if (firstParcel != null) data.delete(OWNER, false, firstParcel);
            if (secondParcel != null) data.delete(OWNER, false, secondParcel);
        }
    }
}
