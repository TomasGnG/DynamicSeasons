package de.tomasgng.commands;

import de.tomasgng.DynamicSeasons;
import de.tomasgng.utils.enums.SeasonType;
import de.tomasgng.utils.managers.ConfigManager;
import de.tomasgng.utils.managers.MessageManager;
import de.tomasgng.utils.managers.SeasonManager;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DynamicSeasonsCMD implements CommandExecutor, TabExecutor {

    SeasonManager seasonManager = DynamicSeasons.getInstance().getSeasonManager();
    MessageManager messageManager = DynamicSeasons.getInstance().getMessageManager();
    ConfigManager configManager = DynamicSeasons.getInstance().getConfigManager();

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String string, @NotNull String[] args) {
        Player player = (Player) commandSender;

        if(!player.hasPermission("dynamicseasons.command.use")) {
            player.sendMessage(messageManager.getCMDNoPermissionComponent());
            return false;
        }
        if(args.length == 0) {
            player.sendMessage(messageManager.getCMDUsageComponent());
            return false;
        }
        if(args.length > 2) {
            player.sendMessage(messageManager.getCMDUsageComponent());
            return false;
        }
        if(!args[0].equalsIgnoreCase("setseason")
                && !args[0].equalsIgnoreCase("setremainingtime")
                && !args[0].equalsIgnoreCase("reload")) {
            player.sendMessage(messageManager.getCMDUsageComponent());
            return false;
        }
        if(args[0].equalsIgnoreCase("setseason")) {
            if(!List.of("spring", "summer", "fall", "winter").contains(args[1].toLowerCase())) {
                player.sendMessage(messageManager.getCMDUsageComponent());
                return false;
            }
            SeasonType season = SeasonType.valueOf(args[1].toUpperCase());
            if(seasonManager.getCurrentSeason() == season) {
                player.sendMessage(messageManager.getCMDSeasonAlreadyActiveComponent());
                return false;
            }
            player.sendMessage(messageManager.getCMDSeasonChangedComponent(seasonManager.getCurrentSeason(), season));
            seasonManager.changeCurrentSeason(season);
        }
        if(args[0].equalsIgnoreCase("setremainingtime")) {
            try {
                Integer.parseInt(args[1]);
            } catch (Exception e) {
                player.sendMessage(messageManager.getCMDInvalidNumberFormatComponent());
                return false;
            }
            int newRemainingTime = Integer.parseInt(args[1]);
            configManager.setRemainingTime(newRemainingTime);
            player.sendMessage(messageManager.getCMDRemainingTimeSetComponent(newRemainingTime));
        }
        if(args[0].equalsIgnoreCase("reload")) {
            configManager.reload();
            seasonManager.reload();
            messageManager.reload();
            player.sendMessage(messageManager.getCMDReloadComponent());
        }
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if(command.getName().equalsIgnoreCase("dynamicseasons") ||
                command.getName().equalsIgnoreCase("dynseasons") ||
                command.getName().equalsIgnoreCase("dseasons")) {
            if(!commandSender.hasPermission("dynamicseasons.command.use"))
                return null;
            if(args.length == 1)
                return List.of("setseason", "setremainingtime", "reload");
            if(args.length == 2) {
                if(args[0].equalsIgnoreCase("setseason"))
                    return List.of("spring", "summer", "fall", "winter");
                if(args[0].equalsIgnoreCase("setremainingtime")) {
                    commandSender.sendActionBar(Component.text("Current remaining time in seconds: " + seasonManager.getRemainingTime()));
                }
            }
            return null;
        }
        return null;
    }
}
