package org.muis.base.layout;

import org.muis.core.MuisElement;
import org.muis.core.layout.*;

public class FlowLayoutTester {
    private MuisElement[] theChildren;

    private Orientation theOrientation;

    private boolean[] theWraps;

    private int[] theRowHeights;

    private int theBaseline;

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
        theRowHeights = null;
        for (int i = 0; i < theWraps.length; i++)
            theWraps[i] = true;
    }

    public void unwrapAll() {
        theRowHeights = null;
        for (int i = 0; i < theWraps.length; i++)
            theWraps[i] = false;
    }

    /**
     * Wraps at the next spot where it shortens the main length the most, according to the given size type
     *
     * @param type
     *            The size type to shorten the most by the wrap
     * @return True if the wrapping was changed; false if all components are already wrapped
     */
    public boolean wrapNext(LayoutGuideType type) {
        theRowHeights = null;
    }

    /**
     * Unwraps at the next spot where it shortens the cross length the most, according to the given size type
     *
     * @param type
     *            The size type to shorten the most by the wrap
     * @return True if the wrapping was changed; false if no components are wrapped
     */
    public boolean unwrapNext(LayoutGuideType type) {
        theRowHeights = null;
    }

    public boolean setWrappedAfter(int childIndex, boolean wrapped) {
        theRowHeights = null;
        theWraps[childIndex] = wrapped;
    }

    /**
     * Determines the row heights (or column widths for vertical layout) for each wrapped row of widgets
     *
     * @param crossSize
     *            The size of the cross dimension
     * @return The heights for each row/column in the layout
     */
    private int[] getRowHeights(int crossSize) {
        if (theRowHeights != null)
            return theRowHeights;
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
        for (boolean wrap : theWraps)
            if (wrap)
                rowCount++;
        theRowHeights = new int[rowCount];
        Orientation crossOrient = theOrientation.opposite();
        // Calculate the sum of the maximum of the preferred sizes for the widgets in each row
        LayoutSize prefRowTotal = new LayoutSize();
        int lastBreak = 0;
        int rowIndex = 0;
        int rowHeight;
        for (int c = 1; c < theChildren.length; c++) {
            if (theWraps[c]) {
                rowHeight = getMaxSize(theChildren, lastBreak, c, crossOrient, LayoutGuideType.pref, crossSize);
                theRowHeights[rowIndex++] = rowHeight;
                prefRowTotal.add(rowHeight);
                lastBreak = c;
            }
        }
        rowHeight = getMaxSize(theChildren, lastBreak, theChildren.length, crossOrient, LayoutGuideType.pref, crossSize);
        theRowHeights[rowIndex++] = rowHeight;
        prefRowTotal.add(rowHeight);

        if (prefRowTotal.getTotal() > crossSize) {
            int[] prefRowHeights = theRowHeights;
            theRowHeights = new int[rowCount];
            // Calculate the sum of the minimum of the preferred sizes for the widgets in each row
            LayoutSize minRowTotal = new LayoutSize();
            lastBreak = 0;
            rowIndex = 0;
            for (int c = 1; c < theChildren.length; c++) {
                if (theWraps[c]) {
                    rowHeight = getMaxSize(theChildren, lastBreak, c, crossOrient, LayoutGuideType.min, crossSize);
                    theRowHeights[rowIndex++] = rowHeight;
                    minRowTotal.add(rowHeight);
                    lastBreak = c;
                }
            }
            rowHeight = getMaxSize(theChildren, lastBreak, theChildren.length, crossOrient, LayoutGuideType.min, crossSize);
            theRowHeights[rowIndex++] = rowHeight;
            minRowTotal.add(rowHeight);

            if (minRowTotal.getTotal() <= crossSize) {
                int[] minRowHeights = theRowHeights;
                int[] minPrefRowHeights = new int[rowCount];
                theRowHeights = new int[rowCount];
                // Calculate the sum of the minimum of the min pref sizes for the widgets in each row
                LayoutSize minPrefRowTotal = new LayoutSize();
                lastBreak = 0;
                rowIndex = 0;
                for (int c = 1; c < theChildren.length; c++) {
                    if (theWraps[c]) {
                        rowHeight = getMaxSize(theChildren, lastBreak, c, crossOrient, LayoutGuideType.minPref, crossSize);
                        minPrefRowHeights[rowIndex++] = rowHeight;
                        minPrefRowTotal.add(rowHeight);
                        lastBreak = c;
                    }
                }
                rowHeight = getMaxSize(theChildren, lastBreak, theChildren.length, crossOrient, LayoutGuideType.minPref, crossSize);
                minPrefRowHeights[rowIndex++] = rowHeight;
                minPrefRowTotal.add(rowHeight);

                float prop;
                if (minPrefRowTotal.getTotal() >= crossSize) {
                    prop = (crossSize - minRowTotal.getTotal()) * 1.0f / (minPrefRowTotal.getTotal() - minRowTotal.getTotal());
                    for (int r = 0; r < rowCount; r++)
                        theRowHeights[r] = minRowHeights[r] + Math.round(prop * (minPrefRowHeights[r] - minRowHeights[r]));
                } else {
                    prop = (crossSize - minPrefRowTotal.getTotal()) * 1.0f / (prefRowTotal.getTotal() - minPrefRowTotal.getTotal());
                    for (int r = 0; r < rowCount; r++)
                        theRowHeights[r] = minPrefRowHeights[r] + Math.round(prop * (prefRowHeights[r] - minPrefRowHeights[r]));
                }
            }
        }
        // Now calculate the vertical baseline for the first row
        int baseline = 0;
        for (int c = 0; c < theChildren.length; c++) {
            int childBase = theChildren[c].bounds().get(crossOrient).getGuide().getBaseline(theRowHeights[0]);
            if (childBase > baseline)
                baseline = childBase;
            if (theWraps[c])
                break;
        }
        theBaseline = baseline;
        return theRowHeights;
    }

    static int getMaxSize(MuisElement[] children, int start, int end, Orientation orient, LayoutGuideType type, int size) {
        // Get the baseline to use for the row
        int baseline = 0;
        for (int c = start; c < end; c++) {
            int childSize = LayoutUtils.getSize(children[c], orient, type, size, Integer.MAX_VALUE, true, null);
            SizeGuide guide = children[c].bounds().get(orient).getGuide();
            int childBase = guide.getBaseline(childSize);
            if (childBase > baseline)
                baseline = childBase;
        }
        int max = 0;
        for (int c = start; c < end; c++) {
            int childSize = LayoutUtils.getSize(children[c], orient, type, 0, Integer.MAX_VALUE, true, null);
            SizeGuide guide = children[c].bounds().get(orient).getGuide();
            int childBase = guide.getBaseline(childSize);
            if (childBase >= 0) {
                childSize += baseline - childBase;
            }
            if (childSize > max)
                max = childSize;
        }
        return max;
    }

    static int getSumSize(MuisElement[] children, int start, int end, Orientation orient, LayoutGuideType type, int crossSize, boolean csMax) {
        LayoutSize ret = new LayoutSize();
        for (int c = start; c < end; c++) {
            LayoutUtils.getSize(children[c], orient, type, Integer.MAX_VALUE, crossSize, csMax, ret);
        }
        return ret.getTotal();
    }

    private class FlowLayoutTesterMainSizeGuide implements SizeGuide {
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
            int[] rowHeights = getRowHeights(crossSize);
            int max = 0;
            int lastBreak = 0;
            int rowIndex = 0;
            for (int c = 1; c < theChildren.length; c++) {
                if (theWraps[c]) {
                    int rowSize = getSumSize(theChildren, lastBreak, c, theOrientation, type, rowHeights[rowIndex], csMax || type.isPref());
                    if (rowSize > max)
                        max = rowSize;
                }
            }
            int rowSize = getSumSize(theChildren, lastBreak, theChildren.length, theOrientation, type, rowHeights[rowIndex], type.isPref());
            if (rowSize > max)
                max = rowSize;
            return max;
        }

        @Override
        public int getBaseline(int size) {
            return -1;
        }
    }

    class FlowLayoutTesterCrossSizeGuide implements SizeGuide {
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
            LayoutSize sum = new LayoutSize();
            LayoutSize max = new LayoutSize();
            LayoutSize temp = new LayoutSize();
            Orientation crossOrient = theOrientation.opposite();
            // TODO For each row, need to get the children's sizes given the crossSize for the container
            for (int c = 0; c < theChildren.length; c++) {
                temp.clear();
                LayoutUtils.getSize(theChildren[c], crossOrient, type, 0, childCrossSize, csMax, temp);
                if (temp.getPixels() > max.getPixels() || temp.getPercent() > max.getPercent()) {
                    max.setPixels(temp.getPixels());
                    max.setPercent(temp.getPercent());
                }
                if (theWraps[c]) {
                    // TODO don't think the straight add is right here because of relative sizes
                    sum.add(max);
                    max.clear();
                }
            }

        }

        @Override
        public int getBaseline(int size) {
            return theBaseline;
        }
    }
}
