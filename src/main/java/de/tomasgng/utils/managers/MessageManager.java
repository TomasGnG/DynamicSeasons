package de.tomasgng.utils;

import de.tomasgng.DynamicSeasons;
import de.tomasgng.utils.enums.SeasonType;
import lombok.SneakyThrows;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
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

}
