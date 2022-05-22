package com.pro4d.prophunt.utils;

import com.pro4d.prophunt.Prophunt;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PHuntUtils {

    private final Logger logger;
    private static final List<Material> unsafeBlocks = new ArrayList<>();
    private static final DecimalFormat df;

    public PHuntUtils(Prophunt plugin) {
        this.logger = plugin.getLogger();
    }

    static {
        unsafeBlocks.add(Material.LAVA);
        unsafeBlocks.add(Material.WATER);
        unsafeBlocks.add(Material.MAGMA_BLOCK);
        unsafeBlocks.add(Material.FIRE);
        unsafeBlocks.add(Material.AIR);

        df = new DecimalFormat("#.##");

    }

    public void log(Level level, String message) {logger.log(level, PHuntMessages.translate(message));}

    public static int randomInteger(int min, int max) {
        return (int) Math.floor(Math.random() * (max - min + 1) + min);
    }

    public static double randomDouble(double min, double max) {
        return Double.parseDouble(df.format(Math.random() * (max - min + 1) + min));
    }

    public static Location randomLocation(World world, Vector min, Vector max) {
        int x = randomInteger(min.getBlockX(), max.getBlockX());
        int z = randomInteger(min.getBlockZ(), max.getBlockZ());

        int y = world.getHighestBlockYAt(x, z);

        return new Location(world, x, y, z);
    }

    public static BlockVector3 convertVector(Vector vector) {
        return BlockVector3.at(vector.getX(), vector.getY(), vector.getZ());
    }

    public static boolean isLocationUnsafe(Location location) {
        if(location == null) return true;

        Block bDown = location.clone().subtract(0, 1, 0).getBlock();
        bDown.getChunk().load(true);
        return unsafeBlocks.contains(bDown.getType());
    }

    public static ItemStack findItem(Inventory inv, ItemStack item) {
        for(int i = 0; i <= inv.getSize(); i++) {
            ItemStack itemStack = inv.getItem(i);
            if(itemStack == null) continue;

            if(itemStack.isSimilar(item)) return itemStack;
        }
            
        return null;
    }

    public static LivingEntity getEntityInLOS(Player player, int radius) {
        List<Entity> entities = player.getNearbyEntities(radius, radius, radius);

        Location loc = player.getEyeLocation();

        for(Entity entity : entities) {
            if(!(entity instanceof LivingEntity)) continue;
            if(!player.hasLineOfSight(entity)) continue;

            Vector entityDirection = ((LivingEntity) entity).getEyeLocation().toVector();
            Vector result = entityDirection.subtract(loc.toVector());

            double rND = result.clone().normalize().dot(loc.getDirection());

            if(rND >= 0.94 && rND <= 1) return (LivingEntity) entity;

        }

        return null;
    }

    public static boolean isNumber(String s) {
        try {
            Double.parseDouble(s);
        } catch (NumberFormatException a) {
            try {
                Integer.parseInt(s);
            } catch (NumberFormatException b) {
                return false;
            }
        }

        return true;
    }

}
