package org.quick.base.layout;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.geom.Point2D;
import java.util.*;

import org.observe.Observable;
import org.observe.SimpleObservable;
import org.qommons.Transaction;
import org.qommons.collect.CollectionElement;
import org.qommons.collect.ElementId;
import org.quick.core.QuickElement;
import org.quick.core.QuickLayout;
import org.quick.core.layout.*;
import org.quick.core.model.DocumentedElement;
import org.quick.core.model.QuickDocumentModel;
import org.quick.core.model.QuickDocumentModel.ContentChangeEvent;
import org.quick.core.model.QuickDocumentModel.StyleChangeEvent;
import org.quick.core.model.SelectableDocumentModel;
import org.quick.core.prop.QuickAttribute;
import org.quick.core.prop.QuickPropertyType;
import org.quick.core.style.FontStyle;
import org.quick.core.style.QuickStyle;
import org.quick.util.CompoundListener;
import org.quick.util.QuickUtils;

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
		Map<ElementId, SimpleObservable<Void>> childRemoves = new HashMap<>();
		try (Transaction t = parent.ch().lock(false, null)) {
			SimpleObservable<Void> remove = new SimpleObservable<>(null, false, parent.getAttributeLocker(), null);
			CollectionElement<? extends QuickElement> el = parent.ch().getTerminalElement(true);
			while (el != null) {
				childAdded(parent, el.get(), remove);
				childRemoves.put(el.getElementId(), remove);
				el = parent.ch().getAdjacentElement(el.getElementId(), true);
			}
			parent.ch().onChange(evt -> {
				switch (evt.getType()) {
				case add:
					SimpleObservable<Void> chRemove = new SimpleObservable<>(null, false, parent.getAttributeLocker(), null);
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

	private void childAdded(QuickElement parent, QuickElement child, Observable<?> until) {
		if (child instanceof DocumentedElement) {
			QuickDocumentModel doc = QuickDocumentModel.flatten(((DocumentedElement) child).getDocumentModel());
			doc.changes().takeUntil(until).filter(evt -> evt instanceof ContentChangeEvent || evt instanceof StyleChangeEvent)
				.act(evt -> parent.relayout(false));
		} else
			parent.msg().error(getClass().getSimpleName() + " requires the container's child to be a " + DocumentedElement.class.getName());
	}

	@Override
	public SizeGuide getSizer(QuickElement parent, Iterable<? extends QuickElement> children, Orientation orientation) {
		Iterator<? extends QuickElement> iter = children.iterator();
		if (!iter.hasNext())
			return new SimpleSizeGuide();
		QuickElement firstChild = iter.next();
		if (iter.hasNext()) {
			parent.msg().error(getClass().getSimpleName() + " allows only one child in a container");
			return new SimpleSizeGuide();
		}
		if (!(firstChild instanceof DocumentedElement)) {
			parent.msg().error(getClass().getSimpleName() + " requires the container's child to be a " + DocumentedElement.class.getName());
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
						style = firstChild.getStyle();
					Font font = QuickUtils.getFont(style).get();
					FontRenderContext ctx = new FontRenderContext(font.getTransform(), style.get(FontStyle.antiAlias).get().booleanValue(),
						false);
					if (orientation.isVertical()) {
						final Integer rows = parent.atts().get(charRowsAtt).get();
						if (rows != null)
							return (int) (rows * font.getStringBounds("00", ctx).getHeight());
					} else {
						final int length = parent.atts().getValue(charLengthAtt, 12);
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
	public void layout(QuickElement parent, List<? extends QuickElement> children) {
		if (children.size() != 1) {
			parent.msg().error(getClass().getSimpleName() + " allows exactly one child in a container");
			return;
		}
		if (!(children.get(0) instanceof DocumentedElement)) {
			parent.msg().error(getClass().getSimpleName() + " requires the container's child to be a " + DocumentedElement.class.getName());
			return;
		}
		QuickElement child = children.get(0);
		DocumentedElement docChild = (DocumentedElement) child;
		boolean wrap = child.getStyle().get(org.quick.core.style.FontStyle.wordWrap).get();
		int w = parent.bounds().getWidth();
		int h = child.bounds().getVertical().getGuide().getPreferred(w, !wrap);

		if (!(docChild.getDocumentModel().get() instanceof SelectableDocumentModel)) {
			child.bounds().setBounds(0, 0, w, h);
			return;
		}
		SelectableDocumentModel doc = (SelectableDocumentModel) docChild.getDocumentModel().get();
		Point2D loc = doc.getLocationAt(doc.getCursor(), Integer.MAX_VALUE);
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
