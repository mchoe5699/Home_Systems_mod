package kr.mchoe.homesystems;

import java.util.function.Consumer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Use the same continuous product surfaces in inventories and in the world. */
public final class DeviceItem extends BlockItem {
    public DeviceItem(Block block,Properties properties){super(block,properties);}
    @Override @OnlyIn(Dist.CLIENT)
    public void initializeClient(Consumer<IClientItemExtensions> consumer){
        consumer.accept(new IClientItemExtensions(){
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){
                return kr.mchoe.homesystems.client.SmoothItemRenderer.instance();
            }
        });
    }
}
