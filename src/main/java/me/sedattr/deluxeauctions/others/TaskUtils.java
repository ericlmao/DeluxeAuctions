package me.sedattr.deluxeauctions.others;

import me.sedattr.deluxeauctions.DeluxeAuctions;
import me.sedattr.deluxeauctions.inventoryapi.HInventory;
import me.sedattr.deluxeauctions.inventoryapi.inventory.InventoryAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.TimeUnit;

public final class TaskUtils {
    public static boolean isFolia;

    static {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
        } catch (final ClassNotFoundException e) {
            isFolia = false;
        }
    }

    public static void run(Runnable runnable) {
        if (isFolia) {
            DeluxeAuctions.getInstance().getServer().getGlobalRegionScheduler().execute(DeluxeAuctions.getInstance(), runnable);
        } else if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            new BukkitRunnable() {
                @Override
                public void run() {
                    runnable.run();
                }
            }.runTask(DeluxeAuctions.getInstance());
        }
    }

    public static void run(Player player, Runnable runnable) {
        if (player == null) {
            run(runnable);
            return;
        }

        if (isFolia) {
            player.getScheduler().execute(DeluxeAuctions.getInstance(), runnable, null, 1L);
        } else {
            run(runnable);
        }
    }

    public static void runAsync(Runnable runnable) {
        if (isFolia) {
            DeluxeAuctions.getInstance().getServer().getAsyncScheduler().runNow(DeluxeAuctions.getInstance(), scheduledTask -> runnable.run());
        } else {
            new BukkitRunnable() {
                @Override
                public void run() {
                    runnable.run();
                }
            }.runTaskAsynchronously(DeluxeAuctions.getInstance());
        }
    }

    public static void runLater(Runnable runnable, long delayTicks) {
        if (isFolia) {
            DeluxeAuctions.getInstance().getServer().getGlobalRegionScheduler().runDelayed(DeluxeAuctions.getInstance(), task -> runnable.run(), delayTicks);
        } else {
            new BukkitRunnable() {
                @Override
                public void run() {
                    runnable.run();
                }
            }.runTaskLater(DeluxeAuctions.getInstance(), delayTicks);
        }
    }

    public static void runLater(Player player, Runnable runnable, long delayTicks) {
        if (player == null) {
            runLater(runnable, delayTicks);
            return;
        }

        if (isFolia) {
            player.getScheduler().runDelayed(DeluxeAuctions.getInstance(), task -> runnable.run(), null, delayTicks);
        } else {
            runLater(runnable, delayTicks);
        }
    }

    public static void runLaterAsync(Runnable runnable, long delayTicks) {
        if (isFolia) {
            DeluxeAuctions.getInstance().getServer().getAsyncScheduler().runDelayed(DeluxeAuctions.getInstance(), scheduledTask -> runnable.run(), delayTicks * 50, TimeUnit.MILLISECONDS);
        } else {
            new BukkitRunnable() {
                @Override
                public void run() {
                    runnable.run();
                }
            }.runTaskLaterAsynchronously(DeluxeAuctions.getInstance(), delayTicks);
        }
    }

    public static void runTimerAsync(Runnable runnable, long delayTicks, long periodTicks) {
        if (isFolia) {
            DeluxeAuctions.getInstance().getServer().getAsyncScheduler().runAtFixedRate(DeluxeAuctions.getInstance(), scheduledTask -> runnable.run(), delayTicks * 50, periodTicks * 50, TimeUnit.MILLISECONDS);
        } else {
            new BukkitRunnable() {
                @Override
                public void run() {
                    runnable.run();
                }
            }.runTaskTimerAsynchronously(DeluxeAuctions.getInstance(), delayTicks, periodTicks);
        }
    }

    public static void runTimerAsync(Player player, String id, Runnable runnable, long delayTicks, long periodTicks) {
        if (isFolia) {
            player.getScheduler().runAtFixedRate(DeluxeAuctions.getInstance(), task -> runPlayerInventoryTimer(player, id, runnable, task), null, delayTicks, periodTicks);
        } else {
            new BukkitRunnable() {
                @Override
                public void run() {
                    HInventory inventory = InventoryAPI.getInventory(player);
                    if (inventory == null) {
                        cancel();
                        return;
                    }

                    String inventoryId = inventory.getId();
                    if (!inventoryId.equalsIgnoreCase(id)) {
                        if (id.equalsIgnoreCase("auctions") && inventoryId.equalsIgnoreCase("search")) {
                            runnable.run();
                            return;
                        }

                        cancel();
                        return;
                    }

                    runnable.run();
                }
            }.runTaskTimerAsynchronously(DeluxeAuctions.getInstance(), delayTicks, periodTicks);
        }
    }

    private static void runPlayerInventoryTimer(Player player, String id, Runnable runnable, io.papermc.paper.threadedregions.scheduler.ScheduledTask task) {
        HInventory inventory = InventoryAPI.getInventory(player);
        if (inventory == null) {
            cancelTask(task);
            return;
        }

        String inventoryId = inventory.getId();
        if (!inventoryId.equalsIgnoreCase(id)) {
            if (id.equalsIgnoreCase("auctions") && inventoryId.equalsIgnoreCase("search")) {
                runnable.run();
                return;
            }

            cancelTask(task);
            return;
        }

        runnable.run();
    }

    private static void cancelTask(io.papermc.paper.threadedregions.scheduler.ScheduledTask task) {
        if (!isFolia)
            return;

        task.cancel();
    }
}
