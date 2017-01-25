package org.quick.base.layout;

import org.quick.core.layout.SizeGuide;

public interface SizeGuide2D extends SizeGuide {
	SizeGuide getOpposite();

	class BoundsGuide2D implements SizeGuide2D {
		private final SizeGuide thePrimary;
		private final SizeGuide theCross;

		public BoundsGuide2D(SizeGuide primary, SizeGuide cross) {
			thePrimary = primary;
			theCross = cross;
		}

		@Override
		public int getMin(int crossSize) {
			return thePrimary.getMin(crossSize);
		}

		@Override
		public int getMinPreferred(int crossSize) {
			return thePrimary.getMinPreferred(crossSize);
		}

		@Override
		public int getPreferred(int crossSize) {
			return thePrimary.getPreferred(crossSize);
		}

		@Override
		public int getMaxPreferred(int crossSize) {
			return thePrimary.getMaxPreferred(crossSize);
		}

		@Override
		public int getMax(int crossSize) {
			return thePrimary.getMax(crossSize);
		}

		@Override
		public int getBaseline(int size) {
			return thePrimary.getBaseline(size);
		}

		@Override
		public SizeGuide getOpposite() {
			return theCross;
		}
	}
}
