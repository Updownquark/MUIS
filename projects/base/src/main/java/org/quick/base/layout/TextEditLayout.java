package org.quick.base.layout;

import org.observe.Observable;
import org.observe.ObservableValueEvent;
import org.observe.Observer;
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
		parent.ch().onElement(el->{
			el.subscribe(new Observer<ObservableValueEvent<? extends QuickElement>>() {
				@Override
				public <V extends ObservableValueEvent<? extends QuickElement>> void onNext(V value) {
					childAdded(parent, value.getValue(), Observable.or(until, el.noInit()));
				}
			});
		});
		theListener.listen(parent, parent, until);
	}

	private void childAdded(QuickElement parent, QuickElement child, Observable<?> until) {
		if (child instanceof DocumentedElement) {
			QuickDocumentModel doc = ((DocumentedElement) child).getDocumentModel().get();
			doc.changes().takeUntil(until).filter(evt -> evt instanceof ContentChangeEvent || evt instanceof StyleChangeEvent)
				.act(evt -> parent.relayout(false));
		} else
			parent.msg().error(getClass().getSimpleName() + " requires the container's child to be a " + DocumentedElement.class.getName());
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
			public int getMin(int crossSize) {
				return get(LayoutGuideType.min, crossSize);
			}

			@Override
			public int getMinPreferred(int crossSize) {
				return get(LayoutGuideType.minPref, crossSize);
			}

			@Override
			public int getPreferred(int crossSize) {
				return get(LayoutGuideType.pref, crossSize);
			}

			@Override
			public int getMaxPreferred(int crossSize) {
				return get(LayoutGuideType.maxPref, crossSize);
			}

			@Override
			public int getMax(int crossSize) {
				return get(LayoutGuideType.max, crossSize);
			}

			@Override
			public int get(LayoutGuideType type, int crossSize) {
				if(type.isPref()) {
					final int length = parent.atts().get(charLengthAtt, 12);
					org.quick.core.model.QuickDocumentModel doc = ((DocumentedElement) children[0]).getDocumentModel().get();
					QuickStyle style;
					if (doc.length() > 0)
						style = doc.getStyleAt(0);
					else
						style = children[0].getStyle();
					java.awt.Font font = org.quick.util.QuickUtils.getFont(style).get();
					java.awt.font.FontRenderContext ctx = new java.awt.font.FontRenderContext(font.getTransform(),
						style.get(org.quick.core.style.FontStyle.antiAlias).get().booleanValue(), false);
					return (int) (length * font.getStringBounds("00", ctx).getWidth() / 2);
				}
				return children[0].bounds().getHorizontal().getGuide().get(type, crossSize);
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
			public int getMin(int crossSize) {
				return get(LayoutGuideType.min, crossSize);
			}

			@Override
			public int getMinPreferred(int crossSize) {
				return get(LayoutGuideType.minPref, crossSize);
			}

			@Override
			public int getPreferred(int crossSize) {
				return get(LayoutGuideType.pref, crossSize);
			}

			@Override
			public int getMaxPreferred(int crossSize) {
				return get(LayoutGuideType.maxPref, crossSize);
			}

			@Override
			public int getMax(int crossSize) {
				return get(LayoutGuideType.max, crossSize);
			}

			@Override
			public int get(LayoutGuideType type, int crossSize) {
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
				return children[0].bounds().getVertical().getGuide().get(type, crossSize);
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
		int w = parent.bounds().getWidth();
		int h = children[0].bounds().getVertical().getGuide().getPreferred(w);

		DocumentedElement child = (DocumentedElement) children[0];
		if (!(child.getDocumentModel().get() instanceof SelectableDocumentModel)) {
			children[0].bounds().setBounds(0, 0, w, h);
			return;
		}
		SelectableDocumentModel doc = (SelectableDocumentModel) child.getDocumentModel().get();
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

	@Override
	public String toString() {
		return "text-edit-layout";
	}
}
