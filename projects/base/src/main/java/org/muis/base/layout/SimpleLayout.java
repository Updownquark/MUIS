package org.muis.base.layout;

import static org.muis.core.layout.LayoutAttributes.*;

import org.muis.core.MuisElement;
import org.muis.core.MuisLayout;
import org.muis.core.layout.*;
import org.muis.core.layout.LayoutAttributes.PositionAttribute;
import org.muis.core.layout.LayoutAttributes.SizeAttribute;
import org.muis.core.style.Position;
import org.muis.core.style.Size;
import org.muis.util.CompoundListener;

/**
 * A very simple layout that positions and sizes children independently using positional ({@link LayoutAttributes#left},
 * {@link LayoutAttributes#right}, {@link LayoutAttributes#top}, {@link LayoutAttributes#bottom}), size ({@link LayoutAttributes#width} and
 * {@link LayoutAttributes#height}) and minimum size ({@link LayoutAttributes#minWidth} and {@link LayoutAttributes#minHeight}) attributes
 * or sizers.
 */
public class SimpleLayout implements MuisLayout {
    private final CompoundListener.MultiElementCompoundListener theListener;

    /** Creates a simple layout */
    public SimpleLayout() {
        theListener = CompoundListener.create(this);
        theListener.accept(LayoutAttributes.maxInf).onChange(CompoundListener.layout);
        theListener.child().acceptAll(left, right, top, bottom, width, minWidth, maxWidth, height, minHeight, maxHeight)
                .onChange(CompoundListener.layout);
    }

    @Override
    public void initChildren(MuisElement parent, MuisElement[] children) {
        theListener.listenerFor(parent);
    }

    @Override
    public void childAdded(MuisElement parent, MuisElement child) {
    }

    @Override
    public void childRemoved(MuisElement parent, MuisElement child) {
    }

    @Override
    public SizeGuide getWSizer(MuisElement parent, MuisElement[] children) {
        return getSizer(children, Orientation.horizontal, parent.atts().get(LayoutAttributes.maxInf));
    }

    @Override
    public SizeGuide getHSizer(MuisElement parent, MuisElement[] children) {
        return getSizer(children, Orientation.vertical, parent.atts().get(LayoutAttributes.maxInf));
    }

