package com.gimdropvalues;

import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;

/** Resolves only exact names, refusing conflicting values instead of guessing. */
final class HighAlchResolver
{
	interface ItemLookup
	{
		List<Candidate> find(String exactName);
	}

	static final class Candidate
	{
		private final String name;
		private final int highAlchValue;

		Candidate(String name, int highAlchValue)
		{
			this.name = name;
			this.highAlchValue = highAlchValue;
		}
	}

	private HighAlchResolver()
	{
	}

	static OptionalLong resolveUnitValue(String itemName, ItemLookup lookup)
	{
		String normalized = normalizeName(itemName);
		if (normalized.isEmpty() || normalized.equals("coins") || normalized.equals("platinum token"))
		{
			// Currency is already money, not an item on which to report spell proceeds.
			return OptionalLong.empty();
		}

		List<Candidate> candidates = lookup.find(itemName);
		if (candidates == null)
		{
			return OptionalLong.empty();
		}

		Long resolved = null;
		for (Candidate candidate : candidates)
		{
			if (candidate == null || !normalized.equals(normalizeName(candidate.name)))
			{
				continue;
			}
			if (candidate.highAlchValue < 0)
			{
				return OptionalLong.empty();
			}
			long value = candidate.highAlchValue;
			if (resolved != null && resolved.longValue() != value)
			{
				return OptionalLong.empty();
			}
			resolved = value;
		}
		return resolved == null ? OptionalLong.empty() : OptionalLong.of(resolved);
	}

	static OptionalLong stackValue(OptionalLong unitValue, long quantity)
	{
		if (!unitValue.isPresent() || unitValue.getAsLong() < 0 || quantity <= 0)
		{
			return OptionalLong.empty();
		}
		try
		{
			return OptionalLong.of(Math.multiplyExact(unitValue.getAsLong(), quantity));
		}
		catch (ArithmeticException ignored)
		{
			return OptionalLong.empty();
		}
	}

	static String normalizeName(String name)
	{
		return name == null ? "" : name.replace('\u00a0', ' ').trim().toLowerCase(Locale.ROOT);
	}
}
