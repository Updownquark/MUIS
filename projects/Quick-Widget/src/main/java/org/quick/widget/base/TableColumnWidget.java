package org.quick.widget.base;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.observe.ObservableValue;
import org.observe.collect.ObservableCollection;
import org.qommons.Transaction;
import org.qommons.collect.CollectionElement;
import org.qommons.collect.ElementId;
import org.quick.base.widget.TableColumn;
import org.quick.core.QuickConstants.CoreStage;
import org.quick.core.QuickDefinedWidget;
import org.quick.core.QuickException;
import org.quick.core.layout.LayoutAttributes;
import org.quick.core.layout.LayoutGuideType;
import org.quick.core.layout.LayoutSize;
import org.quick.core.layout.Orientation;
import org.quick.widget.base.layout.BaseLayoutUtils;
import org.quick.widget.core.Point;
import org.quick.widget.core.QuickElementCapture;
import org.quick.widget.core.QuickTemplateWidget;
import org.quick.widget.core.QuickWidget;
import org.quick.widget.core.QuickWidgetDocument;
import org.quick.widget.core.Rectangle;
import org.quick.widget.core.event.MouseEvent;
import org.quick.widget.core.event.MouseEvent.MouseEventType;
import org.quick.widget.core.layout.SizeGuide;

public class TableColumnWidget<R, C, E extends TableColumn<R, C>> extends QuickTemplateWidget<E> {
	private Point theHoveredPoint;

	private final ObservableValue<QuickWidget<?>> theRenderer;
	private final ObservableValue<QuickWidget<?>> theHover;
	private final ObservableValue<QuickWidget<?>> theEditor;

	public TableColumnWidget() {
		theRenderer = getElement().getRenderer().map(QuickWidget.WILDCARD, renderer -> getChild(renderer));
		theHover = getElement().getRenderer().map(QuickWidget.WILDCARD, hover -> getChild(hover));
		theEditor = getElement().getRenderer().map(QuickWidget.WILDCARD, editor -> getChild(editor));
	}

