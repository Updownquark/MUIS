package org.muis.core;

import org.muis.core.layout.SizeGuide;

/** A simple container element that lays its children out using an implementation of {@link MuisLayout} */
public class LayoutContainer extends MuisElement {
	/** The attribute that specifies the layout type for a layout container */
	public static MuisAttribute<MuisLayout> LAYOUT_ATTR = new MuisAttribute<>("layout", new MuisProperty.MuisTypeInstanceProperty<>(
		MuisLayout.class));

	/** Creates a layout container */
	public LayoutContainer() {
		MuisLayout defLayout = getDefaultLayout();
		life().runWhen(() -> {
			try {
				atts().require(this, LAYOUT_ATTR, defLayout).act(event -> {
					if(event.getOldValue() != null)
						event.getOldValue().remove(this);
					if(event.getValue() != null)
						event.getValue().initChildren(this, ch().toArray());
					relayout(false);
				});
			} catch(MuisException e) {
				msg().error("Could not set default layout", e, "layout", defLayout);
			}
		}, MuisConstants.CoreStage.INIT_CHILDREN.toString(), 1);
	}

	/**
	 * Allows types to specify their default layout
	 *
	 * @return The default layout for this container. Null by default.
	 */
	protected MuisLayout getDefaultLayout() {
		return null;
	}

	/** @return The MuisLayout that lays out this container's children */
	public MuisLayout getLayout() {
		return atts().get(LAYOUT_ATTR);
	}

	@Override
	public SizeGuide getWSizer() {
		MuisLayout layout = getLayout();
		if(layout != null)
			return layout.getWSizer(this, getChildren().toArray());
		else
			return super.getWSizer();
	}

	@Override
	public SizeGuide getHSizer() {
		MuisLayout layout = getLayout();
		if(layout != null)
			return layout.getHSizer(this, getChildren().toArray());
		else
			return super.getHSizer();
	}

	@Override
	public void doLayout() {
		MuisLayout layout = getLayout();
		if(layout != null)
			layout.layout(this, getChildren().toArray());
		super.doLayout();
	}

	@Override
	protected void registerChild(MuisElement child) {
		super.registerChild(child);
		MuisLayout layout = getLayout();
		if(layout != null)
			layout.childAdded(this, child);
	}

	@Override
	protected void unregisterChild(MuisElement child) {
		super.unregisterChild(child);
		MuisLayout layout = getLayout();
		if(layout != null)
			layout.childRemoved(this, child);
	}
}
