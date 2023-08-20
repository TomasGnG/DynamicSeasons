package de.tomasgng.utils.template;

import de.tomasgng.DynamicSeasons;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.text.DecimalFormat;
import java.util.*;

public class BossEntity implements Cloneable {

    //region <Variables>
    @Getter @Setter
    private EntityType entityType;
    @Getter @Setter
    private LivingEntity entity;
    @Getter @Setter
    private String displayName;
    @Getter @Setter
    private boolean nameVisible;
    @Getter @Setter
    private boolean babyMob;
    @Getter @Setter
    private double spawnChance;
    @Getter @Setter
    private double maxHealth;
    @Getter @Setter
    private double attackDamage;
    @Getter @Setter
    private double movementSpeed;
    @Getter @Setter
    private double bonusArmor;
    @Getter @Setter
    private int followRange;
    @Getter @Setter
    private int droppedXPOnDeath;
    @Getter @Setter
    private Sound spawnSound;
    @Getter @Setter
    private Sound ambientSound;
    @Getter @Setter
    private Sound deathSound;
    @Getter @Setter
    private Sound takeDamageSound;
    @Getter @Setter
    private Sound dealDamageSound;
    @Getter @Setter
    private boolean executeCommandsEnabled;
    @Getter @Setter
    private List<String> commandsList;
    @Getter @Setter
    private Map<ItemStack, Double> lootDrops;
    @Getter @Setter
    private List<PotionEffect> spawnPotionEffects;
    @Getter @Setter
    private int cycleTime;
    @Getter @Setter
    private List<PotionEffect> cyclePotionEffects;
    private final Random random = new Random();
    private final DecimalFormat df = new DecimalFormat("0.0");
    private final MiniMessage mm = MiniMessage.miniMessage();
    private int lastHitAgo = 0;
    //endregion

    public BossEntity(EntityType entityType,
                      String displayName,
                      boolean nameVisible,
                      boolean babyMob,
                      double spawnChance,
                      double maxHealth,
                      double attackDamage,
                      double movementSpeed,
                      double bonusArmor,
                      int followRange,
                      int droppedXPOnDeath,
                      Sound spawnSound,
                      Sound ambientSound,
                      Sound deathSound,
                      Sound takeDamageSound,
                      Sound dealDamageSound,
                      boolean executeCommandsEnabled,
                      List<String> commandsList,
                      Map<ItemStack, Double> lootDrops,
                      List<PotionEffect> spawnPotionEffects,
                      int cycleTime,
                      List<PotionEffect> cyclePotionEffects) {
        setEntityType(entityType);
        setDisplayName(displayName);
        setNameVisible(nameVisible);
        setBabyMob(babyMob);
        setSpawnChance(spawnChance);
        setMaxHealth(maxHealth);
        setAttackDamage(attackDamage);
        setMovementSpeed(movementSpeed);
        setBonusArmor(bonusArmor);
        setFollowRange(followRange);
        setDroppedXPOnDeath(droppedXPOnDeath);
        setSpawnSound(spawnSound);
        setAmbientSound(ambientSound);
        setDeathSound(deathSound);
        setTakeDamageSound(takeDamageSound);
        setDealDamageSound(dealDamageSound);
        setExecuteCommandsEnabled(executeCommandsEnabled);
        setCommandsList(commandsList);
        setLootDrops(lootDrops);
        setSpawnPotionEffects(spawnPotionEffects);
        setCycleTime(cycleTime);
        setCyclePotionEffects(cyclePotionEffects);
    }

    private void setBossStats() {
        entity.customName(mm.deserialize(getDisplayName()
                .replace("%hp%", df.format(getMaxHealth()))
                .replace("%maxHP%", df.format(getMaxHealth()))));
        entity.setCustomNameVisible(isNameVisible());
        entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(getMaxHealth());
        entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(getAttackDamage());
        entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(getMovementSpeed());
        entity.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(getBonusArmor());
        entity.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(getFollowRange());
        entity.setHealth(getMaxHealth());
        entity.setSilent(true);
        entity.addPotionEffects(spawnPotionEffects);
        if(entity instanceof Ageable ageable) {
            if(!isBabyMob() && !ageable.isAdult()) {
                ageable.setAdult();
            }
        }
        Bukkit.getScheduler().runTaskTimer(DynamicSeasons.getInstance(), () -> {
            lastHitAgo++;
        }, 0L,20L);
        Bukkit.getScheduler().runTaskTimer(DynamicSeasons.getInstance(), () -> {
            entity.addPotionEffects(cyclePotionEffects);
            Bukkit.getScheduler().runTask(DynamicSeasons.getInstance(), () -> {
                entity.customName(mm.deserialize(getDisplayName()
                        .replace("%hp%", df.format(entity.getHealth()))
                        .replace("%maxHP%", df.format(getMaxHealth()))));
            });
        }, cycleTime * 20L, cycleTime * 20L);
    }

