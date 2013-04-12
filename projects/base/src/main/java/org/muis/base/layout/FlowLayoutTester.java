package org.muis.base.layout;

import org.muis.core.MuisElement;
import org.muis.core.layout.*;

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
		theMainGuide = new FlowLayoutTesterMainSizeGuide();
		theCrossGuide = new FlowLayoutTesterCrossSizeGuide();
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

	/**
	 * Wraps at the next spot where it shortens the main length the most, according to the given size type
	 *
	 * @param type The size type to shorten the most by the wrap
	 * @return True if the wrapping was changed; false if all components are already wrapped
	 */
	public boolean wrapNext(LayoutGuideType type) {
	}

	/**
	 * Unwraps at the next spot where it shortens the cross length the most, according to the given size type
	 *
	 * @param type The size type to shorten the most by the wrap
	 * @return True if the wrapping was changed; false if no components are wrapped
	 */
	public boolean unwrapNext(LayoutGuideType type) {
	}

	public boolean setWrappedAfter(int childIndex, boolean wrapped) {
		theWraps[childIndex] = wrapped;
	}

	static int getMaxSize(MuisElement [] children, int start, int end, Orientation orient, LayoutGuideType type, int size) {

		// TODO take baseline into account
	}

	static int getSumSize(MuisElement [] children, int start, int end, Orientation orient, LayoutGuideType type, int crossSize,
		boolean csMax) {
		LayoutSize ret = new LayoutSize();
		for(MuisElement child : children) {
			LayoutUtils.getSize(child, orient, type, Integer.MAX_VALUE, crossSize, csMax, ret);
		}
		return ret.getTotal();
	}

	private class FlowLayoutTesterMainSizeGuide implements SizeGuide {
		FlowLayoutTesterMainSizeGuide() {
		}

		/**
		 * Determines the row heights (or column widths for vertical layout) for each wrapped row of widgets
		 *
		 * @param crossSize The size of the cross dimension
		 * @return The heights for each row/column in the layout
		 */
		private int [] getRowHeights(int crossSize) {
			/* Sequence:
			 * * If the sum of the maximum of the preferred sizes for the widgets in each row is <=crossSize, use those.
			 * * else if the sum of the maximum of the minimum sizes for the widgets in each row is >=crossSize, use those.
			 * * else if ...                       ... min pref sizes...                            >=crossSize, find a variable prop
			 *     such that the sum of min+prop*(minPref-min) for each row is crossSize, where min and minPref are the maximum of the
			 *     minimum and min pref sizes for the widgets in each row.
			 * * else find a variable prop
			 *     such that the sum of minPref+prop*(pref-minPref) for each row is crossSize, where minPref and pref are the maximum of the
			 *     min pref and preferred sizes for the widgets in each row.
			 * Using the determined row heights as cross sizes, get the maximum of the sum of the minimum sizes for each row.
			 */
			int rowCount = 1;
			for(boolean wrap : theWraps)
				if(wrap)
					rowCount++;
			int [] rowHeights = new int[rowCount];
			Orientation crossOrient = theOrientation.opposite();
			// Calculate the sum of the maximum of the preferred sizes for the widgets in each row
			LayoutSize prefRowTotal = new LayoutSize();
			int lastBreak = 0;
			int rowIndex = 0;
			int rowHeight;
			for(int c = 1; c < theChildren.length; c++) {
				if(theWraps[c]) {
					rowHeight = getMaxSize(theChildren, lastBreak, c, crossOrient, LayoutGuideType.pref);
					rowHeights[rowIndex++] = rowHeight;
					prefRowTotal.add(rowHeight);
					lastBreak = c;
				}
			}
			rowHeight = getMaxSize(theChildren, lastBreak, theChildren.length, crossOrient, LayoutGuideType.pref);
			rowHeights[rowIndex++] = rowHeight;
			prefRowTotal.add(rowHeight);

			if(prefRowTotal.getTotal() > crossSize) {
				int [] prefRowHeights = rowHeights;
				rowHeights = new int[rowCount];
				// Calculate the sum of the minimum of the preferred sizes for the widgets in each row
				LayoutSize minRowTotal = new LayoutSize();
				lastBreak = 0;
				rowIndex = 0;
				for(int c = 1; c < theChildren.length; c++) {
					if(theWraps[c]) {
						rowHeight = getMaxSize(theChildren, lastBreak, c, crossOrient, LayoutGuideType.min);
						rowHeights[rowIndex++] = rowHeight;
						minRowTotal.add(rowHeight);
						lastBreak = c;
					}
				}
				rowHeight = getMaxSize(theChildren, lastBreak, theChildren.length, crossOrient, LayoutGuideType.min);
				rowHeights[rowIndex++] = rowHeight;
				minRowTotal.add(rowHeight);

				if(minRowTotal.getTotal() <= crossSize) {
					int [] minRowHeights = rowHeights;
					int [] minPrefRowHeights = new int[rowCount];
					rowHeights = new int[rowCount];
					// Calculate the sum of the minimum of the min pref sizes for the widgets in each row
					LayoutSize minPrefRowTotal = new LayoutSize();
					lastBreak = 0;
					rowIndex = 0;
					for(int c = 1; c < theChildren.length; c++) {
						if(theWraps[c]) {
							rowHeight = getMaxSize(theChildren, lastBreak, c, crossOrient, LayoutGuideType.minPref);
							minPrefRowHeights[rowIndex++] = rowHeight;
							minPrefRowTotal.add(rowHeight);
							lastBreak = c;
						}
					}
					rowHeight = getMaxSize(theChildren, lastBreak, theChildren.length, crossOrient, LayoutGuideType.minPref);
					minPrefRowHeights[rowIndex++] = rowHeight;
					minPrefRowTotal.add(rowHeight);

					float prop;
					if(minPrefRowTotal.getTotal() >= crossSize) {
						prop = (crossSize - minRowTotal.getTotal()) * 1.0f / (minPrefRowTotal.getTotal() - minRowTotal.getTotal());
						for(int r = 0; r < rowCount; r++)
							rowHeights[r] = minRowHeights[r] + Math.round(prop * (minPrefRowHeights[r] - minRowHeights[r]));
					} else {
						prop = (crossSize - minPrefRowTotal.getTotal()) * 1.0f / (prefRowTotal.getTotal() - minPrefRowTotal.getTotal());
						for(int r = 0; r < rowCount; r++)
							rowHeights[r] = minPrefRowHeights[r] + Math.round(prop * (prefRowHeights[r] - minPrefRowHeights[r]));
					}
				}
			}
			return rowHeights;
		}

		@Override
		public int getMin(int crossSize, boolean csMax) {
			return get(LayoutGuideType.min, crossSize, csMax);
		}

		@Override
		public int getMinPreferred(int crossSize, boolean csMax) {
			return get(LayoutGuideType.minPref, crossSize, csMax);
		}

		@Override
		public int getPreferred(int crossSize, boolean csMax) {
			return get(LayoutGuideType.pref, crossSize, csMax);
		}

		@Override
		public int getMaxPreferred(int crossSize, boolean csMax) {
			return get(LayoutGuideType.maxPref, crossSize, csMax);
		}

		@Override
		public int getMax(int crossSize, boolean csMax) {
			return get(LayoutGuideType.max, crossSize, csMax);
		}

		@Override
		public int get(LayoutGuideType type, int crossSize, boolean csMax) {
			int [] rowHeights = getRowHeights(crossSize);
			int max = 0;
			int lastBreak = 0;
			int rowIndex = 0;
			for(int c = 1; c < theChildren.length; c++) {
				if(theWraps[c]) {
					int rowSize = getSumSize(theChildren, lastBreak, c, theOrientation, type, rowHeights[rowIndex], csMax || type.isPref());
					if(rowSize > max)
						max = rowSize;
				}
			}
			int rowSize = getSumSize(theChildren, lastBreak, theChildren.length, theOrientation, type, rowHeights[rowIndex], type.isPref());
			if(rowSize > max)
				max = rowSize;
			return max;
		}

		@Override
		public int getBaseline(int size) {
			switch (theOrientation) {
			case horizontal:
				return 0;
			case vertical:
				return size;
			}
			return 0;
		}
	}

	class FlowLayoutTesterCrossSizeGuide implements SizeGuide {
	}
}
