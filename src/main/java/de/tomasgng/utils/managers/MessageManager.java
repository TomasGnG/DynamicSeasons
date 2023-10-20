package de.tomasgng.utils.managers;

import de.tomasgng.DynamicSeasons;
import de.tomasgng.utils.enums.SeasonType;
import lombok.SneakyThrows;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.annotation.Nullable;
import java.io.File;
import java.time.Duration;
import java.util.List;

public class MessageManager {

    private final File folder = new File("plugins/DynamicSeasons");
    private final File file = new File("plugins/DynamicSeasons/messages.yml");
    private YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final ConfigManager configManager = DynamicSeasons.getInstance().getConfigManager();

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
            cfg.set("command.usage",
                    List.of("%prefix% <gray>Usage: <yellow>/dynseasons setseason <spring|summer|fall|winter>",
                            "%prefix% <gray>Usage: <yellow>/dynseasons setremainingtime <seconds>",
                            "%prefix% <gray>Usage: <yellow>/dynseasons spawnboss <spring|summer|fall|winter> <bosstype>",
                            "%prefix% <gray>Usage: <yellow>/dynseasons reload"));
            cfg.set("command.seasonAlreadyActive", "%prefix% <gray>This season is already active.");
            cfg.set("command.seasonChanged", "%prefix% <gray>You successfully changed the season from <yellow>%seasonBefore% <gray>to <green>%newSeason%<gray>.");
            cfg.set("command.invalidNumberFormat", "%prefix% <gray>Invalid number!");
            cfg.set("command.remainingTimeSet", "%prefix% <gray>You set the remaining time to <green>%remainingTime% seconds<gray>!");
            cfg.set("command.reload", "%prefix% <green>The plugin message.yml and config.yml were reloaded!");
            cfg.set("command.bossSpawned", "%prefix% <green>You spawned a boss!");
            cfg.set("command.noBossFound", "%prefix% <red>No boss found!");
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

    public void reload() {
        cfg = YamlConfiguration.loadConfiguration(file);
    }

    private String replaceAllPlaceholders(String text,
                                          @Nullable SeasonType seasonBefore,
                                          @Nullable SeasonType newSeason,
                                          int newRemainingTime) {
        text = text.replace("%prefix%", getPrefixRaw());

        if(seasonBefore != null)
            text = text.replace("%seasonBefore%", configManager.getCurrentSeasonText(seasonBefore));

        if(newSeason != null)
            text = text.replace("%newSeason%", configManager.getCurrentSeasonText(newSeason));

        if(newRemainingTime != -1)
            text = text.replace("%remainingTime%", String.valueOf(newRemainingTime));

        return text;
    }

    private String getPrefixRaw() {
        var prefix = cfg.getString("prefix");

        if(prefix == null) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid prefix.");
            return "<gradient:#55C156:#FFFF00:#FFA500:#87CEFA>DynamicSeasons</gradient> <dark_gray>|";
        }

