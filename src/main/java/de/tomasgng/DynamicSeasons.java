package de.tomasgng;

import de.tomasgng.commands.DynamicSeasonsCMD;
import de.tomasgng.listeners.CreatureSpawnListener;
import de.tomasgng.listeners.PlayerPickupExperienceListener;
import de.tomasgng.listeners.ThunderChangeListener;
import de.tomasgng.listeners.WeatherChangeListener;
import de.tomasgng.placeholders.CurrentSeasonExpansion;
import de.tomasgng.placeholders.DurationExpansion;
import de.tomasgng.utils.bstats.Metrics;
import de.tomasgng.utils.managers.ConfigManager;
import de.tomasgng.utils.managers.MessageManager;
import de.tomasgng.utils.managers.SeasonManager;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public final class DynamicSeasons extends JavaPlugin {

    @Getter
    private static DynamicSeasons instance;
    @Getter
    private ConfigManager configManager;
    @Getter
    private SeasonManager seasonManager;
    @Getter
    private MessageManager messageManager;

    @Override
    public void onEnable() {
        instance = this;
        configManager = new ConfigManager();
        seasonManager = new SeasonManager();
        messageManager = new MessageManager();

        register();
    }

    private void register() {
        var manager = Bukkit.getPluginManager();

        manager.registerEvents(new CreatureSpawnListener(), this);
        manager.registerEvents(new PlayerPickupExperienceListener(), this);
        manager.registerEvents(new ThunderChangeListener(), this);
        manager.registerEvents(new WeatherChangeListener(), this);

        getCommand("dynamicseasons").setExecutor(new DynamicSeasonsCMD());

        Metrics metrics = new Metrics(this, 19158);
        metrics.addCustomChart(new Metrics.MultiLineChart("players_and_servers", () -> {
            Map<String, Integer> valueMap = new HashMap<>();
            valueMap.put("servers", 1);
            valueMap.put("players", Bukkit.getOnlinePlayers().size());
            return valueMap;
        }));

        if(Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new DurationExpansion().register();
            new CurrentSeasonExpansion().register();
        } else
            getLogger().warning("PlaceholderAPI not found. Placeholders will be disabled.");
    }

    @Override
    public void onDisable() {
        if(Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new DurationExpansion().unregister();
            new CurrentSeasonExpansion().unregister();
        }
    }
}
