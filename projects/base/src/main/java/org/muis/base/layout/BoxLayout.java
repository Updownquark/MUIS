package org.muis.base.layout;

import org.muis.core.MuisAttribute;
import org.muis.core.MuisElement;
import org.muis.core.MuisLayout;
import org.muis.core.event.MuisEvent;
import org.muis.core.layout.SimpleSizePolicy;
import org.muis.core.layout.SizePolicy;

/**
 * Lays out children one-by-one along a given {@link LayoutConstants#direction direction} ({@link Direction#DOWN DOWN} by default), with a
 * given {@link LayoutConstants#alignment alignment} along the opposite axis. {@link LayoutConstants#width}, {@link LayoutConstants#height},
 * {@link LayoutConstants#minWidth}, and {@link LayoutConstants#minHeight} may be used to help determine the sizes of children.
 */
public class BoxLayout implements MuisLayout
{
	private static class RelayoutParentListener implements org.muis.core.event.MuisEventListener<MuisAttribute<?>>
	{
		private final MuisElement theParent;

		RelayoutParentListener(MuisElement parent)
		{
			theParent = parent;
		}

		@Override
		public void eventOccurred(MuisEvent<MuisAttribute<?>> event, MuisElement element)
		{
			MuisAttribute<?> attr = event.getValue();
			if(attr == LayoutConstants.direction || attr == LayoutConstants.alignment)
				theParent.relayout(false);
		}

		@Override
		public boolean isLocal()
		{
			return true;
		}
	}

	private static class RelayoutChildListener implements org.muis.core.event.MuisEventListener<MuisAttribute<?>>
	{
		private final MuisElement theParent;

		RelayoutChildListener(MuisElement parent)
		{
			theParent = parent;
		}

		@Override
		public void eventOccurred(MuisEvent<MuisAttribute<?>> event, MuisElement element)
		{
			MuisAttribute<?> attr = event.getValue();
			if(attr == LayoutConstants.width || attr == LayoutConstants.height || attr == LayoutConstants.minWidth
				|| attr == LayoutConstants.minHeight)
				theParent.relayout(false);
		}

		@Override
		public boolean isLocal()
		{
			return true;
		}
	}

	@Override
	public void initChildren(MuisElement parent, MuisElement [] children)
	{
		RelayoutParentListener parentListener = new RelayoutParentListener(parent);
		parent.acceptAttribute(LayoutConstants.direction);
		parent.acceptAttribute(LayoutConstants.alignment);
		parent.addListener(MuisElement.ATTRIBUTE_SET, parentListener);
		RelayoutChildListener childListener = new RelayoutChildListener(parent);
		for(MuisElement child : children)
		{
			child.acceptAttribute(LayoutConstants.width);
			child.acceptAttribute(LayoutConstants.height);
			child.acceptAttribute(LayoutConstants.minWidth);
			child.acceptAttribute(LayoutConstants.minHeight);
			child.addListener(MuisElement.ATTRIBUTE_SET, childListener);
		}
	}

	@Override
	public SizePolicy getWSizer(MuisElement parent, MuisElement [] children, int parentHeight)
	{
		Direction dir = parent.getAttribute(LayoutConstants.direction);
		if(dir == null)
			dir = Direction.DOWN;
		switch (dir)
		{
		case UP:
		case DOWN:
			return getCrossSizer(children, false, parentHeight, LayoutConstants.width, LayoutConstants.minWidth);
		case LEFT:
		case RIGHT:
			return getMainSizer(children, false, parentHeight, LayoutConstants.width, LayoutConstants.minWidth);
		}
		throw new IllegalStateException("Unrecognized layout direction: " + dir);
	}

	@Override
	public SizePolicy getHSizer(MuisElement parent, MuisElement [] children, int parentWidth)
	{
		Direction dir = parent.getAttribute(LayoutConstants.direction);
		if(dir == null)
			dir = Direction.DOWN;
		switch (dir)
		{
		case UP:
		case DOWN:
			return getMainSizer(children, false, parentWidth, LayoutConstants.height, LayoutConstants.minHeight);
		case LEFT:
		case RIGHT:
			return getCrossSizer(children, false, parentWidth, LayoutConstants.height, LayoutConstants.minHeight);
		}
		throw new IllegalStateException("Unrecognized layout direction: " + dir);
	}

