package org.muis.core;

import java.util.HashSet;

public class MuisLock implements AutoCloseable
{
	public static class Locker
	{
		private final Object theSync;

		private java.util.HashMap<Object, HashSet<MuisElement>> theElementSets;

		private java.util.ArrayList<MuisLock> theLocks;

		public Locker()
		{
			theSync = new Object();
			theElementSets = new java.util.HashMap<>();
			theLocks = new java.util.ArrayList<>();
		}

		public void lock(MuisLock lock)
		{
			boolean locked = false;
			do
				synchronized(theSync)
				{
					if(canLockSynced(lock.getElements()))
					{
						lockSynced(lock);
						locked = true;
					}
				}
			while(!locked);
		}

		public boolean lockIfUnlocked(MuisLock lock)
		{
			boolean locked = false;
			synchronized(theSync)
			{
				if(canLockSynced(lock.getElements()))
				{
					locked = true;
					lockSynced(lock);
				}
			}
			return locked;
		}

		public boolean canLock(MuisLock lock)
		{
			synchronized(theSync)
			{
				return canLockSynced(lock.getElements());
			}
		}

		public boolean isLocked(Object type, MuisElement element)
		{
			synchronized(theSync)
			{
				HashSet<MuisElement> set = theElementSets.get(type);
				return set == null ? false : set.contains(element);
			}
		}

		void unlock(MuisLock lock)
		{
			synchronized(theSync)
			{
				unlockSynced(lock);
			}
		}

		private boolean canLockSynced(Object type, MuisElement... els)
		{
			HashSet<MuisElement> set = theElementSets.get(type);
			if(set == null)
				return true;
			for(MuisElement el : els)
				if(set.contains(el))
					return false;
			return true;
		}

		public void lockSynced(MuisLock lock)
		{
			HashSet<MuisElement> set = theElementSets.get(lock.getType());
			if(set == null)
			{
				set = new HashSet<>();
				theElementSets.put(lock.getType(), set);
			}
			theLocks.add(lock);
			for(MuisElement el : lock.getElements())
				set.add(el);
			lock.setLocker(this);
		}

		private void unlockSynced(MuisLock lock)
		{
			if(theLocks.remove(lock))
			{
				HashSet<MuisElement> set = theElementSets.get(lock.getType());
				for(MuisElement el : lock.getElements())
					set.remove(el);
			}
		}

		/**
		 * Locks the hierarchy from an ancestor to a parent. If used with {@link MuisElement#CHILDREN_LOCK_TYPE}, this will ensure that the
		 * path from ancestor to descendant remains constant while the lock is held.
		 *
		 * @param type The type of the lock
		 * @param ancestor The ancestor to lock from
		 * @param descendant The descendant to lock to--this element itself is not locked
		 * @param doLock Whether to enforce the lock immediately or merely create the lock instance
		 * @return The lock instance
		 */
		public MuisLock lockFrom(Object type, MuisElement ancestor, MuisElement descendant, boolean doLock)
		{
			java.util.ArrayList<MuisElement> els = new java.util.ArrayList<>();
			descendant = descendant.getParent();
			while(descendant != null && descendant != ancestor)
				els.add(descendant);
			if(descendant == null)
				throw new IllegalArgumentException("Elements are not related by descent");
			MuisLock ret = new MuisLock(type, els.toArray(new MuisElement[els.size()]));
			if(doLock)
				lock(ret);
			return ret;
		}

		public MuisLock lock(Object type, MuisElement element, boolean doLock)
		{
			MuisLock ret = new MuisLock(type, element);
			if(doLock)
				lock(ret);
			return ret;
		}
	}

	private final MuisElement [] theElements;

	private final Object theType;

	private volatile Locker theLocker;

	public MuisLock(Object type, MuisElement... els)
	{
		theElements = els;
		theType = type;
	}

	public MuisElement [] getElements()
	{
		return theElements.clone();
	}

	public Object getType()
	{
		return theType;
	}

	public boolean isActive()
	{
		return theLocker != null;
	}

	void setLocker(Locker locker)
	{
		theLocker = locker;
	}

	@Override
	public void close()
	{
		synchronized(theElements)
		{
			theLocker.unlock(this);
			theLocker = null;
		}
	}
}
