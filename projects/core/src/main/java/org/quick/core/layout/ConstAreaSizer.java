package org.quick.core.layout;

public class ConstAreaSizer extends AbstractSizeGuide {
	private int theArea;

	public ConstAreaSizer(int area) {
		theArea = area;
	}

	@Override
	public int getMin(int crossSize, boolean csMax) {
		return getPreferred(crossSize, csMax);
	}

	@Override
	public int getMinPreferred(int crossSize, boolean csMax) {
		return getPreferred(crossSize, csMax);
	}

	@Override
	public int getPreferred(int crossSize, boolean csMax) {
		if (crossSize == 0)
			crossSize = 30;
		return theArea / crossSize;
	}

	@Override
	public int getMaxPreferred(int crossSize, boolean csMax) {
		return getPreferred(crossSize, csMax);
	}

	@Override
	public int getMax(int crossSize, boolean csMax) {
		return getPreferred(crossSize, csMax);
	}

	@Override
	public int getBaseline(int size) {
		return 0;
	}
}
