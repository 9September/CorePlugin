package org.kingdom.yd.coreplugin;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class PlayerDataStorage {
    private CorePlugin plugin;
    private File dataFile;
    private FileConfiguration config;

    public PlayerDataStorage(CorePlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void saveAttributeSelection(UUID uuid, String playerName,int slot, String attributeKey, int value) {
        config.set(uuid.toString() + ".name", playerName);
        config.set(uuid.toString() + ".attributes." + slot + ".key", attributeKey);
        config.set(uuid.toString() + ".attributes." + slot + ".value", value);
        saveData();
    }

    public String getAttributeSelection(UUID uuid, int slot) {
        return config.getString(uuid.toString() + ".attributes." + slot + ".key");
    }

    public int getAttributeValue(UUID uuid, int slot) {
        return config.getInt(uuid.toString() + ".attributes." + slot + ".value", 0);
    }

    public void incrementAttributeValue(UUID uuid, int slot, int increment) {
        int currentValue = config.getInt(uuid.toString() + ".attributes." + slot + ".value", 0);
        int newValue = currentValue + increment;
        config.set(uuid.toString() + ".attributes." + slot + ".value", newValue);
        saveData();
    }

    public void saveData() {
        try {
            config.save(dataFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadPlayerSelections() {
        if (!dataFile.exists()) {
            return;
        }

        try {
            config = YamlConfiguration.loadConfiguration(dataFile);
            for (String uuidKey : config.getKeys(false)) {
                UUID uuid = UUID.fromString(uuidKey);
                for (String slotKey : config.getConfigurationSection(uuidKey + ".attributes").getKeys(false)) {
                    String attributeKey = config.getString(uuidKey + ".attributes." + slotKey + ".key");
                    int value = config.getInt(uuidKey + ".attributes." + slotKey + ".value");
                    System.out.println("Loaded " + attributeKey + " for slot " + slotKey + " with value " + value + " for UUID " + uuid);
                    // 추가적인 데이터 복원 로직이 필요할 수 있습니다.
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            plugin.getLogger().severe("Failed to load player selections: " + e.getMessage());
        }
    }


}
