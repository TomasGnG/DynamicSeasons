package de.tomasgng.listeners;

import de.tomasgng.DynamicSeasons;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final MiniMessage mm = MiniMessage.miniMessage();

    @EventHandler
    public void on(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if(!event.getPlayer().isOp())
            return;

        if(DynamicSeasons.getInstance().updateCheck(true) != null) {
            player.sendMessage(mm.deserialize("<gradient:#55C156:#FFFF00:#FFA500:#87CEFA>DynamicSeasons</gradient> <dark_gray>| <yellow>Using an outdated version(" + DynamicSeasons.getInstance().getDescription().getVersion() + "). Newest version " + DynamicSeasons.getInstance().updateCheck(true)));
            player.sendMessage(mm.deserialize("<gradient:#55C156:#FFFF00:#FFA500:#87CEFA>DynamicSeasons</gradient> <dark_gray>| <yellow><hover:show_text:'<green>Clicking this message will update the plugin.'><click:run_command:/dynamicseasons update>Click here to update the plugin</click>."));
        }

        if(DynamicSeasons.getInstance().isPLUGIN_DISABLED())
            player.sendMessage(DynamicSeasons.getInstance().getDISABLED_MESSAGE());
    }

}
