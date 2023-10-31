package de.tomasgng.utils.template;

import de.tomasgng.DynamicSeasons;
import de.tomasgng.utils.enums.WeatherType;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
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
    private final boolean isAnimalSpawningEnabled;
    private final Map<EntityType, Double> animalSpawning;
    private final boolean isMobMovenemtEnabled;
    private final Map<EntityType, Double> mobMovement;
    private final boolean isAnimalGrowingEnabled;
    private final Map<EntityType, Integer> animalGrowing;
    private final boolean isMobBonusArmorEnabled;
    private final Map<EntityType, Double> mobBonusArmor;
    private final boolean isMobMaxHealthEnabled;
    private final Map<EntityType, Double> mobMaxHealth;
    private final boolean isMobAttackDamageEnabled;
    private final Map<EntityType, Double> mobAttackDamage;
    private final boolean isPreventCropGrowingEnabled;
    private final List<Material> preventCropGrowing;
    private final boolean isPotionEffectsEnabled;
    private final List<PotionEffect> potionEffects;
    private final boolean isLootDropsEnabled;
    private final List<LootDrop> lootDrops;
    private final List<BossEntity> bossList;
    private final int xpBonus;
    private final Particles particles;
    private final Random rnd = new Random();
    private final List<BossEntity> activeBosses = new ArrayList<>();

    public Season(List<World> worlds,
                  boolean weather,
                  List<WeatherType> weatherTypes,
                  int randomTickSpeed,
                  boolean isAnimalSpawningEnabled,
                  Map<EntityType, Double> animalSpawning,
                  boolean isMobMovenemtEnabled,
                  Map<EntityType, Double> mobMovement,
                  boolean isAnimalGrowingEnabled,
                  Map<EntityType, Integer> animalGrowing,
                  boolean isMobBonusArmorEnabled,
                  Map<EntityType, Double> mobBonusArmor,
                  boolean isMobMaxHealthEnabled,
                  Map<EntityType, Double> mobMaxHealth,
                  boolean isMobAttackDamageEnabled,
                  Map<EntityType, Double> mobAttackDamage,
                  boolean isPreventCropGrowingEnabled,
                  List<Material> preventCropGrowing,
                  boolean isPotionEffectsEnabled,
                  List<PotionEffect> potionEffects,
                  boolean isLootDropsEnabled,
                  List<LootDrop> lootDrops,
                  List<BossEntity> bossList,
                  int xpBonus,
                  Particles particles) {
        this.worlds = worlds;
        this.weather = weather;
        this.weatherTypes = weatherTypes;
        this.randomTickSpeed = randomTickSpeed;
        this.isAnimalSpawningEnabled = isAnimalSpawningEnabled;
        this.animalSpawning = animalSpawning;
        this.isMobMovenemtEnabled = isMobMovenemtEnabled;
        this.mobMovement = mobMovement;
        this.isAnimalGrowingEnabled = isAnimalGrowingEnabled;
        this.animalGrowing = animalGrowing;
        this.isMobBonusArmorEnabled = isMobBonusArmorEnabled;
        this.mobBonusArmor = mobBonusArmor;
        this.isMobMaxHealthEnabled = isMobMaxHealthEnabled;
        this.mobMaxHealth = mobMaxHealth;
        this.isMobAttackDamageEnabled = isMobAttackDamageEnabled;
        this.mobAttackDamage = mobAttackDamage;
        this.isPreventCropGrowingEnabled = isPreventCropGrowingEnabled;
        this.preventCropGrowing = preventCropGrowing;
        this.isPotionEffectsEnabled = isPotionEffectsEnabled;
        this.potionEffects = potionEffects;
        this.isLootDropsEnabled = isLootDropsEnabled;
        this.lootDrops = lootDrops;
        this.bossList = bossList;
        this.xpBonus = xpBonus;
        this.particles = particles;
    }

    private boolean isValidWorld(World world) {
        return worlds.contains(world);
    }

    public void start() {
        for(var world : worlds) {
            world.setGameRule(GameRule.RANDOM_TICK_SPEED, randomTickSpeed);
        }

        startPotionEffectTimer();

        if(particles != null) {
            particles.startParticleTimer();
        }
    }

    public void stop() {
        stopPotionEffectTimer();

        if(particles != null) {
            particles.stopParticleTimer();
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
            handleBossSpawning(entity, false);

            if(isBoss(entity))
                return false;

            handleMobMovement(entity);
            handleAnimalGrowing(entity);
            handleMobArmorBonus(entity);
            handleMobMaxHealth(entity);
            handleMobAttackDamage(entity);

            return false;
        }

        if(isAnimalSpawningEnabled) {
            double chanceToSpawn = animalSpawning.get(entity.getType());
            double randomChance = rnd.nextDouble(0, 101);

            if(randomChance <= chanceToSpawn)
                return true;
        }

        handleBossSpawning(entity, false);

        if(isBoss(entity))
            return false;

        handleAnimalGrowing(entity);
        handleMobMovement(entity);
        handleMobArmorBonus(entity);
        handleMobMaxHealth(entity);
        handleMobAttackDamage(entity);

        return false;
    }

    private void handleAnimalGrowing(LivingEntity entity) {
        if(!isAnimalGrowingEnabled)
            return;

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
        if(!isMobMovenemtEnabled)
            return;

        if(!isValidWorld(entity.getWorld()))
            return;

        if(!mobMovement.containsKey(entity.getType()))
            return;

        var movementSpeed = mobMovement.get(entity.getType());

        entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(movementSpeed);
    }

    private void handleMobArmorBonus(LivingEntity entity) {
        if(!isMobBonusArmorEnabled)
            return;

        if(!isValidWorld(entity.getWorld()))
            return;

        if(!mobBonusArmor.containsKey(entity.getType()))
            return;

        var bonusArmor = mobBonusArmor.get(entity.getType());

        entity.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(bonusArmor);
    }

    private void handleMobMaxHealth(LivingEntity entity) {
        if(!isMobMaxHealthEnabled)
            return;

        if(!isValidWorld(entity.getWorld()))
            return;

        if(!mobMaxHealth.containsKey(entity.getType()))
            return;

        var maxHealth = mobMaxHealth.get(entity.getType());

        entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
        entity.setHealth(entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
    }

    private void handleMobAttackDamage(LivingEntity entity) {
        if(isMobAttackDamageEnabled)
            return;

        if(!isValidWorld(entity.getWorld()))
            return;

        if(!mobAttackDamage.containsKey(entity.getType()))
            return;

        var attackDamage = mobAttackDamage.get(entity.getType());

        entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(attackDamage);
    }

    public boolean handleCropGrowing(Material material, World world) {
        if(!isPreventCropGrowingEnabled)
            return false;

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
        if(!isLootDropsEnabled)
            return;

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

    private boolean isBoss(Entity entity) {
        for(var boss : activeBosses) {
            if(entity == boss.getEntity())
                return true;
        }

        return false;
    }

    public void handleBossSpawning(LivingEntity entity, boolean forceBoss) {
        if(!isValidWorld(entity.getWorld()) && !forceBoss)
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
        var nearbyEntites = entity.getNearbyEntities(25, 10, 25);

        for(var nearbyEntity : nearbyEntites) {
            if(nearbyEntity.getType() == EntityType.PLAYER)
                playerIsNearby = true;
        }

        if(!playerIsNearby && !forceBoss)
            return;

        double randomChance = rnd.nextDouble(0, 101);
        double bossSpawnChance = boss.getSpawnChance();

        if(randomChance <= bossSpawnChance && !forceBoss) {
            activeBosses.add(boss);
            boss.setEntity(entity);
            boss.handleSpawn();
        } else if(forceBoss) {
            activeBosses.add(boss);
            boss.setEntity(entity);
            boss.handleSpawn();
        }
    }

    public void handleBossDeath(EntityDeathEvent event) {
        activeBosses.removeIf(boss -> boss.handleDeath(event));
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
