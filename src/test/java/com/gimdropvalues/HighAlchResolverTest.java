package com.gimdropvalues;

import java.util.Arrays;
import java.util.Collections;
import java.util.OptionalLong;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class HighAlchResolverTest
{
	@Test
	public void exactNameWinsOverSimilarItemsAndFormattingDifferences()
	{
		OptionalLong result = HighAlchResolver.resolveUnitValue(" Rune\u00a0platebody ", name -> Arrays.asList(
			new HighAlchResolver.Candidate("Rune platebody (g)", 40000),
			new HighAlchResolver.Candidate("rune platebody", 39000),
			new HighAlchResolver.Candidate("Rune platebody (t)", 41000)));
		assertEquals(39000, result.getAsLong());
	}

	@Test
	public void duplicateNamesAreSafeOnlyWhenTheirValuesAgree()
	{
		OptionalLong matching = HighAlchResolver.resolveUnitValue("Rune platebody", name -> Arrays.asList(
			new HighAlchResolver.Candidate("Rune platebody", 39000),
			new HighAlchResolver.Candidate("Rune platebody", 39000)));
		assertEquals(39000, matching.getAsLong());

		OptionalLong conflicting = HighAlchResolver.resolveUnitValue("Rune platebody", name -> Arrays.asList(
			new HighAlchResolver.Candidate("Rune platebody", 39000),
			new HighAlchResolver.Candidate("Rune platebody", 1)));
		assertFalse(conflicting.isPresent());
	}

	@Test
	public void unknownAndPartialMatchesDoNotProduceInventedPrices()
	{
		assertFalse(HighAlchResolver.resolveUnitValue("Unknown drop", name -> Collections.emptyList()).isPresent());
		assertFalse(HighAlchResolver.resolveUnitValue("Rune plate", name -> Collections.singletonList(
			new HighAlchResolver.Candidate("Rune platebody", 39000))).isPresent());
		assertFalse(HighAlchResolver.resolveUnitValue("Rune platebody", name -> null).isPresent());
	}

	@Test
	public void coinsAreNotPresentedAsAlchemyProceeds()
	{
		HighAlchResolver.ItemLookup mustNotBeCalled = name ->
		{
			fail("A currency or empty name must not require an item lookup");
			return Collections.emptyList();
		};
		assertFalse(HighAlchResolver.resolveUnitValue("Coins", mustNotBeCalled).isPresent());
		assertFalse(HighAlchResolver.resolveUnitValue("Platinum token", mustNotBeCalled).isPresent());
		assertFalse(HighAlchResolver.resolveUnitValue("", mustNotBeCalled).isPresent());
	}

	@Test
	public void wholeStackUsesLongArithmeticAndFailsClosedOnOverflow()
	{
		assertEquals(78000L, HighAlchResolver.stackValue(OptionalLong.of(39000), 2).getAsLong());
		assertEquals(6000000000L, HighAlchResolver.stackValue(OptionalLong.of(60000), 100000).getAsLong());
		assertFalse(HighAlchResolver.stackValue(OptionalLong.of(Long.MAX_VALUE), 2).isPresent());
		assertFalse(HighAlchResolver.stackValue(OptionalLong.empty(), 2).isPresent());
		assertFalse(HighAlchResolver.stackValue(OptionalLong.of(10), 0).isPresent());
		assertFalse(HighAlchResolver.stackValue(OptionalLong.of(-1), 1).isPresent());
		assertEquals(0L, HighAlchResolver.stackValue(OptionalLong.of(0), 10).getAsLong());
	}
}
