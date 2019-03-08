package org.quick.widget.base.layout;

import java.awt.Dimension;
import java.util.ArrayList;

import org.quick.core.layout.LayoutAttributes;
import org.quick.core.layout.LayoutGuideType;
import org.quick.core.layout.LayoutSize;
import org.quick.core.layout.Orientation;
import org.quick.core.style.Size;
import org.quick.util.SimpleCache;
import org.quick.widget.core.QuickWidget;
import org.quick.widget.core.layout.LayoutUtils;
import org.quick.widget.core.layout.SizeGuide;

/** Allows a flow layout to quickly test out different wrapping configurations for a set of widgets */
public class FlowLayoutTester {
	private QuickWidget[] theChildren;

	private Orientation theOrientation;

	private Size thePaddingX;

	private Size thePaddingY;

	private Size theMarginX;

	private Size theMarginY;

	private boolean isFillContainer;

	private boolean [] theWraps;

	private int theCachedCrossSize;

	private int [] theRowHeights;

	private int theBaseline;

	private final SizeGuide theMainGuide;

	private final SizeGuide theCrossGuide;

	/**
	 * @param main The main axis of the flow layout
	 * @param paddingX The padding size in the x-direction
	 * @param paddingY The padding size in the y-direction
	 * @param marginX The margin size in the x-direction
	 * @param marginY The margin size in the y-direction
	 * @param fillContainer The {@link LayoutAttributes#fillContainer} value for the container
	 * @param children The children to lay out
	 */
	public FlowLayoutTester(Orientation main, Size paddingX, Size paddingY, Size marginX, Size marginY, boolean fillContainer,
		QuickWidget... children) {
		theChildren = children;
		theOrientation = main;
		thePaddingX = paddingX;
		thePaddingY = paddingY;
		theMarginX = marginX;
		theMarginY = marginY;
		isFillContainer = fillContainer;
		theWraps = new boolean[children.length == 0 ? 0 : children.length - 1];
		theMainGuide = new FlowLayoutTesterMainSizeGuide();
		theCrossGuide = new FlowLayoutTesterCrossSizeGuide();
	}

	/** @return The size guide along the main axis of the flow layout */
	public SizeGuide main() {
		return theMainGuide;
	}

	/** @return The size guide along the opposite axis of the flow layout */
	public SizeGuide cross() {
		return theCrossGuide;
	}

	/**
	 * @param index The index of the child to check
	 * @return Whether the layout is wrapped immediately after the child at the given index
	 */
	public boolean isWrapped(int index) {
		return theWraps[index];
	}

	private void clearCache() {
		theRowHeights = null;
	}

	/** Causes all children to be wrapped */
	public void wrapAll() {
		clearCache();
		theRowHeights = null;
		for(int i = 0; i < theWraps.length; i++)
			theWraps[i] = true;
	}

	/** Causes all children to be unwrapped */
	public void unwrapAll() {
		clearCache();
		for(int i = 0; i < theWraps.length; i++)
			theWraps[i] = false;
	}

	/**
	 * Wraps at the next spot where it shortens the main length the most, according to the given size type
	 *
	 * @param type The size type to shorten the most by the wrap
	 * @param crossSize The cross size to find the next wrap for
	 * @param csMax Whether crossSize is a maximum or a real widget size
	 * @return True if the wrapping was changed; false if all components are already wrapped
	 */
	public boolean wrapNext(LayoutGuideType type, int crossSize, boolean csMax) {
		if(theChildren.length == 0)
			return false;
		/* First, if there are already wraps, try to replace a wrap, i.e. move an element down or up a row to decrease the main length just
		 * a little.
		 * If there are no wraps or the first step fails to find a beneficial replacement wrap (i.e. rows are optimal for number of wraps)
		 * then find the new wrap location that shortens the main length the most.
		 */
		LayoutSize maxLen = new LayoutSize();
		if(findGoodWrapMovement(type, crossSize, csMax, maxLen))
			return true;

		// Try to find a good place for a new wrap
		boolean [] testWraps = new boolean[theWraps.length];
		System.arraycopy(theWraps, 0, testWraps, 0, theWraps.length);
		for(int c = 0; c < theWraps.length; c++) {
			if(theWraps[c]) {
				continue;
			}
			testWraps[c] = true;
			int [] testRowHeights = getRowHeights(crossSize, testWraps, new int[1]);
			if(getMaxLength(testWraps, testRowHeights, type, crossSize, csMax).getTotal() < maxLen.getTotal()) {
				theWraps[c] = true;
				clearCache();
				return true;
			} else {
				testWraps[c] = false;
			}
		}

		return false;
	}

