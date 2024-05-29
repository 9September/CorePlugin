package org.kingdom.yd.coreplugin;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import io.lumine.mythic.lib.api.player.EquipmentSlot;
import io.lumine.mythic.lib.api.stat.StatMap;
import io.lumine.mythic.lib.api.stat.modifier.StatModifier;
import io.lumine.mythic.lib.player.modifier.ModifierSource;
import io.lumine.mythic.lib.player.modifier.ModifierType;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.api.player.stats.PlayerStats;
import net.Indyuce.mmocore.api.player.stats.StatType;
import org.bukkit.Bukkit;
import org.bukkit.Material;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.*;

public class CoreSystem implements Listener, CommandExecutor {
    private final CorePlugin plugin;
    private final PlayerDataStorage dataStorage;
    private final Essentials essentials;
    private final ConfigManager configManager;
    private final Map<Integer, Integer> slotCosts = new HashMap<>();

    public CoreSystem(CorePlugin plugin, PlayerDataStorage dataStorage) {
        this.plugin = plugin;
        this.dataStorage = dataStorage;
        this.configManager = plugin.getConfigManager();
        Plugin essentialsPlugin = plugin.getServer().getPluginManager().getPlugin("Essentials");
        this.essentials = (Essentials) essentialsPlugin;

        slotCosts.put(3, 0);
        slotCosts.put(5, 5000);
        slotCosts.put(15, 15000);
        slotCosts.put(33, 30000);
        slotCosts.put(41, 50000);
        slotCosts.put(39, 75000);
        slotCosts.put(29, 105000);
        slotCosts.put(11, 140000);
        slotCosts.put(12, 180000);
        slotCosts.put(14, 225000);
        slotCosts.put(32, 275000);
        slotCosts.put(30, 330000);
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
        String coreSystemTitle = configManager.getGUITitle("core_system");
        Inventory gui = plugin.getServer().createInventory(null, 45, ChatColor.AQUA + coreSystemTitle);
        int[] primarySlots = {3, 5, 11, 15, 29, 33, 39, 41};
        int[] secondarySlots = {12, 14, 30, 32};

        boolean allPrimarySelected = true;

        for (int slot : primarySlots) {
            String attributeKey = dataStorage.getAttributeSelection(player.getUniqueId(), slot);
            StatType selectedStat = attributeKey != null ? StatType.valueOf(attributeKey) : null;
            ItemStack item = createItemForStat(selectedStat, "default");

            if (selectedStat == null) {
                allPrimarySelected = false;
            }

            gui.setItem(slot, item);
        }

        for (int slot : secondarySlots) {
            ItemStack item;
            if (allPrimarySelected) {
                String attributeKey = dataStorage.getAttributeSelection(player.getUniqueId(), slot);
                StatType selectedStat = attributeKey != null ? StatType.valueOf(attributeKey) : null;
                item = createItemForStat(selectedStat, "default");
            } else {
                item = createLockedItem();
            }
            gui.setItem(slot, item);
        }

        player.openInventory(gui);
    }

    private ItemStack createLockedItem() {
        ItemStack item = configManager.getSlotItem("locked");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + configManager.getSlotDisplayName("locked"));
            meta.setLore(Arrays.asList(ChatColor.GRAY + "Unlock by selecting all primary attributes."));
            meta.setLocalizedName("locked");
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createItemForStat(StatType statType, String defaultState) {
        if (statType == null) {
            return configManager.getSlotItem(defaultState);
        }

        String attributeKey = statType.name();
        if (!configManager.getConfigData().getAsJsonObject("gui_items").getAsJsonObject("attributes").has(attributeKey)) {
            throw new IllegalArgumentException("Attribute key '" + attributeKey + "' not found in config");
        }

        return configManager.getAttributeItem(attributeKey);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();
        Inventory clickedInventory = e.getClickedInventory();
        ItemStack clickedItem = e.getCurrentItem();

        if (clickedInventory == null || clickedItem == null) return;

        String coreSystemTitle = configManager.getGUITitle("core_system");
        String attributeSelectionTitle = configManager.getGUITitle("attribute_selection");

        if (e.getView().getTitle().equals(ChatColor.AQUA + coreSystemTitle)) {
            e.setCancelled(true);
            String slotState = configManager.getSlotState(clickedItem);
            if ("default".equals(slotState)) {
                int slot = e.getSlot();
                int cost = slotCosts.getOrDefault(slot, 0);
                User user = essentials.getUser(player);
                double balance = user.getMoney().doubleValue();

                if (balance >= cost) {
                    user.takeMoney(BigDecimal.valueOf(cost));
                    openAttributeGUI(player, e.getSlot());
                } else {
                    player.sendMessage(ChatColor.RED + "해당 코어를 개방하려면 " + ChatColor.YELLOW + cost + "골드" + ChatColor.RED + "가 필요합니다.");
                }

            } else if ("locked".equals(slotState)) {
                if (areAllRequiredSlotsFilled(player)) {
                    openAttributeGUI(player, e.getSlot());
                } else {
                    player.sendMessage(ChatColor.RED + "바깥 슬롯부터 모두 선택해야 안쪽 슬롯이 열립니다!");
                }
            } else {
                player.sendMessage(ChatColor.RED + "한 번 선택한 속성은 다시 변경할 수 없습니다! (속성 초기화권 필요)");
            }
            return;
        }

        if (e.getView().getTitle().equals(ChatColor.AQUA + attributeSelectionTitle)) {
            e.setCancelled(true);
            handleAttributeSelectionFromGUI(player, e.getSlot(), clickedItem);
        }

    }

