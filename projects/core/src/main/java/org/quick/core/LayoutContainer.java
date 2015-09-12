package org.quick.core;

import org.quick.core.layout.SizeGuide;

/** A simple container element that lays its children out using an implementation of {@link QuickLayout} */
public class LayoutContainer extends QuickElement {
	/** The attribute that specifies the layout type for a layout container */
	public static QuickAttribute<QuickLayout> LAYOUT_ATTR = new QuickAttribute<>("layout", new QuickProperty.QuickTypeInstanceProperty<>(
		QuickLayout.class));

	/** Creates a layout container */
	public LayoutContainer() {
		QuickLayout defLayout = getDefaultLayout();
		life().runWhen(() -> {
			try {
				atts().require(this, LAYOUT_ATTR, defLayout).act(event -> {
					if(event.getOldValue() != null)
						event.getOldValue().remove(this);
					if(event.getValue() != null)
						event.getValue().initChildren(this, ch().toArray());
					relayout(false);
				});
			} catch(QuickException e) {
				msg().error("Could not set default layout", e, "layout", defLayout);
			}
		}, QuickConstants.CoreStage.INIT_CHILDREN.toString(), 1);
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
		if(layout != null)
			return layout.getWSizer(this, getChildren().toArray());
		else
			return super.getWSizer();
	}

	@Override
	public SizeGuide getHSizer() {
		QuickLayout layout = getLayout();
		if(layout != null)
			return layout.getHSizer(this, getChildren().toArray());
		else
			return super.getHSizer();
	}

	@Override
	public void doLayout() {
		QuickLayout layout = getLayout();
		if(layout != null)
			layout.layout(this, getChildren().toArray());
		super.doLayout();
	}

	@Override
	protected void registerChild(QuickElement child) {
		super.registerChild(child);
		QuickLayout layout = getLayout();
		if(layout != null)
			layout.childAdded(this, child);
	}

	@Override
	protected void unregisterChild(QuickElement child) {
		super.unregisterChild(child);
		QuickLayout layout = getLayout();
		if(layout != null)
			layout.childRemoved(this, child);
	}
}
