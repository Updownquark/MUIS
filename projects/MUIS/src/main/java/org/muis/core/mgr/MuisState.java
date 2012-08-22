package org.muis.core.mgr;

/** Represents a binary state of an element in MUIS */
public class MuisState implements Comparable<MuisState> {
	private final String theName;

	private final int thePriority;

	/**
	 * @param name The name of the state
	 * @param priority The priority of the state
	 */
	public MuisState(String name, int priority) {
		checkStateName(name);
		if(priority < 0)
			throw new IllegalArgumentException("State priority must be >=0");
		theName = name;
		thePriority = priority;
	}

	/** @return This state's name */
	public String getName() {
		return theName;
	}

	/** @return This state's priority */
	public int getPriority() {
		return thePriority;
	}

	@Override
	public int compareTo(MuisState o) {
		int ret = thePriority - o.thePriority;
		if(ret != 0)
			return ret;
		return theName.compareTo(o.theName);
	}

	@Override
	public String toString() {
		return theName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((theName == null) ? 0 : theName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		MuisState other = (MuisState) obj;
		if(theName == null) {
			if(other.theName != null)
				return false;
		} else if(!theName.equals(other.theName))
			return false;
		return true;
	}

	/**
	 * Checks a state name for validity. This is called from the constructor to avoid creating invalid state instances.
	 *
	 * @param name The name of the state to check
	 * @throws IllegalArgumentException If the state name is invalid for any reason
	 */
	public static void checkStateName(String name) throws IllegalArgumentException {
		if(name.length() == 0)
			throw new IllegalArgumentException("State name may not be empty");
		if(name.startsWith("-"))
			throw new IllegalArgumentException("State name may not start with '-': " + name);
		if(name.contains("."))
			throw new IllegalArgumentException("State name may not contain '.': " + name);
		if(name.contains("_"))
			throw new IllegalArgumentException("State name may not contain '_': " + name);
		if(!name.matches("[a-zA-Z0-9-]*"))
			throw new IllegalArgumentException("State names may only contain number, letters and dashes: " + name);
	}
}
