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

import static io.lumine.mythic.lib.api.stat.SharedStat.CRITICAL_STRIKE_POWER;
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

    private ItemStack createItemForStat(StatType statType) {
        if (statType == null) {
            return new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        }

        switch (statType) {
            case SKILL_CRITICAL_STRIKE_CHANCE:
                return createAttributeItem(Material.RED_DYE, ChatColor.RED + "크리티컬 데미지", "SKILL_CRITICAL_STRIKE_CHANCE");
            case PHYSICAL_DAMAGE:
                return createAttributeItem(Material.ORANGE_DYE, ChatColor.GOLD + "물리 데미지", "PHYSICAL_DAMAGE");
            case MAGIC_DAMAGE:
                return createAttributeItem(Material.YELLOW_DYE, ChatColor.YELLOW + "마법 데미지", "MAGIC_DAMAGE");
            case MAX_MANA:
                return createAttributeItem(Material.GREEN_DYE, ChatColor.GREEN + "마나", "MAX_MANA");
            case MANA_REGENERATION:
                return createAttributeItem(Material.BLUE_DYE, ChatColor.BLUE + "마나 재생", "MANA_REGENERATION");
            case MOVEMENT_SPEED:
                return createAttributeItem(Material.PURPLE_DYE, ChatColor.LIGHT_PURPLE + "이동 속도", "MOVEMENT_SPEED");
            case COOLDOWN_REDUCTION:
                return createAttributeItem(Material.WHITE_DYE, ChatColor.WHITE + "쿨타임 감소", "COOLDOWN_REDUCTION");
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
            case RED_DYE:
                attributeKey = StatType.SKILL_CRITICAL_STRIKE_CHANCE.name();
                attributeValue = 10;
                break;
            case ORANGE_DYE:
                attributeKey = StatType.PHYSICAL_DAMAGE.name();
                attributeValue = 10;
                break;
            case YELLOW_DYE:
                attributeKey = StatType.MAGIC_DAMAGE.name();
                attributeValue = 10;
                break;
            case GREEN_DYE:
                attributeKey = StatType.MAX_MANA.name();
                attributeValue = 10;
                break;
            case BLUE_DYE:
                attributeKey = StatType.MANA_REGENERATION.name();
                attributeValue = 10;
                break;
            case PURPLE_DYE:
                attributeKey = StatType.MOVEMENT_SPEED.name();
                attributeValue = 10;
                break;
            case WHITE_DYE:
                attributeKey = StatType.COOLDOWN_REDUCTION.name();
                attributeValue = 10;
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

        ItemStack critical = createAttributeItem(Material.RED_DYE, ChatColor.RED + "크리티컬 데미지", "SKILL_CRITICAL_STRIKE_CHANCE");
        ItemStack physical = createAttributeItem(Material.ORANGE_DYE, ChatColor.GOLD + "물리 데미지", "PHYSICAL_DAMAGE");
        ItemStack magical = createAttributeItem(Material.YELLOW_DYE, ChatColor.YELLOW + "마법 데미지", "MAGIC_DAMAGE");
        ItemStack mana = createAttributeItem(Material.GREEN_DYE, ChatColor.GREEN + "마나", "MAX_MANA");
        ItemStack manaregeneration = createAttributeItem(Material.BLUE_DYE, ChatColor.BLUE + "마나 재생", "MANA_REGENERATION");
        ItemStack speed = createAttributeItem(Material.PURPLE_DYE, ChatColor.LIGHT_PURPLE + "이동 속도", "MOVEMENT_SPEED");
        ItemStack cooldown = createAttributeItem(Material.WHITE_DYE, ChatColor.WHITE + "쿨타임 감소", "COOLDOWN_REDUCTION");

        attributeGUI.setItem(1, critical);
        attributeGUI.setItem(2, physical);
        attributeGUI.setItem(3, magical);
        attributeGUI.setItem(4, mana);
        attributeGUI.setItem(5, manaregeneration);
        attributeGUI.setItem(6, speed);
        attributeGUI.setItem(7, cooldown);

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

            if (selectedStat == StatType.SKILL_CRITICAL_STRIKE_CHANCE) {
                StatModifier newModifier = existingModifier.add(additionalValue);
                newModifier.register(playerData.getMMOPlayerData());
                StatModifier newModifier2 = statMap.getInstance(StatType.SKILL_CRITICAL_STRIKE_POWER.name()).getModifier("selectedAttribute").add(10);
                newModifier2.register(playerData.getMMOPlayerData());
            } else {
                // 새로운 총 값으로 모디파이어를 추가
                StatModifier newModifier = existingModifier.add(additionalValue);
                newModifier.register(playerData.getMMOPlayerData());
            }
            player.sendMessage(ChatColor.GREEN + "Your " + selectedStat.name() + " has been increased to " + (existingModifier.getValue() + additionalValue) + "!");
        } else {
            if (selectedStat == StatType.SKILL_CRITICAL_STRIKE_CHANCE) {
                StatModifier newModifier = new StatModifier("selectedAttribute", selectedStat.name(), additionalValue, modifierType, equipmentSlot, modifierSource);
                newModifier.register(playerData.getMMOPlayerData());
                StatModifier newModifier2 = new StatModifier("selectedAttribute", StatType.SKILL_CRITICAL_STRIKE_POWER.name(), 10, ModifierType.FLAT, equipmentSlot, modifierSource);
                newModifier2.register(playerData.getMMOPlayerData());
            } else {
                // 새 모디파이어 등록
                StatModifier newModifier = new StatModifier("selectedAttribute", selectedStat.name(), additionalValue, modifierType, equipmentSlot, modifierSource);
                newModifier.register(playerData.getMMOPlayerData());
            }
            player.sendMessage(ChatColor.GREEN + "Your " + selectedStat.name() + " has been increased by " + additionalValue + "!");
        }
    }

    private int getAdditionalValue(StatType selectedStat) {
        // 이전과 같이 구현된 메소드
        return switch (selectedStat) {
            case SKILL_CRITICAL_STRIKE_CHANCE -> 5; //SKILL_CRITICAL_STRIKE_POWER는 handleAttributeSelection()에서 직접 수정.
            case PHYSICAL_DAMAGE -> 7;
            case MAGIC_DAMAGE -> 6;
            case MAX_MANA -> 15;
            case MANA_REGENERATION -> 3;
            case MOVEMENT_SPEED -> 20;
            case COOLDOWN_REDUCTION -> 1;
            default -> 1;
        };
    }

    private ModifierType getModifierType(StatType selectedStat) {
        // 이전과 같이 구현된 메소드
        return switch (selectedStat) {
            case CRITICAL_STRIKE_CHANCE, COOLDOWN_REDUCTION, MOVEMENT_SPEED -> ModifierType.RELATIVE;
            default -> ModifierType.FLAT;
        };
    }

    private ModifierSource getModifierSource(StatType selectedStat) {
        // 이전과 같이 구현된 메소드
        return switch (selectedStat) {
            case MANA_REGENERATION, COOLDOWN_REDUCTION, MOVEMENT_SPEED, MAX_MANA -> ModifierSource.OTHER;
            default -> ModifierSource.MAINHAND_ITEM;
        };
    }

}