	private boolean findGoodWrapMovement(LayoutGuideType type, int crossSize, boolean csMax, LayoutSize maxLen) {
		if(theChildren.length == 0)
			return false;

		int lineBreak = 0;
		int [] rowHeights = getRowHeights(crossSize);
		LayoutSize [] rowLengths = new LayoutSize[rowHeights.length];
		int [] rowCounts = new int[rowHeights.length];
		int [] rowBeginIndices = new int[rowHeights.length];
		rowBeginIndices[0] = 0;
		int rowIndex = 0;
		// Populate row lengths and wrap indices
		for(int c = 0; c < theWraps.length; c++) {
			if(!theWraps[c])
				continue;
			rowCounts[rowIndex] = c + 1 - (rowIndex > 0 ? rowCounts[rowIndex - 1] : 0);
			rowBeginIndices[rowIndex + 1] = c + 1;
			rowLengths[rowIndex] = getSumSize(theChildren, lineBreak, c + 1, theOrientation, type, rowHeights[rowIndex], csMax);
			if(rowLengths[rowIndex].getTotal() > maxLen.getTotal())
				maxLen.set(rowLengths[rowIndex]);
			lineBreak = c + 1;
			rowIndex++;
		}
		rowCounts[rowIndex] = theChildren.length - (rowIndex > 0 ? rowCounts[rowIndex - 1] : 0);
		rowLengths[rowIndex] = getSumSize(theChildren, lineBreak, theChildren.length, theOrientation, type, rowHeights[rowIndex], csMax);
		if(rowLengths[rowIndex].getTotal() > maxLen.getTotal())
			maxLen.set(rowLengths[rowIndex]);

		// Try to replace wraps
		boolean [] testWraps = new boolean[theWraps.length];
		System.arraycopy(theWraps, 0, testWraps, 0, theWraps.length);
		for(rowIndex = 0; rowIndex < rowHeights.length - 1; rowIndex++) {
			if(rowLengths[rowIndex].getTotal() < maxLen.getTotal() && rowLengths[rowIndex + 1].getTotal() < maxLen.getTotal())
				continue; // Can't decrease the max length here
			if(rowLengths[rowIndex].getTotal() > rowLengths[rowIndex + 1].getTotal() && rowCounts[rowIndex] > 1) {
				// Try moving the last child on the upper row down
				testWraps[rowBeginIndices[rowIndex + 1] - 1] = true;
				testWraps[rowBeginIndices[rowIndex + 1]] = false;
				int [] testRowHeights = getRowHeights(crossSize, testWraps, new int[1]);
				if(getMaxLength(testWraps, testRowHeights, type, crossSize, csMax).getTotal() < maxLen.getTotal()) {
					// Success!
					theWraps[rowBeginIndices[rowIndex + 1] - 1] = true;
					theWraps[rowBeginIndices[rowIndex + 1]] = false;
					clearCache();
					return true;
				} else {
					testWraps[rowBeginIndices[rowIndex + 1] - 1] = false;
					testWraps[rowBeginIndices[rowIndex + 1]] = true;
				}
			} else if(rowLengths[rowIndex].getTotal() < rowLengths[rowIndex + 1].getTotal() && rowCounts[rowIndex + 1] > 1) {
				// Try moving the first child on the lower row up
				testWraps[rowBeginIndices[rowIndex + 1] + 1] = true;
				testWraps[rowBeginIndices[rowIndex + 1]] = false;
				int [] testRowHeights = getRowHeights(crossSize, testWraps, new int[1]);
				if(getMaxLength(testWraps, testRowHeights, type, crossSize, csMax).getTotal() < maxLen.getTotal()) {
					// Success!
					theWraps[rowBeginIndices[rowIndex + 1] + 1] = true;
					theWraps[rowBeginIndices[rowIndex + 1]] = false;
					clearCache();
					return true;
				} else {
					testWraps[rowBeginIndices[rowIndex + 1] + 1] = false;
					testWraps[rowBeginIndices[rowIndex + 1]] = true;
				}
			}
		}
		return false;
	}

