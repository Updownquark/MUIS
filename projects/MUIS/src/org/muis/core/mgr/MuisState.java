package org.muis.core.mgr;

/** Represents a binary state of an element in MUIS */
public class MuisState {
	private final String theName;

	private final int thePriority;

	/**
	 * @param name The name of the state
	 * @param priority The priority of the state
	 */
	public MuisState(String name, int priority) {
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
}
