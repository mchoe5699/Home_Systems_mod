package kr.mchoe.homesystems;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/** Tool selection is tied to a device identity so replacing a block cannot hijack a link. */
public final class PhysicalLinkItem extends Item {
    private final boolean home;
    public PhysicalLinkItem(boolean home,Properties properties){super(properties);this.home=home;}
    @Override public void appendHoverText(ItemStack stack,net.minecraft.world.level.Level level,java.util.List<Component> lines,net.minecraft.world.item.TooltipFlag flag){
        lines.add(Component.translatable("tooltip.homesystems."+(home?"home_link":"door_link")));
        lines.add(Component.translatable("tooltip.homesystems.link_unpair"));
    }
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context){return useOn(context);}
    @Override public InteractionResult useOn(UseOnContext context){
        var player=context.getPlayer();if(player==null)return InteractionResult.PASS;
        if(context.getLevel().isClientSide)return InteractionResult.SUCCESS;
        var level=(ServerLevel)context.getLevel();var pos=context.getClickedPos();var stack=context.getItemInHand();
        if(!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,context.getClickedFace(),stack))return InteractionResult.FAIL;
        var entity=level.getBlockEntity(pos);String result;
        boolean selectable=entity instanceof DeviceBlockEntity d&&(home?DeviceRouting.indoor(d):DeviceRouting.indoor(d)||DeviceRouting.lobby(d));
        if(selectable){
            var d=(DeviceBlockEntity)entity;
            if(!d.canConfigure(player))result="owner_only";
            else if(!EstateData.playerMayUse((net.minecraft.server.level.ServerPlayer)player,d))result="outside_estate";
            else if(home&&!d.addressConfigured())result="configure_first";
            else{
                var tag=stack.getOrCreateTag();tag.putString("Dimension",level.dimension().location().toString());tag.putLong("SourcePos",pos.asLong());tag.putUUID("SourceId",d.homeNetwork().id());
                result=home?"home_source_selected":"door_source_selected";
            }
        }else{
            var tag=stack.getTag();DeviceBlockEntity source=null;
            if(tag!=null&&tag.contains("SourcePos")&&tag.hasUUID("SourceId")&&level.dimension().location().toString().equals(tag.getString("Dimension"))){
                BlockPos sourcePos=BlockPos.of(tag.getLong("SourcePos"));
                if(level.hasChunkAt(sourcePos)&&level.getBlockEntity(sourcePos) instanceof DeviceBlockEntity d&&d.homeNetwork().id().equals(tag.getUUID("SourceId")))source=d;
            }
            if(source==null)result=home?"select_indoor":"select_door_controller";
            else if(!source.canConfigure(player))result="owner_only";
            else if(home){
                if(!(entity instanceof DeviceBlockEntity door)||!DeviceRouting.door(door))result="select_door_device";
                else if(!door.canConfigure(player))result="owner_only";
                else if(!DeviceRouting.indoor(source)||!EstateData.playerMayUse((net.minecraft.server.level.ServerPlayer)player,door))result="outside_estate";
                else if(player.isShiftKeyDown())result=door.links().unlinkHome(source)?"home_link_removed":"link_missing";
                else result=door.links().linkHome(source)?"home_link_added":"physical_link_invalid";
            }else{
                if(!source.links().doorController())result="select_door_controller";
                else if(player.isShiftKeyDown())result=source.links().unlinkDoor(pos)?"door_link_removed":"link_missing";
                else result=source.links().linkDoor(pos)?"door_link_added":"physical_link_invalid";
            }
        }
        player.displayClientMessage(Component.translatable("message.homesystems."+result),true);return InteractionResult.CONSUME;
    }
}
