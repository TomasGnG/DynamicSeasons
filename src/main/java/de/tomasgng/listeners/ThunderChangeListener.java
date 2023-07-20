package de.tomasgng.listeners;

import de.tomasgng.DynamicSeasons;
import de.tomasgng.utils.enums.WeatherType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.ThunderChangeEvent;

public class ThunderChangeListener implements Listener {

    @EventHandler
    public void on(ThunderChangeEvent event) {
        if(event.toThunderState())
            event.setCancelled(DynamicSeasons.getInstance().getSeasonManager().getCurrentSeasonInstance().handleWeatherUpdate(event.getWorld(), WeatherType.THUNDER));
        else
            event.setCancelled(DynamicSeasons.getInstance().getSeasonManager().getCurrentSeasonInstance().handleWeatherUpdate(event.getWorld(), WeatherType.CLEAR));
    }

}
