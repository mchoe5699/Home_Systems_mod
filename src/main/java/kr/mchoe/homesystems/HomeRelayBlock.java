package kr.mchoe.homesystems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class HomeRelayBlock extends BaseEntityBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private final int channel;
    public HomeRelayBlock(int channel, Properties properties) {
        super(properties);
        this.channel = channel;
        registerDefaultState(stateDefinition.any().setValue(POWERED, false));
    }
    public int channel() { return channel; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(POWERED); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new HomeRelayEntity(pos, state); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public boolean isSignalSource(BlockState state) { return true; }
    @Override public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) { return state.getValue(POWERED) ? 15 : 0; }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, HomeSystems.RELAY.get(), HomeRelayEntity::serverTick);
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide && placer instanceof Player player && level.getBlockEntity(pos) instanceof HomeRelayEntity relay) relay.setOwner(player.getUUID());
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).is(HomeSystems.NETWORK_LINKER.get())) return InteractionResult.PASS;
        if (!level.isClientSide) player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.homesystems.relay_hint"), true);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof HomeRelayEntity relay) {
            relay.unlink(false);
            level.updateNeighborsAt(pos, this);
        }
        super.onRemove(state, level, pos, next, moving);
    }
}