    /**
     * Gets a sizer for a container in one dimension
     *
     * @param children
     *            The children to lay out
     * @param orient
     *            The orientation to get the sizer for
     * @param maxInfValue
     *            The value for the {@link LayoutAttributes#maxInf} attribute in the parent
     * @return The size policy for the container of the given children in the given dimension
     */
    protected SizeGuide getSizer(final MuisElement[] children, final Orientation orient, final Boolean maxInfValue) {
        final PositionAttribute minPosAtt = LayoutAttributes.getPosAtt(orient, End.leading, null);
        final PositionAttribute maxPosAtt = LayoutAttributes.getPosAtt(orient, End.leading, null);
        final SizeAttribute minSizeAtt = LayoutAttributes.getSizeAtt(orient, LayoutGuideType.min);
        final SizeAttribute sizeAtt = LayoutAttributes.getSizeAtt(orient, null);
        final SizeAttribute maxSizeAtt = LayoutAttributes.getSizeAtt(orient, LayoutGuideType.max);
        final boolean vertical = orient == Orientation.vertical;

        return new AbstractSizeGuide() {
            private int theCachedCrossSize = -1;

            SimpleSizeGuide theCachedSize = new SimpleSizeGuide();

            SimpleSizeGuide theCachedCsMaxSize = new SimpleSizeGuide();

            @Override
            public int getMinPreferred(int crossSize, boolean csMax) {
                calculate(crossSize);
                if (!csMax)
                    return theCachedSize.getMinPreferred(crossSize, csMax);
                else
                    return theCachedCsMaxSize.getMinPreferred(crossSize, csMax);
            }

            @Override
            public int getMaxPreferred(int crossSize, boolean csMax) {
                calculate(crossSize);
                if (!csMax)
                    return theCachedSize.getMaxPreferred(crossSize, csMax);
                else
                    return theCachedCsMaxSize.getMaxPreferred(crossSize, csMax);
            }

            @Override
            public int getMin(int crossSize, boolean csMax) {
                calculate(crossSize);
                if (!csMax)
                    return theCachedSize.getMin(crossSize, csMax);
                else
                    return theCachedCsMaxSize.getMin(crossSize, csMax);
            }

            @Override
            public int getPreferred(int crossSize, boolean csMax) {
                calculate(crossSize);
                if (!csMax)
                    return theCachedSize.getPreferred(crossSize, csMax);
                else
                    return theCachedCsMaxSize.getPreferred(crossSize, csMax);
            }

            @Override
            public int getMax(int crossSize, boolean csMax) {
                calculate(crossSize);
                if (!csMax)
                    return theCachedSize.getMax(crossSize, csMax);
                else
                    return theCachedCsMaxSize.getMax(crossSize, csMax);
            }

            @Override
            public int getBaseline(int size) {
                if (children.length == 0)
                    return 0;
                for (MuisElement child : children) {
                    int childSize = LayoutUtils.getSize(child, orient, LayoutGuideType.pref, size, Integer.MAX_VALUE, true, null);
                    int ret = child.bounds().get(orient).getGuide().getBaseline(childSize);
                    if (ret < 0)
                        continue;
                    Position pos = children[0].atts().get(LayoutAttributes.getPosAtt(orient, End.leading, null));
                    return ret + (pos == null ? 0 : pos.evaluate(size));
                }
                return -1;
            }

            private void calculate(int crossSize) {
                if (crossSize == theCachedCrossSize)
                    return;
                theCachedCrossSize = crossSize;
                calculate(theCachedSize, crossSize, false);
                calculate(theCachedCsMaxSize, crossSize, true);
            }

            private void calculate(SimpleSizeGuide size, int crossSize, boolean csMax) {
                size.set(0, 0, 0, 0, Integer.MAX_VALUE);
                // TODO calculate minPreferred and maxPreferred
                boolean isMaxInf = Boolean.TRUE.equals(maxInfValue);
                for (MuisElement child : children) {
                    Position minPosL = child.atts().get(minPosAtt);
                    Position maxPosL = child.atts().get(maxPosAtt);
                    Size sizeL = child.atts().get(sizeAtt);
                    Size minSizeL = child.atts().get(minSizeAtt);
                    Size maxSizeL = child.atts().get(maxSizeAtt);
                    if (maxPosL != null && !maxPosL.getUnit().isRelative()) {
                        int r = maxPosL.evaluate(0);
                        if (size.getMin(0, csMax) < r)
                            size.setMin(r);
                        if (size.getPreferred(0, csMax) < r)
                            size.setPreferred(r);
                    } else if (sizeL != null && !sizeL.getUnit().isRelative()) {
                        int w = sizeL.evaluate(0);
                        int x;
                        if (minPosL != null && !minPosL.getUnit().isRelative())
                            x = minPosL.evaluate(0);
                        else
                            x = 0;

                        if (size.getMin(0, csMax) < x + w)
                            size.setMin(x + w);
                        if (size.getPreferred(0, csMax) < x + w)
                            size.setPreferred(x + w);
                        if (!isMaxInf) {
                            int max;
                            if (maxSizeL != null && !maxSizeL.getUnit().isRelative())
                                max = maxSizeL.evaluate(0);
                            else {
                                SizeGuide childSizer = vertical ? child.getHSizer() : child.getWSizer();
                                max = childSizer.getMax(crossSize, csMax);
                            }
                            if (max + x > max)
                                max += x;
                            if (size.getMax(0, csMax) > max)
                                size.setMax(max);
                        }
                    } else if (minSizeL != null && !minSizeL.getUnit().isRelative()) {
                        SizeGuide childSizer = vertical ? child.getHSizer() : child.getWSizer();
                        int minW = minSizeL.evaluate(0);
                        int prefW = childSizer.getPreferred(crossSize, csMax);
                        if (prefW < minW)
                            prefW = minW;
                        int x;
                        if (minPosL != null && !minPosL.getUnit().isRelative())
                            x = minPosL.evaluate(0);
                        else
                            x = 0;

                        if (size.getMin(0, csMax) < x + minW)
                            size.setMin(x + minW);
                        if (size.getPreferred(0, csMax) < x + prefW)
                            size.setPreferred(x + prefW);
                        if (!isMaxInf) {
                            int max;
                            if (maxSizeL != null && !maxSizeL.getUnit().isRelative())
                                max = maxSizeL.evaluate(0);
                            else
                                max = childSizer.getMax(crossSize, csMax);
                            if (max + x > max)
                                max += x;
                            if (size.getMax(0, csMax) > max)
                                size.setMax(max);
                        }
                    } else if (minPosL != null && !minPosL.getUnit().isRelative()) {
                        SizeGuide childSizer = vertical ? child.getHSizer() : child.getWSizer();
                        int x = minPosL.evaluate(0);
                        if (size.getMin(0, csMax) < x + childSizer.getMin(crossSize, csMax))
                            size.setMin(x + childSizer.getMin(crossSize, csMax));
                        if (size.getPreferred(0, csMax) < x + childSizer.getPreferred(crossSize, csMax))
                            size.setPreferred(x + childSizer.getPreferred(crossSize, csMax));
                        if (!isMaxInf) {
                            int max;
                            if (maxSizeL != null && !maxSizeL.getUnit().isRelative())
                                max = maxSizeL.evaluate(0);
                            else
                                max = childSizer.getMax(crossSize, csMax);
                            if (max + x > max)
                                max += x;
                            if (size.getMax(0, csMax) > max)
                                size.setMax(max);
                        }
                    } else {
                        SizeGuide childSizer = vertical ? child.getHSizer() : child.getWSizer();
                        if (size.getMin(0, csMax) < childSizer.getMin(crossSize, csMax))
                            size.setMin(childSizer.getMin(crossSize, csMax));
                        if (size.getPreferred(0, csMax) < childSizer.getPreferred(crossSize, csMax))
                            size.setPreferred(childSizer.getPreferred(crossSize, csMax));
                        if (!isMaxInf && size.getMax(0, csMax) > childSizer.getMax(crossSize, csMax))
                            size.setMax(childSizer.getMax(crossSize, csMax));
                    }
                }
            }
        };
    }

