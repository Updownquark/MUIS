package org.muis.base.layout;

import static org.muis.core.layout.LayoutAttributes.*;

import org.muis.core.MuisAttribute;
import org.muis.core.MuisElement;
import org.muis.core.MuisLayout;
import org.muis.core.layout.LayoutAttributes;
import org.muis.core.layout.SimpleSizePolicy;
import org.muis.core.layout.SizePolicy;
import org.muis.core.style.Position;
import org.muis.core.style.Size;
import org.muis.util.CompoundListener;

/**
 * A very simple layout that positions and sizes children independently using positional ({@link LayoutAttributes#left},
 * {@link LayoutAttributes#right}, {@link LayoutAttributes#top}, {@link LayoutAttributes#bottom}), size ({@link LayoutAttributes#width} and
 * {@link LayoutAttributes#height}) and minimum size ({@link LayoutAttributes#minWidth} and {@link LayoutAttributes#minHeight}) attributes or
 * sizers.
 */
public class SimpleLayout implements MuisLayout
{
	private final CompoundListener.MultiElementCompoundListener theListener;

	/** Creates a simple layout */
	public SimpleLayout()
	{
		theListener = CompoundListener.create(this);
		theListener.accept(LayoutAttributes.maxInf).onChange(CompoundListener.layout);
		theListener.child().acceptAll(left, right, top, bottom, width, minWidth, maxWidth, height, minHeight, maxHeight)
			.onChange(CompoundListener.layout);
	}