	@Override
	public void init(QuickWidgetDocument document, E element, QuickDefinedWidget<QuickWidgetDocument, ?> parent) throws QuickException {
		super.init(document, element, parent);

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
			ObservableCollection<R> rows = getTable().getElement().getRows();
			events().filterMap(MouseEvent.mouse.addTypes(MouseEventType.moved)).act(evt -> {
				theHoveredPoint = evt.getPosition(TableColumnWidget.this);
				updateHover(evt);
			});
			events().filterMap(MouseEvent.mouse.addTypes(MouseEventType.exited)).act(evt -> {
				theHoveredPoint = null;
				updateHover(evt);
			});
			events().filterMap(MouseEvent.mouse.addTypes(MouseEventType.pressed)).act(evt -> {
				int row = getTable().getRowAt(evt.getPosition(TableColumnWidget.this));
				ElementId rowId = row < 0 ? null : rows.getElement(row).getElementId();
				getElement().getTable().setSelected(getElement().getColumnId(), rowId, evt);
			});

			getElement().getHoverElement().noInitChanges().act(evt -> {
				sizeNeedsChanged();
				relayout(false);
			});
			getElement().getEditorElement().noInitChanges().act(evt -> {
				sizeNeedsChanged();
				relayout(false);
			});
		}, CoreStage.STARTUP, 1);
	}

	public TableWidget<R, C> getTable() {
		return (TableWidget<R, C>) getParent().get();
	}

	private void updateHover(Object cause) {
		ElementId row;
		if (theHoveredPoint == null)
			row = null;
		else {
			int rowIdx = getTable().getRowAt(theHoveredPoint);
			row = rowIdx < 0 ? null : getElement().getTable().getRows().getElement(rowIdx).getElementId();
			getElement().getTable().setHovered(getElement().getColumnId(), row, cause);
		}
	}

	public SizeGuide getCellSize(Object row) {}

	public Rectangle getCellBounds(ElementId row) {
		TableWidget<R, C> table = getTable();
		int index = table.getElement().getRows().getElementsBefore(row);
		int rowPos = table.getRowPosition(index);
		int rowSize = table.getRowLength(index);
		int padding = 0; // TODO Margin/padding between columns?
		if (table.getElement().atts().get(LayoutAttributes.orientation).get().isVertical())
			return new Rectangle(padding, rowPos, bounds().getWidth(), rowSize);
		else
			return new Rectangle(rowPos, padding, rowSize, bounds().getHeight());
	}

	@Override
	public SizeGuide getSizer(Orientation orientation) {
		if (orientation == getTable().getElement().atts().get(LayoutAttributes.orientation).get())
			return getTable().getSizer(orientation);
		else {
			ObservableCollection<R> rows = getTable().getElement().getRows();
			QuickWidget<?> renderer = theRenderer.get();
			return new SizeGuide.GenericSizeGuide() {
				@Override
				public int get(LayoutGuideType type, int crossSize, boolean csMax) {
					return BaseLayoutUtils.getBoxLayoutCrossSize(new Iterable<QuickWidget<?>>() {
						@Override
						public Iterator<QuickWidget<?>> iterator() {
							return new Iterator<QuickWidget<?>>() {
								private ElementId theRow = CollectionElement.getElementId(rows.getTerminalElement(true));

								@Override
								public boolean hasNext() {
									if (theRow == null) {
										try (Transaction t = renderer.holdEvents(true, true)) {
											getElement().getRenderElement().set(null, null);
										}
									}
									return theRow != null;
								}

								@Override
								public QuickWidget<?> next() {
									try (Transaction t = renderer.holdEvents(true, true)) {
										getElement().getRenderElement().set(rows.getElement(theRow), null);
									}
									theRow = CollectionElement.getElementId(rows.getAdjacentElement(theRow, true));
									return renderer;
								}
							};
						}
					}, orientation.opposite(), type, crossSize, csMax, new LayoutSize());
				}

				@Override
				public int getBaseline(int size) {
					return 0;
				}
			};
		}
	}

	@Override
	public void doLayout() {
		CollectionElement<R> hoveredRow = getElement().getHoverElement().get();
		if (hoveredRow != null)
			theHover.get().bounds().set(getCellBounds(hoveredRow.getElementId()), null);
		CollectionElement<R> editorRow = getElement().getEditorElement().get();
		if (editorRow != null)
			theEditor.get().bounds().set(getCellBounds(editorRow.getElementId()), null);
	}

	@Override
	public void paintSelf(Graphics2D graphics, Rectangle area) {
		super.paintSelf(graphics, area);
		TableWidget<R, C> table = getTable();
		ObservableCollection<R> rows = table.getElement().getRows();
		if (rows.isEmpty())
			return;
		// Render all cells overlapping the given rectangle
		if (area == null)
			area = new Rectangle(0, 0, bounds().getX(), bounds().getY());
		Orientation orientation = table.getElement().atts().get(LayoutAttributes.orientation).get();
		int start, end;
		if (orientation.isVertical()) {
			start = area.y;
			end = area.getMaxY();
		} else {
			start = area.x;
			end = area.getMaxX();
		}
		int rowIdx = table.getRowAt(area.getPosition());
		if (rowIdx < 0) {
			if (table.getRowPosition(0) >= start)
				rowIdx = 0;
			else
				return;
		}
		ElementId row = rows.getElement(rowIdx).getElementId();
		int initIndex = rowIdx;
		int initPosition = table.getRowPosition(initIndex);
		if (initPosition < end) {
			QuickWidget<?> renderer = theRenderer.get();
			paintChildren(new Iterable<QuickWidget<?>>() {
				@Override
				public Iterator<QuickWidget<?>> iterator() {
					return new Iterator<QuickWidget<?>>() {
						ElementId theRow = row;
						int theIndex = initIndex;
						int thePosition = initPosition;

						@Override
						public boolean hasNext() {
							return thePosition < end;
						}

						@Override
						public QuickWidget<?> next() {
							try (Transaction t = renderer.holdEvents(true, true)) {
								getElement().getRenderElement().set(rows.getElement(theRow), null);
								int padding = 0; // TODO Margin/padding between columns?
								if (orientation.isVertical())
									renderer.bounds().setBounds(padding, thePosition, bounds().getWidth(), table.getRowLength(theIndex));
								else
									renderer.bounds().setBounds(thePosition, padding, table.getRowLength(theIndex),
										bounds().getHeight());
							}
							theRow = CollectionElement.getElementId(rows.getAdjacentElement(theRow, true));
							theIndex++;
							thePosition = table.getRowPosition(theIndex);
							return renderer;
						}
					};
				}
			}, graphics, area);
			try (Transaction t = renderer.holdEvents(true, true)) {
				getElement().getRenderElement().set(null, null);
			}
		}
	}

	@Override
	public QuickElementCapture[] paintChildren(Graphics2D graphics, Rectangle area) {
		List<QuickWidget<?>> children = new ArrayList<>(2);
		CollectionElement<R> hoveredRow = getElement().getHoverElement().get();
		if (hoveredRow != null)
			children.add(theHover.get());
		CollectionElement<R> editorRow = getElement().getEditorElement().get();
		if (editorRow != null)
			children.add(theEditor.get());
		if (area == null)
			area = new Rectangle(0, 0, bounds().getWidth(), bounds().getHeight());
		return paintChildren(children, graphics, area);
	}
}
