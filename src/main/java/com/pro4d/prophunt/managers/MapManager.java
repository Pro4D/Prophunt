package com.pro4d.prophunt.managers;

import com.pro4d.prophunt.Prophunt;
import com.pro4d.prophunt.misc.PHuntMap;
import com.pro4d.prophunt.utils.PHuntMessages;
import com.pro4d.prophunt.utils.PHuntUtils;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class MapManager {

    private final Map<PHuntMap, Integer> allMaps;
    private final Map<UUID, PHuntMap> votedFor;
    private final List<BaseComponent> messages;
    private Location lobbySpawnpoint;
    private String currentMapName;

    private final Map<PHuntMap, Clipboard> mapSchematics;

    private final PHuntMessages messageUtils;

    private final Prophunt plugin;
    public MapManager(Prophunt plugin) {
        messageUtils = plugin.getMessages();
        this.plugin = plugin;

        allMaps = new HashMap<>();
        votedFor = new HashMap<>();

        messages = new ArrayList<>();
        mapSchematics = new HashMap<>();
    }


    public void registerMap(PHuntMap map) {
        // map = new PHuntMap(name, world, cornerOne, cornerTwo);
        allMaps.put(map, 0);
        saveMap(map);
    }

    public void saveMap(PHuntMap map) {
        String mapName = map.getName();
        com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(map.getWorld());
        File mapDir = new File(plugin.getMapPath());
        File mapFile = new File(mapDir.getAbsolutePath() + "/" + mapName + ".schem");

        if(!mapDir.exists()) {
            mapDir.mkdirs();
            try {
                mapFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        CuboidRegion region = new CuboidRegion(PHuntUtils.convertVector(map.getCornerOne()), PHuntUtils.convertVector(map.getCornerTwo()));
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);

        ForwardExtentCopy fec = new ForwardExtentCopy(world, region, clipboard, region.getMinimumPoint());
        fec.setCopyingBiomes(true);
        fec.setCopyingEntities(true);


        try {
            Operations.complete(fec);

        } catch (WorldEditException e) {
            throw new RuntimeException(e);
        }

        mapSchematics.put(map, clipboard);

        try {
            ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(Files.newOutputStream(mapFile.toPath()));
            writer.write(clipboard);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void resetMap(PHuntMap map) {
        if(!mapSchematics.containsKey(map)) return;
        Clipboard clipboard = mapSchematics.get(map);
        com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(map.getWorld());
        BlockVector3 origin = clipboard.getOrigin();

        EditSession editSession = WorldEdit.getInstance().newEditSession(world);

        Operation operation = new ClipboardHolder(clipboard)
                .createPaste(editSession)
                .to(origin)
                .ignoreAirBlocks(false)
                .build();

        try {
            Operations.complete(operation);
            editSession.close();

        } catch (WorldEditException e) {
            e.printStackTrace();
        }

    }

    public void createMapMessages() {
        for(PHuntMap map : allMaps.keySet()) {
            messages.add(messageUtils.createComponent("&e" + map.getName(), "/ph map-vote " + map.getName(), "&e[MAP VOTE] Click to vote for " + map.getName()));
        }
    }

    public PHuntMap getMap(String name) {
        for(PHuntMap map : allMaps.keySet()) {
            if(map.getName().equalsIgnoreCase(name)) return map;
        }
        return null;
    }

    public boolean isMap(String name) {
        for(PHuntMap map : allMaps.keySet()) {
            if(map.getName().equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    public Map<PHuntMap, Integer> getAllMaps() {
        return allMaps;
    }

    public List<BaseComponent> getMessages() {return messages;}

    public void setLobbySpawnpoint(Location lobbySpawnpoint) {
        this.lobbySpawnpoint = lobbySpawnpoint;
    }

    public Location getLobbySpawnpoint() {return lobbySpawnpoint;}

    public void setCurrentMapName(String currentMapName) {
        this.currentMapName = currentMapName;
    }

    public PHuntMap getCurrentMap() {return getMap(currentMapName);}

    public void voteForMap(String mapName, Player player) {
        if(!isMap(mapName)) {
            player.sendMessage(PHuntMessages.noMapWithThatName());
            return;
        }
        PHuntMap map = getMap(mapName);
        if(votedFor.containsKey(player.getUniqueId())) {
            if(votedFor.get(player.getUniqueId()) == map) {
                player.sendMessage(PHuntMessages.alreadyVotedForThisMap());
                return;
            }
        }

        votedFor.put(player.getUniqueId(), map);
        int votes = allMaps.get(map);
        allMaps.replace(map, (votes + 1));
        player.sendMessage(PHuntMessages.votedForMap().replace("%map_name%", mapName));
    }

    public PHuntMap getMapWithHighestVotes() {
        Map<Integer, PHuntMap> voteMap = new TreeMap<>();
        for(PHuntMap map : allMaps.keySet()) {
            int votes = allMaps.get(map);
            voteMap.put(votes, map);
        }

        List<Integer> voteOrder = new ArrayList<>(voteMap.keySet());
        Comparator<Integer> comp = Comparator.reverseOrder();
        voteOrder.sort(comp);

        int f = 0;
        if(!voteOrder.isEmpty()) {
            f = voteOrder.get(0);
        }


        List<PHuntMap> mapsWithHighestVotes = new ArrayList<>();
        for(PHuntMap map : allMaps.keySet()) {
            if(allMaps.get(map) == f) {
                mapsWithHighestVotes.add(map);
            }
        }

        if(!mapsWithHighestVotes.isEmpty()) {
            if(mapsWithHighestVotes.size() >= 2) {
                int r = PHuntUtils.randomInteger(0, mapsWithHighestVotes.size() - 1);
                return mapsWithHighestVotes.get(r);
            } else {
                return mapsWithHighestVotes.get(0);
            }
        }

        return null;
    }

    public PHuntMap getRandomMap() {
        int r = PHuntUtils.randomInteger(0, allMaps.size() - 1);
        int i = 0;
        for(PHuntMap map : allMaps.keySet()) {
            if(i == r) {
                return map;
            }
            i++;
        }
        return null;
    }

    public void clearVotes() {
        for(PHuntMap map : allMaps.keySet()) {
            allMaps.replace(map, 0);
        }
    }

    public Map<UUID, PHuntMap> getVotedFor() {
        return votedFor;
    }
}
