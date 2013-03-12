package org.muis.base.layout;

import static org.muis.core.layout.LayoutAttributes.*;

import org.muis.core.MuisElement;
import org.muis.core.layout.LayoutAttributes;
import org.muis.core.layout.Region;
import org.muis.core.layout.SimpleSizePolicy;
import org.muis.core.layout.SizePolicy;
import org.muis.core.style.Position;
import org.muis.core.style.Size;
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
	public SizePolicy getWSizer(MuisElement parent, MuisElement [] children, int parentHeight) {

		SimpleSizePolicy ret = new SimpleSizePolicy();
		for(MuisElement child : children) {
			Position pos;
			Size size = child.atts().get(LayoutAttributes.width);
			Size minSize = child.atts().get(LayoutAttributes.minWidth);
			SizePolicy sizer;
			switch (child.atts().get(LayoutAttributes.region)) {
			case left:
				pos = child.atts().get(LayoutAttributes.right);
				if(pos != null && !pos.getUnit().isRelative()) {
					ret.setMin(ret.getMin() + pos.evaluate(0));
					ret.setPreferred(ret.getPreferred() + pos.evaluate(0));
				}
				else if(size != null && !size.getUnit().isRelative()) {
					ret.setMin(ret.getMin() + size.evaluate(0));
					ret.setPreferred(ret.getPreferred() + size.evaluate(0));
				}
				else {
					sizer = child.getWSizer(parentHeight);
					ret.setMin(ret.getMin() + sizer.getMin());
					ret.setPreferred(ret.getPreferred() + sizer.getPreferred());
				}
				break;
			case right:
				pos = child.atts().get(LayoutAttributes.right);
				if(pos != null && !pos.getUnit().isRelative()) {
					ret.setMin(ret.getMin() + pos.evaluate(0));
					ret.setPreferred(ret.getPreferred() + pos.evaluate(0));
				}
				else if(size != null && !size.getUnit().isRelative()) {
					ret.setMin(ret.getMin() + size.evaluate(0));
					ret.setPreferred(ret.getPreferred() + size.evaluate(0));
				}
				else {
					sizer = child.getWSizer(parentHeight);
					ret.setMin(ret.getMin() + sizer.getMin());
					ret.setPreferred(ret.getPreferred() + sizer.getPreferred());
				}
				break;
			case top:
				if(size != null && !size.getUnit().isRelative()) {
					ret.setMin(ret.getMin() + size.evaluate(0));
					ret.setPreferred(ret.getPreferred() + size.evaluate(0));
				}
				else {
					sizer = child.getWSizer(parentHeight);
					ret.setMin(ret.getMin() + sizer.getMin());
					ret.setPreferred(ret.getPreferred() + sizer.getPreferred());
				}
				break;
			case bottom:
			case center:
			}
		}
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SizePolicy getHSizer(MuisElement parent, MuisElement [] children, int parentWidth) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void layout(MuisElement parent, MuisElement [] children) {
		// TODO Auto-generated method stub

	}

	@Override
	public void remove(MuisElement parent) {
		for(MuisElement child : parent.getChildren())
			childRemoved(parent, child);
	}
}
