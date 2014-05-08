package org.muis.base.layout;

import org.muis.core.MuisElement;
import org.muis.core.MuisLayout;
import org.muis.core.layout.LayoutGuideType;
import org.muis.core.layout.SizeGuide;
import org.muis.core.model.DocumentedElement;
import org.muis.core.model.MuisDocumentModel;
import org.muis.core.model.SelectableDocumentModel;
import org.muis.util.CompoundListener;

/** Controls the location of the text inside a text-editing widget */
public class TextEditLayout implements MuisLayout {
	/** Allows the user to set the length (in characters) of a text-editing widget */
	public static final org.muis.core.MuisAttribute<Long> charLengthAtt = new org.muis.core.MuisAttribute<>("length",
		org.muis.core.MuisProperty.intAttr);

	/** Allows the user to set the height (in characters) of a text-editing widget */
	public static final org.muis.core.MuisAttribute<Long> charRowsAtt = new org.muis.core.MuisAttribute<>("rows",
		org.muis.core.MuisProperty.intAttr);

	private final CompoundListener.MultiElementCompoundListener theListener;

	private final MuisDocumentModel.ContentListener theContentListener;

	private final MuisDocumentModel.StyleListener theStyleListener;

	private MuisElement theParent;

	/** Creates the layout */
	public TextEditLayout() {
		theListener = CompoundListener.create(this);
		theListener.acceptAll(charLengthAtt, charRowsAtt).onChange(CompoundListener.sizeNeedsChanged);
		theListener.child().watchAll(org.muis.core.style.FontStyle.getDomainInstance()).onChange(CompoundListener.layout);
		theContentListener = evt -> {
			theParent.relayout(false);
		};
		theStyleListener = evt -> {
			theParent.relayout(false);
		};
	}

	@Override
	public void initChildren(MuisElement parent, MuisElement [] children) {
		if(theParent != null && theParent != parent)
			throw new IllegalArgumentException(getClass().getName() + " instances can only manage a single container");
		theParent = parent;
		if(children.length == 0) {
			theListener.listenerFor(parent);
			return;
		}
		if(children.length > 1) {
			parent.msg().error(getClass().getSimpleName() + " allows only one child in a container");
			return;
		}
		if(!(children[0] instanceof DocumentedElement)) {
			parent.msg().error(getClass().getSimpleName() + " requires the container's child to be a " + DocumentedElement.class.getName());
			return;
		}
		theListener.listenerFor(parent);
		MuisDocumentModel doc = ((DocumentedElement) children[0]).getDocumentModel();
		doc.addContentListener(theContentListener);
		doc.addStyleListener(theStyleListener);
	}

	@Override
	public void childAdded(MuisElement parent, MuisElement child) {
		if(!(child instanceof DocumentedElement)) {
			parent.msg().error(getClass().getSimpleName() + " requires the container's child to be a " + DocumentedElement.class.getName());
			return;
		}
		MuisDocumentModel doc = ((DocumentedElement) child).getDocumentModel();
		doc.addContentListener(theContentListener);
		doc.addStyleListener(theStyleListener);
	}

	@Override
	public void childRemoved(MuisElement parent, MuisElement child) {
		if(!(child instanceof DocumentedElement)) {
			return;
		}
		MuisDocumentModel doc = ((DocumentedElement) child).getDocumentModel();
		doc.removeContentListener(theContentListener);
		doc.addStyleListener(theStyleListener);
	}

	@Override
	public void remove(MuisElement parent) {
		theListener.dropFor(parent);
		if(theParent == parent)
			theParent = null;
	}

	@Override
	public SizeGuide getWSizer(final MuisElement parent, final MuisElement [] children) {
		if(children.length == 0)
			return new org.muis.core.layout.SimpleSizeGuide();
		if(children.length > 1) {
			parent.msg().error(getClass().getSimpleName() + " allows only one child in a container");
			return new org.muis.core.layout.SimpleSizeGuide();
		}
		if(!(children[0] instanceof DocumentedElement)) {
			parent.msg().error(getClass().getSimpleName() + " requires the container's child to be a " + DocumentedElement.class.getName());
			return new org.muis.core.layout.SimpleSizeGuide();
		}
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
				return get(LayoutGuideType.maxPref, crossSize, csMax);
			}

			@Override
			public int getMax(int crossSize, boolean csMax) {
				return get(LayoutGuideType.max, crossSize, csMax);
			}

			@Override
			public int get(LayoutGuideType type, int crossSize, boolean csMax) {
				if(type.isPref()) {
					final Long length = parent.atts().get(charLengthAtt);
					if(length != null) {
						org.muis.core.model.MuisDocumentModel doc = ((DocumentedElement) children[0]).getDocumentModel();
						org.muis.core.style.MuisStyle style;
						if(doc.length() > 0)
							style = doc.getStyleAt(0);
						else
							style = children[0].getStyle().getSelf();
						java.awt.Font font = org.muis.util.MuisUtils.getFont(style);
						java.awt.font.FontRenderContext ctx = new java.awt.font.FontRenderContext(font.getTransform(), style.get(
							org.muis.core.style.FontStyle.antiAlias).booleanValue(), false);
						return (int) (length * font.getStringBounds("00", ctx).getWidth() / 2);
					}
				}
				return children[0].bounds().getHorizontal().getGuide().get(type, crossSize, csMax);
			}

