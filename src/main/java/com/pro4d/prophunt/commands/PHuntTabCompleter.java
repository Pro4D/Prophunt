package com.pro4d.prophunt.commands;

import com.pro4d.prophunt.Prophunt;
import com.pro4d.prophunt.misc.PHuntMap;
import com.pro4d.prophunt.enums.Teams;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PHuntTabCompleter implements TabCompleter {

    private final Prophunt plugin;
    public PHuntTabCompleter(Prophunt plugin) {
        this.plugin = plugin;
        PluginCommand command = plugin.getServer().getPluginCommand("prophunt");
        if(command != null) command.setTabCompleter(this);
    }


    //ph set-map
    //ph set-team
    //ph clear-team
    //ph start
    //ph end
    //ph play-sound
    //ph map-vote
    //ph lobby
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> tabList = new ArrayList<>();
        switch(args.length) {
            case 1:
                tabList.add("set-map");
                tabList.add("set-team");
                tabList.add("clear-team");
                tabList.add("start");
                tabList.add("end");
                tabList.add("map-vote");
                tabList.add("lobby");
                break;

            case 2:
                if(args[0].equalsIgnoreCase("set-team") || args[0].equalsIgnoreCase("clear-team")) {
                    tabList.add("<name>");
                    tabList.add("-entity");
                }
                if(args[0].equalsIgnoreCase("set-map") || args[0].equalsIgnoreCase("map-vote")) {
                    for(PHuntMap map : plugin.getGameManager().getMapManager().getAllMaps().keySet()) {
                        tabList.add(map.getName());
                    }
                }
                break;

                //ph set-team <name> <team>
            case 3:
                if(args[0].equalsIgnoreCase("set-team")) {
                    if(Bukkit.getPlayer(args[1]) != null || args[1].equalsIgnoreCase("-entity")) {
                        for(Teams teams : Teams.values()) {
                            tabList.add(teams.getName());
                        }
                    }
                }
                break;

        }
        return tabList;
    }
}
