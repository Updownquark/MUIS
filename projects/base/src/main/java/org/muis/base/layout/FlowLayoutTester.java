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

	static int getMaxSize(MuisElement [] children, int start, int end, Orientation orient, LayoutGuideType type) {
	}

	static int getSumSize(MuisElement [] children, int start, int end, Orientation orient, LayoutGuideType type, int crossSize,
		boolean csMax) {
	}

	private class FlowLayoutTesterMainSizeGuide implements SizeGuide {
		FlowLayoutTesterMainSizeGuide() {
		}

		@Override
		public int getMin(int crossSize, boolean csMax) {
			/* Sequence:
			 * Determine the row heights (or column widths for vertical layout).
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
						minRowTotal += rowHeight;
						lastBreak = c;
					}
				}
				rowHeight = getMaxSize(theChildren, lastBreak, theChildren.length, crossOrient, LayoutGuideType.min);
				rowHeights[rowIndex++] = rowHeight;
				minRowTotal += rowHeight;

				if(minRowTotal <= crossSize) {
					int [] minRowHeights = rowHeights;
					int [] minPrefRowHeights = new int[rowCount];
					rowHeights = new int[rowCount];
					// Calculate the sum of the minimum of the min pref sizes for the widgets in each row
					int minPrefRowTotal = 0;
					lastBreak = 0;
					rowIndex = 0;
					for(int c = 1; c < theChildren.length; c++) {
						if(theWraps[c]) {
							rowHeight = getMaxSize(theChildren, lastBreak, c, crossOrient, LayoutGuideType.minPref);
							minPrefRowHeights[rowIndex++] = rowHeight;
							minPrefRowTotal += rowHeight;
							lastBreak = c;
						}
					}
					rowHeight = getMaxSize(theChildren, lastBreak, theChildren.length, crossOrient, LayoutGuideType.minPref);
					minPrefRowHeights[rowIndex++] = rowHeight;
					minPrefRowTotal += rowHeight;

					float prop;
					if(minPrefRowTotal >= crossSize) {
						prop = (crossSize - minRowTotal) * 1.0f / (minPrefRowTotal - minRowTotal);
						for(int r = 0; r < rowCount; r++)
							rowHeights[r] = minRowHeights[r] + Math.round(prop * (minPrefRowHeights[r] - minRowHeights[r]));
					} else {
						prop = (crossSize - minPrefRowTotal) * 1.0f / (prefRowTotal.getTotal() - minPrefRowTotal);
						for(int r = 0; r < rowCount; r++)
							rowHeights[r] = minPrefRowHeights[r] + Math.round(prop * (prefRowHeights[r] - minPrefRowHeights[r]));
					}
				}
			}
			// rowHeights is correct now
			int max = 0;
			lastBreak = 0;
			rowIndex = 0;
			for(int c = 1; c < theChildren.length; c++) {
				if(theWraps[c]) {
					int rowSize = getSumSize(theChildren, lastBreak, c, theOrientation, LayoutGuideType.min, rowHeights[rowIndex], csMax);
					if(rowSize > max)
						max = rowSize;
				}
			}
			int rowSize = getSumSize(theChildren, lastBreak, theChildren.length, theOrientation, LayoutGuideType.min, rowHeights[rowIndex],
				csMax);
			if(rowSize > max)
				max = rowSize;
			return max;
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
