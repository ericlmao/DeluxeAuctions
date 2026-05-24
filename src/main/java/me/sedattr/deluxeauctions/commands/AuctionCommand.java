package me.sedattr.deluxeauctions.commands;

import me.sedattr.auctionsapi.AuctionHook;
import me.sedattr.auctionsapi.cache.AuctionCache;
import me.sedattr.auctionsapi.cache.CategoryCache;
import me.sedattr.auctionsapi.cache.PlayerCache;
import me.sedattr.auctionsapi.events.ItemPreviewEvent;
import me.sedattr.deluxeauctions.DeluxeAuctions;
import me.sedattr.deluxeauctions.managers.Auction;
import me.sedattr.deluxeauctions.managers.AuctionType;
import me.sedattr.deluxeauctions.managers.PlayerPreferences;
import me.sedattr.deluxeauctions.menus.AuctionsMenu;
import me.sedattr.deluxeauctions.menus.BidsMenu;
import me.sedattr.deluxeauctions.menus.BinViewMenu;
import me.sedattr.deluxeauctions.menus.CreateMenu;
import me.sedattr.deluxeauctions.menus.ManageMenu;
import me.sedattr.deluxeauctions.menus.NormalViewMenu;
import me.sedattr.deluxeauctions.menus.ViewAuctionsMenu;
import me.sedattr.deluxeauctions.others.AdventureText;
import me.sedattr.deluxeauctions.others.PlaceholderUtil;
import me.sedattr.deluxeauctions.others.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.incendo.cloud.annotation.specifier.Greedy;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Default;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class AuctionCommand {
    private final HashMap<Player, Long> commandCooldown = new HashMap<>();

    @Command("auction|deluxeauctions|ah|auc")
    public void root(CommandSender commandSender) {
        handleRoot(commandSender);
    }

    @Command("auction|deluxeauctions|ah|auc info")
    public void info(CommandSender commandSender) {
        handleInfo(commandSender);
    }

    @Command("auction|deluxeauctions|ah|auc bids")
    public void bids(CommandSender commandSender) {
        handleBids(commandSender);
    }

    @Command("auction|deluxeauctions|ah|auc menu|open")
    public void menu(CommandSender commandSender) {
        handleMenu(commandSender);
    }

    @Command("auction|deluxeauctions|ah|auc manage")
    public void manage(CommandSender commandSender) {
        handleManage(commandSender);
    }

    @Command("auction|deluxeauctions|ah|auc auctions [category]")
    public void auctions(CommandSender commandSender, @Argument(value = "category", suggestions = "auctionCategories") @Default("") String category) {
        handleAuctions(commandSender, category);
    }

    @Command("auction|deluxeauctions|ah|auc view")
    public void viewUsage(CommandSender commandSender) {
        handleView(commandSender, "");
    }

    @Command("auction|deluxeauctions|ah|auc view <target>")
    public void view(CommandSender commandSender, @Argument(value = "target", suggestions = "auctionPlayers") String target) {
        handleView(commandSender, target);
    }

    @Command("auction|deluxeauctions|ah|auc search")
    public void searchUsage(CommandSender commandSender) {
        handleSearch(commandSender, "");
    }

    @Command("auction|deluxeauctions|ah|auc search <query>")
    public void search(CommandSender commandSender, @Argument("query") @Greedy String query) {
        handleSearch(commandSender, query);
    }

    @Command("auction|deluxeauctions|ah|auc sell")
    public void sellUsage(CommandSender commandSender) {
        handleSell(commandSender, "", "");
    }

    @Command("auction|deluxeauctions|ah|auc sell <price> [details]")
    public void sell(
            CommandSender commandSender,
            @Argument("price") String price,
            @Argument("details") @Greedy @Default("") String details
    ) {
        handleSell(commandSender, price, details);
    }

    @Suggestions("auctionCategories")
    public List<String> categorySuggestions(CommandContext<CommandSender> context, String input) {
        return CategoryCache.getCategories().keySet().stream().sorted().toList();
    }

    @Suggestions("auctionPlayers")
    public List<String> playerSuggestions(CommandContext<CommandSender> context, String input) {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).sorted().toList();
    }

    private boolean commonRootChecks(CommandSender commandSender) {
        if (!Utils.hasPermission(commandSender, "player_commands", "command")) {
            Utils.sendMessage(commandSender, "no_permission");
            return false;
        }

        if (!(commandSender instanceof Player)) {
            Utils.sendMessage(commandSender, "not_player");
            return false;
        }

        return true;
    }

    private Player checkedPlayer(CommandSender commandSender) {
        if (!commonRootChecks(commandSender))
            return null;

        Player player = (Player) commandSender;
        long cooldown = commandCooldown.getOrDefault(player, 0L);
        if (cooldown > 0) {
            long time = ZonedDateTime.now().toInstant().getEpochSecond() - cooldown;
            if (time < 1.5) {
                Utils.sendMessage(player, "command_cooldown");
                return null;
            }
        }
        commandCooldown.put(player, ZonedDateTime.now().toInstant().getEpochSecond());
        return player;
    }

    private Player checkedAuctionPlayer(CommandSender commandSender) {
        Player player = checkedPlayer(commandSender);
        if (player == null)
            return null;

        if (DeluxeAuctions.getInstance().locked && !player.isOp()) {
            Utils.sendMessage(player, "closed");
            return null;
        }

        if (!DeluxeAuctions.getInstance().loaded) {
            Utils.sendMessage(player, "loading");
            return null;
        }

        if (Utils.isDisabledWorld(player.getWorld().getName())) {
            Utils.sendMessage(player, "disabled_world");
            return null;
        }

        if (Utils.isLaggy(player)) {
            Utils.sendMessage(player, "laggy");
            return null;
        }

        return player;
    }

    private void handleRoot(CommandSender commandSender) {
        Player player = checkedPlayer(commandSender);
        if (player == null)
            return;

        PlaceholderUtil placeholderUtil = new PlaceholderUtil()
                .addPlaceholder("%command_name%", "auction");
        String menuToOpen = DeluxeAuctions.getInstance().configFile.getString("settings.menu_to_open_directly");
        if (menuToOpen != null && !menuToOpen.isEmpty()) {
            if (menuToOpen.equalsIgnoreCase("auctions")) {
                String category = PlayerCache.getPlayers().containsKey(player.getUniqueId()) ? PlayerCache.getPreferences(player.getUniqueId()).getCategory().getName() : DeluxeAuctions.getInstance().category;
                new AuctionsMenu(player).open(category, 1);
            } else {
                AuctionHook.openMainMenu(player);
            }
            return;
        }

        Utils.sendMessage(player, "player_usage", placeholderUtil);
    }

    private void handleInfo(CommandSender commandSender) {
        Player player = checkedAuctionPlayer(commandSender);
        if (player == null)
            return;

        List<String> lines = new ArrayList<>(
                Arrays.asList("&8[&6DeluxeAuctions&8] &6Plugin Information",
                        "&8- &fDeluxeAuctions &eis made by &fSedatTR&e.",
                        "&8- &eDiscord Support Server: &fdiscord.gg/nchk86TKMT",
                        "&8- &eCurrent Plugin Version: &fv" + DeluxeAuctions.getInstance().getDescription().getVersion()));

        for (String line : lines)
            player.sendMessage(AdventureText.component(line));
    }

    private void handleBids(CommandSender commandSender) {
        Player player = checkedAuctionPlayer(commandSender);
        if (player == null)
            return;

        if (!Utils.hasPermission(commandSender, "player_commands", "bids")) {
            Utils.sendMessage(commandSender, "no_permission");
            return;
        }

        new BidsMenu(player).open(1, "command");
    }

    private void handleMenu(CommandSender commandSender) {
        Player player = checkedAuctionPlayer(commandSender);
        if (player == null)
            return;

        if (!Utils.hasPermission(commandSender, "player_commands", "menu")) {
            Utils.sendMessage(commandSender, "no_permission");
            return;
        }

        AuctionHook.openMainMenu(player);
    }

    private void handleManage(CommandSender commandSender) {
        Player player = checkedAuctionPlayer(commandSender);
        if (player == null)
            return;

        if (!Utils.hasPermission(commandSender, "player_commands", "manage")) {
            Utils.sendMessage(commandSender, "no_permission");
            return;
        }

        new ManageMenu(player).open(1, "command");
    }

    private void handleSearch(CommandSender commandSender, String search) {
        Player player = checkedAuctionPlayer(commandSender);
        if (player == null)
            return;

        if (!Utils.hasPermission(commandSender, "player_commands", "search")) {
            Utils.sendMessage(commandSender, "no_permission");
            return;
        }

        if (search == null || search.isBlank()) {
            Utils.sendMessage(commandSender, "search_usage");
            return;
        }

        PlayerPreferences playerAuction = PlayerCache.getPreferences(player.getUniqueId());
        playerAuction.setSearch(search);

        new AuctionsMenu(player).open("search", 1);
    }

    private void handleAuctions(CommandSender commandSender, String inputCategory) {
        Player player = checkedAuctionPlayer(commandSender);
        if (player == null)
            return;

        if (!Utils.hasPermission(commandSender, "player_commands", "auctions")) {
            Utils.sendMessage(commandSender, "no_permission");
            return;
        }

        String category;
        if (inputCategory != null && CategoryCache.getCategories().containsKey(inputCategory))
            category = inputCategory;
        else
            category = PlayerCache.getPlayers().containsKey(player.getUniqueId()) ? PlayerCache.getPreferences(player.getUniqueId()).getCategory().getName() : DeluxeAuctions.getInstance().category;

        new AuctionsMenu(player).open(category, 1);
    }

    private void handleView(CommandSender commandSender, String targetInput) {
        Player player = checkedAuctionPlayer(commandSender);
        if (player == null)
            return;

        if (!Utils.hasPermission(commandSender, "player_commands", "view")) {
            Utils.sendMessage(commandSender, "no_permission");
            return;
        }

        PlaceholderUtil placeholderUtil = new PlaceholderUtil()
                .addPlaceholder("%command_name%", "auction");
        if (targetInput == null || targetInput.isBlank()) {
            Utils.sendMessage(player, "view_usage", placeholderUtil);
            return;
        }

        try {
            UUID uuid = UUID.fromString(targetInput);
            Auction auction = AuctionCache.getAuction(uuid);
            if (auction != null) {
                if (auction.getAuctionType().equals(AuctionType.BIN))
                    new BinViewMenu(player, auction).open("command");
                else
                    new NormalViewMenu(player, auction).open("command");
                return;
            }

            Auction endedAuction = AuctionCache.getEndedAuction(uuid);
            if (endedAuction != null) {
                Utils.sendMessage(player, "ended_auction", placeholderUtil);
                return;
            }

            Player target = Bukkit.getPlayer(uuid);
            if (target == null) {
                Utils.sendMessage(player, "view_usage", placeholderUtil);
                return;
            }

            new ViewAuctionsMenu(player, target).open(1);
            return;
        } catch (Exception e) {
            try {
                OfflinePlayer target = Bukkit.getOfflinePlayer(targetInput);
                new ViewAuctionsMenu(player, target.getUniqueId(), targetInput).open(1);
            } catch (Exception ee) {
                Utils.sendMessage(player, "view_usage", placeholderUtil);
            }
        }
    }

    private void handleSell(CommandSender commandSender, String priceInput, String details) {
        Player player = checkedAuctionPlayer(commandSender);
        if (player == null)
            return;

        PlaceholderUtil placeholderUtil = new PlaceholderUtil()
                .addPlaceholder("%command_name%", "auction");
        if (!Utils.hasPermission(player, "player_commands", "sell")) {
            Utils.sendMessage(player, "no_permission");
            return;
        }

        if (priceInput == null || priceInput.isBlank()) {
            Utils.sendMessage(player, "sell_usage", placeholderUtil);
            return;
        }

        int slot = player.getInventory().getHeldItemSlot();
        ItemStack item = slot >= 0 ? player.getInventory().getItem(slot) : null;

        if (item == null || item.getType() == Material.AIR) {
            Utils.sendMessage(player, "wrong_item", placeholderUtil);
            return;
        }

        String sellable = AuctionHook.isSellable(player, item);
        if (!sellable.isEmpty()) {
            Utils.sendMessage(player, sellable);
            return;
        }

        double price;
        double reversedPrice = DeluxeAuctions.getInstance().numberFormat.reverseFormat(priceInput);
        if (reversedPrice > 1)
            price = reversedPrice;
        else {
            try {
                price = Double.parseDouble(priceInput);
            } catch (Exception e) {
                Utils.sendMessage(player, "wrong_price", placeholderUtil);
                return;
            }
        }

        if (price <= 0) {
            Utils.sendMessage(player, "wrong_price", placeholderUtil);
            return;
        }

        double priceLimit = AuctionHook.getPriceLimit(player, "price_limit");
        if (price > priceLimit) {
            Utils.sendMessage(player, "reached_price_limit", new PlaceholderUtil()
                    .addPlaceholder("%price_limit%", DeluxeAuctions.getInstance().numberFormat.format(priceLimit)));
            return;
        }

        String[] remainingArgs = parseArgs(details);
        String type = remainingArgs.length == 0 ? DeluxeAuctions.getInstance().configFile.getString("settings.default_type", "bin") : remainingArgs[remainingArgs.length - 1];
        boolean explicitType = type.equalsIgnoreCase("bin") || type.equalsIgnoreCase("normal");
        String[] durationArgs = explicitType ? Arrays.copyOf(remainingArgs, remainingArgs.length - 1) : remainingArgs;
        int time = DeluxeAuctions.getInstance().createTime;
        if (durationArgs.length > 0) {
            StringBuilder times = new StringBuilder();
            for (String remainingArg : durationArgs)
                times.append(remainingArg).append(" ");

            try {
                time = DeluxeAuctions.getInstance().timeFormat.convertTime(times.toString());
            } catch (Exception e) {
                Utils.sendMessage(player, "wrong_duration", placeholderUtil);
                return;
            }
        }

        if (time <= 0) {
            Utils.sendMessage(player, "wrong_duration", placeholderUtil);
            return;
        }

        int limit = AuctionHook.getLimit(player, "duration_limit");
        if (time > limit) {
            Utils.sendMessage(player, "reached_duration_limit", new PlaceholderUtil()
                    .addPlaceholder("%duration_limit%", String.valueOf(limit)));
            return;
        }

        if (!explicitType)
            type = DeluxeAuctions.getInstance().configFile.getString("settings.default_type", "bin");

        if (!type.equalsIgnoreCase("bin") && !type.equalsIgnoreCase("normal")) {
            Utils.sendMessage(player, "wrong_type", placeholderUtil);
            return;
        }

        if (AuctionHook.isAuctionTypeDisabled(type)) {
            Utils.sendMessage(player, "disabled_auction_type", new PlaceholderUtil()
                    .addPlaceholder("%auction_type%", priceInput.toUpperCase(Locale.ENGLISH)));
            return;
        }

        ItemPreviewEvent event = new ItemPreviewEvent(player, item);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return;

        PlayerPreferences playerAuction = PlayerCache.getPreferences(player.getUniqueId());
        boolean status = playerAuction.updateCreateItem(player, slot, true);
        if (!status)
            return;

        playerAuction.setCreateType(AuctionType.valueOf(type.toUpperCase(Locale.ENGLISH)));
        playerAuction.setCreatePrice(price);
        playerAuction.setCreateTime(time);

        new CreateMenu(player).open("command");
    }

    private String[] parseArgs(String rawArgs) {
        if (rawArgs == null || rawArgs.isBlank())
            return new String[0];

        return rawArgs.trim().split("\\s+");
    }

}
