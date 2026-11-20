package com.gimdropvalues;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the familiar loot-broadcast and personal valuable-drop sentence forms.
 * The caller must select the parser for the server-generated message channel;
 * neither parser should be used for player-written chat.
 *
 * <p>All matching happens against visible text. Edits remove visible characters
 * only: the original colour, icon and other markup tags stay in their original
 * order, even when this leaves an empty colour span. This avoids corrupting a
 * tag around an item, player name, or the entire message.</p>
 */
public final class BroadcastValueFormatter
{
    private static final String NUMBER = "(?:0|[1-9][0-9]*|[1-9][0-9]{0,2}(?:,[0-9]{3})+)";

    private static final Pattern BROADCAST = Pattern.compile(
        "^\\|?[A-Za-z0-9 _-]{1,12} (?:has )?received "
            + "(?:a (?:rare )?drop|a clue item|special loot from a raid): "
            + "(?<item>[^\\r\\n]+?)"
            + "(?<gap> +)"
            + "(?<price>\\((?<value>" + NUMBER + ") (?<currency>coins?|gp)\\))"
            + "(?<suffix>(?: from [^\\r\\n]+| at " + NUMBER
            + " (?:killcount|XP|permits)[^\\r\\n]*|[.!](?: \\((?:killcount|kc|Kill count):? [^\\r\\n]+\\))?)?)$",
        Pattern.CASE_INSENSITIVE);

    // The game's own report, as exercised by RuneLite's ScreenshotPluginTest.
    // Unlike a clan broadcast this has no player name or source suffix.
    private static final Pattern GAME_DROP = Pattern.compile(
        "^Valuable drop: (?<item>[^\\r\\n]+?)"
            + "(?<gap> +)"
            + "(?<price>\\((?<value>" + NUMBER + ") (?<currency>coins?|gp)\\))"
            + "[.!]?$",
        Pattern.CASE_INSENSITIVE);

    private static final Pattern QUANTITY = Pattern.compile(
        "^(?<quantity>" + NUMBER + ") *[x\\u00d7] *(?<name>.+)$",
        Pattern.CASE_INSENSITIVE);

    private static final Pattern EXTRA_PRICE = Pattern.compile(
        "\\(" + NUMBER + " (?:coins?|gp)\\)", Pattern.CASE_INSENSITIVE);

    private BroadcastValueFormatter()
    {
    }

    /** Parses a GIM/clan loot broadcast; returns empty for other sentence forms. */
    public static Optional<ParsedDrop> parse(String message)
    {
        return parse(message, BROADCAST);
    }

    /**
     * Parses a valuable-drop notice or a named received-drop report in the
     * game's own chat. The caller must restrict this to game-system channels.
     */
    public static Optional<ParsedDrop> parseGameMessage(String message)
    {
        Optional<ParsedDrop> valuableDrop = parse(message, GAME_DROP);
        return valuableDrop.isPresent() ? valuableDrop : parse(message);
    }

    /** Returns empty for an unrecognised or ambiguous sentence. */
    private static Optional<ParsedDrop> parse(String message, Pattern pattern)
    {
        if (message == null || message.isEmpty())
        {
            return Optional.empty();
        }

        VisibleText visible = VisibleText.from(message);
        if (visible == null)
        {
            return Optional.empty();
        }
        Matcher loot = pattern.matcher(visible.text);
        if (!loot.matches())
        {
            return Optional.empty();
        }

        String itemName = loot.group("item").trim();
        long quantity = 1L;
        Matcher amount = QUANTITY.matcher(itemName);
        if (amount.matches())
        {
            try
            {
                quantity = Long.parseLong(amount.group("quantity").replace(",", ""));
            }
            catch (NumberFormatException ex)
            {
                return Optional.empty();
            }
            if (quantity < 1)
            {
                return Optional.empty();
            }
            itemName = amount.group("name").trim();
        }

        // A second price-looking fragment cannot be a single item name. Avoid
        // silently converting only the final price of a compound loot message.
        if (itemName.isEmpty() || itemName.contains(":")
            || EXTRA_PRICE.matcher(itemName).find())
        {
            return Optional.empty();
        }

        return Optional.of(new ParsedDrop(
            message, visible.rawIndexes, itemName, quantity,
            loot.start("gap"), loot.end("price"),
            loot.start("value"), loot.end("currency")));
    }

    /** Immutable parse result. It always retains the original message. */
    public static final class ParsedDrop
    {
        private final String original;
        private final int[] rawIndexes;
        private final String itemName;
        private final long quantity;
        private final int hideStart;
        private final int hideEnd;
        private final int replaceStart;
        private final int replaceEnd;

        private ParsedDrop(String original, int[] rawIndexes, String itemName,
            long quantity, int hideStart, int hideEnd,
            int replaceStart, int replaceEnd)
        {
            this.original = original;
            this.rawIndexes = rawIndexes;
            this.itemName = itemName;
            this.quantity = quantity;
            this.hideStart = hideStart;
            this.hideEnd = hideEnd;
            this.replaceStart = replaceStart;
            this.replaceEnd = replaceEnd;
        }

        public String getItemName()
        {
            return itemName;
        }

        public long getQuantity()
        {
            return quantity;
        }

        /** Broadcasts carry text; an icon tag is never treated as an item ID. */
        public OptionalInt getItemId()
        {
            return OptionalInt.empty();
        }

        public String hideValue()
        {
            return replaceVisible(hideStart, hideEnd, "");
        }

        /** The caller supplies the total for the entire item stack. */
        public String withHighAlchValue(long total)
        {
            if (total < 0)
            {
                throw new IllegalArgumentException("High alchemy value must not be negative");
            }
            return replaceVisible(replaceStart, replaceEnd,
                String.format(Locale.US, "%,d", total) + " gp");
        }

        private String replaceVisible(int start, int end, String replacement)
        {
            StringBuilder output = new StringBuilder(original.length() + replacement.length());
            int cursor = 0;
            for (int visibleIndex = start; visibleIndex < end; visibleIndex++)
            {
                int rawIndex = rawIndexes[visibleIndex];
                output.append(original, cursor, rawIndex);
                if (visibleIndex == start)
                {
                    output.append(replacement);
                }
                cursor = rawIndex + 1;
            }
            output.append(original, cursor, original.length());
            return output.toString();
        }
    }

    private static final class VisibleText
    {
        private final String text;
        private final int[] rawIndexes;

        private VisibleText(String text, int[] rawIndexes)
        {
            this.text = text;
            this.rawIndexes = rawIndexes;
        }

        private static VisibleText from(String raw)
        {
            StringBuilder text = new StringBuilder(raw.length());
            List<Integer> indexes = new ArrayList<>();
            for (int i = 0; i < raw.length(); i++)
            {
                char c = raw.charAt(i);
                if (c == '<')
                {
                    int close = raw.indexOf('>', i + 1);
                    if (close < 0)
                    {
                        return null;
                    }
                    // A line break is real layout/content, not harmless styling.
                    String tag = raw.substring(i + 1, close);
                    if ("br".equalsIgnoreCase(tag) || "lt".equalsIgnoreCase(tag)
                        || "gt".equalsIgnoreCase(tag))
                    {
                        return null;
                    }
                    i = close;
                }
                else
                {
                    text.append(c == '\u00a0' ? ' ' : c);
                    indexes.add(i);
                }
            }
            int[] rawIndexes = new int[indexes.size()];
            for (int i = 0; i < indexes.size(); i++)
            {
                rawIndexes[i] = indexes.get(i);
            }
            return new VisibleText(text.toString(), rawIndexes);
        }
    }
}
