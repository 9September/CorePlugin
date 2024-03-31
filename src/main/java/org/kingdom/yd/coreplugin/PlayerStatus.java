package org.kingdom.yd.coreplugin;


import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.api.player.attribute.PlayerAttributes;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class PlayerStatus extends JavaPlugin {


    public static void modifyPlayerDataBasedOnAttributeName(Player player, String attributeName) {
        PlayerData playerData = PlayerData.get(player);

        switch (attributeName) {
            case "힘":
                increaseAttribute(playerData, "strength", 1);
                break;
            case "민첩":
                increaseAttribute(playerData, "dexterity", 1);
                break;
            case "체력":
                increaseAttribute(playerData, "intelligence", 1);
                break;
        }
        savePlayerAttributes(player);
        player.sendMessage(ChatColor.GREEN + attributeName + " 속성이 증가했습니다.");
    }

    public static void increaseAttribute(PlayerData playerData, String attributeId, int amount) {
        PlayerAttributes.AttributeInstance attributeInstance = playerData.getAttributes().getInstance(attributeId);
        if (attributeInstance != null) {
            attributeInstance.addBase(amount);
            attributeInstance.updateStats();
        } else {
            Bukkit.getLogger().warning("Invalid attribute ID: " + attributeId);
        }
    }

    public static void savePlayerAttributes(Player player) {
        CorePlugin plugin = JavaPlugin.getPlugin(CorePlugin.class);
        String uuid = player.getUniqueId().toString();
        PlayerData mmocoreData = PlayerData.get(player);

        // 플레이어 기본 정보 및 속성 정보 저장
        plugin.getPlayersData().set(uuid + ".Name", player.getName());
        for (PlayerAttributes.AttributeInstance instance : mmocoreData.getAttributes().getInstances()) {
            String attributeId = instance.getId();
            int baseValue = instance.getBase();
            plugin.getPlayersData().set(uuid + ".Attributes." + attributeId, baseValue);
        }

        // 변경사항을 파일에 저장
        plugin.savePlayersData();
    }

    public static void resetPlayerAttributes(Player player) {
        CorePlugin plugin = JavaPlugin.getPlugin(CorePlugin.class);
        UUID playerUUID = player.getUniqueId();

        plugin.getPlayersData().set(playerUUID + ".coreSelections", null);
        plugin.savePlayersData();

        CoreSystem.clearPlayerSelections(playerUUID);
    }

}
