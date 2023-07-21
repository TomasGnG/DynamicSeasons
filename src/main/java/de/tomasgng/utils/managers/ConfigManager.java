package de.tomasgng.utils.managers;

import de.tomasgng.DynamicSeasons;
import de.tomasgng.utils.enums.SeasonType;
import de.tomasgng.utils.enums.WeatherType;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private final File folder = new File("plugins/DynamicSeasons");
    private final File config = new File("plugins/DynamicSeasons/config.yml");
    private YamlConfiguration cfg = YamlConfiguration.loadConfiguration(config);
    private Connection connection;

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

        var statement = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS data (time BIGINT, season TEXT)");
        statement.executeUpdate();
        statement.close();
    }

    @SneakyThrows
    private void setupDatabase() {
        var statement = connection.prepareStatement("SELECT * FROM data");
        var rs = statement.executeQuery();
        if(!rs.next()) {
            var statement2 = connection.prepareStatement("INSERT INTO data VALUES (?, ?)");
            statement2.setInt(1, getDuration());
            statement2.setString(2, SeasonType.SPRING.name());
            statement2.executeUpdate();
            statement2.close();
        }
        rs.close();
        statement.close();
    }

    @SneakyThrows
    public SeasonType getCurrentSeasonTypeFromDatabase() {
        var statement = connection.prepareStatement("SELECT * FROM data");
        var rs = statement.executeQuery();
        rs.next();
        SeasonType seasonType = SeasonType.valueOf(rs.getString("season"));
        rs.close();
        statement.close();
        return seasonType;
    }

    @SneakyThrows
    public int getRemainingTimeFromDatabase() {
        var statement = connection.prepareStatement("SELECT * FROM data");
        var rs = statement.executeQuery();
        rs.next();
        int remainingtime = rs.getInt("time");
        rs.close();
        statement.close();
        return remainingtime;
    }

    @SneakyThrows
    public void decreaseRemainingTime() {
        var statement = connection.prepareStatement("UPDATE data SET time=?");
        statement.setInt(1, getRemainingTimeFromDatabase()-1);
        statement.executeUpdate();
        statement.close();
    }

    @SneakyThrows
    public void updateCurrentSeason(SeasonType newSeason) {
        var statement = connection.prepareStatement("UPDATE data SET season=?");
        statement.setString(1, newSeason.name());
        statement.executeUpdate();
        statement.close();
    }

    @SneakyThrows
    public void resetRemainingTime() {
        var statement = connection.prepareStatement("UPDATE data SET time=?");
        statement.setInt(1, getDuration());
        statement.executeUpdate();
        statement.close();
    }
    //endregion

    @SneakyThrows
    private void createFiles() {
        if(!folder.exists()) folder.mkdirs();
        if(!config.exists()) {
            config.createNewFile();

            cfg.set("season_duration", 300);
            cfg.setComments("season_duration", List.of("Specify the duration of the seasons in Seconds", "e.g. for one hour -> season_duration: 3600 "));
            cfg.set("worlds", List.of("world"));
            cfg.setComments("worlds", List.of("Specify the worlds where the seasons should work."));
            cfg.set("placeholders.duration.placeholderName", "duration");
            cfg.set("placeholders.duration.format", "HH:mm:ss");
            cfg.setComments("placeholders.duration.format", List.of("Use your own date format. For help use this site: https://help.gooddata.com/cloudconnect/manual/date-and-time-format.html#:~:text=Table%C2%A028.5.%C2%A0Date%20and%20Time%20Format%20Patterns%20and%20Results%20(Java)"));
            cfg.set("placeholders.currentSeason.placeholderName", "currentSeason");
            cfg.set("placeholders.currentSeason.text.spring", "Spring");
            cfg.set("placeholders.currentSeason.text.summer", "Summer");
            cfg.set("placeholders.currentSeason.text.fall", "Fall");
            cfg.set("placeholders.currentSeason.text.winter", "Winter");
            cfg.setComments("placeholders.currentSeason.text", List.of("Set the replacement for these seasons."));

            cfg.set("spring.weather.enabled", true);
            cfg.set("spring.weather.type.clear", true);
            cfg.set("spring.weather.type.storm", true);
            cfg.set("spring.weather.type.thunder", false);
            cfg.setComments("spring.weather", List.of("Customize the weather for the season"));
            cfg.setComments("spring.weather.type", List.of("Customize the allowed weather types"));
            cfg.set("spring.randomTickSpeed", 4);
            cfg.setComments("spring.randomTickSpeed", List.of("The growth speed of plants etc. default value -> 3.", "higher -> faster | large values can cause server lag!", "Heres a list what will be effected by the change: https://minecraft.fandom.com/wiki/Tick#:~:text=Most%20blocks%20ignore%20this%20tick%2C%20but%20some%20use%20it%20to%20do%20something%3A"));
            cfg.set("spring.animalSpawning.SHEEP", 80);
            cfg.set("spring.animalSpawning.CHICKEN", 40);
            cfg.setComments("spring.animalSpawning", List.of("The probability of an animal to spawn. 1-100%", "Here is a list of all animals: https://pastebin.com/raw/Tf3mMGg6", "Format -> (MOB_NAME): (PERCENT)"));
            cfg.set("spring.mobMovement.ZOMBIE", 0.3);
            cfg.set("spring.mobMovement.SPIDER", 0.4);
            cfg.setComments("spring.mobMovement", List.of("Customize the movement speed of mobs", "Here is a list of all mobs and their default movement speed: https://pastebin.com/raw/2WaGi20Z", "Format -> (MOB_NAME): (SPEED)"));
            cfg.set("spring.animalGrowing.COW", 6000);
            cfg.set("spring.animalGrowing.SHEEP", 3600);
            cfg.setComments("spring.animalGrowing", List.of("Customize the speed of animal growing", "Most baby mobs take 20 mins (24000 ticks) to grow up", "Here is a list of all breedable animals: https://pastebin.com/raw/zzUAc3XM", "Here is a tick calculator: https://mapmaking.fr/tick/", "IMPORTANT: 20 ticks = 1 second", "Format -> (ANIMAL_NAME): (TIME IN TICKS)"));
            cfg.set("spring.xpBonus", 20);
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
            cfg.set("winter.xpBonus", 20);

            save();
        }
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

    public DateTimeFormatter getDurationPlaceholderFormat() {
        var format = cfg.getString("placeholders.duration.format");
        if(format == null) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid duration format. Using default name -> HH:mm:ss");
            return DateTimeFormatter.ofPattern("HH:mm:ss");
        }
        try {
            return DateTimeFormatter.ofPattern(format);
        } catch (Exception e) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid duration format. Using default name -> HH:mm:ss");
            return DateTimeFormatter.ofPattern("HH:mm:ss");
        }
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
        if(worlds.size() == 0) {
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

    public int getXPBonus(String season) {
        int xpBonus = cfg.getInt(season + ".xpBonus");
        if(xpBonus < 0) {
            DynamicSeasons.getInstance().getLogger().severe("Invalid xpBonus for season: " + season);
            return 0;
        }
        return xpBonus;
    }
}
