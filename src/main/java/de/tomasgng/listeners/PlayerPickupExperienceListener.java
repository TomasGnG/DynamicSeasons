package de.tomasgng.listeners;

import com.destroystokyo.paper.event.player.PlayerPickupExperienceEvent;
import de.tomasgng.DynamicSeasons;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class PlayerPickupExperienceListener implements Listener {

    @EventHandler
    public void on(PlayerPickupExperienceEvent event) {
        var xpOrb = event.getExperienceOrb();
        var xp = xpOrb.getExperience();
        xpOrb.setExperience(DynamicSeasons.getInstance().getSeasonManager().getCurrentSeasonInstance().handleXPBonus(xp));
    }

}
