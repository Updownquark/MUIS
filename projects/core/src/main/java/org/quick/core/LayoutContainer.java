package org.quick.core;

import org.quick.core.layout.SizeGuide;
import org.quick.core.mgr.AttributeManager.AttributeHolder;
import org.quick.core.prop.QuickAttribute;
import org.quick.core.prop.QuickPropertyType;

/** A simple container element that lays its children out using an implementation of {@link QuickLayout} */
public class LayoutContainer extends QuickElement {
	/** The attribute that specifies the layout type for a layout container */
	public static QuickAttribute<QuickLayout> LAYOUT_ATTR = QuickAttribute
		.build("layout", QuickPropertyType.forTypeInstance(QuickLayout.class, null)).build();

	/** Creates a layout container */
	public LayoutContainer() {
		QuickLayout defLayout = getDefaultLayout();
		AttributeHolder<QuickLayout> layoutAtt;
		try {
			layoutAtt = atts().require(this, LAYOUT_ATTR, defLayout);
			life().runWhen(() -> {
				layoutAtt.value().act(layout -> layout.install(LayoutContainer.this, layoutAtt.noInit()));
			}, QuickConstants.CoreStage.STARTUP.toString(), -1);
		} catch (QuickException e) {
			msg().error("Could not set default layout", e, "layout", defLayout);
		}
	}

	/**
	 * Allows types to specify their default layout
	 *
	 * @return The default layout for this container. Null by default.
	 */
	protected QuickLayout getDefaultLayout() {
		return null;
	}

	/** @return The QuickLayout that lays out this container's children */
	public QuickLayout getLayout() {
		return atts().get(LAYOUT_ATTR);
	}

	@Override
	public SizeGuide getWSizer() {
		QuickLayout layout = getLayout();
		if (layout != null)
			return layout.getWSizer(this, getPhysicalChildren().toArray());
		else
			return super.getWSizer();
	}

	@Override
	public SizeGuide getHSizer() {
		QuickLayout layout = getLayout();
		if (layout != null)
			return layout.getHSizer(this, getPhysicalChildren().toArray());
		else
			return super.getHSizer();
	}

	@Override
	public void doLayout() {
		QuickLayout layout = getLayout();
		if (layout != null)
			layout.layout(this, getPhysicalChildren().toArray());
		super.doLayout();
	}
}
