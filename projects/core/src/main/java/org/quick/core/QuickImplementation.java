package org.quick.core;

public interface QuickImplementation<W> {
	W createWidget(QuickElement element, Class<? extends W> type) throws QuickException;
}
