package com.pro4d.prophunt.config;

import com.pro4d.prophunt.Prophunt;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class PHuntConfig {

    private File file;
    private FileConfiguration config;
    private final String name;

    private final Prophunt plugin;
    public PHuntConfig(String name, Prophunt plugin) {
        this.name = name;
        this.plugin = plugin;
        plugin.reloadConfig();
        config = createConfig();
    }

    public void saveConfig() {
        try {
            config.save(file);

        } catch (IOException e) {
            plugin.getUtils().log(Level.CONFIG, "Could not save '" + name + ".yml' ");
        }
    }

    public void loadConfig() {
        config = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public String getName() {
        return name;
    }

    private FileConfiguration createConfig() {
        String configName = name + ".yml";
        file = new File(plugin.getDataFolder(), configName);
        if(!file.exists())
        {
            file.getParentFile().mkdirs();
            plugin.saveResource(configName, true);
            plugin.getUtils().log(Level.CONFIG, "Could not find '" + name + ".yml', creating one now...");
        }
        return YamlConfiguration.loadConfiguration(file);
    }

}