	private LayoutSize getMaxLength(boolean [] wraps, int [] rowHeights, LayoutGuideType type, int crossSize, boolean csMax) {
		LayoutSize maxLen = new LayoutSize(true);
		int lineBreak = 0;
		int rowIndex = 0;
		for(int c = 0; c < wraps.length; c++) {
			if(!wraps[c])
				continue;
			LayoutSize len = getSumSize(theChildren, lineBreak, c + 1, theOrientation, type, rowHeights[rowIndex], csMax);
			maxLen.add(len);
			lineBreak = c + 1;
			rowIndex++;
		}
		LayoutSize len = getSumSize(theChildren, lineBreak, theChildren.length, theOrientation, type, rowHeights[rowIndex], csMax);
		maxLen.add(len);
		return maxLen;
	}

	/**
	 * Unwraps at the next spot where it shortens the cross length the most, according to the given size type
	 *
	 * @param type The size type to shorten the most by the unwrap
	 * @param crossSize The cross size to find the next unwrap for
	 * @param csMax Whether crossSize is a maximum or a real widget size
	 * @return True if the wrapping was changed; false if no components are wrapped
	 */
	public boolean unwrapNext(LayoutGuideType type, int crossSize, boolean csMax) {
		/* First, if not everything is already wrapped, try to move a wrap, i.e. move an element down or up a row to decrease the main
		 * length just a little.
		 * If everything is wrapped or the first step fails to find a beneficial wrap movement (i.e. rows are optimal for number of wraps)
		 * then find the new wrap location that shortens the main length the most.
		 */
		LayoutSize maxLen = new LayoutSize();
		if(findGoodWrapMovement(type, crossSize, csMax, maxLen))
			return true;

		// Try to find a good place for a new wrap
		boolean [] testWraps = new boolean[theWraps.length];
		System.arraycopy(theWraps, 0, testWraps, 0, theWraps.length);
		for(int c = 0; c < theWraps.length; c++) {
			if(!theWraps[c]) {
				continue;
			}
			testWraps[c] = false;
			int [] testRowHeights = getRowHeights(crossSize, testWraps, new int[1]);
			if(getMaxLength(testWraps, testRowHeights, type, crossSize, csMax).getTotal() < maxLen.getTotal()) {
				theWraps[c] = false;
				testRowHeights = null;
				return true;
			} else {
				testWraps[c] = true;
			}
		}

		return false;
	}

	/**
	 * @param childIndex The index of the child to set the wrap after
	 * @param wrapped Whether to wrap after the given child
	 */
	public void setWrappedAfter(int childIndex, boolean wrapped) {
		if(theWraps[childIndex] == wrapped)
			return;
		clearCache();
		theWraps[childIndex] = wrapped;
	}

	/**
	 * @param rowIndex The index of the row to get the height of
	 * @return The height of the given row
	 */
	public int getRowHeight(int rowIndex) {
		return theRowHeights[rowIndex];
	}

