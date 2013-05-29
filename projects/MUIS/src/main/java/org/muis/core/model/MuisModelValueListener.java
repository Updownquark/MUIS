package org.muis.core.model;

public interface MuisModelValueListener<T> {
	void valueChanged(MuisModelValueEvent<? extends T> evt);
}
