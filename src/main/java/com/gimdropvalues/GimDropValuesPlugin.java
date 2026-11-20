package com.gimdropvalues;

import com.google.inject.Provides;
import java.util.Optional;
import java.util.OptionalLong;
import javax.inject.Inject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MessageNode;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
	name = "GIM Drop Values",
	description = "Hide coin values or show High Alch values in GIM broadcasts and game-chat loot reports",
	tags = {"group", "ironman", "gim", "loot", "drop", "value", "alchemy", "chat"}
)
public class GimDropValuesPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private GimDropValuesConfig config;

	@Inject
	private RuneLiteHighAlchLookup alchLookup;

	@Provides
	GimDropValuesConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(GimDropValuesConfig.class);
	}

	@Override
	protected void startUp()
	{
		refresh();
	}

	@Override
	protected void shutDown()
	{
		refresh();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (GimDropValuesConfig.GROUP.equals(event.getGroup()))
		{
			refresh();
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			alchLookup.clear();
		}
	}

	/**
	 * Runs after RuneLite's ChatMessageManager (priority 0). This changes only
	 * the text being drawn, leaving MessageNode and ChatMessage events intact.
	 * Switching mode or disabling the plugin therefore also restores old lines.
	 */
	@Subscribe(priority = -10)
	public void onScriptCallbackEvent(ScriptCallbackEvent event)
	{
		if (!"chatMessageBuilding".equals(event.getEventName()))
		{
			return;
		}

		int intSize = client.getIntStackSize();
		int objectSize = client.getObjectStackSize();
		int[] ints = client.getIntStack();
		Object[] objects = client.getObjectStack();
		if (ints == null || objects == null || intSize < 1 || intSize > ints.length
			|| objectSize < 2 || objectSize > objects.length || client.getMessages() == null)
		{
			return;
		}

		MessageNode node = client.getMessages().get(ints[intSize - 1]);
		if (node == null || !accepts(node.getType(), config))
		{
			return;
		}
		int messageIndex = objectSize - 2;
		if (!(objects[messageIndex] instanceof String))
		{
			return;
		}

		String message = (String) objects[messageIndex];
		Optional<BroadcastValueFormatter.ParsedDrop> parsed = isGameMessage(node.getType())
			? BroadcastValueFormatter.parseGameMessage(message)
			: BroadcastValueFormatter.parse(message);
		if (!parsed.isPresent())
		{
			return;
		}
		BroadcastValueFormatter.ParsedDrop drop = parsed.get();
		String replacement = drop.hideValue();
		if (config.displayMode() == ValueDisplayMode.HIGH_ALCH)
		{
			OptionalLong total = alchLookup.stackValue(drop);
			if (total.isPresent())
			{
				replacement = drop.withHighAlchValue(total.getAsLong());
			}
		}
		objects[messageIndex] = replacement;
	}

	static boolean accepts(ChatMessageType type, GimDropValuesConfig configuration)
	{
		if (isGameMessage(type))
		{
			return configuration.gameChat();
		}
		if (type == ChatMessageType.CLAN_GIM_MESSAGE)
		{
			return configuration.groupIronman();
		}
		if (type == ChatMessageType.CLAN_MESSAGE)
		{
			return configuration.clanBroadcasts();
		}
		return type == ChatMessageType.CLAN_GUEST_MESSAGE && configuration.guestClanBroadcasts();
	}

	private static boolean isGameMessage(ChatMessageType type)
	{
		return type == ChatMessageType.GAMEMESSAGE || type == ChatMessageType.SPAM;
	}

	private void refresh()
	{
		clientThread.invokeLater(() ->
		{
			alchLookup.clear();
			client.refreshChat();
		});
	}
}
