package kr.mchoe.homesystems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

public class DeviceBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty MODEL = net.minecraft.world.level.block.state.properties.IntegerProperty.create("model", 0, SiblingModels.MAX_SLOT);
    public static final BooleanProperty OFFHOOK = BooleanProperty.create("offhook");
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final VoxelShape NORTH = Block.box(2, 1, 12, 14, 15, 16);
    private static final VoxelShape SOUTH = Block.box(2, 1, 0, 14, 15, 4);
    private static final VoxelShape WEST = Block.box(12, 1, 2, 16, 15, 14);
    private static final VoxelShape EAST = Block.box(0, 1, 2, 4, 15, 14);

    public DeviceBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false).setValue(MODEL, 0).setValue(OFFHOOK, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, POWERED, MODEL, OFFHOOK); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        return defaultBlockState().setValue(FACING, face.getAxis().isHorizontal() ? face : context.getHorizontalDirection().getOpposite());
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override public BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        var d=ProductDesign.of(state);
        double x0=d.x0,y0=d.y0,z0=d.z0,x1=d.x1,y1=d.y1,z1=d.z1;
        if(d.desktop){y0=0;z0=3;z1=16;}
        return switch(state.getValue(FACING)){
            case EAST->Block.box(16-z1,y0,x0,16-z0,y1,x1);
            case SOUTH->Block.box(16-x1,y0,16-z1,16-x0,y1,16-z0);
            case WEST->Block.box(z0,y0,16-x1,z1,y1,16-x0);
            default->Block.box(x0,y0,z0,x1,y1,z1);
        };
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new DeviceBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, HomeSystems.DEVICE.get(), DeviceBlockEntity::serverTick);
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).is(HomeSystems.SIBLING_SELECTOR.get())||player.getItemInHand(hand).is(HomeSystems.NETWORK_LINKER.get())||player.getItemInHand(hand).is(HomeSystems.DOOR_LINK.get())||player.getItemInHand(hand).is(HomeSystems.HOME_LINK.get())) return InteractionResult.PASS;
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof DeviceBlockEntity device) {
            if (!EstateData.playerMayUse(serverPlayer, device)) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.homesystems.outside_estate"), true);
                return InteractionResult.CONSUME;
            }
            NetworkHooks.openScreen(serverPlayer, device, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (level instanceof ServerLevel serverLevel && placer instanceof Player player && level.getBlockEntity(pos) instanceof DeviceBlockEntity device) {
            device.setOwner(player.getUUID());
        }
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && level instanceof ServerLevel serverLevel && level.getBlockEntity(pos) instanceof DeviceBlockEntity device) {
            DeviceDirectory.get(serverLevel.getServer()).remove(device);
            device.homeNetwork().disconnectAll();
            level.updateNeighborsAt(pos, this);
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
    @Override public boolean isSignalSource(BlockState state) { return true; }
    @Override public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) { return state.getValue(POWERED) ? 15 : 0; }
    @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, false), 3);
            level.updateNeighborsAt(pos, this);
        }
    }
}
