package org.quick.core;

import java.util.ArrayList;

import org.qommons.ThreadPoolWorker;
import org.qommons.Worker;

/** Enables caching of resources in Quick */
public class QuickCache {
	/**
	 * Represents a type of item that can be cached
	 *
	 * @param <K> The key type for the cached item type
	 * @param <V> The value type for the cached item type
	 * @param <E> The type of exception that may be thrown when this item type generates a value
	 */
	public interface CacheItemType<K, V, E extends Exception> {
		/**
		 * @param env The Quick environment to generate the resource for
		 * @param key The key to generate the cached value for
		 * @return The value to cache and return for the given key
		 * @throws E If an error occurs generating the value
		 */
		V generate(QuickEnvironment env, K key) throws E;

		/**
		 * @param value The value to determine the size of
		 * @return The size, in bytes, of the given cached value
		 */
		int size(V value);
	}

	/**
	 * Receives an item from the cache when it is ready
	 *
	 * @param <K> The type key that the cache item is to be generated for
	 * @param <V> The type of value to be generated for the key
	 */
	public interface ItemReceiver<K, V> {
		/**
		 * Called when the item becomes available in the cache
		 *
		 * @param key The key that the item was generated for
		 * @param value The value that was generated
		 */
		public void itemGenerated(K key, V value);

		/**
		 * Called when an item fails to generate
		 *
		 * @param key The key for which item generation failed
		 * @param exception The exception that was thrown representing the failure
		 * @param firstReport Whether the exception is the direct result of the failure of the generation of the item for the first time
		 */
		public void errorOccurred(K key, Throwable exception, boolean firstReport);
	}

	/** Wraps an exception thrown from {@link CacheItemType#generate(QuickEnvironment, Object)} */
	public static class CacheException extends Exception {
		private final boolean isFirstThrown;

		CacheException(Throwable e, boolean firstThrown) {
			super(e);
			isFirstThrown = firstThrown;
		}

		/** @return Whether this exception is the direct result of the failure of the generation of the item for the first time */
		public boolean isFirstThrown() {
			return isFirstThrown;
		}
	}

	private static class CacheKey<K, V, E extends Exception> {
		private CacheItemType<K, V, E> theType;

		private K theKey;

		private volatile boolean isLoading;

		volatile V theValue;

		volatile Throwable theError;

		final ArrayList<ItemReceiver<K, V>> theReceivers;

		CacheKey(CacheItemType<K, V, E> type, K key) {
			theType = type;
			theKey = key;
			theReceivers = new ArrayList<>();
			isLoading = true;
		}

		CacheItemType<K, V, E> getType() {
			return theType;
		}

		K getKey() {
			return theKey;
		}

		@Override
		public boolean equals(Object o) {
			if(!(o instanceof CacheKey))
				return false;
			CacheKey<?, ?, ?> item = (CacheKey<?, ?, ?>) o;
			return item.theType.equals(theType) && item.theKey.equals(theKey);
		}

		@Override
		public int hashCode() {
			return theType.hashCode() * 13 + (theKey == null ? 0 : theKey.hashCode());
		}
	}

	private org.qommons.DemandCache<CacheKey<?, ?, ?>, CacheKey<?, ?, ?>> theInternalCache;

	private final Object theOuterLock;

	private Worker theWorker;

	/** Creates a Quick cache */
	public QuickCache() {
		theInternalCache = new org.qommons.DemandCache<>(new org.qommons.DemandCache.Qualitizer<CacheKey<?, ?, ?>, CacheKey<?, ?, ?>>() {
			@Override
			public float quality(CacheKey<?, ?, ?> key, CacheKey<?, ?, ?> value) {
				if(key.isLoading)
					return 1000000000f;
				else
					return 1;
			}

			@Override
			public float size(CacheKey<?, ?, ?> key, CacheKey<?, ?, ?> value) {
				if(key.isLoading)
					return 0;
				else
					return ((CacheKey<?, Object, ?>) key).getType().size(value.theValue);
			}
		}, 100000000f, 60L * 60 * 1000);
		theOuterLock = new Object();
		theWorker = new ThreadPoolWorker("Quick Cache Worker", 4);
	}

