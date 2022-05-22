package com.pro4d.prophunt;

import com.pro4d.prophunt.commands.PHuntCommands;
import com.pro4d.prophunt.commands.PHuntTabCompleter;
import com.pro4d.prophunt.config.PHuntConfig;
import com.pro4d.prophunt.listener.PHuntListener;
import com.pro4d.prophunt.managers.PHuntGameManager;
import com.pro4d.prophunt.misc.PHuntMap;
import com.pro4d.prophunt.utils.PHuntMessages;
import com.pro4d.prophunt.utils.PHuntUtils;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class Prophunt extends JavaPlugin {

    private PHuntConfig mapConfig;
    private PHuntConfig swapConfig;
    private PHuntConfig settingsConfig;

    private PHuntUtils utils;
    private PHuntGameManager gameManager;
    private PHuntListener listener;
    private PHuntMessages messages;

    private static Scoreboard scoreboard;
    private final PluginManager pm = getServer().getPluginManager();
    private final Map<UUID, ItemStack> heads = new HashMap<>();

    private final String mapPath = this.getDataFolder().getAbsolutePath() + "/maps/";

    @Override
    public void onEnable() {
        // Plugin startup logic
        if(pm.getPlugin("WorldEdit") == null) {
            utils.log(Level.WARNING, "&4WorldEdit is required for this plugin, please install it!");
            pm.disablePlugin(this);
        }

        if(pm.getPlugin("MythicMobs") == null) {
            utils.log(Level.WARNING, "&4MythicMobs is required for this plugin, please install it");
            pm.disablePlugin(this);
        }

        if(pm.getPlugin("LibsDisguises") == null) {
            utils.log(Level.WARNING, "&4LibsDisguises is required for this plugin, please install it");
            pm.disablePlugin(this);
        }

        if(pm.getPlugin("ModelEngine") == null) {
            utils.log(Level.WARNING, "&4ModelEngine is required for this plugin, please install it");
            pm.disablePlugin(this);
        }

        if(getServer().getScoreboardManager() != null) scoreboard = getServer().getScoreboardManager().getNewScoreboard();


        utils = new PHuntUtils(this);
        messages = new PHuntMessages();

        mapConfig = new PHuntConfig("maps", this);
        swapConfig = new PHuntConfig("swappables", this);
        settingsConfig = new PHuntConfig("settings", this);

        gameManager = new PHuntGameManager(this);

        new PHuntCommands(this);
        new PHuntTabCompleter(this);

        listener = new PHuntListener(this);

        gameManager.getMapManager().getAllMaps().keySet().forEach(PHuntMap::createBlockList);

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if(gameManager != null) gameManager.cleanup();
    }


    public PHuntConfig getSettingsConfig() {
        return settingsConfig;
    }

    public PHuntConfig getMapConfig() {
        return mapConfig;
    }

    public PHuntConfig getSwapConfig() {
        return swapConfig;
    }

    public PHuntUtils getUtils() {return utils;}

    public PHuntGameManager getGameManager() {
        return gameManager;
    }

    public PHuntListener getListener() {
        return listener;
    }

    public PHuntMessages getMessages() {return messages;}

    public int getDistance() {
        return 7;
    }

    public Map<UUID, ItemStack> getHeads() {
        return heads;
    }

    public String getMapPath() {
        return mapPath;
    }

    public static Scoreboard getScoreboard() {
        return scoreboard;
    }

}
