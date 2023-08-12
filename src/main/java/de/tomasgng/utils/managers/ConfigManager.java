package de.tomasgng.utils.managers;

import de.tomasgng.DynamicSeasons;
import de.tomasgng.utils.enums.SeasonType;
import de.tomasgng.utils.enums.WeatherType;
import de.tomasgng.utils.template.BossEntity;
import de.tomasgng.utils.template.ItemBuilder;
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
import java.util.*;
import java.util.logging.Logger;

public class ConfigManager {

    private final File folder = new File("plugins/DynamicSeasons");
    private final File config = new File("plugins/DynamicSeasons/config.yml");
    private YamlConfiguration cfg = YamlConfiguration.loadConfiguration(config);
    private Connection connection;
    private final String sqlSelectAllData = "SELECT * FROM data";
    private final String sqlUpdateTime = "UPDATE data SET time=?";
    private final Logger logger = DynamicSeasons.getInstance().getLogger();
    private final MiniMessage mm = MiniMessage.miniMessage();

    public ConfigManager() {
        createFiles();
        fixMissingSections();
        createConnection();
        setupDatabase();
    }

    //region(SQLite)
    @SneakyThrows
    private void createConnection() {
        new File("plugins/DynamicSeasons").mkdirs();
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

            cfg.set("CONFIG_VERSION", DynamicSeasons.getInstance().getDescription().getVersion());
            cfg.set("season_duration", 300);
            cfg.set("worlds", List.of("world"));
            cfg.set("placeholders.duration.placeholderName", "duration");
            cfg.set("placeholders.duration.format", "HH:mm:ss");
            cfg.set("placeholders.currentSeason.placeholderName", "currentSeason");
            cfg.set("placeholders.currentSeason.text.spring", "Spring");
            cfg.set("placeholders.currentSeason.text.summer", "Summer");
            cfg.set("placeholders.currentSeason.text.fall", "Fall");
            cfg.set("placeholders.currentSeason.text.winter", "Winter");
            cfg.set("updater", true);
            cfg.setComments("CONFIG_VERSION", List.of("DONT CHANGE THIS! Simply ignore it :)"));
            cfg.setComments("updater", List.of("Should this plugin update itself if a new version was released?"));
            cfg.setComments("placeholders.duration.format", List.of("Use your own date format. For help use this site: https://help.gooddata.com/cloudconnect/manual/date-and-time-format.html#:~:text=Table%C2%A028.5.%C2%A0Date%20and%20Time%20Format%20Patterns%20and%20Results%20(Java)"));
            cfg.setComments("worlds", List.of("Specify the worlds where the seasons should work."));
            cfg.setComments("season_duration", List.of("Specify the duration of the seasons in Seconds", "e.g. for one hour -> season_duration: 3600 "));
            cfg.setComments("placeholders.currentSeason.text", List.of("Set the replacement for these seasons."));

            cfg.set("spring.weather.enabled", true);
            cfg.set("spring.weather.type.clear", true);
            cfg.set("spring.weather.type.storm", true);
            cfg.set("spring.weather.type.thunder", false);
            cfg.set("spring.randomTickSpeed", 4);
            cfg.set("spring.animalSpawning.SHEEP", 80);
            cfg.set("spring.animalSpawning.CHICKEN", 40);
            cfg.set("spring.mobMovement.ZOMBIE", 0.3);
            cfg.set("spring.mobMovement.SPIDER", 0.4);
            cfg.set("spring.animalGrowing.COW", 6000);
            cfg.set("spring.animalGrowing.SHEEP", 3600);
            cfg.set("spring.mobBonusArmor.ZOMBIE", 2.5);
            cfg.set("spring.mobBonusArmor.CREEPER", 1.0);
            cfg.set("spring.mobMaxHealth.CREEPER", 25.0);
            cfg.set("spring.mobMaxHealth.ZOMBIE", 30.0);
            cfg.set("spring.mobAttackDamage.ZOMBIE", 4.0);
            cfg.set("spring.mobAttackDamage.SPIDER", 3.0);
            cfg.set("spring.preventCropGrowing", List.of("POTATOES", "CARROTS"));
            cfg.set("spring.potionEffects.SPEED", 1);
            cfg.set("spring.potionEffects.REGENERATION", 1);
            cfg.set("spring.lootDrops.ZOMBIE.1.displayname", "<yellow>Mysterious Sword");
            cfg.set("spring.lootDrops.ZOMBIE.1.lore", List.of("<gray>This sword is", "<gray>veeery mysterious!"));
            cfg.set("spring.lootDrops.ZOMBIE.1.material", Material.DIAMOND_SWORD.name());
            cfg.set("spring.lootDrops.ZOMBIE.1.amount", 1);
            cfg.set("spring.lootDrops.ZOMBIE.1.dropChance", 10);
            cfg.set("spring.lootDrops.ZOMBIE.1.enchantments.sharpness", 2);
            cfg.set("spring.bossSpawning.ZOMBIE.displayName", "<yellow>BOSS <dark_gray>| <green>%hp%<dark_gray>/<yellow>%maxHP% <dark_red>❤");
            cfg.set("spring.bossSpawning.ZOMBIE.nameVisible", false);
            cfg.set("spring.bossSpawning.ZOMBIE.spawnChance", 0.5);
            cfg.set("spring.bossSpawning.ZOMBIE.maxHealth", 60.0);
            cfg.set("spring.bossSpawning.ZOMBIE.attackDamage", 5.0);
            cfg.set("spring.bossSpawning.ZOMBIE.movementSpeed", 0.3);
            cfg.set("spring.bossSpawning.ZOMBIE.bonusArmor", 20.0);
            cfg.set("spring.bossSpawning.ZOMBIE.followRange", 20);
            cfg.set("spring.bossSpawning.ZOMBIE.droppedXPOnDeath", 400);
            cfg.set("spring.bossSpawning.ZOMBIE.sounds.spawn.sound", Sound.ENTITY_ENDER_DRAGON_GROWL.key().value());
            cfg.set("spring.bossSpawning.ZOMBIE.sounds.spawn.volume", 1.0F);
            cfg.set("spring.bossSpawning.ZOMBIE.sounds.spawn.pitch", 0.5F);
            cfg.set("spring.bossSpawning.ZOMBIE.sounds.ambient.sound", Sound.ENTITY_ENDER_DRAGON_AMBIENT.key().value());
            cfg.set("spring.bossSpawning.ZOMBIE.sounds.ambient.volume", 1.0F);
            cfg.set("spring.bossSpawning.ZOMBIE.sounds.ambient.pitch", 0.5F);
            cfg.set("spring.bossSpawning.ZOMBIE.sounds.death.sound", Sound.ENTITY_ENDER_DRAGON_DEATH.key().value());
            cfg.set("spring.bossSpawning.ZOMBIE.sounds.death.volume", 1.0F);
            cfg.set("spring.bossSpawning.ZOMBIE.sounds.death.pitch", 0.5F);
            cfg.set("spring.bossSpawning.ZOMBIE.sounds.takeDamage.sound", Sound.ENTITY_ENDER_DRAGON_HURT.key().value());
            cfg.set("spring.bossSpawning.ZOMBIE.sounds.takeDamage.volume", 1.0F);
            cfg.set("spring.bossSpawning.ZOMBIE.sounds.takeDamage.pitch", 0.5F);
            cfg.set("spring.bossSpawning.ZOMBIE.sounds.dealDamage.sound", Sound.ENTITY_ENDER_DRAGON_SHOOT.key().value());
            cfg.set("spring.bossSpawning.ZOMBIE.sounds.dealDamage.volume", 1.0F);
            cfg.set("spring.bossSpawning.ZOMBIE.sounds.dealDamage.pitch", 0.5F);
            cfg.set("spring.bossSpawning.ZOMBIE.executeCommandsOnDeath.enabled", false);
            cfg.set("spring.bossSpawning.ZOMBIE.executeCommandsOnDeath.commands", List.of("bal add %player% 100"));
            cfg.set("spring.bossSpawning.ZOMBIE.lootOnDeath.1.displayname", "<gold>Legendary Sword");
            cfg.set("spring.bossSpawning.ZOMBIE.lootOnDeath.1.lore", List.of("<gray>This sword is", "<gold>LEGENDARY!"));
            cfg.set("spring.bossSpawning.ZOMBIE.lootOnDeath.1.material", Material.DIAMOND_SWORD.name());
            cfg.set("spring.bossSpawning.ZOMBIE.lootOnDeath.1.amount", 1);
            cfg.set("spring.bossSpawning.ZOMBIE.lootOnDeath.1.dropChance", 10);
            cfg.set("spring.bossSpawning.ZOMBIE.lootOnDeath.1.enchantments.sharpness", 6);
            cfg.set("spring.xpBonus", 20);
            cfg.setComments("spring.bossSpawning.ZOMBIE", List.of("Mobtype"));
            cfg.setComments("spring.bossSpawning.ZOMBIE.displayName", List.of("Displayname of the boss"));
            cfg.setComments("spring.bossSpawning.ZOMBIE.nameVisible", List.of("Should the displayname be visible through walls etc?"));
            cfg.setComments("spring.bossSpawning.ZOMBIE.spawnChance", List.of("The chance of the boss to spawn"));
            cfg.setComments("spring.bossSpawning.ZOMBIE.maxHealth", List.of("The max health of the boss"));
            cfg.setComments("spring.bossSpawning.ZOMBIE.attackDamage", List.of("The attack damage of the boss"));
            cfg.setComments("spring.bossSpawning.ZOMBIE.movementSpeed", List.of("The movementspeed of the boss"));
            cfg.setComments("spring.bossSpawning.ZOMBIE.bonusArmor", List.of("The bonus armor of the boss"));
            cfg.setComments("spring.bossSpawning.ZOMBIE.followRange", List.of("The range of blocks where the boss will follow players"));
            cfg.setComments("spring.bossSpawning.ZOMBIE.droppedXPOnDeath", List.of("The dropped xp when the boss dies"));
            cfg.setComments("spring.bossSpawning.ZOMBIE.sounds", List.of("Customize the sounds that the boss will make"));
            cfg.setComments("spring.bossSpawning.ZOMBIE.sounds.spawn", List.of("The sound when the boss spawns"));
            cfg.setComments("spring.bossSpawning.ZOMBIE.sounds.ambient", List.of("The sound when the boss is alive"));
            cfg.setComments("spring.bossSpawning.ZOMBIE.sounds.death", List.of("The sound when the boss dies"));
            cfg.setComments("spring.bossSpawning.ZOMBIE.sounds.takeDamage", List.of("The sound when the boss takes damage"));
            cfg.setComments("spring.bossSpawning.ZOMBIE.sounds.dealDamage", List.of("The sound when the boss deals damage"));
            cfg.setComments("spring.bossSpawning.ZOMBIE.executeCommandsOnDeath", List.of("Run commands when a player kills the boss", "%player% = killer"));
            cfg.setComments("spring.bossSpawning.ZOMBIE.executeCommandsOnDeath.enabled", List.of("Should this feature be enabled?"));
            cfg.setComments("spring.bossSpawning.ZOMBIE.lootOnDeath", List.of("The loot of the boss when he dies", "This section is similar to the 'lootDrops' section"));
            cfg.setComments("spring.lootDrops", List.of("Here you can customize the custom loot from mobs."));
            cfg.setComments("spring.lootDrops.ZOMBIE", List.of("Name of the mob"));
            cfg.setComments("spring.lootDrops.ZOMBIE.1", List.of("You can name this whatever you like as this isnt that important :)"));
            cfg.setComments("spring.lootDrops.ZOMBIE.1.displayname", List.of("Displayname of the item in MiniMessage format. "));
            cfg.setComments("spring.lootDrops.ZOMBIE.1.lore", List.of("Item Lore in MiniMessage format."));
            cfg.setComments("spring.lootDrops.ZOMBIE.1.material", List.of("Material of the item"));
            cfg.setComments("spring.lootDrops.ZOMBIE.1.amount", List.of("The amount of the item"));
            cfg.setComments("spring.lootDrops.ZOMBIE.1.dropChance", List.of("The chance of this item to drop (0-100) in percent"));
            cfg.setComments("spring.lootDrops.ZOMBIE.1.enchantments", List.of("List of all Enchantments: https://pastebin.com/raw/hyRbnm2q", "Format -> (enchantment): (level)"));
            cfg.setComments("spring.potionEffects", List.of("Customize the potion effects for players", "List of all potion effects: https://pastebin.com/raw/KPh96Mf9"));
            cfg.setComments("spring.preventCropGrowing", List.of("Customize the crops that are not allowed to grow", "List of all crops: https://minecraft.fandom.com/wiki/Crops"));
            cfg.setComments("spring.mobAttackDamage", List.of("Customize the attack damage for mobs", "List of all mobs and their attack damage: https://pastebin.com/raw/XnC3kNXi"));
            cfg.setComments("spring.mobBonusArmor", List.of("Customize the bonus armor for mobs", "2 equals 1 Armor-slot"));
            cfg.setComments("spring.mobMaxHealth", List.of("Customize the max health for mobs", "2 equals 1 heart | MAX is 20", "List of all mobs and their max health: https://pastebin.com/raw/5upq7HVr"));
            cfg.setComments("spring.animalGrowing", List.of("Customize the speed of animal growing", "Most baby mobs take 20 mins (24000 ticks) to grow up", "Here is a list of all breedable animals: https://pastebin.com/raw/zzUAc3XM", "Here is a tick calculator: https://mapmaking.fr/tick/", "IMPORTANT: 20 ticks = 1 second", "Format -> (ANIMAL_NAME): (TIME IN TICKS)"));
            cfg.setComments("spring.mobMovement", List.of("Customize the movement speed of mobs", "Here is a list of all mobs and their default movement speed: https://pastebin.com/raw/2WaGi20Z", "Format -> (MOB_NAME): (SPEED)"));
            cfg.setComments("spring.animalSpawning", List.of("The probability of an animal to spawn. 1-100%", "Here is a list of all animals: https://pastebin.com/raw/Tf3mMGg6", "Format -> (MOB_NAME): (PERCENT)"));
            cfg.setComments("spring.randomTickSpeed", List.of("The growth speed of plants etc. default value -> 3.", "higher -> faster | large values can cause server lag!", "Heres a list what will be effected by the change: https://minecraft.fandom.com/wiki/Tick#:~:text=Most%20blocks%20ignore%20this%20tick%2C%20but%20some%20use%20it%20to%20do%20something%3A"));
            cfg.setComments("spring.weather", List.of("Customize the weather for the season"));
            cfg.setComments("spring.weather.type", List.of("Customize the allowed weather types"));
            cfg.setComments("spring.xpBonus", List.of("The bonus xp you get when picking up xp (in percent)", "e.g. if you set 20 then the player will get 20% more xp. (20% of the picked up xp)"));

            for(var season : List.of("summer", "fall", "winter")) {
                cfg.set(season + ".weather.enabled", true);
                cfg.set(season + ".weather.type.clear", true);
                cfg.set(season + ".weather.type.storm", true);
                cfg.set(season + ".weather.type.thunder", false);
                cfg.set(season + ".randomTickSpeed", 4);
                cfg.set(season + ".animalSpawning.SHEEP", 80);
                cfg.set(season + ".animalSpawning.CHICKEN", 40);
                cfg.set(season + ".mobMovement.ZOMBIE", 0.3);
                cfg.set(season + ".mobMovement.SPIDER", 0.4);
                cfg.set(season + ".animalGrowing.COW", 6000);
                cfg.set(season + ".animalGrowing.SHEEP", 3600);
                cfg.set(season + ".mobBonusArmor.ZOMBIE", 2.5);
                cfg.set(season + ".mobBonusArmor.CREEPER", 1.0);
                cfg.set(season + ".mobMaxHealth.CREEPER", 25.0);
                cfg.set(season + ".mobMaxHealth.ZOMBIE", 30.0);
                cfg.set(season + ".mobAttackDamage.ZOMBIE", 4.0);
                cfg.set(season + ".mobAttackDamage.SPIDER", 3.0);
                cfg.set(season + ".preventCropGrowing", List.of("POTATOES", "CARROTS"));
                cfg.set(season + ".potionEffects.SPEED", 1);
                cfg.set(season + ".potionEffects.REGENERATION", 1);
                cfg.set(season + ".lootDrops.ZOMBIE.1.displayname", "<yellow>Mysterious Sword");
                cfg.set(season + ".lootDrops.ZOMBIE.1.lore", List.of("<gray>This sword is", "<gray>veeery mysterious!"));
                cfg.set(season + ".lootDrops.ZOMBIE.1.material", Material.DIAMOND_SWORD.name());
                cfg.set(season + ".lootDrops.ZOMBIE.1.amount", 1);
                cfg.set(season + ".lootDrops.ZOMBIE.1.dropChance", 10);
                cfg.set(season + ".lootDrops.ZOMBIE.1.enchantments.sharpness", 2);
                cfg.set(season + ".bossSpawning.ZOMBIE.displayName", "<yellow>BOSS <dark_gray>| <green>%hp%<dark_gray>/<yellow>%maxHP%");
                cfg.set(season + ".bossSpawning.ZOMBIE.nameVisible", false);
                cfg.set(season + ".bossSpawning.ZOMBIE.spawnChance", 0.5);
                cfg.set(season + ".bossSpawning.ZOMBIE.maxHealth", 60.0);
                cfg.set(season + ".bossSpawning.ZOMBIE.attackDamage", 5.0);
                cfg.set(season + ".bossSpawning.ZOMBIE.movementSpeed", 0.3);
                cfg.set(season + ".bossSpawning.ZOMBIE.bonusArmor", 20.0);
                cfg.set(season + ".bossSpawning.ZOMBIE.followRange", 20);
                cfg.set(season + ".bossSpawning.ZOMBIE.droppedXPOnDeath", 400);
                cfg.set(season + ".bossSpawning.ZOMBIE.sounds.spawn.sound", Sound.ENTITY_ENDER_DRAGON_GROWL.name());
                cfg.set(season + ".bossSpawning.ZOMBIE.sounds.spawn.volume", 1.0F);
                cfg.set(season + ".bossSpawning.ZOMBIE.sounds.spawn.pitch", 0.5F);
                cfg.set(season + ".bossSpawning.ZOMBIE.sounds.ambient.sound", Sound.ENTITY_ENDER_DRAGON_AMBIENT.name());
                cfg.set(season + ".bossSpawning.ZOMBIE.sounds.ambient.volume", 1.0F);
                cfg.set(season + ".bossSpawning.ZOMBIE.sounds.ambient.pitch", 0.5F);
                cfg.set(season + ".bossSpawning.ZOMBIE.sounds.death.sound", Sound.ENTITY_ENDER_DRAGON_DEATH.name());
                cfg.set(season + ".bossSpawning.ZOMBIE.sounds.death.volume", 1.0F);
                cfg.set(season + ".bossSpawning.ZOMBIE.sounds.death.pitch", 0.5F);
                cfg.set(season + ".bossSpawning.ZOMBIE.sounds.takeDamage.sound", Sound.ENTITY_ENDER_DRAGON_HURT.name());
                cfg.set(season + ".bossSpawning.ZOMBIE.sounds.takeDamage.volume", 1.0F);
                cfg.set(season + ".bossSpawning.ZOMBIE.sounds.takeDamage.pitch", 0.5F);
                cfg.set(season + ".bossSpawning.ZOMBIE.sounds.dealDamage.sound", Sound.ENTITY_ENDER_DRAGON_SHOOT.name());
                cfg.set(season + ".bossSpawning.ZOMBIE.sounds.dealDamage.volume", 1.0F);
                cfg.set(season + ".bossSpawning.ZOMBIE.sounds.dealDamage.pitch", 0.5F);
                cfg.set(season + ".bossSpawning.ZOMBIE.executeCommandsOnDeath.enabled", false);
                cfg.set(season + ".bossSpawning.ZOMBIE.executeCommandsOnDeath.commands", List.of("bald add %player% 100"));
                cfg.set(season + ".bossSpawning.ZOMBIE.lootOnDeath.1.displayname", "<gold>Legendary Sword");
                cfg.set(season + ".bossSpawning.ZOMBIE.lootOnDeath.1.lore", List.of("<gray>This sword is", "<gold>LEGENDARY!"));
                cfg.set(season + ".bossSpawning.ZOMBIE.lootOnDeath.1.material", Material.DIAMOND_SWORD.name());
                cfg.set(season + ".bossSpawning.ZOMBIE.lootOnDeath.1.amount", 1);
                cfg.set(season + ".bossSpawning.ZOMBIE.lootOnDeath.1.dropChance", 10);
                cfg.set(season + ".bossSpawning.ZOMBIE.lootOnDeath.1.enchantments.sharpness", 6);
                cfg.set(season + ".xpBonus", 20);
            }

            save();
        }
    }

    private void fixMissingSections() {
        if(cfg.isSet("CONFIG_VERSION") && cfg.getString("CONFIG_VERSION").equalsIgnoreCase(DynamicSeasons.getInstance().getPluginMeta().getVersion()))
            return;
        Map<String, Object> oldValues = new LinkedHashMap<>();
        for(var key : cfg.getKeys(true)) {
            if(key.equalsIgnoreCase("CONFIG_VERSION"))
                continue;
            oldValues.put(key, cfg.get(key));
        }

        config.delete();
        createFiles();

        for(var entry : oldValues.entrySet()) {
            cfg.set(entry.getKey(), entry.getValue());
        }
        save();
    }

    @SneakyThrows
    private void save() {
        cfg.save(config);
        reload();
    }

    public void reload() {
        cfg = YamlConfiguration.loadConfiguration(config);
    }

    public String getDurationPlaceholderName() {
        var name = cfg.getString("placeholders.duration.placeholderName");
        if(name == null) {
            logger.severe("Invalid duration placeholderName. Using default name -> duration");
            return "duration";
        }
        return name;
    }

    public String getDurationPlacerholderRawFormat() {
        var format = cfg.getString("placeholders.duration.format");
        if(format == null) {
            return "HH:mm:ss";
        }
        return format;
    }

    public String getCurrentSeasonPlaceholderName() {
        var name = cfg.getString("placeholders.currentSeason.placeholderName");
        if(name == null) {
            logger.severe("Invalid currentSeason placeholderName. Using default name -> currentSeason");
            return "currentSeason";
        }
        return name;
    }

    public String getCurrentSeasonText(SeasonType seasonType) {
        var text = cfg.getString("placeholders.currentSeason.text."+seasonType.name().toLowerCase());
        if(text == null) {
            logger.severe("Invalid currentSeason text for " + seasonType.name().toLowerCase() + ". Using default text -> " + seasonType.name());
            return seasonType.name();
        }
        return text;
    }

    public int getDuration() {
        int duration = cfg.getInt("season_duration");
        if(duration < 10) {
            logger.severe("Invalid season_duration. Using default value -> 300!");
            return 300;
        }
        return duration;
    }

    private boolean alreadyPrintedInvalidWorlds = false;

    public List<World> getAllowedWorlds() {
        List<World> worlds = new ArrayList<>();
        var section = cfg.getStringList("worlds");
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
                logger.severe("0 worlds loaded. Disabling plugin...");
                logger.severe("Add worlds to your config.yml!");
            }
            Bukkit.getScheduler().runTask(DynamicSeasons.getInstance(), () -> Bukkit.getPluginManager().disablePlugin(DynamicSeasons.getInstance()));
        }
        alreadyPrintedInvalidWorlds = true;
        return worlds;
    }

    public boolean updaterIsActive() {
        return cfg.getBoolean("updater");
    }

    public boolean getWeather(String season) {
        return cfg.getBoolean(season + ".weather.enabled");
    }

    public List<WeatherType> getWeatherTypes(String season) {
        List<WeatherType> weatherTypes = new ArrayList<>();
        var sectionKeys = cfg.getConfigurationSection(season + ".weather.type").getKeys(false);
        for(var weatherType : sectionKeys) {
            if(cfg.getBoolean(season + ".weather.type." + weatherType))
                weatherTypes.add(WeatherType.valueOf(weatherType.toUpperCase()));
        }
        return weatherTypes;
    }

    public int randomTickSpeed(String season) {
        int randomTickSpeed = cfg.getInt(season + ".randomTickSpeed");
        if(randomTickSpeed <= 0) {
            logger.severe("Invalid randomTickSpeed for season: " + season);
            return 3;
        }
        return randomTickSpeed;
    }

    public Map<EntityType, Double> getAnimalSpawning(String season) {
        Map<EntityType, Double> animalSpawning = new HashMap<>();
        if(cfg.getConfigurationSection(season + ".animalSpawning") == null) {
            logger.severe("Invalid animalSpawning for season " + season);
            return animalSpawning;
        }
        var sectionKeys = cfg.getConfigurationSection(season + ".animalSpawning").getKeys(false);
        for (var animal : sectionKeys) {
            try {
                EntityType entityType = EntityType.valueOf(animal);
                double spawnChance = cfg.getDouble(season + ".animalSpawning." + animal);
                if(spawnChance > 100 || spawnChance < 0)
                    throw new NullPointerException();
                animalSpawning.put(entityType, spawnChance);
            } catch (Exception e) {
                logger.severe("Invalid animalSpawning '" + animal + "' for season: " + season);
            }
        }
        return animalSpawning;
    }

    public Map<EntityType, Double> getMobMovement(String season) {
        Map<EntityType, Double> mobMovement = new HashMap<>();
        if(cfg.getConfigurationSection(season + ".mobMovement") == null) {
            logger.severe("Invalid mobMovement for season " + season);
            return mobMovement;
        }
        var sectionKeys = cfg.getConfigurationSection(season + ".mobMovement").getKeys(false);
        for (var mob : sectionKeys) {
            try {
                EntityType entityType = EntityType.valueOf(mob);
                double movement = cfg.getDouble(season + ".mobMovement." + mob);
                if(movement < 0)
                    throw new NullPointerException();
                mobMovement.put(entityType, movement);
            } catch (Exception e) {
                logger.severe("Invalid mobMovement '" + mob + "' for season: " + season);
            }
        }
        return mobMovement;
    }

    public Map<EntityType, Integer> getAnimalGrowing(String season) {
        Map<EntityType, Integer> animalGrowing = new HashMap<>();
        if(cfg.getConfigurationSection(season + ".animalGrowing") == null) {
            logger.severe("Invalid animalGrowing for season " + season);
            return animalGrowing;
        }
        var sectionKeys = cfg.getConfigurationSection(season + ".animalGrowing").getKeys(false);
        for (var animal : sectionKeys) {
            try {
                EntityType entityType = EntityType.valueOf(animal);
                int growTimeInTicks = cfg.getInt(season + ".animalGrowing." + animal);
                if(growTimeInTicks < 20)
                    throw new NullPointerException();
                animalGrowing.put(entityType, growTimeInTicks);
            } catch (Exception e) {
                logger.severe("Invalid animalGrowing '" + animal + "' for season: " + season);
            }
        }
        return animalGrowing;
    }

    public Map<EntityType, Double> getMobBonusArmor(String season) {
        Map<EntityType, Double> mobBonusArmor = new HashMap<>();
        if(cfg.getConfigurationSection(season + ".mobBonusArmor") == null) {
            logger.severe("Invalid mobBonusArmor for season " + season);
            return mobBonusArmor;
        }
        var sectionKeys = cfg.getConfigurationSection(season + ".mobBonusArmor").getKeys(false);
        for (var mob : sectionKeys) {
            try {
                EntityType entityType = EntityType.valueOf(mob);
                double bonusArmor = cfg.getDouble(season + ".mobBonusArmor." + mob);
                if(bonusArmor < 0) {
                    throw new Exception();
                }
                mobBonusArmor.put(entityType, bonusArmor);
            } catch (Exception e) {
                logger.severe("Invalid mobBonusArmor" +
                        "\nSeason: " + season +
                        "\nMob: " + mob);
            }
        }
        return mobBonusArmor;
    }

    public Map<EntityType, Double> getMobMaxHealth(String season) {
        Map<EntityType, Double> mobMaxHealth = new HashMap<>();
        if(cfg.getConfigurationSection(season + ".mobMaxHealth") == null) {
            logger.severe("Invalid mobMaxHealth for season " + season);
            return mobMaxHealth;
        }
        var sectionKeys = cfg.getConfigurationSection(season + ".mobMaxHealth").getKeys(false);
        for (var mob : sectionKeys) {
            try {
                EntityType entityType = EntityType.valueOf(mob);
                double maxHealth = cfg.getDouble(season + ".mobMaxHealth." + mob);
                if(maxHealth <= 0)
                    throw new NullPointerException();
                mobMaxHealth.put(entityType, maxHealth);
            } catch (Exception e) {
                logger.severe("Invalid mobMaxHealth '" + mob + "' for season: " + season);
            }
        }
        return mobMaxHealth;
    }

    public Map<EntityType, Double> getMobAttackDamage(String season) {
        Map<EntityType, Double> mobAttackDamage = new HashMap<>();
        if(cfg.getConfigurationSection(season + ".mobAttackDamage") == null) {
            logger.severe("Invalid mobAttackDamage for season " + season);
            return mobAttackDamage;
        }
        var sectionKeys = cfg.getConfigurationSection(season + ".mobAttackDamage").getKeys(false);
        for (var mob : sectionKeys) {
            try {
                EntityType entityType = EntityType.valueOf(mob);
                double attackDamage = cfg.getDouble(season + ".mobAttackDamage." + mob);
                if(attackDamage <= 0)
                    throw new NullPointerException();
                mobAttackDamage.put(entityType, attackDamage);
            } catch (Exception e) {
                logger.severe("Invalid mobAttackDamage '" + mob + "' for season: " + season);
            }
        }
        return mobAttackDamage;
    }

    public List<Material> getPreventCropGrowing(String season) {
        List<Material> preventCropGrowing = new ArrayList<>();
        var rawpreventCropGrowingList = cfg.getStringList(season + ".preventCropGrowing");
        for(var cropString : rawpreventCropGrowingList) {
            try {
                var cropType = Material.valueOf(cropString);
                preventCropGrowing.add(cropType);
            } catch (Exception e) {
                logger.severe("Invalid preventCropGrowing '" + cropString + "' for season " + season);
            }
        }
        return preventCropGrowing;
    }

    public List<PotionEffect> getPotionEffects(String season) {
        List<PotionEffect> potionEffects = new ArrayList<>();
        if(cfg.getConfigurationSection(season + ".potionEffects") == null) {
            logger.severe("Invalid potionEffects for season " + season);
            return potionEffects;
        }
        var keys = cfg.getConfigurationSection(season + ".potionEffects").getKeys(false);
        for(var key : keys) {
            try {
                var type = PotionEffectType.getByName(key.toUpperCase());
                var amplifier = cfg.getInt(season + ".potionEffects." + key)-1;
                if(amplifier < 0)
                    throw new Exception();
                potionEffects.add(type.createEffect(10*20, amplifier));
            } catch (Exception e) {
                logger.severe("Invalid potionEffects '" + key + "' for season " + season);
            }
        }
        return potionEffects;
    }

    public List<LootDrop> getLootDrops(String season) {
        List<LootDrop> lootDrops = new ArrayList<>();
        var entities = cfg.getConfigurationSection(season + ".lootDrops").getKeys(false);
        for(var entity : entities) {
            try {
                EntityType.valueOf(entity);
            } catch (Exception e) {
                logger.severe("Invalid LootDrops entity '" + entity + "' for season " + season);
                continue;
            }
            Map<ItemStack, Double> itemList = new HashMap<>();
            var itemKeys = cfg.getConfigurationSection(season + ".lootDrops." + entity).getKeys(false);

            for(var item : itemKeys) {
                var displayname = cfg.getString(season + ".lootDrops." + entity + "." + item + ".displayname");
                var lore = cfg.getStringList(season + ".lootDrops." + entity + "." + item + ".lore");
                var material = cfg.getString(season + ".lootDrops." + entity + "." + item + ".material");
                var amount = cfg.getInt(season + ".lootDrops." + entity + "." + item + ".amount");
                var dropChance = cfg.getDouble(season + ".lootDrops." + entity + "." + item + ".dropChance");
                var enchantments = new HashMap<Enchantment, Integer>();

                if(cfg.getConfigurationSection(season + ".lootDrops." + entity + "." + item + ".enchantments") == null) {
                    logger.severe("Invalid enchantments section\n" +
                            "Season: " + season + "\n" +
                            "Mob: " + entity + "\n" +
                            "Item: " + item);
                    continue;
                }

                cfg.getConfigurationSection(season + ".lootDrops." + entity + "." + item + ".enchantments").getKeys(false).forEach(enchantmentName -> {
                    var level = cfg.getInt(season + ".lootDrops." + entity + "." + item + ".enchantments." + enchantmentName);
                    var enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchantmentName));
                    if(enchantment != null) {
                        if(level != 0) {
                            enchantments.put(enchantment, level);
                        } else
                            logger.severe("Invalid enchantment level '" + level + "'\n" +
                                    "Season: " + season + "\n" +
                                    "Mob: " + entity + "\n" +
                                    "Item: " + item);
                    } else
                        logger.severe("Invalid enchantment '" + enchantmentName + "'\n" +
                                "Season: " + season + "\n" +
                                "Mob: " + entity + "\n" +
                                "Item: " + item);
                });

                if(displayname == null) {
                    logger.severe("Invalid displayname!\n" +
                            "Season: " + season + "\n" +
                            "Mob: " + entity + "\n" +
                            "Item: " + item);
                    continue;
                }
                try {
                    mm.deserialize(displayname);
                } catch (Exception e) {
                    logger.severe("Invalid displayname format!\n" +
                            "Season: " + season + "\n" +
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
                            logger.severe("Invalid lore line '" + line + "'\n" +
                                    "Season: " + season + "\n" +
                                    "Mob: " + entity + "\n" +
                                    "Item: " + item + "\n" +
                                    "Error: " + e.getMessage());
                            lore.remove(line);
                        }
                    });
                }

                if(material == null) {
                    logger.severe("Invalid material!\n" +
                            "Season: " + season + "\n" +
                            "Mob: " + entity + "\n" +
                            "Item: " + item);
                    continue;
                }
                if(Material.getMaterial(material) == null) {
                    logger.severe("Invalid material name '" + material + "'\n" +
                            "Season: " + season + "\n" +
                            "Mob: " + entity + "\n" +
                            "Item: " + item + "\n");
                    continue;
                }

                if(amount == 0) {
                    logger.severe("Invalid amount '" + amount + "'\n" +
                            "Season: " + season + "\n" +
                            "Mob: " + entity + "\n" +
                            "Item: " + item + "\n");
                    continue;
                }

                if(dropChance < 0) {
                    logger.severe("Invalid dropChance '" + dropChance + "'\n" +
                            "Season: " + season + "\n" +
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
        List<BossEntity> bossList = new ArrayList<>();
        var bossSection = cfg.getConfigurationSection(season + ".bossSpawning");
        if(bossSection == null) {
            logger.severe("Invalid bossSpawning section for season: " + season);
            return bossList;
        }

        var mobTypeSection = bossSection.getKeys(false);
        for(var mobTypeString : mobTypeSection) {
            try {
                EntityType.valueOf(mobTypeString);
            } catch (Exception e) {
                logger.severe("Invalid bossSpawning mobtype for season: " + season);
                continue;
            }

            var displayName = cfg.getString(season + ".bossSpawning." + mobTypeString + ".displayName");
            var nameVisible = cfg.getBoolean(season + ".bossSpawning." + mobTypeString + ".nameVisible");
            var spawnChance = cfg.getDouble(season + ".bossSpawning." + mobTypeString + ".spawnChance");
            var maxHealth = cfg.getDouble(season + ".bossSpawning." + mobTypeString + ".maxHealth");
            var attackDamage = cfg.getDouble(season + ".bossSpawning." + mobTypeString + ".attackDamage");
            var movementSpeed = cfg.getDouble(season + ".bossSpawning." + mobTypeString + ".movementSpeed");
            var bonusArmor = cfg.getDouble(season + ".bossSpawning." + mobTypeString + ".bonusArmor");
            var followRange = cfg.getInt(season + ".bossSpawning." + mobTypeString + ".followRange");
            var droppedXPOnDeath = cfg.getInt(season + ".bossSpawning." + mobTypeString + ".droppedXPOnDeath");
            if(displayName == null) {
                logger.severe("Invalid bossSpawning displayname!" +
                        "\nSeason: " + season +
                        "\nMobtype: " + mobTypeString);
                continue;
            }
            if(checkMMFormat(displayName) != null) {
                logger.severe("Invalid bossSpawning displayname format!" +
                        "\nSeason: " + season +
                        "\nMobtype: " + mobTypeString +
                        "\nError: " + checkMMFormat(displayName));
                continue;
            }
            if(spawnChance > 100 || spawnChance < 0) {
                logger.severe("Invalid bossSpawning spawnChance!" +
                        "\nSeason: " + season +
                        "\nMobtype: " + mobTypeString);
                continue;
            }
            if(maxHealth <= 0) {
                logger.severe("Invalid bossSpawning maxHealth!" +
                        "\nSeason: " + season +
                        "\nMobtype: " + mobTypeString);
                continue;
            }
            if(attackDamage <= 0) {
                logger.severe("Invalid bossSpawning attackDamage!" +
                        "\nSeason: " + season +
                        "\nMobtype: " + mobTypeString);
                continue;
            }
            if(movementSpeed <= 0) {
                logger.severe("Invalid bossSpawning movementSpeed!" +
                        "\nSeason: " + season +
                        "\nMobtype: " + mobTypeString);
                continue;
            }
            if(bonusArmor < 0) {
                logger.severe("Invalid bossSpawning bonusArmor!" +
                        "\nSeason: " + season +
                        "\nMobtype: " + mobTypeString);
                continue;
            }
            if(followRange <= 0) {
                logger.severe("Invalid bossSpawning followRange!" +
                        "\nSeason: " + season +
                        "\nMobtype: " + mobTypeString);
                continue;
            }
            if(droppedXPOnDeath < 0) {
                logger.severe("Invalid bossSpawning droppedXPOnDeath!" +
                        "\nSeason: " + season +
                        "\nMobtype: " + mobTypeString);
                continue;
            }

            var soundsSection = cfg.getConfigurationSection(season + ".bossSpawning." + mobTypeString + ".sounds");
            if(soundsSection == null) {
                logger.severe("Invalid bossSpawning Sounds section for season: " + season);
                continue;
            }
            var soundsKeys = soundsSection.getKeys(false);

            boolean invalidSound = false;
            for(var soundString : soundsKeys) {
                if(cfg.getDouble(season + ".bossSpawning." + mobTypeString + ".sounds." + soundString + ".volume") < 0) {
                    logger.severe("Invalid bossSpawning sound volume!" +
                            "\nSeason: " + season +
                            "\nMobtype: " + mobTypeString +
                            "\nSound: " + soundString);
                    invalidSound = true;
                }
                if(cfg.getDouble(season + ".bossSpawning." + mobTypeString + ".sounds." + soundString + ".pitch") < 0 || cfg.getDouble(season + ".bossSpawning." + mobTypeString + ".sounds." + soundString + ".pitch") > 2) {
                    logger.severe("Invalid bossSpawning sound pitch!" +
                            "\nSeason: " + season +
                            "\nMobtype: " + mobTypeString +
                            "\nSound: " + soundString);
                    invalidSound = true;
                }

            }
            if(invalidSound)
                continue;

            var spawnSoundType = cfg.getString(season + ".bossSpawning." + mobTypeString + ".sounds.spawn.sound").toLowerCase();
            var spawnSoundVolume = cfg.getDouble(season + ".bossSpawning." + mobTypeString + ".sounds.spawn.volume");
            var spawnSoundPitch = cfg.getDouble(season + ".bossSpawning." + mobTypeString + ".sounds.spawn.pitch");
            var ambientSoundType = cfg.getString(season + ".bossSpawning." + mobTypeString + ".sounds.ambient.sound").toLowerCase();
            var ambientSoundVolume = cfg.getDouble(season + ".bossSpawning." + mobTypeString + ".sounds.ambient.volume");
            var ambientSoundPitch = cfg.getDouble(season + ".bossSpawning." + mobTypeString + ".sounds.ambient.pitch");
            var deathSoundType = cfg.getString(season + ".bossSpawning." + mobTypeString + ".sounds.death.sound").toLowerCase();
            var deathSoundVolume = cfg.getDouble(season + ".bossSpawning." + mobTypeString + ".sounds.death.volume");
            var deathSoundPitch = cfg.getDouble(season + ".bossSpawning." + mobTypeString + ".sounds.death.pitch");
            var takeDamageSoundType = cfg.getString(season + ".bossSpawning." + mobTypeString + ".sounds.takeDamage.sound").toLowerCase();
            var takeDamageSoundVolume = cfg.getDouble(season + ".bossSpawning." + mobTypeString + ".sounds.takeDamage.volume");
            var takeDamageSoundPitch = cfg.getDouble(season + ".bossSpawning." + mobTypeString + ".sounds.takeDamage.pitch");
            var dealDamageSoundType = cfg.getString(season + ".bossSpawning." + mobTypeString + ".sounds.dealDamage.sound").toLowerCase();
            var dealDamageSoundVolume = cfg.getDouble(season + ".bossSpawning." + mobTypeString + ".sounds.dealDamage.volume");
            var dealDamageSoundPitch = cfg.getDouble(season + ".bossSpawning." + mobTypeString + ".sounds.dealDamage.pitch");

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

            var commandsSection = cfg.getConfigurationSection(season + ".bossSpawning." + mobTypeString + ".executeCommandsOnDeath");
            if(commandsSection == null) {
                logger.severe("Invalid executeCommandsOnDeath section for season: " + season);
                continue;
            }
            var executeCommandsOnDeathEnabled = cfg.getBoolean(season + ".bossSpawning." + mobTypeString + ".executeCommandsOnDeath.enabled");
            List<String> commandList = cfg.getStringList(season + ".bossSpawning." + mobTypeString + ".executeCommandsOnDeath.commands");

            var lootDrops = getBossSpawningLootDrops(season, mobTypeString);

            BossEntity bossEntity = new BossEntity(
                    EntityType.valueOf(mobTypeString),
                    displayName,
                    nameVisible,
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
                    lootDrops
            );
            bossList.add(bossEntity);
        }
        return bossList;
    }

    private Map<ItemStack, Double> getBossSpawningLootDrops(String season, String mobType) {
        Map<ItemStack, Double> lootDrops = new HashMap<>();
        if(cfg.getConfigurationSection(season + ".bossSpawning." + mobType + ".lootOnDeath") == null) {
            logger.severe("Invalid bossSpawning lootOnDeath section for season: " + season);
            return lootDrops;
        }

        var items = cfg.getConfigurationSection(season + ".bossSpawning." + mobType + ".lootOnDeath").getKeys(false);
        for(var item : items) {
            var displayname = cfg.getString(season + ".bossSpawning." + mobType + ".lootOnDeath." + item + ".displayname");
            var lore = cfg.getStringList(season + ".bossSpawning." + mobType + ".lootOnDeath." + item + ".lore");
            var material = cfg.getString(season + ".bossSpawning." + mobType + ".lootOnDeath." + item + ".material");
            var amount = cfg.getInt(season + ".bossSpawning." + mobType + ".lootOnDeath." + item + ".amount");
            var dropChance = cfg.getDouble(season + ".bossSpawning." + mobType + ".lootOnDeath." + item + ".dropChance");
            var enchantments = new HashMap<Enchantment, Integer>();

            if (cfg.getConfigurationSection(season + ".bossSpawning." + mobType + ".lootOnDeath." + item + ".enchantments") == null) {
                logger.severe("Invalid bossSpawning lootOnDeath enchantments section\n" +
                        "Season: " + season + "\n" +
                        "Item: " + item);
                continue;
            }

            cfg.getConfigurationSection(season + ".bossSpawning." + mobType + ".lootOnDeath." + item + ".enchantments").getKeys(false).forEach(enchantmentName -> {
                var level = cfg.getInt(season + ".bossSpawning." + mobType + ".lootOnDeath." + item + ".enchantments." + enchantmentName);
                var enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchantmentName));
                if (enchantment != null) {
                    if (level != 0) {
                        enchantments.put(enchantment, level);
                    } else
                        logger.severe("Invalid bossSpawning lootOnDeath enchantment level '" + level + "'\n" +
                                "Season: " + season + "\n" +
                                "Item: " + item);
                } else
                    logger.severe("Invalid bossSpawning lootOnDeath enchantment '" + enchantmentName + "'\n" +
                            "Season: " + season + "\n" +
                            "Item: " + item);
            });

            if (displayname == null) {
                logger.severe("Invalid bossSpawning lootOnDeath displayname!\n" +
                        "Season: " + season + "\n" +
                        "Item: " + item);
                continue;
            }
            try {
                mm.deserialize(displayname);
            } catch (Exception e) {
                logger.severe("Invalid bossSpawning lootOnDeath displayname format!\n" +
                        "Season: " + season + "\n" +
                        "Item: " + item + "\n" +
                        "Error: " + e.getMessage());
                continue;
            }

            if (!lore.isEmpty()) {
                lore.forEach(line -> {
                    try {
                        mm.deserialize(line);
                    } catch (Exception e) {
                        logger.severe("Invalid bossSpawning lootOnDeath lore line '" + line + "'\n" +
                                "Season: " + season + "\n" +
                                "Item: " + item + "\n" +
                                "Error: " + e.getMessage());
                        lore.remove(line);
                    }
                });
            }

            if (material == null) {
                logger.severe("Invalid bossSpawning lootOnDeath material!\n" +
                        "Season: " + season + "\n" +
                        "Item: " + item);
                continue;
            }
            if (Material.getMaterial(material) == null) {
                logger.severe("Invalid bossSpawning lootOnDeath material name '" + material + "'\n" +
                        "Season: " + season + "\n" +
                        "Item: " + item + "\n");
                continue;
            }

            if (amount < 0) {
                logger.severe("Invalid bossSpawning lootOnDeath amount '" + amount + "'\n" +
                        "Season: " + season + "\n" +
                        "Item: " + item + "\n");
                continue;
            }

            if (dropChance < 0) {
                logger.severe("Invalid bossSpawning lootOnDeath dropChance '" + dropChance + "'\n" +
                        "Season: " + season + "\n" +
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
        int xpBonus = cfg.getInt(season + ".xpBonus");
        if(xpBonus < 0) {
            logger.severe("Invalid xpBonus for season: " + season);
            return 0;
        }
        return xpBonus;
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
