package kr.mchoe.homesystems;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class SiblingMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final Level level;
    private final UUID identity;
    private final List<SiblingModels.Option> options;
    public SiblingMenu(int id, Inventory inventory, FriendlyByteBuf buffer) { this(id, inventory, buffer.readBlockPos(), readOptions(buffer)); }
    public SiblingMenu(int id, Inventory inventory, BlockPos pos, List<SiblingModels.Option> options) {
        super(HomeSystems.SIBLING_MENU.get(), id); this.pos = pos.immutable(); this.level=inventory.player.level(); this.options=List.copyOf(options);
        var device=device(); identity=device==null?null:device.homeNetwork().id();
    }
    private static List<SiblingModels.Option> readOptions(FriendlyByteBuf buffer) {
        int count=buffer.readVarInt(); if(count<2||count>16)throw new IllegalArgumentException("Invalid sibling model count");
        List<SiblingModels.Option> list=new ArrayList<>(); Set<Integer> slots=new HashSet<>();
        for(int i=0;i<count;i++){int slot=buffer.readVarInt();if(slot<0||slot>15||!slots.add(slot))throw new IllegalArgumentException("Invalid model slot");list.add(new SiblingModels.Option(slot,buffer.readComponent()));}
        return list;
    }
    public DeviceBlockEntity device(){return level.getBlockEntity(pos) instanceof DeviceBlockEntity device?device:null;}
    public List<SiblingModels.Option> options(){return options;}
    public int selected(){var device=device();return device==null?-1:device.getBlockState().getValue(DeviceBlock.MODEL);}
    @Override public ItemStack quickMoveStack(Player player,int index){return ItemStack.EMPTY;}
    @Override public boolean stillValid(Player player){
        var device=device();
        return device!=null&&!device.isRemoved()&&level==player.level()&&identity!=null&&identity.equals(device.homeNetwork().id())
            &&player.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=64
            &&(player.getMainHandItem().is(HomeSystems.SIBLING_SELECTOR.get())||player.getOffhandItem().is(HomeSystems.SIBLING_SELECTOR.get()))
            &&(level.isClientSide||(device.canConfigure(player)&&device.state()==DeviceBlockEntity.IDLE&&player instanceof net.minecraft.server.level.ServerPlayer server&&EstateData.playerMayUse(server,device)));
    }
    @Override public boolean clickMenuButton(Player player,int slot){
        if(level.isClientSide||!stillValid(player))return false;
        var device=device();
        // Revalidate the catalog after /reload; stale screens must never select a removed/remapped model.
        if(!SiblingModels.options(device.getBlockState().getBlock()).equals(options)||options.stream().noneMatch(option->option.slot()==slot))return false;
        if(selected()==slot)return true;
        var state=device.getBlockState();
        level.setBlock(pos,state.setValue(DeviceBlock.MODEL,slot),3);
        device.changed();
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.homesystems.sibling_changed",options.stream().filter(option->option.slot()==slot).findFirst().orElseThrow().name()),true);
        return true;
    }
}