	/**
	 * Determines the row heights (or column widths for vertical layout) for each wrapped row of widgets
	 *
	 * @param crossSize The size of the cross dimension
	 * @return The heights for each row/column in the layout
	 */
	public int [] getRowHeights(int crossSize) {
		if(theRowHeights != null && theCachedCrossSize == crossSize)
			return theRowHeights;
		theCachedCrossSize = crossSize;
		int [] baseline = new int[1];
		theRowHeights = getRowHeights(crossSize, theWraps, baseline);
		theBaseline = baseline[0];
		return theRowHeights;
	}

	/**
	 * @param rowIndex The index of the row to get the child count of
	 * @return The number of children in the given row with the current wrapping
	 */
	public int getRowChildCount(int rowIndex) {
		int row_i = 0;
		int rowChildCount = 0;
		for(int i = 0; i < theWraps.length; i++) {
			if(row_i == rowIndex) {
				rowChildCount++;
			}
			if(theWraps[i])
				row_i++;
			if(row_i > rowIndex)
				break;
		}
		return rowChildCount;
	}

	private int [] getRowHeights(final int crossSize, final boolean [] wraps, int [] baseline) {
		int rc = 1;
		for(boolean wrap : wraps)
			if(wrap)
				rc++;
		final int rowCount = rc;
		LayoutUtils.LayoutInterpolation<LayoutSize []> result = LayoutUtils.interpolate(new LayoutUtils.LayoutChecker<LayoutSize []>() {
			@Override
			public LayoutSize [] getLayoutValue(LayoutGuideType type) {
				LayoutSize [] ret = new LayoutSize[rowCount];
				int lastBreak = 0;
				int rowIndex = 0;
				LayoutSize rowHeight;
				for(int c = 1; c < theChildren.length; c++) {
					if(wraps[c - 1]) {
						rowHeight = getRowHeight(theChildren, lastBreak, c, LayoutGuideType.pref, crossSize);
						ret[rowIndex++] = rowHeight;
						lastBreak = c;
					}
				}
				rowHeight = getRowHeight(theChildren, lastBreak, theChildren.length, LayoutGuideType.pref, crossSize);
				ret[rowIndex++] = rowHeight;
				return ret;
			}

			@Override
			public long getSize(LayoutSize[] layoutValue) {
				long total = 0;
				for(LayoutSize lv : layoutValue)
					total += lv.getTotal(crossSize);
				return total;
			}
		}, crossSize, LayoutGuideType.min, isFillContainer ? LayoutGuideType.max : LayoutGuideType.maxPref);
		int [] pixRowHeights = new int[result.lowerValue.length];
		for(int i = 0; i < pixRowHeights.length; i++) {
			pixRowHeights[i] = result.lowerValue[i].getTotal(crossSize);
			if(result.proportion > 0)
				pixRowHeights[i] += Math.round(result.proportion * (result.upperValue[i].getTotal(crossSize) - pixRowHeights[i]));
		}

		// Now calculate the vertical baseline for the first row
		baseline[0] = 0;
		for(int c = 0; c < theChildren.length; c++) {
			int childBase = theChildren[c].bounds().get(theOrientation.opposite()).getGuide().getBaseline(pixRowHeights[0]);
			if(childBase > baseline[0])
				baseline[0] = childBase;
			if(c < wraps.length && wraps[c])
				break;
		}
		return pixRowHeights;
	}

	/**
	 * @param length The major size of the container
	 * @return The sizes for each child in the layout
	 */
	public Dimension [] getSizes(int length) {
		Dimension [] ret = new Dimension[theChildren.length];
		for(int i = 0; i < ret.length; i++)
			ret[i] = new Dimension();
		int start = 0;
		int rowIndex = 0;
		for(int i = 1; i < theChildren.length; i++) {
			if(theWraps[i - 1]) {
				fillSizes(ret, rowIndex++, start, i, length);
				start = i;
			}
		}
		fillSizes(ret, rowIndex, start, theChildren.length, length);
		return ret;
	}

