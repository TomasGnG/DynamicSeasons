package de.tomasgng.utils.managers;

import de.tomasgng.DynamicSeasons;
import de.tomasgng.utils.enums.SeasonType;
import de.tomasgng.utils.enums.WeatherType;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.*;

public class ConfigManager {

    private final File folder = new File("plugins/DynamicSeasons");
    private final File config = new File("plugins/DynamicSeasons/config.yml");
    private YamlConfiguration cfg = YamlConfiguration.loadConfiguration(config);
    private Connection connection;
    private final String sqlSelectAllData = "SELECT * FROM data";
    private final String sqlUpdateTime = "UPDATE data SET time=?";

    private final Map<String, Object> generalConfigSections = new TreeMap<>();
    private final Map<String, Object> seasonConfigSections = new TreeMap<>();
    private final Map<String, List<String>> configSectionComments = new TreeMap<>();

    public ConfigManager() {
        createFiles();
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

            cfg.set("season_duration", 300);
            cfg.set("worlds", List.of("world"));
            cfg.set("placeholders.duration.placeholderName", "duration");
            cfg.set("placeholders.duration.format", "HH:mm:ss");
            cfg.set("placeholders.currentSeason.placeholderName", "currentSeason");
            cfg.set("placeholders.currentSeason.text.spring", "Spring");
            cfg.set("placeholders.currentSeason.text.summer", "Summer");
            cfg.set("placeholders.currentSeason.text.fall", "Fall");
            cfg.set("placeholders.currentSeason.text.winter", "Winter");
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
            cfg.set("spring.xpBonus", 20);
            cfg.setComments("spring.potionEffects", List.of("Customize the potion effects for players", "List of all potion effects: https://pastebin.com/raw/KPh96Mf9"));
            cfg.setComments("spring.preventCropGrowing", List.of("Customize the crops that are not allowed to grow", "List of all crops: https://minecraft.fandom.com/wiki/Crops"));
            cfg.setComments("spring.mobAttackDamage", List.of("Customize the attack damage for mobs", "List of all mobs and their attack damage: https://pastebin.com/raw/XnC3kNXi"));
            cfg.setComments("spring.mobBonusArmor", List.of("Customize the bonus armor for mobs", "2 equals 1 Armor-slot | MAX is 20"));
            cfg.setComments("spring.mobMaxHealth", List.of("Customize the max health for mobs", "2 equals 1 heart | MAX is 20", "List of all mobs and their max health: https://pastebin.com/raw/5upq7HVr"));
            cfg.setComments("spring.animalGrowing", List.of("Customize the speed of animal growing", "Most baby mobs take 20 mins (24000 ticks) to grow up", "Here is a list of all breedable animals: https://pastebin.com/raw/zzUAc3XM", "Here is a tick calculator: https://mapmaking.fr/tick/", "IMPORTANT: 20 ticks = 1 second", "Format -> (ANIMAL_NAME): (TIME IN TICKS)"));
            cfg.setComments("spring.mobMovement", List.of("Customize the movement speed of mobs", "Here is a list of all mobs and their default movement speed: https://pastebin.com/raw/2WaGi20Z", "Format -> (MOB_NAME): (SPEED)"));
            cfg.setComments("spring.animalSpawning", List.of("The probability of an animal to spawn. 1-100%", "Here is a list of all animals: https://pastebin.com/raw/Tf3mMGg6", "Format -> (MOB_NAME): (PERCENT)"));
            cfg.setComments("spring.randomTickSpeed", List.of("The growth speed of plants etc. default value -> 3.", "higher -> faster | large values can cause server lag!", "Heres a list what will be effected by the change: https://minecraft.fandom.com/wiki/Tick#:~:text=Most%20blocks%20ignore%20this%20tick%2C%20but%20some%20use%20it%20to%20do%20something%3A"));
            cfg.setComments("spring.weather", List.of("Customize the weather for the season"));
            cfg.setComments("spring.weather.type", List.of("Customize the allowed weather types"));
            cfg.setComments("spring.xpBonus", List.of("The bonus xp you get when picking up xp (in percent)", "e.g. if you set 20 then the player will get 20% more xp. (20% of the picked up xp)"));

            cfg.set("summer.weather.enabled", true);
            cfg.set("summer.weather.type.clear", true);
            cfg.set("summer.weather.type.storm", true);
            cfg.set("summer.weather.type.thunder", false);
            cfg.set("summer.randomTickSpeed", 4);
            cfg.set("summer.animalSpawning.SHEEP", 80);
            cfg.set("summer.animalSpawning.CHICKEN", 40);
            cfg.set("summer.mobMovement.ZOMBIE", 0.3);
            cfg.set("summer.mobMovement.SPIDER", 0.4);
            cfg.set("summer.animalGrowing.COW", 6000);
            cfg.set("summer.animalGrowing.SHEEP", 3600);
            cfg.set("summer.mobBonusArmor.ZOMBIE", 2.5);
            cfg.set("summer.mobBonusArmor.CREEPER", 1.0);
            cfg.set("summer.mobMaxHealth.CREEPER", 25.0);
            cfg.set("summer.mobMaxHealth.ZOMBIE", 30.0);
            cfg.set("summer.mobAttackDamage.ZOMBIE", 4.0);
            cfg.set("summer.mobAttackDamage.SPIDER", 3.0);
            cfg.set("summer.preventCropGrowing", List.of("POTATOES", "CARROTS"));
            cfg.set("summer.potionEffects.SPEED", 1);
            cfg.set("summer.potionEffects.REGENERATION", 1);
            cfg.set("summer.xpBonus", 20);

            cfg.set("fall.weather.enabled", true);
            cfg.set("fall.weather.type.clear", true);
            cfg.set("fall.weather.type.storm", true);
            cfg.set("fall.weather.type.thunder", false);
            cfg.set("fall.randomTickSpeed", 4);
            cfg.set("fall.animalSpawning.SHEEP", 80);
            cfg.set("fall.animalSpawning.CHICKEN", 40);
            cfg.set("fall.mobMovement.ZOMBIE", 0.3);
            cfg.set("fall.mobMovement.SPIDER", 0.4);
            cfg.set("fall.animalGrowing.COW", 6000);
            cfg.set("fall.animalGrowing.SHEEP", 3600);
            cfg.set("fall.mobBonusArmor.ZOMBIE", 2.5);
            cfg.set("fall.mobBonusArmor.CREEPER", 1.0);
            cfg.set("fall.mobMaxHealth.CREEPER", 25.0);
            cfg.set("fall.mobMaxHealth.ZOMBIE", 30.0);
            cfg.set("fall.mobAttackDamage.ZOMBIE", 4.0);
            cfg.set("fall.mobAttackDamage.SPIDER", 3.0);
            cfg.set("fall.preventCropGrowing", List.of("POTATOES", "CARROTS"));
            cfg.set("fall.potionEffects.SPEED", 1);
            cfg.set("fall.potionEffects.REGENERATION", 1);
            cfg.set("fall.xpBonus", 20);

            cfg.set("winter.weather.enabled", true);
            cfg.set("winter.weather.type.clear", true);
            cfg.set("winter.weather.type.storm", true);
            cfg.set("winter.weather.type.thunder", false);
            cfg.set("winter.randomTickSpeed", 4);
            cfg.set("winter.animalSpawning.SHEEP", 80);
            cfg.set("winter.animalSpawning.CHICKEN", 40);
            cfg.set("winter.mobMovement.ZOMBIE", 0.3);
            cfg.set("winter.mobMovement.SPIDER", 0.4);
            cfg.set("winter.animalGrowing.COW", 6000);
            cfg.set("winter.animalGrowing.SHEEP", 3600);
            cfg.set("winter.mobBonusArmor.ZOMBIE", 2.5);
            cfg.set("winter.mobBonusArmor.CREEPER", 1.0);
            cfg.set("winter.mobMaxHealth.CREEPER", 25.0);
            cfg.set("winter.mobMaxHealth.ZOMBIE", 30.0);
            cfg.set("winter.mobAttackDamage.ZOMBIE", 4.0);
            cfg.set("winter.mobAttackDamage.SPIDER", 3.0);
            cfg.set("winter.preventCropGrowing", List.of("POTATOES", "CARROTS"));
            cfg.set("winter.potionEffects.SPEED", 1);
            cfg.set("winter.potionEffects.REGENERATION", 1);
            cfg.set("winter.xpBonus", 20);

            save();
        }
        generalConfigSections.put("season_duration", 300);
        generalConfigSections.put("worlds", List.of("world"));
        generalConfigSections.put("placeholders.duration.placeholderName", "duration");
        generalConfigSections.put("placeholders.duration.format", "HH:mm:ss");
        generalConfigSections.put("placeholders.currentSeason.placeholderName", "currentSeason");
        generalConfigSections.put("placeholders.currentSeason.text.spring", "Spring");
        generalConfigSections.put("placeholders.currentSeason.text.summer", "Summer");
        generalConfigSections.put("placeholders.currentSeason.text.fall", "Fall");
        generalConfigSections.put("placeholders.currentSeason.text.winter", "Winter");

