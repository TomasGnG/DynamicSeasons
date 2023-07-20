package de.tomasgng.utils.managers;

import de.tomasgng.DynamicSeasons;
import de.tomasgng.utils.enums.SeasonType;
import lombok.SneakyThrows;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.time.Duration;
import java.util.List;

public class MessageManager {

    private final File folder = new File("plugins/DynamicSeasons");
    private final File file = new File("plugins/DynamicSeasons/messages.yml");
    private YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
    private final MiniMessage mm = MiniMessage.miniMessage();

    public MessageManager() {
        createFiles();
    }

    @SneakyThrows
    private void createFiles() {
        if(!folder.exists()) folder.mkdirs();
        if(!file.exists()) {
            file.createNewFile();

            cfg.set("prefix", "<gradient:#55C156:#FFFF00:#FFA500:#87CEFA>DynamicSeasons</gradient> <dark_gray>|");
            cfg.set("command.noPermission", "%prefix% <gray>You have no permission to use this command.");
            cfg.set("command.usage", "%prefix% <gray>Usage: <yellow>/dynseasons setseason <spring|summer|fall|winter>");
            cfg.set("command.seasonAlreadyActive", "%prefix% <gray>This season is already active.");
            cfg.set("command.seasonChanged", "%prefix% <gray>You successfully changed the season from <yellow>%seasonBefore% <gray>to <green>%newSeason%<gray>.");
            cfg.setComments("prefix", List.of("Here you can change the message outputs.",
                    "The messages need to be in MiniMessage format.",
                    "MiniMessage Help: https://docs.advntr.dev/minimessage/index.html",
                    "Here you can test if your messages are valid: https://webui.advntr.dev/"));
            cfg.set("season_change.broadcast.enabled", true);
            cfg.set("season_change.broadcast.text", "%prefix% <gray>The season was changed from <yellow>%seasonBefore% <gray>to <green>%newSeason%<gray>.");
            cfg.set("season_change.title.enabled", true);
            cfg.set("season_change.title.title", "<yellow>Season Change");
            cfg.set("season_change.title.subtitle", "<italic><dark_gray>-> <green>%newSeason%");
            cfg.set("season_change.title.times.fadein", 1);
            cfg.set("season_change.title.times.stay", 4);
            cfg.set("season_change.title.times.fadeout", 1);
            cfg.setComments("season_change.title.times", List.of("Time in seconds"));

            save();
        }
    }

    @SneakyThrows
    private void save() {
        cfg.save(file);
        reload();
    }

    private void reload() {
        cfg = YamlConfiguration.loadConfiguration(file);
    }

