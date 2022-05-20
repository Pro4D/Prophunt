package com.pro4d.prophunt.managers;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;

public class BarManager {

    private final BossBar bar;
    public BarManager() {
        bar = Bukkit.createBossBar("Prophunt by Pro4D", BarColor.PINK, BarStyle.SOLID);
    }

    public BossBar getBar() {
        return bar;
    }

}