    private boolean areAllRequiredSlotsFilled(Player player) {
        Inventory inv = player.getOpenInventory().getTopInventory();
        int[] requiredSlots = {3, 5, 11, 15, 29, 33, 39, 41};

        for (int slot : requiredSlots) {
            ItemStack item = inv.getItem(slot);
            if (item == null || !"default".equals(configManager.getSlotState(item))) {
                return false;
            }
        }
        return true;
    }

    private void handleAttributeSelectionFromGUI(Player player, int slot, ItemStack item) {
        String attributeKey = null;
        int attributeValue = 0;

        if (item.equals(configManager.getAttributeItem("MAX_HEALTH"))) {
            attributeKey = StatType.MAX_HEALTH.name();
            attributeValue = 10;
        } else if (item.equals(configManager.getAttributeItem("HEALTH_REGENERATION"))) {
            attributeKey = StatType.HEALTH_REGENERATION.name();
            attributeValue = 10;
        } else if (item.equals(configManager.getAttributeItem("MAX_MANA"))) {
            attributeKey = StatType.MAX_MANA.name();
            attributeValue = 10;
        } else if (item.equals(configManager.getAttributeItem("MANA_REGENERATION"))) {
            attributeKey = StatType.MANA_REGENERATION.name();
            attributeValue = 10;
        } else if (item.equals(configManager.getAttributeItem("STAMINA_REGENERATION"))) {
            attributeKey = StatType.STAMINA_REGENERATION.name();
            attributeValue = 10;
        } else if (item.equals(configManager.getAttributeItem("KNOCKBACK_RESISTANCE"))) {
            attributeKey = StatType.KNOCKBACK_RESISTANCE.name();
            attributeValue = 10;
        } else if (item.equals(configManager.getAttributeItem("COOLDOWN_REDUCTION"))) {
            attributeKey = StatType.COOLDOWN_REDUCTION.name();
            attributeValue = 10;
        } else {
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
        String attributeSelectionTitle = configManager.getGUITitle("attribute_selection");
        Inventory attributeGUI = Bukkit.createInventory(null, 9, ChatColor.AQUA + attributeSelectionTitle);

        for (StatType statType : StatType.values()) {
            if (configManager.getConfigData().getAsJsonObject("gui_items").getAsJsonObject("attributes").has(statType.name())) {
                attributeGUI.addItem(configManager.getAttributeItem(statType.name()));
            }
        }

        player.openInventory(attributeGUI);
        player.setMetadata("selectedSlot", new FixedMetadataValue(plugin, clickedSlot));
    }

    public void handleAttributeSelection(Player player, StatType selectedStat) {
        PlayerData playerData = PlayerData.get(player);
        PlayerStats playerStats = playerData.getStats();
        StatMap statMap = playerStats.getMap();

        // 각 StatType별 증가값 설정
        int additionalValue = getAdditionalValue(selectedStat);
        ModifierType modifierType = getModifierType(selectedStat);
        EquipmentSlot equipmentSlot = EquipmentSlot.OTHER; //필요 시 getEquipmentSlot() 생성 후 사용
        ModifierSource modifierSource = getModifierSource(selectedStat);

        // 기존 모디파이어 찾기
        StatModifier existingModifier = statMap.getInstance(selectedStat.name()).getModifier("selectedAttribute");

        if (existingModifier != null) {
            // 기존 모디파이어를 삭제
            existingModifier.unregister(playerData.getMMOPlayerData());

            // 새로운 총 값으로 모디파이어를 추가
            StatModifier newModifier = existingModifier.add(additionalValue);
            newModifier.register(playerData.getMMOPlayerData());
            player.sendMessage(ChatColor.GREEN + "Your " + selectedStat.name() + " has been increased to " + (existingModifier.getValue() + additionalValue) + "!");
        } else {
            // 새 모디파이어 등록
            StatModifier newModifier = new StatModifier("selectedAttribute", selectedStat.name(), additionalValue, modifierType, equipmentSlot, modifierSource);
            newModifier.register(playerData.getMMOPlayerData());
            player.sendMessage(ChatColor.GREEN + "Your " + selectedStat.name() + " has been increased by " + additionalValue + "!");
        }
    }


    private int getAdditionalValue(StatType selectedStat) {
        // 이전과 같이 구현된 메소드
        return switch (selectedStat) {
            case MAX_HEALTH -> 5;
            case HEALTH_REGENERATION -> 7;
            case STAMINA_REGENERATION -> 3;
            case MAX_MANA -> 15;
            case MANA_REGENERATION -> 3;
            case KNOCKBACK_RESISTANCE -> 5;
            case COOLDOWN_REDUCTION -> 1;
            default -> 1;
        };
    }

    private ModifierType getModifierType(StatType selectedStat) {
        // 이전과 같이 구현된 메소드
        return switch (selectedStat) {
            case HEALTH_REGENERATION, COOLDOWN_REDUCTION, STAMINA_REGENERATION, MANA_REGENERATION -> ModifierType.RELATIVE;
            default -> ModifierType.FLAT;
        };
    }

    private ModifierSource getModifierSource(StatType selectedStat) {
        // 이전과 같이 구현된 메소드
        return switch (selectedStat) {
            case MAX_HEALTH, HEALTH_REGENERATION, STAMINA_REGENERATION, MANA_REGENERATION, COOLDOWN_REDUCTION, MAX_MANA -> ModifierSource.OTHER;
            default -> ModifierSource.MAINHAND_ITEM;
        };
    }

}