    private String getPrefixRaw() {
        var prefix = cfg.getString("prefix");
        if(prefix == null) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid prefix.");
            return "<gradient:#55C156:#FFFF00:#FFA500:#87CEFA>DynamicSeasons</gradient> <dark_gray>|";
        }
        try {
            mm.deserializeOrNull(prefix);
        } catch (Exception e) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid prefix format.");
        }
        return prefix;
    }

    public Component getCMDNoPermissionComponent() {
        var msg = cfg.getString("command.noPermission");
        if(msg == null) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid no permission message.");
            return mm.deserialize("%prefix% <gray>You have no permission to use this command.".replace("%prefix%", getPrefixRaw()));
        }
        msg = msg.replace("%prefix%", getPrefixRaw());
        try {
            mm.deserialize(msg);
        } catch (Exception e) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid no permission format.\nError: " + e.getMessage());
            return mm.deserialize("%prefix% <gray>You have no permission to use this command.".replace("%prefix%", getPrefixRaw()));
        }
        return mm.deserialize(msg);
    }

    public Component getCMDUsageComponent() {
        var msg = cfg.getString("command.usage");
        if(msg == null) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid usage message.");
            return mm.deserialize("%prefix% <gray>Usage: <yellow>/dynseasons setseason <spring|summer|fall|winter>".replace("%prefix%", getPrefixRaw()));
        }
        msg = msg.replace("%prefix%", getPrefixRaw());
        try {
            mm.deserialize(msg);
        } catch (Exception e) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid usage format.\nError: " + e.getMessage());
            return mm.deserialize("%prefix% <gray>Usage: <yellow>/dynseasons setseason <spring|summer|fall|winter>".replace("%prefix%", getPrefixRaw()));
        }
        return mm.deserialize(msg);
    }

    public Component getCMDSeasonAlreadyActiveComponent() {
        var msg = cfg.getString("command.seasonAlreadyActive");
        if(msg == null) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid season already active message.");
            return mm.deserialize("%prefix% <gray>This season is already active.".replace("%prefix%", getPrefixRaw()));
        }
        msg = msg.replace("%prefix%", getPrefixRaw());
        try {
            mm.deserialize(msg);
        } catch (Exception e) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid season already active format.\nError: " + e.getMessage());
            return mm.deserialize("%prefix% <gray>This season is already active.".replace("%prefix%", getPrefixRaw()));
        }
        return mm.deserialize(msg);
    }

    public Component getCMDSeasonChangedComponent(SeasonType seasonBefore, SeasonType newSeason) {
        var configManager = DynamicSeasons.getInstance().getConfigManager();
        var msg = cfg.getString("command.seasonChanged");
        if(msg == null) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid season changed message.");
            return mm.deserialize("%prefix% <gray>You successfully changed the season from <yellow>%seasonBefore% <gray>to <green>%newSeason%<gray>."
                    .replace("%prefix%", getPrefixRaw())
                    .replace("%seasonBefore%", configManager.getCurrentSeasonText(seasonBefore))
                    .replace("%newSeason%", configManager.getCurrentSeasonText(newSeason)));
        }
        msg = msg.replace("%prefix%", getPrefixRaw())
                .replace("%seasonBefore%", configManager.getCurrentSeasonText(seasonBefore))
                .replace("%newSeason%", configManager.getCurrentSeasonText(newSeason));
        try {
            mm.deserializeOrNull(msg);
        } catch (Exception e) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid season changed format.\nError: " + e.getMessage());
            return mm.deserialize("%prefix% <gray>You successfully changed the season from <yellow>%seasonBefore% <gray>to <green>%newSeason%<gray>."
                    .replace("%prefix%", getPrefixRaw())
                    .replace("%seasonBefore%", configManager.getCurrentSeasonText(seasonBefore))
                    .replace("%newSeason%", configManager.getCurrentSeasonText(newSeason)));
        }
        return mm.deserialize(msg);
    }

    public Component getSeasonChangeBroadcastComponent(SeasonType seasonBefore, SeasonType newSeason) {
        var configManager = DynamicSeasons.getInstance().getConfigManager();
        var msg = cfg.getString("season_change.broadcast.text");
        if(msg == null) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid season change broadcast message.");
            return mm.deserialize("%prefix% <gray>The season was changed from <yellow>%seasonBefore% <gray>to <green>%newSeason%<gray>."
                    .replace("%prefix%", getPrefixRaw())
                    .replace("%seasonBefore%", configManager.getCurrentSeasonText(seasonBefore))
                    .replace("%newSeason%", configManager.getCurrentSeasonText(newSeason)));
        }
        msg = msg.replace("%prefix%", getPrefixRaw())
                .replace("%seasonBefore%", configManager.getCurrentSeasonText(seasonBefore))
                .replace("%newSeason%", configManager.getCurrentSeasonText(newSeason));
        try {
            mm.deserializeOrNull(msg);
        } catch (Exception e) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid season change broadcast format.\nError: " + e.getMessage());
            return mm.deserialize("%prefix% <gray>The season was changed from <yellow>%seasonBefore% <gray>to <green>%newSeason%<gray>."
                    .replace("%prefix%", getPrefixRaw())
                    .replace("%seasonBefore%", configManager.getCurrentSeasonText(seasonBefore))
                    .replace("%newSeason%", configManager.getCurrentSeasonText(newSeason)));
        }
        return mm.deserialize(msg);
    }

    public boolean isSeasonChangeBroadcastEnabled() {
        return cfg.getBoolean("season_change.broadcast.enabled");
    }

    public boolean isSeasonChangeTitleEnabled() {
        return cfg.getBoolean("season_change.title.enabled");
    }

    public Title getSeasonChangeTitleComponent(SeasonType seasonBefore, SeasonType newSeason) {
        var configManager = DynamicSeasons.getInstance().getConfigManager();
        var title = cfg.getString("season_change.title.title");
        var subtitle = cfg.getString("season_change.title.subtitle");
        var fadein = cfg.getInt("season_change.title.times.fadein");
        var stay = cfg.getInt("season_change.title.times.stay");
        var fadeout = cfg.getInt("season_change.title.times.fadeout");
        var defaultTitleReturn = Title.title(mm.deserialize("<yellow>Season Change"), mm.deserialize("<italic><dark_gray>-> <green>%newSeason%"
                .replace("%newSeason%", configManager.getCurrentSeasonText(newSeason))), Title.DEFAULT_TIMES);

        if(title == null) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid season change title.");
            return defaultTitleReturn;
        }
        if(subtitle == null) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid season change subtitle.");
            return defaultTitleReturn;
        }
        if(fadein == 0 || fadein > 60) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid season change title fadein.");
            fadein = 1;
        }
        if(stay == 0 || stay > 60) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid season change title stay.");
            stay = 4;
        }
        if(fadeout == 0 || fadeout > 60) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid season change title fadeout.");
            fadeout = 1;
        }
        title = title.replace("%prefix%", getPrefixRaw())
                .replace("%seasonBefore%", configManager.getCurrentSeasonText(seasonBefore))
                .replace("%newSeason%", configManager.getCurrentSeasonText(newSeason));
        subtitle = subtitle.replace("%prefix%", getPrefixRaw())
                .replace("%seasonBefore%", configManager.getCurrentSeasonText(seasonBefore))
                .replace("%newSeason%", configManager.getCurrentSeasonText(newSeason));

        try {
            mm.deserialize(title);
        } catch (Exception e) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid season change title format.\nError: " + e.getMessage());
            return defaultTitleReturn;
        }
        try {
            mm.deserialize(subtitle);
        } catch (Exception e) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid season change subtitle format.\nError: " + e.getMessage());
            return defaultTitleReturn;
        }

        return Title.title(mm.deserialize(title),
                mm.deserialize(subtitle),
                Title.Times.times(
                        Duration.ofSeconds(fadein),
                        Duration.ofSeconds(stay),
                        Duration.ofSeconds(fadeout)));
    }

}