        try {
            mm.deserialize(prefix);
        } catch (Exception e) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid prefix format.");
        }

        return prefix;
    }

    public Component getCMDNoPermissionComponent() {
        var msg = cfg.getString("command.noPermission");
        var defaultReturn = mm.deserialize(replaceAllPlaceholders("%prefix% <gray>You have no permission to use this command.", null, null, -1));

        if(msg == null) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid no permission message.");
            return defaultReturn;
        }

        msg = replaceAllPlaceholders(msg, null, null, -1);

        try {
            mm.deserialize(msg);
        } catch (Exception e) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid no permission format.\nError: " + e.getMessage());
            return defaultReturn;
        }

        return mm.deserialize(msg);
    }

    public Component getCMDUsageComponent() {
        var msgList = cfg.getStringList("command.usage");
        var defaultCMDUsage = mm.deserialize(replaceAllPlaceholders("%prefix% <gray>Usage: <yellow>/dynseasons setseason <spring|summer|fall|winter>" +
                "<br>%prefix% <gray>Usage: <yellow>/dynseasons setremainingtime <seconds>" +
                "<br>%prefix% <gray>Usage: <yellow>/dynseasons spawnboss <spring|summer|fall|winter> <bosstype>" +
                "<br>%prefix% <gray>Usage: <yellow>/dynseasons reload", null, null, -1));

        if(msgList.isEmpty()) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid usage message. Creating entry in message.yml...");

            cfg.set("command.usage",
                    List.of("%prefix% <gray>Usage: <yellow>/dynseasons setseason <spring|summer|fall|winter>",
                            "%prefix% <gray>Usage: <yellow>/dynseasons setremainingtime <seconds>",
                            "%prefix% <gray>Usage: <yellow>/dynseasons spawnboss <spring|summer|fall|winter> <bosstype>",
                            "%prefix% <gray>Usage: <yellow>/dynseasons reload"));

            save();
            return defaultCMDUsage;
        }

        Component msg = Component.text("");

        for (String s : msgList) {
            try {
                if(msgList.indexOf(s) == msgList.size()-1) {
                    msg = msg.append(mm.deserialize(replaceAllPlaceholders(s, null, null, -1)));
                    continue;
                }

                msg = msg.append(mm.deserialize(replaceAllPlaceholders(s, null, null, -1))).appendNewline();
            } catch (Exception e) {
                DynamicSeasons.getInstance().getLogger().severe("Invalid usage format.\nMessage: " + s + "\nError: " + e.getMessage());
                return defaultCMDUsage;
            }
        }

        return msg;
    }

    public Component getCMDSeasonAlreadyActiveComponent() {
        var msg = cfg.getString("command.seasonAlreadyActive");
        var defaultReturn = mm.deserialize(replaceAllPlaceholders("%prefix% <gray>This season is already active.", null, null, -1));

        if(msg == null) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid season already active message.");
            return defaultReturn;
        }

        msg = replaceAllPlaceholders(msg, null, null, -1);

        try {
            mm.deserialize(msg);
        } catch (Exception e) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid season already active format.\nError: " + e.getMessage());
            return defaultReturn;
        }

        return mm.deserialize(msg);
    }

    public Component getCMDSeasonChangedComponent(SeasonType seasonBefore, SeasonType newSeason) {
        var msg = cfg.getString("command.seasonChanged");
        var defaultReturn = mm.deserialize(replaceAllPlaceholders("%prefix% <gray>You successfully changed the season from <yellow>%seasonBefore% <gray>to <green>%newSeason%<gray>.", seasonBefore, newSeason, -1));

        if(msg == null) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid season changed message.");
            return defaultReturn;
        }

        msg = replaceAllPlaceholders(msg, seasonBefore, newSeason, -1);

        try {
            mm.deserialize(msg);
        } catch (Exception e) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid season changed format.\nError: " + e.getMessage());
            return defaultReturn;
        }

        return mm.deserialize(msg);
    }

    public Component getSeasonChangeBroadcastComponent(SeasonType seasonBefore, SeasonType newSeason) {
        var msg = cfg.getString("season_change.broadcast.text");
        var defaultReturn = mm.deserialize(replaceAllPlaceholders("%prefix% <gray>The season was changed from <yellow>%seasonBefore% <gray>to <green>%newSeason%<gray>.", seasonBefore, newSeason, -1));

        if(msg == null) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid season change broadcast message.");
            return defaultReturn;
        }

        msg = replaceAllPlaceholders(msg, seasonBefore, newSeason, -1);

        try {
            mm.deserialize(msg);
        } catch (Exception e) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid season change broadcast format.\nError: " + e.getMessage());
            return defaultReturn;
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
        var title = cfg.getString("season_change.title.title");
        var subtitle = cfg.getString("season_change.title.subtitle");
        var fadein = cfg.getInt("season_change.title.times.fadein");
        var stay = cfg.getInt("season_change.title.times.stay");
        var fadeout = cfg.getInt("season_change.title.times.fadeout");
        var defaultTitleReturn = Title.title(mm.deserialize(replaceAllPlaceholders("<yellow>Season Change",null,null,-1)),
                mm.deserialize(replaceAllPlaceholders("<italic><dark_gray>-> <green>%newSeason%", seasonBefore, newSeason, -1)),
                Title.DEFAULT_TIMES);

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

        title = replaceAllPlaceholders(title, seasonBefore, newSeason, -1);
        subtitle = replaceAllPlaceholders(subtitle, seasonBefore, newSeason, -1);

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
    
    public Component getCMDInvalidNumberFormatComponent() {
        var msg = cfg.getString("command.invalidNumberFormat");
        var defaultReturn = mm.deserialize(replaceAllPlaceholders("%prefix% <gray>Invalid number!", null, null, -1));

        if(msg == null) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid invalidNumberFormat message.");
            return defaultReturn;
        }

        msg = replaceAllPlaceholders(msg, null, null, -1);

        try {
            mm.deserialize(msg);
        } catch (Exception e) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid invalidNumberFormat format.\nError: " + e.getMessage());
            return defaultReturn;
        }

        return mm.deserialize(msg);
    }
    
    public Component getCMDRemainingTimeSetComponent(int newRemainingTime) {
        var msg = cfg.getString("command.remainingTimeSet");
        var defaultReturn = mm.deserialize(replaceAllPlaceholders("%prefix% <gray>You set the remaining time to <green>%remainingTime% seconds<gray>!", null, null, newRemainingTime));

        if(msg == null) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid remainingTimeSet message.");
            return defaultReturn;
        }

        msg = replaceAllPlaceholders(msg, null, null, newRemainingTime);

        try {
            mm.deserialize(msg);
        } catch (Exception e) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid remainingTimeSet format.\nError: " + e.getMessage());
            return defaultReturn;
        }

        return mm.deserialize(msg);
    }

    public Component getCMDReloadComponent() {
        var msg = cfg.getString("command.reload");
        var defaultReturn = mm.deserialize(replaceAllPlaceholders("%prefix% <green>The plugin message.yml and config.yml were reloaded!", null, null, -1));

        if(msg == null) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid reload message.");
            return defaultReturn;
        }

        msg = replaceAllPlaceholders(msg, null, null, -1);

        try {
            mm.deserialize(msg);
        } catch (Exception e) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid reload format.\nError: " + e.getMessage());
            return defaultReturn;
        }

        return mm.deserialize(msg);
    }

    public Component getCMDBossSpawnedComponent() {
        var msg = cfg.getString("command.bossSpawned");
        var defaultReturn = mm.deserialize(replaceAllPlaceholders("%prefix% <green>You spawned a boss!", null, null, -1));

        if(msg == null) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid bossSpawned message. Creating entry in message.yml...");
            cfg.set("command.bossSpawned", "%prefix% <green>You spawned a boss!");
            save();
            return defaultReturn;
        }

        msg = replaceAllPlaceholders(msg, null, null, -1);

        try {
            mm.deserialize(msg);
        } catch (Exception e) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid bossSpawned format.\nError: " + e.getMessage());
            return defaultReturn;
        }

        return mm.deserialize(msg);
    }

    public Component getCMDNoBossFoundComponent() {
        var msg = cfg.getString("command.noBossFound");
        var defaultReturn = mm.deserialize(replaceAllPlaceholders("%prefix% <red>No boss found!", null, null, -1));

        if(msg == null) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid noBossFound message. Creating entry in message.yml...");
            cfg.set("command.noBossFound", "%prefix% <red>No boss found!");
            save();
            return defaultReturn;
        }

        msg = replaceAllPlaceholders(msg, null, null, -1);

        try {
            mm.deserialize(msg);
        } catch (Exception e) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid noBossFound format.\nError: " + e.getMessage());
            return defaultReturn;
        }

        return mm.deserialize(msg);
    }
}
