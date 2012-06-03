package org.wam.core;

import org.wam.layout.SizePolicy;

/**
 * A simple container element that lays its children out using an implementation of
 * {@link WamLayout}
 */
public class LayoutContainer extends WamElement implements WamContainer
{
	/** The attribute that specifies the layout type for a layout container */
	public static WamAttribute<Class<? extends WamLayout>> LAYOUT_ATTR = new WamAttribute<Class<? extends WamLayout>>(
		"layout", new WamAttribute.WamTypeAttribute<WamLayout>(WamLayout.class));

	private WamLayout theLayout;

	@Override
	protected void postInit()
	{
		super.postInit();
		requireAttribute(LAYOUT_ATTR);
	}

	@Override
	public void postCreate()
	{
		Class<? extends WamLayout> layoutClass = getAttribute(LAYOUT_ATTR);
		WamLayout layout;
		try
		{
			layout = layoutClass.newInstance();
		} catch(Throwable e)
		{
			error("Could not instantiate layout class " + layoutClass.getName(), e);
			return;
		}
		theLayout = layout;
		theLayout.initChildren(this, getChildren());
		super.postCreate();
	}

	/**
	 * @return The WamLayout that lays out this container's children
	 */
	public WamLayout getLayout()
	{
		return theLayout;
	}

	/**
	 * @param layout The WamLayout to lay out this container's children
	 */
	public void setLayout(WamLayout layout)
	{
		if(theLayout != null)
			theLayout.remove(this);
		theLayout = layout;
		if(theLayout != null)
			theLayout.initChildren(this, getChildren());
	}

	@Override
	public SizePolicy getHSizer(int height)
	{
		if(theLayout != null)
			return theLayout.getWSizer(this, getChildren(), height);
		else
			return super.getHSizer(height);
	}

	@Override
	public SizePolicy getVSizer(int width)
	{
		if(theLayout != null)
			return theLayout.getHSizer(this, getChildren(), width);
		else
			return super.getVSizer(width);
	}

	@Override
	public void doLayout()
	{
		if(theLayout != null)
			theLayout.layout(this, getChildren(), new java.awt.Rectangle(0, 0, getWidth(),
				getHeight()));
		super.doLayout();
	}

	@Override
	public void addChild(WamElement child, int index)
	{
		super.addChild(child, index);
	}

	@Override
	public WamElement removeChild(int index)
	{
		return super.removeChild(index);
	}
}
