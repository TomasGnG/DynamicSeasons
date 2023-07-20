package de.tomasgng.placeholders;

import de.tomasgng.DynamicSeasons;
import de.tomasgng.utils.managers.ConfigManager;
import de.tomasgng.utils.managers.SeasonManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CurrentSeasonExpansion extends PlaceholderExpansion {

    private final ConfigManager configManager = DynamicSeasons.getInstance().getConfigManager();
    private final SeasonManager seasonManager = DynamicSeasons.getInstance().getSeasonManager();

    @Override
    public @NotNull String getIdentifier() {
        return configManager.getCurrentSeasonPlaceholderName();
    }

    @Override
    public @NotNull String getAuthor() {
        return "TomasGnG";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        return configManager.getCurrentSeasonText(seasonManager.getCurrentSeason());
    }
}
