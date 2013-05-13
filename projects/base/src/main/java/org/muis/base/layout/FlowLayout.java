package org.muis.base.layout;

import static org.muis.core.layout.LayoutAttributes.*;
import static org.muis.core.layout.LayoutGuideType.*;
import static org.muis.core.layout.Orientation.vertical;
import static org.muis.core.style.LayoutStyles.margin;
import static org.muis.core.style.LayoutStyles.padding;

import java.awt.Dimension;

import org.muis.core.MuisElement;
import org.muis.core.layout.*;
import org.muis.core.style.Size;
import org.muis.util.CompoundListener;

/** Arranges components in order along a single axis, wrapping them to the next row or column as needed. */
public class FlowLayout implements org.muis.core.MuisLayout {
    private final CompoundListener.MultiElementCompoundListener theListener;

    /** Creates a flow layout */
    public FlowLayout() {
        theListener = CompoundListener.create(this);
        theListener.acceptAll(direction, alignment, crossAlignment, fillContainer).watchAll(margin, padding)
                .onChange(CompoundListener.layout);
        theListener.child().acceptAll(width, minWidth, maxWidth, height, minHeight, maxHeight).onChange(CompoundListener.layout);
    }

    @Override
    public void initChildren(MuisElement parent, MuisElement[] children) {
        theListener.listenerFor(parent);
    }

    @Override
    public void remove(MuisElement parent) {
        theListener.dropFor(parent);
    }

    @Override
    public void childAdded(MuisElement parent, MuisElement child) {
    }

    @Override
    public void childRemoved(MuisElement parent, MuisElement child) {
    }

    @Override
    public SizeGuide getWSizer(MuisElement parent, MuisElement[] children) {
        Direction dir = parent.atts().get(direction, Direction.RIGHT);
        Size marginSz = parent.getStyle().getSelf().get(margin);
        Size paddingSz = parent.getStyle().getSelf().get(padding);
        boolean fill = parent.atts().get(fillContainer, false);
        boolean major = dir.getOrientation() == Orientation.horizontal;

        return getSizer(dir, marginSz, paddingSz, fill, major, children);
    }

    @Override
    public SizeGuide getHSizer(MuisElement parent, MuisElement[] children) {
        Direction dir = parent.atts().get(direction, Direction.RIGHT);
        Size marginSz = parent.getStyle().getSelf().get(margin);
        Size paddingSz = parent.getStyle().getSelf().get(padding);
        boolean fill = parent.atts().get(fillContainer, false);
        boolean major = dir.getOrientation() == vertical;

        return getSizer(dir, marginSz, paddingSz, fill, major, children);
    }

    private SizeGuide getSizer(final Direction dir, final Size marginSz, final Size paddingSz, final boolean fill, final boolean major,
            final MuisElement[] children) {
        return new AbstractSizeGuide() {
            private FlowLayoutTester tester = new FlowLayoutTester(dir.getOrientation(), paddingSz, paddingSz, marginSz, marginSz, fill,
                    children);

            @Override
            public int getMin(int crossSize, boolean csMax) {
                if (major) {
                    tester.wrapAll();
                    if (crossSize < Integer.MAX_VALUE)
                        while (tester.cross().getMin(Integer.MAX_VALUE, true) > crossSize && tester.unwrapNext(min, crossSize, csMax))
                            ;
                    return tester.main().getMin(crossSize, csMax);
                } else {
                    tester.unwrapAll();
                    if (crossSize < Integer.MAX_VALUE)
                        while (tester.main().getMin(Integer.MAX_VALUE, true) > crossSize && tester.wrapNext(min, Integer.MAX_VALUE, true))
                            ;
                    return tester.cross().getMin(crossSize, csMax);
                }
            }

            @Override
            public int getMinPreferred(int crossSize, boolean csMax) {
                if (major) {
                    tester.wrapAll();
                    if (crossSize < Integer.MAX_VALUE)
                        while (tester.cross().getMinPreferred(Integer.MAX_VALUE, true) > crossSize
                                && tester.unwrapNext(minPref, crossSize, csMax))
                            ;
                    return tester.main().getMinPreferred(crossSize, csMax);
                } else {
                    tester.unwrapAll();
                    if (crossSize < Integer.MAX_VALUE)
                        while (tester.main().getMinPreferred(Integer.MAX_VALUE, true) > crossSize
                                && tester.wrapNext(minPref, Integer.MAX_VALUE, true))
                            ;
                    return tester.cross().getMinPreferred(crossSize, csMax);
                }
            }

            @Override
            public int getPreferred(int crossSize, boolean csMax) {
                tester.unwrapAll();
                if (major)
                    return tester.main().getPreferred(crossSize, csMax);
                else {
                    if (csMax) {
                        while (tester.main().getMaxPreferred(Integer.MAX_VALUE, true) < crossSize
                                && tester.wrapNext(maxPref, crossSize, csMax))
                            ;
                        boolean wrappedOnPref = false;
                        while (tester.main().getPreferred(Integer.MAX_VALUE, true) < crossSize && tester.wrapNext(pref, crossSize, csMax)) {
                            wrappedOnPref = true;
                        }
                        if (wrappedOnPref && tester.main().getPreferred(Integer.MAX_VALUE, true) > crossSize)
                            tester.unwrapNext(pref, crossSize, csMax);
                    } else {
                        // TODO Here's where we would square things up for square style
                    }
                    return tester.cross().getPreferred(crossSize, csMax);
                }
            }

            @Override
            public int getMaxPreferred(int crossSize, boolean csMax) {
                if (major)
                    return BaseLayoutUtils.getBoxLayoutSize(children, dir.getOrientation(), maxPref, crossSize, csMax);
                else
                    return BaseLayoutUtils.getBoxLayoutSize(children, dir.getOrientation().opposite(), maxPref, crossSize, csMax);
            }

            @Override
            public int getMax(int crossSize, boolean csMax) {
                if (fill)
                    return Integer.MAX_VALUE;
                else if (major)
                    return BaseLayoutUtils.getBoxLayoutSize(children, dir.getOrientation(), max, crossSize, csMax);
                else
                    return BaseLayoutUtils.getBoxLayoutSize(children, dir.getOrientation().opposite(), max, crossSize, csMax);
            }

            @Override
            public int getBaseline(int size) {
                return major ? tester.main().getBaseline(size) : tester.cross().getBaseline(size);
            }
        };
    }

