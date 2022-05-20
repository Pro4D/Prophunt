package com.pro4d.prophunt.enums;

import com.pro4d.prophunt.Prophunt;
import com.pro4d.prophunt.managers.PHuntGameManager;
import com.pro4d.prophunt.utils.PHuntMessages;
import org.apache.commons.lang.WordUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public enum Teams {

    HIDERS(ChatColor.RED),
    HUNTERS(ChatColor.BLUE),
    DEAD(ChatColor.GRAY);

    private final List<UUID> members;
    private final Team team;
    private final String name;
    private final ChatColor color;
    Teams(ChatColor color) {
        name = WordUtils.capitalizeFully(this.toString());
        members = new ArrayList<>();
        team = Prophunt.getScoreboard().registerNewTeam(name);
        this.color = color;
        team.setColor(getColor());
        team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
    }

    public List<UUID> getMembers() {
        return members;
    }

    public String getName() {
        return name;
    }

    public ChatColor getColor() {return color;}

    public void addMember(LivingEntity entity) {
        if(entity instanceof Player) {
            Player player = (Player) entity;
            player.setScoreboard(Prophunt.getScoreboard());
            team.addEntry(entity.getName());

        } else {
            team.addEntry(entity.getUniqueId().toString());
        }

        members.add(entity.getUniqueId());
    }

    public void removeMember(LivingEntity entity) {
        if(entity instanceof Player) {
            Player player = (Player) entity;
            team.removeEntry(player.getName());

        } else {
            team.removeEntry(entity.getUniqueId().toString());
        }
        members.remove(entity.getUniqueId());
    }

    public static void clearTeam(LivingEntity entity, PHuntGameManager gm, boolean message) {
        for(Teams team : Teams.values()) {
            if(team.getMembers().contains(entity.getUniqueId())) {
                gm.removeFromTeam(team, entity, message);
            }
        }
    }

    public static Teams getPlayerTeam(LivingEntity entity) {
        for(Teams team : Teams.values()) {
            if(team.getMembers().contains(entity.getUniqueId())) {
                return team;
            }
        }
        return null;
    }

    public static Teams getTeam(String name) {
        for(Teams team : Teams.values()) {
            if(team.getName().equalsIgnoreCase(name)) {
                return team;
            }
        }
        return null;
    }

}