    @Override
    public void layout(MuisElement parent, MuisElement[] children) {
        java.awt.Rectangle bounds = new java.awt.Rectangle();
        int[] dim = new int[2];
        for (MuisElement child : children) {
            Position leftPos = child.atts().get(LayoutAttributes.left);
            Position rightPos = child.atts().get(LayoutAttributes.right);
            Position topPos = child.atts().get(LayoutAttributes.top);
            Position bottomPos = child.atts().get(LayoutAttributes.bottom);
            Size w = child.atts().get(LayoutAttributes.width);
            Size h = child.atts().get(LayoutAttributes.height);
            Size minW = child.atts().get(LayoutAttributes.minWidth);
            Size minH = child.atts().get(LayoutAttributes.minHeight);

            layout(child, false, parent.bounds().getHeight(), leftPos, rightPos, w, minW, parent.bounds().getWidth(), dim);
            bounds.x = dim[0];
            bounds.width = dim[1];
            layout(child, true, parent.bounds().getWidth(), topPos, bottomPos, h, minH, parent.bounds().getHeight(), dim);
            bounds.y = dim[0];
            bounds.height = dim[1];
            child.bounds().setBounds(bounds.x, bounds.y, bounds.width, bounds.height);
        }
    }

    /**
     * Lays out a single child on one dimension within its parent based on its attributes and its size policy
     *
     * @param child
     *            The child to position
     * @param vertical
     *            Whether the layout dimension is vertical (to get the child's sizer if needed)
     * @param breadth
     *            The size of the non-layout dimension of the parent
     * @param minPosAtt
     *            The value of the attribute controlling the child's minimum position (left or top)
     * @param maxPosAtt
     *            The value of the attribute controlling the child's maximum position (right or bottom)
     * @param sizeAtt
     *            The value of the attribute controlling the child's size (width or height)
     * @param minSizeAtt
     *            The value of the attribute controlling the child's minimum size(minWidth or minHeight)
     * @param length
     *            The length of the parent container along the dimension
     * @param dim
     *            The array to put the result (position (x or y) and size (width or height)) into
     */
    protected void layout(MuisElement child, boolean vertical, int breadth, Position minPosAtt, Position maxPosAtt, Size sizeAtt,
            Size minSizeAtt, int length, int[] dim) {
        if (maxPosAtt != null) {
            int max = maxPosAtt.evaluate(length);
            if (minPosAtt != null) {
                dim[0] = minPosAtt.evaluate(length);
                dim[1] = max - dim[0];
            } else if (sizeAtt != null) {
                dim[1] = sizeAtt.evaluate(length);
                dim[0] = max - dim[1];
            } else {
                SizeGuide sizer = vertical ? child.getHSizer() : child.getWSizer();
                dim[1] = sizer.getPreferred(breadth, false);
                if (minSizeAtt != null && dim[1] < minSizeAtt.evaluate(length))
                    dim[1] = minSizeAtt.evaluate(length);
                dim[0] = max - dim[1];
            }
        } else if (sizeAtt != null) {
            dim[1] = sizeAtt.evaluate(length);
            if (minSizeAtt != null && dim[1] < minSizeAtt.evaluate(length))
                dim[1] = minSizeAtt.evaluate(length);
            if (minPosAtt != null)
                dim[0] = minPosAtt.evaluate(length);
            else
                dim[0] = 0;
        } else if (minPosAtt != null) {
            SizeGuide sizer = vertical ? child.getHSizer() : child.getWSizer();
            dim[0] = minPosAtt.evaluate(length);
            dim[1] = sizer.getPreferred(breadth, false);
            if (minSizeAtt != null && dim[1] < minSizeAtt.evaluate(length))
                dim[1] = minSizeAtt.evaluate(length);
        } else {
            SizeGuide sizer = vertical ? child.getHSizer() : child.getWSizer();
            dim[0] = 0;
            dim[1] = sizer.getPreferred(breadth, false);
            if (minSizeAtt != null && dim[1] < minSizeAtt.evaluate(length))
                dim[1] = minSizeAtt.evaluate(length);
        }
    }

    @Override
    public void remove(MuisElement parent) {
        theListener.dropFor(parent);
    }
}
