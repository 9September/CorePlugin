package org.kingdom.yd.coreplugin;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class CorePlugin extends JavaPlugin {

    private PlayerDataStorage playerDataStorage;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        this.playerDataStorage = new PlayerDataStorage(this);
        this.configManager = new ConfigManager(this);
        CoreSystem coreSystem = new CoreSystem(this, playerDataStorage);

        getServer().getPluginManager().registerEvents(coreSystem, this);
        getCommand("core").setExecutor(coreSystem);

        playerDataStorage.loadPlayerSelections();
        getLogger().info(ChatColor.AQUA + "[COREPLUGIN] CorePlugin enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info(ChatColor.AQUA + "[COREPLUGIN] CorePlugin disabled.");

    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
