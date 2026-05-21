package me.sedattr.auctionsapi.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class PlayerNameCache {
    private static final Cache<UUID, String> names = CacheBuilder.newBuilder()
            .maximumSize(10_000)
            .expireAfterAccess(12, TimeUnit.HOURS)
            .build();

    private PlayerNameCache() {
    }

    public static String resolveName(UUID uuid, String fallback) {
        String cleanFallback = fallback != null ? fallback : "";
        if (uuid == null)
            return cleanFallback;

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            String name = player.getName();
            names.put(uuid, name);
            return name;
        }

        String cached = names.getIfPresent(uuid);
        if (cached != null)
            return !cached.isEmpty() ? cached : cleanFallback;

        String name = "";
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        String offlineName = offlinePlayer.getName();
        if (offlineName != null)
            name = offlineName;

        names.put(uuid, name);
        return !name.isEmpty() ? name : cleanFallback;
    }
}
