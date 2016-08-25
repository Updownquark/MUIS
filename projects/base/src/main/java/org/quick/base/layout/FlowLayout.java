package org.quick.base.layout;

import static org.quick.core.layout.LayoutAttributes.*;
import static org.quick.core.layout.LayoutGuideType.*;
import static org.quick.core.layout.Orientation.vertical;
import static org.quick.core.style.LayoutStyle.margin;
import static org.quick.core.style.LayoutStyle.padding;

import java.awt.Dimension;
import java.awt.Rectangle;

import org.observe.Observable;
import org.quick.core.QuickElement;
import org.quick.core.layout.*;
import org.quick.core.style.Size;
import org.quick.util.CompoundListener;
import org.quick.util.SimpleCache;

/** Arranges components in order along a single axis, wrapping them to the next row or column as needed. */
public class FlowLayout implements org.quick.core.QuickLayout {
	private final CompoundListener.MultiElementCompoundListener theListener;

	private SimpleCache<SizeGuide> theSizerCache;

	/** Creates a flow layout */
	public FlowLayout() {
		theListener = CompoundListener.create(this);
		theListener.acceptAll(direction, fillContainer).onChange(CompoundListener.sizeNeedsChanged)
			.acceptAll(alignment, crossAlignment, fillContainer).watchAll(margin, padding).onChange(CompoundListener.sizeNeedsChanged);
		theListener.child().acceptAll(width, minWidth, maxWidth, height, minHeight, maxHeight).onChange(CompoundListener.sizeNeedsChanged);
		theSizerCache = new SimpleCache<>();
	}

	@Override
	public void install(QuickElement parent, Observable<?> until) {
		theListener.listenerFor(parent);
		until.take(1).act(v -> theListener.dropFor(parent));
	}

	@Override
	public SizeGuide getWSizer(QuickElement parent, QuickElement [] children) {
		Direction dir = parent.atts().get(direction, Direction.RIGHT);
		Size marginSz = parent.getStyle().get(margin).get();
		Size paddingSz = parent.getStyle().get(padding).get();
		boolean fill = parent.atts().get(fillContainer, false);
		boolean major = dir.getOrientation() == Orientation.horizontal;

		return getSizer(dir, marginSz, paddingSz, fill, major, parent, children);
	}

	@Override
	public SizeGuide getHSizer(QuickElement parent, QuickElement [] children) {
		Direction dir = parent.atts().get(direction, Direction.RIGHT);
		Size marginSz = parent.getStyle().get(margin).get();
		Size paddingSz = parent.getStyle().get(padding).get();
		boolean fill = parent.atts().get(fillContainer, false);
		boolean major = dir.getOrientation() == vertical;

		return getSizer(dir, marginSz, paddingSz, fill, major, parent, children);
	}