	/**
	 * Gets the size policy in the main direction of the container
	 * 
	 * @param children The children to get the sizer for
	 * @param vertical Whether the main direction is vertical
	 * @param crossSize The size of the opposite (non-main) dimension of the space to lay out the children in
	 * @param sizeAttr The attribute to control a child's size (width or height)
	 * @param minSizeAttr The attribute to control a child's minimum size (minWidth or minHeight)
	 * @return The size policy for the children
	 */
	protected SizePolicy getMainSizer(MuisElement [] children, boolean vertical, int crossSize, MuisAttribute<Length> sizeAttr,
		MuisAttribute<Length> minSizeAttr)
	{
		SimpleSizePolicy ret = new SimpleSizePolicy();
		for(MuisElement child : children)
		{
			Length size = child.getAttribute(sizeAttr);
			Length minSize = child.getAttribute(minSizeAttr);
			if(size != null && !size.getUnit().isRelative())
			{
				ret.setMin(ret.getMin() + size.evaluate(0));
				ret.setPreferred(ret.getPreferred() + size.evaluate(0));
			}
			else
			{
				SizePolicy sizer = vertical ? child.getHSizer(crossSize) : child.getWSizer(crossSize);
				int min = sizer.getMin();
				int pref = sizer.getPreferred();
				if(minSize != null && !minSize.getUnit().isRelative() && min < minSize.evaluate(0))
					min = minSize.evaluate(0);
				if(pref < min)
					pref = min;
				ret.setMin(ret.getMin() + min);
				ret.setPreferred(ret.getPreferred() + pref);
			}
		}
		return ret;
	}

	/**
	 * Gets the size policy in the non-main direction of the container
	 * 
	 * @param children The children to get the sizer for
	 * @param vertical Whether the non-main direction is vertical
	 * @param mainSize The size of the opposite (main) dimension of the space to lay out the children in
	 * @param sizeAttr The attribute to control a child's size (width or height)
	 * @param minSizeAttr The attribute to control a child's minimum size (minWidth or minHeight)
	 * @return The size policy for the children
	 */
	protected SizePolicy getCrossSizer(MuisElement [] children, boolean vertical, int mainSize, MuisAttribute<Length> sizeAttr,
		MuisAttribute<Length> minSizeAttr)
	{
		SimpleSizePolicy ret = new SimpleSizePolicy();
		for(MuisElement child : children)
		{
			Length size = child.getAttribute(sizeAttr);
			Length minSize = child.getAttribute(minSizeAttr);
			if(size != null && !size.getUnit().isRelative())
			{
				int sz = size.evaluate(0);
				if(ret.getMin() < sz)
					ret.setMin(sz);
				if(ret.getPreferred() < sz)
					ret.setPreferred(sz);
			}
			else
			{
				SizePolicy sizer = vertical ? child.getHSizer(mainSize) : child.getWSizer(mainSize);
				int min = sizer.getMin();
				int pref = sizer.getPreferred();
				if(minSize != null && !minSize.getUnit().isRelative() && min < minSize.evaluate(0))
					min = minSize.evaluate(0);
				if(pref < min)
					pref = min;
				if(ret.getMin() < min)
					ret.setMin(min);
				if(ret.getPreferred() < pref)
					ret.setPreferred(pref);
			}
		}
		return ret;
	}

