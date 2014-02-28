package org.muis.base.layout;

import static org.muis.core.layout.LayoutAttributes.*;

import org.muis.core.MuisElement;
import org.muis.core.MuisLayout;
import org.muis.core.layout.*;
import org.muis.core.style.Position;
import org.muis.util.CompoundListener;

/**
 * A very simple layout that positions and sizes children independently using positional ({@link LayoutAttributes#left},
 * {@link LayoutAttributes#right}, {@link LayoutAttributes#top}, {@link LayoutAttributes#bottom}) and size ({@link LayoutAttributes#width}
 * and {@link LayoutAttributes#height}) attributes or sizers.
 */
public class SimpleLayout implements MuisLayout {
	private final CompoundListener.MultiElementCompoundListener theListener;

	/** Creates a simple layout */
	public SimpleLayout() {
		theListener = CompoundListener.create(this);
		theListener.child().acceptAll(left, right, top, bottom, width, height, minWidth, maxWidth, minHeight, maxHeight)
			.onChange(CompoundListener.sizeNeedsChanged);
	}

	@Override
	public void initChildren(MuisElement parent, MuisElement [] children) {
		theListener.listenerFor(parent);
	}

	@Override
	public void remove(MuisElement parent) {
		theListener.dropFor(parent);
	}

	@Override
	public void childAdded(MuisElement parent, MuisElement child) {
	}

	@Override
	public void childRemoved(MuisElement parent, MuisElement child) {
	}

	@Override
	public SizeGuide getWSizer(MuisElement parent, MuisElement [] children) {
		return getSizer(children, Orientation.horizontal);
	}

	@Override
	public SizeGuide getHSizer(MuisElement parent, MuisElement [] children) {
		return getSizer(children, Orientation.vertical);
	}

	/**
	 * Gets a sizer for a container in one dimension
	 *
	 * @param children The children to lay out
	 * @param orient The orientation to get the sizer for
	 * @return The size policy for the container of the given children in the given dimension
	 */
	protected SizeGuide getSizer(final MuisElement [] children, final Orientation orient) {
		return new SizeGuide() {
			@Override
			public int getMin(int crossSize, boolean csMax) {
				return get(LayoutGuideType.min, crossSize, csMax);
			}

			@Override
			public int getMinPreferred(int crossSize, boolean csMax) {
				return get(LayoutGuideType.minPref, crossSize, csMax);
			}

			@Override
			public int getPreferred(int crossSize, boolean csMax) {
				return get(LayoutGuideType.pref, crossSize, csMax);
			}

			@Override
			public int getMaxPreferred(int crossSize, boolean csMax) {
				return Integer.MAX_VALUE; // Don't try to limit the container size due to the contents
				// return get(LayoutGuideType.maxPref, crossSize, csMax);
			}

			@Override
			public int getMax(int crossSize, boolean csMax) {
				return Integer.MAX_VALUE; // Don't try to limit the container size due to the contents
				// return get(LayoutGuideType.max, crossSize, csMax);
			}

			@Override
			public int get(LayoutGuideType type, int crossSize, boolean csMax) {
				int maximum = 0;
				boolean maxChange = true;
				int iterations = 5;
				while(maxChange && iterations > 0) {
					maxChange = false;
					iterations--;
					for(MuisElement child : children) {
						int size = LayoutUtils.getSize(child, orient, type, maximum, crossSize, csMax, null);
						Position pos = child.atts().get(LayoutAttributes.getPosAtt(orient, End.leading, null));
						if(pos != null) {
							if(pos.getUnit() == org.muis.core.style.LengthUnit.lexips)
								size += Math.round(pos.getValue());
							else
								size += pos.evaluate(maximum);
						}
						pos = child.atts().get(LayoutAttributes.getPosAtt(orient, End.trailing, null));
						if(pos != null) {
							if(pos.getUnit() == org.muis.core.style.LengthUnit.lexips)
								size += Math.round(pos.getValue());
							else
								size += pos.evaluate(maximum);
						}
						if(size > maximum) {
							maximum = size;
							maxChange = true;
						}
					}
				}
				return maximum;
			}

			@Override
			public int getBaseline(int size) {
				if(children.length == 0)
					return 0;
				for(MuisElement child : children) {
					int childSize = LayoutUtils.getSize(child, orient, LayoutGuideType.pref, size, Integer.MAX_VALUE, true, null);
					int ret = child.bounds().get(orient).getGuide().getBaseline(childSize);
					if(ret < 0)
						continue;
					Position pos = children[0].atts().get(LayoutAttributes.getPosAtt(orient, End.leading, null));
					if(pos != null) {
						return ret + pos.evaluate(size);
					}
					pos = children[0].atts().get(LayoutAttributes.getPosAtt(orient, End.trailing, null));
					if(pos != null) {
						return size - pos.evaluate(size) - childSize + ret;
					}
					return ret;
				}
				return -1;
			}
		};
	}

	@Override
	public void layout(MuisElement parent, MuisElement [] children) {
		for(MuisElement child : children)
			layout(parent, child);
	}

	private void layout(MuisElement parent, MuisElement child) {
		Position pos1 = child.atts().get(LayoutAttributes.left);
		Position pos2 = child.atts().get(LayoutAttributes.right);
		int x, w;
		if(pos1 != null) {
			x = pos1.evaluate(parent.bounds().getWidth());
			if(pos2 != null)
				w = pos2.evaluate(parent.bounds().getWidth()) - x;
			else
				w = LayoutUtils.getSize(child, Orientation.horizontal, LayoutGuideType.pref, parent.bounds().getWidth(), parent.bounds()
					.getHeight(), false, null);
		} else if(pos2 != null) {
			w = LayoutUtils.getSize(child, Orientation.horizontal, LayoutGuideType.pref, parent.bounds().getWidth(), parent.bounds()
				.getHeight(), false, null);
			x = pos2.evaluate(parent.bounds().getWidth()) - w;
		} else {
			x = 0;
			w = LayoutUtils.getSize(child, Orientation.horizontal, LayoutGuideType.pref, parent.bounds().getWidth(), parent.bounds()
				.getHeight(), false, null);
		}

		pos1 = child.atts().get(LayoutAttributes.top);
		pos2 = child.atts().get(LayoutAttributes.bottom);
		int y, h;
		if(pos1 != null) {
			y = pos1.evaluate(parent.bounds().getHeight());
			if(pos2 != null) {
				h = pos2.evaluate(parent.bounds().getHeight()) - y;
			} else
				h = LayoutUtils.getSize(child, Orientation.vertical, LayoutGuideType.pref, parent.bounds().getHeight(), parent.bounds()
					.getWidth(), false, null);
		} else if(pos2 != null) {
			h = LayoutUtils.getSize(child, Orientation.vertical, LayoutGuideType.pref, parent.bounds().getHeight(), parent.bounds()
				.getWidth(), false, null);
			y = pos2.evaluate(parent.bounds().getHeight()) - h;
		} else {
			y = 0;
			h = LayoutUtils.getSize(child, Orientation.vertical, LayoutGuideType.pref, parent.bounds().getHeight(), parent.bounds()
				.getWidth(), false, null);
		}
		child.bounds().setBounds(x, y, w, h);
	}
}
