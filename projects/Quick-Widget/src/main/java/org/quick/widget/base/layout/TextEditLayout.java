package org.quick.widget.base.layout;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.observe.Observable;
import org.observe.SimpleObservable;
import org.qommons.Transaction;
import org.qommons.collect.CollectionElement;
import org.qommons.collect.ElementId;
import org.quick.base.widget.TextField;
import org.quick.core.layout.LayoutGuideType;
import org.quick.core.layout.Orientation;
import org.quick.core.model.QuickDocumentModel;
import org.quick.core.model.QuickDocumentModel.ContentChangeEvent;
import org.quick.core.model.QuickDocumentModel.StyleChangeEvent;
import org.quick.core.model.SelectableDocumentModel;
import org.quick.core.style.FontStyle;
import org.quick.core.style.QuickStyle;
import org.quick.util.CompoundListener;
import org.quick.util.QuickUtils;
import org.quick.widget.core.QuickWidget;
import org.quick.widget.core.layout.QuickWidgetLayout;
import org.quick.widget.core.layout.SimpleSizeGuide;
import org.quick.widget.core.layout.SizeGuide;
import org.quick.widget.core.model.DocumentedElement;

/** Controls the location of the text inside a text-editing widget */
public class TextEditLayout implements QuickWidgetLayout {
	private final CompoundListener<QuickWidget<?>> theListener;

	/** Creates the layout */
	public TextEditLayout() {
		theListener = CompoundListener.<QuickWidget<?>> buildFromQDW()//
			.acceptAll(TextField.charLengthAtt, TextField.charRowsAtt).onEvent(sizeNeedsChanged)//
			.child(childBuilder -> {
				childBuilder.watchAll(org.quick.core.style.FontStyle.getDomainInstance()).onEvent(layout);
			})//
			.build();
	}

	@Override
	public void install(QuickWidget<?> parent, Observable<?> until) {
		Map<ElementId, SimpleObservable<Void>> childRemoves = new HashMap<>();
		try (Transaction t = parent.getChildren().lock(false, null)) {
			SimpleObservable<Void> remove = new SimpleObservable<>(null, false, parent.getElement().getAttributeLocker(), null);
			CollectionElement<? extends QuickWidget<?>> el = parent.getChildren().getTerminalElement(true);
			while (el != null) {
				childAdded(parent, el.get(), remove);
				childRemoves.put(el.getElementId(), remove);
				el = parent.getChildren().getAdjacentElement(el.getElementId(), true);
			}
			parent.getChildren().onChange(evt -> {
				switch (evt.getType()) {
				case add:
					SimpleObservable<Void> chRemove = new SimpleObservable<>(null, false, parent.getElement().getAttributeLocker(), null);
					childAdded(parent, evt.getNewValue(), chRemove);
					childRemoves.put(evt.getElementId(), chRemove);
					break;
				case remove:
					childRemoves.remove(evt.getElementId()).onNext(null);
					break;
				case set:
					if (evt.getOldValue() != evt.getNewValue()) {
						chRemove = childRemoves.get(evt.getElementId());
						chRemove.onNext(null);
						childAdded(parent, evt.getNewValue(), chRemove);
					}
					break;
				}
			});
		}
		theListener.listen(parent, parent, until);
	}

	private void childAdded(QuickWidget<?> parent, QuickWidget<?> child, Observable<?> until) {
		if (child instanceof DocumentedElement) {
			QuickDocumentModel doc = QuickDocumentModel.flatten(((DocumentedElement) child).getDocumentModel());
			doc.changes().takeUntil(until).filter(evt -> evt instanceof ContentChangeEvent || evt instanceof StyleChangeEvent)
			.act(evt -> parent.relayout(false));
		} else
			parent.getElement().msg()
			.error(getClass().getSimpleName() + " requires the container's child to be a " + DocumentedElement.class.getName());
	}

	@Override
	public SizeGuide getSizer(QuickWidget<?> parent, Iterable<? extends QuickWidget<?>> children, Orientation orientation) {
		Iterator<? extends QuickWidget<?>> iter = children.iterator();
		if (!iter.hasNext())
			return new SimpleSizeGuide();
		QuickWidget<?> firstChild = iter.next();
		if (iter.hasNext()) {
			parent.getElement().msg().error(getClass().getSimpleName() + " allows only one child in a container");
			return new SimpleSizeGuide();
		}
		if (!(firstChild instanceof DocumentedElement)) {
			parent.getElement().msg()
			.error(getClass().getSimpleName() + " requires the container's child to be a " + DocumentedElement.class.getName());
			return new SimpleSizeGuide();
		}
		return new SizeGuide.GenericSizeGuide() {
			@Override
			public int get(LayoutGuideType type, int crossSize, boolean csMax) {
				if(type.isPref()) {
					QuickDocumentModel doc = ((DocumentedElement) firstChild).getDocumentModel().get();
					QuickStyle style;
					if (doc.length() > 0)
						style = doc.getStyleAt(0);
					else
						style = firstChild.getElement().getStyle();
					Font font = QuickUtils.getFont(style).get();
					FontRenderContext ctx = new FontRenderContext(font.getTransform(), style.get(FontStyle.antiAlias).get().booleanValue(),
						false);
					if (orientation.isVertical()) {
						final Integer rows = parent.getElement().atts().get(TextField.charRowsAtt).get();
						if (rows != null)
							return (int) (rows * font.getStringBounds("00", ctx).getHeight());
					} else {
						final int length = parent.getElement().atts().getValue(TextField.charLengthAtt, 12);
						return (int) (length * font.getStringBounds("00", ctx).getWidth() / 2);
					}
				}
				return firstChild.bounds().get(orientation).getGuide().get(type, crossSize, csMax);
			}

			@Override
			public int getBaseline(int size) {
				return firstChild.bounds().getHorizontal().getGuide().getBaseline(size);
			}
		};
	}

	@Override
	public void layout(QuickWidget<?> parent, List<? extends QuickWidget<?>> children) {
		if (children.size() != 1) {
			parent.getElement().msg().error(getClass().getSimpleName() + " allows exactly one child in a container");
			return;
		}
		if (!(children.get(0) instanceof DocumentedElement)) {
			parent.getElement().msg()
			.error(getClass().getSimpleName() + " requires the container's child to be a " + DocumentedElement.class.getName());
			return;
		}
		QuickWidget<?> child = children.get(0);
		DocumentedElement docChild = (DocumentedElement) child;
		boolean wrap = child.getElement().getStyle().get(org.quick.core.style.FontStyle.wordWrap).get();
		int w = parent.bounds().getWidth();
		int h = child.bounds().getVertical().getGuide().getPreferred(w, !wrap);

		if (!(docChild.getDocumentModel().get() instanceof SelectableDocumentModel)) {
			child.bounds().setBounds(0, 0, w, h);
			return;
		}
		SelectableDocumentModel doc = (SelectableDocumentModel) docChild.getDocumentModel().get();
		Point2D loc = docChild.getRenderableDocument().getLocationAt(doc.getCursor(), Integer.MAX_VALUE);
		int x = child.bounds().getX();
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

		int y = child.bounds().getY();
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

		child.bounds().setBounds(x, y, w, h);
	}

	@Override
	public String toString() {
		return "text-edit-layout";
	}
}
