package org.quick.base.layout;

public interface SizeTickIterator {
	int getSize();

	float getTension();

	int getNextSize();

	int getPreviousSize();

	default float getTension(int size) {
		int now = getSize();
		if (size == now)
			return getTension();
		if (size < now) {
			int prev = getPreviousSize();
			while (size < prev && prev < now) {
				now = prev;
				prev = getPreviousSize();
			}
			if (prev == now)
				return LayoutSpring.MAX_TENSION;
			if (prev == size)
				return getTension();
			return interpolateFloat(prev, getTension(), getNextSize(), getTension(), size);
		} else {
			int next = getNextSize();
			while (size < next && next > now) {
				now = next;
				next = getNextSize();
			}
			if (next == now)
				return -LayoutSpring.MAX_TENSION;
			return interpolateFloat(next, getTension(), getPreviousSize(), getTension(), size);
		}
	}

	default int getSize(float tension) {
		float nowT = getTension();
		int now = getSize();
		if (tension == nowT)
			return now;
		if (tension < nowT) {
			int next = getNextSize();
			float nextT = getTension();
			while (tension < nextT && next > now) {
				now = next;
				next = getNextSize();
				nextT = getTension();
			}
			;
			if (next == now)
				return next;
			return interpolateInt(next, nextT, getPreviousSize(), getTension(), tension);
		} else {
			int prev = getPreviousSize();
			float prevT = getTension();
			while (tension > prevT && prev < now) {
				now = prev;
				prev = getPreviousSize();
				prevT = getTension();
			}
			if (prev == now)
				return prev;
			return interpolateInt(prev, prevT, getNextSize(), getTension(), tension);
		}
	}

	public static float interpolateFloat(int lowI, float lowF, int highI, float highF, int iValue) {
		return lowF + (highF - lowF) * (iValue - lowI) / (highI - lowI);
	}

	public static int interpolateInt(int lowI, float lowF, int highI, float highF, float fValue) {
		return lowI + (int) ((highI - lowI) * (fValue - lowF) / (highF - lowF));
	}
}
