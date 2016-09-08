package org.quick.base.layout;

import org.observe.*;
import org.quick.core.QuickElement;
import org.quick.core.QuickLayout;
import org.quick.core.layout.LayoutGuideType;
import org.quick.core.layout.SizeGuide;
import org.quick.core.model.DocumentedElement;
import org.quick.core.model.QuickDocumentModel;
import org.quick.core.model.QuickDocumentModel.ContentChangeEvent;
import org.quick.core.model.QuickDocumentModel.StyleChangeEvent;
import org.quick.core.model.SelectableDocumentModel;
import org.quick.core.prop.QuickAttribute;
import org.quick.core.prop.QuickPropertyType;
import org.quick.core.style.QuickStyle;
import org.quick.util.CompoundListener;

/** Controls the location of the text inside a text-editing widget */
public class TextEditLayout implements QuickLayout {
	/** Allows the user to set the length (in characters) of a text-editing widget */
	public static final QuickAttribute<Integer> charLengthAtt = QuickAttribute.build("length", QuickPropertyType.integer).build();

	/** Allows the user to set the height (in characters) of a text-editing widget */
	public static final QuickAttribute<Integer> charRowsAtt = QuickAttribute.build("rows", QuickPropertyType.integer).build();

	private final CompoundListener theListener;

	private QuickElement theParent;
	private Subscription theDocListenSub;

	/** Creates the layout */
	public TextEditLayout() {
		theListener = CompoundListener.build()//
			.acceptAll(charLengthAtt, charRowsAtt).onEvent(CompoundListener.sizeNeedsChanged)//
			.child(childBuilder -> {
				childBuilder.watchAll(org.quick.core.style.FontStyle.getDomainInstance()).onEvent(CompoundListener.layout);
			})//
			.build();
	}

	@Override
	public void install(QuickElement parent, Observable<?> until) {
		if(theParent != null && theParent != parent)
			throw new IllegalArgumentException(getClass().getName() + " instances can only manage a single container");
		theParent = parent;
		parent.ch().onElement(el->{
			el.subscribe(new Observer<ObservableValueEvent<? extends QuickElement>>() {
				@Override
				public <V extends ObservableValueEvent<? extends QuickElement>> void onNext(V value) {
					// TODO Auto-generated method stub

				}

				@Override
				public <V extends ObservableValueEvent<? extends QuickElement>> void onCompleted(V value) {
					// TODO Auto-generated method stub

				}
			});
		});
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
		theListener.listen(parent, parent, until);
		QuickDocumentModel doc = ((DocumentedElement) children[0]).getDocumentModel().get();
		theDocListenSub = doc.changes().filter(evt -> evt instanceof ContentChangeEvent || evt instanceof StyleChangeEvent)
			.act(evt -> theParent.relayout(false));
	}

	@Override
	public void childAdded(QuickElement parent, QuickElement child) {
		if(!(child instanceof DocumentedElement)) {
			parent.msg().error(getClass().getSimpleName() + " requires the container's child to be a " + DocumentedElement.class.getName());
			return;
		}
		QuickDocumentModel doc = ((DocumentedElement) child).getDocumentModel().get();
		theDocListenSub = doc.changes().filter(evt -> evt instanceof ContentChangeEvent || evt instanceof StyleChangeEvent)
			.act(evt -> theParent.relayout(false));
	}

	private void childRemoved(QuickElement parent, QuickElement child) {
		if(!(child instanceof DocumentedElement)) {
			return;
		}
		if (theDocListenSub != null) {
			theDocListenSub.unsubscribe();
			theDocListenSub = null;
		}
	}

	@Override
	public void remove(QuickElement parent) {
		theListener.dropFor(parent);
		if (theParent == parent) {
			if (theDocListenSub != null) {
				theDocListenSub.unsubscribe();
				theDocListenSub = null;
			}
			theParent = null;
		}
	}

	@Override
	public SizeGuide getWSizer(final QuickElement parent, final QuickElement [] children) {
		if(children.length == 0)
			return new org.quick.core.layout.SimpleSizeGuide();
		if(children.length > 1) {
			parent.msg().error(getClass().getSimpleName() + " allows only one child in a container");
			return new org.quick.core.layout.SimpleSizeGuide();
		}
		if(!(children[0] instanceof DocumentedElement)) {
			parent.msg().error(getClass().getSimpleName() + " requires the container's child to be a " + DocumentedElement.class.getName());
			return new org.quick.core.layout.SimpleSizeGuide();
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
					final Integer length = parent.atts().get(charLengthAtt);
					if(length != null) {
						org.quick.core.model.QuickDocumentModel doc = ((DocumentedElement) children[0]).getDocumentModel().get();
						QuickStyle style;
						if(doc.length() > 0)
							style = doc.getStyleAt(0);
						else
							style = children[0].getStyle();
						java.awt.Font font = org.quick.util.QuickUtils.getFont(style).get();
						java.awt.font.FontRenderContext ctx = new java.awt.font.FontRenderContext(font.getTransform(),
							style.get(org.quick.core.style.FontStyle.antiAlias).get().booleanValue(), false);
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
	public SizeGuide getHSizer(final QuickElement parent, final QuickElement [] children) {
		if(children.length == 0)
			return new org.quick.core.layout.SimpleSizeGuide();
		if(children.length > 1) {
			parent.msg().error(getClass().getSimpleName() + " allows only one child in a container");
			return new org.quick.core.layout.SimpleSizeGuide();
		}
		if(!(children[0] instanceof DocumentedElement)) {
			parent.msg().error(getClass().getSimpleName() + " requires the container's child to be a " + DocumentedElement.class.getName());
			return new org.quick.core.layout.SimpleSizeGuide();
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
					final Integer rows = parent.atts().get(charRowsAtt);
					if(rows != null) {
						org.quick.core.model.QuickDocumentModel doc = ((DocumentedElement) children[0]).getDocumentModel().get();
						QuickStyle style;
						if(doc.length() > 0)
							style = doc.getStyleAt(0);
						else
							style = children[0].getStyle();
						java.awt.Font font = org.quick.util.QuickUtils.getFont(style).get();
						java.awt.font.FontRenderContext ctx = new java.awt.font.FontRenderContext(font.getTransform(), style
							.get(org.quick.core.style.FontStyle.antiAlias).get().booleanValue(), false);
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
	public void layout(QuickElement parent, QuickElement [] children) {
		if(children.length != 1) {
			parent.msg().error(getClass().getSimpleName() + " allows exactly one child in a container");
			return;
		}
		if(!(children[0] instanceof DocumentedElement)) {
			parent.msg().error(getClass().getSimpleName() + " requires the container's child to be a " + DocumentedElement.class.getName());
			return;
		}
		boolean wrap = children[0].getStyle().get(org.quick.core.style.FontStyle.wordWrap).get();
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
