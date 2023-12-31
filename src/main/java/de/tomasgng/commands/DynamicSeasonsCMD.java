package de.tomasgng.commands;

import de.tomasgng.DynamicSeasons;
import de.tomasgng.utils.enums.SeasonType;
import de.tomasgng.utils.managers.ConfigManager;
import de.tomasgng.utils.managers.MessageManager;
import de.tomasgng.utils.managers.SeasonManager;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class DynamicSeasonsCMD extends Command {

    private final SeasonManager seasonManager = DynamicSeasons.getInstance().getSeasonManager();
    private final MessageManager messageManager = DynamicSeasons.getInstance().getMessageManager();
    private final ConfigManager configManager = DynamicSeasons.getInstance().getConfigManager();

    public DynamicSeasonsCMD() {
        super(DynamicSeasons.getInstance().getConfigManager().getCMDName(),
                DynamicSeasons.getInstance().getConfigManager().getCMDDescription(),
                "",
                DynamicSeasons.getInstance().getConfigManager().getCMDAliases());
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        Player player = (Player) sender;

        if(!player.hasPermission(configManager.getCMDPermission())) {
            player.sendMessage(messageManager.getCMDNoPermissionComponent());
            return false;
        }

        if(DynamicSeasons.getInstance().isPLUGIN_DISABLED())
            player.sendMessage(DynamicSeasons.getInstance().getDISABLED_MESSAGE());

        if(args.length == 0) {
            player.sendMessage(messageManager.getCMDUsageComponent());
            return false;
        }

        if(args.length == 1 && args[0].equalsIgnoreCase("update")) {
            DynamicSeasons.getInstance().updatePlugin(player);
            return false;
        }

        if(!args[0].equalsIgnoreCase("setseason")
                && !args[0].equalsIgnoreCase("setremainingtime")
                && !args[0].equalsIgnoreCase("reload")
                && !args[0].equalsIgnoreCase("spawnboss")) {
            player.sendMessage(messageManager.getCMDUsageComponent());
            return false;
        }

        if(args.length == 3 && args[0].equalsIgnoreCase("spawnboss")) {
            if(!List.of("spring", "summer", "fall", "winter").contains(args[1].toLowerCase())) {
                player.sendMessage(messageManager.getCMDUsageComponent());
                return false;
            }

            EntityType mobType;

            try {
                mobType = EntityType.valueOf(args[2]);
            } catch (Exception e) {
                player.sendMessage(messageManager.getCMDNoBossFoundComponent());
                return false;
            }

            var bossList = configManager.getBossList(args[1]);

            for(var boss : bossList) {
                if(boss.getEntityType().equals(mobType)) {
                    seasonManager.getSeasonInstanceByType(SeasonType.valueOf(args[1].toUpperCase()))
                            .handleBossSpawning((LivingEntity) player.getWorld().spawnEntity(player.getLocation(), mobType), true);
                    player.sendMessage(messageManager.getCMDBossSpawnedComponent());
                    return false;
                }
            }

            player.sendMessage(messageManager.getCMDNoBossFoundComponent());
            return false;
        }

        if(args.length == 2 && args[0].equalsIgnoreCase("setseason")) {
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
            return false;
        }

        if(args.length == 2 && args[0].equalsIgnoreCase("setremainingtime")) {
            int newRemainingTime = 0;

            try {
                newRemainingTime = Integer.parseInt(args[1]);
            } catch (Exception e) {
                player.sendMessage(messageManager.getCMDInvalidNumberFormatComponent());
                return false;
            }

            configManager.setRemainingTime(newRemainingTime);
            player.sendMessage(messageManager.getCMDRemainingTimeSetComponent(newRemainingTime));
            return false;
        }

        if(args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            configManager.reload();
            seasonManager.reload();
            messageManager.reload();
            player.sendMessage(messageManager.getCMDReloadComponent());
            return false;
        }

        player.sendMessage(messageManager.getCMDUsageComponent());
        return false;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender commandSender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        if(!commandSender.hasPermission(configManager.getCMDPermission()))
            return List.of();

        if(args.length == 1)
            return List.of("setseason", "setremainingtime", "reload", "update", "spawnboss");

        if(args.length == 2) {
            if(args[0].equalsIgnoreCase("setseason") || args[0].equalsIgnoreCase("spawnboss"))
                return List.of("spring", "summer", "fall", "winter");

            if(args[0].equalsIgnoreCase("setremainingtime")) {
                commandSender.sendActionBar(Component.text("Current remaining time in seconds: " + seasonManager.getRemainingTime()));
            }
        }

        if(args.length == 3) {
            if(args[0].equalsIgnoreCase("spawnboss") && List.of("spring", "summer", "fall", "winter").contains(args[1])) {
                List<String> bosses = new ArrayList<>();
                var bossList = configManager.getBossList(args[1]);

                for(var boss : bossList) {
                    bosses.add(boss.getEntityType().name());
                }

                return bosses;
            }
        }
        return List.of();
    }
}