    @Override
    public void layout(MuisElement parent, MuisElement[] children) {
        Direction dir = parent.atts().get(direction, Direction.RIGHT);
        Size marginSz = parent.getStyle().getSelf().get(margin);
        Size paddingSz = parent.getStyle().getSelf().get(padding);
        boolean fill = parent.atts().get(fillContainer, false);
        Alignment align = parent.atts().get(alignment, dir.getStartEnd() == End.leading ? Alignment.begin : Alignment.end);
        Alignment crossAlign = parent.atts().get(crossAlignment, Alignment.center);
        FlowLayoutTester tester = new FlowLayoutTester(dir.getOrientation(), paddingSz, paddingSz, marginSz, marginSz, fill, children);
        int mainLen = parent.bounds().get(dir.getOrientation()).getSize();
        int crossLen = parent.bounds().get(dir.getOrientation().opposite()).getSize();
        /* tester starts off unwrapped.
         * while the preferred major size is greater than the major length and the preferred minor size <= the minor length, wrap.
         * if the container is too small, unwrap all and try the procedure above with preferred min sizes
         * if still too small, unwrap all and do with min sizes
         * if preferred still has lots of room, try with preferred max sizes
         */
        LayoutGuideType negotiated;
        boolean useProp = true;
        if (tester.main().getPreferred(crossLen, false) > mainLen) {
            while (tester.main().getPreferred(crossLen, false) > mainLen)
                if (!tester.wrapNext(LayoutGuideType.pref, crossLen, false))
                    break;
            if (tester.main().getPreferred(crossLen, false) > mainLen && tester.main().getMinPreferred(crossLen, false) > mainLen) {
                while (tester.main().getMinPreferred(crossLen, false) > mainLen)
                    if (!tester.wrapNext(LayoutGuideType.minPref, crossLen, false))
                        break;
                if (tester.main().getMinPreferred(crossLen, false) > mainLen && tester.main().getMin(crossLen, false) > mainLen) {
                    while (tester.main().getMin(crossLen, false) > mainLen)
                        if (!tester.wrapNext(LayoutGuideType.min, crossLen, false))
                            break;
                    negotiated = min;
                } else {
                    negotiated = minPref;
                }
            } else {
                negotiated = pref;
            }
        } else if (tester.main().getMaxPreferred(crossLen, false) > mainLen) {
            negotiated = pref;
        } else if (align != Alignment.justify && !fill) {
            negotiated = maxPref;
            useProp = false;
        } else if (tester.main().getMax(crossLen, false) > mainLen) {
            negotiated = maxPref;
        } else {
            negotiated = max;
            useProp = false;
        }
        // Now the wrapping is right and we know the two size types to go between
        Dimension[] sizes;
        if (!useProp) {
            tester.main().get(negotiated, crossLen, false);
            sizes = tester.getSizes(mainLen);
        } else {
            float prop;
            int less = tester.main().get(negotiated, crossLen, false);
            Dimension[] lessSizes = tester.getSizes(mainLen);
            int more = tester.main().get(negotiated.next(), crossLen, false);
            Dimension[] moreSizes = tester.getSizes(mainLen);
            prop = (mainLen - less) * 1.0f / (more - less);
            int rowIndex = 0;
            for (int i = 0; i < children.length; i++) {
                int lessSize = LayoutUtils.get(lessSizes[i], dir.getOrientation());
                int moreSize = LayoutUtils.get(moreSizes[i], dir.getOrientation());
                int mainSize = Math.round(lessSize + prop * (moreSize - lessSize));
                LayoutUtils.set(lessSizes[i], dir.getOrientation(), mainSize);
                LayoutUtils.set(lessSizes[i], dir.getOrientation(), FlowLayoutTester.getCrossSize(children[i], dir.getOrientation(),
                        mainSize, tester.getRowContentHeight(rowIndex)));
                if (tester.isWrapped(i))
                    rowIndex++;
            }
            sizes = lessSizes;
        }

        // Set the sizes and correct positions on the widgets
    }
}
