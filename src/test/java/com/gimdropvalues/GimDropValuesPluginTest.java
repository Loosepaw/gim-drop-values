package com.gimdropvalues;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/** Local developer client entry point; this is not an automated test. */
public final class GimDropValuesPluginTest
{
	private GimDropValuesPluginTest()
	{
	}

	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(GimDropValuesPlugin.class);
		RuneLite.main(args);
	}
}
