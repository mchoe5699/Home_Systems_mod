package kr.mchoe.homesystems.client;

import kr.mchoe.homesystems.HomeSystems;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@Mod.EventBusSubscriber(modid = HomeSystems.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {
    @SubscribeEvent public static void renderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(HomeSystems.DEVICE.get(),ProductRenderer::new);}
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(HomeSystems.SIBLING_MENU.get(), SiblingScreen::new);
            MenuScreens.register(HomeSystems.DEVICE_MENU.get(), DeviceScreen::new);
            MenuScreens.register(HomeSystems.DASHBOARD_MENU.get(), DashboardScreen::new);
        });
    }
}
