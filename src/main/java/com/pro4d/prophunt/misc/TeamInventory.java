package com.pro4d.prophunt.misc;

import com.pro4d.prophunt.Prophunt;
import com.pro4d.prophunt.enums.Teams;
import com.pro4d.prophunt.utils.PHuntMessages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TeamInventory {

    private final List<Inventory> pages;
    private final Teams team;

    private final Prophunt prophunt;
    public TeamInventory(Teams team, Prophunt plugin) {
        this.team = team;
        pages = new ArrayList<>();
        prophunt = plugin;
    }

    private void createInventories() {
        if(team.getMembers().size() == 0) return;
        int num = team.getMembers().size();
        if(num % 9 != 0) {
            int a = (num / 9) * 9;

            num = a + 9;
        }
        if(num > 54) {
            int pageCount = num / 9;
            for(int pc = pageCount; pc != 0; pc--) {

                Inventory inv = createInventory(PHuntMessages.translate(team.getColor() + team.getName()), num % 54);
                fillInventory(inv);
                pages.add(inv);
            }
        } else {
            Inventory inv = createInventory(PHuntMessages.translate(team.getColor() + team.getName()), num);
            fillInventory(inv);
            pages.add(inv);
        }

        //fill head list with team members

    }

    public void fillInventory(Inventory inv) {
        if(pages.size() >= 2) {
            if (pages.get(pages.size() - 1) != inv) {
                if (team.getMembers().size() > 54) {

                    for (int i = 0; i < (inv.getSize() - 1); i++) {
                        if(i <= (prophunt.getHeads().size() - 1)) {
                            inv.setItem(i, prophunt.getHeads().get(team.getMembers().get(i)));
                        }
                    }

                    ItemStack item = new ItemStack(Material.PAPER, 1);
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName(PHuntMessages.translate("&o&5Next Page"));
                    }
                    inv.setItem(inv.getSize(), item);
                    return;
                }
            }
        }

        for(int i = 0; i < inv.getSize(); i++) {
            if(i <= (prophunt.getHeads().size() - 1)) {
                inv.setItem(i, prophunt.getHeads().get(team.getMembers().get(i)));
            }
        }

    }

    public void updateInventory() {
        pages.clear();
        createInventories();
    }

    public Inventory createInventory(String title, int size) {
        return Bukkit.createInventory(null, size, title);
    }

    public List<Inventory> getPages() {
        return pages;
    }

    public Teams getTeam() {
        return team;
    }

}