	private SizeGuide getSizer(final Direction dir, final Size marginSz, final Size paddingSz, final boolean fill, final boolean major,
		final QuickElement parent, final QuickElement [] children) {
		SizeGuide ret = theSizerCache.get(dir, marginSz, paddingSz, fill, major, children);
		if(ret != null)
			return ret;
		ret = new AbstractSizeGuide() {
			private final FlowLayoutTester tester = new FlowLayoutTester(dir.getOrientation(), paddingSz, paddingSz, marginSz, marginSz,
				fill, children);

			@Override
			public int getMin(int crossSize, boolean csMax) {
				if(children.length == 0)
					return 0;
				if(major) {
					tester.wrapAll();
					if(crossSize < Integer.MAX_VALUE)
						while(tester.cross().getMin(Integer.MAX_VALUE, true) > crossSize && tester.unwrapNext(min, crossSize, csMax));
					return tester.main().getMin(crossSize, csMax);
				} else {
					tester.unwrapAll();
					if(crossSize < Integer.MAX_VALUE)
						while(tester.main().getMin(Integer.MAX_VALUE, true) > crossSize && tester.wrapNext(min, Integer.MAX_VALUE, true));
					return tester.cross().getMin(crossSize, csMax);
				}
			}

			@Override
			public int getMinPreferred(int crossSize, boolean csMax) {
				if(children.length == 0)
					return 0;
				if(major) {
					tester.wrapAll();
					if(crossSize < Integer.MAX_VALUE)
						while(tester.cross().getMinPreferred(Integer.MAX_VALUE, true) > crossSize
							&& tester.unwrapNext(minPref, crossSize, csMax));
					return tester.main().getMinPreferred(crossSize, csMax);
				} else {
					tester.unwrapAll();
					if(crossSize < Integer.MAX_VALUE)
						while(tester.main().getMinPreferred(Integer.MAX_VALUE, true) > crossSize
							&& tester.wrapNext(minPref, Integer.MAX_VALUE, true));
					return tester.cross().getMinPreferred(crossSize, csMax);
				}
			}

			@Override
			public int getPreferred(int crossSize, boolean csMax) {
				if(children.length == 0)
					return 0;
				tester.unwrapAll();
				if(major)
					return tester.main().getPreferred(crossSize, csMax);
				else {
					if(csMax) {
						while(tester.main().getMaxPreferred(Integer.MAX_VALUE, true) > crossSize
							&& tester.wrapNext(maxPref, crossSize, csMax));
						boolean wrappedOnPref = false;
						while(tester.main().getPreferred(Integer.MAX_VALUE, true) > crossSize && tester.wrapNext(pref, crossSize, csMax)) {
							wrappedOnPref = true;
						}
						if(wrappedOnPref && tester.main().getPreferred(Integer.MAX_VALUE, true) < crossSize)
							tester.unwrapNext(pref, crossSize, csMax);
					} else {
						// TODO Here's where we would square things up for square style
					}
					return tester.cross().getPreferred(crossSize, csMax);
				}
			}

			@Override
			public int getMaxPreferred(int crossSize, boolean csMax) {
				if(!Alignment.justify.equals(parent.atts().get(major ? alignment : crossAlignment)))
					return Integer.MAX_VALUE;
				if(children.length == 0)
					return 0;
				if(major)
					return BaseLayoutUtils.getBoxLayoutSize(children, dir.getOrientation(), maxPref, crossSize, csMax, marginSz, marginSz,
						paddingSz, paddingSz);
				else if(csMax)
					return BaseLayoutUtils.getBoxLayoutSize(children, dir.getOrientation().opposite(), maxPref, crossSize, csMax, marginSz,
						marginSz, paddingSz, paddingSz);
				else {
					tester.unwrapAll();
					while(tester.main().getMaxPreferred(Integer.MAX_VALUE, true) > crossSize && tester.wrapNext(maxPref, crossSize, csMax));
					return tester.cross().getMaxPreferred(crossSize, csMax);
				}
			}

			@Override
			public int getMax(int crossSize, boolean csMax) {
				if(fill || !Alignment.justify.equals(parent.atts().get(major ? alignment : crossAlignment)))
					return Integer.MAX_VALUE;
				else if(children.length == 0)
					return 0;
				else if(major)
					return BaseLayoutUtils.getBoxLayoutSize(children, dir.getOrientation(), max, crossSize, csMax, marginSz, marginSz,
						paddingSz, paddingSz);
				else if(csMax)
					return BaseLayoutUtils.getBoxLayoutSize(children, dir.getOrientation().opposite(), max, crossSize, csMax, marginSz,
						marginSz, paddingSz, paddingSz);
				else {
					tester.unwrapAll();
					while(tester.main().getMax(Integer.MAX_VALUE, true) > crossSize && tester.wrapNext(max, crossSize, csMax));
					return tester.cross().getMax(crossSize, csMax);
				}
			}

			@Override
			public int getBaseline(int size) {
				if(children.length == 0)
					return 0;
				return major ? tester.main().getBaseline(size) : tester.cross().getBaseline(size);
			}
		};
		theSizerCache.set(ret, dir, marginSz, paddingSz, fill, major, children);
		return ret;
	}

