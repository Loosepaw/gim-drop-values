package com.gimdropvalues;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ItemComposition;
import net.runelite.client.game.ItemManager;
import net.runelite.http.api.item.ItemPrice;

/** Uses RuneLite's existing item index and game definitions; makes no network requests. */
@Singleton
final class RuneLiteHighAlchLookup
{
	private final ItemManager itemManager;
	private final Cache<String, OptionalLong> unitValues = CacheBuilder.newBuilder()
		.maximumSize(1024)
		.expireAfterWrite(30, TimeUnit.SECONDS)
		.build();

	@Inject
	RuneLiteHighAlchLookup(ItemManager itemManager)
	{
		this.itemManager = itemManager;
	}

	OptionalLong stackValue(BroadcastValueFormatter.ParsedDrop drop)
	{
		String key = HighAlchResolver.normalizeName(drop.getItemName());
		OptionalLong unit = unitValues.getIfPresent(key);
		if (unit == null)
		{
			unit = HighAlchResolver.resolveUnitValue(drop.getItemName(), this::findCandidates);
			// A brief expiry lets an item-index download finish without requiring a restart.
			unitValues.put(key, unit);
		}
		return HighAlchResolver.stackValue(unit, drop.getQuantity());
	}

	void clear()
	{
		unitValues.invalidateAll();
	}

	private List<HighAlchResolver.Candidate> findCandidates(String name)
	{
		String normalized = HighAlchResolver.normalizeName(name);
		List<HighAlchResolver.Candidate> matches = new ArrayList<>();
		for (ItemPrice price : itemManager.search(name))
		{
			if (!normalized.equals(HighAlchResolver.normalizeName(price.getName())))
			{
				continue;
			}
			int id = itemManager.canonicalize(price.getId());
			ItemComposition item = itemManager.getItemComposition(id);
			// Compare the game definition as well as the name in RuneLite's item index.
			matches.add(new HighAlchResolver.Candidate(item.getMembersName(), item.getHaPrice()));
		}
		return matches;
	}
}
