package org.muis.base.layout;

import org.muis.core.MuisAttribute;
import org.muis.core.MuisElement;
import org.muis.core.MuisLayout;
import org.muis.core.annotations.MuisActionType;
import org.muis.core.annotations.MuisAttrConsumer;
import org.muis.core.annotations.MuisAttrType;
import org.muis.core.annotations.NeededAttr;
import org.muis.core.layout.SimpleSizePolicy;
import org.muis.core.layout.SizePolicy;
import org.muis.core.style.Position;
import org.muis.core.style.Size;

/**
 * A very simple layout that positions and sizes children independently using positional ({@link LayoutConstants#left},
 * {@link LayoutConstants#right}, {@link LayoutConstants#top}, {@link LayoutConstants#bottom}), size ({@link LayoutConstants#width} and
 * {@link LayoutConstants#height}) and minimum size ({@link LayoutConstants#minWidth} and {@link LayoutConstants#minHeight}) attributes or
 * sizers.
 */
@MuisAttrConsumer(
	attrs = {@NeededAttr(name = "max-inf", type = MuisAttrType.BOOLEAN)},
	childAttrs = {@NeededAttr(name = "left", type = MuisAttrType.POSITION), @NeededAttr(name = "right", type = MuisAttrType.POSITION),
			@NeededAttr(name = "top", type = MuisAttrType.POSITION), @NeededAttr(name = "bottom", type = MuisAttrType.POSITION),
			@NeededAttr(name = "width", type = MuisAttrType.SIZE), @NeededAttr(name = "height", type = MuisAttrType.SIZE),
			@NeededAttr(name = "min-width", type = MuisAttrType.SIZE), @NeededAttr(name = "max-width", type = MuisAttrType.SIZE),
			@NeededAttr(name = "min-height", type = MuisAttrType.SIZE), @NeededAttr(name = "max-height", type = MuisAttrType.SIZE)},
	action = MuisActionType.layout)
public class SimpleLayout implements MuisLayout
{
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
	public SizePolicy getWSizer(MuisElement parent, MuisElement [] children, int parentHeight)
	{
		return getSizer(children, LayoutConstants.left, LayoutConstants.right, LayoutConstants.width, LayoutConstants.minWidth,
			LayoutConstants.maxWidth, parentHeight, false, parent.getAttribute(LayoutConstants.maxInf));
	}

	@Override
	public SizePolicy getHSizer(MuisElement parent, MuisElement [] children, int parentWidth)
	{
		return getSizer(children, LayoutConstants.top, LayoutConstants.bottom, LayoutConstants.height, LayoutConstants.minHeight,
			LayoutConstants.maxHeight, parentWidth, true, parent.getAttribute(LayoutConstants.maxInf));
	}

