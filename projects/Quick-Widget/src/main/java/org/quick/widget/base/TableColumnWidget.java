package org.quick.widget.base;

import java.util.Objects;

import org.observe.ObservableValue;
import org.observe.SettableValue;
import org.observe.SimpleSettableValue;
import org.observe.collect.ObservableCollection;
import org.qommons.Transaction;
import org.qommons.collect.CollectionElement;
import org.qommons.collect.ElementId;
import org.quick.base.widget.TableColumn;
import org.quick.core.QuickConstants.CoreStage;
import org.quick.core.layout.LayoutAttributes;
import org.quick.core.layout.Orientation;
import org.quick.widget.core.Point;
import org.quick.widget.core.QuickTemplateWidget;
import org.quick.widget.core.QuickWidget;
import org.quick.widget.core.QuickWidgetDocument;
import org.quick.widget.core.Rectangle;
import org.quick.widget.core.event.MouseEvent;
import org.quick.widget.core.event.MouseEvent.MouseEventType;

import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;

public class TableColumnWidget<R, C> extends QuickTemplateWidget {
	private Point theHoveredPoint;
	private boolean isSelected;

	private final SettableValue<CollectionElement<R>> theRenderElement;
	private final ObservableValue<R> theRenderValue;

	public TableColumnWidget(QuickWidgetDocument doc, TableColumn<R, C> element, QuickWidget parent) {
		super(doc, element, parent);

		ObservableCollection<R> rows = getElement().getTable().getRows();
		theRenderElement = new SimpleSettableValue<>(
			new TypeToken<CollectionElement<R>>() {}.where(new TypeParameter<R>() {}, rows.getType()), true,
			getElement().getAttributeLocker(), null);
		theRenderValue = theRenderElement.map(rows.getType(), el -> el.get());

		getElement().life().runWhen(() -> {
			getElement().getResourcePool().pool(getElement().getTable().getRows().changes()).act(evt -> {
				Orientation orientation = getElement().getTable().atts().get(LayoutAttributes.orientation).get();
				int firstRowPos = getTable().getRowPosition(evt.elements.get(0).index);
				switch (evt.type) {
				case add:
				case remove:
					if (orientation.isVertical())
						repaint(new Rectangle(0, firstRowPos, bounds().getWidth(), bounds().getHeight() - firstRowPos), false);
					else
						repaint(new Rectangle(firstRowPos, 0, bounds().getWidth() - firstRowPos, bounds().getHeight()), false);
					break;
				case set:
					int lastRowEnd = getTable().getRowPosition(evt.elements.get(evt.elements.size() - 1).index)//
						+ getTable().getRowLength(evt.elements.get(evt.elements.size() - 1).index);
					if (orientation.isVertical())
						repaint(new Rectangle(0, firstRowPos, bounds().getWidth(), lastRowEnd), false);
					else
						repaint(new Rectangle(firstRowPos, 0, lastRowEnd, bounds().getHeight()), false);
					break;
				}
			});
		}, CoreStage.INIT_CHILDREN, 1);
		getElement().life().runWhen(() -> {
			events().filterMap(MouseEvent.mouse.addTypes(MouseEventType.moved)).act(evt -> {
				theHoveredPoint = evt.getPosition(TableColumnWidget.this);
				updateHover();
			});
			events().filterMap(MouseEvent.mouse.addTypes(MouseEventType.exited)).act(evt -> {
				theHoveredPoint = null;
				updateHover();
			});
			events().filterMap(MouseEvent.mouse.addTypes(MouseEventType.pressed)).act(evt -> {
				int row = getTable().getRowAt(evt.getPosition(TableColumnWidget.this));
				ElementId rowId = row < 0 ? null : rows.getElement(row).getElementId();
				if (isSelected && Objects.equals(getElement().getTable().getSelectedRow().get(), rowId))
					return;

				isSelected = true;
				getElement().getTable().setSelected(getElement().getColumnElement(), rowId);
			});

			getElement().getHoverElement().changes().act(evt -> {
				ElementId row;
				if (theHoveredPoint == null)
					row = null;
				else {
					int rowIdx = getTable().getRowAt(theHoveredPoint);
					row = rowIdx < 0 ? null : getTable().getRows().getElement(rowIdx).getElementId();
					if (isSelected && Objects.equals(theSelectedRow, row))
						row = null;
				}
				boolean wasActive = theHoveredRow != null;
				if (row == null) {
					if (wasActive) {
						// TODO Disable hover
						theHover.getResourcePool().setActive(false);
						try (Transaction t = theEditor.holdEvents(true, false)) {
							theHover.bounds().setBounds(-1, -1, 0, 0);
						}
						sizeNeedsChanged();
						relayout(false);
					}
				} else if (!Objects.equals(row, theHoveredRow)) {
					theHoveredRow = row;
					theHoverValue.set(getTable().getRows().getElement(row).get(), null);
					try (Transaction t = theEditor.holdEvents(true, false)) {
						theHover.bounds().set(getCellBounds(row), null);
					}
					if (!wasActive) {
						// Enable hover
						theHover.getResourcePool().setActive(true);
					}
					sizeNeedsChanged();
					relayout(false);
				}
			});
			getElement().getEditorElement().changes().act(evt -> {
				boolean wasActive = theEditingRow != null;
				if (row == null) {
					if (wasActive) {
						// Disable editor
						theEditor.getResourcePool().setActive(false);
						try (Transaction t = theEditor.holdEvents(true, false)) {
							theEditor.bounds().setBounds(-1, -1, 0, 0);
						}
						updateHover();
						sizeNeedsChanged();
						relayout(false);
					}
				} else if (!Objects.equals(row, theEditingRow)) {
					theEditingRow = row;
					theEditorValue.set(getTable().getRows().getElement(row).get(), null);
					try (Transaction t = theEditor.holdEvents(true, false)) {
						theEditor.bounds().set(getCellBounds(row), null);
					}
					if (!wasActive) {
						// Enable editor
						theEditor.getResourcePool().setActive(true);
					}
					updateHover();
					sizeNeedsChanged();
					relayout(false);
				}
			});
		}, CoreStage.STARTUP, 1);
	}

	@Override
	public TableColumn<R, C> getElement() {
		return (TableColumn<R, C>) super.getElement();
	}

	public TableWidget<R, C> getTable() {
		return (TableWidget<R, C>) getParent().get();
	}
}
