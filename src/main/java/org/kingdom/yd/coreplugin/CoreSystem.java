package org.kingdom.yd.coreplugin;

import io.lumine.mythic.lib.api.player.EquipmentSlot;
import io.lumine.mythic.lib.api.stat.StatMap;
import io.lumine.mythic.lib.api.stat.modifier.StatModifier;
import io.lumine.mythic.lib.player.modifier.ModifierSource;
import io.lumine.mythic.lib.player.modifier.ModifierType;
import net.Indyuce.mmocore.MMOCore;
import net.Indyuce.mmocore.api.MMOCoreAPI;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.api.player.attribute.PlayerAttributes;
import net.Indyuce.mmocore.api.player.stats.PlayerStats;
import net.Indyuce.mmocore.api.player.stats.StatType;
import net.Indyuce.mmocore.player.stats.StatInfo;
import org.bukkit.Bukkit;
import org.bukkit.Material;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.sql.Array;
import java.util.*;

import static io.lumine.mythic.lib.api.stat.SharedStat.MAX_HEALTH;

public class CoreSystem implements Listener, CommandExecutor {

    private final CorePlugin plugin;
    private PlayerDataStorage dataStorage;

    public CoreSystem(CorePlugin plugin) {
        this.plugin = plugin;
        this.dataStorage = new PlayerDataStorage(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (commandSender instanceof Player) {
            Player player = (Player) commandSender;
            openCoreGUI(player);
        }
        return true;
    }

    public void openCoreGUI(Player player) {
        Inventory gui = plugin.getServer().createInventory(null, 45, ChatColor.AQUA + "CORE SYSTEM");
        int[] slots = {3,5,11,15,29,33,39,41};
        UUID playerUUID = player.getUniqueId();

        for (int slot : slots) {
            StatType selectedStat = getSelectedStatForSlot(player, slot);

            ItemStack item = switch (selectedStat) {
                case MAX_HEALTH -> createAttributeItem(Material.APPLE, ChatColor.GREEN + "Max Health", "MAX_HEALTH");
                case ATTACK_DAMAGE -> createAttributeItem(Material.IRON_SWORD, ChatColor.RED + "Attack Damage", "ATTACK_DAMAGE");
                // 다른 속성에 대한 처리를 여기에 추가...
                default -> new ItemStack(Material.BLACK_STAINED_GLASS_PANE); // 속성이 선택되지 않았을 때 기본 아이템
            };

            gui.setItem(slot, item);
        }

        player.openInventory(gui);
    }

    private StatType getSelectedStatForSlot(Player player, int slot) {
        String attributeKey = dataStorage.getAttributeSelection(player.getUniqueId(), slot);
        if (attributeKey == null || attributeKey.isEmpty()) {
            return null; //또는 기본값
        }
        try {
            return StatType.valueOf(attributeKey.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null; //또는 기본값
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();
        Inventory clickedInventory = e.getClickedInventory();
        ItemStack clickedItem = e.getCurrentItem();

        if (clickedInventory.getType() == InventoryType.CHEST && clickedItem != null) {
            //CORE SYSTEM
            if (e.getView().getTitle().equals(ChatColor.AQUA + "CORE SYSTEM")) {
                e.setCancelled(true);
                openAttributeGUI(player);
            }
            //Attribute Selection
            if (e.getView().getTitle().equals(ChatColor.AQUA + "Attribute Selection")) {
                e.setCancelled(true);
                String attributeKey = null;

                if (clickedItem.getType() == Material.APPLE) {
                    attributeKey = StatType.MAX_HEALTH.name();
                }

                if (attributeKey != null) {
                    dataStorage.saveAttributeSelection(player.getUniqueId(), e.getSlot(), attributeKey);

                    handleAttributeSelection(player, StatType.valueOf(attributeKey));

                    openCoreGUI(player);
                }

            }
        }
    }

    public void openAttributeGUI(Player player) {
        Inventory attributeGUI = Bukkit.createInventory(null, 9, ChatColor.AQUA + "Attribute Selection");

        ItemStack healthItem = createAttributeItem(Material.APPLE, ChatColor.GREEN + "MAX_HEALTH", "health");

        attributeGUI.setItem(0, healthItem);
        player.openInventory(attributeGUI);
    }

    private ItemStack createAttributeItem(Material material, String displayName, String attributeKey) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "속성: "+attributeKey);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void handleAttributeSelection(Player player, StatType selectedStat) {
        PlayerData playerData = PlayerData.get(player);
        PlayerStats playerStats = playerData.getStats();

        double currentMaxHealth = playerStats.getStat(StatType.MAX_HEALTH.name());
        playerStats.getMap().getInstance(StatType.MAX_HEALTH.name()).addModifier(new StatModifier("selectedAttribute", selectedStat.name(), 10, ModifierType.FLAT, EquipmentSlot.OTHER, ModifierSource.OTHER));

        player.sendMessage(ChatColor.GREEN + "Your " + selectedStat.name() + "has been increased!");
    }

    public void loadPlayerSelections() {
        UUID uuid = UUID.fromString("player-uuid");
        Object selection = dataStorage.loadPlayerData(uuid, "selection");
    }

    public void resetAllStatsForPlayer(Player player) {
        PlayerData playerData = PlayerData.get(player);
        if (playerData == null) return;

        StatMap statMap = playerData.getStats().getMap();

        statMap.
    }
}