	@Override
	public void layout(MuisElement parent, MuisElement [] children)
	{
		java.awt.Rectangle bounds = new java.awt.Rectangle();
		Direction dir = parent.getAttribute(LayoutConstants.direction);
		if(dir == null)
			dir = Direction.DOWN;
		Alignment align = parent.getAttribute(LayoutConstants.alignment);
		if(align == null)
			align = Alignment.begin;
		int begin = 0;
		switch (dir)
		{
		case DOWN:
			begin = 0;
			break;
		case UP:
			begin = parent.getHeight();
			break;
		case RIGHT:
			begin = 0;
			break;
		case LEFT:
			begin = parent.getWidth();
			break;
		}
		for(MuisElement child : children)
		{
			Length w = child.getAttribute(LayoutConstants.width);
			Length h = child.getAttribute(LayoutConstants.height);
			Length minW = child.getAttribute(LayoutConstants.minWidth);
			Length minH = child.getAttribute(LayoutConstants.minHeight);

			int mainSize;
			int crossSize;
			switch (dir)
			{
			case UP:
			case DOWN:
				mainSize = getMainSize(child, true, parent.getHeight(), parent.getWidth(), h, minH);
				if(dir.isPositive())
				{
					bounds.y = begin;
					begin += mainSize;
				}
				else
				{
					begin -= mainSize;
					bounds.y = begin;
				}
				bounds.height = mainSize;

				if(align == Alignment.justify)
					crossSize = parent.getWidth();
				else
					crossSize = getCrossSize(child, false, parent.getHeight(), parent.getWidth(), w, minW);
				switch (align)
				{
				case begin:
					bounds.x = 0;
					break;
				case end:
					bounds.x = parent.getWidth() - crossSize;
					break;
				case center:
					bounds.x = (parent.getWidth() - crossSize) / 2;
					break;
				case justify:
					bounds.x = 0;
					break;
				}
				break;
			case LEFT:
			case RIGHT:
				mainSize = getMainSize(child, false, parent.getWidth(), parent.getHeight(), w, minW);
				if(dir.isPositive())
				{
					bounds.x = begin;
					begin += mainSize;
				}
				else
				{
					begin -= mainSize;
					bounds.x = begin;
				}
				bounds.width = mainSize;

				if(align == Alignment.justify)
					crossSize = parent.getHeight();
				else
					crossSize = getCrossSize(child, true, parent.getWidth(), parent.getHeight(), h, minH);
				switch (align)
				{
				case begin:
					bounds.y = 0;
					break;
				case end:
					bounds.y = parent.getHeight() - crossSize;
					break;
				case center:
					bounds.y = (parent.getHeight() - crossSize) / 2;
					break;
				case justify:
					bounds.y = 0;
					break;
				}
				break;
			}
			child.setBounds(bounds.x, bounds.y, bounds.width, bounds.height);
		}
	}

	/**
	 * Gets the size for a child in the main direction of the container
	 * 
	 * @param child The child to size
	 * @param vertical Whether the main direction is vertical
	 * @param mainSize The size of the parent in the main dimension
	 * @param crossSize The size of the parent in the cross dimension
	 * @param sizeAttr The value of the attribute to control the size of the child (width or height)
	 * @param minSizeAttr The value of the attribute to control the minimum size of the child (minWidth or minHeight)
	 * @return The main-dimension size of the child
	 */
	protected int getMainSize(MuisElement child, boolean vertical, int mainSize, int crossSize, Length sizeAttr, Length minSizeAttr)
	{
		int ret;
		if(sizeAttr != null)
		{
			ret = sizeAttr.evaluate(mainSize);
			if(minSizeAttr != null && ret < minSizeAttr.evaluate(mainSize))
				ret = minSizeAttr.evaluate(mainSize);
		}
		else
		{
			SizePolicy sizer = vertical ? child.getHSizer(crossSize) : child.getWSizer(crossSize);
			ret = sizer.getPreferred();
			if(ret > mainSize)
				ret = mainSize;
			if(ret < sizer.getMin())
				ret = sizer.getMin();
			if(minSizeAttr != null && ret < minSizeAttr.evaluate(mainSize))
				ret = minSizeAttr.evaluate(mainSize);
		}
		return ret;
	}

	/**
	 * Gets the size for a child in the non-main direction of the container
	 * 
	 * @param child The child to size
	 * @param vertical Whether the cross direction is vertical
	 * @param mainSize The size of the parent in the main dimension
	 * @param crossSize The size of the parent in the cross dimension
	 * @param sizeAttr The value of the attribute to control the size of the child (width or height)
	 * @param minSizeAttr The value of the attribute to control the minimum size of the child (minWidth or minHeight)
	 * @return The non-main-dimension size of the child
	 */
	protected int getCrossSize(MuisElement child, boolean vertical, int mainSize, int crossSize, Length sizeAttr, Length minSizeAttr)
	{
		return getMainSize(child, vertical, crossSize, mainSize, sizeAttr, minSizeAttr);
	}

	@Override
	public void remove(MuisElement parent)
	{
		parent.removeListener(MuisElement.ATTRIBUTE_SET, RelayoutParentListener.class);
		parent.rejectAttribute(LayoutConstants.direction);
		parent.rejectAttribute(LayoutConstants.alignment);
		for(MuisElement child : parent.getChildren())
		{
			child.removeListener(MuisElement.ATTRIBUTE_SET, RelayoutChildListener.class);
			child.rejectAttribute(LayoutConstants.width);
			child.rejectAttribute(LayoutConstants.height);
			child.rejectAttribute(LayoutConstants.minWidth);
			child.rejectAttribute(LayoutConstants.minHeight);
		}
	}
}
