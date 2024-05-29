package org.kingdom.yd.coreplugin;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.inventory.ItemStack;

public class ItemsAdderUtil {

    public static ItemStack getCustomItem(String itemId) {
        CustomStack customStack = CustomStack.getInstance(itemId);

        if (customStack != null) {
            return customStack.getItemStack();
        }
        return null;
    }
}