    //region <PlaySound methods>
    private void startAmbientSounds() {
        Bukkit.getScheduler().runTaskLater(DynamicSeasons.getInstance(), () -> {
            if(!entity.isDead()) {
                entity.getNearbyEntities(25, 25, 25).forEach(nearbyEntity -> {
                    if(nearbyEntity.getType() == EntityType.PLAYER) {
                        if(lastHitAgo >= 3)
                            nearbyEntity.playSound(getAmbientSound());
                    }
                });
                startAmbientSounds();
            }
        }, random.nextInt(3, 6)*20L);
    }

    private void playSpawnSound() {
        entity.getNearbyEntities(25, 25, 25).forEach(nearbyEntity -> {
            Bukkit.getScheduler().runTaskLater(DynamicSeasons.getInstance(), () -> {
                if(nearbyEntity.getType() == EntityType.PLAYER) {
                    nearbyEntity.playSound(getSpawnSound());
                }
            }, 1L);
        });
    }

    private void playDeathSound() {
        entity.getNearbyEntities(25, 25, 25).forEach(nearbyEntity -> {
            nearbyEntity.playSound(getDeathSound());
            nearbyEntity.stopSound(getTakeDamageSound());
            nearbyEntity.stopSound(getDealDamageSound());
        });
    }

    private void playTakeDamageSound() {
        entity.getNearbyEntities(25, 25, 25).forEach(nearbyEntity -> {
            nearbyEntity.playSound(getTakeDamageSound());
        });
    }

    private void playDealDamageSound() {
        entity.getNearbyEntities(25, 25, 25).forEach(nearbyEntity -> {
            nearbyEntity.playSound(getDealDamageSound());
        });
    }
    //endregion

    //region <Handlers>
    public void handleSpawn() {
        playSpawnSound();
        startAmbientSounds();
        setBossStats();
        entity.setRemoveWhenFarAway(false);
    }

    public void handleDealDamage() {
        playDealDamageSound();
        lastHitAgo = 0;
    }

    public void handleTakeDamage() {
        playTakeDamageSound();
        lastHitAgo = 0;
        Bukkit.getScheduler().runTask(DynamicSeasons.getInstance(), () -> {
            entity.customName(mm.deserialize(getDisplayName()
                    .replace("%hp%", df.format(entity.getHealth()))
                    .replace("%maxHP%", df.format(getMaxHealth()))));
        });
    }

    public void handleDeath(EntityDeathEvent event) {
        if(event.getEntity() != entity)
            return;
        var killer = entity.getKiller();

        event.setDroppedExp(getDroppedXPOnDeath());
        event.setShouldPlayDeathSound(false);
        event.getDrops().clear();
        playDeathSound();

        if(killer == null)
            return;
        var world = entity.getWorld();
        var loc = entity.getLocation();
        for(var entry : lootDrops.entrySet()) {
            double randomChance = random.nextDouble(1, 101);
            if(randomChance <= entry.getValue())
                world.dropItemNaturally(loc, entry.getKey());
        }

        if(!isExecuteCommandsEnabled())
            return;
        getCommandsList().forEach(cmd -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", killer.getName()));
        });
    }
    //endregion

    @Override
    public BossEntity clone() {
        try {
            BossEntity cloned = (BossEntity) super.clone();

            cloned.entityType = this.entityType;
            cloned.displayName = this.displayName;
            cloned.nameVisible = this.nameVisible;
            cloned.spawnChance = this.spawnChance;
            cloned.maxHealth = this.maxHealth;
            cloned.attackDamage = this.attackDamage;
            cloned.movementSpeed = this.movementSpeed;
            cloned.bonusArmor = this.bonusArmor;
            cloned.followRange = this.followRange;
            cloned.droppedXPOnDeath = this.droppedXPOnDeath;
            cloned.spawnSound = this.spawnSound;
            cloned.ambientSound = this.ambientSound;
            cloned.deathSound = this.deathSound;
            cloned.takeDamageSound = this.takeDamageSound;
            cloned.dealDamageSound = this.dealDamageSound;
            cloned.executeCommandsEnabled = this.executeCommandsEnabled;
            cloned.commandsList = new ArrayList<>(this.commandsList);
            cloned.lootDrops = new HashMap<>(this.lootDrops);

            return cloned;
        } catch (CloneNotSupportedException e) {
            System.out.println("Clone error: " + e);
        }
        return null;
    }
}
