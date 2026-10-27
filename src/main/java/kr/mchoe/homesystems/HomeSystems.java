package kr.mchoe.homesystems;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import static net.minecraft.commands.Commands.*;

@Mod(HomeSystems.MODID)
public final class HomeSystems {
    public static final String MODID = "homesystems";
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<Block> LOBBY_PHONE = device("lobby_phone");
    public static final RegistryObject<Block> VIDEO_PHONE = device("video_phone");
    public static final RegistryObject<Block> SHT_308 = device("sht_308");
    public static final RegistryObject<Block> INTERPHONE = device("interphone");
    public static final RegistryObject<Block> WALLPAD = device("wallpad");
    public static final RegistryObject<Block> GUARD_STATION = device("guard_station");
    public static final RegistryObject<Block> DOOR_CAMERA = device("door_camera");
    public static final RegistryObject<Block> DOOR_PHONE = device("door_phone");
    public static final RegistryObject<Block> LIGHT_RELAY = relay("light_relay", 0);
    public static final RegistryObject<Block> DOOR_RELAY = relay("door_relay", 1);
    public static final RegistryObject<Block> GAS_RELAY = relay("gas_relay", 2);
    public static final RegistryObject<Block> UTILITY_RELAY = relay("utility_relay", 3);
    public static final RegistryObject<Item> NETWORK_LINKER = ITEMS.register("network_linker", () -> new NetworkLinker(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> ESTATE_DASHBOARD = ITEMS.register("estate_dashboard", () -> new DashboardItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> DOOR_LINK = ITEMS.register("door_link", () -> new PhysicalLinkItem(false,new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> SIBLING_SELECTOR = ITEMS.register("sibling_selector", () -> new SiblingSelectorItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> HOME_LINK = ITEMS.register("home_link", () -> new PhysicalLinkItem(true,new Item.Properties().stacksTo(1)));
    public static final RegistryObject<BlockEntityType<HomeRelayEntity>> RELAY = ENTITIES.register("home_relay", () ->
        BlockEntityType.Builder.of(HomeRelayEntity::new, LIGHT_RELAY.get(), DOOR_RELAY.get(), GAS_RELAY.get(), UTILITY_RELAY.get()).build(null));
    public static final RegistryObject<BlockEntityType<DeviceBlockEntity>> DEVICE = ENTITIES.register("device", () ->
        BlockEntityType.Builder.of(DeviceBlockEntity::new, LOBBY_PHONE.get(), VIDEO_PHONE.get(),
            INTERPHONE.get(), WALLPAD.get(), GUARD_STATION.get(), DOOR_CAMERA.get(), DOOR_PHONE.get(), SHT_308.get()).build(null));
    public static final RegistryObject<MenuType<DeviceMenu>> DEVICE_MENU = MENUS.register("device", () ->
        new MenuType<>((IContainerFactory<DeviceMenu>) DeviceMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<SiblingMenu>> SIBLING_MENU = MENUS.register("sibling_selector", () -> new MenuType<>((IContainerFactory<SiblingMenu>) SiblingMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<DashboardMenu>> DASHBOARD_MENU = MENUS.register("estate_dashboard", () -> new MenuType<>(DashboardMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("devices", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.homesystems"))
        .icon(() -> WALLPAD.get().asItem().getDefaultInstance())
        .displayItems((parameters, output) -> ITEMS.getEntries().forEach(item -> output.accept(item.get())))
        .build());

    private static RegistryObject<Block> device(String id) {
        RegistryObject<Block> block = BLOCKS.register(id, () -> new DeviceBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).strength(2.5F).sound(net.minecraft.world.level.block.SoundType.METAL)
            .noOcclusion().lightLevel(state -> state.getValue(DeviceBlock.POWERED) ? 7 : 2)));
        ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static RegistryObject<Block> relay(String id, int channel) {
        RegistryObject<Block> block = BLOCKS.register(id, () -> new HomeRelayBlock(channel, BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).strength(2F).sound(net.minecraft.world.level.block.SoundType.METAL)
            .lightLevel(state -> state.getValue(HomeRelayBlock.POWERED) ? 8 : 0)));
        ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    public HomeSystems() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
        MENUS.register(bus);
        TABS.register(bus);
        DeviceNetwork.register();
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.AddReloadListenerEvent event) -> event.addListener(new SiblingModels()));
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.server.ServerStoppedEvent event) -> SiblingModels.clear());
        MinecraftForge.EVENT_BUS.addListener(this::commands);
        MinecraftForge.EVENT_BUS.addListener(this::chat);
        MinecraftForge.EVENT_BUS.addListener(this::serverTick);
    }
    private void serverTick(TickEvent.ServerTickEvent event){if(event.phase==TickEvent.Phase.END)DoorControlData.get(event.getServer()).tick(event.getServer());}

    private void chat(ServerChatEvent event) {
        if (DeviceDirectory.get(event.getPlayer().server).sendMessage(event.getPlayer(), event.getRawText())) event.setCanceled(true);
    }

    private void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(literal("intercom").then(argument("message", StringArgumentType.greedyString())
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                if (!DeviceDirectory.get(player.server).sendMessage(player, StringArgumentType.getString(context, "message"))) {
                    context.getSource().sendFailure(Component.translatable("message.homesystems.no_connected_device"));
                    return 0;
                }
                return 1;
            })));
    }
}
