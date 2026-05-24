package me.sedattr.deluxeauctions.commands;

import me.sedattr.auctionsapi.AuctionHook;
import me.sedattr.auctionsapi.cache.AuctionCache;
import me.sedattr.auctionsapi.cache.CategoryCache;
import me.sedattr.deluxeauctions.DeluxeAuctions;
import me.sedattr.deluxeauctions.inventoryapi.inventory.InventoryAPI;
import me.sedattr.deluxeauctions.managers.Auction;
import me.sedattr.deluxeauctions.managers.Category;
import me.sedattr.deluxeauctions.menus.AuctionsMenu;
import me.sedattr.deluxeauctions.menus.BidsMenu;
import me.sedattr.deluxeauctions.menus.CreateMenu;
import me.sedattr.deluxeauctions.menus.MainMenu;
import me.sedattr.deluxeauctions.menus.ManageMenu;
import me.sedattr.deluxeauctions.menus.StatsMenu;
import me.sedattr.deluxeauctions.others.PlaceholderUtil;
import me.sedattr.deluxeauctions.others.TaskUtils;
import me.sedattr.deluxeauctions.others.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Default;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class AuctionAdminCommand {
    @Command("auctionadmin|deluxeauctionsadmin|ahadmin|aucadmin")
    public void usage(CommandSender commandSender) {
        handleUsage(commandSender);
    }

    @Command("auctionadmin|deluxeauctionsadmin|ahadmin|aucadmin cancel")
    public void cancelUsage(CommandSender commandSender) {
        handleCancel(commandSender, "");
    }

    @Command("auctionadmin|deluxeauctionsadmin|ahadmin|aucadmin cancel <auction>")
    public void cancel(CommandSender commandSender, @Argument(value = "auction", suggestions = "auctionAdminAuctions") String auction) {
        handleCancel(commandSender, auction);
    }

    @Command("auctionadmin|deluxeauctionsadmin|ahadmin|aucadmin convert")
    public void convertUsage(CommandSender commandSender) {
        handleConvert(commandSender, "");
    }

    @Command("auctionadmin|deluxeauctionsadmin|ahadmin|aucadmin convert <source>")
    public void convert(CommandSender commandSender, @Argument(value = "source", suggestions = "auctionAdminConverters") String source) {
        handleConvert(commandSender, source);
    }

    @Command("auctionadmin|deluxeauctionsadmin|ahadmin|aucadmin lock")
    public void lock(CommandSender commandSender) {
        handleLock(commandSender);
    }

    @Command("auctionadmin|deluxeauctionsadmin|ahadmin|aucadmin reload")
    public void reload(CommandSender commandSender) {
        handleReload(commandSender);
    }

    @Command("auctionadmin|deluxeauctionsadmin|ahadmin|aucadmin menu")
    public void menuUsage(CommandSender commandSender) {
        handleMenu(commandSender, "", null);
    }

    @Command("auctionadmin|deluxeauctionsadmin|ahadmin|aucadmin menu <player> [target]")
    public void menu(
            CommandSender commandSender,
            @Argument(value = "player", suggestions = "auctionAdminPlayers") String player,
            @Argument(value = "target", suggestions = "auctionAdminMenuTargets") @Default("") String target
    ) {
        handleMenu(commandSender, player, target);
    }

    @Suggestions("auctionAdminAuctions")
    public List<String> auctionSuggestions(CommandContext<CommandSender> context, String input) {
        return AuctionCache.getAuctions().keySet().stream().map(UUID::toString).sorted().toList();
    }

    @Suggestions("auctionAdminConverters")
    public List<String> converterSuggestions(CommandContext<CommandSender> context, String input) {
        return Arrays.asList("auctionmaster", "zauctionhouse");
    }

    @Suggestions("auctionAdminPlayers")
    public List<String> playerSuggestions(CommandContext<CommandSender> context, String input) {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).sorted().toList();
    }

    @Suggestions("auctionAdminMenuTargets")
    public List<String> menuTargetSuggestions(CommandContext<CommandSender> context, String input) {
        Set<String> suggestions = new LinkedHashSet<>(CategoryCache.getCategories().keySet().stream().sorted().toList());
        suggestions.addAll(Arrays.asList("manage", "bids", "create", "main", "stats"));
        return suggestions.stream().toList();
    }

    private boolean hasRootPermission(CommandSender commandSender) {
        if (!Utils.hasPermission(commandSender, "admin_commands", "command")) {
            Utils.sendMessage(commandSender, "no_permission");
            return false;
        }

        return true;
    }

    private boolean hasLoaded(CommandSender commandSender) {
        if (!hasRootPermission(commandSender))
            return false;

        if (!DeluxeAuctions.getInstance().loaded) {
            Utils.sendMessage(commandSender, "loading");
            return false;
        }

        return true;
    }

    private void handleUsage(CommandSender commandSender) {
        if (!hasRootPermission(commandSender))
            return;

        Utils.sendMessage(commandSender, "admin_usage", placeholderUtil());
    }

    private void handleCancel(CommandSender commandSender, String auctionInput) {
        if (!hasLoaded(commandSender))
            return;

        if (!Utils.hasPermission(commandSender, "admin_commands", "cancel")) {
            Utils.sendMessage(commandSender, "no_permission");
            return;
        }

        if (auctionInput == null || auctionInput.isBlank()) {
            Utils.sendMessage(commandSender, "admin_cancel_usage", placeholderUtil());
            return;
        }

        try {
            UUID uuid = UUID.fromString(auctionInput);
            Auction auction = AuctionCache.getAuction(uuid);
            if (auction == null)
                return;

            auction.setAuctionEndTime(ZonedDateTime.now().toInstant().getEpochSecond() - 1000);
            Utils.sendMessage(commandSender, "admin_cancelled", new PlaceholderUtil()
                    .addPlaceholder("%player_displayname%", auction.getAuctionOwnerDisplayName()));
        } catch (Exception e) {
            Utils.sendMessage(commandSender, "wrong_auction", null);
        }
    }

    private void handleConvert(CommandSender commandSender, String source) {
        if (!hasLoaded(commandSender))
            return;

        if (commandSender instanceof Player) {
            Utils.sendMessage(commandSender, "only_console");
            return;
        }

        if (!commandSender.isOp()) {
            Utils.sendMessage(commandSender, "no_permission");
            return;
        }

        if (source == null || source.isBlank()) {
            Utils.sendMessage(commandSender, "admin_convert_usage", placeholderUtil());
            return;
        }

        Utils.sendMessage(commandSender, "admin_convert_usage", placeholderUtil());
    }

    private void handleLock(CommandSender commandSender) {
        if (!hasLoaded(commandSender))
            return;

        if (!Utils.hasPermission(commandSender, "admin_commands", "lock")) {
            Utils.sendMessage(commandSender, "no_permission");
            return;
        }

        DeluxeAuctions.getInstance().locked = !DeluxeAuctions.getInstance().locked;
        for (Player player : Bukkit.getOnlinePlayers())
            if (!player.isOp() && InventoryAPI.hasInventory(player))
                TaskUtils.run(player, player::closeInventory);

        Utils.sendMessage(commandSender, DeluxeAuctions.getInstance().locked ? "locked" : "unlocked");
    }

    private void handleReload(CommandSender commandSender) {
        if (!hasLoaded(commandSender))
            return;

        if (!Utils.hasPermission(commandSender, "admin_commands", "reload")) {
            Utils.sendMessage(commandSender, "no_permission");
            return;
        }

        long start = System.currentTimeMillis();
        DeluxeAuctions.getInstance().reload();

        if (DeluxeAuctions.getInstance().multiServerManager != null)
            DeluxeAuctions.getInstance().multiServerManager.reload();

        Utils.sendMessage(commandSender, "reloaded", new PlaceholderUtil()
                .addPlaceholder("%reload_time%", String.valueOf(System.currentTimeMillis() - start)));
    }

    private void handleMenu(CommandSender commandSender, String playerName, String targetInput) {
        if (!hasLoaded(commandSender))
            return;

        if (!Utils.hasPermission(commandSender, "admin_commands", "menu")) {
            Utils.sendMessage(commandSender, "no_permission");
            return;
        }

        if (playerName == null || playerName.isBlank()) {
            Utils.sendMessage(commandSender, "admin_menu_usage", placeholderUtil());
            return;
        }

        Player targetPlayer = Bukkit.getPlayerExact(playerName);
        if (targetPlayer == null) {
            Utils.sendMessage(commandSender, "wrong_player", placeholderUtil()
                    .addPlaceholder("%player_name%", playerName));
            return;
        }

        if (targetInput != null && !targetInput.isBlank()) {
            Category category = AuctionHook.getCategory(targetInput);
            if (category != null) {
                new AuctionsMenu(targetPlayer).open(category.getName(), 1);
                return;
            }

            switch (targetInput) {
                case "manage":
                    new ManageMenu(targetPlayer).open(1, "command");
                    return;
                case "bids":
                    new BidsMenu(targetPlayer).open(1, "command");
                    return;
                case "create":
                    new CreateMenu(targetPlayer).open("command");
                    return;
                case "main":
                    new MainMenu(targetPlayer).open();
                    return;
                case "stats":
                    new StatsMenu(targetPlayer).open();
                    return;
                default:
                    break;
            }
        }

        AuctionHook.openMainMenu(targetPlayer);
    }

    private PlaceholderUtil placeholderUtil() {
        return new PlaceholderUtil()
                .addPlaceholder("%command_name%", "auctionadmin");
    }

}
