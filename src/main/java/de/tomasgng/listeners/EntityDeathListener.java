package de.tomasgng.listeners;

import de.tomasgng.DynamicSeasons;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class EntityDeathListener implements Listener {

    @EventHandler
    public void on(EntityDeathEvent event) {
        DynamicSeasons.getInstance().getSeasonManager().getCurrentSeasonInstance().handleLootDrops(event.getEntity());
        DynamicSeasons.getInstance().getSeasonManager().getCurrentSeasonInstance().handleBossDeath(event);
    }

}