        seasonConfigSections.put("weather.enabled", true);
        seasonConfigSections.put("weather.type.clear", true);
        seasonConfigSections.put("weather.type.storm", true);
        seasonConfigSections.put("weather.type.thunder", false);
        seasonConfigSections.put("randomTickSpeed", 4);
        seasonConfigSections.put("animalSpawning.SHEEP", 80);
        seasonConfigSections.put("animalSpawning.CHICKEN", 40);
        seasonConfigSections.put("mobMovement.ZOMBIE", 0.3);
        seasonConfigSections.put("mobMovement.SPIDER", 0.4);
        seasonConfigSections.put("animalGrowing.COW", 6000);
        seasonConfigSections.put("animalGrowing.SHEEP", 3600);
        seasonConfigSections.put("mobBonusArmor.ZOMBIE", 2.5);
        seasonConfigSections.put("mobBonusArmor.CREEPER", 1.0);
        seasonConfigSections.put("mobMaxHealth.CREEPER", 25.0);
        seasonConfigSections.put("mobMaxHealth.ZOMBIE", 30.0);
        seasonConfigSections.put("mobAttackDamage.ZOMBIE", 4.0);
        seasonConfigSections.put("mobAttackDamage.SPIDER", 3.0);
        seasonConfigSections.put("preventCropGrowing", List.of("POTATOES", "CARROTS"));
        seasonConfigSections.put("potionEffects.SPEED", 1);
        seasonConfigSections.put("potionEffects.REGENERATION", 1);
        seasonConfigSections.put("xpBonus", 20);

