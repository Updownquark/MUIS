package org.muis.util;


public class BiTuple<V1, V2> {
	private final V1 theValue1;
	private final V2 theValue2;

	public BiTuple(V1 v1, V2 v2) {
		theValue1 = v1;
		theValue2 = v2;
	}

	public V1 getValue1() {
		return theValue1;
	}

	public V2 getValue2() {
		return theValue2;
	}

	public boolean hasValue() {
		return theValue1 != null || theValue2 != null;
	}

	public boolean has2Values() {
		return theValue1 != null && theValue2 != null;
	}
}
