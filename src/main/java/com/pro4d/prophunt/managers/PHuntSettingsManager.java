package com.pro4d.prophunt.managers;

import com.pro4d.prophunt.Prophunt;
import com.pro4d.prophunt.config.PHuntConfig;
import com.pro4d.prophunt.misc.PHuntMap;
import com.pro4d.prophunt.utils.PHuntMessages;
import com.pro4d.prophunt.utils.PHuntUtils;
import com.ticxo.modelengine.api.ModelEngineAPI;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class PHuntSettingsManager {

    private int roundTime;
    private int voteTime;
    private int hunterFrozenTime;
    private int minimumPlayerCount;
    private boolean allowBlockBreaking;
    //private boolean teamGlowing;
    private boolean autoStart;
    private boolean pvpEnabled;
    private boolean teamPVPEnabled;


    private ItemStack propSwapper;
    private ItemStack propTaunter;
    private ItemStack hunterWeaponItem;

    private final Map<Material, Double> blockHealth;
    private final Map<String, Double> megHealth;
    private final Map<EntityType, Double> entityHealth;

    private final Prophunt plugin;
    private final PHuntGameManager gameManager;
    private final PHuntUtils utils;


    public PHuntSettingsManager(Prophunt plugin, PHuntGameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        utils = plugin.getUtils();

        blockHealth = new HashMap<>();
        megHealth = new HashMap<>();
        entityHealth = new HashMap<>();

    }


    public void validateMapConfig() {
        FileConfiguration config = plugin.getMapConfig().getConfig();

        String path = "Maps.";
        if(!config.isConfigurationSection(path)) config.createSection("Maps");
        for(String key : config.getConfigurationSection(path).getKeys(false)) {
            String keyPath = path + key;

            if(key.equalsIgnoreCase("Lobby")) {
                if(!config.contains(keyPath + ".spawnpoint")) continue;
                Location loc = validateLobbySpawnpoint(plugin.getMapConfig(), keyPath + ".spawnpoint");
                if(loc != null) {
                    gameManager.getMapManager().setLobbySpawnpoint(loc);
                    utils.log(Level.WARNING, "&cSuccessfully set lobby spawnpoint!");
                }

            } else {
                PHuntMap map = validateMap(plugin.getMapConfig(), keyPath, key);
                if(map != null) {
                    gameManager.getMapManager().registerMap(map);
                    utils.log(Level.WARNING, "Registered new map: " + map.getName());
                }
            }

        }

    }

    public void validateSettingsConfig() {
        FileConfiguration config = plugin.getSettingsConfig().getConfig();

        setDefaultConfigValue(config, "round-time", 600);
        roundTime = config.getInt("round-time");

        setDefaultConfigValue(config, "vote-time", 120);
        voteTime = config.getInt("vote-time");

        setDefaultConfigValue(config, "hunter-time", 20);
        hunterFrozenTime = config.getInt("hunter-time");

        setDefaultConfigValue(config, "block-break", false);
        allowBlockBreaking = config.getBoolean("block-break");

//        setDefaultConfigValue(config, "use-glowing", true);
//        teamGlowing = config.getBoolean("use-glowing");

        setDefaultConfigValue(config, "minimum-players", 3);
        minimumPlayerCount = config.getInt("minimum-players");

        setDefaultConfigValue(config, "auto-start-when-enough-players", false);
        autoStart = config.getBoolean("auto-start-when-enough-players");

        setDefaultConfigValue(config, "pvp-enabled", true);
        pvpEnabled = config.getBoolean("pvp-enabled");

        setDefaultConfigValue(config, "team-damage", true);
        teamPVPEnabled = config.getBoolean("team-damage");

        String swapPath = "prop-swap-item";
        String tauntPath = "prop-taunt-item";
        String hunterWeaponPath = "hunter-weapon";

        hunterWeaponItem = getItemFromConfig(config, hunterWeaponPath, Material.DIAMOND_SWORD.name());

        propSwapper = getItemFromConfig(config, swapPath, Material.NETHER_STAR.name());
        ItemMeta swapperMeta = propSwapper.getItemMeta();
        assert swapperMeta != null;
        swapperMeta.setDisplayName(PHuntMessages.translate("#874CDDProp Swapper"));
        propSwapper.setItemMeta(swapperMeta);


        propTaunter = getItemFromConfig(config, tauntPath, Material.GRAY_DYE.name());
        ItemMeta taunterMeta = propTaunter.getItemMeta();
        assert taunterMeta != null;
        taunterMeta.setDisplayName(PHuntMessages.translate("#94F02BTaunter"));
        propTaunter.setItemMeta(taunterMeta);

    }

    public void validateSwappablesConfig() {
        FileConfiguration config = plugin.getSwapConfig().getConfig();
        String path = "Blocks-Mobs.";
        String healthPath = ".health";
        if(!config.isConfigurationSection(path)) config.createSection(path);

        setDefaultConfigValue(config, path + "Anvils.health", 10);
        setDefaultConfigValue(config, path + "Crafting_Table.health", 5);

        //double check
        for(String key : config.getConfigurationSection(path).getKeys(false)) {
            String health = path + key + healthPath;

            if(toMaterial(key) != null) {
                Material mat = toMaterial(key);
                if(config.isInt(health)) {
                    double h = config.getInt(health);
                    blockHealth.put(mat, h);

                } else if(config.isDouble(health)) {
                    double h = config.getDouble(health);
                    blockHealth.put(mat, h);
                }

            } else if(ModelEngineAPI.getModelBlueprint(key) != null) {
                if(config.isInt(health)) {
                    double h = config.getInt(health);
                    megHealth.put(key, h);

                } else if(config.isDouble(health)) {
                    double h = config.getDouble(health);
                    megHealth.put(key, h);
                }

            } else if(getEntityType(key) != null) {
                EntityType type = getEntityType(key);
                if(config.isInt(health)) {
                    double h = config.getInt(health);
                    entityHealth.put(type, h);
                } else if(config.isDouble(health)) {
                    double h = config.getDouble(healthPath);
                    entityHealth.put(type, h);
                }
            }
        }
    }



    private Material toMaterial(String mat) {
        for(Material material : Material.values()) {
            if(material.name().equalsIgnoreCase(mat)) return material;
        }
        return null;
    }


    public EntityType getEntityType(String type) {
        for(EntityType entityType : EntityType.values()) {
            if(entityType.name().equalsIgnoreCase(type)) return entityType;
        }
        return null;
    }


    public Location validateLobbySpawnpoint(PHuntConfig pHuntConfig, String path) {
        FileConfiguration config = pHuntConfig.getConfig();
        if(!config.isConfigurationSection(path)) {
            utils.log(Level.WARNING, "&ePath '&r" + path + "&e' not found!");
            return null;
        }

        Location loc = new Location(null, 0, 0, 0);
        boolean worldFound = false;
        boolean xFound = false;
        boolean yFound = false;
        boolean zFound = false;

        assert config.getConfigurationSection(path) != null;
        for(String key : config.getConfigurationSection(path).getKeys(false)) {
            String keyPath = path + "." + key;
            String keyValue = String.valueOf(config.get(keyPath));

            if(PHuntUtils.isNumber(keyValue)) {
                double coordinate = Double.parseDouble(keyValue);
                switch (key) {
                    case "x":
                        loc.setX(coordinate);
                        xFound = true;
                        continue;

                    case "y":
                        loc.setY(coordinate);
                        yFound = true;
                        continue;

                    case "z":
                        loc.setZ(coordinate);
                        zFound = true;
                }

            } else if(key.equalsIgnoreCase("world")) {

                List<String> worldNames = new ArrayList<>();
                plugin.getServer().getWorlds().forEach(w -> worldNames.add(w.getName()));
                if(worldNames.contains(keyValue)) {
                    World world = plugin.getServer().getWorld(keyValue);
                    loc.setWorld(world);
                    worldFound = true;
                }

            } else {
                utils.log(Level.WARNING, "&eValue of " + key + " is invalid for Lobby spawnpoint!");
                return null;
            }

        }
        if(!worldFound || !xFound || !yFound || !zFound) {
            utils.log(Level.WARNING, "&4AE515Could not set lobby spawnpoint!");
            return null;
        }

        return loc;
    }

    public Vector validateVector(PHuntConfig pHuntConfig, String path) {
        FileConfiguration config = pHuntConfig.getConfig();
        if(!config.isConfigurationSection(path)) {
            utils.log(Level.WARNING, "&ePath '&r" + path + "&e' not found!");
            return null;
        }

        Vector vector = new Vector();
        boolean xFound = false;
        boolean yFound = false;
        boolean zFound = false;

        assert config.getConfigurationSection(path) != null;
        for(String key : config.getConfigurationSection(path).getKeys(false)) {
            String keyPath = path + "." + key;
            if(config.get(keyPath) == null) continue;
            String keyValue = String.valueOf(config.get(keyPath));

            if(PHuntUtils.isNumber(keyValue)) {
                double coordinate = Double.parseDouble(keyValue);
                switch (key) {
                    case "x":
                        vector.setX(coordinate);
                        xFound = true;
                        continue;

                    case "y":
                        vector.setY(coordinate);
                        yFound = true;
                        continue;

                    case "z":
                        vector.setZ(coordinate);
                        zFound = true;
                }

            } else {
                utils.log(Level.WARNING, "&eValue of " + key + " is invalid for vector at path: '&r" + path + "&e' in config: &r" + config.getName());
                return null;
            }

        }
        if(!xFound || !yFound || !zFound) {
            utils.log(Level.WARNING, "&cCould not get all 3 coordinates for vector at path: '&r" + path + "&e' in config: &r" + config.getName());
            return null;
        }

        return vector;
    }

    public PHuntMap validateMap(PHuntConfig pHuntConfig, String path, String mapName) {
        FileConfiguration config = pHuntConfig.getConfig();
        if(!config.isConfigurationSection(path)) {
            utils.log(Level.WARNING, "&ePath '&r" + path + "&e' not found!");
            return null;
        }

        boolean worldFound = false;
        boolean cornerOneSet = false;
        boolean cornerTwoSet = false;
        World world = null;
        Vector vectorOne = null;
        Vector vectorTwo = null;

        assert config.getConfigurationSection(path) != null;
        for(String key : config.getConfigurationSection(path).getKeys(false)) {
            String keyPath = path + "." + key;
            if(config.get(keyPath) == null) continue;

            if(key.equalsIgnoreCase("world")) {
                List<String> worldNames = new ArrayList<>();
                String keyValue = String.valueOf(config.get(keyPath));

                plugin.getServer().getWorlds().forEach(w -> worldNames.add(w.getName()));
                if(worldNames.contains(keyValue)) {
                    world = plugin.getServer().getWorld(keyValue);
                    worldFound = true;
                }

            } else if(key.equalsIgnoreCase("corner-1")) {
                Vector cornerOne = validateVector(pHuntConfig, keyPath);
                if(cornerOne != null) {
                    vectorOne = cornerOne;
                    cornerOneSet = true;
                }

            } else if(key.equalsIgnoreCase("corner-2")) {
                Vector cornerTwo = validateVector(pHuntConfig, keyPath);
                if(cornerTwo != null) {
                    vectorTwo = cornerTwo;
                    cornerTwoSet = true;
                }

            }
        }
        if(!worldFound || !cornerOneSet || !cornerTwoSet) {
            utils.log(Level.WARNING, "&cFailed to register map:&r " + mapName);
            return null;
        }

        return new PHuntMap(mapName, world, vectorOne, vectorTwo);
    }


    public int getRoundTime() {return roundTime;}

    public int getVoteTime() {return voteTime;}

    public int getHunterFrozenTime() {return hunterFrozenTime;}

    public int getMinimumPlayerCount() {return minimumPlayerCount;}

    public boolean allowBlockBreaking() {return allowBlockBreaking;}

//    public boolean allowTeamGlowing() {
//        return teamGlowing;
//    }

    public boolean autoStart() {return autoStart;}

    public boolean allowPVP() {return pvpEnabled;}

    public boolean allowTeamPVP() {return teamPVPEnabled;}

    public ItemStack getPropSwapper() {return propSwapper;}

    public ItemStack getPropTaunter() {return propTaunter;}

    public ItemStack getHunterWeaponItem() {return hunterWeaponItem;}


    public ItemStack convertItem(String s) {
        Material mat = toMaterial(s);
        if(mat != null) {
            return new ItemStack(mat);

        } else {
            if(MythicBukkit.inst().getItemManager().getItem(s).isPresent()) {
                return MythicBukkit.inst().getItemManager().getItemStack(s);
            }
        }

        utils.log(Level.WARNING, "No Vanilla/MythicMobs items found for " + s);
        return null;
    }

    public void setDefaultConfigValue(FileConfiguration config, String path, Object val) {
        if(!config.contains(path)) {
            config.set(path, val);
        }
    }

    public ItemStack getItemFromConfig(FileConfiguration config, String path, Object val) {
        ItemStack item;
        String pathValue = config.getString(path);

        if(pathValue == null) {
            setDefaultConfigValue(config, path, val);
        }

        item = convertItem(pathValue);
        return item;
    }

    public Map<Material, Double> getBlockHealth() {return blockHealth;}

    public Map<String, Double> getMegHealth() {return megHealth;}

    public Map<EntityType, Double> getEntityHealth() {return entityHealth;}

    public Prophunt getPlugin() {return plugin;}

}
