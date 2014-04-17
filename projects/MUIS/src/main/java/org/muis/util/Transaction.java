package org.muis.util;

/**
 * Represents a transaction that happens in sequence on a thread and is not susceptible to interference by other threads.
 * {@link AutoCloseable#close() Closing} the transaction ends it.
 */
public interface Transaction extends AutoCloseable {
	@Override
	void close();
}