	@Override
	public void initChildren(MuisElement parent, MuisElement [] children)
	{
		theListener.listenerFor(parent);
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
	public SizePolicy getWSizer(MuisElement parent, MuisElement [] children)
	{
		return getSizer(children, LayoutAttributes.left, LayoutAttributes.right, LayoutAttributes.width, LayoutAttributes.minWidth,
			LayoutAttributes.maxWidth, false, parent.atts().get(LayoutAttributes.maxInf));
	}

	@Override
	public SizePolicy getHSizer(MuisElement parent, MuisElement [] children)
	{
		return getSizer(children, LayoutAttributes.top, LayoutAttributes.bottom, LayoutAttributes.height, LayoutAttributes.minHeight,
			LayoutAttributes.maxHeight, true, parent.atts().get(LayoutAttributes.maxInf));
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
	 * @param vertical Whether the children are being sized in the vertical dimension or the horizontal
	 * @param maxInfValue The value for the {@link LayoutAttributes#maxInf} attribute in the parent
	 * @return The size policy for the container of the given children in the given dimension
	 */
	protected SizePolicy getSizer(final MuisElement [] children, final MuisAttribute<Position> minPosAtt,
		final MuisAttribute<Position> maxPosAtt, final MuisAttribute<Size> sizeAtt, final MuisAttribute<Size> minSizeAtt,
		final MuisAttribute<Size> maxSizeAtt, final boolean vertical, final Boolean maxInfValue)
	{
		return new SizePolicy() {
			private int theCachedCrossSize = -1;

			SimpleSizePolicy theCachedSize = new SimpleSizePolicy();

			@Override
			public int getMinPreferred() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public int getMaxPreferred() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public int getMin(int crossSize) {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public int getPreferred(int crossSize) {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public int getMax(int crossSize) {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public float getStretch() {
				// TODO Auto-generated method stub
				return 0;
			}

			private void calculate(int crossSize) {
				if(crossSize == theCachedCrossSize)
					return;
				theCachedCrossSize = crossSize;
		boolean isMaxInf = Boolean.TRUE.equals(maxInfValue);
		for(MuisElement child : children)
		{
			Position minPosL = child.atts().get(minPosAtt);
			Position maxPosL = child.atts().get(maxPosAtt);
			Size sizeL = child.atts().get(sizeAtt);
			Size minSizeL = child.atts().get(minSizeAtt);
			Size maxSizeL = child.atts().get(maxSizeAtt);
			if(maxPosL != null && !maxPosL.getUnit().isRelative())
			{
				int r = maxPosL.evaluate(0);
						if(theCachedSize.getMin(0) < r)
							theCachedSize.setMin(r);
						if(theCachedSize.getPreferred(0) < r)
							theCachedSize.setPreferred(r);
			}
			else if(sizeL != null && !sizeL.getUnit().isRelative())
			{
				int w = sizeL.evaluate(0);
				int x;
				if(minPosL != null && !minPosL.getUnit().isRelative())
					x = minPosL.evaluate(0);
				else
					x = 0;

						if(theCachedSize.getMin(0) < x + w)
							theCachedSize.setMin(x + w);
						if(theCachedSize.getPreferred(0) < x + w)
							theCachedSize.setPreferred(x + w);
				if(!isMaxInf)
				{
					int max;
					if(maxSizeL != null && !maxSizeL.getUnit().isRelative())
						max = maxSizeL.evaluate(0);
					else
					{
								SizePolicy childSizer = vertical ? child.getHSizer() : child.getWSizer();
								max = childSizer.getMax(crossSize);
					}
					if(max + x > max)
						max += x;
							if(theCachedSize.getMax(0) > max)
								theCachedSize.setMax(max);
				}
			}
			else if(minSizeL != null && !minSizeL.getUnit().isRelative())
			{
						SizePolicy childSizer = vertical ? child.getHSizer() : child.getWSizer();
				int minW = minSizeL.evaluate(0);
						int prefW = childSizer.getPreferred(crossSize);
				if(prefW < minW)
					prefW = minW;
				int x;
				if(minPosL != null && !minPosL.getUnit().isRelative())
					x = minPosL.evaluate(0);
				else
					x = 0;

						if(theCachedSize.getMin(0) < x + minW)
							theCachedSize.setMin(x + minW);
						if(theCachedSize.getPreferred(0) < x + prefW)
							theCachedSize.setPreferred(x + prefW);
				if(!isMaxInf)
				{
					int max;
					if(maxSizeL != null && !maxSizeL.getUnit().isRelative())
						max = maxSizeL.evaluate(0);
					else
								max = childSizer.getMax(crossSize);
					if(max + x > max)
						max += x;
							if(theCachedSize.getMax(0) > max)
								theCachedSize.setMax(max);
				}
			}
			else if(minPosL != null && !minPosL.getUnit().isRelative())
			{
						SizePolicy childSizer = vertical ? child.getHSizer() : child.getWSizer();
				int x = minPosL.evaluate(0);
						if(theCachedSize.getMin(0) < x + childSizer.getMin(crossSize))
							theCachedSize.setMin(x + childSizer.getMin(crossSize));
						if(theCachedSize.getPreferred(0) < x + childSizer.getPreferred(crossSize))
							theCachedSize.setPreferred(x + childSizer.getPreferred(crossSize));
				if(!isMaxInf)
				{
					int max;
					if(maxSizeL != null && !maxSizeL.getUnit().isRelative())
						max = maxSizeL.evaluate(0);
					else
								max = childSizer.getMax(crossSize);
					if(max + x > max)
						max += x;
							if(theCachedSize.getMax(0) > max)
								theCachedSize.setMax(max);
				}
			}
			else
			{
						SizePolicy childSizer = vertical ? child.getHSizer() : child.getWSizer();
						if(theCachedSize.getMin(0) < childSizer.getMin(crossSize))
							theCachedSize.setMin(childSizer.getMin(crossSize));
						if(theCachedSize.getPreferred(0) < childSizer.getPreferred(crossSize))
							theCachedSize.setPreferred(childSizer.getPreferred(crossSize));
						if(!isMaxInf && theCachedSize.getMax(0) > childSizer.getMax(crossSize))
							theCachedSize.setMax(childSizer.getMax(crossSize));
			}
		}
			}
		};
	}

	@Override
	public void layout(MuisElement parent, MuisElement [] children)
	{
		java.awt.Rectangle bounds = new java.awt.Rectangle();
		int [] dim = new int[2];
		for(MuisElement child : children)
		{
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
				SizePolicy sizer = vertical ? child.getHSizer() : child.getWSizer();
				dim[1] = sizer.getPreferred(breadth);
				if(minSizeAtt != null && dim[1] < minSizeAtt.evaluate(length))
					dim[1] = minSizeAtt.evaluate(length);
				dim[0] = max - dim[1];
			}
		}
		else if(sizeAtt != null)
		{
			dim[1] = sizeAtt.evaluate(length);
			if(minSizeAtt != null && dim[1] < minSizeAtt.evaluate(length))
				dim[1] = minSizeAtt.evaluate(length);
			if(minPosAtt != null)
				dim[0] = minPosAtt.evaluate(length);
			else
				dim[0] = 0;
		}
		else if(minPosAtt != null)
		{
			SizePolicy sizer = vertical ? child.getHSizer() : child.getWSizer();
			dim[0] = minPosAtt.evaluate(length);
			dim[1] = sizer.getPreferred(breadth);
			if(minSizeAtt != null && dim[1] < minSizeAtt.evaluate(length))
				dim[1] = minSizeAtt.evaluate(length);
		}
		else
		{
			SizePolicy sizer = vertical ? child.getHSizer() : child.getWSizer();
			dim[0] = 0;
			dim[1] = sizer.getPreferred(breadth);
			if(minSizeAtt != null && dim[1] < minSizeAtt.evaluate(length))
				dim[1] = minSizeAtt.evaluate(length);
		}
	}

	@Override
	public void remove(MuisElement parent)
	{
		theListener.dropFor(parent);
	}
}
