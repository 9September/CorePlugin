package org.kingdom.yd.coreplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CoreSystem implements Listener, CommandExecutor {

    private final CorePlugin plugin;
    private static final Map<UUID, Map<Integer, Boolean>> playerSelections = new HashMap<>();
    private final Map<UUID, Inventory> playerCoreSelections = new HashMap<>();

    public CoreSystem(CorePlugin plugin) {
        this.plugin = plugin;
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
        Inventory coreGUI = Bukkit.createInventory(null, 54, ChatColor.AQUA + "" + ChatColor.BOLD + "CORE SYSTEM");

        // 선택 가능한 슬롯 설정
        int[] slots = {12,14,20,24,38,42,48,50};
        Map<Integer, Boolean> selections = playerSelections.getOrDefault(player.getUniqueId(), new HashMap<>());

        for (int slot : slots) {
            if (selections.getOrDefault(slot, false)) {
                // 이미 선택된 코어에 대한 처리, 예: 다른 아이템 또는 표시 방법 사용
            } else {
                coreGUI.setItem(slot, createGuiItem(Material.BLACK_STAINED_GLASS_PANE, "Select Attribute"));
            }
        }

        // 스탯 초기화 버튼 추가
        coreGUI.setItem(53, createGuiItem(Material.REDSTONE_BLOCK, ChatColor.RED + "스탯 초기화"));

        player.openInventory(coreGUI);
    }

    private Inventory createCoreSystemGUI() {
        Inventory inventory = Bukkit.createInventory(null, 54, ChatColor.AQUA + "" + ChatColor.BOLD + "CORE SYSTEM");
        int[] slots = {12,14,20,24,38,42,48,50};
        for (int slot : slots) {
            inventory.setItem(slot, createGuiItem(Material.BLACK_STAINED_GLASS_PANE, "Select Attribute"));
        }
        inventory.setItem(53, createGuiItem(Material.REDSTONE_BLOCK, "코어 초기화"));
        return inventory;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player) || e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;

        Player player = (Player) e.getWhoClicked();
        ItemStack clickedItem = e.getCurrentItem();
        Inventory clickedinventory = e.getClickedInventory();
        String inventoryTitle = e.getView().getTitle();
        int slot = e.getRawSlot();


        //CORE SYSTEM GUI
        if (inventoryTitle.equals(ChatColor.AQUA + "" + ChatColor.BOLD + "CORE SYSTEM")) {
            e.setCancelled(true);
            Map<Integer, Boolean> selections = playerSelections.getOrDefault(player.getUniqueId(), new HashMap<>());
            if (slot == 12 || slot == 14 || slot == 20 || slot == 24 || slot == 38 || slot == 42 || slot == 48 || slot == 50) {
                openAttributeSelectionGUI(player, slot);
            }
            if (slot == 53) {
                PlayerStatus.resetPlayerAttributes(player);
                player.sendMessage(ChatColor.RED + "스탯이 초기화되었습니다.");
                openCoreGUI(player);
                return;
            }
            if (selections.getOrDefault(slot, false)) {
                player.sendMessage(ChatColor.RED + "이미 선택된 항목입니다.");
                return;
            }
        }

        //속성 선택 GUI
        if (inventoryTitle.startsWith("속성 선택")) {
            if (player.hasMetadata("selectedCoreSlot")) {
                MetadataValue value = player.getMetadata("selectedCoreSlot").get(0);
                int selectedSlot = value.asInt();

                updateCoreSystemGUI(player, selectedSlot, clickedItem);
                PlayerStatus.modifyPlayerDataBasedOnAttributeName(player, clickedItem.getItemMeta().getDisplayName());
                player.removeMetadata("selectedCoreSlot", JavaPlugin.getPlugin(CorePlugin.class));
            }
            e.setCancelled(true);
        }
    }


    private void openAttributeSelectionGUI(Player player, int slot) {
        Inventory attrInv = Bukkit.createInventory(null,9,"속성 선택");
        attrInv.setItem(0,createGuiItem(Material.IRON_SWORD,"힘"));
        attrInv.setItem(1, createGuiItem(Material.FEATHER, "민첩"));
        attrInv.setItem(2, createGuiItem(Material.SNOWBALL, "지력"));

        player.setMetadata("selectedCoreSlot", new FixedMetadataValue(JavaPlugin.getPlugin(CorePlugin.class), slot));
        player.openInventory(attrInv);
    }

    private void updateCoreSystemGUI(Player player, int slot, ItemStack attributeItem) {
        Inventory coreSystem = playerCoreSelections.get(player.getUniqueId());
        if (coreSystem == null) {
            coreSystem = createCoreSystemGUI();
            playerCoreSelections.put(player.getUniqueId(), coreSystem);
        }

        coreSystem.setItem(slot, attributeItem);

        player.openInventory(coreSystem);
    }


    private ItemStack createGuiItem(Material material, String name) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta  = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }


    public void setPlayerSelections(UUID playerUUID, Map<Integer, Boolean> selections) {
        this.playerSelections.put(playerUUID, selections);
    }

    public void updatePlayerGUI(Player player) {
        Inventory coreGUI = playerCoreSelections.get(player.getUniqueId());
        if (coreGUI == null) {
            coreGUI = createCoreSystemGUI();
            playerCoreSelections.put(player.getUniqueId(), coreGUI);
        }

        Map<Integer, Boolean> selections = playerSelections.getOrDefault(player.getUniqueId(), new HashMap<>());
    }


    public static void clearPlayerSelections(UUID playerUUID) {
        playerSelections.remove(playerUUID);
    }

}
