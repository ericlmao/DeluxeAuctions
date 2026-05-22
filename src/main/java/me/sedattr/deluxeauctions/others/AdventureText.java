package me.sedattr.deluxeauctions.others;

import me.sedattr.deluxeauctions.DeluxeAuctions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AdventureText {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private static final Pattern HEX_PATTERN = Pattern.compile("(?i)(?:&?#|§#)([a-f0-9]{6})|&x((&[a-f0-9]){6})|§x((§[a-f0-9]){6})");

    private AdventureText() {
    }

    public static Component component(String input) {
        if (input == null || input.isEmpty())
            return Component.empty();

        String normalized = normalizeLegacyHex(input);
        try {
            return MINI_MESSAGE.deserialize(normalized);
        } catch (Exception ignored) {
            return LEGACY_AMPERSAND.deserialize(input.replace('§', '&'));
        }
    }

    public static Component component(CommandSender sender, String input, PlaceholderUtil placeholderUtil) {
        String replaced = Utils.replacePlaceholders(input == null ? "" : input, placeholderUtil);
        replaced = placeholderApi(sender, replaced);
        return component(replaced);
    }

    public static List<Component> components(CommandSender sender, List<String> lines, PlaceholderUtil placeholderUtil) {
        List<Component> components = new ArrayList<>();
        for (String line : lines)
            components.add(component(sender, line, placeholderUtil));
        return components;
    }

    public static String legacy(String input) {
        return LEGACY_SECTION.serialize(component(input));
    }

    public static String legacy(Component component) {
        return LEGACY_SECTION.serialize(component == null ? Component.empty() : component);
    }

    public static String plain(String input) {
        return PLAIN.serialize(component(input));
    }

    public static String plain(Component component) {
        return PLAIN.serialize(component == null ? Component.empty() : component);
    }

    private static String placeholderApi(CommandSender sender, String message) {
        if (!(sender instanceof Player player))
            return message;
        if (!DeluxeAuctions.getInstance().placeholderApi)
            return message;

        return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, message);
    }

    private static String normalizeLegacyHex(String input) {
        Matcher matcher = HEX_PATTERN.matcher(input);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex;
            if (matcher.group(1) != null) {
                hex = matcher.group(1);
            } else if (matcher.group(2) != null) {
                hex = matcher.group(2).replace("&", "");
            } else {
                hex = matcher.group(4).replace("§", "");
            }
            matcher.appendReplacement(buffer, "<#" + hex + ">");
        }
        matcher.appendTail(buffer);
        return buffer.toString()
                .replace('§', '&')
                .replace("&0", "<black>")
                .replace("&1", "<dark_blue>")
                .replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>")
                .replace("&4", "<dark_red>")
                .replace("&5", "<dark_purple>")
                .replace("&6", "<gold>")
                .replace("&7", "<gray>")
                .replace("&8", "<dark_gray>")
                .replace("&9", "<blue>")
                .replace("&a", "<green>")
                .replace("&A", "<green>")
                .replace("&b", "<aqua>")
                .replace("&B", "<aqua>")
                .replace("&c", "<red>")
                .replace("&C", "<red>")
                .replace("&d", "<light_purple>")
                .replace("&D", "<light_purple>")
                .replace("&e", "<yellow>")
                .replace("&E", "<yellow>")
                .replace("&f", "<white>")
                .replace("&F", "<white>")
                .replace("&k", "<obfuscated>")
                .replace("&K", "<obfuscated>")
                .replace("&l", "<bold>")
                .replace("&L", "<bold>")
                .replace("&m", "<strikethrough>")
                .replace("&M", "<strikethrough>")
                .replace("&n", "<underlined>")
                .replace("&N", "<underlined>")
                .replace("&o", "<italic>")
                .replace("&O", "<italic>")
                .replace("&r", "<reset>")
                .replace("&R", "<reset>");
    }
}
