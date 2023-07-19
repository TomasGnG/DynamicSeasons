package de.tomasgng.utils;

import de.tomasgng.DynamicSeasons;
import de.tomasgng.utils.enums.SeasonType;
import lombok.Getter;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.bukkit.Bukkit;

import java.util.*;

public class SeasonManager {

    private final ConfigManager config = DynamicSeasons.getInstance().getConfigManager();
    private final int duration = config.getDuration();
    private final TreeMap<SeasonType, Season> seasons = new TreeMap<>();
    @Getter
    private SeasonType currentSeason;
    @Getter
    private Season currentSeasonInstance;
    @Getter
    private int remainingTime = duration;

    public SeasonManager() {
        initialize();
        startSeasonTimer();
    }

    private void initialize() {
        for(var seasonName : List.of("spring", "summer", "fall", "winter")) {
            SeasonType seasonType = SeasonType.valueOf(seasonName.toUpperCase());
            Season season = new Season(seasonType,
                    config.getAllowedWorlds(),
                    config.getWeather(seasonName),
                    config.getWeatherTypes(seasonName),
                    config.randomTickSpeed(seasonName),
                    config.getAnimalSpawning(seasonName),
                    config.getMobMovement(seasonName),
                    config.getAnimalGrowing(seasonName),
                    config.getXPBonus(seasonName));

            seasons.put(seasonType, season);
        }
        currentSeason = SeasonType.SPRING;
        currentSeasonInstance = seasons.get(currentSeason);
        currentSeasonInstance.start();
    }

    private void startSeasonTimer() {
        Bukkit.getScheduler().runTaskTimer(DynamicSeasons.getInstance(), task -> {
            remainingTime--;
            if(remainingTime <= 0) {
                changeCurrentSeason();
                resetRemainingTime();
                currentSeasonInstance.start();
            }
        }, 3*20L, 20L);
    }

    private void changeCurrentSeason() {
        List<SeasonType> seasonTypes = Arrays.asList(SeasonType.values());
        var nextInt = seasonTypes.indexOf(currentSeason)+1;
        if(nextInt > 3)
            nextInt = 0;
        currentSeason = seasonTypes.get(nextInt);
        currentSeasonInstance = seasons.get(currentSeason);
    }

    public void changeCurrentSeason(SeasonType seasonType) {
        resetRemainingTime();
        currentSeason = seasonType;
        currentSeasonInstance = seasons.get(currentSeason);
    }

    private void resetRemainingTime() {
        remainingTime = duration;
    }

    public String getFormattedDuration() {
        return DurationFormatUtils.formatDuration(remainingTime* 1000L, config.getDurationPlacerholderRawFormat());
    }

}
