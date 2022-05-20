package com.pro4d.prophunt.misc;

import com.pro4d.prophunt.utils.PHuntUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ItemBuilder {

    private final ItemStack item;
    private final int slot;

    public ItemBuilder(ItemStack i, int s) {
        item = i;
        slot = s;
    }

    public void giveToPlayer(Player player) {
        player.getInventory().setItem(slot, item);
    }

    public ItemStack getItem() {
        return item;
    }

    public static void removeItem(Player player, ItemStack i) {
        ItemStack item = PHuntUtils.findItem(player.getInventory(), i);
        if(item != null) player.getInventory().removeItem(item);

    }

}