	private void fillSizes(Dimension [] sizes, int rowIndex, int start, int end, int length) {
		int margin = (theOrientation == Orientation.horizontal ? theMarginX : theMarginY).evaluate(length) * 2;
		int padding = (theOrientation == Orientation.horizontal ? thePaddingX : thePaddingY).evaluate(length) * (end - start - 1);
		length -= margin + padding;
		int prefSize = 0;
		for(int i = start; i < end; i++) {
			prefSize += LayoutUtils.getSize(theChildren[i], theOrientation, LayoutGuideType.pref, length, theRowHeights[rowIndex], false,
				null);
		}
		if(prefSize > length) {
			int minPrefSize = 0;
			for(int i = start; i < end; i++) {
				prefSize += LayoutUtils.getSize(theChildren[i], theOrientation, LayoutGuideType.minPref, length, theRowHeights[rowIndex],
					false, null);
			}
			if(minPrefSize > length) {
				int minSize = 0;
				for(int i = start; i < end; i++) {
					prefSize += LayoutUtils.getSize(theChildren[i], theOrientation, LayoutGuideType.min, length, theRowHeights[rowIndex],
						false, null);
				}
				if(minSize > length) {
					// Use min size
					setSizes(sizes, start, end, LayoutGuideType.min, rowIndex, length, 0);
				} else {
					// Use a size betwen min and min pref
					setSizes(sizes, start, end, LayoutGuideType.min, rowIndex, length, (length - minSize) * 1.0f / (minPrefSize - minSize));
				}
			} else {
				// Use a size between min pref and preferred
				setSizes(sizes, start, end, LayoutGuideType.minPref, rowIndex, length, (length - minPrefSize) * 1.0f
					/ (prefSize - minPrefSize));
			}
		} else {
			int maxPrefSize = 0;
			for(int i = start; i < end; i++) {
				maxPrefSize += LayoutUtils.getSize(theChildren[i], theOrientation, LayoutGuideType.maxPref, length,
					theRowHeights[rowIndex], false, null);
			}
			if(maxPrefSize >= length) {
				// Use a size between preferred and max pref
				setSizes(sizes, start, end, LayoutGuideType.pref, rowIndex, length, (length - prefSize) * 1.0f / (maxPrefSize - prefSize));
			} else {
				if(isFillContainer) {
					int maxSize = 0;
					for(int i = start; i < end; i++) {
						prefSize += LayoutUtils.getSize(theChildren[i], theOrientation, LayoutGuideType.max, length,
							theRowHeights[rowIndex], false, null);
					}
					if(maxSize > length) {
						// Use max size
						setSizes(sizes, start, end, LayoutGuideType.max, rowIndex, length, 0);
					} else {
						// Use a size between max pref and max
						setSizes(sizes, start, end, LayoutGuideType.maxPref, rowIndex, length, (length - maxPrefSize) * 1.0f
							/ (maxSize - maxPrefSize));
					}
				} else {
					// Use max pref size
					setSizes(sizes, start, end, LayoutGuideType.maxPref, rowIndex, length, 0);
				}
			}
		}
	}

	private void setSizes(Dimension [] sizes, int start, int end, LayoutGuideType type, int rowIndex, int length, float prop) {
		int rowHeight = theRowHeights[rowIndex];
		for(int i = start; i < end; i++) {
			int size = LayoutUtils.getSize(theChildren[i], theOrientation, type, length, rowHeight, false, null);
			if(prop > 0) {
				int upSize = LayoutUtils.getSize(theChildren[i], theOrientation, type.next(), length, rowHeight, false, null);
				size = Math.round(size + prop * upSize);
			}
			LayoutUtils.set(sizes[i], theOrientation, size);
			LayoutUtils.set(sizes[i], theOrientation.opposite(), getCrossSize(theChildren[i], theOrientation, length, rowHeight));
		}
	}

