package org.muis.core;

import java.util.ArrayList;

/** Enables caching of resources in MUIS */
public class MuisCache
{
	/**
	 * Represents a type of item that can be cached
	 *
	 * @param <K> The key type for the cached item type
	 * @param <V> The value type for the cached item type
	 * @param <E> The type of exception that may be thrown when this item type generates a value
	 */
	public interface CacheItemType<K, V, E extends Exception>
	{
		/**
		 * @param env The MUIS environment to generate the resource for
		 * @param key The key to generate the cached value for
		 * @return The value to cache and return for the given key
		 * @throws E If an error occurs generating the value
		 */
		V generate(MuisEnvironment env, K key) throws E;

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
	public interface ItemReceiver<K, V>
	{
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
		 */
		public void errorOccurred(K key, Throwable exception);
	}

	private static class CacheKey<K, V, E extends Exception>
	{
		private CacheItemType<K, V, E> theType;

		private K theKey;

		private volatile boolean isLoading;

		volatile V theValue;

		volatile Throwable theError;

		final ArrayList<ItemReceiver<K, V>> theReceivers;

		CacheKey(CacheItemType<K, V, E> type, K key)
		{
			theType = type;
			theKey = key;
			theReceivers = new ArrayList<>();
			isLoading = true;
		}

		CacheItemType<K, V, E> getType()
		{
			return theType;
		}

		K getKey()
		{
			return theKey;
		}

		@Override
		public boolean equals(Object o)
		{
			if(!(o instanceof CacheKey))
				return false;
			CacheKey<?, ?, ?> item = (CacheKey<?, ?, ?>) o;
			return item.theType.equals(theType) && item.theKey.equals(theKey);
		}

		@Override
		public int hashCode()
		{
			return theType.hashCode() * 13 + (theKey == null ? 0 : theKey.hashCode());
		}
	}

	private prisms.util.DemandCache<CacheKey<?, ?, ?>, CacheKey<?, ?, ?>> theInternalCache;

	private final Object theOuterLock;

	private prisms.arch.Worker theWorker;

	/** Creates a MUIS cache */
	public MuisCache()
	{
		theInternalCache = new prisms.util.DemandCache<>(
			new prisms.util.DemandCache.Qualitizer<CacheKey<?, ?, ?>, CacheKey<?, ?, ?>>() {
				@Override
				public float quality(CacheKey<?, ?, ?> key, CacheKey<?, ?, ?> value)
				{
					if(key.isLoading)
						return 1000000000f;
					else
						return 1;
				}

				@Override
				public float size(CacheKey<?, ?, ?> key, CacheKey<?, ?, ?> value)
				{
					if(key.isLoading)
						return 0;
					else
						return ((CacheKey<?, Object, ?>) key).getType().size(value.theValue);
				}
			}, 100000000f, 60L * 60 * 1000);
		theOuterLock = new Object();
		theWorker = new prisms.impl.ThreadPoolWorker("MUIS Cache Worker", 4);
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
	 * @return The value cached for the given type and key
	 * @throws E If an exception occurs generating a new cache value
	 */
	public <K, V, E extends Exception> V getAndWait(MuisEnvironment env, CacheItemType<K, V, E> type, K key) throws E
	{
		CacheKey<K, V, E> cacheKey = new CacheKey<>(type, key);
		CacheKey<K, V, E> stored = (CacheKey<K, V, E>) theInternalCache.get(cacheKey);
		if(stored == null)
		{
			stored = (CacheKey<K, V, E>) theInternalCache.get(cacheKey);
			while(stored == null)
			{ // This is in a while loop because it's remotely possible that the entry could be purged before get returns
				get(env, type, key, null);
				stored = (CacheKey<K, V, E>) theInternalCache.get(cacheKey);
			}
		}
		assert stored != null : "Should not return null from getAndWait if generate is true";

		while(stored.isLoading)
			try
			{
				Thread.sleep(10);
			} catch(InterruptedException e)
			{
			}
		if(stored.theError != null)
		{
			if(stored.theError instanceof RuntimeException)
				throw (RuntimeException) stored.theError;
			else if(stored.theError instanceof Error)
				throw (Error) stored.theError;
			else
				throw (E) stored.theError;
		}
		else
			return stored.theValue;
	}

	/**
	 * An asynchronous get method
	 *
	 * @param <K> The type of key for the cached item
	 * @param <V> The type of value for the cached item
	 * @param <E> The type of exception that may be thrown when generating the cached item
	 * @param env The MUIS environment to use to generate the value
	 * @param type The type of the cached item to get
	 * @param key The key of the cached item to get
	 * @param receiver A receiver to be notified when the cached item is available. May be null.
	 * @return The cached item if it is immediately available, otherwise null
	 * @throws E The error that occurred while generating the cache value, if the cache has already attempted to generate the item and
	 *             failed
	 */
	public <K, V, E extends Exception> V get(MuisEnvironment env, CacheItemType<K, V, E> type, K key, ItemReceiver<K, V> receiver) throws E
	{
		CacheKey<K, V, E> cacheKey = new CacheKey<>(type, key);
		CacheKey<K, V, E> stored = (CacheKey<K, V, E>) theInternalCache.get(cacheKey);
		if(stored == null)
			synchronized(theOuterLock)
			{
				stored = (CacheKey<K, V, E>) theInternalCache.get(cacheKey);
				if(stored == null)
				{
					stored = cacheKey;
					theInternalCache.put(cacheKey, stored);
					startGet(env, stored);
				}
			}
		if(stored.isLoading && receiver != null)
			synchronized(stored.theReceivers)
			{
				if(stored.isLoading)
				{
					stored.theReceivers.add(receiver);
					return null;
				}
			}
		if(stored.theError != null)
		{
			if(receiver != null)
				receiver.errorOccurred(key, stored.theError);
			if(stored.theError instanceof RuntimeException)
				throw (RuntimeException) stored.theError;
			else if(stored.theError instanceof Error)
				throw (Error) stored.theError;
			else
				throw (E) stored.theError;
		}
		else
		{
			if(receiver != null)
				receiver.itemGenerated(key, stored.theValue);
			return stored.theValue;
		}
	}

	/**
	 * @param <K> The type of key for the cached item
	 * @param <V> The type of value for the cached item
	 * @param type The type of item to remove from the cache
	 * @param key The key of the item to remove from the cache
	 * @return The value cached for the given type and key that was removed
	 */
	public <K, V> V remove(CacheItemType<K, V, ?> type, K key)
	{
		CacheKey<K, V, ?> cacheKey = new CacheKey<>(type, key);
		return (V) theInternalCache.remove(cacheKey);
	}

	private <K, V, E extends Exception> void startGet(final MuisEnvironment env, final CacheKey<K, V, E> key)
	{
		theWorker.run(new Runnable() {
			@Override
			public void run()
			{
				try
				{
					key.theValue = key.getType().generate(env, key.getKey());
				} catch(Throwable e)
				{
					key.theError = e;
				} finally
				{
					try
					{
						synchronized(key.theReceivers)
						{
							for(ItemReceiver<K, V> receiver : key.theReceivers)
								if(key.theError != null)
									receiver.errorOccurred(key.getKey(), key.theError);
								else
									receiver.itemGenerated(key.getKey(), key.theValue);
						}
					} finally
					{
						key.isLoading = false;
					}
				}
			}
		}, new prisms.arch.Worker.ErrorListener() {
			@Override
			public void error(Error error)
			{
			}

			@Override
			public void runtime(RuntimeException ex)
			{
			}
		});
	}
}