			@Override
			public int getBaseline(int size) {
				return children[0].bounds().getHorizontal().getGuide().getBaseline(size);
			}
		};
	}

	@Override
	public SizeGuide getHSizer(final MuisElement parent, final MuisElement [] children) {
		if(children.length == 0)
			return new org.muis.core.layout.SimpleSizeGuide();
		if(children.length > 1) {
			parent.msg().error(getClass().getSimpleName() + " allows only one child in a container");
			return new org.muis.core.layout.SimpleSizeGuide();
		}
		if(!(children[0] instanceof DocumentedElement)) {
			parent.msg().error(getClass().getSimpleName() + " requires the container's child to be a " + DocumentedElement.class.getName());
			return new org.muis.core.layout.SimpleSizeGuide();
		}
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
				return get(LayoutGuideType.maxPref, crossSize, csMax);
			}

			@Override
			public int getMax(int crossSize, boolean csMax) {
				return get(LayoutGuideType.max, crossSize, csMax);
			}

			@Override
			public int get(LayoutGuideType type, int crossSize, boolean csMax) {
				if(type.isPref()) {
					final Long rows = parent.atts().get(charRowsAtt);
					if(rows != null) {
						org.muis.core.model.MuisDocumentModel doc = ((DocumentedElement) children[0]).getDocumentModel();
						org.muis.core.style.MuisStyle style;
						if(doc.length() > 0)
							style = doc.getStyleAt(0);
						else
							style = children[0].getStyle().getSelf();
						java.awt.Font font = org.muis.util.MuisUtils.getFont(style);
						java.awt.font.FontRenderContext ctx = new java.awt.font.FontRenderContext(font.getTransform(), style.get(
							org.muis.core.style.FontStyle.antiAlias).booleanValue(), false);
						return (int) (rows * font.getStringBounds("00", ctx).getHeight());
					}
				}
				return children[0].bounds().getVertical().getGuide().get(type, crossSize, csMax);
			}

			@Override
			public int getBaseline(int size) {
				return children[0].bounds().getVertical().getGuide().getBaseline(size);
			}
		};
	}

	@Override
	public void layout(MuisElement parent, MuisElement [] children) {
		if(children.length != 1) {
			parent.msg().error(getClass().getSimpleName() + " allows exactly one child in a container");
			return;
		}
		if(!(children[0] instanceof DocumentedElement)) {
			parent.msg().error(getClass().getSimpleName() + " requires the container's child to be a " + DocumentedElement.class.getName());
			return;
		}
		boolean wrap = children[0].getStyle().getSelf().get(org.muis.core.style.FontStyle.wordWrap);
		int w = wrap ? parent.bounds().getWidth() : children[0].bounds().getHorizontal().getGuide().getPreferred(Integer.MAX_VALUE, true);
		int h = children[0].bounds().getVertical().getGuide().getPreferred(w, !wrap);

		DocumentedElement child = (DocumentedElement) children[0];
		if(!(child.getDocumentModel() instanceof SelectableDocumentModel)) {
			children[0].bounds().setBounds(0, 0, w, h);
			return;
		}
		SelectableDocumentModel doc = (SelectableDocumentModel) child.getDocumentModel();
		java.awt.geom.Point2D loc = doc.getLocationAt(doc.getCursor(), Integer.MAX_VALUE);
		int x = children[0].bounds().getX();
		if(w <= parent.bounds().getWidth())
			x = 0;
		else {
			if(loc.getX() < -x)
				x = -(int) Math.ceil(loc.getX());
			else if(loc.getX() > -x + parent.bounds().getWidth())
				x = -(int) Math.ceil(loc.getX()) + parent.bounds().getWidth();
			if(x > 0)
				x = 0;
			else if(x + w < parent.bounds().getWidth())
				x = parent.bounds().getWidth() - w;
		}

		int y = children[0].bounds().getY();
		if(h <= parent.bounds().getHeight())
			y = 0;
		else {
			if(loc.getY() < -y)
				y = -(int) Math.ceil(loc.getY());
			else if(loc.getY() > -y + parent.bounds().getHeight())
				y = -(int) Math.ceil(loc.getY()) + parent.bounds().getHeight();
			if(y > 0)
				y = 0;
			else if(y + h < parent.bounds().getHeight())
				y = parent.bounds().getHeight() - h;
		}

		if(w < parent.bounds().getWidth())
			w = parent.bounds().getWidth();
		if(h < parent.bounds().getHeight())
			h = parent.bounds().getHeight();

		children[0].bounds().setBounds(x, y, w, h);
	}
}
