package de.tomasgng.placeholders;

import de.tomasgng.DynamicSeasons;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DurationExpansion extends PlaceholderExpansion {
    @Override
    public @NotNull String getIdentifier() {
        return DynamicSeasons.getInstance().getConfigManager().getDurationPlaceholderName();
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
        return DynamicSeasons.getInstance().getSeasonManager().getFormattedDuration();
    }
}