	@Override
	public void layout(QuickElement parent, QuickElement [] children) {
		Direction dir = parent.atts().get(direction, Direction.RIGHT);
		Size marginSz = parent.getStyle().get(margin).get();
		Size paddingSz = parent.getStyle().get(padding).get();
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
		if(tester.main().getPreferred(crossLen, false) > mainLen) {
			while(tester.main().getPreferred(crossLen, false) > mainLen)
				if(!tester.wrapNext(LayoutGuideType.pref, crossLen, false))
					break;
			if(tester.main().getPreferred(crossLen, false) > mainLen && tester.main().getMinPreferred(crossLen, false) > mainLen) {
				while(tester.main().getMinPreferred(crossLen, false) > mainLen)
					if(!tester.wrapNext(LayoutGuideType.minPref, crossLen, false))
						break;
				if(tester.main().getMinPreferred(crossLen, false) > mainLen && tester.main().getMin(crossLen, false) > mainLen) {
					while(tester.main().getMin(crossLen, false) > mainLen)
						if(!tester.wrapNext(LayoutGuideType.min, crossLen, false))
							break;
					negotiated = min;
				} else {
					negotiated = minPref;
				}
			} else {
				negotiated = pref;
			}
		} else if(tester.main().getMaxPreferred(crossLen, false) > mainLen) {
			negotiated = pref;
		} else if(align != Alignment.justify && !fill) {
			negotiated = maxPref;
			useProp = false;
		} else if(tester.main().getMax(crossLen, false) > mainLen) {
			negotiated = maxPref;
		} else {
			negotiated = max;
			useProp = false;
		}
		// Now the wrapping is right and we know the two size types to go between
		Dimension [] sizes;
		if(!useProp) {
			tester.main().get(negotiated, crossLen, false);
			sizes = tester.getSizes(mainLen);
		} else {
			float prop;
			int less = tester.main().get(negotiated, crossLen, false);
			Dimension [] lessSizes = tester.getSizes(mainLen);
			int more = tester.main().get(negotiated.next(), crossLen, false);
			Dimension [] moreSizes = tester.getSizes(mainLen);
			if(less < more)
				prop = (mainLen - less) * 1.0f / (more - less);
			else
				prop = 0;
			int rowIndex = 0;
			for(int i = 0; i < children.length; i++) {
				int lessSize = LayoutUtils.get(lessSizes[i], dir.getOrientation());
				int moreSize = LayoutUtils.get(moreSizes[i], dir.getOrientation());
				int mainSize = Math.round(lessSize + prop * (moreSize - lessSize));
				LayoutUtils.set(lessSizes[i], dir.getOrientation(), mainSize);
				int rowHeight = tester.getRowHeight(rowIndex);
				if(crossAlign == Alignment.justify) {
					int minCross = LayoutUtils.getSize(children[i], dir.getOrientation().opposite(), LayoutGuideType.min, mainLen,
						rowHeight, false, null);
					if(minCross > rowHeight)
						LayoutUtils.set(lessSizes[i], dir.getOrientation().opposite(), minCross);
					else {
						int maxCross = LayoutUtils.getSize(children[i], dir.getOrientation().opposite(), LayoutGuideType.max, mainLen,
							rowHeight, false, null);
						if(maxCross < rowHeight)
							LayoutUtils.set(lessSizes[i], dir.getOrientation().opposite(), maxCross);
						else
							LayoutUtils.set(lessSizes[i], dir.getOrientation().opposite(), rowHeight);
					}
				} else {
					LayoutUtils.set(lessSizes[i], dir.getOrientation().opposite(),
						FlowLayoutTester.getCrossSize(children[i], dir.getOrientation(), mainLen, rowHeight));
				}
				if(i < children.length - 1 && tester.isWrapped(i))
					rowIndex++;
			}
			sizes = lessSizes;
		}

		// Set the sizes and correct positions on the widgets
		Rectangle [] bounds = new Rectangle[sizes.length];
		for(int i = 0; i < bounds.length; i++) {
			bounds[i] = new Rectangle(sizes[i]);
		}
		Dimension parentBounds = parent.bounds().getSize();
		int crossPos = marginSz.evaluate(crossLen);
		int start = 0;
		int rowIndex = 0;
		int [] rowHeights = tester.getRowHeights(crossLen);
		int totalRowHeight = 0;
		for(int rh : rowHeights)
			totalRowHeight += rh;
		int usedRowSpace = 0;
		int leftover = crossLen - totalRowHeight - marginSz.evaluate(crossLen) * 2;
		if(bounds.length > 1)
			leftover -= paddingSz.evaluate(crossLen) * (bounds.length - 1);
		for(int i = 0; i < bounds.length - 1; i++) {
			if(tester.isWrapped(i)) {
				int space = Math.round(leftover * 1.0f * (rowIndex + 1) / (rowHeights.length + 1));
				crossPos += space - usedRowSpace;
				int rowHeight = rowHeights[rowIndex];
				position(parentBounds, bounds, dir, start, i + 1, crossPos, rowHeights, rowIndex, align, crossAlign, marginSz, marginSz,
					paddingSz, paddingSz);
				usedRowSpace = space;
				start = i + 1;
				rowIndex++;
				crossPos += rowHeight + paddingSz.evaluate(crossLen);
			}
		}
		int space = Math.round(leftover * 1.0f * (rowIndex + 1) / (rowHeights.length + 1));
		position(parent.bounds().getSize(), bounds, dir, start, children.length, crossPos + space - usedRowSpace, rowHeights, rowIndex,
			align, crossAlign, marginSz, marginSz, paddingSz, paddingSz);

		for(int c = 0; c < children.length; c++)
			children[c].bounds().setBounds(bounds[c].x, bounds[c].y, bounds[c].width, bounds[c].height);

		theSizerCache.clear();
	}