        configSectionComments.put("spring.potionEffects", List.of("Customize the potion effects for players", "List of all potion effects: https://pastebin.com/raw/KPh96Mf9"));
        configSectionComments.put("spring.preventCropGrowing", List.of("Customize the crops that are not allowed to grow", "List of all crops: https://minecraft.fandom.com/wiki/Crops"));
        configSectionComments.put("spring.mobAttackDamage", List.of("Customize the attack damage for mobs", "List of all mobs and their attack damage: https://pastebin.com/raw/XnC3kNXi"));
        configSectionComments.put("spring.mobBonusArmor", List.of("Customize the bonus armor for mobs", "2 equals 1 Armor-slot | MAX is 20"));
        configSectionComments.put("spring.mobMaxHealth", List.of("Customize the max health for mobs", "2 equals 1 heart | MAX is 20", "List of all mobs and their max health: https://pastebin.com/raw/5upq7HVr"));
        configSectionComments.put("spring.animalGrowing", List.of("Customize the speed of animal growing", "Most baby mobs take 20 mins (24000 ticks) to grow up", "Here is a list of all breedable animals: https://pastebin.com/raw/zzUAc3XM", "Here is a tick calculator: https://mapmaking.fr/tick/", "IMPORTANT: 20 ticks = 1 second", "Format -> (ANIMAL_NAME): (TIME IN TICKS)"));
        configSectionComments.put("spring.mobMovement", List.of("Customize the movement speed of mobs", "Here is a list of all mobs and their default movement speed: https://pastebin.com/raw/2WaGi20Z", "Format -> (MOB_NAME): (SPEED)"));
        configSectionComments.put("spring.animalSpawning", List.of("The probability of an animal to spawn. 1-100%", "Here is a list of all animals: https://pastebin.com/raw/Tf3mMGg6", "Format -> (MOB_NAME): (PERCENT)"));
        configSectionComments.put("spring.randomTickSpeed", List.of("The growth speed of plants etc. default value -> 3.", "higher -> faster | large values can cause server lag!", "Heres a list what will be effected by the change: https://minecraft.fandom.com/wiki/Tick#:~:text=Most%20blocks%20ignore%20this%20tick%2C%20but%20some%20use%20it%20to%20do%20something%3A"));
        configSectionComments.put("spring.weather", List.of("Customize the weather for the season"));
        configSectionComments.put("spring.weather.type", List.of("Customize the allowed weather types"));
        configSectionComments.put("spring.xpBonus", List.of("The bonus xp you get when picking up xp (in percent)", "e.g. if you set 20 then the player will get 20% more xp. (20% of the picked up xp)"));

