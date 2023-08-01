package de.tomasgng.listeners;

import de.tomasgng.DynamicSeasons;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockSpreadEvent;

public class BlockSpreadListener implements Listener {

    @EventHandler
    public void on(BlockSpreadEvent event) {
        event.setCancelled(DynamicSeasons.getInstance().getSeasonManager().getCurrentSeasonInstance().handleCropGrowing(event.getNewState().getType(), event.getNewState().getWorld()));
    }

}
