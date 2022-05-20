package com.pro4d.prophunt.managers;

import com.pro4d.prophunt.Prophunt;
import com.pro4d.prophunt.misc.TeamInventory;
import com.pro4d.prophunt.enums.Teams;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class SpectatorManager {

    private final Map<UUID, Teams> oldTeam;
    private final List<TeamInventory> inventories;

    private final Prophunt prophunt;
    public SpectatorManager(Prophunt plugin) {
        prophunt = plugin;
        oldTeam = new HashMap<>();
        inventories = new ArrayList<>();
        createInventories(plugin);
    }

    private void createInventories(Prophunt plugin) {
        for(Teams team : Teams.values()) {
            if(team == Teams.DEAD) continue;
            inventories.add(new TeamInventory(team, plugin));
        }
    }

    public void openMenu(Player player, Teams team) {
        if(Teams.getPlayerTeam(player) == null) return;
        TeamInventory teamInv = null;
        for(TeamInventory inv : inventories) {
            if(inv.getTeam() == team) teamInv = inv;

        }
        if(teamInv == null) return;
        if(teamInv.getPages().isEmpty()) return;
        player.openInventory(teamInv.getPages().get(0));

    }

    public UUID getUUID(ItemStack item) {
        for(UUID uuid : prophunt.getHeads().keySet()) {
            ItemStack head = prophunt.getHeads().get(uuid);
            if(head == null) continue;

            if(head.isSimilar(item)) {
                return uuid;
            }

        }

        return null;
    }

    public List<TeamInventory> getInventories() {
        return inventories;
    }

    public Map<UUID, Teams> getOldTeam() {
        return oldTeam;
    }
}
