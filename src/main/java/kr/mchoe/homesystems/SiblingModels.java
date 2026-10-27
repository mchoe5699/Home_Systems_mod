package kr.mchoe.homesystems;

import com.google.gson.*;
import java.util.*;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

/** Server-owned family catalog. Visuals for each slot are supplied by the matching resource pack. */
public final class SiblingModels extends SimpleJsonResourceReloadListener {
    public static final int MAX_SLOT = 15;
    public record Option(int slot, Component name) {}
    private static volatile Map<ResourceLocation, List<Option>> families = Map.of();
    public SiblingModels() { super(new Gson(), "sibling_models"); }
    public static List<Option> options(Block block) {
        var id = ForgeRegistries.BLOCKS.getKey(block);
        return families.getOrDefault(id, List.of(new Option(0, block.getName())));
    }
    static List<Option> parse(JsonElement json) {
        JsonArray array = json.getAsJsonObject().getAsJsonArray("models");
        if (array == null || array.size() < 2 || array.size() > MAX_SLOT + 1) throw new IllegalArgumentException("Expected 2-16 models");
        List<Option> result = new ArrayList<>(); Set<Integer> slots = new HashSet<>();
        for (JsonElement element : array) {
            JsonObject entry = element.getAsJsonObject();
            JsonPrimitive raw = entry.getAsJsonPrimitive("slot");
            if (raw == null || !raw.isNumber() || !raw.getAsString().matches("[0-9]{1,2}")) throw new IllegalArgumentException("Invalid slot");
            int slot = raw.getAsInt(); String name = entry.get("name").getAsString().strip();
            if (slot < 0 || slot > MAX_SLOT || !slots.add(slot) || name.isEmpty() || name.length() > 64 || name.codePoints().anyMatch(Character::isISOControl)) throw new IllegalArgumentException("Invalid/duplicate slot or name");
            result.add(new Option(slot, Component.literal(name)));
        }
        if (!slots.contains(0)) throw new IllegalArgumentException("Slot zero is required for the original model");
        return List.copyOf(result);
    }
    @Override protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation,List<Option>> next = new HashMap<>();
        resources.forEach((id,json) -> {
            try {
                if (!(ForgeRegistries.BLOCKS.getValue(id) instanceof DeviceBlock)) throw new IllegalArgumentException("Unknown device block");
                next.put(id, parse(json));
            } catch (RuntimeException exception) { LogUtils.getLogger().warn("Ignoring sibling model family {}: {}", id, exception.getMessage()); }
        });
        families = Map.copyOf(next);
    }
    static void clear() { families = Map.of(); }
}