	/**
	 * Gets the cross size for a single element in a container with a flow layout
	 *
	 * @param child The child to get the cross size for
	 * @param mainDim The orientation of the flow layout
	 * @param mainSize The main length of the container
	 * @param rowHeight The content height of the row that the child is in
	 * @return The size for the given element along the cross dimension
	 */
	public static int getCrossSize(QuickWidget child, Orientation mainDim, int mainSize, int rowHeight) {
		int crossSize;
		int prefCross = LayoutUtils.getSize(child, mainDim.opposite(), LayoutGuideType.pref, rowHeight, mainSize, false, null);
		if(prefCross > rowHeight) {
			int minCross = LayoutUtils.getSize(child, mainDim.opposite(), LayoutGuideType.min, rowHeight, mainSize, false, null);
			if(minCross > rowHeight) {
				crossSize = minCross;
			} else {
				crossSize = rowHeight;
			}
		} else {
			int maxPrefCross = LayoutUtils.getSize(child, mainDim.opposite(), LayoutGuideType.maxPref, rowHeight, mainSize, false, null);
			if(maxPrefCross > rowHeight)
				crossSize = rowHeight;
			else
				crossSize = maxPrefCross;
		}
		return crossSize;
	}

	LayoutSize getRowHeight(final QuickWidget[] children, final int start, final int end, LayoutGuideType type, final int size) {
		// Get the baseline to use for the row
		int baseline = 0;
		Orientation orient = theOrientation.opposite();
		for(int c = start; c < end; c++) {
			int childSize = LayoutUtils.getSize(children[c], orient, type, size, Integer.MAX_VALUE, true, null);
			SizeGuide guide = children[c].bounds().get(orient).getGuide();
			int childBase = guide.getBaseline(childSize);
			if(childBase > baseline)
				baseline = childBase;
		}
		LayoutSize max = new LayoutSize(true);
		LayoutSize temp = new LayoutSize();
		for(int c = start; c < end; c++) {
			temp.clear();
			int childSize = LayoutUtils.getSize(children[c], orient, type, 0, Integer.MAX_VALUE, true, temp);
			SizeGuide guide = children[c].bounds().get(orient).getGuide();
			int childBase = guide.getBaseline(childSize);
			if(childBase >= 0) {
				temp.add(baseline - childBase);
			}
			max.add(temp);
		}
		// We have a max row height now. Now we lay out the elements along the main axis and use those widths to get a more accurate
		// (likely smaller) row height
		final int maxPix = max.getTotal(500); 	/* This number only affects maxPix if a percent size is used. If a size attribute is so set,
												 * then the height is somewhat artificial anyway and shouldn't affect the opposite dimension
												 * very much.  So this is just a positive value with a significant but not huge layout size
												 * and shouldn't really matter in the vast majority of cases.  Or so I hope. */
		LayoutUtils.LayoutInterpolation<int []> result = LayoutUtils.interpolate(new LayoutUtils.LayoutChecker<int []>() {
			@Override
			public int [] getLayoutValue(LayoutGuideType checkType) {
				int [] ret = new int[end - start];
				for(int i = start; i < end; i++)
					ret[i - start] = LayoutUtils.getSize(children[i], theOrientation, checkType, size, maxPix, true, null);
				return ret;
			}

			@Override
			public long getSize(int[] layoutValue) {
				long ret = 0;
				for(int lv : layoutValue)
					ret += lv;
				return ret;
			}
		}, size, LayoutGuideType.min, isFillContainer ? LayoutGuideType.max : LayoutGuideType.maxPref);
		if(result.proportion > 0)
			for(int i = 0; i < result.lowerValue.length; i++)
				result.lowerValue[i] += Math.round(result.proportion * (result.upperValue[i] - result.lowerValue[i]));
		max.clear();
		for(int c = start; c < end; c++) {
			temp.clear();
			int childSize = LayoutUtils.getSize(children[c], orient, type, 0, result.lowerValue[c - start], false, temp);
			SizeGuide guide = children[c].bounds().get(orient).getGuide();
			int childBase = guide.getBaseline(childSize);
			if(childBase >= 0) {
				temp.add(baseline - childBase);
			}
			max.add(temp);
		}
		return max;
	}

