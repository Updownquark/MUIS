package org.muis.layout;

import static org.muis.layout.LayoutConstants.*;

import org.muis.core.*;

/**
 * An extremely flexible and useful layout, FlowLayout renders similarly to HTML. By default, this layout will lay out
 * all content in a row, breaking into multiple rows when content prefers to be larger. Several options are available:
 * <ul>
 * <li>It can be adjusted to layout content to fill its container, breaking as needed to make the width and height
 * similar, or to lay out in a row, never breaking.</li>
 * <li>It can be made to lay out left-to-right, right-to-left, top-to-bottom, or bottom-to-top</li>
 * <li>In addition individual elements may use the top, left, bottom, right, height, and width attributes to control how
 * they are positioned and sized.</li>
 * <li>If an element should not "get in the way" of other elements being layed out in a container, the included="false"
 * attribute may be specified--the element will then be laid out as if it were the only piece of content in the
 * container and the rest of the content will not be affected by it.</li>
 * </ul>
 */
public class FlowLayout extends AbstractFlowLayout
{
	@Override
	public void layout(MuisElement parent, MuisElement [] children)
	{
		if(!isShapeSet)
			checkLayoutAttributes(parent);
		// TODO Auto-generated method stub
	}

	@Override
	public void remove(MuisElement parent)
	{
		// TODO Auto-generated method stub

	}

	@Override
	protected SizePolicy getMajorSize(MuisElement [] children, int minorSize)
	{
		final MuisAttribute<Length> posAtt, rPosAtt, sizeAtt;
		switch(getDirection())
		{
		case RIGHT:
			posAtt=left;
			rPosAtt=right;
			sizeAtt=width;
			break;
		case LEFT:
			posAtt=right;
			rPosAtt=left;
			sizeAtt=width;
			prisms.util.ArrayUtils.reverse(children);
			break;
		case DOWN:
			posAtt=top;
			rPosAtt=bottom;
			sizeAtt=height;
			break;
		case UP:
			posAtt=bottom;
			rPosAtt=top;
			sizeAtt=height;
			prisms.util.ArrayUtils.reverse(children);
			break;
		}
		java.util.ArrayList<MuisElement> loners = new java.util.ArrayList<MuisElement>(0);
		for(int i = 0; i < children.length; i++)
		{
			if(isLoner(children[i]))
			{
				loners.add(children[i]);
				children[i] = null;
				continue;
			}
			if(children[i].getAttribute(posAtt)!=null || children[i].getAttribute(rPosAtt)!=null)
		}
		if(minorSize > 0)
		{
		}
		else
		{}
	}

	@Override
	protected SizePolicy getMinorSize(MuisElement [] children, int majorSize)
	{
	}

	private static boolean isLoner(MuisElement element)
	{
		return Boolean.FALSE.equals(element.getAttribute(included));
	}
}
