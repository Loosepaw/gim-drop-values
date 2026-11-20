package com.gimdropvalues;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(GimDropValuesConfig.GROUP)
public interface GimDropValuesConfig extends Config
{
	String GROUP = "gimdropvalues";

	@ConfigItem(
		keyName = "displayMode",
		name = "Value display",
		description = "Hide the coin value, or show the stack's High Alchemy value before rune costs. "
			+ "If an item cannot be identified reliably, its coin value is hidden.",
		position = 0
	)
	default ValueDisplayMode displayMode()
	{
		return ValueDisplayMode.HIDE;
	}

	@ConfigItem(
		keyName = "groupIronman",
		name = "Group Ironman broadcasts",
		description = "Apply to loot announcements in your Group Ironman chat.",
		position = 1
	)
	default boolean groupIronman()
	{
		return true;
	}

	@ConfigItem(
		keyName = "gameChat",
		name = "Game chat loot reports",
		description = "Apply to personal valuable-drop reports and supported loot announcements in game chat.",
		position = 2
	)
	default boolean gameChat()
	{
		return true;
	}

	@ConfigItem(
		keyName = "clanBroadcasts",
		name = "Clan broadcasts",
		description = "Also apply to loot announcements in your regular clan chat.",
		position = 3
	)
	default boolean clanBroadcasts()
	{
		return false;
	}

	@ConfigItem(
		keyName = "guestClanBroadcasts",
		name = "Guest clan broadcasts",
		description = "Also apply to loot announcements in the clan you are visiting.",
		position = 4
	)
	default boolean guestClanBroadcasts()
	{
		return false;
	}
}
