package org.muis.base.layout;

import static org.muis.core.layout.LayoutAttributes.*;

import org.muis.core.MuisElement;
import org.muis.core.layout.Region;
import org.muis.core.layout.SizeGuide;
import org.muis.util.CompoundListener;
import org.muis.util.CompoundListener.CompoundElementListener;

/**
 * Lays components out by {@link Region regions}. Containers with this layout may have any number of components in any region except center,
 * which may have zero or one component in it.
 */
public class BorderLayout implements org.muis.core.MuisLayout {
	private final CompoundListener.MultiElementCompoundListener theListener;

	/** Creates a border layout */
	public BorderLayout() {
		theListener = CompoundListener.create(this);
		theListener.child().accept(region).onChange(theListener.individualChecker(false)).onChange(CompoundListener.layout);
		theListener.eachChild(new CompoundListener.IndividualElementListener() {
			@Override
			public void individual(MuisElement element, CompoundElementListener listener) {
				listener.chain(Region.left.name()).acceptAll(width, minWidth, maxWidth, right, minRight, maxRight)
					.onChange(CompoundListener.layout);
				listener.chain(Region.right.name()).acceptAll(width, minWidth, maxWidth, left, minLeft, maxLeft)
					.onChange(CompoundListener.layout);
				listener.chain(Region.top.name()).acceptAll(height, minHeight, maxHeight, bottom, minBottom, maxBottom)
					.onChange(CompoundListener.layout);
				listener.chain(Region.bottom.name()).acceptAll(height, minHeight, maxHeight, top, minTop, maxTop)
					.onChange(CompoundListener.layout);
				update(element, listener);
			}

			@Override
			public void update(MuisElement element, CompoundElementListener listener) {
				listener.chain(Region.left.name()).setActive(element.atts().get(region) == Region.left);
				listener.chain(Region.right.name()).setActive(element.atts().get(region) == Region.right);
				listener.chain(Region.top.name()).setActive(element.atts().get(region) == Region.top);
				listener.chain(Region.bottom.name()).setActive(element.atts().get(region) == Region.bottom);
			}
		});
	}

	@Override
	public void initChildren(MuisElement parent, MuisElement [] children) {
		theListener.listenerFor(parent);
	}

	@Override
	public void childAdded(MuisElement parent, MuisElement child) {
	}

	@Override
	public void childRemoved(MuisElement parent, MuisElement child) {
	}

	@Override
	public void remove(MuisElement parent) {
		theListener.dropFor(parent);
	}

	@Override
	public SizeGuide getWSizer(MuisElement parent, MuisElement [] children) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SizeGuide getHSizer(MuisElement parent, MuisElement [] children) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void layout(MuisElement parent, MuisElement [] children) {
		// TODO Auto-generated method stub

	}
}
