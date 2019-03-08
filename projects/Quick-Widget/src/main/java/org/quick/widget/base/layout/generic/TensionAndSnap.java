package org.quick.widget.base.layout.generic;

public class TensionAndSnap {
	public static final TensionAndSnap ZERO = new TensionAndSnap(0, 0);
	public final float tension;
	public final int snap;

	public TensionAndSnap(float tension, int snap) {
		if (Float.isNaN(tension))
			throw new IllegalArgumentException("NaN");
		this.tension = tension;
		this.snap = snap;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof TensionAndSnap && tension == ((TensionAndSnap) o).tension && snap == ((TensionAndSnap) o).snap;
	}

	@Override
	public String toString() {
		return tension + "->" + snap;
	}
}