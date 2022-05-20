package com.pro4d.prophunt.utils;

import com.pro4d.prophunt.enums.Teams;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PHuntMessages {

    private final List<BaseComponent> taunts;
    private final List<BaseComponent> teamMessages;
    private BaseComponent lockMessage;
    private BaseComponent unlockMessage;

    private static final Pattern pattern;

    public PHuntMessages() {
        taunts = new ArrayList<>();
        teamMessages = new ArrayList<>();
        createTaunts();
        createLockMessages();
        createTeamMessages();
    }

    private void createTeamMessages() {
        TextComponent hiders = createComponent(Teams.HIDERS.getColor() + "Join: " + Teams.HIDERS.getName(), "/ph set-team " + Teams.HIDERS.getName(), "Click to join Hiders team");
        teamMessages.add(hiders);

        TextComponent hunters = createComponent(Teams.HUNTERS.getColor() + "Join: " + Teams.HUNTERS.getName(), "/ph set-team " + Teams.HUNTERS.getName(), "Click to join Hunters team");
        teamMessages.add(hunters);

        TextComponent spectator = createComponent(Teams.DEAD.getColor() + "Join: " + Teams.DEAD.getName(), "/ph set-team " + Teams.DEAD.getName(), "Click to join Spectators team");
        teamMessages.add(spectator);

    }

    private void createTaunts() {
        TextComponent chicken = createComponent("[&eCHICKEN&r]","/ph play-sound chicken","Click to play chicken taunt!");
        taunts.add(chicken);

        TextComponent ironGolem = createComponent("[&lGOLEM&r]","/ph play-sound iron-golem","Click to play golem taunt!");
        taunts.add(ironGolem);

        TextComponent anvil = createComponent("[&8ANVIL&r]","/ph play-sound anvil","Click to play anvil taunt!");
        taunts.add(anvil);

        TextComponent dragon = createComponent("[&5DRAGON&r]","/ph play-sound dragon","Click to play dragon taunt!");
        taunts.add(dragon);

        TextComponent enderman = createComponent("[&dENDERMAN&r]","/ph play-sound enderman","Click to play enderman taunt!");
        taunts.add(enderman);

        TextComponent curse = createComponent("[&4CURSE&r]","/ph play-sound curse","Click to play curse taunt!");
        taunts.add(curse);

        TextComponent wolf = createComponent("[&7WOLF&r]","/ph play-sound wolf","Click to play wolf taunt!");
        taunts.add(wolf);

        TextComponent skeleton = createComponent("[SKELETON]","/ph play-sound skeleton","Click to play skeleton taunt!");
        taunts.add(skeleton);

        TextComponent cave = createComponent("[&7CAVE]","/ph play-sound cave","Click to play cave taunt!");
        taunts.add(cave);

        TextComponent firework = createComponent("[FIREWORK]","/ph play-sound firework","Click to launch firework!");
        taunts.add(firework);

        //ADD COLOR TO FIREWORK
        //GRADIENTS?

    }

    private void createLockMessages() {
        KeybindComponent sneakKey = createKeybindComponent("key.sneak");

        TextComponent lockStart = createComponent("Press [ ", null, null);
        TextComponent lockEnd = createComponent(" ] to Lock Disguise", null, null);
        lockStart.addExtra(sneakKey);
        lockStart.addExtra(lockEnd);
        lockStart.setColor(net.md_5.bungee.api.ChatColor.YELLOW);

        TextComponent unlockStart = createComponent("Press [ ", null, null);
        TextComponent unlockEnd = createComponent(" ] to Unlock Disguise", null, null);
        unlockStart.addExtra(sneakKey);
        unlockStart.addExtra(unlockEnd);
        unlockStart.setColor(net.md_5.bungee.api.ChatColor.YELLOW);

        lockMessage = lockStart;
        unlockMessage = unlockStart;

    }
    
    public KeybindComponent createKeybindComponent(String text) {
        return new KeybindComponent(text);
    }    
    
    public TextComponent createComponent(String text, @Nullable String click, @Nullable String hover) {
        TextComponent component = makeChatColor(text);
        if(click != null) component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, click));
        if(hover != null) component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(translate(hover))));

        return component;
    }

    public TextComponent makeChatColor(String text) {
        TextComponent comp = new TextComponent("");
        List<String> list = new ArrayList<>(Arrays.asList(text.split("&")));

        list.removeAll(Arrays.asList("", null));

        for(String s : list) {
            char c = s.charAt(0);
            net.md_5.bungee.api.ChatColor color = net.md_5.bungee.api.ChatColor.WHITE;
            String txt = s;
            if(ChatColor.getByChar(c) != null) {
                txt = s.replaceFirst(String.valueOf(c), "");
                color = net.md_5.bungee.api.ChatColor.getByChar(c);
            }
            TextComponent added = new TextComponent(txt);
            added.setColor(color);
            comp.addExtra(added);
        }

        return comp;
    }
    
    public List<BaseComponent> getTaunts() {
        return taunts;
    }

    public List<BaseComponent> getTeamMessages() {
        return teamMessages;
    }

    public BaseComponent getLockMessage() {
        return lockMessage;
    }

    public BaseComponent getUnlockMessage() {
        return unlockMessage;
    }

    static {
        pattern = Pattern.compile("#[a-fA-F0\\d]{6}");
    }

    public static String translate(String message) {
        Matcher match = pattern.matcher(message);
        while(match.find()) {
            String hex = message.substring(match.start(), match.end());
            net.md_5.bungee.api.ChatColor color = net.md_5.bungee.api.ChatColor.of(hex);
            if(color != null) {
                message = message.replace(hex, net.md_5.bungee.api.ChatColor.of(hex) + "");
                match = pattern.matcher(message);
            }
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String reloadMessage() {
        return translate("&aSuccessfully reloaded configs!");
    }

    public static String onlyPlayersMessage() {
        return translate("&cOnly players can use this command");
    }

    public static String invalidCommandUsageMessage() {
        return translate("&cInvalid command usage!");
    }

    public static String cannotUseCommandDuringGame() {return translate("&cYou can't use this command during an active game!");}

    public static String noGameIsRunning() {return translate("&cCould not find any game currently running!");}

    public static String noMapWithThatName() {return translate("&cNo map found with the name: &r %input%");}

    public static String votedForMap() {return translate("&aVoted for map: &r%map_name%");}

    public static String invalidTeam() {
        StringBuilder builder = new StringBuilder();
        for(Teams team : Teams.values()) {
            if(builder.length() == 0) {
                builder.append(translate(team.getColor() + team.getName()));
            } else {
                builder.append(translate("&r, " + team.getColor() + team.getName()));
            }
        }
        return translate("&cCould not find any team named %team%, here are the following teams: " + builder);
    }

    public static String invalidMob() {return translate("&cCould not find any living entity within %num% blocks infront of you.");}

    public static String notEnoughPlayers() {return translate("&cNot enough players to start!");}

    public static String alreadyVotedForThisMap() {return translate("&cCannot vote for the same map!");}

    public static String cannotLockHere() {return translate("&cCannot lock here!");}

    public static String cantDisguiseAsPlayer() {return translate("&cCannot disguise as a player!");}

    public static String teleportedTo() {return translate("&aTeleported to  " + "&r&e%target%");}

}
