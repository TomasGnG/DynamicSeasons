package de.tomasgng.utils.managers;

import de.tomasgng.DynamicSeasons;
import de.tomasgng.utils.enums.SeasonType;
import de.tomasgng.utils.enums.WeatherType;
import de.tomasgng.utils.template.BossEntity;
import de.tomasgng.utils.template.ItemBuilder;
import de.tomasgng.utils.template.LootDrop;
import de.tomasgng.utils.template.Particles;
import lombok.SneakyThrows;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class ConfigManager {

    private final File folder = new File("plugins/DynamicSeasons");
    private final File config = new File("plugins/DynamicSeasons/config.yml");
    private final File springFile = new File("plugins/DynamicSeasons/spring.yml");
    private final File summerFile = new File("plugins/DynamicSeasons/summer.yml");
    private final File fallFile = new File("plugins/DynamicSeasons/fall.yml");
    private final File winterFile = new File("plugins/DynamicSeasons/winter.yml");
    private YamlConfiguration configCfg = YamlConfiguration.loadConfiguration(config);
    private YamlConfiguration springCfg = YamlConfiguration.loadConfiguration(springFile);
    private YamlConfiguration summerCfg = YamlConfiguration.loadConfiguration(summerFile);
    private YamlConfiguration fallCfg = YamlConfiguration.loadConfiguration(fallFile);
    private YamlConfiguration winterCfg = YamlConfiguration.loadConfiguration(winterFile);
    private Connection connection;
    private final String sqlSelectAllData = "SELECT * FROM data";
    private final String sqlUpdateTime = "UPDATE data SET time=?";
    private final Logger logger = DynamicSeasons.getInstance().getLogger();
    private final MiniMessage mm = MiniMessage.miniMessage();

    public ConfigManager() {
        createFiles();
        createConnection();
        setupDatabase();
    }

    //region(SQLite)
    @SneakyThrows
    private void createConnection() {
        connection = DriverManager.getConnection("jdbc:sqlite:plugins/DynamicSeasons/data.db");

        try (var statement = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS data (time BIGINT, season TEXT)")) {
            statement.executeUpdate();
        }
    }

    @SneakyThrows
    private void setupDatabase() {
        try(var statement = connection.prepareStatement(sqlSelectAllData)) {
            var rs = statement.executeQuery();

            if(rs.next())
                return;
        }

        try(var statement2 = connection.prepareStatement("INSERT INTO data VALUES (?, ?)")) {
            statement2.setInt(1, getDuration());
            statement2.setString(2, SeasonType.SPRING.name());
            statement2.executeUpdate();
        }
    }

    @SneakyThrows
    public SeasonType getCurrentSeasonTypeFromDatabase() {
        try(var statement = connection.prepareStatement(sqlSelectAllData)) {
            var rs = statement.executeQuery();

            rs.next();
            return SeasonType.valueOf(rs.getString("season"));
        }
    }

    @SneakyThrows
    public int getRemainingTimeFromDatabase() {
        try (var statement = connection.prepareStatement(sqlSelectAllData)) {
            var rs = statement.executeQuery();

            rs.next();
            return rs.getInt("time");
        }
    }

    @SneakyThrows
    public void decreaseRemainingTime() {
        try(var statement = connection.prepareStatement(sqlUpdateTime)) {
            statement.setInt(1, getRemainingTimeFromDatabase()-1);
            statement.executeUpdate();
        }
    }

    @SneakyThrows
    public void updateCurrentSeason(SeasonType newSeason) {
        try(var statement = connection.prepareStatement("UPDATE data SET season=?")) {
            statement.setString(1, newSeason.name());
            statement.executeUpdate();
        }
    }

    @SneakyThrows
    public void resetRemainingTime() {
        try(var statement = connection.prepareStatement(sqlUpdateTime)) {
            statement.setInt(1, getDuration());
            statement.executeUpdate();
        }
    }

    @SneakyThrows
    public void setRemainingTime(int newRemainingTime) {
        try(var statement = connection.prepareStatement(sqlUpdateTime)) {
            statement.setInt(1, newRemainingTime);
            statement.executeUpdate();
        }
    }
    //endregion

    @SneakyThrows
    private void createFiles() {
        if(!folder.exists()) folder.mkdirs();

        if(!config.exists()) {
            config.createNewFile();

            configCfg.set("season_duration", 300);
            configCfg.set("worlds", List.of("world"));
            configCfg.set("placeholders.duration.placeholderName", "duration");
            configCfg.set("placeholders.duration.format", "HH:mm:ss");
            configCfg.set("placeholders.currentSeason.placeholderName", "currentSeason");
            configCfg.set("placeholders.currentSeason.text.spring", "Spring");
            configCfg.set("placeholders.currentSeason.text.summer", "Summer");
            configCfg.set("placeholders.currentSeason.text.fall", "Fall");
            configCfg.set("placeholders.currentSeason.text.winter", "Winter");
            configCfg.set("command.name", "dynamicseasons");
            configCfg.set("command.description", "Main DynamicSeasons command.");
            configCfg.set("command.permission", "dynamicseasons.cmd.use");
            configCfg.set("command.alias", List.of("dynseasons"));
            configCfg.setComments("placeholders.duration.format", List.of("Use your own date format. For help use this site: https://help.gooddata.com/cloudconnect/manual/date-and-time-format.html#:~:text=Table%C2%A028.5.%C2%A0Date%20and%20Time%20Format%20Patterns%20and%20Results%20(Java)"));
            configCfg.setComments("worlds", List.of("Specify the worlds where the seasons should work."));
            configCfg.setComments("season_duration", List.of("Visit the wiki for more help: https://github.com/TomasGnG/DynamicSeasons/wiki", "Specify the duration of the seasons in Seconds", "e.g. for one hour -> season_duration: 3600 "));
            configCfg.setComments("placeholders.currentSeason.text", List.of("Set the replacement for these seasons."));
        }

        if(!springFile.exists()) {
            springFile.createNewFile();
            createConfigValues(springCfg);
        }

        if(!summerFile.exists()) {
            summerFile.createNewFile();
            createConfigValues(summerCfg);
        }

        if(!fallFile.exists()) {
            fallFile.createNewFile();
            createConfigValues(fallCfg);
        }

        if(!winterFile.exists()) {
            winterFile.createNewFile();
            createConfigValues(winterCfg);
        }

        save();
    }
    
    private void createConfigValues(YamlConfiguration seasonCfg) {
        seasonCfg.set("weather.enabled", true);
        seasonCfg.set("weather.type.clear", true);
        seasonCfg.set("weather.type.storm", true);
        seasonCfg.set("weather.type.thunder", false);

        seasonCfg.set("randomTickSpeed", 4);

        seasonCfg.set("animalSpawning.enabled", true);
        seasonCfg.set("animalSpawning.SHEEP", 80);
        seasonCfg.set("animalSpawning.CHICKEN", 40);

        seasonCfg.set("mobMovement.enabled", true);
        seasonCfg.set("mobMovement.ZOMBIE", 0.3);
        seasonCfg.set("mobMovement.SPIDER", 0.4);

        seasonCfg.set("animalGrowing.enabled", true);
        seasonCfg.set("animalGrowing.COW", 6000);
        seasonCfg.set("animalGrowing.SHEEP", 3600);

        seasonCfg.set("mobBonusArmor.enabled", true);
        seasonCfg.set("mobBonusArmor.ZOMBIE", 2.5);
        seasonCfg.set("mobBonusArmor.CREEPER", 1.0);

        seasonCfg.set("mobMaxHealth.enabled", true);
        seasonCfg.set("mobMaxHealth.CREEPER", 25.0);
        seasonCfg.set("mobMaxHealth.ZOMBIE", 30.0);

        seasonCfg.set("mobAttackDamage.enabled", true);
        seasonCfg.set("mobAttackDamage.ZOMBIE", 4.0);
        seasonCfg.set("mobAttackDamage.SPIDER", 3.0);

        seasonCfg.set("preventCropGrowing.enabled", true);
        seasonCfg.set("preventCropGrowing.crops", List.of("POTATOES", "CARROTS"));

        seasonCfg.set("potionEffects.enabled", true);
        seasonCfg.set("potionEffects.SPEED", 1);
        seasonCfg.set("potionEffects.REGENERATION", 1);

        seasonCfg.set("lootDrops.enabled", true);
        seasonCfg.set("lootDrops.ZOMBIE.1.displayname", "<yellow>Mysterious Sword");
        seasonCfg.set("lootDrops.ZOMBIE.1.lore", List.of("<gray>This sword is", "<gray>veeery mysterious!"));
        seasonCfg.set("lootDrops.ZOMBIE.1.material", Material.DIAMOND_SWORD.name());
        seasonCfg.set("lootDrops.ZOMBIE.1.amount", 1);
        seasonCfg.set("lootDrops.ZOMBIE.1.dropChance", 10);
        seasonCfg.set("lootDrops.ZOMBIE.1.enchantments.sharpness", 2);

        seasonCfg.set("bossSpawning.ZOMBIE.displayName", "<yellow>BOSS <dark_gray>| <green>%hp%<dark_gray>/<yellow>%maxHP% <dark_red>❤");
        seasonCfg.set("bossSpawning.ZOMBIE.nameVisible", false);
        seasonCfg.set("bossSpawning.ZOMBIE.babyMob", false);
        seasonCfg.set("bossSpawning.ZOMBIE.spawnChance", 0.5);
        seasonCfg.set("bossSpawning.ZOMBIE.maxHealth", 60.0);
        seasonCfg.set("bossSpawning.ZOMBIE.attackDamage", 5.0);
        seasonCfg.set("bossSpawning.ZOMBIE.movementSpeed", 0.3);
        seasonCfg.set("bossSpawning.ZOMBIE.bonusArmor", 20.0);
        seasonCfg.set("bossSpawning.ZOMBIE.followRange", 20);
        seasonCfg.set("bossSpawning.ZOMBIE.droppedXPOnDeath", 400);
        seasonCfg.set("bossSpawning.ZOMBIE.sounds.spawn.sound", Sound.ENTITY_ENDER_DRAGON_GROWL.key().value());
        seasonCfg.set("bossSpawning.ZOMBIE.sounds.spawn.volume", 1.0F);
        seasonCfg.set("bossSpawning.ZOMBIE.sounds.spawn.pitch", 0.5F);
        seasonCfg.set("bossSpawning.ZOMBIE.sounds.ambient.sound", Sound.ENTITY_ENDER_DRAGON_AMBIENT.key().value());
        seasonCfg.set("bossSpawning.ZOMBIE.sounds.ambient.volume", 1.0F);
        seasonCfg.set("bossSpawning.ZOMBIE.sounds.ambient.pitch", 0.5F);
        seasonCfg.set("bossSpawning.ZOMBIE.sounds.death.sound", Sound.ENTITY_ENDER_DRAGON_DEATH.key().value());
        seasonCfg.set("bossSpawning.ZOMBIE.sounds.death.volume", 1.0F);
        seasonCfg.set("bossSpawning.ZOMBIE.sounds.death.pitch", 0.5F);
        seasonCfg.set("bossSpawning.ZOMBIE.sounds.takeDamage.sound", Sound.ENTITY_ENDER_DRAGON_HURT.key().value());
        seasonCfg.set("bossSpawning.ZOMBIE.sounds.takeDamage.volume", 1.0F);
        seasonCfg.set("bossSpawning.ZOMBIE.sounds.takeDamage.pitch", 0.5F);
        seasonCfg.set("bossSpawning.ZOMBIE.sounds.dealDamage.sound", Sound.ENTITY_ENDER_DRAGON_SHOOT.key().value());
        seasonCfg.set("bossSpawning.ZOMBIE.sounds.dealDamage.volume", 1.0F);
        seasonCfg.set("bossSpawning.ZOMBIE.sounds.dealDamage.pitch", 0.5F);
        seasonCfg.set("bossSpawning.ZOMBIE.executeCommandsOnDeath.enabled", false);
        seasonCfg.set("bossSpawning.ZOMBIE.executeCommandsOnDeath.commands", List.of("bal add %player% 100"));
        seasonCfg.set("bossSpawning.ZOMBIE.lootOnDeath.1.displayname", "<gold>Legendary Sword");
        seasonCfg.set("bossSpawning.ZOMBIE.lootOnDeath.1.lore", List.of("<gray>This sword is", "<gold>LEGENDARY!"));
        seasonCfg.set("bossSpawning.ZOMBIE.lootOnDeath.1.material", Material.DIAMOND_SWORD.name());
        seasonCfg.set("bossSpawning.ZOMBIE.lootOnDeath.1.amount", 1);
        seasonCfg.set("bossSpawning.ZOMBIE.lootOnDeath.1.dropChance", 10);
        seasonCfg.set("bossSpawning.ZOMBIE.lootOnDeath.1.enchantments.sharpness", 6);
        seasonCfg.set("bossSpawning.ZOMBIE.potionEffects.spawn.INCREASE_DAMAGE.level", 1);
        seasonCfg.set("bossSpawning.ZOMBIE.potionEffects.spawn.INCREASE_DAMAGE.time", 10);
        seasonCfg.set("bossSpawning.ZOMBIE.potionEffects.spawn.SPEED.level", 1);
        seasonCfg.set("bossSpawning.ZOMBIE.potionEffects.spawn.SPEED.time", 10);
        seasonCfg.set("bossSpawning.ZOMBIE.potionEffects.cycle.time", 5);
        seasonCfg.set("bossSpawning.ZOMBIE.potionEffects.cycle.effects.HARM.level", 1);
        seasonCfg.set("bossSpawning.ZOMBIE.potionEffects.cycle.effects.HARM.time", 1);
        seasonCfg.set("bossSpawning.ZOMBIE.summoning.enabled", true);
        seasonCfg.set("bossSpawning.ZOMBIE.summoning.radius", 5);
        seasonCfg.set("bossSpawning.ZOMBIE.summoning.cycleTime", 15);
        seasonCfg.set("bossSpawning.ZOMBIE.summoning.minSpawnCount", 1);
        seasonCfg.set("bossSpawning.ZOMBIE.summoning.maxSpawnCount", 4);
        seasonCfg.set("bossSpawning.ZOMBIE.summoning.mobs", List.of(EntityType.ZOMBIE.name(), EntityType.SKELETON.name()));

        seasonCfg.set("particles.enabled", true);
        seasonCfg.set("particles.offsetX", 5);
        seasonCfg.set("particles.offsetY", 5);
        seasonCfg.set("particles.offsetZ", 5);
        seasonCfg.set("particles.spawnTime", 5);
        seasonCfg.set("particles.speed", 0.0);
        seasonCfg.set("particles.particle.SNOWFLAKE.minSpawnAmount", 10);
        seasonCfg.set("particles.particle.SNOWFLAKE.maxSpawnAmount", 40);

        seasonCfg.set("xpBonus", 20);

        seasonCfg.setInlineComments("particles.enabled", List.of("Should this feature be enabled?"));
        seasonCfg.setInlineComments("particles.offsetX", List.of("spread the spawned particle"));
        seasonCfg.setInlineComments("particles.offsetY", List.of("spread the spawned particle"));
        seasonCfg.setInlineComments("particles.offsetZ", List.of("spread the spawned particle"));
        seasonCfg.setInlineComments("particles.spawnTime", List.of("Repeating time in ticks (20 ticks = 1 second)"));
        seasonCfg.setInlineComments("particles.speed", List.of("Speed of the particles"));
        seasonCfg.setInlineComments("particles.particle.SNOWFLAKE.minSpawnAmount", List.of("Minimum spawn amount of the particle"));
        seasonCfg.setInlineComments("particles.particle.SNOWFLAKE.maxSpawnAmount", List.of("Maximum spawn amount of the particle"));
        seasonCfg.setInlineComments("bossSpawning.ZOMBIE.summoning.enabled", List.of("Should this feature be enabled?"));
        seasonCfg.setInlineComments("bossSpawning.ZOMBIE.summoning.radius", List.of("Spawnradius in blocks"));
        seasonCfg.setInlineComments("bossSpawning.ZOMBIE.summoning.cycleTime", List.of("The repeating time in seconds"));
        seasonCfg.setInlineComments("bossSpawning.ZOMBIE.summoning.minSpawnCount", List.of("Minimum spawn count of mobs"));
        seasonCfg.setInlineComments("bossSpawning.ZOMBIE.summoning.maxSpawnCount", List.of("Maximum spawn count of mobs"));
        seasonCfg.setInlineComments("bossSpawning.ZOMBIE.summoning.mobs", List.of("Mobs that should spawn -> random mobs from the list will spawn!"));
        seasonCfg.setComments("bossSpawning.ZOMBIE.potionEffects.spawn", List.of("The effects the boss will get when he spawns"));
        seasonCfg.setComments("bossSpawning.ZOMBIE.potionEffects.spawn.INCREASE_DAMAGE", List.of("The PotionEffectType. Here is a list: https://jd.papermc.io/paper/1.20/org/bukkit/potion/PotionEffectType.html#:~:text=Modifier%20and%20Type-,Field,-Description"));
        seasonCfg.setComments("bossSpawning.ZOMBIE.potionEffects.spawn.INCREASE_DAMAGE.level", List.of("The level of the effect"));
        seasonCfg.setComments("bossSpawning.ZOMBIE.potionEffects.spawn.INCREASE_DAMAGE.time", List.of("The effect time in seconds"));
        seasonCfg.setComments("bossSpawning.ZOMBIE.potionEffects.cycle", List.of("The effects the boss will get every cycle while the boss is alive."));
        seasonCfg.setComments("bossSpawning.ZOMBIE.potionEffects.cycle.time", List.of("The time(in seconds) of every cycle. Example: 5 -> The cycle will run every 5 seconds."));
        seasonCfg.setComments("bossSpawning.ZOMBIE.potionEffects.cycle.time.effects", List.of("The effects that the boss will get"));
        seasonCfg.setComments("bossSpawning.ZOMBIE.babyMob", List.of("Should this boss be able to spawn as a baby?"));
        seasonCfg.setComments("bossSpawning.ZOMBIE", List.of("Mobtype"));
        seasonCfg.setComments("bossSpawning.ZOMBIE.displayName", List.of("Displayname of the boss"));
        seasonCfg.setComments("bossSpawning.ZOMBIE.nameVisible", List.of("Should the displayname be visible through walls etc?"));
        seasonCfg.setComments("bossSpawning.ZOMBIE.spawnChance", List.of("The chance of the boss to spawn"));
        seasonCfg.setComments("bossSpawning.ZOMBIE.maxHealth", List.of("The max health of the boss"));
        seasonCfg.setComments("bossSpawning.ZOMBIE.attackDamage", List.of("The attack damage of the boss"));
        seasonCfg.setComments("bossSpawning.ZOMBIE.movementSpeed", List.of("The movementspeed of the boss"));
        seasonCfg.setComments("bossSpawning.ZOMBIE.bonusArmor", List.of("The bonus armor of the boss"));
        seasonCfg.setComments("bossSpawning.ZOMBIE.followRange", List.of("The range of blocks where the boss will follow players"));
        seasonCfg.setComments("bossSpawning.ZOMBIE.droppedXPOnDeath", List.of("The dropped xp when the boss dies"));
        seasonCfg.setComments("bossSpawning.ZOMBIE.sounds", List.of("Customize the sounds that the boss will make"));
        seasonCfg.setComments("bossSpawning.ZOMBIE.sounds.spawn", List.of("The sound when the boss spawns"));
        seasonCfg.setComments("bossSpawning.ZOMBIE.sounds.ambient", List.of("The sound when the boss is alive"));
        seasonCfg.setComments("bossSpawning.ZOMBIE.sounds.death", List.of("The sound when the boss dies"));
        seasonCfg.setComments("bossSpawning.ZOMBIE.sounds.takeDamage", List.of("The sound when the boss takes damage"));
        seasonCfg.setComments("bossSpawning.ZOMBIE.sounds.dealDamage", List.of("The sound when the boss deals damage"));
        seasonCfg.setComments("bossSpawning.ZOMBIE.executeCommandsOnDeath", List.of("Run commands when a player kills the boss", "%player% = killer"));
        seasonCfg.setComments("bossSpawning.ZOMBIE.executeCommandsOnDeath.enabled", List.of("Should this feature be enabled?"));
        seasonCfg.setComments("bossSpawning.ZOMBIE.lootOnDeath", List.of("The loot of the boss when he dies", "This section is similar to the 'lootDrops' section"));
        seasonCfg.setComments("lootDrops", List.of("Here you can customize the custom loot from mobs."));
        seasonCfg.setComments("lootDrops.ZOMBIE", List.of("Name of the mob"));
        seasonCfg.setComments("lootDrops.ZOMBIE.1", List.of("You can name this whatever you like as this isnt that important :)"));
        seasonCfg.setComments("lootDrops.ZOMBIE.1.displayname", List.of("Displayname of the item in MiniMessage format. "));
        seasonCfg.setComments("lootDrops.ZOMBIE.1.lore", List.of("Item Lore in MiniMessage format."));
        seasonCfg.setComments("lootDrops.ZOMBIE.1.material", List.of("Material of the item"));
        seasonCfg.setComments("lootDrops.ZOMBIE.1.amount", List.of("The amount of the item"));
        seasonCfg.setComments("lootDrops.ZOMBIE.1.dropChance", List.of("The chance of this item to drop (0-100) in percent"));
        seasonCfg.setComments("lootDrops.ZOMBIE.1.enchantments", List.of("List of all Enchantments: https://pastebin.com/raw/hyRbnm2q", "Format -> (enchantment): (level)"));
        seasonCfg.setComments("potionEffects", List.of("Customize the potion effects for players", "List of all potion effects: https://pastebin.com/raw/KPh96Mf9"));
        seasonCfg.setComments("preventCropGrowing", List.of("Customize the crops that are not allowed to grow", "List of all crops: https://minecraft.fandom.com/wiki/Crops"));
        seasonCfg.setComments("mobAttackDamage", List.of("Customize the attack damage for mobs", "List of all mobs and their attack damage: https://pastebin.com/raw/XnC3kNXi"));
        seasonCfg.setComments("mobBonusArmor", List.of("Customize the bonus armor for mobs", "2 equals 1 Armor-slot"));
        seasonCfg.setComments("mobMaxHealth", List.of("Customize the max health for mobs", "2 equals 1 heart | MAX is 20", "List of all mobs and their max health: https://pastebin.com/raw/5upq7HVr"));
        seasonCfg.setComments("animalGrowing", List.of("Customize the speed of animal growing", "Most baby mobs take 20 mins (24000 ticks) to grow up", "Here is a list of all breedable animals: https://pastebin.com/raw/zzUAc3XM", "Here is a tick calculator: https://mapmaking.fr/tick/", "IMPORTANT: 20 ticks = 1 second", "Format -> (ANIMAL_NAME): (TIME IN TICKS)"));
        seasonCfg.setComments("mobMovement", List.of("Customize the movement speed of mobs", "Here is a list of all mobs and their default movement speed: https://pastebin.com/raw/2WaGi20Z", "Format -> (MOB_NAME): (SPEED)"));
        seasonCfg.setComments("animalSpawning", List.of("The probability of an animal to spawn. 1-100%", "Here is a list of all animals: https://pastebin.com/raw/Tf3mMGg6", "Format -> (MOB_NAME): (PERCENT)"));
        seasonCfg.setComments("randomTickSpeed", List.of("The growth speed of plants etc. default value -> 3.", "higher -> faster | large values can cause server lag!", "Heres a list what will be effected by the change: https://minecraft.fandom.com/wiki/Tick#:~:text=Most%20blocks%20ignore%20this%20tick%2C%20but%20some%20use%20it%20to%20do%20something%3A"));
        seasonCfg.setComments("weather", List.of("Visit the wiki for more help: https://github.com/TomasGnG/DynamicSeasons/wiki", "Customize the weather for the season"));
        seasonCfg.setComments("weather.type", List.of("Customize the allowed weather types"));
        seasonCfg.setComments("xpBonus", List.of("The bonus xp you get when picking up xp (in percent)", "e.g. if you set 20 then the player will get 20% more xp. (20% of the picked up xp)"));
    }

    @SneakyThrows
    private void save() {
        configCfg.save(config);
        springCfg.save(springFile);
        summerCfg.save(summerFile);
        fallCfg.save(fallFile);
        winterCfg.save(winterFile);
        reload();
    }

    public void reload() {
        configCfg = YamlConfiguration.loadConfiguration(config);
        springCfg = YamlConfiguration.loadConfiguration(springFile);
        summerCfg = YamlConfiguration.loadConfiguration(summerFile);
        fallCfg = YamlConfiguration.loadConfiguration(fallFile);
        winterCfg = YamlConfiguration.loadConfiguration(winterFile);
    }

    private YamlConfiguration getCfgFromSeason(String seasonName) {
        switch (seasonName) {
            case "spring" -> {
                return springCfg;
            }

            case "summer" -> {
                return summerCfg;
            }

            case "fall" -> {
                return fallCfg;
            }

            case "winter" -> {
                return winterCfg;
            }

            default -> {
                return null;
            }
        }
    }

    public String getDurationPlaceholderName() {
        var name = configCfg.getString("placeholders.duration.placeholderName");

        if(name == null) {
            logger.severe("Invalid duration placeholderName. Using default name -> duration");
            return "duration";
        }

        return name;
    }

    public String getDurationPlacerholderRawFormat() {
        var format = configCfg.getString("placeholders.duration.format");

        if(format == null) {
            return "HH:mm:ss";
        }

        return format;
    }

    public String getCurrentSeasonPlaceholderName() {
        var name = configCfg.getString("placeholders.currentSeason.placeholderName");

        if(name == null) {
            logger.severe("Invalid currentSeason placeholderName. Using default name -> currentSeason");
            return "currentSeason";
        }

        return name;
    }

    public String getCurrentSeasonText(SeasonType seasonType) {
        var text = configCfg.getString("placeholders.currentSeason.text."+seasonType.name().toLowerCase());

        if(text == null) {
            logger.severe("Invalid currentSeason text for " + seasonType.name().toLowerCase() + ". Using default text -> " + seasonType.name());
            return seasonType.name();
        }

        return text;
    }

    public int getDuration() {
        int duration = configCfg.getInt("season_duration");

        if(duration < 10) {
            logger.severe("Invalid season_duration. Using default value -> 300!");
            return 300;
        }

        return duration;
    }

    private boolean alreadyPrintedInvalidWorlds = false;

    public List<World> getAllowedWorlds() {
        List<World> worlds = new ArrayList<>();
        var section = configCfg.getStringList("worlds");

        for(var worldName : section) {
            if(Bukkit.getWorld(worldName) == null) {
                if(!alreadyPrintedInvalidWorlds)
                    logger.severe("Invalid world -> \"" + worldName + "\"");
                continue;
            }
            worlds.add(Bukkit.getWorld(worldName));
        }

        if(worlds.isEmpty()) {
            if(!alreadyPrintedInvalidWorlds) {
                logger.severe("0 worlds loaded. Please add worlds to your config.yml");
            }

            DynamicSeasons.getInstance().setPLUGIN_DISABLED(true);
            DynamicSeasons.getInstance().setDISABLED_MESSAGE(mm.deserialize("<br><red>DynamicSeasons | 0 worlds loaded. Please add worlds to your config.yml.<br>"));
        }

        alreadyPrintedInvalidWorlds = true;
        return worlds;
    }

    public String getCMDName() {
        if(!configCfg.isSet("command.name")) {
            configCfg.set("command.name", "dynamicseasons");
            configCfg.set("command.description", "Main DynamicSeasons command.");
            configCfg.set("command.permission", "dynamicseasons.cmd.use");
            configCfg.set("command.alias", List.of("dynseasons"));
            save();
        }

        var name = configCfg.getString("command.name");
        return name == null ? "dynamicseasons" : name;
    }

    public String getCMDDescription() {
        var description = configCfg.getString("command.description");
        return description == null ? "Main DynamicSeasons command." : description;
    }

    public String getCMDPermission() {
        var permission = configCfg.getString("command.permission");
        return permission == null ? "dynamicseasons.cmd.use" : permission;
    }

    public List<String> getCMDAliases() {
        return configCfg.getStringList("command.alias");
    }

    public boolean getWeather(String season) {
        return getCfgFromSeason(season).getBoolean("weather.enabled");
    }

    public List<WeatherType> getWeatherTypes(String season) {
        var cfg = getCfgFromSeason(season);
        List<WeatherType> weatherTypes = new ArrayList<>();
        var sectionKeys = cfg.getConfigurationSection("weather.type").getKeys(false);

        for(var weatherType : sectionKeys) {
            if(cfg.getBoolean("weather.type." + weatherType))
                weatherTypes.add(WeatherType.valueOf(weatherType.toUpperCase()));
        }

        return weatherTypes;
    }

    public int randomTickSpeed(String season) {
        var cfg = getCfgFromSeason(season);
        int randomTickSpeed = cfg.getInt("randomTickSpeed");

        if(randomTickSpeed <= 0) {
            logger.severe("[" + season + "] Invalid randomTickSpeed");
            return 3;
        }

        return randomTickSpeed;
    }

    public Map<EntityType, Double> getAnimalSpawning(String season) {
        var cfg = getCfgFromSeason(season);
        Map<EntityType, Double> animalSpawning = new HashMap<>();

        if(cfg.getConfigurationSection("animalSpawning") == null) {
            logger.severe("[" + season + "] Invalid animalSpawning");
            return animalSpawning;
        }

        var sectionKeys = cfg.getConfigurationSection("animalSpawning").getKeys(false);

        for (var animal : sectionKeys) {
            if(animal.equalsIgnoreCase("enabled"))
                continue;

            try {
                EntityType entityType = EntityType.valueOf(animal);
                double spawnChance = cfg.getDouble("animalSpawning." + animal);

                if(spawnChance > 100 || spawnChance < 0) {
                    logger.severe("[" + season + "] Invalid spawnChance for " + animal);
                    continue;
                }

                animalSpawning.put(entityType, spawnChance);
            } catch (Exception e) {
                logger.severe("[" + season + "] Invalid animalSpawning for " + animal);
            }
        }

        return animalSpawning;
    }

    public Map<EntityType, Double> getMobMovement(String season) {
        var cfg = getCfgFromSeason(season);
        Map<EntityType, Double> mobMovement = new HashMap<>();

        if(cfg.getConfigurationSection("mobMovement") == null) {
            logger.severe("[" + season + "] Invalid mobMovement");
            return mobMovement;
        }

        var sectionKeys = cfg.getConfigurationSection("mobMovement").getKeys(false);

        for (var mob : sectionKeys) {
            if(mob.equalsIgnoreCase("enabled"))
                continue;

            try {
                EntityType entityType = EntityType.valueOf(mob);
                double movement = cfg.getDouble("mobMovement." + mob);

                if(movement < 0)
                    throw new NullPointerException();

                mobMovement.put(entityType, movement);
            } catch (Exception e) {
                logger.severe("[" + season + "] Invalid mobMovement for " + mob);
            }
        }

        return mobMovement;
    }

    public Map<EntityType, Integer> getAnimalGrowing(String season) {
        var cfg = getCfgFromSeason(season);
        Map<EntityType, Integer> animalGrowing = new HashMap<>();

        if(cfg.getConfigurationSection("animalGrowing") == null) {
            logger.severe("[" + season + "] Invalid animalGrowing");
            return animalGrowing;
        }

        var sectionKeys = cfg.getConfigurationSection("animalGrowing").getKeys(false);

        for (var animal : sectionKeys) {
            if(animal.equalsIgnoreCase("enabled"))
                continue;

            try {
                EntityType entityType = EntityType.valueOf(animal);
                int growTimeInTicks = cfg.getInt("animalGrowing." + animal);

                if(growTimeInTicks < 20)
                    throw new NullPointerException();

                animalGrowing.put(entityType, growTimeInTicks);
            } catch (Exception e) {
                logger.severe("[" + season + "] Invalid animalGrowing for " + animal);
            }
        }

        return animalGrowing;
    }

    public Map<EntityType, Double> getMobBonusArmor(String season) {
        var cfg = getCfgFromSeason(season);
        Map<EntityType, Double> mobBonusArmor = new HashMap<>();

        if(cfg.getConfigurationSection("mobBonusArmor") == null) {
            logger.severe("[" + season + "] Invalid mobBonusArmor");
            return mobBonusArmor;
        }

        var sectionKeys = cfg.getConfigurationSection("mobBonusArmor").getKeys(false);

        for (var mob : sectionKeys) {
            if(mob.equalsIgnoreCase("enabled"))
                continue;

            try {
                EntityType entityType = EntityType.valueOf(mob);
                double bonusArmor = cfg.getDouble("mobBonusArmor." + mob);

                if(bonusArmor < 0) {
                    throw new Exception();
                }

                mobBonusArmor.put(entityType, bonusArmor);
            } catch (Exception e) {
                logger.severe("[" + season + "] Invalid mobBonusArmor for " + mob);
            }
        }

        return mobBonusArmor;
    }

    public Map<EntityType, Double> getMobMaxHealth(String season) {
        var cfg = getCfgFromSeason(season);
        Map<EntityType, Double> mobMaxHealth = new HashMap<>();

        if(cfg.getConfigurationSection("mobMaxHealth") == null) {
            logger.severe("[" + season + "] Invalid mobMaxHealth");
            return mobMaxHealth;
        }

        var sectionKeys = cfg.getConfigurationSection("mobMaxHealth").getKeys(false);

        for (var mob : sectionKeys) {
            if(mob.equalsIgnoreCase("enabled"))
                continue;

            try {
                EntityType entityType = EntityType.valueOf(mob);
                double maxHealth = cfg.getDouble("mobMaxHealth." + mob);

                if(maxHealth <= 0)
                    throw new NullPointerException();

                mobMaxHealth.put(entityType, maxHealth);
            } catch (Exception e) {
                logger.severe("[" + season + "] Invalid mobMaxHealth for " + mob);
            }
        }

        return mobMaxHealth;
    }

    public Map<EntityType, Double> getMobAttackDamage(String season) {
        var cfg = getCfgFromSeason(season);
        Map<EntityType, Double> mobAttackDamage = new HashMap<>();

        if(cfg.getConfigurationSection("mobAttackDamage") == null) {
            logger.severe("[" + season + "] Invalid mobAttackDamage");
            return mobAttackDamage;
        }

        var sectionKeys = cfg.getConfigurationSection("mobAttackDamage").getKeys(false);

        for (var mob : sectionKeys) {
            if(mob.equalsIgnoreCase("enabled"))
                continue;

            try {
                EntityType entityType = EntityType.valueOf(mob);
                double attackDamage = cfg.getDouble("mobAttackDamage." + mob);

                if(attackDamage <= 0)
                    throw new NullPointerException();

                mobAttackDamage.put(entityType, attackDamage);
            } catch (Exception e) {
                logger.severe("[" + season + "] Invalid mobAttackDamage for " + mob);
            }
        }

        return mobAttackDamage;
    }

    public List<Material> getPreventCropGrowing(String season) {
        var cfg = getCfgFromSeason(season);
        List<Material> preventCropGrowing = new ArrayList<>();

        if(!cfg.isSet("preventCropGrowing")) {
            cfg.set("preventCropGrowing.enabled", true);
            cfg.set("preventCropGrowing.crops", List.of("POTATOES", "CARROTS"));
            save();
        }

        if(!cfg.isSet("preventCropGrowing.enabled")) {
            cfg.set("preventCropGrowing.enabled", true);
            save();
        }

        if(!cfg.isSet("preventCropGrowing.crops")) {
            cfg.set("preventCropGrowing.crops", List.of("POTATOES", "CARROTS"));
            save();
        }

        var rawpreventCropGrowingList = cfg.getStringList("preventCropGrowing.crops");

        for(var cropString : rawpreventCropGrowingList) {
            try {
                var cropType = Material.valueOf(cropString);

                preventCropGrowing.add(cropType);
            } catch (Exception e) {
                logger.severe("[" + season + "] Invalid preventCropGrowing for " + cropString);
            }
        }

        return preventCropGrowing;
    }

    public List<PotionEffect> getPotionEffects(String season) {
        var cfg = getCfgFromSeason(season);
        List<PotionEffect> potionEffects = new ArrayList<>();

        if(cfg.getConfigurationSection("potionEffects") == null) {
            logger.severe("[" + season + "] Invalid potionEffects");
            return potionEffects;
        }

        var keys = cfg.getConfigurationSection("potionEffects").getKeys(false);

        for(var key : keys) {
            if(key.equalsIgnoreCase("enabled"))
                continue;

            try {
                var type = PotionEffectType.getByName(key.toUpperCase());
                var amplifier = cfg.getInt("potionEffects." + key)-1;

                if(amplifier < 0)
                    throw new Exception();

                potionEffects.add(type.createEffect(10*20, amplifier));
            } catch (Exception e) {
                logger.severe("[" + season + "] Invalid potionEffects for " + key);
            }
        }

        return potionEffects;
    }

    public List<LootDrop> getLootDrops(String season) {
        var cfg = getCfgFromSeason(season);
        List<LootDrop> lootDrops = new ArrayList<>();
        var entities = cfg.getConfigurationSection("lootDrops").getKeys(false);

        for(var entity : entities) {
            if(entity.equalsIgnoreCase("enabled"))
                continue;

            try {
                EntityType.valueOf(entity);
            } catch (Exception e) {
                logger.severe("[" + season + "] Invalid LootDrops for " + entity);
                continue;
            }

            Map<ItemStack, Double> itemList = new HashMap<>();
            var itemKeys = cfg.getConfigurationSection("lootDrops." + entity).getKeys(false);

            for(var item : itemKeys) {
                var displayname = cfg.getString("lootDrops." + entity + "." + item + ".displayname");
                var lore = cfg.getStringList("lootDrops." + entity + "." + item + ".lore");
                var material = cfg.getString("lootDrops." + entity + "." + item + ".material");
                var amount = cfg.getInt("lootDrops." + entity + "." + item + ".amount");
                var dropChance = cfg.getDouble("lootDrops." + entity + "." + item + ".dropChance");
                var enchantments = new HashMap<Enchantment, Integer>();

                if(cfg.getConfigurationSection("lootDrops." + entity + "." + item + ".enchantments") == null) {
                    logger.severe("[" + season + "] Invalid enchantments section\n" +
                            "Mob: " + entity + "\n" +
                            "Item: " + item);
                    continue;
                }

                cfg.getConfigurationSection("lootDrops." + entity + "." + item + ".enchantments").getKeys(false).forEach(enchantmentName -> {
                    var level = cfg.getInt("lootDrops." + entity + "." + item + ".enchantments." + enchantmentName);
                    var enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchantmentName));

                    if(enchantment != null) {
                        if(level != 0) {
                            enchantments.put(enchantment, level);
                        } else
                            logger.severe("[" + season + "] Invalid enchantment level '" + level + "'\n" +
                                    "Mob: " + entity + "\n" +
                                    "Item: " + item);
                    } else
                        logger.severe("[" + season + "] Invalid enchantment '" + enchantmentName + "'\n" +
                                "Mob: " + entity + "\n" +
                                "Item: " + item);
                });

                if(displayname == null) {
                    logger.severe("[" + season + "] Invalid displayname!\n" +
                            "Mob: " + entity + "\n" +
                            "Item: " + item);
                    continue;
                }

                try {
                    mm.deserialize(displayname);
                } catch (Exception e) {
                    logger.severe("[" + season + "] Invalid displayname format!\n" +
                            "Mob: " + entity + "\n" +
                            "Item: " + item + "\n" +
                            "Error: " + e.getMessage());
                    continue;
                }

                if(!lore.isEmpty()) {
                    lore.forEach(line -> {
                        try {
                            mm.deserialize(line);
                        } catch (Exception e) {
                            logger.severe("[" + season + "] Invalid lore line '" + line + "'\n" +
                                    "Mob: " + entity + "\n" +
                                    "Item: " + item + "\n" +
                                    "Error: " + e.getMessage());
                            lore.remove(line);
                        }
                    });
                }

                if(material == null) {
                    logger.severe("[" + season + "] Invalid material!\n" +
                            "Mob: " + entity + "\n" +
                            "Item: " + item);
                    continue;
                }

                if(Material.getMaterial(material) == null) {
                    logger.severe("[" + season + "] Invalid material name '" + material + "'\n" +
                            "Mob: " + entity + "\n" +
                            "Item: " + item + "\n");
                    continue;
                }

                if(amount == 0) {
                    logger.severe("[" + season + "] Invalid amount '" + amount + "'\n" +
                            "Mob: " + entity + "\n" +
                            "Item: " + item + "\n");
                    continue;
                }

                if(dropChance < 0) {
                    logger.severe("[" + season + "] Invalid dropChance '" + dropChance + "'\n" +
                            "Mob: " + entity + "\n" +
                            "Item: " + item + "\n");
                    continue;
                }

                itemList.put(new ItemBuilder(Material.getMaterial(material))
                                .setDisplayName(mm.deserialize(displayname))
                                .setLore(lore.toArray(new String[0]))
                                .setAmount(amount)
                                .addEnchantments(enchantments)
                        .build(), dropChance);
            }

            lootDrops.add(new LootDrop(EntityType.valueOf(entity), itemList));
        }

        return lootDrops;
    }

    public List<BossEntity> getBossList(String season) {
        var cfg = getCfgFromSeason(season);
        List<BossEntity> bossList = new ArrayList<>();
        var bossSection = cfg.getConfigurationSection("bossSpawning");

        if(bossSection == null) {
            logger.severe("[" + season + "] Invalid bossSpawning section");
            return bossList;
        }

        var mobTypeSection = bossSection.getKeys(false);

        for(var mobTypeString : mobTypeSection) {
            try {
                EntityType.valueOf(mobTypeString);
            } catch (Exception e) {
                logger.severe("[" + season + "] Invalid bossSpawning mobtype");
                continue;
            }

            var displayName = cfg.getString("bossSpawning." + mobTypeString + ".displayName");
            var nameVisible = cfg.getBoolean("bossSpawning." + mobTypeString + ".nameVisible");
            var babyMob = cfg.getBoolean("bossSpawning." + mobTypeString + ".babyMob");
            var spawnChance = cfg.getDouble("bossSpawning." + mobTypeString + ".spawnChance");
            var maxHealth = cfg.getDouble("bossSpawning." + mobTypeString + ".maxHealth");
            var attackDamage = cfg.getDouble("bossSpawning." + mobTypeString + ".attackDamage");
            var movementSpeed = cfg.getDouble("bossSpawning." + mobTypeString + ".movementSpeed");
            var bonusArmor = cfg.getDouble("bossSpawning." + mobTypeString + ".bonusArmor");
            var followRange = cfg.getInt("bossSpawning." + mobTypeString + ".followRange");
            var droppedXPOnDeath = cfg.getInt("bossSpawning." + mobTypeString + ".droppedXPOnDeath");

            if(displayName == null) {
                logger.severe("[" + season + "] Invalid bossSpawning displayname!" +
                        "\nMobtype: " + mobTypeString);
                continue;
            }

            if(checkMMFormat(displayName) != null) {
                logger.severe("[" + season + "] Invalid bossSpawning displayname format!" +
                        "\nMobtype: " + mobTypeString +
                        "\nError: " + checkMMFormat(displayName));
                continue;
            }

            if(spawnChance > 100 || spawnChance < 0) {
                logger.severe("[" + season + "] Invalid bossSpawning spawnChance!" +
                        "\nMobtype: " + mobTypeString);
                continue;
            }

            if(maxHealth <= 0) {
                logger.severe("[" + season + "] Invalid bossSpawning maxHealth!" +
                        "\nMobtype: " + mobTypeString);
                continue;
            }

            if(attackDamage <= 0) {
                logger.severe("[" + season + "] Invalid bossSpawning attackDamage!" +
                        "\nMobtype: " + mobTypeString);
                continue;
            }

            if(movementSpeed <= 0) {
                logger.severe("[" + season + "] Invalid bossSpawning movementSpeed!" +
                        "\nMobtype: " + mobTypeString);
                continue;
            }

            if(bonusArmor < 0) {
                logger.severe("[" + season + "] Invalid bossSpawning bonusArmor!" +
                        "\nMobtype: " + mobTypeString);
                continue;
            }

            if(followRange <= 0) {
                logger.severe("[" + season + "] Invalid bossSpawning followRange!" +
                        "\nMobtype: " + mobTypeString);
                continue;
            }

            if(droppedXPOnDeath < 0) {
                logger.severe("[" + season + "] Invalid bossSpawning droppedXPOnDeath!" +
                        "\nMobtype: " + mobTypeString);
                continue;
            }

            var soundsSection = cfg.getConfigurationSection("bossSpawning." + mobTypeString + ".sounds");

            if(soundsSection == null) {
                logger.severe("[" + season + "] Invalid bossSpawning Sounds section for " + mobTypeString);
                continue;
            }

            var soundsKeys = soundsSection.getKeys(false);
            boolean invalidSound = false;

            for(var soundString : soundsKeys) {
                if(cfg.getDouble("bossSpawning." + mobTypeString + ".sounds." + soundString + ".volume") < 0) {
                    logger.severe("[" + season + "] Invalid bossSpawning sound volume!" +
                            "\nMobtype: " + mobTypeString +
                            "\nSound: " + soundString);

                    invalidSound = true;
                }

                if(cfg.getDouble("bossSpawning." + mobTypeString + ".sounds." + soundString + ".pitch") < 0 || cfg.getDouble("bossSpawning." + mobTypeString + ".sounds." + soundString + ".pitch") > 2) {
                    logger.severe("[" + season + "] Invalid bossSpawning sound pitch!" +
                            "\nMobtype: " + mobTypeString +
                            "\nSound: " + soundString);

                    invalidSound = true;
                }
            }

            if(invalidSound)
                continue;

            var spawnSoundType = cfg.getString("bossSpawning." + mobTypeString + ".sounds.spawn.sound").toLowerCase();
            var spawnSoundVolume = cfg.getDouble("bossSpawning." + mobTypeString + ".sounds.spawn.volume");
            var spawnSoundPitch = cfg.getDouble("bossSpawning." + mobTypeString + ".sounds.spawn.pitch");
            var ambientSoundType = cfg.getString("bossSpawning." + mobTypeString + ".sounds.ambient.sound").toLowerCase();
            var ambientSoundVolume = cfg.getDouble("bossSpawning." + mobTypeString + ".sounds.ambient.volume");
            var ambientSoundPitch = cfg.getDouble("bossSpawning." + mobTypeString + ".sounds.ambient.pitch");
            var deathSoundType = cfg.getString("bossSpawning." + mobTypeString + ".sounds.death.sound").toLowerCase();
            var deathSoundVolume = cfg.getDouble("bossSpawning." + mobTypeString + ".sounds.death.volume");
            var deathSoundPitch = cfg.getDouble("bossSpawning." + mobTypeString + ".sounds.death.pitch");
            var takeDamageSoundType = cfg.getString("bossSpawning." + mobTypeString + ".sounds.takeDamage.sound").toLowerCase();
            var takeDamageSoundVolume = cfg.getDouble("bossSpawning." + mobTypeString + ".sounds.takeDamage.volume");
            var takeDamageSoundPitch = cfg.getDouble("bossSpawning." + mobTypeString + ".sounds.takeDamage.pitch");
            var dealDamageSoundType = cfg.getString("bossSpawning." + mobTypeString + ".sounds.dealDamage.sound").toLowerCase();
            var dealDamageSoundVolume = cfg.getDouble("bossSpawning." + mobTypeString + ".sounds.dealDamage.volume");
            var dealDamageSoundPitch = cfg.getDouble("bossSpawning." + mobTypeString + ".sounds.dealDamage.pitch");

            net.kyori.adventure.sound.Sound spawnSound = net.kyori.adventure.sound.Sound.sound()
                    .type(NamespacedKey.minecraft(spawnSoundType))
                    .volume((float) spawnSoundVolume)
                    .pitch((float) spawnSoundPitch)
                    .source(net.kyori.adventure.sound.Sound.Source.HOSTILE)
                    .build();
            net.kyori.adventure.sound.Sound ambientSound = net.kyori.adventure.sound.Sound.sound()
                    .type(NamespacedKey.minecraft(ambientSoundType))
                    .volume((float) ambientSoundVolume)
                    .pitch((float) ambientSoundPitch)
                    .source(net.kyori.adventure.sound.Sound.Source.HOSTILE)
                    .build();
            net.kyori.adventure.sound.Sound deathSound = net.kyori.adventure.sound.Sound.sound()
                    .type(NamespacedKey.minecraft(deathSoundType))
                    .volume((float) deathSoundVolume)
                    .pitch((float) deathSoundPitch)
                    .source(net.kyori.adventure.sound.Sound.Source.HOSTILE)
                    .build();
            net.kyori.adventure.sound.Sound takeDamageSound = net.kyori.adventure.sound.Sound.sound()
                    .type(NamespacedKey.minecraft(takeDamageSoundType))
                    .volume((float) takeDamageSoundVolume)
                    .pitch((float) takeDamageSoundPitch)
                    .source(net.kyori.adventure.sound.Sound.Source.HOSTILE)
                    .build();
            net.kyori.adventure.sound.Sound dealDamageSound = net.kyori.adventure.sound.Sound.sound()
                    .type(NamespacedKey.minecraft(dealDamageSoundType))
                    .volume((float) dealDamageSoundVolume)
                    .pitch((float) dealDamageSoundPitch)
                    .source(net.kyori.adventure.sound.Sound.Source.HOSTILE)
                    .build();

            var commandsSection = cfg.getConfigurationSection("bossSpawning." + mobTypeString + ".executeCommandsOnDeath");

            if(commandsSection == null) {
                logger.severe("[" + season + "] Invalid BossSpawning executeCommandsOnDeath section for " + mobTypeString);
                continue;
            }

            var executeCommandsOnDeathEnabled = cfg.getBoolean("bossSpawning." + mobTypeString + ".executeCommandsOnDeath.enabled");
            var commandList = cfg.getStringList("bossSpawning." + mobTypeString + ".executeCommandsOnDeath.commands");
            var lootDrops = getBossSpawningLootDrops(season, mobTypeString);

            if(cfg.getConfigurationSection("bossSpawning." + mobTypeString + ".potionEffects") == null) {
                logger.severe("[" + season + "] Invalid BossSpawning potionEffects section for " + mobTypeString);
                continue;
            }

            List<PotionEffect> spawnPotionEffects = new ArrayList<>();
            var spawnPotionEffectTypes = cfg.getConfigurationSection("bossSpawning." + mobTypeString + ".potionEffects.spawn").getKeys(false);

            for(var effectString : spawnPotionEffectTypes) {
                PotionEffectType potionEffectType = PotionEffectType.getByName(effectString);
                int level = cfg.getInt("bossSpawning." + mobTypeString + ".potionEffects.spawn." + effectString + ".level");
                int time = cfg.getInt("bossSpawning." + mobTypeString + ".potionEffects.spawn." + effectString + ".time");

                if(potionEffectType == null) {
                    logger.severe("[" + season + "] Invalid BossSpawning potionEffects PotionEffectType '" + effectString + "' for " + mobTypeString);
                    continue;
                }

                if(level < 1) {
                    logger.severe("[" + season + "] Invalid bossSpawning potionEffects level!" +
                            "\nPotion: " + effectString +
                            "\nMobtype: " + mobTypeString);
                    continue;
                }

                if(time < 1) {
                    logger.severe("[" + season + "] Invalid bossSpawning potionEffects time!" +
                            "\nPotion: " + effectString +
                            "\nMobtype: " + mobTypeString);
                    continue;
                }

                spawnPotionEffects.add(new PotionEffect(potionEffectType, level, time*20));
            }

            int cycleTime = cfg.getInt("bossSpawning." + mobTypeString + ".potionEffects.cycle.time");
            List<PotionEffect> cyclePotionEffects = new ArrayList<>();

            if(cycleTime < 1) {
                logger.severe("[" + season + "] Invalid bossSpawning potionEffects cycle time!" +
                        "\nMobtype: " + mobTypeString);
                continue;
            }

            var cyclePotionEffectTypes = cfg.getConfigurationSection("bossSpawning." + mobTypeString + ".potionEffects.cycle.effects").getKeys(false);

            for(var effectString : cyclePotionEffectTypes) {
                PotionEffectType potionEffectType = PotionEffectType.getByName(effectString);
                int level = cfg.getInt("bossSpawning." + mobTypeString + ".potionEffects.cycle.effects." + effectString + ".level");
                int time = cfg.getInt("bossSpawning." + mobTypeString + ".potionEffects..cycle.effects." + effectString + ".time");

                if(potionEffectType == null) {
                    logger.severe("[" + season + "] Invalid BossSpawning potionEffects cycle PotionEffectType '" + effectString + "' for " + mobTypeString);
                    continue;
                }

                if(level < 1) {
                    logger.severe("[" + season + "] Invalid bossSpawning potionEffects cycle level!" +
                            "\nPotion: " + effectString +
                            "\nMobtype: " + mobTypeString);
                    continue;
                }

                if(time < 1) {
                    logger.severe("[" + season + "] Invalid bossSpawning potionEffects cycle time!" +
                            "\nPotion: " + effectString +
                            "\nMobtype: " + mobTypeString);
                    continue;
                }

                cyclePotionEffects.add(new PotionEffect(potionEffectType, level, time*20));
            }

            if(cfg.getConfigurationSection("bossSpawning." + mobTypeString + ".summoning") == null) {
                cfg.set("bossSpawning." + mobTypeString + ".summoning.enabled", true);
                cfg.set("bossSpawning." + mobTypeString + ".summoning.radius", 5);
                cfg.set("bossSpawning." + mobTypeString + ".summoning.cycleTime", 15);
                cfg.set("bossSpawning." + mobTypeString + ".summoning.minSpawnCount", 1);
                cfg.set("bossSpawning." + mobTypeString + ".summoning.maxSpawnCount", 4);
                cfg.set("bossSpawning." + mobTypeString + ".summoning.mobs", List.of(EntityType.ZOMBIE.name(), EntityType.SKELETON.name()));
                cfg.setInlineComments("bossSpawning.ZOMBIE.summoning.enabled", List.of("Should this feature be enabled?"));
                cfg.setInlineComments("bossSpawning.ZOMBIE.summoning.radius", List.of("Spawnradius in blocks"));
                cfg.setInlineComments("bossSpawning.ZOMBIE.summoning.cycleTime", List.of("The repeating time in seconds"));
                cfg.setInlineComments("bossSpawning.ZOMBIE.summoning.minSpawnCount", List.of("Minimum spawn count of mobs"));
                cfg.setInlineComments("bossSpawning.ZOMBIE.summoning.maxSpawnCount", List.of("Maximum spawn count of mobs"));
                cfg.setInlineComments("bossSpawning.ZOMBIE.summoning.mobs", List.of("Mobs that should spawn -> random mobs from the list will spawn!"));

                save();
            }

            boolean summoningEnabled = cfg.getBoolean("bossSpawning." + mobTypeString + ".summoning.enabled");
            int summoningRadius = cfg.getInt("bossSpawning." + mobTypeString + ".summoning.radius");
            int summoningCycleTime = cfg.getInt("bossSpawning." + mobTypeString + ".summoning.cycleTime");
            int summoningMinSpawnCount = cfg.getInt("bossSpawning." + mobTypeString + ".summoning.minSpawnCount");
            int summoningMaxSpawnCount = cfg.getInt("bossSpawning." + mobTypeString + ".summoning.maxSpawnCount");
            List<EntityType> summoningMobs = new ArrayList<>();

            if(summoningRadius <= 0) {
                logger.severe("[" + season + "] Invalid bossSpawning summoning radius!" +
                        "\nSeason: " + season);
                continue;
            }

            if(summoningCycleTime <= 0) {
                logger.severe("[" + season + "] Invalid bossSpawning summoning cycleTime!" +
                        "\nSeason: " + season);
                continue;
            }

            if(summoningMinSpawnCount < 0) {
                logger.severe("[" + season + "] Invalid bossSpawning summoning minSpawnCount!" +
                        "\nSeason: " + season);
                continue;
            }

            if(summoningMaxSpawnCount < 0) {
                logger.severe("[" + season + "] Invalid bossSpawning summoning maxSpawnCount!" +
                        "\nSeason: " + season);
                continue;
            }

            var tempSummoningMobs = cfg.getStringList("bossSpawning." + mobTypeString + ".summoning.mobs");

            for(var summoningMobType : tempSummoningMobs) {
                try {
                    summoningMobs.add(EntityType.valueOf(summoningMobType.toUpperCase()));
                } catch (Exception e) {
                    logger.severe("[" + season + "] Invalid bossSpawning summoning Mob type!" +
                            "\nSeason: " + season +
                            "\nMobtype: " + summoningMobType);
                }
            }

            BossEntity bossEntity = new BossEntity(
                    EntityType.valueOf(mobTypeString),
                    displayName,
                    nameVisible,
                    babyMob,
                    spawnChance,
                    maxHealth,
                    attackDamage,
                    movementSpeed,
                    bonusArmor,
                    followRange,
                    droppedXPOnDeath,
                    spawnSound,
                    ambientSound,
                    deathSound,
                    takeDamageSound,
                    dealDamageSound,
                    executeCommandsOnDeathEnabled,
                    commandList,
                    lootDrops,
                    spawnPotionEffects,
                    cycleTime,
                    cyclePotionEffects,
                    summoningEnabled,
                    summoningRadius,
                    summoningCycleTime,
                    summoningMinSpawnCount,
                    summoningMaxSpawnCount,
                    summoningMobs
            );

            bossList.add(bossEntity);
        }

        return bossList;
    }

    private Map<ItemStack, Double> getBossSpawningLootDrops(String season, String mobType) {
        var cfg = getCfgFromSeason(season);
        Map<ItemStack, Double> lootDrops = new HashMap<>();

        if(cfg.getConfigurationSection("bossSpawning." + mobType + ".lootOnDeath") == null) {
            logger.severe("[" + season + "] Invalid bossSpawning lootOnDeath section");
            return lootDrops;
        }

        var items = cfg.getConfigurationSection("bossSpawning." + mobType + ".lootOnDeath").getKeys(false);

        for(var item : items) {
            var displayname = cfg.getString("bossSpawning." + mobType + ".lootOnDeath." + item + ".displayname");
            var lore = cfg.getStringList("bossSpawning." + mobType + ".lootOnDeath." + item + ".lore");
            var material = cfg.getString("bossSpawning." + mobType + ".lootOnDeath." + item + ".material");
            var amount = cfg.getInt("bossSpawning." + mobType + ".lootOnDeath." + item + ".amount");
            var dropChance = cfg.getDouble("bossSpawning." + mobType + ".lootOnDeath." + item + ".dropChance");
            var enchantments = new HashMap<Enchantment, Integer>();

            if (cfg.getConfigurationSection("bossSpawning." + mobType + ".lootOnDeath." + item + ".enchantments") == null) {
                logger.severe("[" + season + "] Invalid bossSpawning lootOnDeath enchantments section\n" +
                        "Item: " + item);
                continue;
            }

            cfg.getConfigurationSection("bossSpawning." + mobType + ".lootOnDeath." + item + ".enchantments").getKeys(false).forEach(enchantmentName -> {
                var level = cfg.getInt("bossSpawning." + mobType + ".lootOnDeath." + item + ".enchantments." + enchantmentName);
                var enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchantmentName));

                if (enchantment != null) {
                    if (level != 0) {
                        enchantments.put(enchantment, level);
                    } else
                        logger.severe("[" + season + "] Invalid bossSpawning lootOnDeath enchantment level '" + level + "'\n" +
                                "Item: " + item);
                } else
                    logger.severe("[" + season + "] Invalid bossSpawning lootOnDeath enchantment '" + enchantmentName + "'\n" +
                            "Item: " + item);
            });

            if (displayname == null) {
                logger.severe("[" + season + "] Invalid bossSpawning lootOnDeath displayname!\n" +
                        "Item: " + item);
                continue;
            }

            try {
                mm.deserialize(displayname);
            } catch (Exception e) {
                logger.severe("[" + season + "] Invalid bossSpawning lootOnDeath displayname format!\n" +
                        "Item: " + item + "\n" +
                        "Error: " + e.getMessage());
                continue;
            }

            if (!lore.isEmpty()) {
                lore.forEach(line -> {
                    try {
                        mm.deserialize(line);
                    } catch (Exception e) {
                        logger.severe("[" + season + "] Invalid bossSpawning lootOnDeath lore line '" + line + "'\n" +
                                "Item: " + item + "\n" +
                                "Error: " + e.getMessage());
                        lore.remove(line);
                    }
                });
            }

            if (material == null) {
                logger.severe("[" + season + "] Invalid bossSpawning lootOnDeath material!\n" +
                        "Item: " + item);
                continue;
            }

            if (Material.getMaterial(material) == null) {
                logger.severe("[" + season + "] Invalid bossSpawning lootOnDeath material name '" + material + "'\n" +
                        "Item: " + item + "\n");
                continue;
            }

            if (amount < 0) {
                logger.severe("[" + season + "] Invalid bossSpawning lootOnDeath amount '" + amount + "'\n" +
                        "Item: " + item + "\n");
                continue;
            }

            if (dropChance < 0) {
                logger.severe("[" + season + "] Invalid bossSpawning lootOnDeath dropChance '" + dropChance + "'\n" +
                        "Item: " + item + "\n");
                continue;
            }

            lootDrops.put(new ItemBuilder(Material.getMaterial(material))
                    .setDisplayName(mm.deserialize(displayname))
                    .setLore(lore.toArray(new String[0]))
                    .setAmount(amount)
                    .addEnchantments(enchantments)
                    .build(), dropChance);
        }

        return lootDrops;
    }

    public int getXPBonus(String season) {
        var cfg = getCfgFromSeason(season);
        int xpBonus = cfg.getInt("xpBonus");

        if(xpBonus < 0) {
            logger.severe("[" + season + "] Invalid xpBonus");
            return 0;
        }

        return xpBonus;
    }

    public Particles getParticles(String season) {
        var cfg = getCfgFromSeason(season);

        if(!cfg.isSet("particles")) {
            cfg.set("particles.enabled", true);
            cfg.set("particles.offsetX", 5);
            cfg.set("particles.offsetY", 5);
            cfg.set("particles.offsetZ", 5);
            cfg.set("particles.spawnTime", 5);
            cfg.set("particles.speed", 0.0);
            cfg.set("particles.particle.SNOWFLAKE.minSpawnAmount", 10);
            cfg.set("particles.particle.SNOWFLAKE.maxSpawnAmount", 40);
            cfg.setInlineComments("particles.enabled", List.of("Should this feature be enabled?"));
            cfg.setInlineComments("particles.offsetX", List.of("spread the spawned particle"));
            cfg.setInlineComments("particles.offsetY", List.of("spread the spawned particle"));
            cfg.setInlineComments("particles.offsetZ", List.of("spread the spawned particle"));
            cfg.setInlineComments("particles.spawnTime", List.of("Repeating time in ticks (20 ticks = 1 second)"));
            cfg.setInlineComments("particles.speed", List.of("Speed of the particles"));
            cfg.setInlineComments("particles.particle.SNOWFLAKE.minSpawnAmount", List.of("Minimum spawn amount of the particle"));
            cfg.setInlineComments("particles.particle.SNOWFLAKE.maxSpawnAmount", List.of("Maximum spawn amount of the particle"));

            save();
        }

        var particlesEnabled = cfg.getBoolean("particles.enabled");
        var offsetX = cfg.getInt("particles.offsetX");
        var offsetY = cfg.getInt("particles.offsetY");
        var offsetZ = cfg.getInt("particles.offsetZ");
        var spawnTime = cfg.getInt("particles.spawnTime");
        var speed = cfg.getDouble("particles.speed");
        Map<Particle, Integer[]> particleMap = new HashMap<>();

        if(offsetX < 0 || offsetY < 0 || offsetZ < 0) {
            logger.severe("[" + season + "] particle offset cant be below 0!");
            return null;
        }

        if(spawnTime < 0) {
            logger.severe("[" + season + "] particle spawnTime cant be below 0!");
            return null;
        }

        var particleKeys = cfg.getConfigurationSection("particles.particle").getKeys(false);

        for(var particleString : particleKeys) {
            Particle particle;
            Integer[] spawnAmounts = new Integer[2];

            try {
                particle = Particle.valueOf(particleString);
            } catch (IllegalArgumentException e) {
                logger.severe("[" + season + "] Invalid particle type!");
                continue;
            }

            try {
                spawnAmounts[0] = Integer.parseInt(cfg.getString("particles.particle." + particleString + ".minSpawnAmount"));
                spawnAmounts[1] = Integer.parseInt(cfg.getString("particles.particle." + particleString + ".maxSpawnAmount"));
            } catch (NumberFormatException e) {
                logger.severe("[" + season + "] Invalid particle spawnAmount for " + particleString);
                continue;
            }

            particleMap.put(particle, spawnAmounts);
        }

        return new Particles(
                particlesEnabled,
                offsetX,
                offsetY,
                offsetZ,
                spawnTime,
                speed,
                particleMap);
    }

    public boolean isFeatureEnabled(String season, String feature) {
        var cfg = getCfgFromSeason(season);

        if(!cfg.isSet(feature)) {
            logger.severe("[" + season + "] " + feature + " section is missing!");
            return false;
        }

        if(!cfg.isSet(feature + ".enabled")) {
            cfg.set(feature + ".enabled", true);
            save();
            return true;
        }

        return cfg.getBoolean(feature + ".enabled");
    }

    private String checkMMFormat(String mmString) {
        try {
            mm.deserialize(mmString);

            return null;
        } catch (Exception e) {
            return e.getMessage();
        }
    }
}
