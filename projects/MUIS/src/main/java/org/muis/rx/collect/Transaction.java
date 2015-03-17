package org.muis.rx.collect;

public interface Transaction extends AutoCloseable {
	@Override
	void close();
}
