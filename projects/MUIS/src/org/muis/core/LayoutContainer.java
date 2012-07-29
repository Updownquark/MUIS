package org.muis.core;

import org.muis.core.event.MuisEvent;
import org.muis.core.layout.SizePolicy;

/** A simple container element that lays its children out using an implementation of {@link MuisLayout} */
public class LayoutContainer extends MuisElement implements MuisContainer
{
	/** The attribute that specifies the layout type for a layout container */
	public static MuisAttribute<MuisLayout> LAYOUT_ATTR = new MuisAttribute<MuisLayout>("layout",
		new MuisAttribute.MuisTypeInstanceAttribute<MuisLayout>(MuisLayout.class));

	private MuisLayout theLayout;

	/** Creates a layout container */
	public LayoutContainer()
	{
		MuisLayout defLayout = getDefaultLayout();
		try
		{
			requireAttribute(this, LAYOUT_ATTR, defLayout);
		} catch(MuisException e)
		{
			error("Could not set default layout", e, "layout", defLayout);
		}
	}

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
				MuisLayout layout = getAttribute(LAYOUT_ATTR);
				setLayout(layout);
			}

			@Override
			public boolean isLocal()
			{
				return true;
			}
		});
		setLayout(getAttribute(LAYOUT_ATTR));
	}

	/**
	 * Allows types to specify their default layout
	 *
	 * @return The default layout for this container. Null by default.
	 */
	protected MuisLayout getDefaultLayout()
	{
		return null;
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
		if(theLayout != null)
			theLayout.childAdded(this, child);
	}

	@Override
	public MuisElement removeChild(int index)
	{
		MuisElement ret = super.removeChild(index);
		if(theLayout != null)
			theLayout.childRemoved(this, ret);
		return ret;
	}
}
