package me.sedattr.deluxeauctions.managers;

import lombok.Getter;
import lombok.Setter;
import me.sedattr.auctionsapi.cache.PlayerNameCache;
import me.sedattr.deluxeauctions.others.Utils;
import org.bukkit.entity.Player;

import java.time.ZonedDateTime;
import java.util.UUID;

@Getter
public class PlayerBid {
    private final UUID uuid;
    private final long bidTime;
    private final double bidPrice;
    private final UUID bidOwner;
    private final String bidOwnerName;
    private final String bidOwnerDisplayName;
    @Setter private boolean collected = false;

    public PlayerBid(Player player, double price) {
        this.uuid = UUID.randomUUID();
        this.bidOwner = player.getUniqueId();
        this.bidOwnerName = player.getName();
        String displayName = Utils.getDisplayName(player);
        this.bidOwnerDisplayName = !displayName.isEmpty() ? displayName : player.getName();
        this.bidPrice = price;
        this.bidTime = ZonedDateTime.now().toInstant().getEpochSecond();
    }

    public PlayerBid(Player player, double price, boolean collected) {
        this.uuid = UUID.randomUUID();
        this.bidOwner = player.getUniqueId();
        this.bidOwnerName = player.getName();
        String displayName = Utils.getDisplayName(player);
        this.bidOwnerDisplayName = !displayName.isEmpty() ? displayName : player.getName();
        this.bidPrice = price;
        this.bidTime = ZonedDateTime.now().toInstant().getEpochSecond();
        this.collected = collected;
    }

    public PlayerBid(UUID player, double price, boolean collected) {
        this.uuid = UUID.randomUUID();
        this.bidOwner = player;
        this.bidPrice = price;
        this.bidTime = ZonedDateTime.now().toInstant().getEpochSecond();
        this.collected = collected;

        this.bidOwnerName = PlayerNameCache.resolveName(player, "");
        this.bidOwnerDisplayName = this.bidOwnerName;
    }

    public PlayerBid(UUID player, String displayName, double price, long time) {
        this(player, displayName, displayName, price, time);
    }

    public PlayerBid(UUID player, String name, String displayName, double price, long time) {
        this.uuid = UUID.randomUUID();
        this.bidOwner = player;
        this.bidOwnerName = name != null && !name.isEmpty() ? name : displayName != null ? displayName : "";
        this.bidOwnerDisplayName = displayName != null && !displayName.isEmpty() ? displayName : this.bidOwnerName;
        this.bidPrice = price;
        this.bidTime = time;
    }

    public PlayerBid(UUID uuid, UUID player, String displayName, double price, long time, boolean collected) {
        this(uuid, player, displayName, displayName, price, time, collected);
    }

    public PlayerBid(UUID uuid, UUID player, String name, String displayName, double price, long time, boolean collected) {
        this.uuid = uuid;
        this.bidOwner = player;
        this.bidOwnerName = name != null && !name.isEmpty() ? name : displayName != null ? displayName : "";
        this.bidOwnerDisplayName = displayName != null && !displayName.isEmpty() ? displayName : this.bidOwnerName;
        this.bidPrice = price;
        this.bidTime = time;
        this.collected = collected;
    }

    @Override
    public String toString() {
        return uuid + "," + this.bidOwner + "," + this.bidOwnerName + "," + this.bidOwnerDisplayName + "," + this.bidPrice + "," + this.bidTime + "," + this.collected;
    }
}
