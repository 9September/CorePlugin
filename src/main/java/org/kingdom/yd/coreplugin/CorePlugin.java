package org.kingdom.yd.coreplugin;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

public final class CorePlugin extends JavaPlugin {
    private File dataFile;
    private FileConfiguration playersData;
    private CoreSystem coreSystem;

    @Override
    public void onEnable() {
        this.coreSystem = new CoreSystem(this);
        getCommand("core").setExecutor(coreSystem);
        getServer().getPluginManager().registerEvents(coreSystem, this);
        getLogger().info("CorePlugin has been enabled.");

        loadPlayerData();
        loadPlayerSelections();
    }

    private void loadPlayerData() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        dataFile = new File(getDataFolder(), "playerData.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        playersData = YamlConfiguration.loadConfiguration(dataFile);
    }

    public FileConfiguration getPlayersData() {
        return playersData;
    }

    public void savePlayersData() {
        try {
            playersData.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadPlayerSelections() {
        File dataFile = new File(getDataFolder(), "playerData.yml");
        FileConfiguration dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        for (String playerUUIDString : dataConfig.getKeys(false)) {
            UUID playerUUID = UUID.fromString(playerUUIDString);
            ConfigurationSection section = dataConfig.getConfigurationSection(playerUUIDString + ".coreSelections");
            Map<Integer, Boolean> selections = new HashMap<>();
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    selections.put(Integer.parseInt(key), section.getBoolean(key));
                }
            }
            coreSystem.setPlayerSelections(playerUUID, selections); // CoreSystem에 선택 정보 전달
        }
    }

    public static void saveSelections(CorePlugin plugin, UUID playerUUID, Map<Integer, Boolean> selections) {
        FileConfiguration dataConfig = plugin.getPlayersData();
        selections.forEach((slot, selected) -> {
            dataConfig.set(playerUUID.toString() + ".coreSelections." + slot, selected);
        });
        plugin.savePlayersData();
    }

    @Override
    public void onDisable() {
        getLogger().info("CorePlugin has been disabled.");
    }


}
