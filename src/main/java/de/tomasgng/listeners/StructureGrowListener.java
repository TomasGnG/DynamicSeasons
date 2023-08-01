package de.tomasgng.listeners;

import de.tomasgng.DynamicSeasons;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.StructureGrowEvent;

public class StructureGrowListener implements Listener {

    @EventHandler
    public void on(StructureGrowEvent event) {
        event.setCancelled(DynamicSeasons.getInstance().getSeasonManager().getCurrentSeasonInstance().handleCropGrowing(event.getLocation().getBlock().getType(), event.getWorld()));
    }

}
