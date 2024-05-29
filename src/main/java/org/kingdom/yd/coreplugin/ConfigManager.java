package org.kingdom.yd.coreplugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

public class ConfigManager {
    private JavaPlugin plugin;
    private File configFile;
    private JsonObject configData;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.json");
        loadConfig();
    }

    private void loadConfig() {
        if (!configFile.exists()) {
            createDefaultConfig();
        }
        try (FileReader reader = new FileReader(configFile)) {
            Gson gson = new Gson();
            this.configData = gson.fromJson(reader, JsonObject.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createDefaultConfig() {
        JsonObject defaultConfig = new JsonObject();
        JsonObject guiTitles = new JsonObject();
        guiTitles.addProperty("core_system", "CORE SYSTEM");
        guiTitles.addProperty("attribute_selection", "Attribute Selection");
        defaultConfig.add("gui_titles", guiTitles);

        JsonObject guiItems = new JsonObject();
        JsonObject slots = new JsonObject();
        JsonObject defaultSlot = new JsonObject();
        defaultSlot.addProperty("material", "BLACK_STAINED_GLASS_PANE");
        defaultSlot.addProperty("display_name", "Select an attribute");
        defaultSlot.add("lore", new Gson().toJsonTree(Arrays.asList("This is a default slot.")));
        slots.add("default", defaultSlot);

        JsonObject lockedSlot = new JsonObject();
        lockedSlot.addProperty("material", "RED_STAINED_GLASS_PANE");
        lockedSlot.addProperty("display_name", "Locked");
        lockedSlot.add("lore", new Gson().toJsonTree(Arrays.asList("Unlock by selecting all primary attributes.")));
        slots.add("locked", lockedSlot);

        guiItems.add("slots", slots);

        JsonObject attributes = new JsonObject();
        attributes.add("MAX_HEALTH", createAttributeItemConfig("RED_DYE", "Health", Arrays.asList("Increase your health")));
        attributes.add("HEALTH_REGENERATION", createAttributeItemConfig("ORANGE_DYE", "Health Regeneration", Arrays.asList("Increase your health regeneration")));
        attributes.add("MAX_MANA", createAttributeItemConfig("YELLOW_DYE", "Mana", Arrays.asList("Increase your mana")));
        attributes.add("MANA_REGENERATION", createAttributeItemConfig("GREEN_DYE", "Mana Regeneration", Arrays.asList("Increase your mana regeneration")));
        attributes.add("STAMINA_REGENERATION", createAttributeItemConfig("BLUE_DYE", "Stamina Regeneration", Arrays.asList("Increase your stamina regeneration")));
        attributes.add("KNOCKBACK_RESISTANCE", createAttributeItemConfig("PURPLE_DYE", "Knockback Resistance", Arrays.asList("Increase your knockback resistance")));
        attributes.add("COOLDOWN_REDUCTION", createAttributeItemConfig("WHITE_DYE", "Cooldown Reduction", Arrays.asList("Reduce your cooldown time")));

        guiItems.add("attributes", attributes);
        defaultConfig.add("gui_items", guiItems);

        try (FileWriter writer = new FileWriter(configFile)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(defaultConfig, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private JsonObject createAttributeItemConfig(String material, String displayName, Iterable<String> lore) {
        JsonObject attribute = new JsonObject();
        attribute.addProperty("material", material);
        attribute.addProperty("display_name", displayName);
        attribute.add("lore", new Gson().toJsonTree(lore));
        return attribute;
    }

    public String getGUITitle(String key) {
        JsonObject guiTitles = configData.getAsJsonObject("gui_titles");
        if (guiTitles != null && guiTitles.has(key)) {
            return guiTitles.get(key).getAsString();
        }
        throw new IllegalArgumentException("GUI title '" + key + "' not found in config");
    }

    public ItemStack getSlotItem(String state) {
        JsonObject slots = configData.getAsJsonObject("gui_items").getAsJsonObject("slots");
        if (slots != null && slots.has(state)) {
            JsonObject slotObject = slots.getAsJsonObject(state);
            if (slotObject.has("material")) {
                return createItem(slotObject.get("material").getAsString(), slotObject.get("display_name").getAsString(), slotObject.getAsJsonArray("lore"));
            }
        }
        throw new IllegalArgumentException("Slot state '" + state + "' not found or invalid in config");
    }

    public String getSlotDisplayName(String state) {
        JsonObject slots = configData.getAsJsonObject("gui_items").getAsJsonObject("slots");
        if (slots != null && slots.has(state)) {
            return slots.getAsJsonObject(state).get("display_name").getAsString();
        }
        throw new IllegalArgumentException("Slot state '" + state + "' not found in config");
    }

    public ItemStack getAttributeItem(String attributeKey) {
        JsonObject attributes = configData.getAsJsonObject("gui_items").getAsJsonObject("attributes");
        if (attributes != null && attributes.has(attributeKey)) {
            JsonObject attributeObject = attributes.getAsJsonObject(attributeKey);
            if (attributeObject.has("material")) {
                return createItem(attributeObject.get("material").getAsString(), attributeObject.get("display_name").getAsString(), attributeObject.getAsJsonArray("lore"));
            }
        }
        throw new IllegalArgumentException("Attribute key '" + attributeKey + "' not found or invalid in config");
    }

    public String getSlotState(ItemStack item) {
        JsonObject slots = configData.getAsJsonObject("gui_items").getAsJsonObject("slots");
        for (Map.Entry<String, JsonElement> entry : slots.entrySet()) {
            String state = entry.getKey();
            ItemStack stateItem = createItem(entry.getValue().getAsJsonObject().get("material").getAsString(), entry.getValue().getAsJsonObject().get("display_name").getAsString(), entry.getValue().getAsJsonObject().getAsJsonArray("lore"));
            if (item.equals(stateItem)) {
                return state;
            }
        }
        return null;
    }

    private ItemStack createItem(String materialName, String displayName, JsonElement loreElement) {
        Material material = Material.getMaterial(materialName);
        if (material != null) {
            ItemStack itemStack = new ItemStack(material);
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(displayName);
                if (loreElement != null && loreElement.isJsonArray()) {
                    meta.setLore(new Gson().fromJson(loreElement, java.util.List.class));
                }
                itemStack.setItemMeta(meta);
            }
            return itemStack;
        }
        return null;
    }

    public JsonObject getConfigData() {
        return configData;
    }
}