	LayoutSize getSumSize(QuickWidget[] children, int start, int end, Orientation orient, LayoutGuideType type, int crossSize,
		boolean csMax) {
		LayoutSize ret = new LayoutSize();
		ret.add(orient == Orientation.horizontal ? theMarginX : theMarginY);
		for(int c = start; c < end; c++) {
			LayoutUtils.getSize(children[c], orient, type, Integer.MAX_VALUE, crossSize, csMax, ret);
			if(c != start)
				ret.add(orient == Orientation.horizontal ? thePaddingX : thePaddingY);
		}
		ret.add(orient == Orientation.horizontal ? theMarginX : theMarginY);
		return ret;
	}

	private class FlowLayoutTesterMainSizeGuide implements SizeGuide {
		private SimpleCache<Integer> theCache = new SimpleCache<>();

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
			Integer ret = theCache.get(type, crossSize, csMax, theWraps);
			if(ret != null)
				return ret;
			int [] rowHeights = getRowHeights(crossSize);
			LayoutSize max = new LayoutSize(true);
			int lastBreak = 0;
			int rowIndex = 0;
			for(int c = 1; c < theChildren.length; c++) {
				if(theWraps[c - 1])
					max.add(getSumSize(theChildren, lastBreak, c, theOrientation, type, rowHeights[rowIndex], csMax || type.isPref()));
			}
			max.add(getSumSize(theChildren, lastBreak, theChildren.length, theOrientation, type, rowHeights[rowIndex], type.isPref()));
			max = new LayoutSize(max);
			theCache.set(max.getTotal(), type, crossSize, csMax, theWraps);
			return max.getTotal();
		}

		@Override
		public int getBaseline(int size) {
			return -1;
		}
	}

	class FlowLayoutTesterCrossSizeGuide implements SizeGuide {
		private SimpleCache<Integer> theCache = new SimpleCache<>();

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
			Integer ret = theCache.get(type, crossSize, csMax, theWraps);
			if(ret != null)
				return ret;
			LayoutSize temp = new LayoutSize();
			ArrayList<QuickWidget> row = new ArrayList<>();
			int size = 0;
			boolean sizeChanged = true;
			int iterations = 5;
			while(sizeChanged && iterations > 0) {
				sizeChanged = false;
				iterations--;
				int sum = 0;
				float percentSum = 0;
				for(int c = 0; c < theWraps.length; c++) {
					row.add(theChildren[c]);
					if(theWraps[c]) {
						temp.clear();
						BaseLayoutUtils.getBoxLayoutCrossSize(row, theOrientation, type, crossSize, csMax, temp);
						if(c == 0)
							temp.add(theOrientation == Orientation.horizontal ? theMarginY : theMarginX);
						else
							temp.add(theOrientation == Orientation.horizontal ? thePaddingY : thePaddingX);
						int rowHeight = temp.getPixels();
						percentSum += temp.getPercent();
						int percentHeight = Math.round(temp.getPercent() * size);
						if(percentHeight > rowHeight)
							rowHeight = percentHeight;
						sum += rowHeight;
						row.clear();
					}
				}
				row.add(theChildren[theWraps.length]);
				temp.clear();
				BaseLayoutUtils.getBoxLayoutCrossSize(row, theOrientation, type, crossSize, csMax, temp);
				temp.add(theOrientation == Orientation.horizontal ? theMarginY : theMarginX);
				int rowHeight = temp.getPixels();
				percentSum += temp.getPercent();
				int percentHeight = Math.round(temp.getPercent() * size);
				if(percentHeight > rowHeight)
					rowHeight = percentHeight;
				sum += rowHeight;
				if(sum > size) {
					size = sum;
					if(percentSum > 0)
						sizeChanged = true;
				}
			}
			theCache.set(size, type, crossSize, csMax, theWraps);
			theCache.set(theBaseline, "baseline", size, theWraps);
			return size;
		}

		@Override
		public int getBaseline(int size) {
			Number ret=theCache.get("baseline", size, theWraps);
			return ret == null ? 0 : ret.intValue();
		}
	}
}
