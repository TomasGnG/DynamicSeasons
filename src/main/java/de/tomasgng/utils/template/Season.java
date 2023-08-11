package de.tomasgng.utils.template;

import de.tomasgng.DynamicSeasons;
import de.tomasgng.utils.enums.WeatherType;
import de.tomasgng.utils.managers.LootDrop;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Season {
    private final List<World> worlds;
    private final boolean weather;
    private final List<WeatherType> weatherTypes;
    private final int randomTickSpeed;
    private final Map<EntityType, Double> animalSpawning;
    private final Map<EntityType, Double> mobMovement;
    private final Map<EntityType, Integer> animalGrowing;
    private final Map<EntityType, Double> mobBonusArmor;
    private final Map<EntityType, Double> mobMaxHealth;
    private final Map<EntityType, Double> mobAttackDamage;
    private final List<Material> preventCropGrowing;
    private final List<PotionEffect> potionEffects;
    private final List<LootDrop> lootDrops;
    private final List<BossEntity> bossList;
    private final int xpBonus;
    private final Random rnd = new Random();
    private final List<BossEntity> activeBosses = new ArrayList<>();

    public Season(List<World> worlds,
                  boolean weather,
                  List<WeatherType> weatherTypes,
                  int randomTickSpeed,
                  Map<EntityType, Double> animalSpawning,
                  Map<EntityType, Double> mobMovement,
                  Map<EntityType, Integer> animalGrowing,
                  Map<EntityType, Double> mobBonusArmor,
                  Map<EntityType, Double> mobMaxHealth,
                  Map<EntityType, Double> mobAttackDamage,
                  List<Material> preventCropGrowing,
                  List<PotionEffect> potionEffects,
                  List<LootDrop> lootDrops,
                  List<BossEntity> bossList,
                  int xpBonus) {
        this.worlds = worlds;
        this.weather = weather;
        this.weatherTypes = weatherTypes;
        this.randomTickSpeed = randomTickSpeed;
        this.animalSpawning = animalSpawning;
        this.mobMovement = mobMovement;
        this.animalGrowing = animalGrowing;
        this.mobBonusArmor = mobBonusArmor;
        this.mobMaxHealth = mobMaxHealth;
        this.mobAttackDamage = mobAttackDamage;
        this.preventCropGrowing = preventCropGrowing;
        this.potionEffects = potionEffects;
        this.lootDrops = lootDrops;
        this.bossList = bossList;
        this.xpBonus = xpBonus;
    }

    private boolean isValidWorld(World world) {
        return worlds.contains(world);
    }

    public void start() {
        for(var world : worlds) {
            world.setGameRule(GameRule.RANDOM_TICK_SPEED, randomTickSpeed);
        }
        startPotionEffectTimer();
    }

    public void stop() {
        stopPotionEffectTimer();
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
            handleMobArmorBonus(entity);
            handleMobMaxHealth(entity);
            handleMobAttackDamage(entity);
            handleBossSpawning(entity);
            return false;
        }
        double chanceToSpawn = animalSpawning.get(entity.getType());
        double randomChance = rnd.nextDouble(0, 101);
        if(randomChance <= chanceToSpawn)
            return true;
        handleAnimalGrowing(entity);
        handleMobMovement(entity);
        handleMobArmorBonus(entity);
        handleMobMaxHealth(entity);
        handleMobAttackDamage(entity);
        handleBossSpawning(entity);
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

    private void handleMobArmorBonus(LivingEntity entity) {
        if(!isValidWorld(entity.getWorld()))
            return;
        if(!mobBonusArmor.containsKey(entity.getType()))
            return;
        var bonusArmor = mobBonusArmor.get(entity.getType());
        entity.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(bonusArmor);
    }

    private void handleMobMaxHealth(LivingEntity entity) {
        if(!isValidWorld(entity.getWorld()))
            return;
        if(!mobMaxHealth.containsKey(entity.getType()))
            return;
        var maxHealth = mobMaxHealth.get(entity.getType());
        entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
        entity.setHealth(entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
    }

    private void handleMobAttackDamage(LivingEntity entity) {
        if(!isValidWorld(entity.getWorld()))
            return;
        if(!mobAttackDamage.containsKey(entity.getType()))
            return;
        var attackDamage = mobAttackDamage.get(entity.getType());
        entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(attackDamage);
    }

    public boolean handleCropGrowing(Material material, World world) {
        if(!isValidWorld(world))
            return false;
        return preventCropGrowing.contains(material);
    }

    private boolean stopPotionEffectTimer = false;

    private void startPotionEffectTimer() {
        stopPotionEffectTimer = false;
        Bukkit.getScheduler().runTaskTimer(DynamicSeasons.getInstance(), bukkitTask -> {
            if(stopPotionEffectTimer)
                bukkitTask.cancel();
            List<Player> players = new ArrayList<>();
            for(var world : worlds) {
                players.addAll(world.getPlayers());
            }
            players.forEach(player -> {
                player.addPotionEffects(potionEffects);
            });
        }, 3*20L, 5 * 20L);
    }

    private void stopPotionEffectTimer() {
        stopPotionEffectTimer = true;
    }

    public int handleXPBonus(int xp) {
        return (int) Math.round(xp*(1 + (double) xpBonus/100));
    }

    public void handleLootDrops(LivingEntity entity) {
        if(!isValidWorld(entity.getWorld()))
            return;
        if(entity.getKiller() == null)
            return;
        LootDrop lootdrop = null;
        for(var loot : lootDrops) {
            if(loot.getEntity().equals(entity.getType()))
                lootdrop = loot;
        }
        if(lootdrop == null)
            return;

        boolean isBoss = false;
        for(var boss : activeBosses) {
            if (entity == boss.getEntity()) {
                isBoss = true;
                break;
            }
        }
        if(isBoss)
            return;

        var world = entity.getWorld();
        var loc = entity.getLocation();
        for(var entry : lootdrop.getItemStacks().entrySet()) {
            double randomChance = rnd.nextDouble(1, 101);
            if(randomChance <= entry.getValue())
                world.dropItemNaturally(loc, entry.getKey());
        }
    }

    public void handleBossSpawning(LivingEntity entity) {
        if(!isValidWorld(entity.getWorld()))
            return;
        var type = entity.getType();
        BossEntity boss = null;
        for(var currentBoss : bossList) {
            if(currentBoss.getEntityType() == type)
                boss = currentBoss.clone();
        }
        if(boss == null)
            return;
        boolean playerIsNearby = false;
        var nearbyEntites = entity.getNearbyEntities(25, 25, 25);
        for(var nearbyEntity : nearbyEntites) {
            if(nearbyEntity.getType() == EntityType.PLAYER)
                playerIsNearby = true;
        }
        if(!playerIsNearby)
            return;
        double randomChance = rnd.nextDouble(0, 101);
        double bossSpawnChance = boss.getSpawnChance();
        if(randomChance <= bossSpawnChance) {
            activeBosses.add(boss);
            boss.setEntity(entity);
            boss.handleSpawn();
        }
    }

    public void handleBossDeath(EntityDeathEvent event) {
        activeBosses.forEach(boss -> {
            boss.handleDeath(event);
        });
    }

    public void handleBossDamageEvent(EntityDamageByEntityEvent event) {
        activeBosses.forEach(boss -> {
            if(event.getDamager() == boss.getEntity())
                boss.handleDealDamage();
            if(event.getEntity() == boss.getEntity())
                boss.handleTakeDamage();
        });
    }
}
