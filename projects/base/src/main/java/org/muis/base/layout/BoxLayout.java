package org.muis.base.layout;

import static org.muis.core.layout.LayoutAttributes.*;

import org.muis.core.MuisAttribute;
import org.muis.core.MuisElement;
import org.muis.core.MuisLayout;
import org.muis.core.layout.*;
import org.muis.core.style.Size;
import org.muis.util.CompoundListener;

/**
 * Lays out children one-by-one along a given {@link LayoutAttributes#direction direction} ({@link Direction#DOWN DOWN} by default), with a
 * given {@link LayoutAttributes#alignment alignment} along the opposite axis. {@link LayoutAttributes#width}, {@link LayoutAttributes#height},
 * {@link LayoutAttributes#minWidth}, and {@link LayoutAttributes#minHeight} may be used to help determine the sizes of children.
 */
public class BoxLayout implements MuisLayout
{
	private final CompoundListener.MultiElementCompoundListener theListener;

	/** Creates a box layout */
	public BoxLayout()
	{
		theListener = CompoundListener.create(this);
		theListener.acceptAll(direction, alignment).onChange(CompoundListener.layout);
		theListener.child().acceptAll(width, minWidth, height, minHeight).onChange(CompoundListener.layout);
	}

	@Override
	public void initChildren(MuisElement parent, MuisElement [] children)
	{
	}

	@Override
	public void childAdded(MuisElement parent, MuisElement child)
	{
	}

	@Override
	public void childRemoved(MuisElement parent, MuisElement child)
	{
	}

	@Override
	public SizeGuide getWSizer(MuisElement parent, MuisElement [] children)
	{
		Direction dir = parent.atts().get(LayoutAttributes.direction);
		if(dir == null)
			dir = Direction.DOWN;
		switch (dir)
		{
		case UP:
		case DOWN:
			return getCrossSizer(children, false, LayoutAttributes.width, LayoutAttributes.minWidth);
		case LEFT:
		case RIGHT:
			return getMainSizer(children, false, LayoutAttributes.width, LayoutAttributes.minWidth);
		}
		throw new IllegalStateException("Unrecognized layout direction: " + dir);
	}

	@Override
	public SizeGuide getHSizer(MuisElement parent, MuisElement [] children)
	{
		Direction dir = parent.atts().get(LayoutAttributes.direction);
		if(dir == null)
			dir = Direction.DOWN;
		switch (dir)
		{
		case UP:
		case DOWN:
			return getMainSizer(children, false, LayoutAttributes.height, LayoutAttributes.minHeight);
		case LEFT:
		case RIGHT:
			return getCrossSizer(children, false, LayoutAttributes.height, LayoutAttributes.minHeight);
		}
		throw new IllegalStateException("Unrecognized layout direction: " + dir);
	}

	/**
	 * Gets the size policy in the main direction of the container
	 *
	 * @param children The children to get the sizer for
	 * @param vertical Whether the main direction is vertical
	 * @param sizeAttr The attribute to control a child's size (width or height)
	 * @param minSizeAttr The attribute to control a child's minimum size (minWidth or minHeight)
	 * @return The size policy for the children
	 */
	protected SizeGuide getMainSizer(MuisElement [] children, boolean vertical, MuisAttribute<Size> sizeAttr,
		MuisAttribute<Size> minSizeAttr)
	{
		SimpleSizeGuide ret = new SimpleSizeGuide();
		for(MuisElement child : children)
		{
			Size size = child.atts().get(sizeAttr);
			Size minSize = child.atts().get(minSizeAttr);
			if(size != null && !size.getUnit().isRelative())
			{
				ret.setMin(ret.getMin() + size.evaluate(0));
				ret.setPreferred(ret.getPreferred() + size.evaluate(0));
			}
			else
			{
				SizeGuide sizer = vertical ? child.getHSizer(crossSize) : child.getWSizer(crossSize);
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
	 * @param sizeAttr The attribute to control a child's size (width or height)
	 * @param minSizeAttr The attribute to control a child's minimum size (minWidth or minHeight)
	 * @return The size policy for the children
	 */
	protected SizeGuide getCrossSizer(MuisElement [] children, boolean vertical, MuisAttribute<Size> sizeAttr,
		MuisAttribute<Size> minSizeAttr)
	{
		SimpleSizeGuide ret = new SimpleSizeGuide();
		for(MuisElement child : children)
		{
			Size size = child.atts().get(sizeAttr);
			Size minSize = child.atts().get(minSizeAttr);
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
				SizeGuide sizer = vertical ? child.getHSizer(mainSize) : child.getWSizer(mainSize);
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
		Direction dir = parent.atts().get(LayoutAttributes.direction);
		if(dir == null)
			dir = Direction.DOWN;
		Alignment align = parent.atts().get(LayoutAttributes.alignment);
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
			Size w = child.atts().get(LayoutAttributes.width);
			Size h = child.atts().get(LayoutAttributes.height);
			Size minW = child.atts().get(LayoutAttributes.minWidth);
			Size minH = child.atts().get(LayoutAttributes.minHeight);

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
				bounds.width = crossSize;
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
				bounds.height = crossSize;
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
	protected int getMainSize(MuisElement child, boolean vertical, int mainSize, int crossSize, Size sizeAttr, Size minSizeAttr)
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
			SizeGuide sizer = vertical ? child.getHSizer(crossSize) : child.getWSizer(crossSize);
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
	protected int getCrossSize(MuisElement child, boolean vertical, int mainSize, int crossSize, Size sizeAttr, Size minSizeAttr)
	{
		return getMainSize(child, vertical, crossSize, mainSize, sizeAttr, minSizeAttr);
	}

	@Override
	public void remove(MuisElement parent)
	{
	}
}
