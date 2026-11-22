package com.gimdropvalues;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BroadcastValueFormatterTest
{
    private BroadcastValueFormatter.ParsedDrop parse(String input)
    {
        assertTrue("Expected a supported broadcast: " + input,
            BroadcastValueFormatter.parse(input).isPresent());
        return BroadcastValueFormatter.parse(input).get();
    }

    private String visible(String text)
    {
        return text.replaceAll("<[^>]*>", "");
    }

    private BroadcastValueFormatter.ParsedDrop parseGameMessage(String input)
    {
        assertTrue("Expected a supported game drop report: " + input,
            BroadcastValueFormatter.parseGameMessage(input).isPresent());
        return BroadcastValueFormatter.parseGameMessage(input).get();
    }

    @Test
    public void formatsGameValuableDropFromRuneLiteFixture()
    {
        // Official RuneLite ScreenshotPluginTest fixture, with a caller-supplied
        // High Alch value. The game's report normally has no final full stop.
        String input = "<col=ef1020>Valuable drop: Rune scimitar (25,600 coins)</col>";
        BroadcastValueFormatter.ParsedDrop drop = parseGameMessage(input);
        assertEquals("Rune scimitar", drop.getItemName());
        assertEquals(1L, drop.getQuantity());
        assertEquals("<col=ef1020>Valuable drop: Rune scimitar</col>", drop.hideValue());
        assertEquals("<col=ef1020>Valuable drop: Rune scimitar (15,360 gp)</col>",
            drop.withHighAlchValue(15360));
        assertFalse(drop.withHighAlchValue(15360).contains("HA"));
    }

    @Test
    public void formatsGameStackFromRuneLiteFixture()
    {
        // Exact game-report format from RuneLite's ScreenshotPluginTest.
        BroadcastValueFormatter.ParsedDrop drop = parseGameMessage(
            "<col=ef1020>Valuable drop: 6 x Bronze arrow (42 coins)</col>");
        assertEquals("Bronze arrow", drop.getItemName());
        assertEquals(6L, drop.getQuantity());
        assertEquals("<col=ef1020>Valuable drop: 6 x Bronze arrow</col>", drop.hideValue());
        assertEquals("<col=ef1020>Valuable drop: 6 x Bronze arrow (6 gp)</col>",
            drop.withHighAlchValue(6));
    }

    @Test
    public void keepsPersonalItemParenthesesQuantityAndTags()
    {
        String input = "<col=ef1020>Valuable drop: <col=ff9040>2 x Dragon dagger(p++)"
            + "</col> (<col=ffff00>60,002</col> coins).</col>";
        BroadcastValueFormatter.ParsedDrop drop = parseGameMessage(input);
        assertEquals("Dragon dagger(p++)", drop.getItemName());
        assertEquals(2L, drop.getQuantity());
        assertEquals("Valuable drop: 2 x Dragon dagger(p++).", visible(drop.hideValue()));
        assertEquals("Valuable drop: 2 x Dragon dagger(p++) (36,000 gp).",
            visible(drop.withHighAlchValue(36000)));
        assertEquals("<col=ef1020>Valuable drop: <col=ff9040>2 x Dragon dagger(p++)"
            + "</col> (<col=ffff00>36,000 gp</col>).</col>", drop.withHighAlchValue(36000));
    }

    @Test
    public void supportsPersonalCommaQuantitiesAndNonbreakingSpaces()
    {
        BroadcastValueFormatter.ParsedDrop drop = parseGameMessage(
            "Valuable drop: 1,200\u00a0x\u00a0Nature rune\u00a0(100,000 coins)");
        assertEquals("Nature rune", drop.getItemName());
        assertEquals(1200L, drop.getQuantity());
        assertEquals("Valuable drop: 1,200\u00a0x\u00a0Nature rune", drop.hideValue());
    }

    @Test
    public void keepsPersonalPrefixOutOfBroadcastParser()
    {
        assertFalse(BroadcastValueFormatter.parse(
            "Valuable drop: Rune platebody (39,000 coins)").isPresent());
    }

    @Test
    public void supportsPriceBearingReceivedDropReportsInGameChat()
    {
        // RuneLite's ChatNotificationsPluginTest uses GAMEMESSAGE for a named
        // received-drop report. These valued variants are constructed fixtures.
        BroadcastValueFormatter.ParsedDrop named = parseGameMessage(
            "Logic Knot received a drop: Adamant longsword (3,200 coins).");
        assertEquals("Adamant longsword", named.getItemName());
        assertEquals("Logic Knot received a drop: Adamant longsword.", named.hideValue());
        assertEquals("Logic Knot received a drop: Adamant longsword (1,920 gp).",
            named.withHighAlchValue(1920));

        BroadcastValueFormatter.ParsedDrop own = parseGameMessage(
            "You received a drop: 2 x Rune platebody (100,000 coins).");
        assertEquals("Rune platebody", own.getItemName());
        assertEquals(2L, own.getQuantity());
        assertEquals("You received a drop: 2 x Rune platebody (78,000 gp).",
            own.withHighAlchValue(78000));
    }

    @Test
    public void rejectsUnrelatedValuelessOrAmbiguousGameMessages()
    {
        String[] inputs = {
            "Untradeable drop: Rusty sword",
            "<col=ef1020>Untradeable drop: Rusty sword",
            "Valuable drop: Rune platebody",
            "Your loot is worth around 100,000 coins.",
            "Your treasure is worth around 100,000 coins!",
            "You have been awarded 100,000 coins.",
            "New item added to your collection log: Rune platebody",
            "Jurad: Valuable drop: Rune platebody (39,000 coins)",
            "Valuable drop: Rune platebody (1,23 coins)",
            "Valuable drop: Rune platebody (39,000 coins). unrelated text",
            "Valuable drop: Rune platebody (39,000 coins), Rune sword (12,000 coins)",
            "Valuable drop: Rune platebody (39,000 coins)<br>Second line",
            "Valuable drop: Rune platebody (39,000 coins)<unclosed",
            "Valuable drop: 0 x Rune platebody (39,000 coins)",
            "Valuable drop: 9,999,999,999,999,999,999 x Rune platebody (39,000 coins)"
        };
        for (String input : inputs)
        {
            assertFalse("Must leave unchanged: " + input,
                BroadcastValueFormatter.parseGameMessage(input).isPresent());
        }
        assertFalse(BroadcastValueFormatter.parseGameMessage(null).isPresent());
        assertFalse(BroadcastValueFormatter.parseGameMessage("").isPresent());
    }

    @Test
    public void hidesOnlyPriceInOrdinaryBroadcast()
    {
        BroadcastValueFormatter.ParsedDrop drop = parse(
            "Jurad received a drop: Rune platebody (39,000 coins).");
        assertEquals("Rune platebody", drop.getItemName());
        assertEquals(1L, drop.getQuantity());
        assertEquals("Jurad received a drop: Rune platebody.", drop.hideValue());
        assertEquals("Jurad received a drop: Rune platebody (39,000 gp).",
            drop.withHighAlchValue(39000));
    }

    @Test
    public void hidesPricesInVisibleMessagesFromReportedScreenshot()
    {
        // These are the visible bodies in the user's screenshot. Hidden
        // transport prefixes are modeled separately in the plugin tests.
        assertEquals("xXHairflip69 received a drop: Rune battleaxe.",
            parse("xXHairflip69 received a drop: Rune battleaxe (24,666 coins).")
                .hideValue());
        assertEquals("Khol Snirpo received a drop: Rune med helm.",
            parse("Khol Snirpo received a drop: Rune med helm (11,216 coins).")
                .hideValue());
    }

    @Test
    public void handlesRealGimFixtureFormatsFromExistingPlugin()
    {
        // Source formats: DaRoms/group-event-notifications,
        // src/test/java/com/groupeventnotifications/GroupEventParserTest.java.
        assertEquals("Iron Timzor received a drop: Bones.",
            parse("Iron Timzor received a drop: Bones (31 coins).").hideValue());
        BroadcastValueFormatter.ParsedDrop stack = parse(
            "Iron Timzor received a drop: 30 x Cosmic rune (3,540 coins) from Sergeant Strongstack.");
        assertEquals("Cosmic rune", stack.getItemName());
        assertEquals(30L, stack.getQuantity());
        assertEquals("Iron Timzor received a drop: 30 x Cosmic rune from Sergeant Strongstack.",
            stack.hideValue());
    }

    @Test
    public void preservesGimMarkerSourceAndKillCount()
    {
        String input = "|Jurad received a drop: Bandos chestplate (31,234,567 coins)"
            + " from General Graardor. Kill count: 123.";
        BroadcastValueFormatter.ParsedDrop drop = parse(input);
        assertEquals("|Jurad received a drop: Bandos chestplate"
            + " from General Graardor. Kill count: 123.", drop.hideValue());
        assertEquals("|Jurad received a drop: Bandos chestplate (159,000 gp)"
            + " from General Graardor. Kill count: 123.", drop.withHighAlchValue(159000));
    }

    @Test
    public void preservesTagsEvenWhenPriceHasItsOwnTags()
    {
        String input = "|<col=005f00><img=3><u>Jurad</u> received a drop: "
            + "<col=ff9040>2 x Rune platebody</col> (<col=ffff00>100,000</col> coins)."
            + "</col>";
        BroadcastValueFormatter.ParsedDrop drop = parse(input);
        assertEquals("Rune platebody", drop.getItemName());
        assertEquals(2L, drop.getQuantity());
        assertFalse(drop.getItemId().isPresent());
        assertEquals("|<col=005f00><img=3><u>Jurad</u> received a drop: "
            + "<col=ff9040>2 x Rune platebody</col><col=ffff00></col>.</col>",
            drop.hideValue());
        assertEquals("|Jurad received a drop: 2 x Rune platebody (78,000 gp).",
            visible(drop.withHighAlchValue(78000)));
        assertTrue(drop.withHighAlchValue(78000).contains("<col=ffff00>78,000 gp</col>"));
    }

    @Test
    public void keepsParenthesesInItemNames()
    {
        BroadcastValueFormatter.ParsedDrop drop = parse(
            "A B received a drop: Dragon dagger(p++) (30,001 coins).");
        assertEquals("Dragon dagger(p++)", drop.getItemName());
        assertEquals("A B received a drop: Dragon dagger(p++).", drop.hideValue());
    }

    @Test
    public void supportsQuantityVariantsAndCommaQuantities()
    {
        for (String separator : new String[] {" x ", "x ", " x", " \u00d7 "})
        {
            BroadcastValueFormatter.ParsedDrop drop = parse(
                "Name_1 received a drop: 1,200" + separator + "Nature rune (100,000 coins).");
            assertEquals(1200L, drop.getQuantity());
            assertEquals("Nature rune", drop.getItemName());
        }
    }

    @Test
    public void supportsRaidAndClueBroadcasts()
    {
        assertEquals("Jurad received special loot from a raid: Twisted bow.",
            parse("Jurad received special loot from a raid: Twisted bow (1,000,000,000 coins).")
                .hideValue());
        assertEquals("Jurad received a clue item: Rune platebody (g).",
            parse("Jurad received a clue item: Rune platebody (g) (90,000 coins).")
                .hideValue());
    }

    @Test
    public void supportsSingularCoinAndGpSuffixes()
    {
        assertEquals("Jurad received a rare drop: Coins.",
            parse("Jurad received a rare drop: Coins (1 coin).").hideValue());
        assertEquals("Jurad received a drop: Rune platebody (39,000 gp).",
            parse("Jurad received a drop: Rune platebody (39,000 gp).")
                .withHighAlchValue(39000));
    }

    @Test
    public void supportsNonbreakingSpacesAndWholeMessageColour()
    {
        BroadcastValueFormatter.ParsedDrop drop = parse(
            "<col=00ff00>Jurad\u00a0II received a drop: 2\u00a0x\u00a0Rune platebody\u00a0(100,000 coins).</col>");
        assertEquals("Rune platebody", drop.getItemName());
        assertEquals(2L, drop.getQuantity());
        assertEquals("<col=00ff00>Jurad\u00a0II received a drop: 2\u00a0x\u00a0Rune platebody.</col>",
            drop.hideValue());
    }

    @Test
    public void rejectsUnrelatedAndValuelessMessages()
    {
        String[] inputs = {
            "Jurad has reached Attack level 70.",
            "Jurad has completed the Dragon Slayer II quest.",
            "Jurad received a drop: Rune platebody.",
            "Jurad received a rare drop: Dragon warhammer",
            "Jurad has a funny feeling like he's being followed: Tiny tempor at 25 permits.",
            "New item added to your collection log: Rune platebody",
            "Jurad has defeated Other and received (123,456 coins) worth of loot!",
            "Jurad has been defeated by Other and lost (123,456 coins) worth of loot.",
            "I think Jurad received a drop: Rune platebody (39,000 coins).",
            "Jurad: I received a drop: Rune platebody (39,000 coins).",
            "Valuable drop: Rune platebody (39,000 coins)",
            "Jurad received a drop: Rune platebody (HA: 39,000 gp).",
            "Jurad received a drop: Rune platebody (1,23 coins).",
            "Jurad received a drop: Rune platebody (39,000 coins). unrelated text",
            "Jurad received a drop: Rune platebody (39,000 coins).<br>Second line",
            "Jurad received a drop: Rune platebody (39,000 coins).<unclosed",
            "Jurad received a drop: Rune platebody (39,000 coins), Rune sword (12,000 coins).",
            "Jurad received a drop: 0 x Rune platebody (39,000 coins).",
            "Jurad received a drop: 9,999,999,999,999,999,999 x Rune platebody (39,000 coins)."
        };
        for (String input : inputs)
        {
            assertFalse("Must leave unchanged: " + input,
                BroadcastValueFormatter.parse(input).isPresent());
        }
        assertFalse(BroadcastValueFormatter.parse(null).isPresent());
        assertFalse(BroadcastValueFormatter.parse("").isPresent());
    }

    @Test
    public void largeHighAlchTotalsDoNotOverflowOrDependOnLocale()
    {
        BroadcastValueFormatter.ParsedDrop drop = parse(
            "Jurad received a drop: 100,000 x Rune platebody (3,900,000,000 coins).");
        assertEquals("Jurad received a drop: 100,000 x Rune platebody (3,900,000,000 gp).",
            drop.withHighAlchValue(3900000000L));
        assertTrue(drop.withHighAlchValue(0).contains("(0 gp)"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void refusesNegativeHighAlchValues()
    {
        parse("Jurad received a drop: Rune platebody (39,000 coins).")
            .withHighAlchValue(-1);
    }
}
