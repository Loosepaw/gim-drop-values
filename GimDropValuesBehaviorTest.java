package com.gimdropvalues;

import com.google.common.cache.Cache;
import java.awt.Color;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Iterator;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicInteger;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.IterableHashTable;
import net.runelite.api.MessageNode;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ChatColorConfig;
import net.runelite.client.eventbus.EventBus;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GimDropValuesBehaviorTest
{
	private static final GimDropValuesConfig DEFAULTS = new GimDropValuesConfig() {};

	@Test
	public void defaultsAcceptGroupAndGameSystemMessages()
	{
		for (ChatMessageType type : ChatMessageType.values())
		{
			assertEquals(type.name(), type == ChatMessageType.CLAN_GIM_MESSAGE
				|| type == ChatMessageType.GAMEMESSAGE || type == ChatMessageType.SPAM,
				GimDropValuesPlugin.accepts(type, DEFAULTS));
		}
	}

	@Test
	public void optionalChannelsStillNeverIncludeHumanChat()
	{
		GimDropValuesConfig allChannels = new GimDropValuesConfig()
		{
			@Override public boolean clanBroadcasts() { return true; }
			@Override public boolean guestClanBroadcasts() { return true; }
		};
		assertTrue(GimDropValuesPlugin.accepts(ChatMessageType.CLAN_MESSAGE, allChannels));
		assertTrue(GimDropValuesPlugin.accepts(ChatMessageType.CLAN_GUEST_MESSAGE, allChannels));
		assertFalse(GimDropValuesPlugin.accepts(ChatMessageType.CLAN_CHAT, allChannels));
		assertFalse(GimDropValuesPlugin.accepts(ChatMessageType.CLAN_GUEST_CHAT, allChannels));
		assertFalse(GimDropValuesPlugin.accepts(ChatMessageType.CLAN_GIM_CHAT, allChannels));
	}

	@Test
	public void hidesOnlyDrawnPriceAndLeavesOriginalMessageAndOtherStackFieldsIntact() throws Exception
	{
		String original = "|Alice received a drop: Rune platebody (40,000 coins) from Vorkath.";
		Fixture fixture = new Fixture(original, ChatMessageType.CLAN_GIM_MESSAGE, DEFAULTS);
		fixture.post("chatMessageBuilding");
		assertEquals("|Alice received a drop: Rune platebody from Vorkath.", fixture.objects[2]);
		assertEquals("test-channel", fixture.objects[0]);
		assertEquals("test-sender", fixture.objects[1]);
		assertEquals("trailing-stack-value", fixture.objects[3]);
		assertEquals(original, fixture.node.getValue());
		assertEquals(0, fixture.messageMutations.get());
	}

	@Test
	public void sameWordsTypedByPlayerAreNotModified() throws Exception
	{
		String message = "|Alice received a drop: Rune platebody (40,000 coins).";
		Fixture fixture = new Fixture(message, ChatMessageType.CLAN_GIM_CHAT, DEFAULTS);
		fixture.post("chatMessageBuilding");
		assertEquals(message, fixture.objects[2]);
	}

	@Test
	public void unrelatedCallbackAndNonDropBroadcastAreNotModified() throws Exception
	{
		String drop = "|Alice received a drop: Rune platebody (40,000 coins).";
		Fixture fixture = new Fixture(drop, ChatMessageType.CLAN_GIM_MESSAGE, DEFAULTS);
		fixture.post("chatFilterCheck");
		assertEquals(drop, fixture.objects[2]);

		String achievement = "|Alice has reached Mining level 75.";
		Fixture other = new Fixture(achievement, ChatMessageType.CLAN_GIM_MESSAGE, DEFAULTS);
		other.post("chatMessageBuilding");
		assertEquals(achievement, other.objects[2]);
	}

	@Test
	public void highAlchModeUsesWholeStackAndUnknownValueFallsBackToNameOnly() throws Exception
	{
		GimDropValuesConfig alch = new GimDropValuesConfig()
		{
			@Override public ValueDisplayMode displayMode() { return ValueDisplayMode.HIGH_ALCH; }
		};
		String original = "|Alice received a drop: 2 x Rune platebody (80,000 coins).";
		Fixture fixture = new Fixture(original, ChatMessageType.CLAN_GIM_MESSAGE, alch);
		fixture.setKnownUnitPrice("rune platebody", OptionalLong.of(39000));
		fixture.post("chatMessageBuilding");
		assertEquals("|Alice received a drop: 2 x Rune platebody (78,000 gp).", fixture.objects[2]);
		assertEquals(original, fixture.node.getValue());

		Fixture unknown = new Fixture(original, ChatMessageType.CLAN_GIM_MESSAGE, alch);
		unknown.setKnownUnitPrice("rune platebody", OptionalLong.empty());
		unknown.post("chatMessageBuilding");
		assertEquals("|Alice received a drop: 2 x Rune platebody.", unknown.objects[2]);
	}

	@Test
	public void hidesPersonalGameDropInBothGameMessageTypesWithoutChangingOriginal() throws Exception
	{
		String original = "<col=ef1020>Valuable drop: Rune scimitar (25,600 coins)</col>";
		for (ChatMessageType type : new ChatMessageType[] {ChatMessageType.GAMEMESSAGE, ChatMessageType.SPAM})
		{
			Fixture fixture = new Fixture(original, type, DEFAULTS);
			fixture.post("chatMessageBuilding");
			assertEquals("<col=ef1020>Valuable drop: Rune scimitar</col>", fixture.objects[2]);
			assertEquals(original, fixture.node.getValue());
			assertEquals(0, fixture.messageMutations.get());
			assertEquals("test-sender", fixture.objects[1]);
			assertEquals("trailing-stack-value", fixture.objects[3]);
		}
	}

	@Test
	public void bothReportsUseTheSameHighAlchModeAndQuantity() throws Exception
	{
		GimDropValuesConfig alch = new GimDropValuesConfig()
		{
			@Override public ValueDisplayMode displayMode() { return ValueDisplayMode.HIGH_ALCH; }
		};
		String[][] reports = {
			{"|Alice received a drop: 2 x Rune platebody (80,000 coins).",
				"|Alice received a drop: 2 x Rune platebody (78,000 gp)."},
			{"<col=ef1020>Valuable drop: 2 x Rune platebody (80,000 coins)</col>",
				"<col=ef1020>Valuable drop: 2 x Rune platebody (78,000 gp)</col>"},
			{"Valuable drop: 2 x Rune platebody (80,000 coins)",
				"Valuable drop: 2 x Rune platebody (78,000 gp)"}
		};
		ChatMessageType[] types = {ChatMessageType.CLAN_GIM_MESSAGE, ChatMessageType.GAMEMESSAGE, ChatMessageType.SPAM};
		for (int i = 0; i < types.length; i++)
		{
			Fixture fixture = new Fixture(reports[i][0], types[i], alch);
			fixture.setKnownUnitPrice("rune platebody", OptionalLong.of(39000));
			fixture.post("chatMessageBuilding");
			assertEquals(reports[i][1], fixture.objects[2]);
			assertEquals(reports[i][0], fixture.node.getValue());
			assertEquals(0, fixture.messageMutations.get());
		}
	}

	@Test
	public void unknownHighAlchValueAlsoHidesPersonalGamePrice() throws Exception
	{
		GimDropValuesConfig alch = new GimDropValuesConfig()
		{
			@Override public ValueDisplayMode displayMode() { return ValueDisplayMode.HIGH_ALCH; }
		};
		Fixture fixture = new Fixture("Valuable drop: Unknown item (80,000 coins)", ChatMessageType.GAMEMESSAGE, alch);
		fixture.setKnownUnitPrice("unknown item", OptionalLong.empty());
		fixture.post("chatMessageBuilding");
		assertEquals("Valuable drop: Unknown item", fixture.objects[2]);
	}

	@Test
	public void gameAndGroupSettingsCanBeDisabledIndependently() throws Exception
	{
		GimDropValuesConfig groupOnly = new GimDropValuesConfig()
		{
			@Override public boolean gameChat() { return false; }
		};
		GimDropValuesConfig gameOnly = new GimDropValuesConfig()
		{
			@Override public boolean groupIronman() { return false; }
		};
		assertTrue(GimDropValuesPlugin.accepts(ChatMessageType.CLAN_GIM_MESSAGE, groupOnly));
		assertFalse(GimDropValuesPlugin.accepts(ChatMessageType.GAMEMESSAGE, groupOnly));
		assertFalse(GimDropValuesPlugin.accepts(ChatMessageType.SPAM, groupOnly));
		assertFalse(GimDropValuesPlugin.accepts(ChatMessageType.CLAN_GIM_MESSAGE, gameOnly));
		assertTrue(GimDropValuesPlugin.accepts(ChatMessageType.GAMEMESSAGE, gameOnly));
		assertTrue(GimDropValuesPlugin.accepts(ChatMessageType.SPAM, gameOnly));

		String game = "Valuable drop: Rune platebody (40,000 coins)";
		Fixture gameDisabled = new Fixture(game, ChatMessageType.GAMEMESSAGE, groupOnly);
		gameDisabled.post("chatMessageBuilding");
		assertEquals(game, gameDisabled.objects[2]);
		String group = "|Alice received a drop: Rune platebody (40,000 coins).";
		Fixture groupDisabled = new Fixture(group, ChatMessageType.CLAN_GIM_MESSAGE, gameOnly);
		groupDisabled.post("chatMessageBuilding");
		assertEquals(group, groupDisabled.objects[2]);
	}

	@Test
	public void personalDropWordsTypedByPlayersAreNotModified() throws Exception
	{
		String message = "Valuable drop: Rune platebody (40,000 coins)";
		for (ChatMessageType type : new ChatMessageType[] {
			ChatMessageType.PUBLICCHAT, ChatMessageType.PRIVATECHAT, ChatMessageType.CLAN_GIM_CHAT})
		{
			Fixture fixture = new Fixture(message, type, DEFAULTS);
			fixture.post("chatMessageBuilding");
			assertEquals(message, fixture.objects[2]);
		}
	}

	@Test
	public void unrelatedGamePricesAndAggregateTotalsAreNotModified() throws Exception
	{
		for (String message : new String[] {
			"You bought a Rune platebody (40,000 coins).",
			"Your loot is worth around 80,000 coins.",
			"Untradeable drop: Rusty sword",
			"You have reached Mining level 75."})
		{
			Fixture fixture = new Fixture(message, ChatMessageType.GAMEMESSAGE, DEFAULTS);
			fixture.post("chatMessageBuilding");
			assertEquals(message, fixture.objects[2]);
		}
	}

	@Test
	public void namedDropReportsAreAlsoHandledWhenDeliveredAsGameMessages() throws Exception
	{
		String message = "Logic Knot received a drop: Adamant longsword (1,200 coins).";
		Fixture fixture = new Fixture(message, ChatMessageType.GAMEMESSAGE, DEFAULTS);
		fixture.post("chatMessageBuilding");
		assertEquals("Logic Knot received a drop: Adamant longsword.", fixture.objects[2]);
		assertEquals(message, fixture.node.getValue());
	}

	/**
	 * The visible sentences come from the reported screenshot. Pipe prefixes
	 * are modeled from the game chat script contract, not captured live packets.
	 */
	@Test
	public void hiddenGroupPrefixesWorkWithRuneLiteChatFormatting() throws Exception
	{
		String[][] reports = {
			{"||xXHairflip69 received a drop: Rune battleaxe (24,666 coins).",
				"|<col=ff0000>|xXHairflip69 received a drop: Rune battleaxe.</col>"},
			{"|metadata|Khol Snirpo received a drop: Rune med helm (11,216 coins).",
				"|<col=ff0000>metadata|Khol Snirpo received a drop: Rune med helm.</col>"}
		};
		for (String[] report : reports)
		{
			Fixture fixture = new Fixture(report[0], ChatMessageType.CLAN_GIM_MESSAGE, DEFAULTS);
			fixture.addRuneLiteChatFormatting();
			fixture.post("chatMessageBuilding");
			assertEquals(report[1], fixture.objects[2]);
			assertEquals(report[0], fixture.node.getValue());
			assertEquals(0, fixture.messageMutations.get());
			assertEquals("test-channel", fixture.objects[0]);
			assertEquals("test-sender", fixture.objects[1]);
			assertEquals("trailing-stack-value", fixture.objects[3]);
		}
	}

	@Test
	public void hiddenGroupMetadataKeepsHighAlchValueAndOriginalNode() throws Exception
	{
		String original = "|metadata|xXHairflip69 received a drop: Rune battleaxe (24,666 coins).";
		Fixture fixture = new Fixture(original, ChatMessageType.CLAN_GIM_MESSAGE, highAlchConfig());
		fixture.setKnownUnitPrice("rune battleaxe", OptionalLong.of(24960));
		fixture.addRuneLiteChatFormatting();
		fixture.post("chatMessageBuilding");
		assertEquals("|<col=ff0000>metadata|xXHairflip69 received a drop: Rune battleaxe (24,960 gp).</col>",
			fixture.objects[2]);
		assertEquals(original, fixture.node.getValue());
		assertEquals(0, fixture.messageMutations.get());
	}

	@Test
	public void hiddenGameMetadataUsesWholeStackHighAlchValue() throws Exception
	{
		String original = "metadata|Valuable drop: 2 x Rune platebody (80,000 coins)";
		Fixture fixture = new Fixture(original, ChatMessageType.GAMEMESSAGE, highAlchConfig());
		fixture.setKnownUnitPrice("rune platebody", OptionalLong.of(39000));
		fixture.addRuneLiteChatFormatting();
		fixture.post("chatMessageBuilding");
		assertEquals("<col=ff0000>metadata|Valuable drop: 2 x Rune platebody (78,000 gp)</col>", fixture.objects[2]);
		assertEquals(original, fixture.node.getValue());
		assertEquals(0, fixture.messageMutations.get());
	}

	@Test
	public void hiddenClanMetadataUsesTheClanOption() throws Exception
	{
		String original = "metadata|Khol Snirpo received a drop: Rune med helm (11,216 coins).";
		Fixture disabled = new Fixture(original, ChatMessageType.CLAN_MESSAGE, DEFAULTS);
		disabled.post("chatMessageBuilding");
		assertEquals(original, disabled.objects[2]);
		GimDropValuesConfig clanEnabled = new GimDropValuesConfig()
		{
			@Override public boolean clanBroadcasts() { return true; }
		};
		Fixture enabled = new Fixture(original, ChatMessageType.CLAN_MESSAGE, clanEnabled);
		enabled.post("chatMessageBuilding");
		assertEquals("metadata|Khol Snirpo received a drop: Rune med helm.", enabled.objects[2]);
	}

	@Test
	public void metadataDoesNotBypassGroupGameOrPlayerChatExclusions() throws Exception
	{
		GimDropValuesConfig disabled = new GimDropValuesConfig()
		{
			@Override public boolean groupIronman() { return false; }
			@Override public boolean gameChat() { return false; }
		};
		String group = "|metadata|Khol Snirpo received a drop: Rune med helm (11,216 coins).";
		String game = "metadata|Valuable drop: Rune med helm (11,216 coins)";
		for (ChatMessageType type : new ChatMessageType[] {
			ChatMessageType.CLAN_GIM_MESSAGE, ChatMessageType.CLAN_GIM_CHAT})
		{
			Fixture fixture = new Fixture(group, type,
				type == ChatMessageType.CLAN_GIM_MESSAGE ? disabled : DEFAULTS);
			fixture.post("chatMessageBuilding");
			assertEquals(group, fixture.objects[2]);
		}
		Fixture fixture = new Fixture(game, ChatMessageType.GAMEMESSAGE, disabled);
		fixture.post("chatMessageBuilding");
		assertEquals(game, fixture.objects[2]);
	}

	@Test
	public void unsupportedMetadataFieldsRemainUntouched() throws Exception
	{
		GimDropValuesConfig guestEnabled = new GimDropValuesConfig()
		{
			@Override public boolean guestClanBroadcasts() { return true; }
		};
		String group = "metadata|Khol Snirpo received a drop: Rune med helm (11,216 coins).";
		Fixture guest = new Fixture(group, ChatMessageType.CLAN_GUEST_MESSAGE, guestEnabled);
		guest.post("chatMessageBuilding");
		assertEquals(group, guest.objects[2]);
		String game = "metadata|Valuable drop: Rune med helm (11,216 coins)";
		Fixture spam = new Fixture(game, ChatMessageType.SPAM, DEFAULTS);
		spam.post("chatMessageBuilding");
		assertEquals(game, spam.objects[2]);
		String extra = "|||Khol Snirpo received a drop: Rune med helm (11,216 coins).";
		Fixture extraField = new Fixture(extra, ChatMessageType.CLAN_GIM_MESSAGE, DEFAULTS);
		extraField.post("chatMessageBuilding");
		assertEquals(extra, extraField.objects[2]);
	}

	private static GimDropValuesConfig highAlchConfig()
	{
		return new GimDropValuesConfig()
		{
			@Override public ValueDisplayMode displayMode() { return ValueDisplayMode.HIGH_ALCH; }
		};
	}

	private static ScriptCallbackEvent event(String name)
	{
		ScriptCallbackEvent event = new ScriptCallbackEvent();
		event.setEventName(name);
		return event;
	}

	private static void inject(Object object, String name, Object value) throws Exception
	{
		Field field = object.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(object, value);
	}

	/** Uses the actual RuneLite API interfaces without starting a game client. */
	private static final class Fixture
	{
		private final Object[] objects;
		private final AtomicInteger messageMutations = new AtomicInteger();
		private final MessageNode node;
		private final Client client;
		private final EventBus eventBus = new EventBus(error -> { throw new AssertionError(error); });
		private final GimDropValuesPlugin plugin = new GimDropValuesPlugin();
		private final RuneLiteHighAlchLookup alchLookup = new RuneLiteHighAlchLookup(null);

		private Fixture(String original, ChatMessageType type, GimDropValuesConfig config) throws Exception
		{
			objects = new Object[] {"test-channel", "test-sender", original, "trailing-stack-value"};
			node = (MessageNode) Proxy.newProxyInstance(MessageNode.class.getClassLoader(),
				new Class<?>[] {MessageNode.class}, (proxy, method, args) ->
				{
					switch (method.getName())
					{
						case "getType": return type;
						case "getValue": return original;
						case "getRuneLiteFormatMessage": return null;
						case "setValue":
						case "setRuneLiteFormatMessage": messageMutations.incrementAndGet(); return null;
						default: throw new AssertionError("Unexpected MessageNode access: " + method.getName());
					}
				});
			IterableHashTable<MessageNode> messages = new IterableHashTable<MessageNode>()
			{
				@Override public MessageNode get(long id) { return id == 42 ? node : null; }
				@Override public void put(MessageNode value, long id) { throw new AssertionError("Message mutation"); }
				@Override public Iterator<MessageNode> iterator() { return Collections.singleton(node).iterator(); }
			};
			client = (Client) Proxy.newProxyInstance(Client.class.getClassLoader(),
				new Class<?>[] {Client.class}, (proxy, method, args) ->
				{
					switch (method.getName())
					{
						case "getIntStackSize": return 2;
						case "getObjectStackSize": return objects.length;
						case "getIntStack": return new int[] {0, 42};
						case "getObjectStack": return objects;
						case "getMessages": return messages;
						case "isResized": return false;
						default: throw new AssertionError("Unexpected Client access: " + method.getName());
					}
				});
			inject(plugin, "client", client);
			inject(plugin, "config", config);
			inject(plugin, "alchLookup", alchLookup);
			eventBus.register(plugin);
		}

		private void post(String name)
		{
			eventBus.post(event(name));
		}

		/** Runs RuneLite's real priority-0 recoloring before our priority -10 subscriber. */
		private void addRuneLiteChatFormatting() throws Exception
		{
			ChatColorConfig colors = (ChatColorConfig) Proxy.newProxyInstance(ChatColorConfig.class.getClassLoader(),
				new Class<?>[] {ChatColorConfig.class}, (proxy, method, args) ->
					"opaqueClanChatInfo".equals(method.getName()) || "opaqueServerMessage".equals(method.getName())
						? Color.RED : null);
			Constructor<ChatMessageManager> constructor = ChatMessageManager.class.getDeclaredConstructor(
				Client.class, ChatColorConfig.class, ClientThread.class, EventBus.class);
			constructor.setAccessible(true);
			ChatMessageManager manager = constructor.newInstance(client, colors, null, eventBus);
			Method loadColors = ChatMessageManager.class.getDeclaredMethod("loadColors");
			loadColors.setAccessible(true);
			loadColors.invoke(manager);
		}

		@SuppressWarnings("unchecked")
		private void setKnownUnitPrice(String name, OptionalLong value) throws Exception
		{
			Field field = RuneLiteHighAlchLookup.class.getDeclaredField("unitValues");
			field.setAccessible(true);
			((Cache<String, OptionalLong>) field.get(alchLookup)).put(name, value);
		}
	}
}
