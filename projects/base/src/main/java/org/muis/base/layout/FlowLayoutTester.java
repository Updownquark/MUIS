package org.muis.base.layout;

import org.muis.core.MuisElement;
import org.muis.core.layout.LayoutGuideType;
import org.muis.core.layout.Orientation;
import org.muis.core.layout.SizeGuide;

public class FlowLayoutTester {
	private MuisElement [] theChildren;

	private Orientation theOrientation;

	private boolean [] theWraps;

	private final SizeGuide theMainGuide;

	private final SizeGuide theCrossGuide;

	public FlowLayoutTester(Orientation main, MuisElement... children) {
		theChildren = children;
		theOrientation = main;
		theWraps = new boolean[children.length - 1];
		theMainGuide = new FlowLayoutTesterSizeGuide(true);
		theCrossGuide = new FlowLayoutTesterSizeGuide(false);
	}

	public SizeGuide main() {
		return theMainGuide;
	}

	public SizeGuide cross() {
		return theCrossGuide;
	}

	public void wrapAll() {
		for(int i = 0; i < theWraps.length; i++)
			theWraps[i] = true;
	}

	public void unwrapAll() {
		for(int i = 0; i < theWraps.length; i++)
			theWraps[i] = false;
	}

	public boolean wrapNext(LayoutGuideType type) {
	}

	public boolean unwrapNext(LayoutGuideType type) {
	}

	private class FlowLayoutTesterMainSizeGuide implements SizeGuide {
		FlowLayoutTesterMainSizeGuide() {
		}

		@Override
		public int getMin(int crossSize, boolean csMax) {
			int pixels=0;
			int percent=0;
			for(int c=0;c<theChildren.length;c++){
				Size size=theChildren[c].
			}
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getMinPreferred(int crossSize, boolean csMax) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getPreferred(int crossSize, boolean csMax) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getMaxPreferred(int crossSize, boolean csMax) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getMax(int crossSize, boolean csMax) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int get(LayoutGuideType type, int crossSize, boolean csMax) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getBaseline(int size) {
			// TODO Auto-generated method stub
			return 0;
		}
	}
}
