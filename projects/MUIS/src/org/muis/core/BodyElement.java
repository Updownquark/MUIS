package org.muis.core;

/** The root element in a MUIS document */
public class BodyElement extends LayoutContainer
{
	/** Creates a body element */
	public BodyElement()
	{
		setFocusable(false);
		life().addListener(new LifeCycleListener() {
			@Override
			public void preTransition(String fromStage, String toStage)
			{
				if(toStage.equals(CoreStage.STARTUP.toString()))
				 if(getAttribute(LAYOUT_ATTR) == null)
						try
						{
						setAttribute(LAYOUT_ATTR, LayerLayout.class);
						} catch(MuisException e)
						{
							error("Could not set default layout in body element", e);
						}
			}

			@Override
			public void postTransition(String oldStage, String newStage)
			{
			}
		});
	}
}
