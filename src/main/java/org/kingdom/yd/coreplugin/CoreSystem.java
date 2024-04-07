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

import java.io.File;
import java.sql.Array;
import java.util.*;

import static io.lumine.mythic.lib.api.stat.SharedStat.MAX_HEALTH;

public class CoreSystem implements Listener, CommandExecutor {
    private final CorePlugin plugin;
    private final PlayerDataStorage dataStorage;

    public CoreSystem(CorePlugin plugin, PlayerDataStorage dataStorage) {
        this.plugin = plugin;
        this.dataStorage = dataStorage;
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

        for (int slot : slots) {
            String attributeKey = dataStorage.getAttributeSelection(player.getUniqueId(), slot);
            StatType selectedStat = attributeKey != null ? StatType.valueOf(attributeKey) : null;
            ItemStack item = createItemForStat(selectedStat);

            gui.setItem(slot, item);
        }

        player.openInventory(gui);
    }

    private StatType getSelectedStatForSlot(Player player, int slot) {
        String attributeKey = dataStorage.getAttributeSelection(player.getUniqueId(), slot);
        if (attributeKey == null) {
            return null;
        }
        try {
            return StatType.valueOf(attributeKey.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid StatType: " + attributeKey);
            return null;
        }
    }

    private ItemStack createItemForStat(StatType statType) {
        if (statType == null) {
            return new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        }

        switch (statType) {
            case MAX_HEALTH:
                return createAttributeItem(Material.APPLE, "Max Health", "MAX_HEALTH");
            case ATTACK_DAMAGE:
                return createAttributeItem(Material.IRON_SWORD, "Attack Damage", "ATTACK_DAMAGE");
            default:
                return new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();
        Inventory clickedInventory = e.getClickedInventory();
        ItemStack clickedItem = e.getCurrentItem();

        if (clickedInventory == null || clickedItem == null) return;


        //CORE SYSTEM
        if (e.getView().getTitle().equals(ChatColor.AQUA + "CORE SYSTEM")) {
            e.setCancelled(true);
            if (clickedItem.getType().equals(Material.BLACK_STAINED_GLASS_PANE)) {
                openAttributeGUI(player, e.getSlot());
            } else {
                player.sendMessage(ChatColor.RED + "한 번 선택한 속성은 다시 변경할 수 없습니다! (속성 초기화권 필요)");
            }
            return;
        }

        //Attribute Selection
        if (e.getView().getTitle().equals(ChatColor.AQUA + "Attribute Selection")) {
            e.setCancelled(true);
            handleAttributeSelectionFromGUI(player, e.getSlot(), clickedItem);
        }

    }
    private void handleAttributeSelectionFromGUI(Player player, int slot, ItemStack item) {
        Material type = item.getType();
        String attributeKey = null;
        int attributeValue = 0;

        switch (type) {
            case APPLE:
                attributeKey = StatType.MAX_HEALTH.name();
                attributeValue = 10;    //1당 하트 반칸
                break;
            case IRON_SWORD:
                attributeKey = StatType.ATTACK_DAMAGE.name();
                attributeValue = 5;     //1당 인게임 2.5 데미지
                break;
            default:
                plugin.getLogger().warning("Unknown item type for attribute selection");
                return;
        }

        if (attributeKey != null) {
            int clickedSlot = player.getMetadata("selectedSlot").isEmpty() ? slot : player.getMetadata("selectedSlot").get(0).asInt();
            dataStorage.saveAttributeSelection(player.getUniqueId(), player.getName(), clickedSlot, attributeKey, attributeValue);
            handleAttributeSelection(player, StatType.valueOf(attributeKey));
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> openCoreGUI(player), 2L);
        }
    }
    public void openAttributeGUI(Player player, int clickedSlot) {
        Inventory attributeGUI = Bukkit.createInventory(null, 9, ChatColor.AQUA + "Attribute Selection");

        ItemStack healthItem = createAttributeItem(Material.APPLE, ChatColor.GREEN + "Max Health", "MAX_HEALTH");
        ItemStack damageItem = createAttributeItem(Material.IRON_SWORD, ChatColor.RED + "Attack Damage", "ATTACK_DAMAGE");

        attributeGUI.setItem(0, healthItem);
        attributeGUI.setItem(1, damageItem);

        player.openInventory(attributeGUI);
        player.setMetadata("selectedSlot", new FixedMetadataValue(plugin, clickedSlot));
    }

    private ItemStack createAttributeItem(Material material, String displayName, String loreText) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + displayName);
            meta.setLore(Arrays.asList(ChatColor.GRAY + "속성: " + loreText));
            item.setItemMeta(meta);
        }
        return item;
    }


    public void handleAttributeSelection(Player player, StatType selectedStat) {
        PlayerData playerData = PlayerData.get(player);
        PlayerStats playerStats = playerData.getStats();

        switch (selectedStat) {
            case MAX_HEALTH:
                double currentMaxHealth = playerStats.getStat(StatType.MAX_HEALTH.name());
                playerStats.getMap().getInstance(StatType.MAX_HEALTH.name()).addModifier(new StatModifier("selectedAttribute", selectedStat.name(), 10, ModifierType.FLAT, EquipmentSlot.OTHER, ModifierSource.OTHER));
                break;
            case ATTACK_DAMAGE:
                playerStats.getMap().getInstance(StatType.ATTACK_DAMAGE.name()).addModifier(new StatModifier("selectedAttribute", selectedStat.name(), 10, ModifierType.FLAT, EquipmentSlot.MAIN_HAND, ModifierSource.MAINHAND_ITEM));
                break;
        }


        player.sendMessage(ChatColor.GREEN + "Your " + selectedStat.name() + " has been increased!");
    }


}
