package com.gimdropvalues;

public enum ValueDisplayMode
{
	HIDE("Hide coin value"),
	HIGH_ALCH("Show High Alch value");

	private final String label;

	ValueDisplayMode(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
