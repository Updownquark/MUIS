package org.muis.core;

import org.muis.core.event.MuisEvent;
import org.muis.layout.SizePolicy;

/** A simple container element that lays its children out using an implementation of {@link MuisLayout} */
public class LayoutContainer extends MuisElement implements MuisContainer
{
	/** The attribute that specifies the layout type for a layout container */
	public static MuisAttribute<Class<? extends MuisLayout>> LAYOUT_ATTR = new MuisAttribute<Class<? extends MuisLayout>>("layout",
		new MuisAttribute.MuisTypeAttribute<MuisLayout>(MuisLayout.class));

	private MuisLayout theLayout;

	@Override
	public void initChildren(MuisElement [] children)
	{
		super.initChildren(children);
		addListener(ATTRIBUTE_SET, new org.muis.core.event.MuisEventListener<MuisAttribute<?>>() {
			@Override
			public void eventOccurred(MuisEvent<MuisAttribute<?>> event, MuisElement element)
			{
				if(event.getValue() != LAYOUT_ATTR)
					return;
				Class<? extends MuisLayout> layoutClass = getAttribute(LAYOUT_ATTR);
				if(layoutClass == null)
					setLayout(null);
				else
				{
					MuisLayout layout;
					try
					{
						layout = layoutClass.newInstance();
					} catch(InstantiationException | IllegalAccessException e)
					{
						error("Could not instantiate layout of type " + layoutClass.getName(), e);
						return;
					}
					setLayout(layout);
				}
			}

			@Override
			public boolean isLocal()
			{
				return true;
			}
		});
	}

	@Override
	protected void postInit()
	{
		super.postInit();
		requireAttribute(LAYOUT_ATTR);
	}

	@Override
	public void postCreate()
	{
		super.postCreate();
	}

	/** @return The MuisLayout that lays out this container's children */
	public MuisLayout getLayout()
	{
		return theLayout;
	}

	/** @param layout The MuisLayout to lay out this container's children */
	public void setLayout(MuisLayout layout)
	{
		if(theLayout != null)
			theLayout.remove(this);
		theLayout = layout;
		if(theLayout != null)
			theLayout.initChildren(this, getChildren());
	}

	@Override
	public SizePolicy getWSizer(int height)
	{
		if(theLayout != null)
			return theLayout.getWSizer(this, getChildren(), height);
		else
			return super.getWSizer(height);
	}

	@Override
	public SizePolicy getHSizer(int width)
	{
		if(theLayout != null)
			return theLayout.getHSizer(this, getChildren(), width);
		else
			return super.getHSizer(width);
	}

	@Override
	public void doLayout()
	{
		if(theLayout != null)
			theLayout.layout(this, getChildren());
		super.doLayout();
	}

	@Override
	public void addChild(MuisElement child, int index)
	{
		super.addChild(child, index);
	}

	@Override
	public MuisElement removeChild(int index)
	{
		return super.removeChild(index);
	}
}
