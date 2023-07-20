package de.tomasgng.utils;

import de.tomasgng.utils.enums.SeasonType;
import de.tomasgng.utils.enums.WeatherType;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class Season {
    private final List<World> worlds;
    private final boolean weather;
    private final List<WeatherType> weatherTypes;
    private final int randomTickSpeed;
    private final Map<EntityType, Integer> animalSpawning;
    private final Map<EntityType, Double> mobMovement;
    private final Map<EntityType, Integer> animalGrowing;
    private final int xpBonus;

    public Season(List<World> worlds,
                  boolean weather,
                  List<WeatherType> weatherTypes,
                  int randomTickSpeed,
                  Map<EntityType, Integer> animalSpawning,
                  Map<EntityType, Double> mobMovement,
                  Map<EntityType, Integer> animalGrowing,
                  int xpBonus) {
        this.worlds = worlds;
        this.weather = weather;
        this.weatherTypes = weatherTypes;
        this.randomTickSpeed = randomTickSpeed;
        this.animalSpawning = animalSpawning;
        this.mobMovement = mobMovement;
        this.animalGrowing = animalGrowing;
        this.xpBonus = xpBonus;
    }

    private boolean isValidWorld(World world) {
        return worlds.contains(world);
    }

    public void start() {
        for(var world : worlds) {
            world.setGameRule(GameRule.RANDOM_TICK_SPEED, randomTickSpeed);
        }
    }

    public boolean handleWeatherUpdate(World world, WeatherType weatherTo) {
        if(!weather)
            return false;
        if(!isValidWorld(world))
            return false;
        return !weatherTypes.contains(weatherTo);
    }

    public boolean handleAnimalSpawning(LivingEntity entity, CreatureSpawnEvent.SpawnReason spawnReason) {
        if(!isValidWorld(entity.getWorld()))
            return false;
        if(spawnReason.equals(CreatureSpawnEvent.SpawnReason.EGG))
            return false;
        if(!animalSpawning.containsKey(entity.getType())) {
            handleMobMovement(entity);
            handleAnimalGrowing(entity);
            return false;
        }
        int chanceToSpawn = animalSpawning.get(entity.getType());
        int randomChance = new Random().nextInt(0, 101);
        if(randomChance > chanceToSpawn)
            return true;
        handleAnimalGrowing(entity);
        handleMobMovement(entity);
        return false;
    }

    private void handleAnimalGrowing(LivingEntity entity) {
        if(!isValidWorld(entity.getWorld()))
            return;
        if(!animalGrowing.containsKey(entity.getType()))
            return;
        if(!(entity instanceof Ageable ageable))
            return;
        if(!ageable.isAdult())
            ageable.setAge(-animalGrowing.get(entity.getType()));
    }

    private void handleMobMovement(LivingEntity entity) {
        if(!isValidWorld(entity.getWorld()))
            return;
        if(!mobMovement.containsKey(entity.getType()))
            return;
        var movementSpeed = mobMovement.get(entity.getType());
        entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(movementSpeed);
    }

    public int handleXPBonus(int xp) {
        return (int) Math.round(xp*(1 + (double) xpBonus/100));
    }

}
