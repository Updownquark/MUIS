package org.muis.base.layout;

import org.muis.core.MuisElement;
import org.muis.core.layout.SizeGuide;

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
public class SuperFlowLayout extends AbstractFlowLayout
{
	@Override
	public void layout(MuisElement parent, MuisElement [] children)
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void remove(MuisElement parent)
	{
		// TODO Auto-generated method stub

	}

	@Override
	protected SizeGuide getMajorSize(MuisElement [] children, int minorSize)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected SizeGuide getMinorSize(MuisElement [] children, int majorSize)
	{
		// TODO Auto-generated method stub
		return null;
	}
}