	/**
	 * Gets a sizer for a container in one dimension
	 *
	 * @param children The children to lay out
	 * @param minPosAtt The attribute to control the minimum position of a child (left or top)
	 * @param maxPosAtt The attribute to control the maximum position of a child (right or bottom)
	 * @param sizeAtt The attribute to control the size of a child (width or height)
	 * @param minSizeAtt The attribute to control the minimum size of a child (minWidth or minHeight)
	 * @param maxSizeAtt The attribute to control the maximum size of a child (maxWidth or maxHeight)
	 * @param breadth The size of the opposite dimension of the space to lay out the children in
	 * @param vertical Whether the children are being sized in the vertical dimension or the horizontal
	 * @param maxInfValue The value for the {@link LayoutConstants#maxInf} attribute in the parent
	 * @return The size policy for the container of the given children in the given dimension
	 */
	protected SizePolicy getSizer(MuisElement [] children, MuisAttribute<Position> minPosAtt, MuisAttribute<Position> maxPosAtt,
		MuisAttribute<Size> sizeAtt, MuisAttribute<Size> minSizeAtt, MuisAttribute<Size> maxSizeAtt, int breadth, boolean vertical,
		Boolean maxInfValue)
	{
		SimpleSizePolicy ret = new SimpleSizePolicy();
		boolean maxInf = Boolean.TRUE.equals(maxInfValue);
		for(MuisElement child : children)
		{
			Position minPosL = child.getAttribute(minPosAtt);
			Position maxPosL = child.getAttribute(maxPosAtt);
			Size sizeL = child.getAttribute(sizeAtt);
			Size minSizeL = child.getAttribute(minSizeAtt);
			Size maxSizeL = child.getAttribute(maxSizeAtt);
			if(maxPosL != null && !maxPosL.getUnit().isRelative())
			{
				int r = maxPosL.evaluate(0);
				if(ret.getMin() < r)
				{
					ret.setMin(r);
				}
				if(ret.getPreferred() < r)
				{
					ret.setPreferred(r);
				}
			}
			else if(sizeL != null && !sizeL.getUnit().isRelative())
			{
				int w = sizeL.evaluate(0);
				int x;
				if(minPosL != null && !minPosL.getUnit().isRelative())
				{
					x = minPosL.evaluate(0);
				}
				else
				{
					x = 0;
				}

				if(ret.getMin() < x + w)
				{
					ret.setMin(x + w);
				}
				if(ret.getPreferred() < x + w)
				{
					ret.setPreferred(x + w);
				}
				if(!maxInf)
				{
					int max;
					if(maxSizeL != null && !maxSizeL.getUnit().isRelative())
					{
						max = maxSizeL.evaluate(0);
					}
					else
					{
						SizePolicy childSizer = vertical ? child.getHSizer(breadth) : child.getWSizer(breadth);
						max = childSizer.getMax();
					}
					if(max + x > max)
					{
						max += x;
					}
					if(ret.getMax() > max)
					{
						ret.setMax(max);
					}
				}
			}
			else if(minSizeL != null && !minSizeL.getUnit().isRelative())
			{
				SizePolicy childSizer = vertical ? child.getHSizer(breadth) : child.getWSizer(breadth);
				int minW = minSizeL.evaluate(0);
				int prefW = childSizer.getPreferred();
				if(prefW < minW)
				{
					prefW = minW;
				}
				int x;
				if(minPosL != null && !minPosL.getUnit().isRelative())
				{
					x = minPosL.evaluate(0);
				}
				else
				{
					x = 0;
				}

				if(ret.getMin() < x + minW)
				{
					ret.setMin(x + minW);
				}
				if(ret.getPreferred() < x + prefW)
				{
					ret.setPreferred(x + prefW);
				}
				if(!maxInf)
				{
					int max;
					if(maxSizeL != null && !maxSizeL.getUnit().isRelative())
					{
						max = maxSizeL.evaluate(0);
					}
					else
					{
						max = childSizer.getMax();
					}
					if(max + x > max)
					{
						max += x;
					}
					if(ret.getMax() > max)
					{
						ret.setMax(max);
					}
				}
			}
			else if(minPosL != null && !minPosL.getUnit().isRelative())
			{
				SizePolicy childSizer = vertical ? child.getHSizer(breadth) : child.getWSizer(breadth);
				int x = minPosL.evaluate(0);
				if(ret.getMin() < x + childSizer.getMin())
				{
					ret.setMin(x + childSizer.getMin());
				}
				if(ret.getPreferred() < x + childSizer.getPreferred())
				{
					ret.setPreferred(x + childSizer.getPreferred());
				}
				if(!maxInf)
				{
					int max;
					if(maxSizeL != null && !maxSizeL.getUnit().isRelative())
					{
						max = maxSizeL.evaluate(0);
					}
					else
					{
						max = childSizer.getMax();
					}
					if(max + x > max)
					{
						max += x;
					}
					if(ret.getMax() > max)
					{
						ret.setMax(max);
					}
				}
			}
			else
			{
				SizePolicy childSizer = vertical ? child.getHSizer(breadth) : child.getWSizer(breadth);
				if(ret.getMin() < childSizer.getMin())
				{
					ret.setMin(childSizer.getMin());
				}
				if(ret.getPreferred() < childSizer.getPreferred())
				{
					ret.setPreferred(childSizer.getPreferred());
				}
				if(!maxInf && ret.getMax() > childSizer.getMax())
				{
					ret.setMax(childSizer.getMax());
				}
			}
		}
		return ret;
	}

