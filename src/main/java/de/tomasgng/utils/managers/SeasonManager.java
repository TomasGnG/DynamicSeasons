package de.tomasgng.utils.managers;

import de.tomasgng.DynamicSeasons;
import de.tomasgng.utils.enums.SeasonType;
import de.tomasgng.utils.template.Season;
import lombok.Getter;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.bukkit.Bukkit;

import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

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

            Season season = new Season(
                    config.getAllowedWorlds(),
                    config.getWeather(seasonName),
                    config.getWeatherTypes(seasonName),
                    config.randomTickSpeed(seasonName),
                    config.isFeatureEnabled(seasonName, "animalSpawning"),
                    config.getAnimalSpawning(seasonName),
                    config.isFeatureEnabled(seasonName, "mobMovement"),
                    config.getMobMovement(seasonName),
                    config.isFeatureEnabled(seasonName, "animalGrowing"),
                    config.getAnimalGrowing(seasonName),
                    config.isFeatureEnabled(seasonName, "mobBonusArmor"),
                    config.getMobBonusArmor(seasonName),
                    config.isFeatureEnabled(seasonName, "mobMaxHealth"),
                    config.getMobMaxHealth(seasonName),
                    config.isFeatureEnabled(seasonName, "mobAttackDamage"),
                    config.getMobAttackDamage(seasonName),
                    config.isFeatureEnabled(seasonName, "preventCropGrowing"),
                    config.getPreventCropGrowing(seasonName),
                    config.isFeatureEnabled(seasonName, "potionEffects"),
                    config.getPotionEffects(seasonName),
                    config.isFeatureEnabled(seasonName, "lootDrops"),
                    config.getLootDrops(seasonName),
                    config.getBossList(seasonName),
                    config.getXPBonus(seasonName),
                    config.getParticles(seasonName));

            seasons.put(seasonType, season);
        }

        currentSeason = config.getCurrentSeasonTypeFromDatabase();
        currentSeasonInstance = seasons.get(currentSeason);
        currentSeasonInstance.start();
    }

    public void reload() {
        currentSeasonInstance.stop();
        initialize();
    }

    private void startSeasonTimer() {
        Bukkit.getScheduler().runTaskTimer(DynamicSeasons.getInstance(), task -> {
            config.decreaseRemainingTime();
            remainingTime = config.getRemainingTimeFromDatabase();

            if(remainingTime <= 0) {
                changeCurrentSeason();
                config.resetRemainingTime();
                currentSeasonInstance.start();
            }
        }, 3*20L, 20L);
    }

    private void changeCurrentSeason() {
        var messageManager = DynamicSeasons.getInstance().getMessageManager();
        List<SeasonType> seasonTypes = Arrays.asList(SeasonType.values());
        var nextInt = seasonTypes.indexOf(currentSeason)+1;

        if(nextInt > 3)
            nextInt = 0;

        if(messageManager.isSeasonChangeBroadcastEnabled())
            Bukkit.broadcast(messageManager.getSeasonChangeBroadcastComponent(getCurrentSeason(), seasonTypes.get(nextInt)));

        if(messageManager.isSeasonChangeTitleEnabled())
            Bukkit.getServer().showTitle(messageManager.getSeasonChangeTitleComponent(getCurrentSeason(), seasonTypes.get(nextInt)));

        currentSeasonInstance.stop();
        config.updateCurrentSeason(seasonTypes.get(nextInt));
        currentSeason = config.getCurrentSeasonTypeFromDatabase();
        currentSeasonInstance = seasons.get(currentSeason);
        currentSeasonInstance.start();
    }

    public void changeCurrentSeason(SeasonType seasonType) {
        var messageManager = DynamicSeasons.getInstance().getMessageManager();

        if(messageManager.isSeasonChangeBroadcastEnabled())
            Bukkit.broadcast(messageManager.getSeasonChangeBroadcastComponent(getCurrentSeason(), seasonType));

        if(messageManager.isSeasonChangeTitleEnabled())
            Bukkit.getServer().showTitle(messageManager.getSeasonChangeTitleComponent(getCurrentSeason(), seasonType));

        currentSeasonInstance.stop();
        config.resetRemainingTime();
        config.updateCurrentSeason(seasonType);
        currentSeason = config.getCurrentSeasonTypeFromDatabase();
        currentSeasonInstance = seasons.get(currentSeason);
        currentSeasonInstance.start();
    }

    public String getFormattedDuration() {
        return DurationFormatUtils.formatDuration(config.getRemainingTimeFromDatabase()*1000L, config.getDurationPlacerholderRawFormat());
    }

    public Season getSeasonInstanceByType(SeasonType seasonType) {
        return seasons.get(seasonType);
    }
}
