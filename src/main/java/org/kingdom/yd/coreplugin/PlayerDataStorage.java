package org.kingdom.yd.coreplugin;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.UUID;

public class PlayerDataStorage {
    private CorePlugin plugin;
    private File dataFolder;
    private FileConfiguration config;

    public PlayerDataStorage(CorePlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerData");
        if (!dataFolder.exists()) dataFolder.mkdirs();
        loadData();
    }

    public void loadData() {
        File file = new File(dataFolder, "data.yml");
        if (!file.exists()) {
            plugin.saveResource("data.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void savePlayerData(UUID uuid, String path, Object value) {
        config.set(uuid.toString() + "." + path, value);
        saveData();
    }

    public void saveAttributeSelection(UUID uuid, int slot, String attributeKey) {
        config.set(uuid.toString() + ".attributes." + slot, attributeKey);
        saveData();
    }

    public String getAttributeSelection(UUID uuid, int slot) {
        return config.getString(uuid.toString() + ".attributes." + slot);
    }

    public Object loadPlayerData(UUID uuid, String path) {
        return config.get(uuid.toString() + "." + path);
    }

    public void saveData() {
        try {
            config.save(new File(dataFolder, "data.yml"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