	@Override
	public void layout(MuisElement parent, MuisElement [] children)
	{
		java.awt.Rectangle bounds = new java.awt.Rectangle();
		int [] dim = new int[2];
		for(MuisElement child : children)
		{
			Position left = child.getAttribute(LayoutConstants.left);
			Position right = child.getAttribute(LayoutConstants.right);
			Position top = child.getAttribute(LayoutConstants.top);
			Position bottom = child.getAttribute(LayoutConstants.bottom);
			Size w = child.getAttribute(LayoutConstants.width);
			Size h = child.getAttribute(LayoutConstants.height);
			Size minW = child.getAttribute(LayoutConstants.minWidth);
			Size minH = child.getAttribute(LayoutConstants.minHeight);

			layout(child, false, parent.getHeight(), left, right, w, minW, parent.getWidth(), dim);
			bounds.x = dim[0];
			bounds.width = dim[1];
			layout(child, true, parent.getWidth(), top, bottom, h, minH, parent.getHeight(), dim);
			bounds.y = dim[0];
			bounds.height = dim[1];
			child.setBounds(bounds.x, bounds.y, bounds.width, bounds.height);
		}
	}

	/**
	 * Lays out a single child on one dimension within its parent based on its attributes and its size policy
	 *
	 * @param child The child to position
	 * @param vertical Whether the layout dimension is vertical (to get the child's sizer if needed)
	 * @param breadth The size of the non-layout dimension of the parent
	 * @param minPosAtt The value of the attribute controlling the child's minimum position (left or top)
	 * @param maxPosAtt The value of the attribute controlling the child's maximum position (right or bottom)
	 * @param sizeAtt The value of the attribute controlling the child's size (width or height)
	 * @param minSizeAtt The value of the attribute controlling the child's minimum size(minWidth or minHeight)
	 * @param length The length of the parent container along the dimension
	 * @param dim The array to put the result (position (x or y) and size (width or height)) into
	 */
	protected void layout(MuisElement child, boolean vertical, int breadth, Position minPosAtt, Position maxPosAtt, Size sizeAtt,
		Size minSizeAtt, int length, int [] dim)
	{
		if(maxPosAtt != null)
		{
			int max = maxPosAtt.evaluate(length);
			if(minPosAtt != null)
			{
				dim[0] = minPosAtt.evaluate(length);
				dim[1] = max - dim[0];
			}
			else if(sizeAtt != null)
			{
				dim[1] = sizeAtt.evaluate(length);
				dim[0] = max - dim[1];
			}
			else
			{
				SizePolicy sizer = vertical ? child.getHSizer(breadth) : child.getWSizer(breadth);
				dim[1] = sizer.getPreferred();
				if(minSizeAtt != null && dim[1] < minSizeAtt.evaluate(length))
				{
					dim[1] = minSizeAtt.evaluate(length);
				}
				dim[0] = max - dim[1];
			}
		}
		else if(sizeAtt != null)
		{
			dim[1] = sizeAtt.evaluate(length);
			if(minSizeAtt != null && dim[1] < minSizeAtt.evaluate(length))
			{
				dim[1] = minSizeAtt.evaluate(length);
			}
			if(minPosAtt != null)
			{
				dim[0] = minPosAtt.evaluate(length);
			}
			else
			{
				dim[0] = 0;
			}
		}
		else if(minPosAtt != null)
		{
			SizePolicy sizer = vertical ? child.getHSizer(breadth) : child.getWSizer(breadth);
			dim[0] = minPosAtt.evaluate(length);
			dim[1] = sizer.getPreferred();
			if(minSizeAtt != null && dim[1] < minSizeAtt.evaluate(length))
			{
				dim[1] = minSizeAtt.evaluate(length);
			}
		}
		else
		{
			SizePolicy sizer = vertical ? child.getHSizer(breadth) : child.getWSizer(breadth);
			dim[0] = 0;
			dim[1] = sizer.getPreferred();
			if(minSizeAtt != null && dim[1] < minSizeAtt.evaluate(length))
			{
				dim[1] = minSizeAtt.evaluate(length);
			}
		}
	}

	@Override
	public void remove(MuisElement parent)
	{
	}
}
