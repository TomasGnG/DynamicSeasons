package de.tomasgng.listeners;

import de.tomasgng.DynamicSeasons;
import de.tomasgng.utils.enums.WeatherType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.WeatherChangeEvent;

public class WeatherChangeListener implements Listener {

    @EventHandler
    public void on(WeatherChangeEvent event) {
        if(event.toWeatherState())
            event.setCancelled(DynamicSeasons.getInstance().getSeasonManager().getCurrentSeasonInstance().handleWeatherUpdate(event.getWorld(), WeatherType.STORM));
        else
            event.setCancelled(DynamicSeasons.getInstance().getSeasonManager().getCurrentSeasonInstance().handleWeatherUpdate(event.getWorld(), WeatherType.CLEAR));
    }

}