	/**
	 * Retrieves a cached item and optionally generates the item if not cached, waiting for the generation to be complete
	 *
	 * @param <K> The type of key for the cached item
	 * @param <V> The type of value for the cached item
	 * @param <E> The type of exception that may be thrown when generating the cached item
	 * @param env The environment to get the resource within
	 * @param type The type of item to get
	 * @param key The key to get the cached item by
	 * @param generate Whether to generate the value if it does not currently exist in the cache
	 * @return The value cached for the given type and key, or null if it has not been generated and {@code generated} is false
	 * @throws CacheException If an exception occurs generating the cache value
	 */
	public <K, V, E extends Exception> V getAndWait(QuickEnvironment env, CacheItemType<K, V, E> type, K key, boolean generate)
		throws CacheException {
		Object[] reportedValue = new Object[1];
		Throwable[] reportedException = new Throwable[1];
		boolean[] firstThrown = new boolean[1];
		boolean[] reported = new boolean[1];
		if (generate) {
			get(env, type, key, new ItemReceiver<K, V>() {
				@Override
				public void itemGenerated(K key2, V value) {
					reportedValue[0] = value;
					reported[0] = true;
				}

				@Override
				public void errorOccurred(K key2, Throwable exception, boolean firstReport) {
					reportedException[0] = exception;
					firstThrown[0] = firstReport;
					reported[0] = true;
				}
			});

			while (!reported[0]) {
				try {
					Thread.sleep(25);
				} catch (InterruptedException e) {
				}
			}
			if (reportedException[0] != null)
				throw new CacheException(reportedException[0], firstThrown[0]);
			else
				return (V) reportedValue[0];
		} else {
			CacheKey<K, V, E> stored = (CacheKey<K, V, E>) theInternalCache.get(new CacheKey<>(type, key));
			if (stored == null)
				return null;
			else if (stored.theError != null)
				throw new CacheException(stored.theError, false);
			else
				return stored.theValue;
		}
	}

	/**
	 * An asynchronous get method
	 *
	 * @param <K> The type of key for the cached item
	 * @param <V> The type of value for the cached item
	 * @param <E> The type of exception that may be thrown when generating the cached item
	 * @param env The Quick environment to use to generate the value
	 * @param type The type of the cached item to get
	 * @param key The key of the cached item to get
	 * @param receiver A receiver to be notified when the cached item is available. May be null.
	 */
	public <K, V, E extends Exception> void get(QuickEnvironment env, CacheItemType<K, V, E> type, K key, ItemReceiver<K, V> receiver) {
		CacheKey<K, V, E> cacheKey = new CacheKey<>(type, key);
		CacheKey<K, V, E> stored = (CacheKey<K, V, E>) theInternalCache.get(cacheKey);
		boolean needsGen = stored == null;
		if (needsGen) {
			synchronized(theOuterLock) {
				stored = (CacheKey<K, V, E>) theInternalCache.get(cacheKey);
				needsGen = stored == null;
				if (needsGen) {
					stored = cacheKey;
					theInternalCache.put(cacheKey, stored);
					startGet(env, stored);
				}
			}
		}
		if (stored.isLoading && receiver != null) {
			synchronized(stored.theReceivers) {
				if(stored.isLoading) {
					stored.theReceivers.add(receiver);
					return;
				}
			}
		}
		if (stored.theError != null)
			receiver.errorOccurred(key, stored.theError, needsGen);
		else
			receiver.itemGenerated(key, stored.theValue);
	}

	/**
	 * @param <K> The type of key for the cached item
	 * @param <V> The type of value for the cached item
	 * @param type The type of item to remove from the cache
	 * @param key The key of the item to remove from the cache
	 * @return The value cached for the given type and key that was removed
	 */
	public <K, V> V remove(CacheItemType<K, V, ?> type, K key) {
		CacheKey<K, V, ?> cacheKey = new CacheKey<>(type, key);
		return (V) theInternalCache.remove(cacheKey);
	}

	private <K, V, E extends Exception> void startGet(final QuickEnvironment env, final CacheKey<K, V, E> key) {
		theWorker.run(() -> {
			try {
				key.theValue = key.getType().generate(env, key.getKey());
			} catch(Throwable e) {
				key.theError = e;
			} finally {
				try {
					synchronized(key.theReceivers) {
						for(ItemReceiver<K, V> receiver : key.theReceivers)
							if(key.theError != null)
								receiver.errorOccurred(key.getKey(), key.theError, true);
							else
								receiver.itemGenerated(key.getKey(), key.theValue);
					}
				} finally {
					key.isLoading = false;
				}
			}
		}, new Worker.ErrorListener() {
			@Override
			public void error(Error error) {
			}

			@Override
			public void runtime(RuntimeException ex) {
			}
		});
	}
}