        fixMissingSections();
    }

    private void fixMissingSections() {
        reload();
        for(var entry : generalConfigSections.entrySet()) {
            if(!cfg.isSet(entry.getKey()))
                cfg.set(entry.getKey(), entry.getValue());
        }
        for(var entry : seasonConfigSections.entrySet()) {
            for(var season : List.of("spring", "summer", "fall", "winter")) {
                if(entry.getKey().contains(".")) {
                    if(cfg.isSet(season + "." + entry.getKey().split("\\.")[0]))
                        continue;
                } else {
                    if(cfg.isSet(season + "." + entry.getKey()))
                        continue;
                }
                cfg.set(season + "." + entry.getKey(), entry.getValue());
                if(!season.equalsIgnoreCase("spring"))
                    continue;
                for (var cEntry : configSectionComments.entrySet()) {
                    if((season + "." + entry.getKey()).startsWith(cEntry.getKey())) {
                        cfg.setComments(cEntry.getKey(), cEntry.getValue());
                    }

                }
            }
        }
        save();
    }

    @SneakyThrows
    private void save() {
        cfg.save(config);
        reload();
    }

    private void reload() {
        cfg = YamlConfiguration.loadConfiguration(config);
    }

    public String getDurationPlaceholderName() {
        var name = cfg.getString("placeholders.duration.placeholderName");
        if(name == null) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid duration placeholderName. Using default name -> duration");
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
            DynamicSeasons.getInstance().getLogger().severe("Invalid currentSeason placeholderName. Using default name -> currentSeason");
            return "currentSeason";
        }
        return name;
    }

    public String getCurrentSeasonText(SeasonType seasonType) {
        var text = cfg.getString("placeholders.currentSeason.text."+seasonType.name().toLowerCase());
        if(text == null) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid currentSeason text for " + seasonType.name().toLowerCase() + ". Using default text -> " + seasonType.name());
            return seasonType.name();
        }
        return text;
    }

    public int getDuration() {
        int duration = cfg.getInt("season_duration");
        if(duration < 10) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid season_duration. Using default value -> 300!");
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
                    DynamicSeasons.getInstance().getLogger().severe("Invalid world -> \"" + worldName + "\"");
                continue;
            }
            worlds.add(Bukkit.getWorld(worldName));
        }
        if(worlds.isEmpty()) {
            if(!alreadyPrintedInvalidWorlds) {
                DynamicSeasons.getInstance().getLogger().severe("0 worlds loaded. Disabling plugin...");
                DynamicSeasons.getInstance().getLogger().severe("Add worlds to your config.yml!");
            }
            Bukkit.getScheduler().runTask(DynamicSeasons.getInstance(), () -> Bukkit.getPluginManager().disablePlugin(DynamicSeasons.getInstance()));
        }
        alreadyPrintedInvalidWorlds = true;
        return worlds;
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
            DynamicSeasons.getInstance().getLogger().severe("Invalid randomTickSpeed for season: " + season);
            return 3;
        }
        return randomTickSpeed;
    }

    public Map<EntityType, Integer> getAnimalSpawning(String season) {
        Map<EntityType, Integer> animalSpawning = new HashMap<>();
        if(cfg.getConfigurationSection(season + ".animalSpawning") == null) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid animalSpawning for season " + season);
            return animalSpawning;
        }
        var sectionKeys = cfg.getConfigurationSection(season + ".animalSpawning").getKeys(false);
        for (var animal : sectionKeys) {
            try {
                EntityType entityType = EntityType.valueOf(animal);
                int spawnChance = cfg.getInt(season + ".animalSpawning." + animal);
                if(spawnChance > 100 || spawnChance < 0)
                    throw new NullPointerException();
                animalSpawning.put(entityType, spawnChance);
            } catch (Exception e) {
                DynamicSeasons.getInstance().getLogger().severe("Invalid animalSpawning '" + animal + "' for season: " + season);
            }
        }
        return animalSpawning;
    }

    public Map<EntityType, Double> getMobMovement(String season) {
        Map<EntityType, Double> mobMovement = new HashMap<>();
        if(cfg.getConfigurationSection(season + ".mobMovement") == null) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid mobMovement for season " + season);
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
                DynamicSeasons.getInstance().getLogger().severe("Invalid mobMovement '" + mob + "' for season: " + season);
            }
        }
        return mobMovement;
    }

    public Map<EntityType, Integer> getAnimalGrowing(String season) {
        Map<EntityType, Integer> animalGrowing = new HashMap<>();
        if(cfg.getConfigurationSection(season + ".animalGrowing") == null) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid animalGrowing for season " + season);
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
                DynamicSeasons.getInstance().getLogger().severe("Invalid animalGrowing '" + animal + "' for season: " + season);
            }
        }
        return animalGrowing;
    }

    public Map<EntityType, Double> getMobBonusArmor(String season) {
        Map<EntityType, Double> mobBonusArmor = new HashMap<>();
        if(cfg.getConfigurationSection(season + ".mobBonusArmor") == null) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid mobBonusArmor for season " + season);
            return mobBonusArmor;
        }
        var sectionKeys = cfg.getConfigurationSection(season + ".mobBonusArmor").getKeys(false);
        for (var mob : sectionKeys) {
            try {
                EntityType entityType = EntityType.valueOf(mob);
                double bonusArmor = cfg.getDouble(season + ".mobBonusArmor." + mob);
                if(bonusArmor < 0)
                    throw new NullPointerException();
                mobBonusArmor.put(entityType, bonusArmor);
            } catch (Exception e) {
                DynamicSeasons.getInstance().getLogger().severe("Invalid mobBonusArmor '" + mob + "' for season: " + season);
            }
        }
        return mobBonusArmor;
    }

    public Map<EntityType, Double> getMobMaxHealth(String season) {
        Map<EntityType, Double> mobMaxHealth = new HashMap<>();
        if(cfg.getConfigurationSection(season + ".mobMaxHealth") == null) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid mobMaxHealth for season " + season);
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
                DynamicSeasons.getInstance().getLogger().severe("Invalid mobMaxHealth '" + mob + "' for season: " + season);
            }
        }
        return mobMaxHealth;
    }

    public Map<EntityType, Double> getMobAttackDamage(String season) {
        Map<EntityType, Double> mobAttackDamage = new HashMap<>();
        if(cfg.getConfigurationSection(season + ".mobAttackDamage") == null) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid mobAttackDamage for season " + season);
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
                DynamicSeasons.getInstance().getLogger().severe("Invalid mobAttackDamage '" + mob + "' for season: " + season);
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
                DynamicSeasons.getInstance().getLogger().severe("Invalid preventCropGrowing '" + cropString + "' for season " + season);
            }
        }
        return preventCropGrowing;
    }

    public List<PotionEffect> getPotionEffects(String season) {
        List<PotionEffect> potionEffects = new ArrayList<>();
        if(cfg.getConfigurationSection(season + ".potionEffects") == null) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid potionEffects for season " + season);
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
                DynamicSeasons.getInstance().getLogger().severe("Invalid potionEffects '" + key + "' for season " + season);
            }
        }
        return potionEffects;
    }

    public int getXPBonus(String season) {
        int xpBonus = cfg.getInt(season + ".xpBonus");
        if(xpBonus < 0) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid xpBonus for season: " + season);
            return 0;
        }
        return xpBonus;
    }
}
