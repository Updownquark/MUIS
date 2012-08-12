package org.muis.base.layout;

import static org.muis.base.layout.LayoutConstants.*;

import org.muis.core.MuisAttribute;
import org.muis.core.MuisElement;
import org.muis.core.event.MuisEvent;
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
				listener.chain(Region.left.name()).setActive(element.getAttribute(region) == Region.left);
				listener.chain(Region.right.name()).setActive(element.getAttribute(region) == Region.right);
				listener.chain(Region.top.name()).setActive(element.getAttribute(region) == Region.top);
				listener.chain(Region.bottom.name()).setActive(element.getAttribute(region) == Region.bottom);
			}
		});
	}

	private class RelayoutListener implements org.muis.core.event.MuisEventListener<MuisAttribute<?>> {
		private final MuisElement theParent;

		RelayoutListener(MuisElement parent) {
			theParent = parent;
		}

		@Override
		public void eventOccurred(MuisEvent<MuisAttribute<?>> event, MuisElement element) {
			MuisAttribute<?> attr = event.getValue();
			if(attr == LayoutConstants.region) {
				switch (element.getAttribute(LayoutConstants.region)) {
				case left:
					element.rejectAttribute(BorderLayout.this, LayoutConstants.left);
					element.rejectAttribute(BorderLayout.this, LayoutConstants.top);
					element.rejectAttribute(BorderLayout.this, LayoutConstants.bottom);
					element.acceptAttribute(BorderLayout.this, LayoutConstants.right);
					break;
				case top:
					element.rejectAttribute(BorderLayout.this, LayoutConstants.left);
					element.rejectAttribute(BorderLayout.this, LayoutConstants.top);
					element.rejectAttribute(BorderLayout.this, LayoutConstants.right);
					element.acceptAttribute(BorderLayout.this, LayoutConstants.bottom);
					break;
				case right:
					element.rejectAttribute(BorderLayout.this, LayoutConstants.right);
					element.rejectAttribute(BorderLayout.this, LayoutConstants.top);
					element.rejectAttribute(BorderLayout.this, LayoutConstants.bottom);
					element.acceptAttribute(BorderLayout.this, LayoutConstants.left);
					break;
				case bottom:
					element.rejectAttribute(BorderLayout.this, LayoutConstants.left);
					element.rejectAttribute(BorderLayout.this, LayoutConstants.bottom);
					element.rejectAttribute(BorderLayout.this, LayoutConstants.right);
					element.acceptAttribute(BorderLayout.this, LayoutConstants.top);
					break;
				case center:
					element.rejectAttribute(BorderLayout.this, LayoutConstants.left);
					element.rejectAttribute(BorderLayout.this, LayoutConstants.right);
					element.rejectAttribute(BorderLayout.this, LayoutConstants.top);
					element.rejectAttribute(BorderLayout.this, LayoutConstants.bottom);
					break;
				}
			}
			if(attr == LayoutConstants.left || attr == LayoutConstants.right || attr == LayoutConstants.top
				|| attr == LayoutConstants.bottom)
				theParent.relayout(false);
		}

		@Override
		public boolean isLocal() {
			return true;
		}
	}

	@Override
	public void initChildren(MuisElement parent, MuisElement [] children) {
		RelayoutListener listener = new RelayoutListener(parent);
		for(MuisElement child : children) {
			child.addListener(MuisElement.ATTRIBUTE_SET, listener);
		}
	}

	@Override
	public void childAdded(MuisElement parent, MuisElement child) {
		child.addListener(MuisElement.ATTRIBUTE_SET, new RelayoutListener(parent));
	}

	@Override
	public void childRemoved(MuisElement parent, MuisElement child) {
		child.removeListener(MuisElement.ATTRIBUTE_SET, RelayoutListener.class);
		child.rejectAttribute(this, LayoutConstants.left);
		child.rejectAttribute(this, LayoutConstants.right);
		child.rejectAttribute(this, LayoutConstants.top);
		child.rejectAttribute(this, LayoutConstants.bottom);
	}

	@Override
	public SizePolicy getWSizer(MuisElement parent, MuisElement [] children, int parentHeight) {

		SimpleSizePolicy ret = new SimpleSizePolicy();
		for(MuisElement child : children) {
			Position pos;
			Size size = child.getAttribute(LayoutConstants.width);
			Size minSize = child.getAttribute(LayoutConstants.minWidth);
			SizePolicy sizer;
			switch (child.getAttribute(LayoutConstants.region)) {
			case left:
				pos = child.getAttribute(LayoutConstants.right);
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
				pos = child.getAttribute(LayoutConstants.right);
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