	private void position(Dimension parentSize, Rectangle [] bounds, Direction dir, int start, int end, int crossPos, int [] rowHeights,
		int rowIndex, Alignment mainAlign, Alignment crossAlign, Size marginX, Size marginY, Size paddingX, Size paddingY) {
		int mainLen = LayoutUtils.get(parentSize, dir.getOrientation());
		int childLen = 0;
		for(int i = start; i < end; i++)
			childLen += LayoutUtils.getSize(bounds[i], dir.getOrientation());
		int mainMargin = (dir.getOrientation() == Orientation.horizontal ? marginX : marginY).evaluate(mainLen);
		int mainPadding = (dir.getOrientation() == Orientation.horizontal ? paddingX : paddingY).evaluate(mainLen);
		int rowHeight = rowHeights[rowIndex];
		int leftover = mainLen - childLen - mainMargin * 2 - mainPadding * (end - start - 1);
		int mainPos = mainMargin;
		int usedSpace = 0;
		for(int i = start; i < end; i++) {
			// Align in main dimension
			switch (mainAlign) {
			case begin:
				LayoutUtils.setPos(bounds[i], dir.getOrientation(), mainPos);
				break;
			case end:
				LayoutUtils.setPos(bounds[i], dir.getOrientation(),
					mainLen - mainPos - LayoutUtils.getSize(bounds[i], dir.getOrientation()) - 1);
				break;
			case center:
			case justify:
				int space = Math.round(leftover * 1.0f * (i - start + 1) / (end - start + 1));
				LayoutUtils.setPos(bounds[i], dir.getOrientation(), mainPos + space - usedSpace);
				usedSpace = space;
				break;
			}
			mainPos += LayoutUtils.getSize(bounds[i], dir.getOrientation());
			mainPos += mainPadding;

			// Align in cross dimension
			switch (crossAlign) {
			case begin:
				LayoutUtils.setPos(bounds[i], dir.getOrientation().opposite(), crossPos);
				break;
			case end:
				LayoutUtils.setPos(bounds[i], dir.getOrientation().opposite(),
					crossPos + rowHeight - LayoutUtils.getSize(bounds[i], dir.getOrientation().opposite()) - 1);
				break;
			case center:
			case justify:
				LayoutUtils.setPos(bounds[i], dir.getOrientation().opposite(),
					crossPos + (rowHeight - LayoutUtils.getSize(bounds[i], dir.getOrientation().opposite())) / 2);
				break;
			}
		}
	}
}
