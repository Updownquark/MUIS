package org.muis.rx.collect;

public interface CollectionSession {
	Object get(Object listener, String key);
	Object put(Object listener, String key, Object value);
}
