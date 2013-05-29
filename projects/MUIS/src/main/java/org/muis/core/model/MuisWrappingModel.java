package org.muis.core.model;

public class MuisWrappingModel implements MuisModel {
	private final Object theWrapped;

	public MuisWrappingModel(Object wrap) {
		theWrapped = wrap;
	}
}
