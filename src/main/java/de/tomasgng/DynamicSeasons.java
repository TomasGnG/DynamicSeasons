package de.tomasgng;

import de.tomasgng.commands.DynamicSeasonsCMD;
import de.tomasgng.listeners.*;
import de.tomasgng.placeholders.CurrentSeasonExpansion;
import de.tomasgng.placeholders.DurationExpansion;
import de.tomasgng.utils.bstats.Metrics;
import de.tomasgng.utils.managers.ConfigManager;
import de.tomasgng.utils.managers.MessageManager;
import de.tomasgng.utils.managers.SeasonManager;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;
import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public final class DynamicSeasons extends JavaPlugin {

    @Getter
    private static DynamicSeasons instance;
    @Getter
    private ConfigManager configManager;
    @Getter
    private SeasonManager seasonManager;
    @Getter
    private MessageManager messageManager;
    @Getter @Setter
    private boolean PLUGIN_DISABLED = false;
    @Getter @Setter
    private Component DISABLED_MESSAGE;

    @Override
    public void onEnable() {
        instance = this;
        configManager = new ConfigManager();
        seasonManager = new SeasonManager();
        messageManager = new MessageManager();

        register();
        updateCheck(false);
    }

    @Override
    public void onDisable() {
        if(Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new DurationExpansion().unregister();
            new CurrentSeasonExpansion().unregister();
        }
    }

    private void register() {
        var manager = Bukkit.getPluginManager();

        manager.registerEvents(new CreatureSpawnListener(), this);
        manager.registerEvents(new PlayerPickupExperienceListener(), this);
        manager.registerEvents(new ThunderChangeListener(), this);
        manager.registerEvents(new WeatherChangeListener(), this);
        manager.registerEvents(new BlockGrowListener(), this);
        manager.registerEvents(new BlockSpreadListener(), this);
        manager.registerEvents(new StructureGrowListener(), this);
        manager.registerEvents(new EntityDeathListener(), this);
        manager.registerEvents(new EntityDamageByEntityListener(), this);
        manager.registerEvents(new PlayerJoinListener(), this);

        getServer().getCommandMap().register("dynamicseasons", new DynamicSeasonsCMD());

        var metrics = new Metrics(this, 19158);

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

    public String updateCheck(boolean silent) {
        var currentVersion = getDescription().getVersion();
        String latestVersion = "";

        try {
            var url = new URI("https://pastebin.com/raw/DZXYPzR7").toURL();
            var scanner = new Scanner(url.openStream());
            var sb = new StringBuilder();

            while(scanner.hasNext()) {
                sb.append(scanner.next());
            }

            latestVersion = sb.toString();
        } catch (Exception e) {
            getLogger().severe("Update checker failed.");
            return null;
        }

        if(!silent) {
            if(currentVersion.equalsIgnoreCase(latestVersion)) {
                Bukkit.getConsoleSender().sendMessage(MiniMessage.miniMessage().deserialize("<gradient:#55C156:#FFFF00:#FFA500:#87CEFA>DynamicSeasons</gradient> <dark_gray>| <green>Using the latest version(" + currentVersion + ")."));
                Bukkit.getConsoleSender().sendMessage(MiniMessage.miniMessage().deserialize("<gradient:#55C156:#FFFF00:#FFA500:#87CEFA>DynamicSeasons</gradient> <dark_gray>| <green>Thank you for using my plugin ;)"));
                return null;
            }

            Bukkit.getConsoleSender().sendMessage(MiniMessage.miniMessage().deserialize("<gradient:#55C156:#FFFF00:#FFA500:#87CEFA>DynamicSeasons</gradient> <dark_gray>| <yellow>Using an outdated version(" + currentVersion + "). Newest version " + latestVersion));
            Bukkit.getConsoleSender().sendMessage(MiniMessage.miniMessage().deserialize("<gradient:#55C156:#FFFF00:#FFA500:#87CEFA>DynamicSeasons</gradient> <dark_gray>| <yellow>Download: https://www.spigotmc.org/resources/dynamicseasons-%E2%8C%9B-enhance-your-survival-experience-%E2%9C%85.111362/"));
            return latestVersion;
        }

        if(currentVersion.equalsIgnoreCase(latestVersion))
            return null;

        return latestVersion;
    }

    public void updatePlugin(@Nullable Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            if(updateCheck(true) == null) return;

            var downloadFile = Path.of(getServer().getUpdateFolderFile().getPath(),"DynamicSeasons.jar").toFile();

            try {
                FileUtils.copyURLToFile(new URI("https://api.spiget.org/v2/resources/111362/download").toURL(), downloadFile, 3000, 3000);
                Bukkit.getConsoleSender().sendMessage(MiniMessage.miniMessage().deserialize("<gradient:#55C156:#FFFF00:#FFA500:#87CEFA>DynamicSeasons</gradient> <dark_gray>| <green>The latest version was successfully downloaded and will be used on the next restart!"));

                if(player != null)
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<gradient:#55C156:#FFFF00:#FFA500:#87CEFA>DynamicSeasons</gradient> <dark_gray>| <green>The latest version was successfully downloaded and will be used on the next restart!"));
            } catch (Exception e) {
                getLogger().severe("Download error!\nError: " + e.getMessage());
            }
        });
    }
}
